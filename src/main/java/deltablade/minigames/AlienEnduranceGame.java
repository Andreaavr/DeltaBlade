package deltablade.minigames;

import com.almasb.fxgl.entity.Entity;
import deltablade.FlightPaths;
import deltablade.GameVars;
import deltablade.SoundHelper;
import deltablade.SpriteSheets;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static com.almasb.fxgl.dsl.FXGL.getAppHeight;
import static com.almasb.fxgl.dsl.FXGL.getAppWidth;
import static com.almasb.fxgl.dsl.FXGL.geti;

/**
 * Alien Endurance: survive as long as possible against colourful alien swarms.
 * Own life counter (the main game's lives are never touched).
 */
public class AlienEnduranceGame implements Minigame {

    static final int START_LIVES = 3;
    static final int MAX_LIVES = 5;
    private static final double SHOT_SPEED = 560;
    private static final double FIRE_GAP = 0.17;
    private static final double INVULN = 1.6;
    private static final double PLAYER_RADIUS = 13;

    final Random rng = new Random();
    MinigameHost host;
    final Group layer = new Group();
    private Text hudText;

    double time;
    int lives;
    private double invuln;
    boolean over;
    private double fireCd;
    private double spawnTimer;
    int kills;
    int killScore;

    final List<Alien> aliens = new ArrayList<>();
    private final List<Shot> shots = new ArrayList<>();
    final List<Bullet> bullets = new ArrayList<>();

    static final class Alien {
        int kind;
        ImageView view;
        double x, y, size;
        List<Point2D> path;
        int idx;
        double speed;
        int hp, maxHp;
        double shootCd;
        double delay;
        boolean boss;
        double hitFlash;
        // boss data
        boolean big;
        double bossT, attackCd, specialCd, helperCd, homeX;
        int phase;
    }

    private static final class Shot {
        Rectangle node;
        double x, y, vx;
    }

    static final class Bullet {
        Circle node;
        double x, y, vx, vy;
    }

    @Override
    public void start(MinigameHost host) {
        this.host = host;
        lives = START_LIVES;
        time = 0;
        spawnTimer = 1.5;
        host.hidePlayer(false);
        host.addUi(layer);
        hudText = new Text();
        hudText.setFont(Font.font("Monospace", FontWeight.BOLD, 14));
        hudText.setFill(Color.rgb(140, 255, 220));
        hudText.setEffect(new DropShadow(6, Color.BLACK));
        hudText.setTranslateX(host.playLeft() + 12);
        hudText.setTranslateY(host.playTop() + 22);
        layer.getChildren().add(hudText);
        host.displayBanner("ALIEN ENDURANCE", Color.rgb(120, 255, 200), 2.0);
    }

    @Override
    public boolean usesPlayerShip() {
        return true;
    }

    @Override
    public void update(double tpf) {
        if (over) {
            updateAfterGame(tpf);
            return;
        }
        time += tpf;
        fireCd -= tpf;
        if (host.holdingFire() && fireCd <= 0) {
            fireShot();
        }
        updateDirector(tpf);
        maybeSpawnBoss();
        updateShots(tpf);
        updateAliens(tpf);
        updateBullets(tpf);
        updateExtras(tpf);
        updateInvuln(tpf);
        refreshHud();
    }

    @Override
    public void onFirePress() {
        if (over) {
            onConfirm();
            return;
        }
        if (fireCd <= 0) {
            fireShot();
        }
    }

    @Override
    public void cleanup() {
        aliens.clear();
        shots.clear();
        bullets.clear();
        layer.getChildren().clear();
        host.removeUi(layer);
        Entity p = host.player();
        if (p != null && p.isActive()) {
            p.getViewComponent().setOpacity(1.0);
        }
    }

    // ---------- hooks for later steps ----------

    // ---------- falling hearts (extra lives) ----------

    private static final class Heart {
        javafx.scene.Node node;
        double x, y, t;
    }

    private final List<Heart> hearts = new ArrayList<>();
    private double heartTimer = 40 + new Random().nextDouble() * 20;
    int heartsCaught;

