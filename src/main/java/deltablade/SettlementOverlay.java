package deltablade;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import static com.almasb.fxgl.dsl.FXGL.getAppHeight;
import static com.almasb.fxgl.dsl.FXGL.getAppWidth;

/**
 * Desglose de los bonus al final de la partida, antes de introducir el récord.
 */
public final class SettlementOverlay {

    /**
     * Línea del desglose de bonificaciones.
     *
     * @param label texto de la línea
     * @param amount puntos que suma
     */
    public record Line(String label, int amount) {}

    private final Group root = new Group();
    private final IntConsumer onFinished;
    private final int baseScore;
    private final int totalBonus;
    private boolean finished;

    /**
     * Crea la pantalla de desglose de bonificaciones.
     *
     * @param baseScore puntuación antes de sumar las bonificaciones
     * @param lines líneas de bonificación que se muestran
     * @param onFinished acción al cerrar; recibe la puntuación final
     */
    public SettlementOverlay(int baseScore, List<Line> lines, IntConsumer onFinished) {
        this.baseScore = baseScore;
        this.onFinished = onFinished;
        int sum = 0;
        for (Line line : lines) {
            sum += Math.max(0, line.amount());
        }
        this.totalBonus = sum;

        double width = getAppWidth();
        double height = getAppHeight();

        Rectangle dim = new Rectangle(width, height);
        dim.setFill(Color.rgb(0, 0, 0, 0.82));

        VBox panel = new VBox(8);
        panel.setAlignment(Pos.CENTER_LEFT);
        panel.setPadding(new Insets(28, 36, 28, 36));
        panel.setMaxWidth(520);
        panel.setStyle(
                "-fx-background-color: rgb(10,16,28);"
                        + "-fx-border-color: #4a90d9;"
                        + "-fx-border-width: 2;"
                        + "-fx-background-radius: 10;"
                        + "-fx-border-radius: 10;");

        Text title = mono(I18n.tr("settle.title"), 26, Color.CYAN);
        title.setEffect(new DropShadow(12, Color.rgb(40, 180, 255, 0.7)));
        Text base = mono(I18n.tr("settle.score", format(baseScore)), 16, Color.LIGHTGRAY);

        panel.getChildren().addAll(title, base, spacer());

        List<Text> amountTexts = new ArrayList<>();
        for (Line line : lines) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            Text label = mono(pad(line.label(), 16), 14, Color.GOLD);
            Text amount = mono("+" + format(line.amount()), 14, Color.WHITE);
            amount.setOpacity(0);
            amountTexts.add(amount);
            row.getChildren().addAll(label, amount);
            panel.getChildren().add(row);
        }

        Text total = mono(I18n.tr("settle.total", format(baseScore + totalBonus)), 18, Color.CYAN);
        total.setOpacity(0);
        panel.getChildren().addAll(spacer(), total);

        Text hint = mono(I18n.tr("settle.hint"), 12, Color.rgb(160, 180, 200));
        hint.setOpacity(0);
        panel.getChildren().add(hint);

        panel.setLayoutX((width - 520) / 2.0);
        panel.setLayoutY(Math.max(40, (height - 420) / 2.0));
        panel.setOpacity(0);

        root.getChildren().addAll(dim, panel);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(320), panel);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        SequentialTransition reveal = new SequentialTransition();
        reveal.getChildren().add(fadeIn);
        for (Text amount : amountTexts) {
            FadeTransition ft = new FadeTransition(Duration.millis(180), amount);
            ft.setFromValue(0);
            ft.setToValue(1);
            reveal.getChildren().add(ft);
            reveal.getChildren().add(new PauseTransition(Duration.millis(60)));
        }
        FadeTransition totalFade = new FadeTransition(Duration.millis(220), total);
        totalFade.setFromValue(0);
        totalFade.setToValue(1);
        FadeTransition hintFade = new FadeTransition(Duration.millis(220), hint);
        hintFade.setFromValue(0);
        hintFade.setToValue(1);
        reveal.getChildren().addAll(totalFade, hintFade);
        reveal.play();
    }

    /**
     * Devuelve el nodo raíz de la pantalla, para añadirlo a la escena.
     *
     * @return nodo raíz
     */
    public Group getRoot() {
        return root;
    }

    /**
     * Confirma la pantalla y entrega la puntuación final.
     *
     * @return {@code true} siempre, porque la tecla de confirmación se ha usado
     */
    public boolean handleConfirm() {
        if (finished) {
            return true;
        }
        finished = true;
        onFinished.accept(baseScore + totalBonus);
        return true;
    }

    private static Text mono(String s, double size, Color color) {
        Text t = new Text(s);
        t.setFont(Font.font("Monospace", FontWeight.BOLD, size));
        t.setFill(color);
        return t;
    }

    private static Rectangle spacer() {
        Rectangle r = new Rectangle(1, 8);
        r.setFill(Color.TRANSPARENT);
        return r;
    }

    private static String format(int value) {
        return String.format("%06d", Math.max(0, value));
    }

    private static String pad(String s, int width) {
        if (s.length() >= width) {
            return s;
        }
        return s + " ".repeat(width - s.length());
    }
}
