package deltablade.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import javafx.scene.paint.Color;

/**
 * Componente FXGL de los marcadores de rango que se recogen para subir de rango; cada uno tiene un color.
 */
public class RankMarkerComponent extends Component {

    private static final Color[] COLORS = {
            Color.DEEPPINK,
            Color.DODGERBLUE,
            Color.LIMEGREEN,
            Color.GOLD,
            Color.ORANGE,
            Color.CRIMSON
    };

    public static final String[] NAMES = {
            "ROSA", "AZUL", "VERDE", "AMARILLO", "NARANJA", "ROJO"
    };

    private static final String[] TEXTURES = {
            "rank_pink.png",
            "rank_blue.png",
            "rank_green.png",
            "rank_yellow.png",
            "rank_orange.png",
            "rank_red.png"
    };

    private final int colorIndex;
    private double speed = 70;
    private double pulsePhase = 0;

    /**
     * Devuelve el número de colores de marcador disponibles.
     *
     * @return número de colores
     */
    public static int colorCount() {
        return COLORS.length;
    }

    /**
     * Devuelve el color del marcador con el índice indicado.
     *
     * @param index índice del color
     * @return color del marcador
     */
    public static Color colorAt(int index) {
        return COLORS[index];
    }

    /**
     * Devuelve la textura del marcador con el índice indicado.
     *
     * @param index índice del color
     * @return nombre del fichero de la textura
     */
    public static String textureAt(int index) {
        return TEXTURES[index];
    }

    /**
     * Crea un marcador de rango del color indicado.
     *
     * @param colorIndex índice del color (se ajusta al rango válido)
     */
    public RankMarkerComponent(int colorIndex) {
        this.colorIndex = Math.max(0, Math.min(COLORS.length - 1, colorIndex));
    }

    @Override
    public void onUpdate(double tpf) {
        entity.translateY(speed * tpf);
        pulsePhase += tpf * 6;
        double scale = 1 + Math.sin(pulsePhase) * 0.12;
        entity.setScaleX(scale);
        entity.setScaleY(scale);
        if (entity.getY() > FXGL.getAppHeight()) {
            entity.removeFromWorld();
        }
    }

    /**
     * Devuelve el índice del color de este marcador.
     *
     * @return índice del color
     */
    public int getColorIndex() {
        return colorIndex;
    }

    /**
     * Devuelve el color de este marcador.
     *
     * @return color del marcador
     */
    public Color getColor() {
        return COLORS[colorIndex];
    }
}
