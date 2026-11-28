package deltablade.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import deltablade.GameVars;

/**
 * Proyectil de plasma lento y pesado del arma Plasma Orb de la tienda.
 */
public class PlasmaOrbComponent extends Component {

    private final double speedX;
    private final double speedY;
    private int pierceLeft;
    private final int coreDamage;
    private final int splashDamage;
    private final double splashRadius;
    private boolean spentAmmo;

    /**
     * Crea un orbe de plasma.
     *
     * @param speedX velocidad horizontal en píxeles por segundo
     * @param speedY velocidad vertical en píxeles por segundo
     * @param pierceLeft número de enemigos que puede atravesar
     * @param coreDamage daño al enemigo que golpea directamente
     * @param splashDamage daño a los enemigos cercanos
     * @param splashRadius radio del daño en área, en píxeles
     */
    public PlasmaOrbComponent(double speedX, double speedY, int pierceLeft,
                              int coreDamage, int splashDamage, double splashRadius) {
        this.speedX = speedX;
        this.speedY = speedY;
        this.pierceLeft = pierceLeft;
        this.coreDamage = coreDamage;
        this.splashDamage = splashDamage;
        this.splashRadius = splashRadius;
    }

    @Override
    public void onUpdate(double tpf) {
        entity.translateX(speedX * tpf);
        entity.translateY(speedY * tpf);
        if (entity.getY() + entity.getHeight() < -40
                || entity.getY() > FXGL.getAppHeight() + 40
                || entity.getX() < -80
                || entity.getX() > FXGL.getAppWidth() + 80) {
            releaseAmmoSlots();
            entity.removeFromWorld();
        }
    }

    /**
     * Libera los dos huecos de munición que ocupa el orbe. Solo tiene efecto la primera vez.
     */
    public void releaseAmmoSlots() {
        if (spentAmmo) {
            return;
        }
        spentAmmo = true;
        FXGL.inc(GameVars.ACTIVE_BULLETS, -2);
        if (FXGL.geti(GameVars.ACTIVE_BULLETS) < 0) {
            FXGL.set(GameVars.ACTIVE_BULLETS, 0);
        }
    }

    /**
     * Devuelve el daño al enemigo que golpea directamente.
     *
     * @return daño directo
     */
    public int getCoreDamage() {
        return coreDamage;
    }

    /**
     * Devuelve el daño a los enemigos cercanos.
     *
     * @return daño en área
     */
    public int getSplashDamage() {
        return splashDamage;
    }

    /**
     * Devuelve el radio del daño en área.
     *
     * @return radio en píxeles
     */
    public double getSplashRadius() {
        return splashRadius;
    }

    /**
     * Gasta un uso de atravesar enemigos.
     *
     * @return {@code true} si le quedaban usos
     */
    public boolean consumePierce() {
        if (pierceLeft <= 0) {
            return false;
        }
        pierceLeft--;
        return true;
    }

    /**
     * Indica si el orbe puede atravesar más enemigos.
     *
     * @return {@code true} si le quedan usos
     */
    public boolean hasPierceLeft() {
        return pierceLeft > 0;
    }

    /** @return true si el orbe debe eliminarse después de este impacto */
    public boolean hitAndShouldRemove() {
        if (pierceLeft <= 0) {
            return true;
        }
        pierceLeft--;
        return false;
    }
}
