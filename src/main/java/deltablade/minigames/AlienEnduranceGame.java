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

    void updateExtras(double tpf) {}

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
        return 1.0;
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

    void updateBoss(Alien a, double tpf) {}

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
