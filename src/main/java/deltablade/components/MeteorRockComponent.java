package deltablade.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;

/**
 * Componente FXGL de las rocas del desafío de meteoritos, que caen girando.
 */
public class MeteorRockComponent extends Component {

    private final double baseSpeed;
    private final double driftX;
    private final double spin;
    private double speedScale = 1.0;

    /**
     * Crea una roca que cae.
     *
     * @param baseSpeed velocidad de caída base en píxeles por segundo
     * @param driftX desplazamiento horizontal en píxeles por segundo
     * @param spin velocidad de giro en grados por segundo
     */
    public MeteorRockComponent(double baseSpeed, double driftX, double spin) {
        this.baseSpeed = baseSpeed;
        this.driftX = driftX;
        this.spin = spin;
    }

    /**
     * Cambia el multiplicador de la velocidad de caída.
     *
     * @param speedScale factor por el que se multiplica la velocidad base
     */
    public void setSpeedScale(double speedScale) {
        this.speedScale = speedScale;
    }

    @Override
    public void onUpdate(double tpf) {
        entity.translateX(driftX * speedScale * tpf);
        entity.translateY(baseSpeed * speedScale * tpf);
        entity.rotateBy(spin * tpf);

        if (entity.getY() > FXGL.getAppHeight() + 50) {
            entity.removeFromWorld();
        }
    }
}
