package deltablade.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import deltablade.GameVars;
import javafx.scene.Group;
import javafx.scene.text.Text;

/**
 * Componente FXGL de los orbes con las letras de la palabra EXTRA, que caen y giran hasta que el jugador los recoge.
 */
public class ExtraLetterPickupComponent extends Component {

    private final char letter;
    private final int letterIndex;
    private final Group flipWrapper;
    private double speed = 80;
    private double flipPhase = 0;

    /**
     * Crea el orbe de una letra.
     *
     * @param letter letra que contiene
     * @param letterIndex posición de la letra en la palabra EXTRA (0 a 4)
     * @param letterText texto que muestra la letra; puede ser {@code null}
     */
    public ExtraLetterPickupComponent(char letter, int letterIndex, Text letterText) {
        this.letter = letter;
        this.letterIndex = letterIndex;
        this.flipWrapper = letterText != null ? new Group(letterText) : null;
    }

    /**
     * Devuelve el grupo que contiene el texto de la letra y que se gira para animarla.
     *
     * @return grupo de la letra, o {@code null} si no hay texto
     */
    public Group getFlipWrapper() {
        return flipWrapper;
    }

    @Override
    public void onAdded() {
    }

    @Override
    public void onUpdate(double tpf) {
        entity.translateY(speed * tpf);

        if (flipWrapper != null) {
            flipPhase += tpf * 2.5;
            double scaleX = Math.cos(flipPhase);
            if (Math.abs(scaleX) < 0.18) {
                scaleX = scaleX < 0 ? -0.18 : 0.18;
            }
            flipWrapper.setScaleX(scaleX);
        }

        double playableLeft = GameVars.RAIL_WIDTH;
        double playableRight = FXGL.getAppWidth() - GameVars.RAIL_WIDTH;
        double centerX = entity.getX() + 16;
        if (centerX < playableLeft) {
            entity.setX(playableLeft - 16);
        } else if (centerX > playableRight) {
            entity.setX(playableRight - 16);
        }

        if (entity.getY() > FXGL.getAppHeight() + 80) {
            entity.removeFromWorld();
        }
    }

    /**
     * Devuelve la letra del orbe.
     *
     * @return letra
     */
    public char getLetter() {
        return letter;
    }

    /**
     * Devuelve la posición de la letra en la palabra EXTRA.
     *
     * @return índice de 0 a 4
     */
    public int getLetterIndex() {
        return letterIndex;
    }
}
