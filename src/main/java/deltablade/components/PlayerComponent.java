package deltablade.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import deltablade.GameVars;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * Componente FXGL de la nave del jugador: movimiento, cadencia de disparo, invulnerabilidad
 * tras recibir un impacto, escudo y cargas del rayo tractor.
 */
public class PlayerComponent extends Component {

    private static final double MAX_TPF = 1.0 / 30.0;
    private static final int SHIP_SIZE = 48;
    private static final int SHIELD_SIZE = 64;
    private double speed = 230;
    private boolean invulnerable = false;
    private double invulnerableTimer = 0;
    private static final double INVULNERABLE_DURATION = 2.0;
    private double shieldTimer = 0;
    private boolean missileShield = false;
    private double shieldPulse = 0;
    private Group shieldView;
    private Circle shieldRing;
    private Circle shieldHalo;
    private int scoopCharges = 0;

    private double fireCooldown = 0;
    private static final double FIRE_RATE = 0.10;

    private boolean wasMovingLeft = false;
    private boolean wasMovingRight = false;
    private double fireAnimTimer = 0;
    private static final double FIRE_ANIM_DURATION = 0.36;

    @Override
    public void onAdded() {
        double radius = SHIELD_SIZE / 2.0;
        // Sin efecto Glow de JavaFX: solo pulso de opacidad/escala (por rendimiento).
        Circle fill = new Circle(radius, Color.rgb(70, 210, 255, 0.22));
        shieldHalo = new Circle(radius + 6);
        shieldHalo.setFill(Color.rgb(60, 200, 255, 0.10));
        shieldHalo.setStroke(Color.rgb(140, 245, 255, 0.55));
        shieldHalo.setStrokeWidth(5.0);
        shieldRing = new Circle(radius);
        shieldRing.setFill(Color.TRANSPARENT);
        shieldRing.setStroke(Color.rgb(200, 255, 255, 0.95));
        shieldRing.setStrokeWidth(2.8);
        shieldView = new Group(fill, shieldHalo, shieldRing);
        shieldView.setTranslateX(SHIP_SIZE / 2.0);
        shieldView.setTranslateY(SHIP_SIZE / 2.0);
        shieldView.setMouseTransparent(true);
        shieldView.setVisible(false);
        entity.getViewComponent().addChild(shieldView);
    }

    @Override
    public void onUpdate(double tpf) {
        tpf = Math.min(tpf, MAX_TPF);

        if (fireCooldown > 0) {
            fireCooldown -= tpf;
        }
        if (fireAnimTimer > 0) {
            fireAnimTimer -= tpf;
        }

        shieldPulse += tpf;
        if (shieldTimer > 0) {
            shieldTimer = Math.max(0, shieldTimer - tpf);
        }
        updateShieldView();

        if (invulnerable) {
            invulnerableTimer -= tpf;
            if (invulnerableTimer <= 0) {
                invulnerable = false;
                entity.getViewComponent().setOpacity(1.0);
            } else {
                double flash = Math.sin(invulnerableTimer * 20) > 0 ? 1.0 : 0.3;
                entity.getViewComponent().setOpacity(flash);
            }
        }
    }

    /**
     * Mueve la nave hacia la izquierda sin salirse del raíl izquierdo.
     *
     * @param tpf tiempo transcurrido desde el fotograma anterior, en segundos
     */
    public void moveLeft(double tpf) {
        double newX = entity.getX() - speed * tpf;
        double minX = GameVars.RAIL_WIDTH;
        if (newX >= minX) {
            entity.setX(newX);
        } else {
            entity.setX(minX);
        }
        wasMovingLeft = true;
        updateAnimationState();
    }

    /**
     * Mueve la nave hacia la derecha sin salirse del raíl derecho.
     *
     * @param tpf tiempo transcurrido desde el fotograma anterior, en segundos
     */
    public void moveRight(double tpf) {
        double newX = entity.getX() + speed * tpf;
        double maxX = FXGL.getAppWidth() - entity.getWidth() - GameVars.RAIL_WIDTH;
        if (newX <= maxX) {
            entity.setX(newX);
        } else {
            entity.setX(maxX);
        }
        wasMovingRight = true;
        updateAnimationState();
    }

    /**
     * Pone la animación de reposo (o de disparo, si acaba de disparar) cuando la nave no se mueve.
     */
    public void updateIdle() {
        if (!wasMovingLeft && !wasMovingRight) {
            if (fireAnimTimer > 0) {
                setAnimationState(PlayerAnimationComponent.AnimationState.FIRING);
            } else {
                setAnimationState(PlayerAnimationComponent.AnimationState.IDLE);
            }
        }
        wasMovingLeft = false;
        wasMovingRight = false;
    }

    private void updateAnimationState() {
        if (wasMovingLeft && !wasMovingRight) {
            setAnimationState(PlayerAnimationComponent.AnimationState.BANKING_LEFT);
        } else if (wasMovingRight && !wasMovingLeft) {
            setAnimationState(PlayerAnimationComponent.AnimationState.BANKING_RIGHT);
        } else {
            setAnimationState(PlayerAnimationComponent.AnimationState.IDLE);
        }
    }

    private void setAnimationState(PlayerAnimationComponent.AnimationState state) {
        if (entity.hasComponent(PlayerAnimationComponent.class)) {
            entity.getComponent(PlayerAnimationComponent.class).setState(state);
        }
    }

