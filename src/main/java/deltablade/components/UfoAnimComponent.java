package deltablade.components;

import com.almasb.fxgl.entity.component.Component;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Componente FXGL que anima el sprite del ovni recorriendo sus fotogramas.
 */
public class UfoAnimComponent extends Component {

    private static final double FRAME_TIME = 0.12;

    private final ImageView view;
    private final Image[] frames;
    private double elapsed;
    private int index;

    /**
     * Crea la animación del ovni.
     *
     * @param view vista en la que se dibuja
     * @param frames fotogramas de la animación
     */
    public UfoAnimComponent(ImageView view, Image[] frames) {
        this.view = view;
        this.frames = frames;
    }

    @Override
    public void onUpdate(double tpf) {
        if (view == null || frames == null || frames.length == 0) {
            return;
        }
        elapsed += tpf;
        if (elapsed >= FRAME_TIME) {
            elapsed = 0;
            index = (index + 1) % frames.length;
            if (frames[index] != null) {
                view.setImage(frames[index]);
            }
        }
    }
}
