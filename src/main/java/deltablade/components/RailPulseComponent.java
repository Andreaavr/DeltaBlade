package deltablade.components;

import com.almasb.fxgl.entity.component.Component;
import javafx.scene.Node;

/**
 * Pulso lento de opacidad para la franja de energía interior de una barra lateral.
 */
public class RailPulseComponent extends Component {

    private final Node energy;
    private double time;

    /**
     * Crea el pulso de energía de un raíl lateral.
     *
     * @param energy nodo que brilla con el pulso
     */
    public RailPulseComponent(Node energy) {
        this.energy = energy;
    }

    @Override
    public void onUpdate(double tpf) {
        time += tpf;
        energy.setOpacity(0.55 + 0.35 * Math.sin(time * 2.4));
    }
}
