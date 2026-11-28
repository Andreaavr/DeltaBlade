package deltablade.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;

/**
 * Componente FXGL de los objetos que caen y el jugador puede recoger.
 */
public class PickupComponent extends Component {

    /**
     * Tipos de objeto que se pueden recoger.
     */
    public enum PickupType {
        WEAPON_UPGRADE,
        EXTRA_AMMO,
        EXTRA_LIFE,
        AUTOFIRE,
        METEOR,
        COGNITIVE,
        MAZE,
        ALIEN,
        EXTRA_TIME,
        SHIELD,
        SCOOP,
        MISSILE,
        SKULL
    }

    private PickupType type;
    private double speed = 80;
    private double pulsePhase = 0;

    /**
     * Crea un objeto que cae a la velocidad por defecto.
     *
     * @param type tipo de objeto
     */
    public PickupComponent(PickupType type) {
        this(type, 80);
    }

    /**
     * Crea un objeto que cae a la velocidad indicada.
     *
     * @param type tipo de objeto
     * @param speed velocidad de caída en píxeles por segundo
     */
    public PickupComponent(PickupType type, double speed) {
        this.type = type;
        this.speed = speed;
    }

    @Override
    public void onUpdate(double tpf) {
        entity.translateY(speed * tpf);

        // Escudo/misil: pulso suave del extra; los demás objetos, algo más marcado.
        boolean softExtra = type == PickupType.SHIELD || type == PickupType.MISSILE;
        if (softExtra) {
            pulsePhase += tpf * 3.0;
            double wave = 0.5 + 0.5 * Math.sin(pulsePhase);
            double scale = 1.0 + wave * 0.10;
            entity.setScaleX(scale);
            entity.setScaleY(scale);
            entity.getViewComponent().setOpacity(0.78 + wave * 0.22);
        } else {
            pulsePhase += tpf * 8;
            double scale = 1 + Math.sin(pulsePhase) * 0.15;
            entity.setScaleX(scale);
            entity.setScaleY(scale);
        }

        if (entity.getY() > FXGL.getAppHeight()) {
            entity.removeFromWorld();
        }
    }

    /**
     * Devuelve el tipo de objeto.
     *
     * @return tipo de objeto
     */
    public PickupType getType() {
        return type;
    }
}