    void updateExtras(double tpf) {
        heartTimer -= tpf;
        if (heartTimer <= 0) {
            dropHeart(host.playLeft() + 40 + rng.nextDouble() * Math.max(40, host.playRight() - host.playLeft() - 80), -20);
            heartTimer = time > 180 ? 75 : 40 + rng.nextDouble() * 20;
        }
        Iterator<Heart> it = hearts.iterator();
        while (it.hasNext()) {
            Heart h = it.next();
            h.t += tpf;
            h.y += 90 * tpf;
            h.x += Math.sin(h.t * 3) * 30 * tpf;
            double pulse = 1.0 + Math.sin(h.t * 6) * 0.12;
            h.node.setScaleX(pulse);
            h.node.setScaleY(pulse);
            h.node.setTranslateX(h.x - 14);
            h.node.setTranslateY(h.y - 14);
            double dx = h.x - playerX();
            double dy = h.y - playerY();
            boolean caught = dx * dx + dy * dy < 32 * 32;
            if (caught) {
                heartsCaught++;
                if (lives < MAX_LIVES) {
                    lives++;
                    host.displayBanner("EXTRA LIFE!", Color.HOTPINK, 1.0);
                } else {
                    killScore += 500;
                    host.displayBanner("LIVES FULL  +500", Color.HOTPINK, 1.0);
                }
                SoundHelper.play("extra_life.wav");
            }
            if (caught || h.y > getAppHeight() + 30) {
                layer.getChildren().remove(h.node);
                it.remove();
            }
        }
    }

    void dropHeart(double x, double y) {
        Heart h = new Heart();
        h.x = x;
        h.y = y;
        javafx.scene.image.Image img = deltablade.EmbeddedTextures.getImage("heart.png", 28, 28);
        if (img != null) {
            ImageView v = new ImageView(img);
            v.setFitWidth(28);
            v.setFitHeight(28);
            v.setPreserveRatio(true);
            h.node = v;
        } else {
            Text t = new Text("\u2665");
            t.setFont(Font.font("Monospace", FontWeight.BOLD, 26));
            t.setFill(Color.HOTPINK);
            t.setTranslateY(24);
            h.node = new Group(t);
        }
        h.node.setEffect(new DropShadow(12, Color.HOTPINK));
        layer.getChildren().add(h.node);
        hearts.add(h);
    }

    void updateAfterGame(double tpf) {}

    void onConfirm() {}

    void onAlienKilled(Alien a) {}

    void endRun() {
        over = true;
        int score = (int) time * 100 + killScore;
        host.finishMinigame(false, score, kills * 2, "SURVIVED " + timeLabel() + "  +" + score, Color.rgb(120, 255, 200));
    }

    // ---------- player ----------

    double playerX() {
        Entity p = host.player();
        return p == null ? getAppWidth() / 2.0 : p.getX() + p.getWidth() / 2;
    }

    double playerY() {
        Entity p = host.player();
        return p == null ? getAppHeight() - 60 : p.getY() + p.getHeight() / 2;
    }

    private void fireShot() {
        fireCd = FIRE_GAP;
        int grade = Math.max(1, Math.min(3, geti(GameVars.WEAPON_GRADE)));
        double[] spread = grade == 1 ? new double[] {0} : grade == 2 ? new double[] {-7, 7} : new double[] {-12, 0, 12};
        for (double dx : spread) {
            Shot s = new Shot();
            s.x = playerX() + dx;
            s.y = playerY() - 22;
            s.vx = dx * 3;
            s.node = new Rectangle(3, 12, Color.rgb(140, 255, 230));
            s.node.setEffect(new DropShadow(6, Color.AQUA));
            layer.getChildren().add(s.node);
            shots.add(s);
        }
        SoundHelper.play("shot.wav");
    }

    private void updateInvuln(double tpf) {
        Entity p = host.player();
        if (p == null || !p.isActive()) {
            return;
        }
        if (invuln > 0) {
            invuln -= tpf;
            p.getViewComponent().setOpacity(((int) (invuln * 12)) % 2 == 0 ? 0.25 : 1.0);
        } else {
            p.getViewComponent().setOpacity(1.0);
        }
    }

