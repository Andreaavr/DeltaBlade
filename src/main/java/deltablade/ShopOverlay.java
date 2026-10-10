package deltablade;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.util.function.Consumer;

import static com.almasb.fxgl.dsl.FXGL.getAppHeight;
import static com.almasb.fxgl.dsl.FXGL.getAppWidth;

/**
 * Post-warp shop: left preview/description, right list (teuerste oben).
 */
public final class ShopOverlay {

    private static final String BUTTON_BASE =
            "-fx-background-color: linear-gradient(to bottom, #2a5298, #1e3c72);"
                    + "-fx-text-fill: white;"
                    + "-fx-padding: 10 28;"
                    + "-fx-background-radius: 8;"
                    + "-fx-border-color: #4a90d9;"
                    + "-fx-border-width: 2;"
                    + "-fx-border-radius: 8;"
                    + "-fx-cursor: hand;";

    private static final String BUTTON_HOVER =
            "-fx-background-color: linear-gradient(to bottom, #3a6ab8, #2e4c82);"
                    + "-fx-text-fill: white;"
                    + "-fx-padding: 10 28;"
                    + "-fx-background-radius: 8;"
                    + "-fx-border-color: #6ab0f9;"
                    + "-fx-border-width: 2;"
                    + "-fx-border-radius: 8;"
                    + "-fx-cursor: hand;";

    private static final String BUTTON_DISABLED =
            "-fx-background-color: #1a2438;"
                    + "-fx-text-fill: #8899aa;"
                    + "-fx-padding: 10 28;"
                    + "-fx-background-radius: 8;"
                    + "-fx-border-color: #334455;"
                    + "-fx-border-width: 2;"
                    + "-fx-border-radius: 8;";

    public record RunStats(int money, int rank, int wave, int meteorWins, int cognitiveWins,
                           int squadCombos, int bossKills, ShopWeapon equipped,
                           int weaponGrade, int ammoCap, boolean autofire) {}

    @FunctionalInterface
    public interface BuyHandler {
        boolean tryBuy(ShopItem item);
    }

    private final Group root = new Group();
    private final ShopItem[] items = ShopItem.catalog();
    private final Text[] rowTexts = new Text[items.length];
    private final RunStats initialStats;
    private final BuyHandler onBuy;
    private final Consumer<ShopWeapon> onClose;
    private final Text moneyText;
    private final Text titleText;
    private final Text typeText;
    private final Text priceText;
    private final Text descText;
    private final Text unlockText;
    private final Rectangle previewBox;
    private final Button buyButton;
    private int selected;
    private int money;
    private ShopWeapon equipped;
    private int weaponGrade;
    private int ammoCap;
    private boolean autofire;
    private boolean closed;

