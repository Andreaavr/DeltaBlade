package deltablade.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import deltablade.EntityType;
import deltablade.GameVars;

/**
 * Componente FXGL de un disparo (del jugador o enemigo): movimiento, seguimiento al jugador y salida de pantalla.
 */
public class BulletComponent extends Component {

    private double speedX;
    private double speedY;
    private final boolean isPlayerBullet;
    private final boolean homing;
    private final double turnRate;
    private final double speed;
    private double acquireDelay;
    private double homingLeft;
    private String shotKind = "bolt";
    private double wobble;

    /** Tiempo de espera antes de que el misil empiece a girar. */
    private static final double HOMING_ACQUIRE = 0.10;
    /** Cuánto tiempo puede dirigirse después de fijar el blanco. Después vuela recto. */
    private static final double HOMING_DURATION = 1.45;
    /** Deja de dirigirse en el último tramo, por encima del jugador. */
    private static final double HOMING_CUTOFF_Y_RATIO = 0.84;

    /**
     * Crea un disparo vertical.
     *
     * @param speedY velocidad vertical en píxeles por segundo
     * @param isPlayerBullet {@code true} si lo dispara el jugador
     */
    public BulletComponent(double speedY, boolean isPlayerBullet) {
        this(0, speedY, isPlayerBullet, false);
    }

    /**
     * Crea un disparo en cualquier dirección.
     *
     * @param speedX velocidad horizontal en píxeles por segundo
     * @param speedY velocidad vertical en píxeles por segundo
     * @param isPlayerBullet {@code true} si lo dispara el jugador
     */
    public BulletComponent(double speedX, double speedY, boolean isPlayerBullet) {
        this(speedX, speedY, isPlayerBullet, false);
    }

    /**
     * Crea un disparo que puede seguir al jugador.
     *
     * @param speedX velocidad horizontal en píxeles por segundo
     * @param speedY velocidad vertical en píxeles por segundo
     * @param isPlayerBullet {@code true} si lo dispara el jugador
     * @param homing {@code true} si persigue al jugador
     */
    public BulletComponent(double speedX, double speedY, boolean isPlayerBullet, boolean homing) {
        this.speedX = speedX;
        this.speedY = speedY;
        this.isPlayerBullet = isPlayerBullet;
        this.homing = homing;
        this.turnRate = 0.82;
        this.speed = Math.hypot(speedX, speedY);
        this.acquireDelay = homing ? HOMING_ACQUIRE : 0;
        this.homingLeft = homing ? HOMING_DURATION : 0;
    }

    /**
     * Cambia el aspecto del disparo enemigo.
     *
     * @param shotKind tipo de disparo (por ejemplo "bolt", "blob", "drip" o "flame"); {@code null} equivale a "bolt"
     */
    public void setShotKind(String shotKind) {
        this.shotKind = shotKind != null ? shotKind : "bolt";
    }

    private static final double MAX_TPF = 1.0 / 30.0;

    @Override
    public void onUpdate(double tpf) {
        tpf = Math.min(tpf, MAX_TPF);

        if (homing && !isPlayerBullet) {
            if (acquireDelay > 0) {
                acquireDelay -= tpf;
            } else if (homingLeft > 0) {
                if (entity.getY() >= FXGL.getAppHeight() * HOMING_CUTOFF_Y_RATIO) {
                    homingLeft = 0;
                } else {
                    homingLeft -= tpf;
                    steerTowardPlayer(tpf);
                }
            }
        }

        if (!isPlayerBullet && !"bolt".equals(shotKind)) {
            wobble += tpf * ("flame".equals(shotKind) ? 14 : 9);
            entity.translateX(speedX * tpf + Math.sin(wobble) * ("drip".equals(shotKind) ? 18 : 28) * tpf);
        } else {
            entity.translateX(speedX * tpf);
        }
        entity.translateY(speedY * tpf);

        if (homing || Math.abs(speedX) > 8) {
            entity.setRotation(Math.toDegrees(Math.atan2(speedX, -speedY)));
        }

        boolean outOfBounds = entity.getY() < -20
                || entity.getY() > FXGL.getAppHeight() + 20
                || entity.getX() < -20
                || entity.getX() > FXGL.getAppWidth() + 20;

        if (outOfBounds) {
            if (isPlayerBullet) {
                int active = FXGL.geti(GameVars.ACTIVE_BULLETS);
                FXGL.set(GameVars.ACTIVE_BULLETS, Math.max(0, active - 1));
            }
            entity.removeFromWorld();
        }
    }

    private void steerTowardPlayer(double tpf) {
        try {
            var player = FXGL.getGameWorld().getSingleton(EntityType.PLAYER);
            double dx = player.getX() + player.getWidth() / 2 - (entity.getX() + entity.getWidth() / 2);
            double dy = player.getY() + player.getHeight() / 2 - (entity.getY() + entity.getHeight() / 2);
            double dist = Math.hypot(dx, dy);
            if (dist < 4) {
                return;
            }
            dx /= dist;
            dy /= dist;
            speedX += dx * turnRate * tpf * speed;
            speedY += dy * turnRate * tpf * speed;
            double n = Math.hypot(speedX, speedY);
            if (n > 1) {
                speedX = speedX / n * speed;
                speedY = speedY / n * speed;
            }
        } catch (Exception ignored) {
            // Puede que el jugador ya no exista durante el reinicio.
        }
    }

    /**
     * Indica si el disparo es del jugador.
     *
     * @return {@code true} si lo disparó el jugador
     */
    public boolean isPlayerBullet() {
        return isPlayerBullet;
    }

    /**
     * Registra que el disparo ha impactado y, si es del jugador, libera su hueco de munición.
     */
    public void onHit() {
        if (isPlayerBullet) {
            int active = FXGL.geti(GameVars.ACTIVE_BULLETS);
            FXGL.set(GameVars.ACTIVE_BULLETS, Math.max(0, active - 1));
        }
    }
}