    void hitPlayer() {
        if (invuln > 0 || over) {
            return;
        }
        lives--;
        invuln = INVULN;
        host.spawnExplosion(playerX(), playerY(), "ship");
        SoundHelper.play("bad_drop.wav");
        if (lives <= 0) {
            host.spawnExplosion(playerX(), playerY(), "ship");
            endRun();
        }
    }

    // ---------- spawning ----------

    /** Danger level: rises by one every 30 seconds. */
    int stage() {
        return (int) (time / 30.0);
    }

    private int announcedStage;

    /** Spawn pace multiplier (bosses slow the stream down). */
    double paceFactor() {
        return activeBoss != null ? 0.45 : 1.0;
    }

    void updateDirector(double tpf) {
        int st = stage();
        if (st > announcedStage) {
            announcedStage = st;
            host.displayBanner("DANGER " + (st + 1), Color.rgb(255, 120 + Math.max(0, 120 - st * 15), 80), 1.2);
        }
        spawnTimer -= tpf * paceFactor();
        if (spawnTimer > 0) {
            return;
        }
        spawnTimer = Math.max(0.8, 3.0 - 0.28 * st);
        int count = Math.min(12, 4 + st);
        double speed = Math.min(340, 170 + 18 * st);
        int kindsUnlocked = Math.min(5, 1 + st);
        boolean mixed = st >= 4 && rng.nextDouble() < 0.4;
        int kind = rng.nextInt(kindsUnlocked);
        boolean mirror = rng.nextBoolean();
        spawnGroupOnPath(kind, randomPath(st, mirror), count, speed, mixed);
        if (st >= 5 && rng.nextDouble() < 0.5) {
            // twin group from the other side
            spawnGroupOnPath(rng.nextInt(kindsUnlocked), randomPath(st, !mirror), Math.max(3, count - 3), speed, mixed);
        }
    }

    private List<Point2D> randomPath(int st, boolean mirror) {
        List<Object> pool = new ArrayList<>(List.of(FlightPaths.Kind.BOGEN, FlightPaths.Kind.WELLE, FlightPaths.Kind.S_KURVE));
        if (st >= 1) {
            pool.add(FlightPaths.Cross.ZICKZACK);
            pool.add(FlightPaths.Kind.SCHLEIFE);
        }
        if (st >= 2) {
            pool.add(FlightPaths.Kind.KREIS);
            pool.add(FlightPaths.Cross.ACHT);
        }
        if (st >= 3) {
            pool.add(FlightPaths.Kind.SPIRALE);
            pool.add(FlightPaths.Cross.SCHWARM);
        }
        if (st >= 4) {
            pool.add(FlightPaths.Cross.REGEN);
            pool.add(FlightPaths.Kind.HAKEN);
        }
        Object pick = pool.get(rng.nextInt(pool.size()));
        if (pick instanceof FlightPaths.Cross c) {
            return FlightPaths.crossing(c, mirror, rng.nextDouble());
        }
        return withExit(FlightPaths.build((FlightPaths.Kind) pick, mirror));
    }

    void spawnGroup(int kind, FlightPaths.Kind pathKind, boolean mirror, int count, double speed, boolean mixed) {
        List<Point2D> path = withExit(FlightPaths.build(pathKind, mirror));
        spawnGroupOnPath(kind, path, count, speed, mixed);
    }

    void spawnGroupOnPath(int kind, List<Point2D> path, int count, double speed, boolean mixed) {
        for (int i = 0; i < count; i++) {
            int k = mixed ? rng.nextInt(SpriteSheets.ALIEN_LEAF + 1) : kind;
            Alien a = newAlien(k, 40, 1 + (k == SpriteSheets.ALIEN_JELLY ? 1 : 0));
            a.path = path;
            a.speed = speed;
            a.delay = i * (34.0 / speed) * 1.25;
            a.x = path.get(0).getX();
            a.y = path.get(0).getY();
            a.view.setVisible(false);
        }
    }