    public ShopOverlay(RunStats stats, BuyHandler onBuy, Consumer<ShopWeapon> onClose) {
        this.initialStats = stats;
        this.onBuy = onBuy;
        this.onClose = onClose;
        this.money = stats.money();
        this.equipped = stats.equipped() == null ? ShopWeapon.NONE : stats.equipped();
        this.weaponGrade = stats.weaponGrade();
        this.ammoCap = stats.ammoCap();
        this.autofire = stats.autofire();
        this.selected = 0;

        double width = getAppWidth();
        double height = getAppHeight();

        Rectangle dim = new Rectangle(width, height);
        dim.setFill(Color.rgb(0, 0, 0, 0.78));

        VBox panel = new VBox(12);
        panel.setPadding(new Insets(18, 22, 18, 22));
        panel.setPrefWidth(740);
        panel.setStyle(
                "-fx-background-color: rgb(8,14,26);"
                        + "-fx-border-color: #4a90d9;"
                        + "-fx-border-width: 2;"
                        + "-fx-background-radius: 12;"
                        + "-fx-border-radius: 12;");
        panel.setEffect(new DropShadow(18, Color.rgb(40, 140, 220, 0.45)));

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Text armory = mono("SHOP", 24, Color.CYAN);
        armory.setEffect(new DropShadow(10, Color.rgb(40, 180, 255, 0.6)));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        moneyText = mono("$" + money, 20, Color.GOLD);
        header.getChildren().addAll(armory, spacer, moneyText);

        HBox body = new HBox(18);
        body.setAlignment(Pos.TOP_LEFT);

        VBox left = new VBox(10);
        left.setPrefWidth(300);
        left.setAlignment(Pos.TOP_CENTER);

        previewBox = new Rectangle(220, 140);
        previewBox.setArcWidth(12);
        previewBox.setArcHeight(12);
        previewBox.setStroke(Color.web("#4a90d9"));
        previewBox.setStrokeWidth(2);
        StackPane previewPane = new StackPane(previewBox);
        Text previewLabel = mono("NO IMAGE", 12, Color.rgb(120, 140, 160));
        previewPane.getChildren().add(previewLabel);

        titleText = mono("", 18, Color.WHITE);
        typeText = mono("", 12, Color.CYAN);
        priceText = mono("", 14, Color.GOLD);
        descText = mono("", 12, Color.LIGHTGRAY);
        descText.setWrappingWidth(280);
        descText.setTextAlignment(TextAlignment.LEFT);
        unlockText = mono("", 11, Color.ORANGERED);
        unlockText.setWrappingWidth(280);

        left.getChildren().addAll(previewPane, titleText, typeText, priceText, descText, unlockText);

        VBox right = new VBox(4);
        right.setPrefWidth(360);
        for (int i = 0; i < items.length; i++) {
            final int index = i;
            Text row = mono("", 13, Color.WHITE);
            row.setOnMouseClicked(e -> {
                selected = index;
                refresh();
            });
            rowTexts[i] = row;
            right.getChildren().add(row);
        }

        body.getChildren().addAll(left, right);

        buyButton = new Button("BUY");
        styleButton(buyButton, true);
        buyButton.setOnAction(e -> tryPurchase());

        Button continueButton = new Button("CONTINUE");
        styleButton(continueButton, true);
        continueButton.setOnAction(e -> closeShop());

        HBox actions = new HBox(16, buyButton, continueButton);
        actions.setAlignment(Pos.CENTER);

        Text hint = mono("UP/DOWN  SELECT   ENTER  BUY   ESC  CONTINUE", 11, Color.rgb(140, 160, 180));
        hint.setTextAlignment(TextAlignment.CENTER);

        panel.getChildren().addAll(header, body, actions, hint);
        panel.setLayoutX((width - 740) / 2.0);
        panel.setLayoutY(Math.max(16, (height - 520) / 2.0) + 16);
        panel.setOpacity(0);

        root.getChildren().addAll(dim, panel);
        refresh();

        FadeTransition fade = new FadeTransition(Duration.millis(350), panel);
        fade.setFromValue(0);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(350), panel);
        slide.setFromY(24);
        slide.setToY(0);
        fade.play();
        slide.play();
    }

    public Group getRoot() {
        return root;
    }

    public boolean handleKey(KeyEvent e) {
        if (closed) {
            return true;
        }
        KeyCode code = e.getCode();
        if (code == KeyCode.UP || code == KeyCode.W) {
            selected = (selected + items.length - 1) % items.length;
            refresh();
            return true;
        }
        if (code == KeyCode.DOWN || code == KeyCode.S) {
            selected = (selected + 1) % items.length;
            refresh();
            return true;
        }
        if (code == KeyCode.ENTER || code == KeyCode.X) {
            tryPurchase();
            return true;
        }
        if (code == KeyCode.ESCAPE) {
            closeShop();
            return true;
        }
        return true;
    }

    private void tryPurchase() {
        ShopItem item = items[selected];
        if (!canPurchase(item)) {
            return;
        }
        if (onBuy.tryBuy(item)) {
            money -= item.price();
            applyLocalPurchase(item);
            moneyText.setText("$" + money);
            refresh();
        }
    }

    private void applyLocalPurchase(ShopItem item) {
        switch (item.kind()) {
            case WEAPON -> equipped = item.weapon();
            case GRADE -> weaponGrade = Math.max(weaponGrade, item.targetGrade());
            case AUTOFIRE -> autofire = true;
            case CONSUMABLE -> {
                if (item == ShopItem.EXTRA_AMMO) {
                    ammoCap = Math.min(GameVars.MAX_AMMO_CAP, ammoCap + 1);
                }
            }
        }
    }

    private void closeShop() {
        if (closed) {
            return;
        }
        closed = true;
        onClose.accept(equipped);
    }

    private boolean unlocked(ShopItem item) {
        return item.isUnlocked(initialStats.rank(), initialStats.wave(), initialStats.meteorWins(),
                initialStats.cognitiveWins(), initialStats.squadCombos(), initialStats.bossKills());
    }

    private boolean isMaxed(ShopItem item) {
        return switch (item.kind()) {
            case WEAPON -> equipped == item.weapon();
            case GRADE -> weaponGrade >= item.targetGrade();
            case AUTOFIRE -> autofire;
            case CONSUMABLE -> item == ShopItem.EXTRA_AMMO && ammoCap >= GameVars.MAX_AMMO_CAP;
        };
    }

    private boolean canPurchase(ShopItem item) {
        return unlocked(item) && !isMaxed(item) && money >= item.price();
    }

    private void refresh() {
        ShopItem item = items[selected];
        boolean unlocked = unlocked(item);
        boolean maxed = isMaxed(item);
        boolean canBuy = canPurchase(item);

        titleText.setText(item.displayName());
        typeText.setText(item.typeTag());
        typeText.setFill(colorFor(item.colorTag()));
        previewBox.setFill(Color.rgb(16, 24, 40));
        previewBox.setStroke(colorFor(item.colorTag()));
        descText.setText(item.description());

        if (!unlocked) {
            priceText.setText("LOCKED");
            priceText.setFill(Color.ORANGERED);
            unlockText.setText(item.unlockHint());
        } else if (maxed) {
            priceText.setText(item.kind() == ShopItem.Kind.WEAPON ? "EQUIPPED" : "MAX");
            priceText.setFill(Color.LIMEGREEN);
            unlockText.setText("");
        } else {
            priceText.setText("$" + item.price());
            priceText.setFill(money >= item.price() ? Color.GOLD : Color.ORANGERED);
            unlockText.setText("");
        }

        buyButton.setDisable(!canBuy);
        styleButton(buyButton, canBuy);

        for (int i = 0; i < items.length; i++) {
            ShopItem rowItem = items[i];
            boolean itemUnlocked = unlocked(rowItem);
            boolean itemMaxed = isMaxed(rowItem);
            boolean sel = i == selected;
            String marker = sel ? "> " : "  ";
            String right;
            Color color;
            if (!itemUnlocked) {
                right = "LOCKED";
                color = Color.rgb(120, 90, 90);
            } else if (itemMaxed) {
                right = rowItem.kind() == ShopItem.Kind.WEAPON ? "ON" : "MAX";
                color = Color.LIMEGREEN;
            } else {
                right = "$" + rowItem.price();
                color = sel ? Color.CYAN : Color.WHITE;
            }
            rowTexts[i].setText(marker + pad(rowItem.displayName(), 14) + "  " + right);
            rowTexts[i].setFill(color);
        }
    }

    private static void styleButton(Button button, boolean enabled) {
        button.setStyle(enabled ? BUTTON_BASE : BUTTON_DISABLED);
        if (enabled) {
            button.setOnMouseEntered(e -> button.setStyle(BUTTON_HOVER));
            button.setOnMouseExited(e -> button.setStyle(BUTTON_BASE));
        } else {
            button.setOnMouseEntered(null);
            button.setOnMouseExited(null);
        }
        button.setFont(Font.font("Monospace", FontWeight.BOLD, 14));
    }

    private static Text mono(String s, double size, Color color) {
        Text t = new Text(s);
        t.setFont(Font.font("Monospace", FontWeight.BOLD, size));
        t.setFill(color);
        return t;
    }

    private static String pad(String s, int width) {
        if (s.length() >= width) {
            return s.substring(0, width);
        }
        return s + " ".repeat(width - s.length());
    }

    private static Color colorFor(ShopItem.ColorTag tag) {
        return switch (tag) {
            case CYAN -> Color.CYAN;
            case AQUA -> Color.AQUA;
            case VIOLET -> Color.MEDIUMPURPLE;
            case GOLD -> Color.GOLD;
            case ORANGE -> Color.ORANGE;
            case PINK -> Color.HOTPINK;
            case LIME -> Color.LIMEGREEN;
        };
    }
}
