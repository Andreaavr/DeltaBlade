package deltablade.minigames;

import com.almasb.fxgl.entity.Entity;
import deltablade.EntityType;
import deltablade.GameVars;
import deltablade.components.MeteorRockComponent;
import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.almasb.fxgl.dsl.FXGL.getGameWorld;
import static com.almasb.fxgl.dsl.FXGL.inc;
import static com.almasb.fxgl.dsl.FXGL.spawn;

public class MeteorStormGame implements Minigame {

    private static final double GOAL_DISTANCE = 640;
    private static final Random RANDOM = new Random();

    private MinigameHost host;
    private boolean active;
    private boolean finished;
    private double distance;
    private double spawnTimer;
    private double thrustSum;
    private double thrustTime;
    private double holdTime;
    private final List<NodeHud> hud = new ArrayList<>();
    private Rectangle distanceFill;
    private Text speedText;
    private Text holdText;

    private record NodeHud(javafx.scene.Node node) {}

    @Override
    public void start(MinigameHost host) {
        this.host = host;
        this.active = true;
        this.finished = false;
        this.distance = 0;
        this.spawnTimer = 0.4;
        this.thrustSum = 0;
        this.thrustTime = 0;
        this.holdTime = 0;
        host.hidePlayer(false);
        buildHud();
        host.displayBanner("METEOR STORM", Color.ORANGERED, 1.6);
    }

    @Override
    public boolean usesPlayerShip() {
        return true;
    }

    @Override
    public void update(double tpf) {
        if (!active || finished) {
            return;
        }

        double thrust = host.holdingFire() ? 2.2 : 0.9;
        thrustSum += thrust * tpf;
        thrustTime += tpf;
        if (host.holdingFire()) {
            holdTime += tpf;
        }
        distance += 14 * thrust * tpf;

        double progress = Math.min(1.0, distance / GOAL_DISTANCE);
        double rush = endRush(progress);
        if (distanceFill != null) {
            distanceFill.setHeight(Math.max(4, 220 * progress));
            distanceFill.setTranslateY(hudBarBottom() - distanceFill.getHeight());
        }
        if (speedText != null) {
            speedText.setText(host.holdingFire() ? "THRUST" : "GLIDE");
            speedText.setFill(host.holdingFire() ? Color.ORANGE : Color.rgb(160, 180, 200));
        }
        updateHoldHud();

        for (Entity rock : getGameWorld().getEntitiesByType(EntityType.MINIGAME_HAZARD)) {
            if (rock.isActive() && rock.hasComponent(MeteorRockComponent.class)) {
                rock.getComponent(MeteorRockComponent.class).setSpeedScale(thrust * rush);
            }
        }

        spawnTimer -= tpf;
        if (spawnTimer <= 0) {
            spawnWave(progress);
            spawnTimer = Math.max(0.12, (0.70 - progress * 0.34) / rush);
        }

        if (playerHitRock()) {
            fail();
            return;
        }

        if (distance >= GOAL_DISTANCE) {
            win();
        }
    }

    @Override
    public void cleanup() {
        active = false;
        clearStormLoot();
        for (NodeHud item : hud) {
            host.removeUi(item.node());
        }
        hud.clear();
    }

    private void spawnWave(double progress) {
        int count = 1
                + (progress > 0.28 ? 1 : 0)
                + (progress > 0.68 ? 1 : 0)
                + (RANDOM.nextDouble() < progress * 0.5 ? 1 : 0);
        double left = host.playLeft() + 8;
        double right = host.playRight() - 40;

        for (int i = 0; i < count; i++) {
            double size = 30 + RANDOM.nextDouble() * 34;
            double x = left + RANDOM.nextDouble() * Math.max(20, right - left);
            double y = -size - RANDOM.nextDouble() * 40;
            double baseSpeed = 88 + progress * 70 + RANDOM.nextDouble() * 40;
            spawn("meteorRock", new com.almasb.fxgl.entity.SpawnData(x, y)
                    .put("size", size)
                    .put("baseSpeed", baseSpeed)
                    .put("driftX", (RANDOM.nextDouble() - 0.5) * 48)
                    .put("spin", (RANDOM.nextDouble() - 0.5) * 180)
                    .put("variant", RANDOM.nextInt(4)));
        }

        if (RANDOM.nextDouble() < 0.22 + progress * 0.1) {
            String type = RANDOM.nextDouble() < 0.7 ? "white" : (RANDOM.nextDouble() < 0.7 ? "green" : "blue");
            double cx = left + 16 + RANDOM.nextDouble() * Math.max(20, right - left - 16);
            host.spawnCoinAt(cx, -12, type);
        }
    }