    Alien newAlien(int kind, double size, int hp) {
        Alien a = new Alien();
        a.kind = kind;
        a.size = size;
        a.hp = hp;
        a.maxHp = hp;
        a.view = SpriteSheets.alienView(kind, size);
        a.shootCd = 1.5 + rng.nextDouble() * 3;
        layer.getChildren().add(a.view);
        aliens.add(a);
        return a;
    }

    /** Path continues off the bottom/side so the group leaves the screen. */
    static List<Point2D> withExit(List<Point2D> path) {
        List<Point2D> out = new ArrayList<>(path);
        Point2D last = path.get(path.size() - 1);
        double w = getAppWidth();
        double exitX = last.getX() < w / 2 ? -80 : w + 80;
        out.add(new Point2D((last.getX() + exitX) / 2, last.getY() + 160));
        out.add(new Point2D(exitX, getAppHeight() + 80));
        return out;
    }

    // ---------- movement ----------

    private void updateShots(double tpf) {
        Iterator<Shot> it = shots.iterator();
        while (it.hasNext()) {
            Shot s = it.next();
            s.y -= SHOT_SPEED * tpf;
            s.x += s.vx * tpf;
            s.node.setTranslateX(s.x - 1.5);
            s.node.setTranslateY(s.y - 6);
            boolean gone = s.y < host.playTop() - 20;
            if (!gone) {
                for (Alien a : aliens) {
                    if (a.delay > 0 || a.hp <= 0) {
                        continue;
                    }
                    double r = a.size * 0.42;
                    if (Math.abs(s.x - a.x) < r && Math.abs(s.y - a.y) < r) {
                        damage(a, 1);
                        gone = true;
                        break;
                    }
                }
            }
            if (gone) {
                layer.getChildren().remove(s.node);
                it.remove();
            }
        }
    }

    void damage(Alien a, int dmg) {
        a.hp -= dmg;
        a.hitFlash = 0.08;
        if (a.hp <= 0) {
            kills++;
            killScore += killValue(a.kind);
            host.spawnExplosion(a.x, a.y, a.boss ? "big" : "ship");
            SoundHelper.play(a.boss ? "explode_boss.wav" : "explode_ship.wav");
            if (a.boss) {
                onBossKilled(a);
            }
            onAlienKilled(a);
        }
    }

    static int killValue(int kind) {
        return switch (kind) {
            case SpriteSheets.ALIEN_CRAB -> 50;
            case SpriteSheets.ALIEN_MOTH -> 80;
            case SpriteSheets.ALIEN_EYE -> 100;
            case SpriteSheets.ALIEN_JELLY -> 120;
            case SpriteSheets.ALIEN_LEAF -> 90;
            default -> 150;
        };
    }

    void updateAliens(double tpf) {
        Iterator<Alien> it = aliens.iterator();
        while (it.hasNext()) {
            Alien a = it.next();
            boolean remove = a.hp <= 0;
            if (!remove) {
                if (a.delay > 0) {
                    a.delay -= tpf;
                    if (a.delay <= 0) {
                        a.view.setVisible(true);
                    }
                    continue;
                }
                if (a.boss) {
                    updateBoss(a, tpf);
                } else {
                    remove = followPath(a, tpf);
                    alienShoot(a, tpf);
                }
                a.view.setTranslateX(a.x - a.size / 2);
                a.view.setTranslateY(a.y - a.size / 2);
                a.hitFlash -= tpf;
                a.view.setOpacity(a.hitFlash > 0 ? 0.55 : 1.0);
                double dx = a.x - playerX();
                double dy = a.y - playerY();
                double r = a.size * 0.38 + PLAYER_RADIUS;
                if (dx * dx + dy * dy < r * r) {
                    hitPlayer();
                    if (!a.boss) {
                        a.hp = 0;
                        host.spawnExplosion(a.x, a.y, "ship");
                        remove = true;
                    }
                }
            }
            if (remove) {
                layer.getChildren().remove(a.view);
                it.remove();
            }
        }
    }

    // ---------- bosses ----------

