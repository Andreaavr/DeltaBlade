package deltablade.components;

import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Componente que reproduce una animación de explosión a partir de una hoja de sprites.
 * Admite tanto tiras horizontales (1 fila) como rejillas (varias filas).
 * En las rejillas, los fotogramas se leen por filas: de izquierda a derecha y de arriba abajo.
 * Después del último fotograma, la entidad se elimina del mundo.
 */
public class ExplosionComponent extends Component {

    private final ImageView imageView;
    private final int frameCount;
    private final int frameWidth;
    private final int frameHeight;
    private final int columns;
    private final double frameDuration;

    private double elapsed = 0;
    private int currentFrame = 0;
    private boolean finished = false;

    /**
     * Crea un componente de animación de explosión.
     *
     * @param spriteSheet imagen de la hoja de sprites (tira o rejilla)
     * @param frameCount  número total de fotogramas de la hoja
     * @param frameWidth  ancho de cada fotograma en píxeles
     * @param frameHeight alto de cada fotograma en píxeles
     * @param columns     número de columnas de la rejilla (en las tiras, es igual a frameCount)
     * @param totalDuration duración total de la animación en segundos
     */
    public ExplosionComponent(Image spriteSheet, int frameCount, int frameWidth, int frameHeight, int columns, double totalDuration) {
        this.frameCount = frameCount;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.columns = columns;
        this.frameDuration = totalDuration / frameCount;

        this.imageView = new ImageView(spriteSheet);
        this.imageView.setFitWidth(frameWidth);
        this.imageView.setFitHeight(frameHeight);
        this.imageView.setSmooth(false);
        this.imageView.setPreserveRatio(false);
        this.imageView.setViewport(new Rectangle2D(0, 0, frameWidth, frameHeight));
    }

    /**
     * Devuelve el ImageView que se usa como vista de la entidad.
     *
     * @return vista de la explosión
     */
    public ImageView getView() {
        return imageView;
    }

    /**
     * Fija el tamaño con el que se muestra la explosión, independientemente del tamaño del fotograma.
     *
     * @param width ancho en píxeles
     * @param height alto en píxeles
     */
    public void setDisplaySize(int width, int height) {
        this.imageView.setFitWidth(width);
        this.imageView.setFitHeight(height);
    }

    @Override
    public void onUpdate(double tpf) {
        if (finished) {
            return;
        }

        elapsed += tpf;

        int newFrame = (int) (elapsed / frameDuration);

        if (newFrame >= frameCount) {
            finished = true;
            entity.removeFromWorld();
            return;
        }

        if (newFrame != currentFrame) {
            currentFrame = newFrame;
            int col = currentFrame % columns;
            int row = currentFrame / columns;
            imageView.setViewport(new Rectangle2D(col * frameWidth, row * frameHeight, frameWidth, frameHeight));
        }
    }
}