    /**
     * Indica si la nave puede disparar ahora: ha pasado el tiempo de recarga y no supera el límite de disparos en pantalla.
     *
     * @param grade número de disparos que salen a la vez
     * @return {@code true} si puede disparar
     */
    public boolean canFire(int grade) {
        if (fireCooldown > 0) return false;

        int activeBullets = FXGL.geti(GameVars.ACTIVE_BULLETS);
        int ammoCap = FXGL.geti(GameVars.AMMO_CAP);
        return activeBullets + grade <= ammoCap;
    }

    /**
     * Registra que la nave acaba de disparar: reinicia la recarga y muestra la animación de disparo.
     */
    public void onFired() {
        fireCooldown = FIRE_RATE;
        fireAnimTimer = FIRE_ANIM_DURATION;
        if (!wasMovingLeft && !wasMovingRight) {
            setAnimationState(PlayerAnimationComponent.AnimationState.FIRING);
        }
    }

    /**
     * Hace la nave invulnerable durante un tiempo, por ejemplo tras perder una vida.
     */
    public void makeInvulnerable() {
        invulnerable = true;
        invulnerableTimer = INVULNERABLE_DURATION;
    }

    /**
     * Indica si la nave no puede recibir daño, por invulnerabilidad temporal o por escudo.
     *
     * @return {@code true} si es invulnerable
     */
    public boolean isInvulnerable() {
        return invulnerable || isShielded();
    }

    /**
     * Añade tiempo de escudo a la nave.
     *
     * @param seconds segundos de escudo que se suman (los negativos se ignoran)
     */
    public void addShield(double seconds) {
        shieldTimer += Math.max(0, seconds);
        updateShieldView();
    }

    /**
     * Indica si la nave tiene algún escudo activo.
     *
     * @return {@code true} si tiene escudo
     */
    public boolean isShielded() {
        return shieldTimer > 0 || missileShield;
    }

    /**
     * Indica si la nave tiene el escudo que dan los misiles.
     *
     * @return {@code true} si tiene el escudo de los misiles
     */
    public boolean isMissileShielded() {
        return missileShield;
    }

    /**
     * Activa o desactiva el escudo que dan los misiles.
     *
     * @param on {@code true} para activarlo
     */
    public void setMissileShield(boolean on) {
        missileShield = on;
        updateShieldView();
    }

    /**
     * Devuelve el tiempo de escudo que queda.
     *
     * @return segundos de escudo restantes
     */
    public double getShieldRemaining() {
        return shieldTimer;
    }

    /**
     * Quita todos los escudos de la nave.
     */
    public void clearShield() {
        shieldTimer = 0;
        missileShield = false;
        updateShieldView();
    }

    /**
     * Equipa la nave con todas las cargas del rayo tractor.
     */
    public void attachScoops() {
        scoopCharges = GameVars.SCOOP_COUNT;
        syncScoopVisual();
    }

    /**
     * Quita todas las cargas del rayo tractor.
     */
    public void clearScoops() {
        scoopCharges = 0;
        syncScoopVisual();
    }

    /**
     * Indica si la nave tiene el rayo tractor equipado.
     *
     * @return {@code true} si le quedan cargas
     */
    public boolean isScoopActive() {
        return scoopCharges > 0;
    }

    /**
     * Devuelve las cargas del rayo tractor que le quedan a la nave.
     *
     * @return número de cargas
     */
    public int getScoopCharges() {
        return scoopCharges;
    }

    private void syncScoopVisual() {
        if (entity != null && entity.hasComponent(PlayerAnimationComponent.class)) {
            entity.getComponent(PlayerAnimationComponent.class).setScooping(scoopCharges > 0);
        }
    }

    private void updateShieldView() {
        if (shieldView == null) {
            return;
        }
        boolean on = isShielded();
        shieldView.setVisible(on);
        if (!on) {
            return;
        }
        double t = shieldPulse;
        double wave = 0.5 + 0.5 * Math.sin(t * 5.5);
        double pulse = 0.78 + 0.22 * wave;
        double scale = 1.0 + 0.06 * wave;
        if (shieldTimer > 0 && shieldTimer <= 3 && !missileShield) {
            // Últimos segundos: parpadeo más rápido para que se note que se acaba.
            pulse = 0.45 + 0.55 * (0.5 + 0.5 * Math.sin(shieldTimer * 14));
            scale = 1.0 + 0.10 * (0.5 + 0.5 * Math.sin(shieldTimer * 14));
        }
        shieldView.setOpacity(pulse);
        shieldView.setScaleX(scale);
        shieldView.setScaleY(scale);
        if (shieldHalo != null) {
            shieldHalo.setOpacity(0.65 + 0.35 * wave);
        }
        if (shieldRing != null) {
            shieldRing.setOpacity(0.85 + 0.15 * wave);
        }
    }

    /** Destello breve cuando el escudo absorbe un impacto. */
    public void pingShieldHit() {
        if (!isShielded() || shieldView == null) {
            return;
        }
        shieldView.setOpacity(1.0);
        shieldView.setScaleX(1.18);
        shieldView.setScaleY(1.18);
    }

    /**
     * Devuelve la coordenada X del centro de la nave.
     *
     * @return X del centro, en píxeles
     */
    public double getCenterX() {
        return entity.getX() + entity.getWidth() / 2;
    }

    /**
     * Devuelve la coordenada Y del borde superior de la nave.
     *
     * @return Y del borde superior, en píxeles
     */
    public double getTopY() {
        return entity.getY();
    }
}