    private static final int[] MINI_ROTATION = {
            SpriteSheets.ALIEN_JELLY, SpriteSheets.ALIEN_EYE, SpriteSheets.ALIEN_MOTH,
            SpriteSheets.ALIEN_CRAB, SpriteSheets.ALIEN_LEAF};
    private static final String[] ALIEN_NAMES = {"CRAB", "MOTH", "EYE", "JELLY", "LEAF", "BRAIN"};

    Alien activeBoss;
    private double nextBossAt = 60;
    private int miniCount;
    int minibossKills;
    int brainKills;
    private Rectangle bossBarBg;
    private Rectangle bossBarFill;
    private Text bossBarText;

    private void maybeSpawnBoss() {
        if (activeBoss != null || time < nextBossAt) {
            return;
        }
        boolean brain = ((int) Math.round(nextBossAt)) % 300 == 0;
        nextBossAt += 60;
        Alien b;
        if (brain) {
            int round = brainKills + 1;
            b = newAlien(SpriteSheets.ALIEN_BRAIN, 190, 200 + 80 * brainKills);
            b.big = true;
            host.displayBanner("BRAIN OCTOPUS " + (round > 1 ? round : "") + "!", Color.MEDIUMORCHID, 2.0);
        } else {
            int kind = MINI_ROTATION[miniCount % MINI_ROTATION.length];
            miniCount++;
            b = newAlien(kind, 140, 28 + 12 * miniCount);
            host.displayBanner("GIANT " + ALIEN_NAMES[kind] + "!", Color.ORANGE, 1.6);
        }
        b.boss = true;
        b.x = getAppWidth() / 2.0;
        b.homeX = b.x;
        b.y = -b.size / 2;
        b.attackCd = 2.0;
        b.specialCd = 4.0;
        b.helperCd = 5.0;
        activeBoss = b;
        SoundHelper.play("ufo_appear.wav");
        showBossBar(true);
    }

    void updateBoss(Alien a, double tpf) {
        a.bossT += tpf;
        double targetY = host.playTop() + 40 + a.size / 2;
        if (a.y < targetY) {
            a.y = Math.min(targetY, a.y + 70 * tpf);
            return;
        }
        double range = Math.max(40, (host.playRight() - host.playLeft()) / 2 - a.size / 2 - 10);
        double swaySpeed = a.kind == SpriteSheets.ALIEN_MOTH ? 1.3 : 0.6;
        if (a.phase >= 2) {
            swaySpeed *= 1.4;
        }
        a.x = a.homeX + Math.sin(a.bossT * swaySpeed) * range;
        if (a.kind == SpriteSheets.ALIEN_JELLY) {
            a.y = targetY + Math.sin(a.bossT * 1.7) * 18;
        }

        double hpPart = a.hp / (double) a.maxHp;
        if (a.big) {
            int want = hpPart < 0.25 ? 2 : (hpPart < 0.6 ? 1 : 0);
            if (want > a.phase) {
                a.phase = want;
                if (want == 1) {
                    host.displayBanner("BRAIN CALLS FOR HELP!", Color.MEDIUMORCHID, 1.4);
                } else {
                    host.displayBanner("BRAIN GETS ANGRY!", Color.RED, 1.4);
                    a.view.setEffect(new javafx.scene.effect.Lighting(
                            new javafx.scene.effect.Light.Distant(45, 60, Color.rgb(255, 110, 110))));
                }
            }
        }

        double bottom = a.y + a.size * 0.4;
        a.attackCd -= tpf;
        a.specialCd -= tpf;
        if (a.attackCd <= 0) {
            switch (a.kind) {
                case SpriteSheets.ALIEN_JELLY -> { fan(a.x, bottom, 5, 0.9, 170, Color.HOTPINK); a.attackCd = 2.2; }
                case SpriteSheets.ALIEN_EYE -> { aimedShot(a.x - 14, bottom, 230, Color.MEDIUMPURPLE); aimedShot(a.x, bottom, 230, Color.MEDIUMPURPLE); aimedShot(a.x + 14, bottom, 230, Color.MEDIUMPURPLE); a.attackCd = 1.7; }
                case SpriteSheets.ALIEN_MOTH -> { for (int i = 0; i < 3; i++) { enemyBullet(a.x + (rng.nextDouble() - 0.5) * a.size, bottom, (rng.nextDouble() - 0.5) * 60, 210, Color.GOLD); } a.attackCd = 1.2; }
                case SpriteSheets.ALIEN_CRAB -> { fan(a.x, bottom, 7, 1.2, 140, Color.TURQUOISE); a.attackCd = 2.6; }
                case SpriteSheets.ALIEN_LEAF -> { ring(a.x, a.y, 10, 150, Color.LIMEGREEN); a.attackCd = 3.0; }
                default -> { // brain
                    fan(a.x, bottom, a.phase >= 2 ? 7 : 5, 1.0, 170, Color.ORCHID);
                    aimedShot(a.x, bottom, 220, Color.VIOLET);
                    a.attackCd = a.phase >= 2 ? 1.6 : 2.2;
                }
            }
        }
        if (a.specialCd <= 0) {
            if (a.kind == SpriteSheets.ALIEN_EYE) {
                ring(a.x, a.y, 12, 160, Color.MEDIUMPURPLE);
                a.specialCd = 5.0;
            } else if (a.big && a.phase >= 2) {
                ring(a.x, a.y, 14, 170, Color.RED);
                a.specialCd = 3.0;
            } else {
                a.specialCd = 4.0;
            }
        }
        if (a.big && a.phase >= 1) {
            a.helperCd -= tpf;
            if (a.helperCd <= 0) {
                a.helperCd = 6.0;
                List<Point2D> path = new ArrayList<>();
                path.add(new Point2D(a.x, a.y));
                path.add(new Point2D(a.x + (rng.nextBoolean() ? 160 : -160), a.y + 120));
                path.add(new Point2D(playerX(), getAppHeight() + 80));
                spawnGroupOnPath(0, path, 3, 200, true);
            }
        }
        updateBossBar(a);
    }

