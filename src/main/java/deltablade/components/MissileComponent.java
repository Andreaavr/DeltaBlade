package deltablade.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import deltablade.DeltaBladeApp;
import deltablade.GameVars;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;

public class MissileComponent extends Component {

    /** Sprite nose sits this many degrees clockwise of "up"; rotate the art back so it flies nose-first. */
    public static final double ART_OFFSET_DEG = -51;

    private static final double MAX_TPF = 1.0 / 30.0;

    private double heading;
    private double speed;
    private boolean steering;
    private boolean turnLeft;
    private boolean turnRight;
    private boolean detonated;
    private double trailAcc;
    private double ribbonPhase;

    public MissileComponent() {
        this.heading = 0;
        this.speed = GameVars.MISSILE_SPEED;
    }

    public void setSteering(boolean steering) {
        this.steering = steering;
    }

    public void setTurnInput(boolean left, boolean right) {
        this.turnLeft = left;
        this.turnRight = right;
    }

    public boolean isDetonated() {
        return detonated;
    }

    @Override
    public void onAdded() {
        if (entity != null) {
            entity.getTransformComponent().setRotationOrigin(
                    new Point2D(entity.getWidth() / 2.0, entity.getHeight() / 2.0));
            entity.setRotation(heading);
        }
    }

    @Override
    public void onUpdate(double tpf) {
        tpf = Math.min(tpf, MAX_TPF);
        if (detonated || entity == null || !entity.isActive()) {
            return;
        }
        if (FXGL.getApp() instanceof DeltaBladeApp app) {
            steering = app.holdingFire();
            turnLeft = app.movingLeft();
            turnRight = app.movingRight();
        }

        if (steering) {
            if (turnLeft && !turnRight) {
                heading -= GameVars.MISSILE_TURN_RATE * tpf;
            } else if (turnRight && !turnLeft) {
                heading += GameVars.MISSILE_TURN_RATE * tpf;
            }
        }

        double rad = Math.toRadians(heading);
        double vx = Math.sin(rad) * speed;
        double vy = -Math.cos(rad) * speed;
        entity.translate(vx * tpf, vy * tpf);
        entity.setRotation(heading);

        spawnTrail(tpf, vx, vy);

        if (outOfBounds()) {
            requestDetonate();
        }
    }

    public void requestDetonate() {
        if (detonated || entity == null || !entity.isActive()) {
            return;
        }
        detonated = true;
        if (FXGL.getApp() instanceof DeltaBladeApp app) {
            app.detonatePlayerMissile(entity);
        } else if (entity.isActive()) {
            entity.removeFromWorld();
        }
    }

    private boolean outOfBounds() {
        double x = entity.getX() + entity.getWidth() / 2;
        double y = entity.getY() + entity.getHeight() / 2;
        return x < -24 || x > FXGL.getAppWidth() + 24
                || y < -24 || y > FXGL.getAppHeight() + 24;
    }

    private void spawnTrail(double tpf, double vx, double vy) {
        trailAcc += tpf;
        ribbonPhase += tpf * 22;
        double interval = 0.016;
        while (trailAcc >= interval) {
            trailAcc -= interval;
            double cx = entity.getX() + entity.getWidth() / 2;
            double cy = entity.getY() + entity.getHeight() / 2;
            double n = Math.hypot(vx, vy);
            double bx = n > 1 ? -vx / n : 0;
            double by = n > 1 ? -vy / n : 1;
            double jitter = Math.sin(ribbonPhase) * 3.2;
            double px = cx + bx * 16 + -by * jitter;
            double py = cy + by * 16 + bx * jitter;

            Ellipse core = new Ellipse(2.4, 7.5);
            core.setFill(Color.rgb(255, 230, 140, 0.95));
            core.setRotate(heading);
            FXGL.entityBuilder()
                    .at(px - 2.4, py - 4)
                    .view(core)
                    .zIndex(72)
                    .with(new FadePuffComponent(0.28, bx * 18, by * 18))
                    .buildAndAttach();

            Circle smoke = new Circle(5.5, new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.rgb(255, 120, 40, 0.7)),
                    new Stop(0.55, Color.rgb(220, 50, 20, 0.35)),
                    new Stop(1, Color.rgb(80, 20, 10, 0))));
            FXGL.entityBuilder()
                    .at(px + bx * 6 - 5.5, py + by * 6 - 5.5)
                    .view(new Group(smoke))
                    .zIndex(71)
                    .with(new FadePuffComponent(0.48, bx * 28, by * 28))
                    .buildAndAttach();
        }
    }
}