    private static double endRush(double progress) {
        if (progress < 0.62) {
            return 1.0;
        }
        double t = (progress - 0.62) / 0.38;
        return 1.0 + t * t * 1.2;
    }

    private void clearStormLoot() {
        for (EntityType type : List.of(EntityType.MINIGAME_HAZARD, EntityType.COIN)) {
            for (Entity entity : List.copyOf(getGameWorld().getEntitiesByType(type))) {
                if (entity.isActive()) {
                    entity.removeFromWorld();
                }
            }
        }
    }

    private boolean playerHitRock() {
        Entity player = host.player();
        if (player == null || !player.isActive()) {
            return false;
        }
        double px = player.getX() + 8;
        double py = player.getY() + 8;
        double pw = Math.max(8, player.getWidth() - 16);
        double ph = Math.max(8, player.getHeight() - 16);

        for (Entity rock : getGameWorld().getEntitiesByType(EntityType.MINIGAME_HAZARD)) {
            if (!rock.isActive()) {
                continue;
            }
            double inset = Math.min(10, rock.getWidth() * 0.18);
            if (rectsOverlap(px, py, pw, ph,
                    rock.getX() + inset, rock.getY() + inset,
                    rock.getWidth() - inset * 2, rock.getHeight() - inset * 2)) {
                host.spawnExplosion(player.getX() + player.getWidth() / 2, player.getY() + player.getHeight() / 2, "ship");
                return true;
            }
        }
        return false;
    }

    private static boolean rectsOverlap(double ax, double ay, double aw, double ah,
                                        double bx, double by, double bw, double bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }

    private void win() {
        finished = true;
        clearStormLoot();
        double avg = thrustTime > 0 ? thrustSum / thrustTime : 0.9;
        int score = 600 + (int) (avg * 220);
        int money = 40 + (int) (avg * 28);
        String banner = "STORM CLEAR  +" + score;
        if (holdRatio() >= 0.95) {
            score += 350;
            money += 30;
            banner = "BONUS  +" + score;
        }
        inc(GameVars.METEOR_WINS, 1);
        host.finishMinigame(true, score, money, banner, Color.ORANGE);
    }

    private void fail() {
        finished = true;
        clearStormLoot();
        int score = 80;
        host.finishMinigame(false, score, 10, "CRASH  +" + score, Color.ORANGERED);
    }

    private void buildHud() {
        double x = host.playRight() - 22;
        double top = 70;
        double h = 220;

        Rectangle bg = new Rectangle(12, h);
        bg.setFill(Color.rgb(10, 16, 28, 0.85));
        bg.setStroke(Color.rgb(255, 140, 40, 0.7));
        bg.setTranslateX(x);
        bg.setTranslateY(top);

        distanceFill = new Rectangle(8, 4);
        distanceFill.setFill(new LinearGradient(0, 1, 0, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.ORANGERED),
                new Stop(1, Color.GOLD)));
        distanceFill.setTranslateX(x + 2);
        distanceFill.setTranslateY(top + h - 4);

        Text caption = new Text("DIST");
        caption.setFont(Font.font("Monospace", FontWeight.BOLD, 9));
        caption.setFill(Color.rgb(255, 180, 80));
        caption.setTranslateX(x - 6);
        caption.setTranslateY(top - 6);

        speedText = new Text("GLIDE");
        speedText.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
        speedText.setFill(Color.rgb(160, 180, 200));
        speedText.setEffect(new DropShadow(6, Color.BLACK));
        speedText.setTranslateX(host.playLeft() + 12);
        speedText.setTranslateY(host.playBottom() - 18);

        holdText = new Text("HOLD  0%");
        holdText.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
        holdText.setFill(Color.rgb(160, 180, 200));
        holdText.setEffect(new DropShadow(6, Color.BLACK));
        holdText.setTranslateX(host.playLeft() + 12);
        holdText.setTranslateY(host.playBottom() - 34);

        Group group = new Group(bg, distanceFill, caption, speedText, holdText);
        host.addUi(group);
        hud.add(new NodeHud(group));
    }

    private double hudBarBottom() {
        return 70 + 220;
    }

    private double holdRatio() {
        return thrustTime > 0 ? holdTime / thrustTime : 0;
    }

    private void updateHoldHud() {
        if (holdText == null) {
            return;
        }
        double ratio = holdRatio();
        int pct = (int) Math.floor(ratio * 100 + 1e-6);
        if (ratio >= 0.95) {
            holdText.setText("BONUS  " + pct + "%");
            holdText.setFill(Color.GOLD);
        } else {
            holdText.setText("HOLD  " + pct + "%");
            holdText.setFill(pct >= 80 ? Color.ORANGE : Color.rgb(160, 180, 200));
        }
    }
}