    void fan(double x, double y, int n, double spread, double speed, Color c) {
        for (int i = 0; i < n; i++) {
            double ang = Math.PI / 2 + (n == 1 ? 0 : (i / (double) (n - 1) - 0.5) * spread);
            enemyBullet(x, y, Math.cos(ang) * speed, Math.sin(ang) * speed, c);
        }
    }

    void ring(double x, double y, int n, double speed, Color c) {
        double off = rng.nextDouble() * Math.PI;
        for (int i = 0; i < n; i++) {
            double ang = off + i * Math.PI * 2 / n;
            enemyBullet(x, y, Math.cos(ang) * speed, Math.sin(ang) * speed, c);
        }
    }

    private void showBossBar(boolean show) {
        if (bossBarBg == null) {
            double w = 260;
            double x = getAppWidth() / 2.0 - w / 2;
            double y = host.playTop() + 34;
            bossBarBg = new Rectangle(x, y, w, 10);
            bossBarBg.setFill(Color.rgb(20, 10, 30, 0.85));
            bossBarBg.setStroke(Color.ORCHID);
            bossBarFill = new Rectangle(x + 1, y + 1, w - 2, 8);
            bossBarFill.setFill(Color.MEDIUMORCHID);
            bossBarText = new Text(x, y - 4, "");
            bossBarText.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
            bossBarText.setFill(Color.WHITE);
        }
        layer.getChildren().removeAll(bossBarBg, bossBarFill, bossBarText);
        if (show) {
            layer.getChildren().addAll(bossBarBg, bossBarFill, bossBarText);
        }
    }

    private void updateBossBar(Alien a) {
        bossBarFill.setWidth(Math.max(0, (bossBarBg.getWidth() - 2) * a.hp / (double) a.maxHp));
        bossBarText.setText(a.big ? "BRAIN OCTOPUS" : "GIANT " + ALIEN_NAMES[a.kind]);
        bossBarBg.toFront();
        bossBarFill.toFront();
        bossBarText.toFront();
    }

