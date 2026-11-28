package deltablade.components;

import com.almasb.fxgl.entity.component.Component;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * Nave del jugador animada a partir de las hojas de sprites azules de Gerald.
 * Cada hoja mide 384x48 (8 fotogramas de 48x48, 120 ms).
 *
 * Propulsión: llama del motor en bucle cuando la nave está parada.
 * Inclinación lateral: fotogramas 1-3 inclinan a la derecha y 5-7 a la izquierda (ambas direcciones en una sola hoja).
 * Morro: punta de la nave, reservada para el empuje hacia delante.
 * Pulso del disparo: fogonazo del cañón mientras se dispara.
 */
public class PlayerAnimationComponent extends Component {

    /**
     * Animaciones posibles de la nave del jugador.
     */
    public enum AnimationState {
        IDLE,
        BANKING_LEFT,
        BANKING_RIGHT,
        MOVING_UP,
        FIRING
    }

    private final ImageView imageView;
    private final Image thrusterSheet;
    private final Image bankSheet;
    private final Image bowwaveSheet;
    private final Image beamSheet;

    private static final int FRAME_COUNT = 8;
    private static final int FRAME_WIDTH = 48;
    private static final int FRAME_HEIGHT = 48;
    private static final double FRAME_DURATION = 0.120;

    private AnimationState currentState = AnimationState.IDLE;
    private double elapsed = 0;
    private int currentFrame = 0;
    private int frameMin = 0;
    private int frameMax = FRAME_COUNT - 1;
    private boolean holdLast = false;

    private Image currentSheet;
    private boolean scooping;

    /**
     * Crea las animaciones de la nave sin la animación del rayo.
     *
     * @param thrusterSheet hoja de sprites del propulsor en reposo
     * @param bankSheet hoja de sprites del giro lateral
     * @param bowwaveSheet hoja de sprites de la onda de proa al disparar
     */
    public PlayerAnimationComponent(Image thrusterSheet, Image bankSheet, Image bowwaveSheet) {
        this(thrusterSheet, bankSheet, bowwaveSheet, null);
    }

    /**
     * Crea las animaciones de la nave.
     *
     * @param thrusterSheet hoja de sprites del propulsor en reposo
     * @param bankSheet hoja de sprites del giro lateral
     * @param bowwaveSheet hoja de sprites de la onda de proa al disparar
     * @param beamSheet hoja de sprites del rayo tractor, o {@code null} si no hay
     */
    public PlayerAnimationComponent(Image thrusterSheet, Image bankSheet, Image bowwaveSheet, Image beamSheet) {
        this.thrusterSheet = thrusterSheet;
        this.bankSheet = bankSheet;
        this.bowwaveSheet = bowwaveSheet;
        this.beamSheet = beamSheet;
        this.currentSheet = thrusterSheet;

        this.imageView = new ImageView(thrusterSheet);
        this.imageView.setFitWidth(FRAME_WIDTH);
        this.imageView.setFitHeight(FRAME_HEIGHT);
        this.imageView.setSmooth(false);
        this.imageView.setPreserveRatio(false);
        this.imageView.setViewport(new Rectangle2D(0, 0, FRAME_WIDTH, FRAME_HEIGHT));
        applyRange(AnimationState.IDLE);
    }

    /**
     * Devuelve la vista en la que se dibuja la animación.
     *
     * @return vista de la animación
     */
    public ImageView getView() {
        return imageView;
    }

    /**
     * Activa o desactiva la animación del rayo tractor.
     *
     * @param scooping {@code true} mientras el rayo tractor está activo
     */
    public void setScooping(boolean scooping) {
        if (this.scooping == scooping) {
            return;
        }
        this.scooping = scooping;
        Image newSheet = getSheetForState(currentState);
        if (newSheet != null && newSheet != currentSheet) {
            currentSheet = newSheet;
            imageView.setImage(currentSheet);
        }
        applyRange(currentState);
        elapsed = 0;
        currentFrame = frameMin;
        imageView.setViewport(new Rectangle2D(currentFrame * FRAME_WIDTH, 0, FRAME_WIDTH, FRAME_HEIGHT));
    }

    /**
     * Cambia la animación de la nave.
     *
     * @param newState nueva animación
     */
    public void setState(AnimationState newState) {
        if (currentState == newState) {
            return;
        }
        currentState = newState;
        Image newSheet = getSheetForState(newState);
        if (newSheet != currentSheet && newSheet != null) {
            currentSheet = newSheet;
            imageView.setImage(currentSheet);
        }
        applyRange(newState);
        elapsed = 0;
        currentFrame = frameMin;
        imageView.setViewport(new Rectangle2D(currentFrame * FRAME_WIDTH, 0, FRAME_WIDTH, FRAME_HEIGHT));
    }

    /**
     * Devuelve la animación actual de la nave.
     *
     * @return animación actual
     */
    public AnimationState getState() {
        return currentState;
    }

    private void applyRange(AnimationState state) {
        holdLast = false;
        switch (state) {
            case BANKING_RIGHT -> {
                frameMin = 1;
                frameMax = 3;
                holdLast = true;
            }
            case BANKING_LEFT -> {
                frameMin = 5;
                frameMax = 7;
                holdLast = true;
            }
            default -> {
                frameMin = 0;
                frameMax = FRAME_COUNT - 1;
            }
        }
    }

    private Image getSheetForState(AnimationState state) {
        if (scooping && bowwaveSheet != null
                && state != AnimationState.BANKING_LEFT
                && state != AnimationState.BANKING_RIGHT) {
            return bowwaveSheet;
        }
        return switch (state) {
            case BANKING_LEFT, BANKING_RIGHT -> bankSheet != null ? bankSheet : thrusterSheet;
            case MOVING_UP -> bowwaveSheet != null ? bowwaveSheet : thrusterSheet;
            case FIRING -> beamSheet != null ? beamSheet : thrusterSheet;
            default -> thrusterSheet;
        };
    }

    @Override
    public void onUpdate(double tpf) {
        elapsed += tpf;

        int span = frameMax - frameMin + 1;
        int steps = (int) (elapsed / FRAME_DURATION);
        int newFrame;
        if (holdLast) {
            newFrame = frameMin + Math.min(steps, span - 1);
        } else {
            newFrame = frameMin + (steps % span);
        }

        if (newFrame != currentFrame) {
            currentFrame = newFrame;
            imageView.setViewport(new Rectangle2D(currentFrame * FRAME_WIDTH, 0, FRAME_WIDTH, FRAME_HEIGHT));
        }
    }

    /**
     * Devuelve el ancho de cada fotograma de la animación.
     *
     * @return ancho en píxeles
     */
    public int getFrameWidth() {
        return FRAME_WIDTH;
    }

    /**
     * Devuelve el alto de cada fotograma de la animación.
     *
     * @return alto en píxeles
     */
    public int getFrameHeight() {
        return FRAME_HEIGHT;
    }
}
