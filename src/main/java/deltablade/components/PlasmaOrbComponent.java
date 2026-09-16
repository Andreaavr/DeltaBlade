package deltablade.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import deltablade.GameVars;

/**
 * Slow heavy plasma projectile for the shop Plasma Orb weapon.
 */
public class PlasmaOrbComponent extends Component {

    private final double speedX;
    private final double speedY;
    private int pierceLeft;
    private final int coreDamage;
    private final int splashDamage;
    private final double splashRadius;
    private boolean spentAmmo;

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

    public int getCoreDamage() {
        return coreDamage;
    }

    public int getSplashDamage() {
        return splashDamage;
    }

    public double getSplashRadius() {
        return splashRadius;
    }

    public boolean consumePierce() {
        if (pierceLeft <= 0) {
            return false;
        }
        pierceLeft--;
        return true;
    }

    public boolean hasPierceLeft() {
        return pierceLeft > 0;
    }

    /** @return true if the orb should be removed after this hit */
    public boolean hitAndShouldRemove() {
        if (pierceLeft <= 0) {
            return true;
        }
        pierceLeft--;
        return false;
    }
}
