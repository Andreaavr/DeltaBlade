package deltablade.components;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.component.Component;
import javafx.scene.paint.Color;

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

    /** Número de colores de marcador disponibles. */
    public static int colorCount() {
        return COLORS.length;
    }

    /** Color del marcador con el índice indicado. */
    public static Color colorAt(int index) {
        return COLORS[index];
    }

    /** Textura del marcador con el índice indicado. */
    public static String textureAt(int index) {
        return TEXTURES[index];
    }

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

    public int getColorIndex() {
        return colorIndex;
    }

    public Color getColor() {
        return COLORS[colorIndex];
    }
}