    void onBossKilled(Alien a) {
        activeBoss = null;
        showBossBar(false);
        if (a.big) {
            brainKills++;
            killScore += 15000;
            host.displayBanner("BRAIN OCTOPUS DOWN!  +15000", Color.GOLD, 2.0);
            dropHeart(a.x, a.y);
        } else {
            minibossKills++;
            killScore += 3000;
            host.displayBanner("GIANT " + ALIEN_NAMES[a.kind] + " DOWN!  +3000", Color.GOLD, 1.6);
            if (rng.nextBoolean()) {
                dropHeart(a.x, a.y);
            }
        }
        for (int i = 0; i < 4; i++) {
            host.spawnExplosion(a.x + (rng.nextDouble() - 0.5) * a.size * 0.7, a.y + (rng.nextDouble() - 0.5) * a.size * 0.7, "big");
        }
    }

    /** @return true when the alien has run off the end of its path. */
    private boolean followPath(Alien a, double tpf) {
        double step = a.speed * tpf;
        while (step > 0 && a.idx < a.path.size()) {
            Point2D t = a.path.get(a.idx);
            double dx = t.getX() - a.x;
            double dy = t.getY() - a.y;
            double d = Math.sqrt(dx * dx + dy * dy);
            if (d <= step) {
                a.x = t.getX();
                a.y = t.getY();
                step -= d;
                a.idx++;
            } else {
                a.x += dx / d * step;
                a.y += dy / d * step;
                step = 0;
            }
        }
        return a.idx >= a.path.size();
    }

    double shootRate() {
        return 1.0 + 0.25 * stage();
    }

    private void alienShoot(Alien a, double tpf) {
        a.shootCd -= tpf * shootRate();
        if (a.shootCd > 0) {
            return;
        }
        a.shootCd = 2.5 + rng.nextDouble() * 3.5;
        if (a.y < host.playTop() + 10 || a.y > playerY() - 80) {
            return;
        }
        if (a.kind == SpriteSheets.ALIEN_EYE) {
            aimedShot(a.x, a.y, 210, Color.MEDIUMPURPLE);
        } else {
            enemyBullet(a.x, a.y, 0, 190, Color.rgb(255, 180, 90));
        }
    }

    void aimedShot(double x, double y, double speed, Color c) {
        double dx = playerX() - x;
        double dy = playerY() - y;
        double d = Math.max(1, Math.sqrt(dx * dx + dy * dy));
        enemyBullet(x, y, dx / d * speed, dy / d * speed, c);
    }

    void enemyBullet(double x, double y, double vx, double vy, Color c) {
        Bullet b = new Bullet();
        b.x = x;
        b.y = y;
        b.vx = vx;
        b.vy = vy;
        b.node = new Circle(5, c);
        b.node.setEffect(new DropShadow(8, c));
        layer.getChildren().add(b.node);
        bullets.add(b);
    }

    private void updateBullets(double tpf) {
        Iterator<Bullet> it = bullets.iterator();
        double w = getAppWidth();
        double h = getAppHeight();
        while (it.hasNext()) {
            Bullet b = it.next();
            b.x += b.vx * tpf;
            b.y += b.vy * tpf;
            b.node.setTranslateX(b.x);
            b.node.setTranslateY(b.y);
            boolean gone = b.x < -30 || b.x > w + 30 || b.y < -30 || b.y > h + 30;
            double dx = b.x - playerX();
            double dy = b.y - playerY();
            if (!gone && dx * dx + dy * dy < (PLAYER_RADIUS + 4) * (PLAYER_RADIUS + 4)) {
                hitPlayer();
                gone = true;
            }
            if (gone) {
                layer.getChildren().remove(b.node);
                it.remove();
            }
        }
    }

    // ---------- HUD ----------

    String timeLabel() {
        int s = (int) time;
        return String.format("%d:%02d", s / 60, s % 60);
    }

    void refreshHud() {
        hudText.setText("TIME " + timeLabel() + "   LIVES " + "\u2665".repeat(Math.max(0, lives)) + "   KILLS " + kills);
        hudText.toFront();
    }
}
