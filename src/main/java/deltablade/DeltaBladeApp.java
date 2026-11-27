package deltablade;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.input.UserAction;
import deltablade.components.BackgroundScrollComponent;
import deltablade.components.BulletComponent;
import deltablade.components.CoverMeteorComponent;
import deltablade.components.EnemyComponent;
import deltablade.components.ExtraLetterPickupComponent;
import deltablade.components.MissileComponent;
import deltablade.components.PickupComponent;
import deltablade.components.PlasmaOrbComponent;
import deltablade.components.PlayerComponent;
import deltablade.components.RankMarkerComponent;
import deltablade.components.StarComponent;
import deltablade.minigames.CognitiveTestGame;
import deltablade.minigames.MazeEscapeGame;
import deltablade.minigames.AlienEnduranceGame;
import deltablade.minigames.MeteorStormGame;
import deltablade.minigames.Minigame;
import deltablade.minigames.MinigameHost;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import static com.almasb.fxgl.dsl.FXGL.*;

public class DeltaBladeApp extends GameApplication implements MinigameHost {

    private Entity player;
    private boolean movingLeft = false;
    private boolean movingRight = false;
    private boolean movingUp = false;
    private boolean movingDown = false;
    private boolean holdingFire = false;
    private long lastMissileTapAt = 0;
    private double meteorMissileCooldown = 0;
    private boolean gameOver = false;
    private boolean showingTitleScreen = true;
    private boolean gameStarted = false;

    private WaveManager waveManager;
    private boolean waveTransition = false;
    private static final double GET_READY_DURATION = 2.4;
    private double getReadyRemaining = 0;
    private boolean getReadySoundQueued = false;
    private boolean minigameActive = false;
    private Minigame activeMinigame;
    private boolean warping = false;
    private double warpElapsed = 0;
    private Rectangle warpFlash;
    private boolean shopOpen = false;
    private ShopOverlay shopOverlay;
    private Node shopRoot;
    private SettlementOverlay settlementOverlay;
    private Node settlementRoot;
    private double controlReverseRemaining = 0;
    private double fireJamRemaining = 0;
    private double laserHeat = 0;
    private double laserCooldown = 0;
    private double laserTickTimer = 0;
    private boolean laserShotCounted = false;
    private double sweepAngle = 0;
    private double sweepActive = 0;
    private double sweepPause = 0;
    private double plasmaCooldown = 0;
    private double arcCooldown = 0;
    private final List<Line> laserBeams = new ArrayList<>();
    private int extraTimeMax = GameVars.LEVEL_TIME_INITIAL;
    private double extraTime = GameVars.LEVEL_TIME_INITIAL;
    private boolean extraTimeWarned = false;
    private boolean ufoSpawnedThisWave = false;
    private double extraTimePulse = 0;
    /** Segundos que quedan después de HURRY UP antes de que pueda aparecer el ovni. */
    private double hurryUfoDelay = 0;
    private boolean pendingHurryUfo = false;
    private double scoopHitLock = 0;

    private static final Random RANDOM = new Random();
    private int frameCount = 0;
    private static final int WARMUP_FRAMES = 5;

    private List<Node> titleScreenNodes = new ArrayList<>();
    private List<Animation> extraLetterAnimations = new ArrayList<>();
    private List<Node> activeBanners = new ArrayList<>();
    private int scorePopupSeq = 0;
    private boolean optionsOpen = false;
    private boolean enginePausedByOptions = false;
    private Node optionsRoot;
    private HighScoreOverlay highScoreOverlay;
    private Node highScoreRoot;
    private Text titleHiLine;
    private Timeline titleHiRotate;
    private boolean titleHiGlobal;
    private static boolean escFilterInstalled = false;

    private Timeline activeShake = null;
    private List<Node> shakenViewNodes = new ArrayList<>();

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setWidth(800);
        settings.setHeight(600);
        settings.setTitle("DeltaBlade");
        settings.setVersion(AppVersion.current());
        settings.setMainMenuEnabled(false);
        settings.setGameMenuEnabled(false);

        settings.setManualResizeEnabled(true);
        settings.setPreserveResizeRatio(true);
        settings.setScaleAffectedOnResize(true);
        settings.setFullScreenAllowed(true);
        // LaunchServices (doble clic) arranca con el directorio de trabajo "/" o con el DMG de solo lectura.
        // Si no, FXGL crea "logs/" y se cierra sin avisar con el error Read-only file system.
        settings.setFileSystemWriteAllowed(false);
    }

    @Override
    protected void initGameVars(Map<String, Object> vars) {
        vars.put(GameVars.SCORE, 0);
        vars.put(GameVars.LIVES, GameVars.INITIAL_LIVES);
        vars.put(GameVars.LEVEL, 1);
        vars.put(GameVars.WEAPON_GRADE, 1);
        vars.put(GameVars.AMMO_CAP, GameVars.INITIAL_AMMO_CAP);
        vars.put(GameVars.ACTIVE_BULLETS, 0);
        vars.put(GameVars.ENEMIES_REMAINING, 0);
        vars.put(GameVars.MONEY, 0);
        vars.put(GameVars.EXTRA_E, 0);
        vars.put(GameVars.EXTRA_X, 0);
        vars.put(GameVars.EXTRA_T, 0);
        vars.put(GameVars.EXTRA_R, 0);
        vars.put(GameVars.EXTRA_A, 0);
        vars.put(GameVars.AUTOFIRE, false);
        vars.put(GameVars.EXTRA_LETTER_SPAWNED_THIS_WAVE, 0);
        vars.put(GameVars.SQUAD_COMBOS, 0);
        vars.put(GameVars.COGNITIVE_WINS, 0);
        vars.put(GameVars.METEOR_WINS, 0);
        vars.put(GameVars.MAZE_WINS, 0);
        vars.put(GameVars.RANK, 0);
        vars.put(GameVars.RANK_MASK, 0);
        vars.put(GameVars.SHOTS_FIRED, 0);
        vars.put(GameVars.HITS_LANDED, 0);
        vars.put(GameVars.SHOP_WEAPON, 0);
        vars.put(GameVars.BOSS_KILLS, 0);
        vars.put(GameVars.BONUS_PERFECT_CLEARS, 0);
    }

    @Override
    protected void initInput() {
        getInput().addAction(new UserAction("Move Left") {
            @Override
            protected void onAction() {
                movingLeft = true;
            }
            @Override
            protected void onActionEnd() {
                movingLeft = false;
            }
        }, KeyCode.LEFT);

        getInput().addAction(new UserAction("Move Left A") {
            @Override
            protected void onAction() {
                movingLeft = true;
            }
            @Override
            protected void onActionEnd() {
                movingLeft = false;
            }
        }, KeyCode.A);

        getInput().addAction(new UserAction("Move Right") {
            @Override
            protected void onAction() {
                movingRight = true;
            }
            @Override
            protected void onActionEnd() {
                movingRight = false;
            }
        }, KeyCode.RIGHT);

        getInput().addAction(new UserAction("Move Right D") {
            @Override
            protected void onAction() {
                movingRight = true;
            }
            @Override
            protected void onActionEnd() {
                movingRight = false;
            }
        }, KeyCode.D);

        getInput().addAction(new UserAction("Move Up") {
            @Override
            protected void onAction() {
                movingUp = true;
            }
            @Override
            protected void onActionEnd() {
                movingUp = false;
            }
        }, KeyCode.UP);

        getInput().addAction(new UserAction("Move Up W") {
            @Override
            protected void onAction() {
                movingUp = true;
            }
            @Override
            protected void onActionEnd() {
                movingUp = false;
            }
        }, KeyCode.W);

        getInput().addAction(new UserAction("Move Down") {
            @Override
            protected void onAction() {
                movingDown = true;
            }
            @Override
            protected void onActionEnd() {
                movingDown = false;
            }
        }, KeyCode.DOWN);

        getInput().addAction(new UserAction("Move Down S") {
            @Override
            protected void onAction() {
                movingDown = true;
            }
            @Override
            protected void onActionEnd() {
                movingDown = false;
            }
        }, KeyCode.S);

        getInput().addAction(new UserAction("Fire") {
            @Override
            protected void onActionBegin() {
                onFirePressed();
            }
            @Override
            protected void onAction() {
                holdingFire = true;
                if (!gameOver && !minigameActive && findActiveMissile() == null && getb(GameVars.AUTOFIRE)) {
                    fire();
                }
            }
            @Override
            protected void onActionEnd() {
                holdingFire = false;
            }
        }, KeyCode.SPACE);

        bindTestDigit("Test 0", KeyCode.DIGIT0, KeyCode.NUMPAD0);
        bindTestDigit("Test 1", KeyCode.DIGIT1, KeyCode.NUMPAD1);
        bindTestDigit("Test 2", KeyCode.DIGIT2, KeyCode.NUMPAD2);
        bindTestDigit("Test 3", KeyCode.DIGIT3, KeyCode.NUMPAD3);
        bindTestDigit("Test 4", KeyCode.DIGIT4, KeyCode.NUMPAD4);
        bindTestDigit("Test 5", KeyCode.DIGIT5, KeyCode.NUMPAD5);
        bindTestDigit("Test 6", KeyCode.DIGIT6, KeyCode.NUMPAD6);
        bindTestDigit("Test 7", KeyCode.DIGIT7, KeyCode.NUMPAD7);
        bindTestDigit("Test 8", KeyCode.DIGIT8, KeyCode.NUMPAD8);
        bindTestDigit("Test 9", KeyCode.DIGIT9, KeyCode.NUMPAD9);

        getInput().addAction(new UserAction("Fire X") {
            @Override
            protected void onActionBegin() {
                onFirePressed();
            }
            @Override
            protected void onAction() {
                holdingFire = true;
                if (!gameOver && !minigameActive && findActiveMissile() == null && getb(GameVars.AUTOFIRE)) {
                    fire();
                }
            }
            @Override
            protected void onActionEnd() {
                holdingFire = false;
            }
        }, KeyCode.X);

        getInput().addAction(new UserAction("Minigame Bonus") {
            @Override
            protected void onActionBegin() {
                if (minigameActive && activeMinigame != null) {
                    activeMinigame.onBonusKey();
                }
            }
        }, KeyCode.B);

        getInput().addAction(new UserAction("Minigame Bonus Click") {
            @Override
            protected void onActionBegin() {
                if (minigameActive && activeMinigame != null) {
                    activeMinigame.onBonusKey();
                }
            }
        }, MouseButton.SECONDARY);

        getInput().addAction(new UserAction("Restart") {
            @Override
            protected void onActionBegin() {
                if (gameOver && !optionsOpen && !enteringHighScoreName()) {
                    restartGame();
                }
            }
        }, KeyCode.R);
    }

    private void bindTestDigit(String name, KeyCode... codes) {
        for (int i = 0; i < codes.length; i++) {
            KeyCode code = codes[i];
            getInput().addAction(new UserAction(name + " " + code) {
                @Override
                protected void onActionBegin() {
                    TestMode.feed(code).ifPresent(DeltaBladeApp.this::dropTestMinigame);
                }
            }, code);
        }
    }

    private static boolean initialWindowSizeSet = false;

    @Override
    protected void initGame() {
        getGameScene().clearUINodes();

        gameOver = false;
        movingLeft = false;
        movingRight = false;
        waveTransition = false;
        getReadyRemaining = 0;
        getReadySoundQueued = false;
        showingTitleScreen = true;
        gameStarted = false;
        optionsOpen = false;
        enginePausedByOptions = false;
        optionsRoot = null;
        highScoreOverlay = null;
        highScoreRoot = null;
        player = null;
        waveManager = null;
        minigameActive = false;
        activeMinigame = null;
        stopWarpVisuals();
        holdingFire = false;
        lastMissileTapAt = 0;
        meteorMissileCooldown = 0;
        movingUp = false;
        movingDown = false;
        frameCount = 0;

        getGameWorld().addEntityFactory(new DeltaBladeFactory());
        applySystemCursor();
        installTestKeyFilter();

        getGameScene().getViewport().setX(0);
        getGameScene().getViewport().setY(0);

        if (!initialWindowSizeSet) {
            initialWindowSizeSet = true;
            setInitialWindowSize();
        }

        spawn("background", new com.almasb.fxgl.entity.SpawnData(0, 0)
                .put("width", getAppWidth())
                .put("height", getAppHeight()));

        spawnStars();
        spawnSideRails();

        showTitleScreen();
    }

    private void setInitialWindowSize() {
        Platform.runLater(() -> {
            javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
            double screenHeight = screen.getVisualBounds().getHeight();
            double screenWidth = screen.getVisualBounds().getWidth();

            double margin = 120;
            double availableHeight = screenHeight - margin;
            double availableWidth = screenWidth - margin;

            double maxScaleByHeight = availableHeight / 600.0;
            double maxScaleByWidth = availableWidth / 800.0;
            double maxScale = Math.min(maxScaleByHeight, maxScaleByWidth);

            int integerScale = (int) maxScale;
            double scale;
            if (integerScale >= 2) {
                scale = integerScale;
            } else if (maxScale >= 1.5) {
                scale = Math.floor(maxScale * 2) / 2;
            } else {
                scale = Math.max(1.0, maxScale);
            }

            double windowWidth = 800 * scale;
            double windowHeight = 600 * scale;

            javafx.stage.Stage stage = getPrimaryStage();
            stage.setWidth(windowWidth);
            stage.setHeight(windowHeight);
            stage.centerOnScreen();
        });
    }

    private void showTitleScreen() {
        titleScreenNodes.clear();

        String preloadError = EmbeddedTextures.preloadAll();

        Rectangle overlay = new Rectangle(getAppWidth(), getAppHeight());
        overlay.setFill(Color.rgb(0, 0, 0, 0.85));

        Text title = new Text("DELTABLADE");
        title.setFont(Font.font("Monospace", FontWeight.BOLD, 56));
        title.setFill(Color.CYAN);
        title.setStroke(Color.WHITE);
        title.setStrokeWidth(2);

        DropShadow titleGlow = new DropShadow(20, Color.CYAN);
        Glow glow = new Glow(0.6);
        glow.setInput(titleGlow);
        title.setEffect(glow);

        centerHorizontally(title, 150);

        Text subtitle = new Text(I18n.tr("menu.subtitle"));
        subtitle.setFont(Font.font("Monospace", 16));
        subtitle.setFill(Color.LIGHTGRAY);
        centerHorizontally(subtitle, 188);

        Text versionLine = new Text(AppVersion.label());
        versionLine.setFont(Font.font("Monospace", 12));
        versionLine.setFill(Color.rgb(120, 160, 190));
        centerHorizontally(versionLine, 208);

        titleHiGlobal = false;
        titleHiLine = new Text(titleHiText(false));
        titleHiLine.setFont(Font.font("Monospace", FontWeight.BOLD, 14));
        titleHiLine.setFill(Color.GOLD);
        centerHorizontally(titleHiLine, 226);

        Button startButton = createMenuButton(I18n.tr("menu.start"));
        startButton.setOnAction(e -> startActualGame());

        Button optionsButton = createMenuButton(I18n.tr("menu.options"));
        optionsButton.setOnAction(e -> showOptions());

        Button hiscoreButton = createMenuButton(I18n.tr("menu.hiscore"));
        hiscoreButton.setOnAction(e -> showHighScoreTable(null, null, true, true));

        VBox menuBox = new VBox(12, startButton, optionsButton, hiscoreButton);
        menuBox.setAlignment(Pos.CENTER);
        menuBox.setPrefWidth(260);
        menuBox.setTranslateX(getAppWidth() / 2.0 - 130);
        menuBox.setTranslateY(240);

        Text controls = new Text(I18n.tr("menu.controls"));
        controls.setFont(Font.font("Monospace", 12));
        controls.setFill(Color.GRAY);
        centerHorizontally(controls, 455);

        Text extraInfo = new Text(I18n.tr("menu.extraInfo"));
        extraInfo.setFont(Font.font("Monospace", 12));
        extraInfo.setFill(Color.GOLD);
        centerHorizontally(extraInfo, 478);

        Text optionsHint = new Text(I18n.tr("menu.optionsHint"));
        optionsHint.setFont(Font.font("Monospace", 11));
        optionsHint.setFill(Color.rgb(120, 160, 190));
        centerHorizontally(optionsHint, 500);

        titleScreenNodes.add(overlay);
        titleScreenNodes.add(title);
        titleScreenNodes.add(subtitle);
        titleScreenNodes.add(versionLine);
        titleScreenNodes.add(titleHiLine);
        titleScreenNodes.add(menuBox);
        titleScreenNodes.add(controls);
        titleScreenNodes.add(extraInfo);
        titleScreenNodes.add(optionsHint);

        for (Node node : titleScreenNodes) {
            getGameScene().addUINode(node);
        }

        installEscFilter();
        MusicHelper.applyFromStore();
        SoundHelper.warmup();

        if (preloadError != null) {
            showBanner(preloadError, Color.RED);
        }
        startTitleHiRotate();
    }

    private void centerHorizontally(Node node, double y) {
        var bounds = node.getLayoutBounds();
        node.setTranslateX((getAppWidth() - bounds.getWidth()) / 2.0 - bounds.getMinX());
        node.setTranslateY(y);
    }

    private Button createMenuButton(String label) {
        Button button = new Button(label);
        button.setFont(Font.font("Monospace", FontWeight.BOLD, 20));
        button.setPrefWidth(240);
        String base =
            "-fx-background-color: linear-gradient(to bottom, #2a5298, #1e3c72);" +
            "-fx-text-fill: white;" +
            "-fx-padding: 10 40;" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: #4a90d9;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 8;" +
            "-fx-cursor: hand;";
        String hover =
            "-fx-background-color: linear-gradient(to bottom, #3a6ab8, #2e4c82);" +
            "-fx-text-fill: white;" +
            "-fx-padding: 10 40;" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: #6ab0f9;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 8;" +
            "-fx-cursor: hand;";
        button.setStyle(base);
        button.setOnMouseEntered(e -> button.setStyle(hover));
        button.setOnMouseExited(e -> button.setStyle(base));
        return button;
    }

    /**
     * La imagen de cursor que trae FXGL son píxeles corruptos, no un puntero real.
     * Se sustituye por el cursor del sistema para no mostrar una mancha gigante.
     */
    private void applySystemCursor() {
        getGameScene().setCursor(Cursor.DEFAULT);
        Platform.runLater(() -> {
            var scene = getPrimaryStage().getScene();
            if (scene != null) {
                scene.setCursor(Cursor.DEFAULT);
            }
        });
    }

    private void installEscFilter() {
        if (escFilterInstalled) {
            return;
        }
        Platform.runLater(() -> {
            var scene = getPrimaryStage().getScene();
            if (scene == null) {
                return;
            }
            scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                if (settlementOverlay != null) {
                    KeyCode code = e.getCode();
                    if (code == KeyCode.SPACE || code == KeyCode.ENTER || code == KeyCode.X) {
                        settlementOverlay.handleConfirm();
                        e.consume();
                        return;
                    }
                    e.consume();
                    return;
                }
                if (shopOverlay != null) {
                    if (shopOverlay.handleKey(e)) {
                        e.consume();
                        return;
                    }
                }
                if (highScoreOverlay != null) {
                    KeyCode code = e.getCode();
                    boolean fireHeld = holdingFire && (code == KeyCode.SPACE || code == KeyCode.X);
                    if (enteringHighScoreName() && fireHeld) {
                        e.consume();
                        return;
                    }
                    if (highScoreOverlay.handleKey(e)) {
                        e.consume();
                        return;
                    }
                }
                if (enteringHighScoreName() || shopOpen) {
                    e.consume();
                    return;
                }
                if (e.getCode() == KeyCode.ESCAPE) {
                    toggleOptions();
                    e.consume();
                }
            });
            escFilterInstalled = true;
        });
    }

    private void installTestKeyFilter() {
        var root = getGameScene().getRoot();
        if (Boolean.TRUE.equals(root.getProperties().get("dbTestKeys"))) {
            return;
        }
        root.getProperties().put("dbTestKeys", true);
        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            TestMode.feed(e.getCode()).ifPresent(this::dropTestMinigame);
        });
    }

    private void toggleOptions() {
        if (optionsOpen) {
            hideOptions();
        } else {
            showOptions();
        }
    }

    private void showOptions() {
        if (optionsOpen || enteringHighScoreName() || shopOpen || settlementOverlay != null) {
            return;
        }
        OptionsOverlay overlay = new OptionsOverlay(this::hideOptions);
        optionsRoot = overlay.getRoot();
        getGameScene().addUINode(optionsRoot);
        optionsOpen = true;

        if (gameStarted && !showingTitleScreen) {
            getGameController().pauseEngine();
            enginePausedByOptions = true;
        }
    }

    private void hideOptions() {
        if (!optionsOpen) {
            return;
        }
        if (optionsRoot != null) {
            getGameScene().removeUINode(optionsRoot);
            optionsRoot = null;
        }
        optionsOpen = false;
        if (enginePausedByOptions) {
            getGameController().resumeEngine();
            enginePausedByOptions = false;
        }
    }

    /**
     * Muestra un aviso de alto contraste a todo lo ancho, en la parte superior de la pantalla.
     * Se ve sobre la pantalla de título y durante la partida.
     */
    private void showBanner(String message, Color textColor) {
        showBanner(message, textColor, 2.0);
    }

    private void showBanner(String message, Color textColor, double durationSeconds) {
        int railWidth = GameVars.RAIL_WIDTH;
        int innerWidth = getAppWidth() - 2 * railWidth;

        Rectangle bar = new Rectangle(innerWidth, 48);
        bar.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(8, 16, 28, 0.96)),
                new Stop(0.5, Color.rgb(16, 28, 44, 0.96)),
                new Stop(1, Color.rgb(8, 16, 28, 0.96))));
        bar.setStroke(textColor);
        bar.setStrokeWidth(1.5);
        bar.setTranslateX(railWidth);
        bar.setTranslateY(48);

        Rectangle topLine = new Rectangle(innerWidth, 2);
        topLine.setFill(textColor);
        topLine.setOpacity(0.85);
        topLine.setTranslateX(railWidth);
        topLine.setTranslateY(48);

        Rectangle bottomLine = new Rectangle(innerWidth, 2);
        bottomLine.setFill(textColor);
        bottomLine.setOpacity(0.85);
        bottomLine.setTranslateX(railWidth);
        bottomLine.setTranslateY(94);

        Text text = new Text(message);
        text.setFont(Font.font("Monospace", FontWeight.BOLD, 26));
        text.setFill(textColor);
        text.setStroke(Color.rgb(0, 0, 0, 0.8));
        text.setStrokeWidth(1.2);
        DropShadow glow = new DropShadow(18, textColor);
        glow.setSpread(0.25);
        text.setEffect(glow);

        double textWidth = text.getLayoutBounds().getWidth();
        text.setTranslateX(railWidth + (innerWidth - textWidth) / 2);
        text.setTranslateY(80);

        Group banner = new Group(bar, topLine, bottomLine, text);

        activeBanners.add(banner);
        getGameScene().addUINode(banner);

        // Timeline de JavaFX: tiene que funcionar aunque el motor del juego esté en pausa (tienda/opciones).
        PauseTransition hide = new PauseTransition(Duration.seconds(durationSeconds));
        hide.setOnFinished(e -> {
            getGameScene().removeUINode(banner);
            activeBanners.remove(banner);
        });
        hide.play();
    }

    private void hideTitleScreen() {
        stopTitleHiRotate();
        for (Node node : titleScreenNodes) {
            getGameScene().removeUINode(node);
        }
        titleScreenNodes.clear();
        titleHiLine = null;
    }

    private void startActualGame() {
        startActualGame(true);
    }

    private void startActualGame(boolean getReady) {
        hideOptions();
        hideHighScoreOverlay();
        hideTitleScreen();
        showingTitleScreen = false;
        gameStarted = true;
        frameCount = 0;

        resetGameVars();

        waveManager = new WaveManager();
        spawnPlayer();
        if (getReady) {
            startGetReady();
        } else {
            beginWave();
        }
    }

    private void resetGameVars() {
        set(GameVars.SCORE, 0);
        set(GameVars.LIVES, GameVars.INITIAL_LIVES);
        set(GameVars.LEVEL, 1);
        set(GameVars.WEAPON_GRADE, 1);
        set(GameVars.AMMO_CAP, GameVars.INITIAL_AMMO_CAP);
        set(GameVars.ACTIVE_BULLETS, 0);
        set(GameVars.ENEMIES_REMAINING, 0);
        set(GameVars.MONEY, 0);
        set(GameVars.AUTOFIRE, false);
        set(GameVars.EXTRA_LETTER_SPAWNED_THIS_WAVE, 0);
        set(GameVars.SQUAD_COMBOS, 0);
        set(GameVars.COGNITIVE_WINS, 0);
        alienBossCount = 0;
        alienBossPending = false;
        set(GameVars.METEOR_WINS, 0);
        set(GameVars.MAZE_WINS, 0);
        set(GameVars.RANK, 0);
        set(GameVars.RANK_MASK, 0);
        set(GameVars.SHOTS_FIRED, 0);
        set(GameVars.HITS_LANDED, 0);
        set(GameVars.SHOP_WEAPON, 0);
        set(GameVars.BOSS_KILLS, 0);
        set(GameVars.BONUS_PERFECT_CLEARS, 0);
        controlReverseRemaining = 0;
        fireJamRemaining = 0;
        clearLaserBeams();
        laserHeat = 0;
        laserCooldown = 0;
        laserTickTimer = 0;
        java.util.Arrays.fill(laserTickTimers, 0);
        laserShotCounted = false;
        sweepAngle = 0;
        sweepActive = 0;
        sweepPause = 0;
        hideShopOverlay();
        hideSettlementOverlay();
        extraTimeMax = GameVars.LEVEL_TIME_INITIAL;
        refillWaveTimer();
        if (player != null && player.hasComponent(PlayerComponent.class)) {
            player.getComponent(PlayerComponent.class).clearShield();
        }
        clearScoops();
        updateShieldHud();
        ufoSpawnedThisWave = false;
        for (String var : GameVars.EXTRA_VARS) {
            set(var, 0);
        }
        hurryUfoDelay = 0;
        pendingHurryUfo = false;
        updateRankHud();
    }

    private void spawnSideRails() {
        spawn("sideRail", new com.almasb.fxgl.entity.SpawnData(0, 0)
                .put("height", getAppHeight())
                .put("isLeft", true));
        spawn("sideRail", new com.almasb.fxgl.entity.SpawnData(getAppWidth() - GameVars.RAIL_WIDTH, 0)
                .put("height", getAppHeight())
                .put("isLeft", false));
    }

    private void spawnStars() {
        for (int i = 0; i < 80; i++) {
            double x = RANDOM.nextDouble() * getAppWidth();
            double y = RANDOM.nextDouble() * getAppHeight();
            double size = 1.0 + RANDOM.nextDouble() * 1.2;
            double opacity = 0.25 + RANDOM.nextDouble() * 0.35;
            double scrollSpeed = 15 + RANDOM.nextDouble() * 10;

            spawn("scrollingStar", new com.almasb.fxgl.entity.SpawnData(x, y)
                    .put("size", size)
                    .put("opacity", opacity)
                    .put("scrollSpeed", scrollSpeed)
                    .put("isNear", false));
        }

        for (int i = 0; i < 40; i++) {
            double x = RANDOM.nextDouble() * getAppWidth();
            double y = RANDOM.nextDouble() * getAppHeight();
            double size = 1.5 + RANDOM.nextDouble() * 2.0;
            double opacity = 0.5 + RANDOM.nextDouble() * 0.5;
            double scrollSpeed = 35 + RANDOM.nextDouble() * 25;

            spawn("scrollingStar", new com.almasb.fxgl.entity.SpawnData(x, y)
                    .put("size", size)
                    .put("opacity", opacity)
                    .put("scrollSpeed", scrollSpeed)
                    .put("isNear", true));
        }
    }

    private void spawnPlayer() {
        player = spawn("player", getAppWidth() / 2 - 20, getAppHeight() - 60);
    }

    private void startGetReady() {
        if (gameOver || showingTitleScreen || waveManager == null) {
            return;
        }
        waveTransition = true;
        getReadyRemaining = GET_READY_DURATION;
        getReadySoundQueued = true;
        refillWaveTimer();
        ufoSpawnedThisWave = false;
        showWaveAnnouncement(WaveManager.resolveWaveType(geti(GameVars.LEVEL)));
    }

    private void cancelGetReady() {
        getReadyRemaining = 0;
        getReadySoundQueued = false;
    }

    private void updateGetReady(double tpf) {
        if (getReadyRemaining <= 0) {
            return;
        }
        if (getReadySoundQueued) {
            getReadySoundQueued = false;
            playWaveIntroSound(WaveManager.resolveWaveType(geti(GameVars.LEVEL)));
        }
        getReadyRemaining -= tpf;
        if (getReadyRemaining > 0) {
            return;
        }
        getReadyRemaining = 0;
        beginWave();
    }

    private void beginWave() {
        if (waveManager == null || gameOver || showingTitleScreen || warping || minigameActive) {
            return;
        }
        cancelGetReady();
        waveTransition = false;
        refillWaveTimer();
        ufoSpawnedThisWave = false;
        bossCollectLeft = 0;
        bossCollectDone = false;
        meteorMissileCooldown = 0.35;
        waveManager.startWave(geti(GameVars.LEVEL));
    }

    private void showWaveAnnouncement() {
        WaveManager.WaveType waveType = waveManager != null
                ? waveManager.getCurrentWaveType()
                : WaveManager.resolveWaveType(geti(GameVars.LEVEL));
        showWaveAnnouncement(waveType);
    }

    private void showWaveAnnouncement(WaveManager.WaveType waveType) {
        String message;
        Color color;

        switch (waveType) {
            case BOSS -> {
                message = I18n.tr("wave.boss", geti(GameVars.LEVEL));
                color = Color.MAGENTA;
            }
            case KAMIKAZE -> {
                message = I18n.tr("wave.kamikaze", geti(GameVars.LEVEL));
                color = Color.ORANGERED;
            }
            case BONUS -> {
                message = I18n.tr("wave.bonus", geti(GameVars.LEVEL));
                color = Color.GOLD;
            }
            case RANK -> {
                message = I18n.tr("wave.rank", geti(GameVars.LEVEL));
                color = Color.HOTPINK;
            }
            case PEARL -> {
                message = I18n.tr("wave.attack", geti(GameVars.LEVEL));
                color = Color.AQUA;
            }
            case MONSTER -> {
                message = I18n.tr("wave.swarm", geti(GameVars.LEVEL));
                color = Color.MEDIUMPURPLE;
            }
            case SPINNER -> {
                message = I18n.tr("wave.spinner", geti(GameVars.LEVEL));
                color = Color.LIGHTGREEN;
            }
            case PINCER -> {
                message = I18n.tr("wave.pincer", geti(GameVars.LEVEL));
                color = Color.DEEPSKYBLUE;
            }
            case CASCADE -> {
                message = I18n.tr("wave.cascade", geti(GameVars.LEVEL));
                color = Color.LIGHTSKYBLUE;
            }
            case RUSH -> {
                message = I18n.tr("wave.rush", geti(GameVars.LEVEL));
                color = Color.ORANGE;
            }
            case METEOR -> {
                message = I18n.tr("wave.meteor");
                color = Color.SANDYBROWN;
            }
            default -> {
                message = I18n.tr("wave.normal", geti(GameVars.LEVEL));
                color = Color.YELLOW;
            }
        }

        showBanner(message, color, GET_READY_DURATION);
    }

    private void playWaveIntroSound(WaveManager.WaveType waveType) {
        if (waveType == WaveManager.WaveType.BONUS) {
            SoundHelper.play("bonus_round.wav");
        } else if (waveType == WaveManager.WaveType.RANK) {
            SoundHelper.play("rank_wave.wav");
        } else {
            SoundHelper.play("get_ready.wav");
        }
    }

    public void showBonusPerfect(int bonus) {
        boolean roundClear = bonus >= GameVars.BONUS_ROUND_PERFECT_SCORE;
        showBanner(I18n.tr("banner.perfect"), Color.GOLD, roundClear ? 2.2 : 1.6);
        showScorePopup(bonus, Color.GOLD, roundClear ? 48 : 36);
        SoundHelper.play("bonus_perfect.wav");
    }

    public void showSquadCombo(int bonus) {
        showBanner(I18n.tr("banner.squadCombo"), Color.ORANGE, 1.8);
        showScorePopup(bonus, Color.ORANGE, 36);
        if (comboPlate != null) {
            comboPlate.setStroke(Color.GOLD);
            comboPlate.setStrokeWidth(2);
            runOnce(() -> {
                if (comboPlate != null) {
                    comboPlate.setStroke(Color.rgb(255, 160, 40, 0.45));
                    comboPlate.setStrokeWidth(1.2);
                }
            }, Duration.seconds(1.8));
        }
    }

    private void showScorePopup(int amount, Color color, double fontSize) {
        Text text = new Text("+" + String.format(Locale.US, "%,d", amount));
        text.setFont(Font.font("Monospace", FontWeight.BOLD, fontSize));
        text.setFill(Color.WHITE);
        text.setStroke(color);
        text.setStrokeWidth(1.8);
        DropShadow glow = new DropShadow(32, color);
        glow.setSpread(0.5);
        text.setEffect(glow);

        double width = text.getLayoutBounds().getWidth();
        text.setTranslateX(-width / 2.0);

        Group popup = new Group(text);
        popup.setMouseTransparent(true);
        popup.setTranslateX(getAppWidth() / 2.0);
        popup.setTranslateY(170 + (scorePopupSeq % 3) * 22);
        popup.setScaleX(0.45);
        popup.setScaleY(0.45);
        scorePopupSeq++;

        getGameScene().addUINode(popup);
        activeBanners.add(popup);

        ScaleTransition pop = new ScaleTransition(Duration.millis(160), popup);
        pop.setToX(1.2);
        pop.setToY(1.2);
        pop.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition settle = new ScaleTransition(Duration.millis(110), popup);
        settle.setToX(1.0);
        settle.setToY(1.0);

        TranslateTransition rise = new TranslateTransition(Duration.seconds(1.2), popup);
        rise.setByY(-64);
        rise.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fade = new FadeTransition(Duration.seconds(0.85), popup);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setDelay(Duration.millis(350));

        SequentialTransition bump = new SequentialTransition(pop, settle);
        ParallelTransition all = new ParallelTransition(bump, rise, fade);
        extraLetterAnimations.add(all);
        all.setOnFinished(e -> {
            extraLetterAnimations.remove(all);
            getGameScene().removeUINode(popup);
            activeBanners.remove(popup);
        });
        all.play();
    }

    private static final double BULLET_SPEED = -500;

    private void onFirePressed() {
        holdingFire = true;
        if (gameOver) {
            return;
        }
        // Los minijuegos deben recibir Espacio/X aunque el misil dirigido se los fuera a quedar.
        if (minigameActive) {
            if (activeMinigame != null) {
                activeMinigame.onFirePress();
            }
            return;
        }
        if (tryMissileFirePress()) {
            return;
        }
        fire();
    }

    private boolean tryMissileFirePress() {
        Entity missile = findActiveMissile();
        if (missile == null) {
            lastMissileTapAt = 0;
            return false;
        }
        long now = System.nanoTime();
        if (lastMissileTapAt > 0 && (now - lastMissileTapAt) / 1_000_000_000.0 <= GameVars.MISSILE_DOUBLE_TAP) {
            lastMissileTapAt = 0;
            if (missile.hasComponent(MissileComponent.class)) {
                missile.getComponent(MissileComponent.class).requestDetonate();
            }
            return true;
        }
        lastMissileTapAt = now;
        return true;
    }

    private Entity findActiveMissile() {
        for (Entity missile : getGameWorld().getEntitiesByType(EntityType.PLAYER_MISSILE)) {
            if (missile.isActive() && missile.hasComponent(MissileComponent.class)
                    && !missile.getComponent(MissileComponent.class).isDetonated()) {
                return missile;
            }
        }
        return null;
    }

    private void launchPlayerMissile() {
        if (player == null || !player.isActive() || !player.hasComponent(PlayerComponent.class)) {
            return;
        }
        Entity live = findActiveMissile();
        if (live != null) {
            return;
        }
        PlayerComponent pc = player.getComponent(PlayerComponent.class);
        spawn("playerMissile", new com.almasb.fxgl.entity.SpawnData(pc.getCenterX() - 16, pc.getTopY() - 28));
        pc.setMissileShield(true);
        lastMissileTapAt = 0;
        SoundHelper.play("missile_launch.wav");
    }

    public void detonatePlayerMissile(Entity missile) {
        if (missile == null || !missile.isActive()) {
            return;
        }
        double cx = missile.getX() + missile.getWidth() / 2.0;
        double cy = missile.getY() + missile.getHeight() / 2.0;
        missile.removeFromWorld();
        lastMissileTapAt = 0;
        dropMissileShield();
        spawnExplosion(cx, cy, "ship");
        spawnShockwaveRing(cx, cy, "ship");
        triggerScreenShake("ship");
        SoundHelper.play("explode_ship.wav");

        double radius = GameVars.MISSILE_BLAST_RADIUS;
        double radiusSq = radius * radius;
        for (Entity enemy : List.copyOf(getGameWorld().getEntitiesByType(EntityType.ENEMY))) {
            if (!enemy.isActive() || !enemy.hasComponent(EnemyComponent.class)) {
                continue;
            }
            EnemyComponent ec = enemy.getComponent(EnemyComponent.class);
            if (ec.isScooped()) {
                continue;
            }
            double ex = enemy.getX() + enemy.getWidth() / 2.0;
            double ey = enemy.getY() + enemy.getHeight() / 2.0;
            double dx = ex - cx;
            double dy = ey - cy;
            if (dx * dx + dy * dy > radiusSq) {
                continue;
            }
            boolean chunky = ec.getType() == EnemyComponent.EnemyType.TOUGH || ec.isBoss();
            ec.hit(GameVars.MISSILE_BLAST_HITS);
            if (chunky && !ec.isDead()) {
                spawnExplosion(ex, ey, "boss_hit");
                spawnChipDamageSparks(ex, ey);
                flashEnemySprite(enemy);
            } else if (!ec.isDead()) {
                spawnExplosion(ex, ey, "hit");
            }
            if (ec.isDead()) {
                finishEnemyKill(enemy, ec);
            }
        }
    }

    private void missileHitMeteor(Entity missile, Entity meteor) {
        if (missile == null || !missile.isActive() || meteor == null || !meteor.isActive()) {
            return;
        }
        if (missile.hasComponent(MissileComponent.class)) {
            if (missile.getComponent(MissileComponent.class).isDetonated()) {
                return;
            }
            missile.getComponent(MissileComponent.class).markDetonated();
        }
        double cx = missile.getX() + missile.getWidth() / 2.0;
        double cy = missile.getY() + missile.getHeight() / 2.0;
        missile.removeFromWorld();
        lastMissileTapAt = 0;
        dropMissileShield();
        spawnExplosion(cx, cy, "hit");
        SoundHelper.play("explode_ship.wav");
        if (!meteor.hasComponent(CoverMeteorComponent.class)) {
            return;
        }
        CoverMeteorComponent rock = meteor.getComponent(CoverMeteorComponent.class);
        if (!rock.hit()) {
            return;
        }
        double mx = meteor.getX() + meteor.getWidth() / 2.0;
        double my = meteor.getY() + meteor.getHeight() / 2.0;
        meteor.removeFromWorld();
        spawnExplosion(mx, my, "ship");
        spawnShockwaveRing(mx, my, "ship");
        inc(GameVars.SCORE, 75);
    }

    private void fire() {
        if (minigameActive) {
            if (activeMinigame != null) {
                activeMinigame.onFirePress();
            }
            return;
        }
        if (findActiveMissile() != null) {
            return;
        }
        if (player == null || gameOver || showingTitleScreen || optionsOpen || warping || shopOpen) {
            return;
        }
        if (fireJamRemaining > 0) {
            return;
        }

        ShopWeapon shopWeapon = currentShopWeapon();
        if (shopWeapon == ShopWeapon.PLASMA_ORB || shopWeapon == ShopWeapon.ARC_LANCE) {
            fireShopImpulse(shopWeapon);
            return;
        }
        if (shopWeapon == ShopWeapon.NEEDLE_LASER
                || shopWeapon == ShopWeapon.TWIN_BEAM
                || shopWeapon == ShopWeapon.SWEEP_LASER) {
            // Los rayos continuos se controlan desde onUpdate mientras se mantiene el disparo o el autodisparo.
            return;
        }

        PlayerComponent pc = player.getComponent(PlayerComponent.class);
        int grade = geti(GameVars.WEAPON_GRADE);

        if (!pc.canFire(grade)) {
            return;
        }

        pc.onFired();
        SoundHelper.play("shot.wav");

        double centerX = pc.getCenterX();
        double topY = pc.getTopY();

        if (grade == 1) {
            spawnPlayerShot(centerX - 6, topY, 0);
        } else if (grade == 2) {
            spawnPlayerShot(centerX - 12, topY, 0);
            spawnPlayerShot(centerX + 4, topY, 0);
        } else if (grade == 3) {
            spawnPlayerShot(centerX - 10, topY, -16);
            spawnPlayerShot(centerX - 6, topY, 0);
            spawnPlayerShot(centerX + 2, topY, 16);
        } else {
            spawnPlayerShot(centerX - 14, topY, -20);
            spawnPlayerShot(centerX - 8, topY, -6);
            spawnPlayerShot(centerX, topY, 6);
            spawnPlayerShot(centerX + 6, topY, 20);
        }
        fireDockedEscorts();
    }

    private void fireDockedEscorts() {
        for (Entity escort : List.copyOf(getGameWorld().getEntitiesByType(EntityType.ENEMY))) {
            if (!escort.isActive() || !escort.hasComponent(EnemyComponent.class)) {
                continue;
            }
            EnemyComponent ec = escort.getComponent(EnemyComponent.class);
            if (!ec.isDocked()) {
                continue;
            }
            spawnPlayerShot(escort.getX() + escort.getWidth() / 2.0 - 4, escort.getY() - 14, 0);
        }
    }

    private void spawnPlayerShot(double x, double y, double angleDeg) {
        double rad = Math.toRadians(angleDeg);
        double speed = Math.abs(BULLET_SPEED);
        spawnBullet(x, y, Math.sin(rad) * speed, -Math.cos(rad) * speed);
    }

    private void spawnBullet(double x, double y, double speedX, double speedY) {
        spawn("playerBullet", new com.almasb.fxgl.entity.SpawnData(x, y)
                .put("speedX", speedX)
                .put("speedY", speedY));
        inc(GameVars.ACTIVE_BULLETS, 1);
        inc(GameVars.SHOTS_FIRED, 1);
    }

    @Override
    public void spawnExplosion(double centerX, double centerY, String size) {
        spawn("explosion", new com.almasb.fxgl.entity.SpawnData(centerX, centerY)
                .put("size", size));

        if ("ship".equals(size) || "big".equals(size)) {
            spawnShockwaveRing(centerX, centerY, size);
            triggerScreenShake(size);
        }

        if ("big".equals(size)) {
            spawnFlashOverlay();
        }
    }

    private void spawnChipDamageSparks(double hitX, double hitY) {
        for (int i = 0; i < 2; i++) {
            double offsetX = (RANDOM.nextDouble() - 0.5) * 2 * (8 + RANDOM.nextDouble() * 10);
            double offsetY = (RANDOM.nextDouble() - 0.5) * 2 * (8 + RANDOM.nextDouble() * 10);
            long delayMs = (long) (RANDOM.nextDouble() * 50);

            runOnce(() -> {
                spawnExplosion(hitX + offsetX, hitY + offsetY, "hit");
            }, Duration.millis(delayMs));
        }
    }

    private void flashEnemySprite(Entity enemy) {
        if (enemy == null || !enemy.isActive()) return;

        Node view = enemy.getViewComponent().getChildren().isEmpty()
            ? null : enemy.getViewComponent().getChildren().get(0);
        if (view == null) return;

        javafx.scene.effect.ColorAdjust flash = new javafx.scene.effect.ColorAdjust();
        flash.setBrightness(0.7);
        view.setEffect(flash);

        runOnce(() -> {
            if (enemy.isActive()) {
                view.setEffect(null);
            }
        }, Duration.millis(80));
    }

    private void spawnShockwaveRing(double centerX, double centerY, String size) {
        double maxRadius = "big".equals(size) ? 160 : 110;
        double duration = "big".equals(size) ? 0.35 : 0.25;

        Circle ring = new Circle(10);
        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(Color.rgb(255, 200, 100, 0.9));
        ring.setStrokeWidth(4);

        Entity shockwaveEntity = entityBuilder()
                .at(centerX, centerY)
                .view(ring)
                .zIndex(95)
                .build();
        getGameWorld().addEntity(shockwaveEntity);

        ScaleTransition scale = new ScaleTransition(Duration.seconds(duration), ring);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(maxRadius / 10.0);
        scale.setToY(maxRadius / 10.0);
        scale.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fade = new FadeTransition(Duration.seconds(duration), ring);
        fade.setFromValue(0.9);
        fade.setToValue(0.0);

        Timeline strokeFade = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(ring.strokeWidthProperty(), 4)),
            new KeyFrame(Duration.seconds(duration), new KeyValue(ring.strokeWidthProperty(), 1))
        );

        ParallelTransition combo = new ParallelTransition(scale, fade, strokeFade);
        combo.setOnFinished(e -> {
            if (shockwaveEntity.isActive()) {
                shockwaveEntity.removeFromWorld();
            }
        });
        combo.play();
    }

    private void resetShakenViews() {
        for (Node node : shakenViewNodes) {
            node.setTranslateX(0);
            node.setTranslateY(0);
        }
        shakenViewNodes.clear();
    }

    private void triggerScreenShake(String size) {
        double intensity = "big".equals(size) ? 12 : 6;
        double shakeDuration = 0.18;
        int shakeSteps = 5;
        double stepDuration = shakeDuration / shakeSteps;

        if (activeShake != null) {
            activeShake.stop();
        }
        resetShakenViews();

        shakenViewNodes.clear();
        for (Entity e : getGameWorld().getEntitiesByType(EntityType.ENEMY)) {
            if (e.isActive()) shakenViewNodes.add(e.getViewComponent().getParent());
        }
        for (Entity e : getGameWorld().getEntitiesByType(EntityType.PLAYER)) {
            if (e.isActive()) shakenViewNodes.add(e.getViewComponent().getParent());
        }
        for (Entity e : getGameWorld().getEntitiesByType(EntityType.ENEMY_BULLET)) {
            if (e.isActive()) shakenViewNodes.add(e.getViewComponent().getParent());
        }
        for (Entity e : getGameWorld().getEntitiesByType(EntityType.PLAYER_BULLET)) {
            if (e.isActive()) shakenViewNodes.add(e.getViewComponent().getParent());
        }

        List<Node> currentNodes = new ArrayList<>(shakenViewNodes);

        Timeline shake = new Timeline();
        for (int i = 0; i < shakeSteps; i++) {
            double decay = 1.0 - (double) i / shakeSteps;
            final double offsetX = (RANDOM.nextDouble() - 0.5) * 2 * intensity * decay;
            final double offsetY = (RANDOM.nextDouble() - 0.5) * 2 * intensity * decay;
            shake.getKeyFrames().add(new KeyFrame(Duration.seconds(i * stepDuration),
                e -> {
                    for (Node node : currentNodes) {
                        node.setTranslateX(offsetX);
                        node.setTranslateY(offsetY);
                    }
                }
            ));
        }
        shake.getKeyFrames().add(new KeyFrame(Duration.seconds(shakeDuration),
            e -> {
                for (Node node : currentNodes) {
                    node.setTranslateX(0);
                    node.setTranslateY(0);
                }
            }
        ));

        shake.setOnFinished(e -> {
            for (Node node : currentNodes) {
                node.setTranslateX(0);
                node.setTranslateY(0);
            }
            if (activeShake == shake) {
                activeShake = null;
                shakenViewNodes.clear();
            }
        });

        activeShake = shake;
        shake.play();
    }

    private void spawnFlashOverlay() {
        Rectangle flash = new Rectangle(getAppWidth() + 40, getAppHeight() + 40);
        flash.setFill(Color.rgb(255, 220, 180, 0.10));
        flash.setMouseTransparent(true);

        Entity flashEntity = entityBuilder()
                .at(-20, -20)
                .view(flash)
                .zIndex(500)
                .build();
        getGameWorld().addEntity(flashEntity);

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.08), flash);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            if (flashEntity.isActive()) {
                flashEntity.removeFromWorld();
            }
        });
        fadeOut.play();
    }

    public void spawnEnemyBullet(double x, double y) {
        spawnEnemyBullet(x, y, 0, 250, false);
    }

    public void spawnEnemyShot(EnemyComponent.EnemyType type, double x, double y) {
        switch (type) {
            case FAST, TOUGH -> {
                if (geti(GameVars.LEVEL) < GameVars.ANGLED_FIRE_MIN_LEVEL) {
                    spawnEnemyBullet(x, y, 0, 250, false);
                    SoundHelper.play("enemy_shot.wav");
                } else {
                    spawnAngledEnemyShot(x, y);
                }
            }
            case MONSTER -> {
                String[] kinds = {"blob", "drip", "flame"};
                String kind = kinds[Math.abs(RANDOM.nextInt(kinds.length))];
                double drift = (RANDOM.nextDouble() - 0.5) * 70;
                spawnEnemyBullet(x, y, drift, 150 + RANDOM.nextDouble() * 40, false, kind);
                SoundHelper.play("enemy_shot.wav");
            }
            case UFO -> {
                spawnEnemyBullet(x, y, 0, 165, true);
                SoundHelper.play("missile_launch.wav");
            }
            case BOSS -> {
                spawnEnemyBullet(x, y, 0, 250, false);
                SoundHelper.play("enemy_shot.wav");
                if (RANDOM.nextDouble() < 0.55) {
                    spawnEnemyBullet(x, y, -90, 240, false);
                    spawnEnemyBullet(x, y, 90, 240, false);
                }
            }
            default -> {
                spawnEnemyBullet(x, y, 0, 250, false);
                SoundHelper.play("enemy_shot.wav");
            }
        }
    }

    private void spawnAngledEnemyShot(double x, double y) {
        double angle = Math.toRadians(22 + RANDOM.nextDouble() * 13);
        if (RANDOM.nextBoolean()) {
            angle = -angle;
        }
        try {
            var target = getGameWorld().getSingleton(EntityType.PLAYER);
            double dx = target.getX() + target.getWidth() / 2 - x;
            if (dx != 0) {
                angle = Math.copySign(angle, dx);
            }
        } catch (Exception ignored) {
        }
        double speed = 280;
        spawnEnemyBullet(x, y, Math.sin(angle) * speed, Math.cos(angle) * speed, false);
        SoundHelper.play("enemy_shot.wav");
    }

    private void spawnEnemyBullet(double x, double y, double speedX, double speedY, boolean homing) {
        spawnEnemyBullet(x, y, speedX, speedY, homing, "bolt");
    }

    private void spawnEnemyBullet(double x, double y, double speedX, double speedY, boolean homing, String kind) {
        spawn("enemyBullet", new com.almasb.fxgl.entity.SpawnData(x - 5, y)
                .put("speedX", speedX)
                .put("speedY", speedY)
                .put("homing", homing)
                .put("kind", kind));
    }

    public void bossAnnounce(String text) {
        showBanner(text, Color.ORANGERED, 1.4);
        SoundHelper.play("explode_ship.wav");
    }

    /** Jefe en fase 2+: pequeños ayudantes se lanzan sobre el jugador. */
    public void spawnBossHelpers(Entity boss, int count) {
        if (boss == null || !boss.isActive() || player == null) {
            return;
        }
        double cx = boss.getX() + boss.getWidth() / 2.0;
        double by = boss.getY() + boss.getHeight() * 0.7;
        int level = geti(GameVars.LEVEL);
        for (int i = 0; i < count; i++) {
            double ox = (i - (count - 1) / 2.0) * 40;
            inc(GameVars.ENEMIES_REMAINING, 1);
            spawn("enemy", new com.almasb.fxgl.entity.SpawnData(cx + ox - 24, by)
                    .put("enemyType", EnemyComponent.EnemyType.FAST)
                    .put("level", level)
                    .put("diveNow", true)
                    .put("diveTargetX", player.getX() + ox));
        }
    }

    private static final int[] ALIEN_BOSS_ROTATION = {
            SpriteSheets.ALIEN_CRAB, SpriteSheets.ALIEN_BRAIN, SpriteSheets.ALIEN_EYE};
    private int alienBossCount = 0;
    private boolean alienBossPending = false;

    /** Oleada de monstruos: un alienígena grande de la hoja nueva se une al combate (cangrejo, cerebro y ojo por turnos). */
    private void spawnAlienBoss() {
        alienBossPending = false;
        if (gameOver || minigameActive || warping || player == null
                || waveManager.getCurrentWaveType() != WaveManager.WaveType.MONSTER) {
            inc(GameVars.ENEMIES_REMAINING, -1);
            checkWaveComplete();
            return;
        }
        int kind = ALIEN_BOSS_ROTATION[alienBossCount % ALIEN_BOSS_ROTATION.length];
        alienBossCount++;
        int size = GameVars.BOSS_DRAW_SIZE;
        double tx = getAppWidth() / 2.0 - size / 2.0;
        int level = Math.max(geti(GameVars.LEVEL), GameVars.BOSS_EVERY);
        WaveManager.EntryPath path = WaveManager.bossEntry();
        spawn("enemy", new com.almasb.fxgl.entity.SpawnData(path.startX - size / 2.0, path.startY - size)
                .put("enemyType", EnemyComponent.EnemyType.BOSS)
                .put("level", level)
                .put("targetX", tx)
                .put("targetY", 50.0)
                .put("entryPath", path)
                .put("squadId", -1)
                .put("entering", true)
                .put("alienBoss", kind)
                .put("alienCycle", alienBossCount));
        refillWaveTimer();
        String name = kind == SpriteSheets.ALIEN_CRAB ? I18n.tr("boss.crab")
                : kind == SpriteSheets.ALIEN_BRAIN ? I18n.tr("boss.brain") : I18n.tr("boss.eye");
        showBanner(I18n.tr("banner.warning", name), Color.MEDIUMORCHID, 1.8);
        SoundHelper.play("ufo_appear.wav");
    }

    /** El pulpo cerebro llama a monstruos pequeños que se lanzan en picado sobre el jugador. */
    public void spawnAlienBossHelpers(Entity boss, int count) {
        if (boss == null || !boss.isActive() || player == null) {
            return;
        }
        double cx = boss.getX() + boss.getWidth() / 2.0;
        double by = boss.getY() + boss.getHeight() * 0.7;
        int level = geti(GameVars.LEVEL);
        for (int i = 0; i < count; i++) {
            double ox = (i - (count - 1) / 2.0) * 44;
            inc(GameVars.ENEMIES_REMAINING, 1);
            Entity child = spawn("enemy", new com.almasb.fxgl.entity.SpawnData(cx + ox - 24, by)
                    .put("enemyType", EnemyComponent.EnemyType.MONSTER)
                    .put("level", level)
                    .put("spriteIndex", RANDOM.nextInt(SpriteSheets.MONSTER_TILES))
                    .put("canSplit", false)
                    .put("diveNow", true)
                    .put("diveTargetX", player.getX() + ox));
            child.setScaleX(0.7);
            child.setScaleY(0.7);
        }
    }

    /** Disparo del jefe apuntado al jugador. */
    public void spawnAimedBossBullet(double x, double y, double speed) {
        if (player == null) {
            spawnEnemyBullet(x, y, 0, speed, false);
            return;
        }
        double dx = player.getX() + player.getWidth() / 2.0 - x;
        double dy = player.getY() + player.getHeight() / 2.0 - y;
        double d = Math.max(1, Math.sqrt(dx * dx + dy * dy));
        spawnEnemyBullet(x, y, dx / d * speed, dy / d * speed, false);
    }

    /** Disparo del jefe en cualquier dirección. */
    public void spawnBossBullet(double x, double y, double vx, double vy) {
        spawnEnemyBullet(x, y, vx, vy, false);
    }

    // --- Barra de vida del jefe en la parte superior --------------------------
    private Group bossBar;
    private javafx.scene.shape.Rectangle bossBarFill;
    private Text bossBarLabel;
    private static final double BOSS_BAR_W = 300;

    private void updateBossBar() {
        EnemyComponent boss = null;
        if (!gameOver && !showingTitleScreen && !minigameActive) {
            for (Entity e : getGameWorld().getEntitiesByType(EntityType.ENEMY)) {
                if (e.isActive() && e.hasComponent(EnemyComponent.class)
                        && e.getComponent(EnemyComponent.class).isBoss()) {
                    boss = e.getComponent(EnemyComponent.class);
                    break;
                }
            }
        }
        if (boss == null) {
            if (bossBar != null) {
                bossBar.setVisible(false);
            }
            return;
        }
        if (bossBar == null) {
            javafx.scene.shape.Rectangle bg = new javafx.scene.shape.Rectangle(BOSS_BAR_W + 4, 14);
            bg.setFill(Color.rgb(20, 10, 30, 0.85));
            bg.setStroke(Color.rgb(255, 120, 200));
            bg.setTranslateX(-2);
            bg.setTranslateY(-2);
            bossBarFill = new javafx.scene.shape.Rectangle(BOSS_BAR_W, 10);
            bossBarFill.setFill(Color.ORANGERED);
            bossBarLabel = new Text(I18n.tr("boss.bar"));
            bossBarLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 12));
            bossBarLabel.setFill(Color.WHITE);
            bossBarLabel.setTranslateY(-6);
            bossBar = new Group(bg, bossBarFill, bossBarLabel);
            bossBar.setMouseTransparent(true);
            bossBar.setTranslateX((getAppWidth() - BOSS_BAR_W) / 2.0);
            bossBar.setTranslateY(26);
            getGameScene().addUINode(bossBar);
        }
        double ratio = Math.max(0, boss.getHealth() / (double) Math.max(1, boss.getMaxHealth()));
        bossBarFill.setWidth(BOSS_BAR_W * ratio);
        bossBarFill.setFill(ratio > 0.5 ? Color.ORANGE : ratio > 0.25 ? Color.ORANGERED : Color.RED);
        bossBarLabel.setText(boss.bossLabel());
        bossBar.setVisible(true);
        // Al reiniciar se eliminan todos los nodos de la interfaz -> hay que volver a añadir la barra.
        if (bossBar.getParent() == null) {
            getGameScene().addUINode(bossBar);
        }
        // Dibujarla siempre encima de todo (los avisos, láseres y el HUD se añaden después).
        bossBar.toFront();
    }

    public void splitMonster(Entity parent, EnemyComponent parentEc) {
        if (parent == null || !parent.isActive() || parentEc == null) {
            return;
        }
        double cx = parent.getX() + parent.getWidth() / 2.0;
        double cy = parent.getY() + parent.getHeight() / 2.0;
        int squadId = parentEc.getSquadId();
        int sprite = parentEc.getSpriteIndex();
        int level = geti(GameVars.LEVEL);
        parent.removeFromWorld();
        inc(GameVars.ENEMIES_REMAINING, 1);
        spawnMonsterChild(cx - 22, cy + 8, -40, squadId, sprite, level);
        spawnMonsterChild(cx + 6, cy + 8, 40, squadId, sprite, level);
        SoundHelper.play("scoop.wav");
    }

    private void spawnMonsterChild(double x, double y, double diveOffset, int squadId, int sprite, int level) {
        Entity child = spawn("enemy", new com.almasb.fxgl.entity.SpawnData(x, y)
                .put("enemyType", EnemyComponent.EnemyType.MONSTER)
                .put("level", level)
                .put("spriteIndex", sprite)
                .put("canSplit", false)
                .put("diveNow", true)
                .put("diveTargetX", x + diveOffset)
                .put("squadId", squadId));
        child.setScaleX(0.62);
        child.setScaleY(0.62);
    }

    public void onSquadMemberSettled(int squadId) {
        waveManager.markSquadSettled(squadId);
    }

    public void onEnemyLeftScreen(int squadId) {
        waveManager.onEnemyEscaped(squadId);
        inc(GameVars.ENEMIES_REMAINING, -1);
        waveManager.checkKillBonuses();
        checkWaveComplete();
    }

    @Override
    protected void initPhysics() {
        onCollisionBegin(EntityType.PLAYER_BULLET, EntityType.ENEMY, (bullet, enemy) -> {
            EnemyComponent ec = enemy.getComponent(EnemyComponent.class);
            if (ec.isScooped()) {
                return;
            }
            if (ec.isTuckedBehindMeteor()) {
                return;
            }
            if (shotBlockedByMeteor(bullet)) {
                if (bullet.isActive() && bullet.hasComponent(BulletComponent.class)) {
                    bullet.getComponent(BulletComponent.class).onHit();
                }
                if (bullet.isActive()) {
                    bullet.removeFromWorld();
                }
                return;
            }
            bullet.getComponent(BulletComponent.class).onHit();
            bullet.removeFromWorld();
            inc(GameVars.HITS_LANDED, 1);
            ec.hit();

            double hitX = bullet.getX() + bullet.getWidth() / 2;
            double hitY = bullet.getY() + bullet.getHeight() / 2;

            boolean isToughOrBoss = ec.getType() == EnemyComponent.EnemyType.TOUGH || ec.isBoss();
            boolean isNonFatal = !ec.isDead();

            if (isToughOrBoss && isNonFatal) {
                spawnExplosion(hitX, hitY, "boss_hit");
                spawnChipDamageSparks(hitX, hitY);
                flashEnemySprite(enemy);
            } else {
                spawnExplosion(hitX, hitY, "hit");
            }

            if (ec.isDead()) {
                finishEnemyKill(enemy, ec);
            }
        });

        onCollisionBegin(EntityType.PLAYER_MISSILE, EntityType.ENEMY, (missile, enemy) -> {
            if (!enemy.isActive() || !enemy.hasComponent(EnemyComponent.class)) {
                return;
            }
            if (enemy.getComponent(EnemyComponent.class).isScooped()) {
                return;
            }
            EnemyComponent mec = enemy.getComponent(EnemyComponent.class);
            if (mec.isTuckedBehindMeteor()) {
                Entity cover = meteorCoveringShot(missile);
                if (cover == null) {
                    cover = nearestCoverMeteor(enemy);
                }
                if (cover != null) {
                    missileHitMeteor(missile, cover);
                }
                return;
            }
            Entity cover = meteorCoveringShot(missile);
            if (cover != null) {
                missileHitMeteor(missile, cover);
                return;
            }
            if (missile.isActive() && missile.hasComponent(MissileComponent.class)) {
                missile.getComponent(MissileComponent.class).requestDetonate();
            }
        });

        onCollisionBegin(EntityType.PLAYER_MISSILE, EntityType.METEOR, (missile, meteor) -> {
            missileHitMeteor(missile, meteor);
        });

        onCollisionBegin(EntityType.PLAYER_BULLET, EntityType.METEOR, (bullet, meteor) -> {
            if (bullet.isActive() && bullet.hasComponent(BulletComponent.class)) {
                bullet.getComponent(BulletComponent.class).onHit();
            }
            if (bullet.isActive()) {
                bullet.removeFromWorld();
            }
            inc(GameVars.HITS_LANDED, 1);
        });

        onCollisionBegin(EntityType.PLAYER_PLASMA, EntityType.ENEMY, (orb, enemy) -> {
            if (!orb.isActive() || !orb.hasComponent(PlasmaOrbComponent.class)) {
                return;
            }
            if (!enemy.isActive() || !enemy.hasComponent(EnemyComponent.class)) {
                return;
            }
            EnemyComponent ec = enemy.getComponent(EnemyComponent.class);
            if (ec.isScooped() || ec.isTuckedBehindMeteor()) {
                return;
            }
            applyPlasmaHit(orb, enemy);
        });

        onCollisionBegin(EntityType.PLAYER_PLASMA, EntityType.METEOR, (orb, meteor) -> {
            if (orb.isActive()) {
                orb.removeFromWorld();
            }
            inc(GameVars.HITS_LANDED, 1);
        });

        onCollisionBegin(EntityType.ENEMY_BULLET, EntityType.ENEMY, (bullet, enemy) -> {
            if (!enemy.isActive() || !enemy.hasComponent(EnemyComponent.class)) {
                return;
            }
            if (!enemy.getComponent(EnemyComponent.class).isDocked()) {
                return;
            }
            bullet.removeFromWorld();
            hitScoopFormation();
        });

        onCollisionBegin(EntityType.ENEMY_BULLET, EntityType.PLAYER, (bullet, playerEntity) -> {
            bullet.removeFromWorld();
            hitScoopFormation();
        });

        onCollisionBegin(EntityType.ENEMY, EntityType.PLAYER, (enemy, playerEntity) -> {
            if (!enemy.isActive()) {
                return;
            }
            EnemyComponent ec = enemy.getComponent(EnemyComponent.class);
            if (ec.isScooped()) {
                return;
            }
            if (playerScoopCharges() > 0 && scoopSlotsUsed() < GameVars.SCOOP_COUNT && ec.beginScoop(nextDockSlot())) {
                SoundHelper.play("scoop.wav");
                return;
            }
            if (ec.isHarmlessOnContact()) {
                return;
            }
            hitScoopFormation();
        });

        onCollisionBegin(EntityType.PICKUP, EntityType.PLAYER, (pickup, playerEntity) -> {
            PickupComponent pc = pickup.getComponent(PickupComponent.class);
            applyPickup(pc.getType());
            pickup.removeFromWorld();
        });

        onCollisionBegin(EntityType.RANK_MARKER, EntityType.PLAYER, (marker, playerEntity) -> {
            collectRankMarker(marker.getComponent(RankMarkerComponent.class).getColorIndex());
            marker.removeFromWorld();
        });

        onCollisionBegin(EntityType.EXTRA_LETTER_PICKUP, EntityType.PLAYER, (letterOrb, playerEntity) -> {
            ExtraLetterPickupComponent lpc = letterOrb.getComponent(ExtraLetterPickupComponent.class);
            collectExtraLetter(lpc.getLetter(), lpc.getLetterIndex());
            letterOrb.removeFromWorld();
        });

        onCollisionBegin(EntityType.COIN, EntityType.PLAYER, (coin, playerEntity) -> {
            deltablade.components.CoinComponent cc = coin.getComponent(deltablade.components.CoinComponent.class);
            inc(GameVars.MONEY, cc.getValue());
            coin.removeFromWorld();
            SoundHelper.play("money.wav");
        });
    }

    private void playerHit(PlayerComponent pc) {
        if (minigameActive || warping) {
            return;
        }
        if (destroyScoopedBySlot(0) || destroyScoopedBySlot(1)) {
            scoopHitLock = 0.35;
            return;
        }
        inc(GameVars.LIVES, -1);
        pc.makeInvulnerable();

        set(GameVars.AUTOFIRE, false);
        clearShopWeapon();

        if (player != null) {
            double explosionX = player.getX() + player.getWidth() / 2;
            double explosionY = player.getY() + player.getHeight() / 2;
            spawnExplosion(explosionX, explosionY, "ship");
            SoundHelper.play("explode_ship.wav");
        }

        if (geti(GameVars.WEAPON_GRADE) > 1) {
            inc(GameVars.WEAPON_GRADE, -1);
        }

        changeLevelTime(-GameVars.LEVEL_TIME_DEATH_PENALTY);

        if (geti(GameVars.LIVES) <= 0) {
            triggerGameOver();
        }
    }

    private void trySpawnPickup(double x, double y, EnemyComponent.EnemyType enemyType) {
        if (isMeteorChallenge()) {
            return;
        }
        boolean extraLetterInWorld = !getGameWorld().getEntitiesByType(EntityType.EXTRA_LETTER_PICKUP).isEmpty();
        boolean extraLetterSpawnedThisWave = geti(GameVars.EXTRA_LETTER_SPAWNED_THIS_WAVE) > 0;

        if (!extraLetterInWorld && !extraLetterSpawnedThisWave && RANDOM.nextDouble() < GameVars.EXTRA_LETTER_DROP_CHANCE) {
            int letterIndex = getRandomUnownedLetterIndex();
            if (letterIndex >= 0) {
                char letter = GameVars.EXTRA_LETTERS[letterIndex];
                spawn("extraLetterOrb", new com.almasb.fxgl.entity.SpawnData(x - 14, y - 14)
                        .put("letter", letter)
                        .put("letterIndex", letterIndex));
                set(GameVars.EXTRA_LETTER_SPAWNED_THIS_WAVE, 1);
                return;
            }
        }

        double roll = RANDOM.nextDouble();

        if (roll < GameVars.MINIGAME_DROP_CHANCE) {
            // Reparto a partes iguales: meteoritos / cognitivo / laberinto / alien endurance (misma rareza total).
            int which = RANDOM.nextInt(4);
            String mini = switch (which) {
                case 0 -> "meteorPickup";
                case 1 -> "cognitivePickup";
                case 2 -> "mazePickup";
                default -> "alienPickup";
            };
            spawn(mini, x - 14, y - 14);
            return;
        }
        roll -= GameVars.MINIGAME_DROP_CHANCE;

        if (roll < GameVars.SKULL_DROP_CHANCE) {
            spawn("skullPickup", x - 14, y - 14);
            return;
        }
        roll -= GameVars.SKULL_DROP_CHANCE;

        if (roll < GameVars.AUTOFIRE_DROP_CHANCE) {
            if (!getb(GameVars.AUTOFIRE)) {
                spawn("autofirePickup", x - 14, y - 14);
            }
            return;
        }
        roll -= GameVars.AUTOFIRE_DROP_CHANCE;

        if (roll < GameVars.TIME_PICKUP_DROP_CHANCE) {
            spawn("timePickup", x - 14, y - 14);
            return;
        }
        roll -= GameVars.TIME_PICKUP_DROP_CHANCE;

        if (roll < GameVars.SHIELD_DROP_CHANCE) {
            spawn("shieldPickup", x - 14, y - 14);
            return;
        }
        roll -= GameVars.SHIELD_DROP_CHANCE;

        if (scoopDropAllowed() && playerScoopCharges() < GameVars.SCOOP_COUNT && roll < GameVars.SCOOP_DROP_CHANCE) {
            spawn("scoopPickup", x - 14, y - 14);
            return;
        }
        roll -= GameVars.SCOOP_DROP_CHANCE;

        if (roll < GameVars.MISSILE_DROP_CHANCE) {
            spawn("missilePickup", x - 14, y - 14);
            return;
        }
        roll -= GameVars.MISSILE_DROP_CHANCE;

        if (roll < GameVars.PICKUP_DROP_CHANCE) {
            String pickupType = RANDOM.nextBoolean() ? "weaponPickup" : "ammoPickup";
            spawn(pickupType, x - 14, y - 14);
            return;
        }
        roll -= GameVars.PICKUP_DROP_CHANCE;

        int level = geti(GameVars.LEVEL);
        boolean canDropViolet = level >= 8 && enemyType == EnemyComponent.EnemyType.TOUGH;
        if (canDropViolet && roll < GameVars.COIN_VIOLET_DROP_CHANCE) {
            spawnCoin(x, y, "violet");
            return;
        }
        if (canDropViolet) roll -= GameVars.COIN_VIOLET_DROP_CHANCE;

        if (roll < GameVars.COIN_WHITE_DROP_CHANCE) {
            spawnCoin(x, y, "white");
            return;
        }
        roll -= GameVars.COIN_WHITE_DROP_CHANCE;

        if (roll < GameVars.COIN_GREEN_DROP_CHANCE) {
            spawnCoin(x, y, "green");
            return;
        }
        roll -= GameVars.COIN_GREEN_DROP_CHANCE;

        if (roll < GameVars.COIN_BLUE_DROP_CHANCE) {
            spawnCoin(x, y, "blue");
        }
    }

    private int getRandomUnownedLetterIndex() {
        List<Integer> unownedIndices = new ArrayList<>();
        for (int i = 0; i < GameVars.EXTRA_VARS.length; i++) {
            if (geti(GameVars.EXTRA_VARS[i]) == 0) {
                unownedIndices.add(i);
            }
        }
        if (unownedIndices.isEmpty()) {
            return -1;
        }
        return unownedIndices.get(RANDOM.nextInt(unownedIndices.size()));
    }

    private void spawnCoin(double x, double y, String coinType) {
        spawn("coin", new com.almasb.fxgl.entity.SpawnData(x - 8, y - 8)
                .put("coinType", coinType));
    }

    private void spawnBossCoins(double x, double y) {
        if (RANDOM.nextDouble() < 0.85) {
            spawnCoin(x, y, "violet");
        }

        int extraCoins = 2 + RANDOM.nextInt(2);
        for (int i = 0; i < extraCoins; i++) {
            double offsetX = (RANDOM.nextDouble() - 0.5) * 40;
            double offsetY = (RANDOM.nextDouble() - 0.5) * 30;
            double coinRoll = RANDOM.nextDouble();
            String type;
            if (coinRoll < 0.5) {
                type = "green";
            } else if (coinRoll < 0.85) {
                type = "blue";
            } else {
                type = "white";
            }
            spawnCoin(x + offsetX, y + offsetY, type);
        }
    }

    /**
     * Muerte del jefe: 3-5 explosiones grandes escalonadas con sonido irregular.
     * La primera explosión es inmediata, con onda expansiva y destello; las demás son más suaves.
     */
    private void spawnBossDeathSequence(double centerX, double centerY) {
        int boomCount = 3 + RANDOM.nextInt(3);

        spawnExplosion(centerX, centerY, "big");
        SoundHelper.play("explode_boss.wav");

        double accumulatedDelay = 0;
        for (int i = 1; i < boomCount; i++) {
            double delay = 0.08 + RANDOM.nextDouble() * 0.27;
            accumulatedDelay += delay;

            final double offsetX = (RANDOM.nextDouble() - 0.5) * 90;
            final double offsetY = (RANDOM.nextDouble() - 0.5) * 70;
            final double finalDelay = accumulatedDelay;

            runOnce(() -> {
                spawnBossFollowupExplosion(centerX + offsetX, centerY + offsetY);
                SoundHelper.play("explode_boss.wav");
            }, Duration.seconds(finalDelay));
        }
    }

    private void spawnBossFollowupExplosion(double x, double y) {
        spawn("explosion", new com.almasb.fxgl.entity.SpawnData(x, y).put("size", "big"));
    }

    private void collectExtraLetter(char letter, int letterIndex) {
        String varName = GameVars.EXTRA_VARS[letterIndex];
        if (geti(varName) == 0) {
            set(varName, 1);
            inc(GameVars.SCORE, 25);
            showBanner(String.valueOf(letter), LETTER_COLORS[letterIndex], 1.0);
            SoundHelper.play("extra.wav");

            if (isExtraComplete()) {
                inc(GameVars.LIVES, 1);
                resetExtraLetters();
                showExtraLifeFlash();
                SoundHelper.play("extra_life.wav");
            }
        }
    }

    private boolean isExtraComplete() {
        for (String var : GameVars.EXTRA_VARS) {
            if (geti(var) == 0) return false;
        }
        return true;
    }

    private void resetExtraLetters() {
        for (String var : GameVars.EXTRA_VARS) {
            set(var, 0);
        }
    }

    private void showExtraLifeFlash() {
        showBanner(I18n.tr("banner.extraLife"), Color.GOLD, 1.5);
    }

    private void applyPickup(PickupComponent.PickupType type) {
        switch (type) {
            case WEAPON_UPGRADE -> {
                if (geti(GameVars.WEAPON_GRADE) < GameVars.MAX_WEAPON_GRADE) {
                    inc(GameVars.WEAPON_GRADE, 1);
                }
                inc(GameVars.SCORE, 50);
                showBanner(I18n.tr("banner.weapon"), Color.GOLD, 1.2);
                SoundHelper.play("weapon.wav");
            }
            case EXTRA_AMMO -> {
                if (geti(GameVars.AMMO_CAP) < GameVars.MAX_AMMO_CAP) {
                    inc(GameVars.AMMO_CAP, 1);
                }
                inc(GameVars.SCORE, 25);
                showBanner(I18n.tr("banner.ammo"), Color.CYAN, 1.2);
                SoundHelper.play("ammo.wav");
            }
            case EXTRA_LIFE -> {
                inc(GameVars.LIVES, 1);
                inc(GameVars.SCORE, 100);
                showExtraLifeFlash();
                SoundHelper.play("extra_life.wav");
            }
            case AUTOFIRE -> {
                set(GameVars.AUTOFIRE, true);
                inc(GameVars.SCORE, 100);
                showBanner(I18n.tr("banner.auto"), Color.CYAN, 1.2);
                SoundHelper.play("autofire.wav");
            }
            case METEOR -> {
                inc(GameVars.SCORE, 25);
                startMinigame(new MeteorStormGame());
            }
            case COGNITIVE -> {
                inc(GameVars.SCORE, 25);
                SoundHelper.play("cognitive_test.mp3");
                startMinigame(new CognitiveTestGame());
            }
            case MAZE -> {
                inc(GameVars.SCORE, 25);
                startMinigame(new MazeEscapeGame());
            }
            case ALIEN -> {
                inc(GameVars.SCORE, 25);
                startMinigame(new AlienEnduranceGame());
            }
            case EXTRA_TIME -> {
                changeLevelTime(GameVars.LEVEL_TIME_PICKUP);
                inc(GameVars.SCORE, 25);
                showBanner(I18n.tr("banner.time", GameVars.LEVEL_TIME_PICKUP), Color.LIMEGREEN, 1.1);
                SoundHelper.play("extra_time.wav");
            }
            case SHIELD -> {
                if (player != null && player.hasComponent(PlayerComponent.class)) {
                    player.getComponent(PlayerComponent.class).addShield(GameVars.SHIELD_DURATION);
                }
                inc(GameVars.SCORE, 25);
                showBanner(I18n.tr("banner.shield", (int) GameVars.SHIELD_DURATION), Color.AQUA, 1.1);
                SoundHelper.play("shield.wav");
                updateShieldHud();
            }
            case SCOOP -> {
                attachScoops();
                inc(GameVars.SCORE, 25);
                showBanner(I18n.tr("banner.scoop"), Color.LIMEGREEN, 1.1);
                SoundHelper.play("scoop.wav");
            }
            case MISSILE -> {
                inc(GameVars.SCORE, 25);
                if (findActiveMissile() == null) {
                    launchPlayerMissile();
                    showBanner(I18n.tr("banner.missileSteer"), Color.ORANGERED, 1.2);
                } else {
                    showBanner(I18n.tr("banner.missileLive"), Color.ORANGERED, 1.0);
                }
            }
            case SKULL -> applySkullCurse();
            default -> {}
        }
    }

    private void trySpawnRankMarker(double x, double y, EnemyComponent.EnemyType enemyType) {
        if (enemyType != EnemyComponent.EnemyType.TOUGH && enemyType != EnemyComponent.EnemyType.BOSS) {
            return;
        }
        boolean rankWave = waveManager != null && waveManager.getCurrentWaveType() == WaveManager.WaveType.RANK;
        double chance = rankWave ? 1.0 : GameVars.RANK_MARKER_DROP_CHANCE;
        if (enemyType == EnemyComponent.EnemyType.BOSS) {
            chance = 1.0;
        }
        if (RANDOM.nextDouble() > chance) {
            return;
        }
        int mask = geti(GameVars.RANK_MASK);
        List<Integer> missing = new ArrayList<>();
        for (int i = 0; i < GameVars.RANK_COLOR_COUNT; i++) {
            if ((mask & (1 << i)) == 0) {
                missing.add(i);
            }
        }
        int colorIndex = missing.isEmpty()
                ? RANDOM.nextInt(GameVars.RANK_COLOR_COUNT)
                : missing.get(RANDOM.nextInt(missing.size()));
        spawn("rankMarker", new com.almasb.fxgl.entity.SpawnData(x - 10, y - 10)
                .put("colorIndex", colorIndex));
    }

    private void collectRankMarker(int colorIndex) {
        int bit = 1 << colorIndex;
        int mask = geti(GameVars.RANK_MASK);
        if ((mask & bit) == 0) {
            mask |= bit;
            set(GameVars.RANK_MASK, mask);
        }
        inc(GameVars.SCORE, 50);
        SoundHelper.play("rank_marker.wav");
        updateRankHud();
        if (mask == (1 << GameVars.RANK_COLOR_COUNT) - 1) {
            inc(GameVars.RANK, 1);
            set(GameVars.RANK_MASK, 0);
            showBanner(RankNames.banner(geti(GameVars.RANK)), Color.GOLD, 2.0);
            SoundHelper.play("rank_up.wav");
            updateRankHud();
        }
    }

    private void checkWaveComplete() {
        if (minigameActive || warping || gameOver || waveManager == null || waveTransition) {
            return;
        }
        if (waveManager.hasPendingSpawns() || hasLiveCombatEnemies() || alienBossPending) {
            return;
        }
        // Después del jefe: una fase corta de recogida para poder coger el botín.
        if ((waveManager.getCurrentWaveType() == WaveManager.WaveType.BOSS || waveManager.alienBossThisWave())
                && !bossCollectDone) {
            if (bossCollectLeft <= 0) {
                bossCollectLeft = BOSS_COLLECT_SECONDS;
            }
            return;
        }
        waveTransition = true;
        set(GameVars.ENEMIES_REMAINING, 0);
        // Salto + tienda: antes dependía de la oleada kamikaze de los niveles 10, 20, 30...
        // Desde que el orden de las oleadas es aleatorio, el salto llega siempre cada WARP_EVERY niveles
        // (y además después de cada oleada kamikaze).
        int lvl = geti(GameVars.LEVEL);
        if ((lvl > 0 && lvl % GameVars.WARP_EVERY == 0)
                || waveManager.getCurrentWaveType() == WaveManager.WaveType.KAMIKAZE) {
            startWarp();
            return;
        }
        double pause = waveManager.getCurrentWaveType() == WaveManager.WaveType.BONUS ? 2.1 : 1.2;
        runOnce(() -> {
            if (gameOver || showingTitleScreen) {
                return;
            }
            continueToNextWave();
        }, Duration.seconds(pause));
    }

    private static final double BOSS_COLLECT_SECONDS = 5.0;
    private double bossCollectLeft = 0;
    private boolean bossCollectDone = false;
    private static final EntityType[] DROP_TYPES = {
            EntityType.PICKUP, EntityType.COIN, EntityType.RANK_MARKER, EntityType.EXTRA_LETTER_PICKUP
    };

    /** Fase de recogida después del jefe: el botín flota suavemente hacia el jugador. Máx. 5 s o hasta recogerlo todo. */
    private void updateBossCollect(double tpf) {
        if (bossCollectLeft <= 0 || bossCollectDone) {
            return;
        }
        bossCollectLeft -= tpf;
        boolean anyDrop = false;
        if (player != null && player.isActive()) {
            double px = player.getX() + player.getWidth() / 2.0;
            double py = player.getY() + player.getHeight() / 2.0;
            for (EntityType t : DROP_TYPES) {
                for (Entity d : getGameWorld().getEntitiesByType(t)) {
                    if (!d.isActive()) {
                        continue;
                    }
                    anyDrop = true;
                    double dx = px - (d.getX() + d.getWidth() / 2.0);
                    double dy = py - (d.getY() + d.getHeight() / 2.0);
                    double dist = Math.hypot(dx, dy);
                    if (dist > 1) {
                        double pull = 140 * tpf;
                        d.translate(dx / dist * pull, dy / dist * pull);
                    }
                }
            }
        }
        if (bossCollectLeft <= 0 || !anyDrop) {
            bossCollectLeft = 0;
            bossCollectDone = true;
            checkWaveComplete();
        }
    }

    private boolean hasLiveCombatEnemies() {
        for (Entity enemy : getGameWorld().getEntitiesByType(EntityType.ENEMY)) {
            if (!enemy.isActive() || !enemy.hasComponent(EnemyComponent.class)) {
                continue;
            }
            if (!enemy.getComponent(EnemyComponent.class).isScooped()) {
                return true;
            }
        }
        return false;
    }

    public boolean isCombatStopped() {
        return gameOver || showingTitleScreen;
    }

    private void triggerGameOver() {
        if (gameOver) {
            return;
        }
        gameOver = true;
        clearScoops();
        clearLaserBeams();
        cancelGetReady();
        if (waveManager != null) {
            waveManager.stop();
        }
        final int wave = geti(GameVars.LEVEL);
        Platform.runLater(() -> {
            clearWaveEntities();
            showSettlementThenHiscore(wave);
        });
    }

    private void showSettlementThenHiscore(int wave) {
        hideSettlementOverlay();
        List<SettlementOverlay.Line> lines = buildSettlementLines();
        int baseScore = geti(GameVars.SCORE);
        settlementOverlay = new SettlementOverlay(baseScore, lines, finalScore -> {
            hideSettlementOverlay();
            set(GameVars.SCORE, finalScore);
            HighScoreClient.fetch(ignored -> {
                if (!gameOver || showingTitleScreen) {
                    return;
                }
                boolean localOk = HighScoreStore.qualifies(finalScore);
                boolean globalOk = HighScoreClient.hasCache()
                        && HighScoreStore.qualifies(finalScore, HighScoreClient.cachedEntries());
                if (localOk || globalOk) {
                    showHighScoreNameEntry(finalScore, wave);
                } else {
                    showHighScoreTable(null, null, false, true);
                }
            });
        });
        settlementRoot = settlementOverlay.getRoot();
        getGameScene().addUINode(settlementRoot);
    }

    private List<SettlementOverlay.Line> buildSettlementLines() {
        int shots = Math.max(0, geti(GameVars.SHOTS_FIRED));
        int hits = Math.max(0, geti(GameVars.HITS_LANDED));
        double hitRate = shots == 0 ? 0.0 : Math.min(1.0, (double) hits / (double) shots);
        int accuracyBonus = (int) Math.floor(hitRate * hitRate * GameVars.SETTLEMENT_ACCURACY_CAP);

        List<SettlementOverlay.Line> lines = new ArrayList<>();
        lines.add(new SettlementOverlay.Line(I18n.tr("settle.rank"), geti(GameVars.RANK) * GameVars.SETTLEMENT_RANK));
        lines.add(new SettlementOverlay.Line(I18n.tr("settle.accuracy"), accuracyBonus));
        lines.add(new SettlementOverlay.Line(I18n.tr("settle.combos"), geti(GameVars.SQUAD_COMBOS) * GameVars.SETTLEMENT_COMBO));
        lines.add(new SettlementOverlay.Line(I18n.tr("settle.meteor"), geti(GameVars.METEOR_WINS) * GameVars.SETTLEMENT_METEOR));
        lines.add(new SettlementOverlay.Line(I18n.tr("settle.cognitive"), geti(GameVars.COGNITIVE_WINS) * GameVars.SETTLEMENT_COGNITIVE));
        lines.add(new SettlementOverlay.Line(I18n.tr("settle.maze"), geti(GameVars.MAZE_WINS) * GameVars.SETTLEMENT_MAZE));
        lines.add(new SettlementOverlay.Line(I18n.tr("settle.waves"), Math.max(0, geti(GameVars.LEVEL) - 1) * GameVars.SETTLEMENT_WAVE));
        lines.add(new SettlementOverlay.Line(I18n.tr("settle.cash"), Math.max(0, geti(GameVars.MONEY)) * GameVars.SETTLEMENT_CASH_MULT));
        lines.add(new SettlementOverlay.Line(I18n.tr("settle.perfect"), geti(GameVars.BONUS_PERFECT_CLEARS) * GameVars.SETTLEMENT_PERFECT));
        return lines;
    }

    private void hideSettlementOverlay() {
        if (settlementRoot != null) {
            getGameScene().removeUINode(settlementRoot);
            settlementRoot = null;
        }
        settlementOverlay = null;
    }

    private void startTitleHiRotate() {
        stopTitleHiRotate();
        refreshTitleHi();
        HighScoreClient.fetch(ignored -> refreshTitleHi());
        titleHiRotate = new Timeline(new KeyFrame(Duration.seconds(3.5), e -> {
            titleHiGlobal = !titleHiGlobal;
            refreshTitleHi();
        }));
        titleHiRotate.setCycleCount(Animation.INDEFINITE);
        titleHiRotate.play();
    }

    private void stopTitleHiRotate() {
        if (titleHiRotate != null) {
            titleHiRotate.stop();
            titleHiRotate = null;
        }
    }

    private void refreshTitleHi() {
        if (!showingTitleScreen || titleHiLine == null) {
            return;
        }
        if (titleHiGlobal && !HighScoreClient.hasCache()) {
            titleHiGlobal = false;
        }
        titleHiLine.setText(titleHiText(titleHiGlobal));
        centerHorizontally(titleHiLine, 226);
    }

    private String titleHiText(boolean global) {
        var table = global ? HighScoreClient.cachedEntries() : HighScoreStore.entries();
        String tag = global ? I18n.tr("menu.hiGlobal") : I18n.tr("menu.hiLocal");
        if (table.isEmpty()) {
            return tag + "  ---  000000";
        }
        var top = table.getFirst();
        return tag + "  " + top.name() + "  " + HighScoreStore.formatScore(top.score());
    }

    private boolean enteringHighScoreName() {
        return highScoreOverlay != null && highScoreOverlay.isNameEntry();
    }

    private void showHighScoreNameEntry(int score, int wave) {
        hideHighScoreOverlay();
        highScoreOverlay = HighScoreOverlay.nameEntry(score, wave, this::showHighScoreTableAfterSave);
        highScoreRoot = highScoreOverlay.getRoot();
        getGameScene().addUINode(highScoreRoot);
    }

    private void showHighScoreTableAfterSave(String name, int score, int wave, int localRank) {
        Integer localHighlight = localRank >= 0 ? localRank : null;
        showHighScoreTable(localHighlight, null, false, false);
        HighScoreClient.submit(name, score, wave, result -> {
            if (highScoreOverlay == null || highScoreOverlay.isNameEntry()) {
                return;
            }
            if (result.ok()) {
                Integer globalHighlight = result.rank() >= 0 ? result.rank() : null;
                highScoreOverlay.setGlobalEntries(result.entries(), globalHighlight);
            } else {
                HighScoreClient.fetch(list -> {
                    if (list != null && highScoreOverlay != null && !highScoreOverlay.isNameEntry()) {
                        highScoreOverlay.setGlobalEntries(list);
                    }
                });
            }
        });
    }

    private void showHighScoreTable(Integer localHighlight, Integer globalHighlight, boolean fromTitle, boolean fetchRemote) {
        hideHighScoreOverlay();
        String closeLabel = fromTitle ? I18n.tr("hiscore.back") : I18n.tr("hiscore.restart");
        Runnable onClose = fromTitle ? this::hideHighScoreOverlay : null;
        highScoreOverlay = HighScoreOverlay.table(localHighlight, globalHighlight, closeLabel, onClose);
        highScoreRoot = highScoreOverlay.getRoot();
        getGameScene().addUINode(highScoreRoot);
        if (fetchRemote) {
            HighScoreClient.fetch(list -> {
                if (list != null && highScoreOverlay != null && !highScoreOverlay.isNameEntry()) {
                    highScoreOverlay.setGlobalEntries(list);
                }
            });
        }
    }

    private void hideHighScoreOverlay() {
        if (highScoreOverlay != null) {
            highScoreOverlay.dispose();
            highScoreOverlay = null;
        }
        if (highScoreRoot != null) {
            getGameScene().removeUINode(highScoreRoot);
            highScoreRoot = null;
        }
    }

    private void restartGame() {
        hideOptions();
        stopExtraLetterAnimations();
        if (activeShake != null) {
            activeShake.stop();
            activeShake = null;
        }
        resetShakenViews();
        var viewport = getGameScene().getViewport();
        viewport.setX(0);
        viewport.setY(0);
        getGameController().startNewGame();
    }

    private void stopExtraLetterAnimations() {
        for (Animation anim : extraLetterAnimations) {
            anim.stop();
        }
        extraLetterAnimations.clear();
    }

    private void dropTestMinigame(TestMode.Drop drop) {
        if (drop == null) {
            return;
        }
        if (drop.spawnName() == null) {
            showBanner(drop.banner(), Color.LIGHTGRAY, 1.2);
            return;
        }
        if ("MONEY".equals(drop.spawnName())) {
            if (gameOver || optionsOpen) {
                return;
            }
            if (!gameStarted || showingTitleScreen) {
                if (showingTitleScreen) {
                    startActualGame(false);
                }
            }
            inc(GameVars.MONEY, 15_000);
            showBanner(drop.banner(), Color.GOLD, 1.2);
            SoundHelper.play("money.wav");
            return;
        }
        if ("WARP".equals(drop.spawnName())) {
            if (gameOver || optionsOpen || minigameActive || warping) {
                return;
            }
            if (!gameStarted || showingTitleScreen || player == null) {
                if (showingTitleScreen) {
                    startActualGame(false);
                }
                if (player == null) {
                    return;
                }
            }
            startWarp();
            return;
        }
        if ("UFO".equals(drop.spawnName())) {
            if (gameOver || optionsOpen || minigameActive || warping) {
                return;
            }
            if (!gameStarted || showingTitleScreen || player == null) {
                if (showingTitleScreen) {
                    startActualGame(false);
                }
                if (player == null) {
                    return;
                }
            }
            ufoSpawnedThisWave = false;
            spawnHurryUpUfo();
            showBanner(drop.banner(), Color.CYAN, 1.2);
            return;
        }
        if ("BONUS".equals(drop.spawnName())) {
            if (gameOver || optionsOpen || minigameActive || warping) {
                return;
            }
            if (!gameStarted || showingTitleScreen || player == null) {
                if (showingTitleScreen) {
                    startActualGame(false);
                }
                if (player == null) {
                    return;
                }
            }
            cancelGetReady();
            clearWaveEntities();
            refillWaveTimer();
            ufoSpawnedThisWave = false;
            waveTransition = false;
            waveManager.startWave(geti(GameVars.LEVEL), WaveManager.WaveType.BONUS);
            showWaveAnnouncement();
            SoundHelper.play("bonus_round.wav");
            showBanner(drop.banner(), Color.GOLD, 1.2);
            return;
        }
        if ("SCOOP".equals(drop.spawnName())) {
            if (gameOver || optionsOpen || minigameActive || warping) {
                return;
            }
            if (!gameStarted || showingTitleScreen || player == null) {
                if (showingTitleScreen) {
                    startActualGame(false);
                }
                if (player == null) {
                    return;
                }
            }
            attachScoops();
            showBanner(drop.banner(), Color.LIMEGREEN, 1.1);
            SoundHelper.play("scoop.wav");
            return;
        }

        if ("MONSTER_BOSS".equals(drop.spawnName())) {
            if (gameOver || optionsOpen || minigameActive || warping) {
                return;
            }
            if (!gameStarted || showingTitleScreen || player == null) {
                if (showingTitleScreen) {
                    startActualGame(false);
                }
                if (player == null) {
                    return;
                }
            }
            cancelGetReady();
            clearWaveEntities();
            refillWaveTimer();
            ufoSpawnedThisWave = false;
            waveTransition = false;
            bossCollectLeft = 0;
            bossCollectDone = false;
            waveManager.startWave(Math.max(geti(GameVars.LEVEL), GameVars.MONSTER_WAVE_MIN_LEVEL), WaveManager.WaveType.MONSTER);
            waveManager.markAlienBossSpawned();
            inc(GameVars.ENEMIES_REMAINING, 1);
            alienBossPending = true;
            runOnce(this::spawnAlienBoss, Duration.seconds(3));
            showWaveAnnouncement();
            SoundHelper.play("get_ready.wav");
            showBanner(drop.banner(), Color.AQUA, 1.2);
            return;
        }
        if ("ALIEN_ENDURANCE".equals(drop.spawnName())) {
            if (gameOver || optionsOpen || minigameActive || warping) {
                return;
            }
            if (!gameStarted || showingTitleScreen || player == null) {
                if (showingTitleScreen) {
                    startActualGame(false);
                }
                if (player == null) {
                    return;
                }
            }
            showBanner(drop.banner(), Color.AQUAMARINE, 1.2);
            startMinigame(new AlienEnduranceGame());
            return;
        }
        if ("MAZE_ESCAPE".equals(drop.spawnName())) {
            if (gameOver || optionsOpen || minigameActive || warping) {
                return;
            }
            if (!gameStarted || showingTitleScreen || player == null) {
                if (showingTitleScreen) {
                    startActualGame(false);
                }
                if (player == null) {
                    return;
                }
            }
            showBanner(drop.banner(), Color.MEDIUMPURPLE, 1.2);
            startMinigame(new MazeEscapeGame());
            return;
        }
        if ("METEOR_CHALLENGE".equals(drop.spawnName())) {
            if (gameOver || optionsOpen || minigameActive || warping) {
                return;
            }
            if (!gameStarted || showingTitleScreen || player == null) {
                if (showingTitleScreen) {
                    startActualGame(false);
                }
                if (player == null) {
                    return;
                }
            }
            cancelGetReady();
            clearWaveEntities();
            refillWaveTimer();
            ufoSpawnedThisWave = false;
            waveTransition = false;
            meteorMissileCooldown = 0.4;
            waveManager.startWave(geti(GameVars.LEVEL), WaveManager.WaveType.METEOR);
            showWaveAnnouncement();
            SoundHelper.play("get_ready.wav");
            showBanner(drop.banner(), Color.SANDYBROWN, 1.2);
            return;
        }
        if ("MONSTER".equals(drop.spawnName())) {
            if (gameOver || optionsOpen || minigameActive || warping) {
                return;
            }
            if (!gameStarted || showingTitleScreen || player == null) {
                if (showingTitleScreen) {
                    startActualGame(false);
                }
                if (player == null) {
                    return;
                }
            }
            cancelGetReady();
            clearWaveEntities();
            refillWaveTimer();
            ufoSpawnedThisWave = false;
            waveTransition = false;
            waveManager.startWave(Math.max(geti(GameVars.LEVEL), GameVars.MONSTER_SPLIT_MIN_LEVEL), WaveManager.WaveType.MONSTER);
            showWaveAnnouncement();
            SoundHelper.play("get_ready.wav");
            showBanner(drop.banner(), Color.MEDIUMPURPLE, 1.2);
            return;
        }
        if (drop.spawnName() != null && drop.spawnName().startsWith("WAVE:")) {
            if (gameOver || optionsOpen || minigameActive || warping) {
                return;
            }
            if (!gameStarted || showingTitleScreen || player == null) {
                if (showingTitleScreen) {
                    startActualGame(false);
                }
                if (player == null) {
                    return;
                }
            }
            WaveManager.WaveType forced = WaveManager.WaveType.valueOf(drop.spawnName().substring(5));
            cancelGetReady();
            clearWaveEntities();
            refillWaveTimer();
            ufoSpawnedThisWave = false;
            waveTransition = false;
            int lvl = geti(GameVars.LEVEL);
            if (forced == WaveManager.WaveType.BOSS) {
                lvl = Math.max(lvl, GameVars.BOSS_EVERY);
            }
            bossCollectLeft = 0;
            bossCollectDone = false;
            waveManager.startWave(lvl, forced);
            showWaveAnnouncement();
            SoundHelper.play("get_ready.wav");
            showBanner(drop.banner(), Color.AQUA, 1.2);
            return;
        }
        if ("PEARL".equals(drop.spawnName())) {
            if (gameOver || optionsOpen || minigameActive || warping) {
                return;
            }
            if (!gameStarted || showingTitleScreen || player == null) {
                if (showingTitleScreen) {
                    startActualGame(false);
                }
                if (player == null) {
                    return;
                }
            }
            cancelGetReady();
            clearWaveEntities();
            refillWaveTimer();
            ufoSpawnedThisWave = false;
            waveTransition = false;
            waveManager.startWave(geti(GameVars.LEVEL), WaveManager.WaveType.PEARL);
            showWaveAnnouncement();
            SoundHelper.play("get_ready.wav");
            showBanner(drop.banner(), Color.AQUA, 1.2);
            return;
        }
        if (gameOver || optionsOpen || minigameActive || warping) {
            return;
        }
        if (!gameStarted || showingTitleScreen || player == null) {
            if (showingTitleScreen) {
                startActualGame(false);
            }
            if (player == null) {
                return;
            }
        }
        double x = player.getX() + player.getWidth() / 2 - 14;
        double y = Math.max(56, player.getY() - 90);
        spawn(drop.spawnName(), x, y);
        showBanner(drop.banner(), Color.LIGHTGRAY, 1.2);
    }

    private void startMinigame(Minigame game) {
        if (game == null || gameOver || showingTitleScreen || warping) {
            return;
        }
        waveTransition = true;
        cancelGetReady();
        minigameActive = true;
        if (waveManager != null) {
            waveManager.stop(); // que no aparezcan enemigos del juego principal durante el minijuego
        }
        clearWaveEntities();
        activeMinigame = game;
        game.start(this);
    }

    private void clearWaveEntities() {
        for (EntityType type : List.of(
                EntityType.ENEMY,
                EntityType.PLAYER_BULLET,
                EntityType.PLAYER_MISSILE,
                EntityType.PLAYER_PLASMA,
                EntityType.ENEMY_BULLET,
                EntityType.PICKUP,
                EntityType.RANK_MARKER,
                EntityType.EXTRA_LETTER_PICKUP,
                EntityType.COIN,
                EntityType.METEOR,
                EntityType.MINIGAME_HAZARD)) {
            for (Entity entity : List.copyOf(getGameWorld().getEntitiesByType(type))) {
                if (entity.isActive()) {
                    entity.removeFromWorld();
                }
            }
        }
        set(GameVars.ACTIVE_BULLETS, 0);
        set(GameVars.ENEMIES_REMAINING, 0);
        dropMissileShield();
    }

    private void dropMissileShield() {
        if (player != null && player.isActive() && player.hasComponent(PlayerComponent.class)) {
            player.getComponent(PlayerComponent.class).setMissileShield(false);
        }
    }

    @Override
    public void finishMinigame(boolean won, int scoreBonus, int moneyBonus, String banner, Color color) {
        if (activeMinigame != null) {
            activeMinigame.cleanup();
            activeMinigame = null;
        }
        minigameActive = false;
        MusicHelper.applyFromStore();
        hidePlayer(false);
        for (Entity coin : List.copyOf(getGameWorld().getEntitiesByType(EntityType.COIN))) {
            if (coin.isActive()) {
                coin.removeFromWorld();
            }
        }

        if (scoreBonus > 0) {
            inc(GameVars.SCORE, scoreBonus);
        }
        if (moneyBonus > 0) {
            inc(GameVars.MONEY, moneyBonus);
        }
        if (banner != null && !banner.isBlank()) {
            showBanner(banner, color, 2.0);
        }

        runOnce(() -> {
            continueToNextWave();
        }, Duration.seconds(2));
    }

    private static final double WARP_DURATION = 8.9;

    private void startWarp() {
        if (warping || gameOver || showingTitleScreen) {
            return;
        }
        warping = true;
        waveTransition = true;
        cancelGetReady();
        warpElapsed = 0;
        clearScoops();
        clearWaveEntities();
        showBanner(I18n.tr("banner.warp"), Color.CYAN, 2.2);
        MusicHelper.stop();
        SoundHelper.play("warp.mp3");
        ensureWarpFlash();
        applyWarpVisuals(0);
    }

    private void updateWarp(double tpf) {
        warpElapsed += tpf;
        applyWarpVisuals(warpIntensity(warpElapsed / WARP_DURATION));
        if (warpElapsed >= WARP_DURATION) {
            finishWarp();
        }
    }

    private static double warpIntensity(double t) {
        double x = Math.max(0, Math.min(1, t));
        if (x < 0.14) {
            return easeIn(x / 0.14);
        }
        if (x > 0.82) {
            return 1.0 - easeIn((x - 0.82) / 0.18);
        }
        return 1.0;
    }

    private static double easeIn(double t) {
        return t * t;
    }

    private void finishWarp() {
        stopWarpVisuals();
        MusicHelper.applyFromStore();
        openShopAfterWarp();
    }

    private void openShopAfterWarp() {
        if (gameOver || showingTitleScreen) {
            return;
        }
        hideShopOverlay();
        ShopOverlay.RunStats stats = new ShopOverlay.RunStats(
                geti(GameVars.MONEY),
                geti(GameVars.RANK),
                geti(GameVars.LEVEL),
                geti(GameVars.METEOR_WINS),
                geti(GameVars.COGNITIVE_WINS),
                geti(GameVars.SQUAD_COMBOS),
                geti(GameVars.BOSS_KILLS),
                currentShopWeapon(),
                geti(GameVars.WEAPON_GRADE),
                geti(GameVars.AMMO_CAP),
                getb(GameVars.AUTOFIRE)
        );
        shopOverlay = new ShopOverlay(stats, this::purchaseShopItem, equipped -> {
            hideShopOverlay();
            continueToNextWave();
        });
        shopRoot = shopOverlay.getRoot();
        getGameScene().addUINode(shopRoot);
        shopOpen = true;
        SoundHelper.play("shop.wav");
        if (gameStarted && !showingTitleScreen) {
            getGameController().pauseEngine();
            enginePausedByOptions = true;
        }
    }

    private boolean purchaseShopItem(ShopItem item) {
        if (item == null) {
            return false;
        }
        if (!item.isUnlocked(geti(GameVars.RANK), geti(GameVars.LEVEL), geti(GameVars.METEOR_WINS),
                geti(GameVars.COGNITIVE_WINS), geti(GameVars.SQUAD_COMBOS), geti(GameVars.BOSS_KILLS))) {
            return false;
        }
        if (geti(GameVars.MONEY) < item.price()) {
            return false;
        }
        return switch (item.kind()) {
            case WEAPON -> purchaseShopWeapon(item.weapon());
            case GRADE -> {
                int target = item.targetGrade();
                if (geti(GameVars.WEAPON_GRADE) >= target) {
                    yield false;
                }
                inc(GameVars.MONEY, -item.price());
                set(GameVars.WEAPON_GRADE, target);
                showBanner(item.displayName(), Color.GOLD, 1.1);
                SoundHelper.play("weapon.wav");
                yield true;
            }
            case AUTOFIRE -> {
                if (getb(GameVars.AUTOFIRE)) {
                    yield false;
                }
                inc(GameVars.MONEY, -item.price());
                set(GameVars.AUTOFIRE, true);
                showBanner(I18n.tr("banner.auto"), Color.CYAN, 1.1);
                SoundHelper.play("autofire.wav");
                yield true;
            }
            case CONSUMABLE -> purchaseShopConsumable(item);
        };
    }

    private boolean purchaseShopConsumable(ShopItem item) {
        if (item == ShopItem.EXTRA_AMMO) {
            if (geti(GameVars.AMMO_CAP) >= GameVars.MAX_AMMO_CAP) {
                return false;
            }
            inc(GameVars.MONEY, -item.price());
            inc(GameVars.AMMO_CAP, 1);
            showBanner(I18n.tr("banner.ammo"), Color.CYAN, 1.1);
            SoundHelper.play("ammo.wav");
            return true;
        }
        if (item == ShopItem.EXTRA_TIME) {
            inc(GameVars.MONEY, -item.price());
            changeLevelTime(5);
            showBanner(I18n.tr("banner.time", 5), Color.LIMEGREEN, 1.1);
            SoundHelper.play("extra_time.wav");
            return true;
        }
        if (item == ShopItem.EXTRA_LIFE) {
            inc(GameVars.MONEY, -item.price());
            inc(GameVars.LIVES, 1);
            showExtraLifeFlash();
            SoundHelper.play("extra_life.wav");
            return true;
        }
        return false;
    }

    private boolean purchaseShopWeapon(ShopWeapon weapon) {
        if (weapon == null || weapon == ShopWeapon.NONE) {
            return false;
        }
        if (!weapon.isUnlocked(geti(GameVars.RANK), geti(GameVars.LEVEL), geti(GameVars.METEOR_WINS),
                geti(GameVars.COGNITIVE_WINS), geti(GameVars.SQUAD_COMBOS), geti(GameVars.BOSS_KILLS))) {
            return false;
        }
        if (geti(GameVars.MONEY) < weapon.price()) {
            return false;
        }
        if (currentShopWeapon() == weapon) {
            return false;
        }
        inc(GameVars.MONEY, -weapon.price());
        set(GameVars.SHOP_WEAPON, weapon.ordinal());
        clearLaserBeams();
        laserHeat = 0;
        laserCooldown = 0;
        showBanner(weapon.displayName(), Color.CYAN, 1.2);
        SoundHelper.play("weapon.wav");
        return true;
    }

    private void hideShopOverlay() {
        if (shopRoot != null) {
            getGameScene().removeUINode(shopRoot);
            shopRoot = null;
        }
        shopOverlay = null;
        if (shopOpen && enginePausedByOptions) {
            getGameController().resumeEngine();
            enginePausedByOptions = false;
        }
        shopOpen = false;
    }

    private ShopWeapon currentShopWeapon() {
        return ShopWeapon.fromId(geti(GameVars.SHOP_WEAPON));
    }

    private void clearShopWeapon() {
        set(GameVars.SHOP_WEAPON, ShopWeapon.NONE.ordinal());
        clearLaserBeams();
        laserHeat = 0;
        laserCooldown = 0;
        laserTickTimer = 0;
        java.util.Arrays.fill(laserTickTimers, 0);
        laserShotCounted = false;
        sweepActive = 0;
        sweepPause = 0;
        plasmaCooldown = 0;
        arcCooldown = 0;
    }

    private void clearLaserBeams() {
        for (Line beam : laserBeams) {
            getGameScene().removeUINode(beam);
        }
        laserBeams.clear();
    }

    private void ensureLaserBeamCount(int count, Color color) {
        while (laserBeams.size() < count) {
            Line beam = new Line();
            beam.setStroke(color);
            beam.setStrokeWidth(3.2);
            beam.setOpacity(0.9);
            beam.setMouseTransparent(true);
            getGameScene().addUINode(beam);
            laserBeams.add(beam);
        }
        while (laserBeams.size() > count) {
            Line beam = laserBeams.remove(laserBeams.size() - 1);
            getGameScene().removeUINode(beam);
        }
        for (Line beam : laserBeams) {
            beam.setStroke(color);
            beam.setVisible(true);
        }
    }

    private void updateCurses(double tpf) {
        if (controlReverseRemaining > 0) {
            controlReverseRemaining = Math.max(0, controlReverseRemaining - tpf);
        }
        if (fireJamRemaining > 0) {
            fireJamRemaining = Math.max(0, fireJamRemaining - tpf);
        }
        if (plasmaCooldown > 0) {
            plasmaCooldown = Math.max(0, plasmaCooldown - tpf);
        }
        if (arcCooldown > 0) {
            arcCooldown = Math.max(0, arcCooldown - tpf);
        }
    }

    private void updateShopWeapons(double tpf) {
        ShopWeapon weapon = currentShopWeapon();
        if (weapon == ShopWeapon.NONE || weapon == ShopWeapon.PLASMA_ORB || weapon == ShopWeapon.ARC_LANCE) {
            clearLaserBeams();
            return;
        }
        if (fireJamRemaining > 0 || findActiveMissile() != null) {
            clearLaserBeams();
            return;
        }
        // Las armas de rayo disparan SOLO mientras Espacio/X está realmente pulsado.
        // (Aquí el autodisparo no cuenta: un rayo ya es fuego continuo.)
        installFireKeyTracking();
        boolean wantFire = fireKeyPhysicallyDown();
        if (weapon == ShopWeapon.SWEEP_LASER) {
            updateSweepLaser(tpf, wantFire);
            return;
        }
        updateContinuousLaser(tpf, wantFire, weapon == ShopWeapon.TWIN_BEAM);
    }

    private boolean fireKeyTrackingInstalled = false;
    private boolean spaceDown = false;
    private boolean xDown = false;

    /** Detección propia y fiable de si la tecla de disparo se está manteniendo pulsada. */
    private void installFireKeyTracking() {
        if (fireKeyTrackingInstalled) {
            return;
        }
        javafx.stage.Stage stage = getPrimaryStage();
        if (stage == null || stage.getScene() == null) {
            return;
        }
        fireKeyTrackingInstalled = true;
        stage.getScene().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.SPACE) spaceDown = true;
            if (e.getCode() == KeyCode.X) xDown = true;
        });
        stage.getScene().addEventFilter(javafx.scene.input.KeyEvent.KEY_RELEASED, e -> {
            if (e.getCode() == KeyCode.SPACE) spaceDown = false;
            if (e.getCode() == KeyCode.X) xDown = false;
        });
        // La ventana pierde el foco -> ya no se considera pulsada ninguna tecla.
        stage.focusedProperty().addListener((obs, was, now) -> {
            if (!now) {
                spaceDown = false;
                xDown = false;
                holdingFire = false;
            }
        });
    }

    private boolean fireKeyPhysicallyDown() {
        if (!fireKeyTrackingInstalled) {
            return holdingFire;
        }
        if (!spaceDown && !xDown) {
            holdingFire = false;
        }
        return spaceDown || xDown;
    }

    private void updateContinuousLaser(double tpf, boolean wantFire, boolean twin) {
        if (laserCooldown > 0) {
            laserCooldown = Math.max(0, laserCooldown - tpf);
            clearLaserBeams();
            laserShotCounted = false;
            return;
        }
        if (!wantFire) {
            clearLaserBeams();
            laserHeat = Math.max(0, laserHeat - tpf * 1.2);
            laserShotCounted = false;
            return;
        }
        if (!laserShotCounted) {
            inc(GameVars.SHOTS_FIRED, 1);
            laserShotCounted = true;
        }
        laserHeat += tpf;
        double heatLimit = twin ? 2.5 : 2.0;
        double coolDown = twin ? 0.8 : 1.2;
        if (laserHeat >= heatLimit) {
            laserHeat = 0;
            laserCooldown = coolDown;
            clearLaserBeams();
            return;
        }

        PlayerComponent pc = player.getComponent(PlayerComponent.class);
        double originY = pc.getTopY();
        Color color = twin ? Color.AQUA : Color.CYAN;
        if (twin) {
            ensureLaserBeamCount(2, color);
            fireLaserColumn(laserBeams.get(0), pc.getCenterX() - 14, originY, 0, 1, tpf, TWIN_TICK, 0);
            fireLaserColumn(laserBeams.get(1), pc.getCenterX() + 14, originY, 0, 1, tpf, TWIN_TICK, 1);
        } else {
            ensureLaserBeamCount(1, color);
            fireLaserColumn(laserBeams.get(0), pc.getCenterX(), originY, 0, 1, tpf, NEEDLE_TICK, 0);
        }
    }

    private void updateSweepLaser(double tpf, boolean wantFire) {
        if (sweepPause > 0) {
            sweepPause = Math.max(0, sweepPause - tpf);
            clearLaserBeams();
            laserShotCounted = false;
            return;
        }
        if (!wantFire) {
            clearLaserBeams();
            sweepActive = 0;
            laserShotCounted = false;
            return;
        }
        if (!laserShotCounted) {
            inc(GameVars.SHOTS_FIRED, 1);
            laserShotCounted = true;
        }
        sweepActive += tpf;
        if (sweepActive >= 2.0) {
            sweepActive = 0;
            sweepPause = 0.8;
            clearLaserBeams();
            return;
        }
        sweepAngle = Math.sin(sweepActive * Math.PI * 2.0 / 1.2) * 55.0;
        PlayerComponent pc = player.getComponent(PlayerComponent.class);
        ensureLaserBeamCount(1, Color.ORANGE);
        fireLaserColumn(laserBeams.get(0), pc.getCenterX(), pc.getTopY(), sweepAngle, 2, tpf, SWEEP_TICK, 0);
    }

    private void fireLaserColumn(Line beam, double originX, double originY, double angleDeg, int damage, double tpf,
                                 double tickEvery, int timerIdx) {
        double rad = Math.toRadians(angleDeg);
        double dirX = Math.sin(rad);
        double dirY = -Math.cos(rad);
        double endX = originX + dirX * getAppHeight();
        double endY = originY + dirY * getAppHeight();

        Entity nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (Entity enemy : getGameWorld().getEntitiesByType(EntityType.ENEMY)) {
            if (!enemy.isActive() || !enemy.hasComponent(EnemyComponent.class)) {
                continue;
            }
            EnemyComponent ec = enemy.getComponent(EnemyComponent.class);
            if (ec.isScooped() || ec.isTuckedBehindMeteor()) {
                continue;
            }
            double cx = enemy.getX() + enemy.getWidth() / 2.0;
            double cy = enemy.getY() + enemy.getHeight() / 2.0;
            double dx = cx - originX;
            double dy = cy - originY;
            double proj = dx * dirX + dy * dirY;
            if (proj < 0) {
                continue;
            }
            double perp = Math.abs(dx * -dirY + dy * dirX);
            if (perp > Math.max(enemy.getWidth(), enemy.getHeight()) * 0.55) {
                continue;
            }
            if (proj < nearestDist) {
                nearestDist = proj;
                nearest = enemy;
            }
        }

        if (nearest != null) {
            endX = nearest.getX() + nearest.getWidth() / 2.0;
            endY = nearest.getY() + nearest.getHeight() / 2.0;
        }
        beam.setStartX(originX);
        beam.setStartY(originY);
        beam.setEndX(endX);
        beam.setEndY(endY);

        laserTickTimers[timerIdx] += tpf;
        if (nearest != null && laserTickTimers[timerIdx] >= tickEvery) {
            laserTickTimers[timerIdx] = 0;
            int dmg = damage;
            EnemyComponent tec = nearest.getComponent(EnemyComponent.class);
            if (tec.isBoss() && currentShopWeapon() != ShopWeapon.SWEEP_LASER) {
                // Needle/Twin solo hacen la mitad de daño al jefe (el resto se acumula).
                bossLaserCarry += damage * LASER_BOSS_FACTOR;
                dmg = (int) bossLaserCarry;
                bossLaserCarry -= dmg;
            }
            if (dmg > 0) {
                damageEnemy(nearest, dmg);
            }
            inc(GameVars.HITS_LANDED, 1);
        }
    }

    // Equilibrio de los láseres: Needle 1 de daño / 0,15 s; Twin 1 por rayo / 0,20 s; Sweep sin cambios (2 / 0,10 s).
    private static final double NEEDLE_TICK = 0.15;
    private static final double TWIN_TICK = 0.20;
    private static final double SWEEP_TICK = 0.10;
    private static final double LASER_BOSS_FACTOR = 0.5;
    private final double[] laserTickTimers = new double[2];
    private double bossLaserCarry = 0;

    private void fireShopImpulse(ShopWeapon weapon) {
        if (weapon == ShopWeapon.PLASMA_ORB) {
            if (plasmaCooldown > 0) {
                return;
            }
            int cap = geti(GameVars.AMMO_CAP);
            int active = geti(GameVars.ACTIVE_BULLETS);
            if (active + 2 > cap) {
                return;
            }
            PlayerComponent pc = player.getComponent(PlayerComponent.class);
            pc.onFired();
            plasmaCooldown = 0.28;
            spawn("plasmaOrb", new com.almasb.fxgl.entity.SpawnData(pc.getCenterX() - 14, pc.getTopY() - 10)
                    .put("speedX", 0.0)
                    .put("speedY", -280.0)
                    .put("pierce", 1)
                    .put("coreDamage", 3)
                    .put("splashDamage", 2)
                    .put("splashRadius", 64.0));
            inc(GameVars.ACTIVE_BULLETS, 2);
            inc(GameVars.SHOTS_FIRED, 1);
            SoundHelper.play("shot.wav");
            return;
        }
        if (weapon == ShopWeapon.ARC_LANCE) {
            if (arcCooldown > 0) {
                return;
            }
            arcCooldown = 0.30;
            inc(GameVars.SHOTS_FIRED, 1);
            fireArcLance();
            SoundHelper.play("weapon.wav");
        }
    }

    private void fireArcLance() {
        PlayerComponent pc = player.getComponent(PlayerComponent.class);
        double ox = pc.getCenterX();
        double oy = pc.getTopY();
        List<Entity> hit = new ArrayList<>();
        Entity current = nearestEnemyFrom(ox, oy, 240, hit);
        int jumps = 0;
        while (current != null && jumps < 5) {
            hit.add(current);
            damageEnemy(current, 2);
            inc(GameVars.HITS_LANDED, 1);
            spawnExplosion(current.getX() + current.getWidth() / 2.0,
                    current.getY() + current.getHeight() / 2.0, "hit");
            jumps++;
            double cx = current.getX() + current.getWidth() / 2.0;
            double cy = current.getY() + current.getHeight() / 2.0;
            current = nearestEnemyFrom(cx, cy, 100, hit);
        }
    }

    private Entity nearestEnemyFrom(double x, double y, double maxDist, List<Entity> exclude) {
        Entity best = null;
        double bestDist = maxDist;
        for (Entity enemy : getGameWorld().getEntitiesByType(EntityType.ENEMY)) {
            if (!enemy.isActive() || !enemy.hasComponent(EnemyComponent.class) || exclude.contains(enemy)) {
                continue;
            }
            EnemyComponent ec = enemy.getComponent(EnemyComponent.class);
            if (ec.isScooped() || ec.isTuckedBehindMeteor()) {
                continue;
            }
            double cx = enemy.getX() + enemy.getWidth() / 2.0;
            double cy = enemy.getY() + enemy.getHeight() / 2.0;
            double dist = Math.hypot(cx - x, cy - y);
            if (dist <= bestDist) {
                bestDist = dist;
                best = enemy;
            }
        }
        return best;
    }

    private void applyPlasmaHit(Entity orb, Entity enemy) {
        PlasmaOrbComponent plasma = orb.getComponent(PlasmaOrbComponent.class);
        EnemyComponent ec = enemy.getComponent(EnemyComponent.class);
        damageEnemy(enemy, plasma.getCoreDamage());
        inc(GameVars.HITS_LANDED, 1);
        double cx = enemy.getX() + enemy.getWidth() / 2.0;
        double cy = enemy.getY() + enemy.getHeight() / 2.0;
        applySplashDamage(cx, cy, plasma.getSplashRadius(), plasma.getSplashDamage(), enemy);
        spawnExplosion(cx, cy, "hit");
        if (plasma.hitAndShouldRemove()) {
            plasma.releaseAmmoSlots();
            if (orb.isActive()) {
                orb.removeFromWorld();
            }
        }
    }

    private void applySplashDamage(double x, double y, double radius, int damage, Entity ignore) {
        for (Entity enemy : List.copyOf(getGameWorld().getEntitiesByType(EntityType.ENEMY))) {
            if (enemy == ignore || !enemy.isActive() || !enemy.hasComponent(EnemyComponent.class)) {
                continue;
            }
            EnemyComponent ec = enemy.getComponent(EnemyComponent.class);
            if (ec.isScooped()) {
                continue;
            }
            double cx = enemy.getX() + enemy.getWidth() / 2.0;
            double cy = enemy.getY() + enemy.getHeight() / 2.0;
            if (Math.hypot(cx - x, cy - y) <= radius) {
                damageEnemy(enemy, damage);
                inc(GameVars.HITS_LANDED, 1);
            }
        }
    }

    private void damageEnemy(Entity enemy, int amount) {
        if (enemy == null || !enemy.isActive() || !enemy.hasComponent(EnemyComponent.class)) {
            return;
        }
        EnemyComponent ec = enemy.getComponent(EnemyComponent.class);
        if (ec.isScooped() || ec.isDead()) {
            return;
        }
        ec.hit(amount);
        if (ec.isDead()) {
            finishEnemyKill(enemy, ec);
        }
    }

    private void applySkullCurse() {
        SoundHelper.play("bad_drop.wav");
        int roll = RANDOM.nextInt(9);
        switch (roll) {
            case 0 -> {
                clearShopWeapon();
                if (geti(GameVars.WEAPON_GRADE) > 1) {
                    inc(GameVars.WEAPON_GRADE, -1);
                }
                showBanner(I18n.tr("curse.weaponDown"), Color.ORANGERED, 1.4);
            }
            case 1 -> {
                pendingHurryUfo = true;
                hurryUfoDelay = 0;
                spawnHurryUpUfo();
                showBanner(I18n.tr("curse.ufoNow"), Color.ORANGERED, 1.4);
            }
            case 2 -> {
                controlReverseRemaining = GameVars.CURSE_REVERSE_SECONDS;
                showBanner(I18n.tr("curse.reverse"), Color.ORANGERED, 1.4);
            }
            case 3 -> {
                fireJamRemaining = GameVars.CURSE_JAM_SECONDS;
                clearLaserBeams();
                showBanner(I18n.tr("curse.jam"), Color.ORANGERED, 1.4);
            }
            case 4 -> {
                changeLevelTime(-5);
                showBanner(I18n.tr("curse.timeDrain"), Color.ORANGERED, 1.4);
            }
            case 5 -> {
                int cap = Math.max(GameVars.INITIAL_AMMO_CAP, geti(GameVars.AMMO_CAP) - 2);
                set(GameVars.AMMO_CAP, cap);
                showBanner(I18n.tr("curse.ammoLeak"), Color.ORANGERED, 1.4);
            }
            case 6 -> {
                set(GameVars.MONEY, Math.max(0, geti(GameVars.MONEY) - 50));
                showBanner(I18n.tr("curse.moneySting"), Color.ORANGERED, 1.4);
            }
            case 7 -> {
                int mask = geti(GameVars.RANK_MASK);
                if (mask != 0) {
                    List<Integer> bits = new ArrayList<>();
                    for (int i = 0; i < GameVars.RANK_COLOR_COUNT; i++) {
                        if ((mask & (1 << i)) != 0) {
                            bits.add(i);
                        }
                    }
                    if (!bits.isEmpty()) {
                        int bit = bits.get(RANDOM.nextInt(bits.size()));
                        set(GameVars.RANK_MASK, mask & ~(1 << bit));
                        updateRankHud();
                    }
                }
                showBanner(I18n.tr("curse.rankBleed"), Color.ORANGERED, 1.4);
            }
            default -> {
                spawnSkullSwarm();
                showBanner(I18n.tr("curse.swarm"), Color.ORANGERED, 1.4);
            }
        }
    }

    private void spawnSkullSwarm() {
        int count = 3 + RANDOM.nextInt(3);
        for (int i = 0; i < count; i++) {
            double x = GameVars.RAIL_WIDTH + 20 + RANDOM.nextDouble() * (getAppWidth() - GameVars.RAIL_WIDTH * 2 - 40);
            double y = -40 - RANDOM.nextDouble() * 80;
            spawn("enemy", new com.almasb.fxgl.entity.SpawnData(x, y)
                    .put("enemyType", EnemyComponent.EnemyType.FAST)
                    .put("level", geti(GameVars.LEVEL)));
            inc(GameVars.ENEMIES_REMAINING, 1);
        }
    }

    private void continueToNextWave() {
        if (gameOver || showingTitleScreen) {
            return;
        }
        clearScoops();
        clearWaveEntities();
        inc(GameVars.LEVEL, 1);
        startGetReady();
    }

    private boolean scoopDropAllowed() {
        if (waveManager == null) {
            return false;
        }
        return switch (waveManager.getCurrentWaveType()) {
            case FIGHTERS, MIXED, PEARL, RANK, BONUS, KAMIKAZE, MONSTER,
                 SPINNER, PINCER, CASCADE, RUSH -> true;
            default -> false;
        };
    }

    public boolean isMeteorChallenge() {
        return waveManager != null && waveManager.getCurrentWaveType() == WaveManager.WaveType.METEOR;
    }

    private boolean shotBlockedByMeteor(Entity shot) {
        return meteorCoveringShot(shot) != null;
    }

    private Entity meteorCoveringShot(Entity shot) {
        if (shot == null || !shot.isActive()) {
            return null;
        }
        for (Entity meteor : getGameWorld().getEntitiesByType(EntityType.METEOR)) {
            if (!meteor.isActive() || !meteor.hasComponent(CoverMeteorComponent.class)) {
                continue;
            }
            if (meteor.getComponent(CoverMeteorComponent.class).coversShot(shot)) {
                return meteor;
            }
        }
        return null;
    }

    private Entity nearestCoverMeteor(Entity around) {
        if (around == null) {
            return null;
        }
        double ax = around.getX() + around.getWidth() / 2.0;
        double ay = around.getY() + around.getHeight() / 2.0;
        Entity best = null;
        double bestD = 90 * 90;
        for (Entity meteor : getGameWorld().getEntitiesByType(EntityType.METEOR)) {
            if (!meteor.isActive()) {
                continue;
            }
            double dx = (meteor.getX() + meteor.getWidth() / 2.0) - ax;
            double dy = (meteor.getY() + meteor.getHeight() / 2.0) - ay;
            double d = dx * dx + dy * dy;
            if (d < bestD) {
                bestD = d;
                best = meteor;
            }
        }
        return best;
    }

    private void updateMeteorMissileSupply(double tpf) {
        if (!isMeteorChallenge() || waveTransition || getReadyRemaining > 0 || gameOver || minigameActive) {
            return;
        }
        if (findActiveMissile() != null) {
            return;
        }
        for (Entity pickup : getGameWorld().getEntitiesByType(EntityType.PICKUP)) {
            if (!pickup.isActive() || !pickup.hasComponent(PickupComponent.class)) {
                continue;
            }
            if (pickup.getComponent(PickupComponent.class).getType() == PickupComponent.PickupType.MISSILE) {
                return;
            }
        }
        meteorMissileCooldown -= tpf;
        if (meteorMissileCooldown > 0) {
            return;
        }
        meteorMissileCooldown = GameVars.METEOR_MISSILE_GAP;
        double left = GameVars.RAIL_WIDTH + 16;
        double span = Math.max(40, getAppWidth() - 2.0 * GameVars.RAIL_WIDTH - 48);
        double x = left + RANDOM.nextDouble() * span;
        spawn("missilePickup", x, -28);
    }

    private int playerScoopCharges() {
        if (player == null || !player.isActive() || !player.hasComponent(PlayerComponent.class)) {
            return 0;
        }
        return player.getComponent(PlayerComponent.class).getScoopCharges();
    }

    private void attachScoops() {
        if (player == null || !player.isActive() || !player.hasComponent(PlayerComponent.class)) {
            return;
        }
        player.getComponent(PlayerComponent.class).attachScoops();
        updateScoopCatch();
    }

    private void updateScoopCatch() {
        if (player == null || !player.isActive() || playerScoopCharges() <= 0 || scoopSlotsUsed() >= GameVars.SCOOP_COUNT) {
            return;
        }
        double padX = 28;
        double padTop = 36;
        double padBottom = 12;
        double x1 = player.getX() - padX;
        double y1 = player.getY() - padTop;
        double x2 = player.getX() + player.getWidth() + padX;
        double y2 = player.getY() + player.getHeight() + padBottom;
        for (Entity enemy : List.copyOf(getGameWorld().getEntitiesByType(EntityType.ENEMY))) {
            if (!enemy.isActive() || !enemy.hasComponent(EnemyComponent.class)) {
                continue;
            }
            EnemyComponent ec = enemy.getComponent(EnemyComponent.class);
            if (ec.isBoss() || ec.isScooped()) {
                continue;
            }
            if (scoopSlotsUsed() >= GameVars.SCOOP_COUNT) {
                return;
            }
            double ex2 = enemy.getX() + enemy.getWidth();
            double ey2 = enemy.getY() + enemy.getHeight();
            if (enemy.getX() < x2 && ex2 > x1 && enemy.getY() < y2 && ey2 > y1) {
                if (ec.beginScoop(nextDockSlot())) {
                    SoundHelper.play("scoop.wav");
                }
            }
        }
    }

    private int scoopSlotsUsed() {
        int used = 0;
        for (Entity enemy : getGameWorld().getEntitiesByType(EntityType.ENEMY)) {
            if (!enemy.isActive() || !enemy.hasComponent(EnemyComponent.class)) {
                continue;
            }
            if (enemy.getComponent(EnemyComponent.class).isScooped()) {
                used++;
            }
        }
        return used;
    }

    private int nextDockSlot() {
        boolean left = false;
        boolean right = false;
        for (Entity enemy : getGameWorld().getEntitiesByType(EntityType.ENEMY)) {
            if (!enemy.isActive() || !enemy.hasComponent(EnemyComponent.class)) {
                continue;
            }
            EnemyComponent ec = enemy.getComponent(EnemyComponent.class);
            if (!ec.isScooped()) {
                continue;
            }
            if (ec.getDockSlot() == 0) {
                left = true;
            } else if (ec.getDockSlot() == 1) {
                right = true;
            }
        }
        if (!left) {
            return 0;
        }
        if (!right) {
            return 1;
        }
        return -1;
    }

    private void clearScoops() {
        for (Entity enemy : List.copyOf(getGameWorld().getEntitiesByType(EntityType.ENEMY))) {
            if (!enemy.isActive() || !enemy.hasComponent(EnemyComponent.class)) {
                continue;
            }
            if (enemy.getComponent(EnemyComponent.class).isScooped()) {
                enemy.removeFromWorld();
            }
        }
        if (player != null && player.hasComponent(PlayerComponent.class)) {
            player.getComponent(PlayerComponent.class).clearScoops();
        }
    }

    public void completeScoop(Entity enemy, EnemyComponent ec) {
        if (enemy == null || !enemy.isActive() || ec == null) {
            return;
        }
        if (ec.getDockSlot() < 0) {
            finishEnemyKill(enemy, ec, true);
            return;
        }
        inc(GameVars.SCORE, ec.getScoreValue());
        inc(GameVars.ENEMIES_REMAINING, -1);
        waveManager.onEnemyDestroyed(ec.getSquadId(), ec.isEntering());
        if (ec.isMonster() && waveManager.noteMonsterKillAndCheckBoss()) {
            runOnce(this::spawnAlienBoss, Duration.seconds(1.5));
            inc(GameVars.ENEMIES_REMAINING, 1); // mantener la oleada abierta hasta que caiga el jefe
            alienBossPending = true;
        }
        ec.dock();
        waveManager.checkKillBonuses();
        checkWaveComplete();
    }

    private boolean hasScoopEscorts() {
        for (Entity enemy : getGameWorld().getEntitiesByType(EntityType.ENEMY)) {
            if (enemy.isActive() && enemy.hasComponent(EnemyComponent.class)
                    && enemy.getComponent(EnemyComponent.class).isScooped()) {
                return true;
            }
        }
        return false;
    }

    private void hitScoopFormation() {
        if (scoopHitLock > 0 || minigameActive || warping) {
            return;
        }
        if (player != null && player.isActive() && player.hasComponent(PlayerComponent.class)
                && player.getComponent(PlayerComponent.class).isInvulnerable()) {
            PlayerComponent shielded = player.getComponent(PlayerComponent.class);
            if (shielded.isShielded()) {
                shielded.pingShieldHit();
            }
            return;
        }
        if (destroyScoopedBySlot(0) || destroyScoopedBySlot(1)) {
            scoopHitLock = 0.35;
            return;
        }
        if (player == null || !player.isActive() || !player.hasComponent(PlayerComponent.class)) {
            return;
        }
        playerHit(player.getComponent(PlayerComponent.class));
    }

    private boolean destroyScoopedBySlot(int slot) {
        for (Entity enemy : List.copyOf(getGameWorld().getEntitiesByType(EntityType.ENEMY))) {
            if (!enemy.isActive() || !enemy.hasComponent(EnemyComponent.class)) {
                continue;
            }
            EnemyComponent ec = enemy.getComponent(EnemyComponent.class);
            if (ec.isScooped() && ec.getDockSlot() == slot) {
                destroyDockedEscort(enemy);
                return true;
            }
        }
        return false;
    }

    private void destroyDockedEscort(Entity escort) {
        if (escort == null || !escort.isActive()) {
            return;
        }
        spawnExplosion(escort.getX() + escort.getWidth() / 2.0, escort.getY() + escort.getHeight() / 2.0, "ship");
        SoundHelper.play("explode_ship.wav");
        escort.removeFromWorld();
    }

    private void finishEnemyKill(Entity enemy, EnemyComponent ec) {
        finishEnemyKill(enemy, ec, false);
    }

    private void finishEnemyKill(Entity enemy, EnemyComponent ec, boolean scooped) {
        if (enemy == null || !enemy.isActive() || ec == null) {
            return;
        }
        inc(GameVars.SCORE, ec.getScoreValue());
        inc(GameVars.ENEMIES_REMAINING, -1);
        waveManager.onEnemyDestroyed(ec.getSquadId(), ec.isEntering());

        if (!scooped) {
            double deathX = enemy.getX() + enemy.getWidth() / 2;
            double deathY = enemy.getY() + enemy.getHeight() / 2;
            if (ec.isBoss()) {
                inc(GameVars.BOSS_KILLS, 1);
                spawnBossDeathSequence(deathX, deathY);
                spawnBossCoins(deathX, deathY);
                // Recompensa: mucho dinero + escudo o misil garantizado.
                int bossMoney = GameVars.BOSS_KILL_MONEY * ec.getBossNumber();
                inc(GameVars.MONEY, bossMoney);
                spawn(RANDOM.nextBoolean() ? "shieldPickup" : "missilePickup", deathX - 14, deathY - 14);
                showBanner(I18n.tr("banner.bossDefeated", ec.getScoreValue(), bossMoney), Color.GOLD, 2.0);
                trySpawnRankMarker(deathX, deathY, ec.getType());
            } else {
                String explosionSize = (ec.getType() == EnemyComponent.EnemyType.TOUGH) ? "big" : "ship";
                spawnExplosion(deathX, deathY, explosionSize);
                SoundHelper.play("explode_ship.wav");
                trySpawnPickup(deathX, deathY, ec.getType());
                trySpawnRankMarker(deathX, deathY, ec.getType());
            }
        }
        enemy.removeFromWorld();
        waveManager.checkKillBonuses();
        checkWaveComplete();
    }

    private void ensureWarpFlash() {
        if (warpFlash != null) {
            return;
        }
        warpFlash = new Rectangle(getAppWidth(), getAppHeight());
        warpFlash.setFill(Color.rgb(140, 210, 255, 0.0));
        warpFlash.setMouseTransparent(true);
        getGameScene().addUINode(warpFlash);
    }

    private void applyWarpVisuals(double intensity) {
        StarComponent.setWarp(intensity);
        BackgroundScrollComponent.setWarp(intensity);
        if (warpFlash != null) {
            warpFlash.setFill(Color.rgb(150, 220, 255, 0.04 + intensity * 0.16));
        }
    }

    private void stopWarpVisuals() {
        warping = false;
        warpElapsed = 0;
        StarComponent.setWarp(0);
        BackgroundScrollComponent.setWarp(0);
        SoundHelper.stop("warp.mp3");
        if (warpFlash != null) {
            getGameScene().removeUINode(warpFlash);
            warpFlash = null;
        }
    }

    @Override
    public void displayBanner(String message, Color color, double seconds) {
        showBanner(message, color, seconds);
    }

    @Override
    public Entity player() {
        return player;
    }

    @Override
    public boolean holdingFire() {
        return holdingFire;
    }

    @Override
    public boolean movingLeft() {
        return movingLeft;
    }

    @Override
    public boolean movingRight() {
        return movingRight;
    }

    @Override
    public boolean movingUp() {
        return movingUp;
    }

    @Override
    public boolean movingDown() {
        return movingDown;
    }

    @Override
    public double playLeft() {
        return GameVars.RAIL_WIDTH;
    }

    @Override
    public double playRight() {
        return getAppWidth() - GameVars.RAIL_WIDTH;
    }

    @Override
    public double playTop() {
        return 48;
    }

    @Override
    public double playBottom() {
        return getAppHeight();
    }

    @Override
    public void addUi(Node node) {
        getGameScene().addUINode(node);
    }

    @Override
    public void removeUi(Node node) {
        getGameScene().removeUINode(node);
    }

    @Override
    public void hidePlayer(boolean hide) {
        if (player != null && player.isActive()) {
            player.getViewComponent().setOpacity(hide ? 0.0 : 1.0);
        }
    }

    @Override
    public void playMusicOverride(String fileName) {
        MusicHelper.playOverride(fileName);
    }

    @Override
    public void restoreMusic() {
        MusicHelper.applyFromStore();
    }

    @Override
    public void spawnCoinAt(double x, double y, String coinType) {
        spawnCoin(x, y, coinType);
    }

    private static final double MAX_TPF = 1.0 / 45.0;

    @Override
    protected void onUpdate(double tpf) {
        updateShieldHud();
        updateBossBar();
        if (showingTitleScreen || gameOver || optionsOpen || shopOpen || settlementOverlay != null || player == null) return;

        frameCount++;
        if (frameCount <= WARMUP_FRAMES) {
            tpf = Math.min(tpf, 0.008);
        } else {
            tpf = Math.min(tpf, MAX_TPF);
        }

        if (minigameActive && activeMinigame != null) {
            if (activeMinigame.usesPlayerShip()) {
                PlayerComponent pc = player.getComponent(PlayerComponent.class);
                if (movingLeft) {
                    pc.moveLeft(tpf);
                }
                if (movingRight) {
                    pc.moveRight(tpf);
                }
                pc.updateIdle();
            }
            activeMinigame.update(tpf);
            return;
        }

        if (warping) {
            PlayerComponent warpPilot = player.getComponent(PlayerComponent.class);
            if (movingLeft) {
                warpPilot.moveLeft(tpf);
            }
            if (movingRight) {
                warpPilot.moveRight(tpf);
            }
            warpPilot.updateIdle();
            updateWarp(tpf);
            return;
        }

        PlayerComponent pc = player.getComponent(PlayerComponent.class);
        Entity missile = findActiveMissile();
        if (missile != null && missile.hasComponent(MissileComponent.class)) {
            MissileComponent mc = missile.getComponent(MissileComponent.class);
            mc.setSteering(holdingFire);
            boolean left = controlReverseRemaining > 0 ? movingRight : movingLeft;
            boolean right = controlReverseRemaining > 0 ? movingLeft : movingRight;
            mc.setTurnInput(left, right);
            pc.setMissileShield(true);
        } else if (pc.isMissileShielded()) {
            pc.setMissileShield(false);
        }
        boolean steeringMissile = holdingFire && missile != null;
        if (!steeringMissile) {
            boolean left = controlReverseRemaining > 0 ? movingRight : movingLeft;
            boolean right = controlReverseRemaining > 0 ? movingLeft : movingRight;
            if (left) {
                pc.moveLeft(tpf);
            }
            if (right) {
                pc.moveRight(tpf);
            }
        }
        pc.updateIdle();
        if (scoopHitLock > 0) {
            scoopHitLock = Math.max(0, scoopHitLock - tpf);
        }
        updateScoopCatch();
        updateMeteorMissileSupply(tpf);
        updateCurses(tpf);
        updateShopWeapons(tpf);

        updateGetReady(tpf);

        if (waveManager != null && !waveTransition) {
            waveManager.update(tpf);
            updateBossCollect(tpf);
            if (isBossWave()) {
                // Nivel de jefe: sin límite de tiempo.
                extraTime = extraTimeMax;
                extraTimeWarned = true;
                pendingHurryUfo = false;
            } else {
                updateExtraTime(tpf);
            }
            updateExtraTimeBar();
            checkWaveComplete();
        }
    }

    private void updateExtraTime(double tpf) {
        if (hurryUfoDelay > 0) {
            hurryUfoDelay = Math.max(0, hurryUfoDelay - tpf);
        }
        if (extraTime > 0) {
            extraTime = Math.max(0, extraTime - tpf);
            extraTimePulse += tpf;
        }
        if (!extraTimeWarned && extraTime <= 5) {
            triggerHurryUpWarning();
        }
        if (extraTime <= 0) {
            extraTime = 0;
            if (!extraTimeWarned) {
                triggerHurryUpWarning();
            }
            if (hurryUfoDelay <= 0) {
                pendingHurryUfo = true;
            }
        }
        if (pendingHurryUfo && hurryUfoDelay <= 0) {
            boolean bonus = waveManager != null && (waveManager.getCurrentWaveType() == WaveManager.WaveType.BONUS
                    || waveManager.getCurrentWaveType() == WaveManager.WaveType.BOSS);
            if (bonus) {
                pendingHurryUfo = false;
            } else {
                spawnHurryUpUfo();
            }
        }
        updateExtraTimeBar();
    }

    private void triggerHurryUpWarning() {
        if (isBossWave()) {
            extraTimeWarned = true;
            return;
        }
        if (extraTimeWarned) {
            return;
        }
        extraTimeWarned = true;
        hurryUfoDelay = Math.max(hurryUfoDelay, GameVars.HURRY_UFO_DELAY);
        SoundHelper.play("hurry_warning.wav");
        showBanner(I18n.tr("banner.hurryUp"), Color.ORANGERED, 1.2);
    }

    private void spawnHurryUpUfo() {
        if (ufoSpawnedThisWave || gameOver || showingTitleScreen || warping || minigameActive) {
            return;
        }
        if (waveTransition || getReadyRemaining > 0) {
            // Se deja pendiente para reintentarlo cuando se reanude el combate o se estabilice la oleada.
            pendingHurryUfo = true;
            return;
        }
        ufoSpawnedThisWave = true;
        pendingHurryUfo = false;
        double x = getAppWidth() / 2.0 - 48;
        double y = 56;
        spawn("enemy", new com.almasb.fxgl.entity.SpawnData(x, y)
                .put("enemyType", EnemyComponent.EnemyType.UFO)
                .put("level", geti(GameVars.LEVEL)));
        inc(GameVars.ENEMIES_REMAINING, 1);
        SoundHelper.play("ufo_appear.wav");
        showBanner(I18n.tr("banner.ufo"), Color.CYAN, 1.3);
    }

    private Group[] extraLetterGroups = new Group[5];
    private Text[] extraLetterTexts = new Text[5];
    private Group[] extraFlipWrappers = new Group[5];
    private Rectangle ammoBar;
    private Rectangle weaponBar;
    private Rectangle livesBar;
    private Rectangle extraTimeBar;
    private Rectangle timeBarBg;
    private Text timeLabelNode;

    private boolean isBossWave() {
        return waveManager != null && waveManager.getCurrentWaveType() == WaveManager.WaveType.BOSS;
    }
    private Text extraTimeMaxText;
    private double extraTimeBarMaxWidth;
    private Circle[] rankDots = new Circle[GameVars.RANK_COLOR_COUNT];
    private Text rankTitleText;
    private Rectangle autoLamp;
    private Text autoLampText;
    private Text shieldHudText;
    private Text accuracyHudText;
    private Rectangle comboPlate;

    private static final Color[] LETTER_COLORS = {
        Color.rgb(255, 80, 80),
        Color.rgb(80, 255, 80),
        Color.rgb(80, 180, 255),
        Color.rgb(255, 180, 80),
        Color.rgb(200, 80, 255)
    };

    @Override
    protected void initUI() {
        int railWidth = GameVars.RAIL_WIDTH;
        int xOffset = 4;
        int extraY = 36;
        int letterSpacing = 32;

        for (int i = 0; i < 5; i++) {
            char letter = GameVars.EXTRA_LETTERS[i];
            Group letterGroup = createAnimatedLetterSlot(letter, i, xOffset + 22, extraY + i * letterSpacing);
            extraLetterGroups[i] = letterGroup;

            final int idx = i;
            getip(GameVars.EXTRA_VARS[i]).addListener((obs, oldVal, newVal) -> {
                updateExtraLetter(idx, newVal.intValue() > 0, LETTER_COLORS[idx]);
            });

            getGameScene().addUINode(letterGroup);
        }

        int barsY = extraY + 5 * letterSpacing + 14;
        int barWidth = railWidth - 26;
        int barHeight = 7;
        int barSpacing = 20;
        int labelOffset = 14;

        Text ammoLabelB = new Text("B");
        ammoLabelB.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
        ammoLabelB.setFill(Color.DEEPSKYBLUE);
        ammoLabelB.setTranslateX(xOffset);
        ammoLabelB.setTranslateY(barsY + barHeight);
        getGameScene().addUINode(ammoLabelB);

        Rectangle ammoBarBg = createBarBackground(xOffset + labelOffset, barsY, barWidth, barHeight);
        ammoBar = createStatusBar(xOffset + labelOffset + 1, barsY + 1, barWidth - 2, barHeight - 2, Color.DEEPSKYBLUE);
        getGameScene().addUINode(ammoBarBg);
        getGameScene().addUINode(ammoBar);

        Text weaponLabelW = new Text("W");
        weaponLabelW.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
        weaponLabelW.setFill(Color.ORANGE);
        weaponLabelW.setTranslateX(xOffset);
        weaponLabelW.setTranslateY(barsY + barSpacing + barHeight);
        getGameScene().addUINode(weaponLabelW);

        Rectangle weaponBarBg = createBarBackground(xOffset + labelOffset, barsY + barSpacing, barWidth, barHeight);
        weaponBar = createStatusBar(xOffset + labelOffset + 1, barsY + barSpacing + 1, barWidth - 2, barHeight - 2, Color.ORANGE);
        getGameScene().addUINode(weaponBarBg);
        getGameScene().addUINode(weaponBar);

        Rectangle livesBarBg = createBarBackground(xOffset + labelOffset, barsY + barSpacing * 2, barWidth, barHeight);
        livesBar = createStatusBar(xOffset + labelOffset + 1, barsY + barSpacing * 2 + 1, barWidth - 2, barHeight - 2, Color.LIMEGREEN);
        getGameScene().addUINode(livesBarBg);
        getGameScene().addUINode(livesBar);

        int timeBlockY = barsY + barSpacing * 3;
        Text timeLabelT = new Text("T");
        timeLabelT.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
        timeLabelT.setFill(Color.LIMEGREEN);
        timeLabelT.setTranslateX(xOffset);
        timeLabelT.setTranslateY(timeBlockY + 10);
        getGameScene().addUINode(timeLabelT);
        timeLabelNode = timeLabelT;

        extraTimeMaxText = new Text(String.valueOf(extraTimeMax));
        extraTimeMaxText.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
        extraTimeMaxText.setFill(Color.LIMEGREEN);
        extraTimeMaxText.setTranslateX(xOffset + labelOffset);
        extraTimeMaxText.setTranslateY(timeBlockY + 10);
        extraTimeMaxText.setEffect(new DropShadow(2, Color.BLACK));
        getGameScene().addUINode(extraTimeMaxText);

        timeBarBg = createBarBackground(xOffset, timeBlockY + 13, railWidth - 10, barHeight);
        extraTimeBar = createStatusBar(xOffset + 1, timeBlockY + 14, railWidth - 12, barHeight - 2, Color.LIME);
        extraTimeBarMaxWidth = railWidth - 12;
        getGameScene().addUINode(timeBarBg);
        getGameScene().addUINode(extraTimeBar);

        int rankY = timeBlockY + 34;
        double dotRadius = 4.4;
        double dotGapX = 16;
        double dotGapY = 13;
        double dotStartX = xOffset + 10;
        for (int i = 0; i < GameVars.RANK_COLOR_COUNT; i++) {
            int col = i % 3;
            int row = i / 3;
            Circle dot = new Circle(dotRadius);
            dot.setCenterX(dotStartX + col * dotGapX);
            dot.setCenterY(rankY + row * dotGapY);
            dot.setStrokeWidth(1.8);
            rankDots[i] = dot;
            styleRankDot(dot, i, false);
            getGameScene().addUINode(dot);
        }
        rankTitleText = new Text();
        rankTitleText.setFont(Font.font("Monospace", FontWeight.BOLD, 8));
        rankTitleText.setFill(Color.GOLD);
        rankTitleText.setWrappingWidth(railWidth - 8);
        rankTitleText.setTranslateX(xOffset);
        rankTitleText.setTranslateY(rankY + dotGapY + 14);
        getGameScene().addUINode(rankTitleText);
        updateRankHud();

        int shieldY = rankY + 44;
        Text shieldLabel = new Text("S");
        shieldLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
        shieldLabel.setFill(Color.AQUA);
        shieldLabel.setTranslateX(xOffset);
        shieldLabel.setTranslateY(shieldY);
        getGameScene().addUINode(shieldLabel);

        shieldHudText = new Text("0");
        shieldHudText.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
        shieldHudText.setFill(Color.rgb(60, 80, 90));
        shieldHudText.setTranslateX(xOffset + 14);
        shieldHudText.setTranslateY(shieldY);
        shieldHudText.setEffect(new DropShadow(2, Color.BLACK));
        getGameScene().addUINode(shieldHudText);
        updateShieldHud();

        int autoY = shieldY + 20;
        autoLamp = new Rectangle(10, 10);
        autoLamp.setFill(Color.rgb(40, 40, 40));
        autoLamp.setStroke(Color.rgb(80, 80, 80));
        autoLamp.setStrokeWidth(1);
        autoLamp.setArcWidth(3);
        autoLamp.setArcHeight(3);
        autoLamp.setTranslateX(xOffset);
        autoLamp.setTranslateY(autoY);
        getGameScene().addUINode(autoLamp);

        autoLampText = new Text(I18n.tr("hud.auto"));
        autoLampText.setFont(Font.font("Monospace", FontWeight.BOLD, 8));
        autoLampText.setFill(Color.rgb(60, 60, 60));
        autoLampText.setTranslateX(xOffset + 13);
        autoLampText.setTranslateY(autoY + 8);
        getGameScene().addUINode(autoLampText);

        int moneyY = autoY + 28;
        Text moneyLabel = new Text();
        moneyLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
        moneyLabel.setFill(Color.GOLD);
        moneyLabel.setTranslateX(xOffset);
        moneyLabel.setTranslateY(moneyY);
        moneyLabel.textProperty().bind(getip(GameVars.MONEY).asString("$%d"));
        DropShadow moneyShadow = new DropShadow(2, Color.BLACK);
        moneyLabel.setEffect(moneyShadow);
        getGameScene().addUINode(moneyLabel);

        int accuracyY = moneyY + 16;
        accuracyHudText = new Text(I18n.tr("hud.accuracyEmpty"));
        accuracyHudText.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
        accuracyHudText.setFill(Color.rgb(120, 220, 255));
        accuracyHudText.setTranslateX(xOffset);
        accuracyHudText.setTranslateY(accuracyY);
        accuracyHudText.setEffect(new DropShadow(2, Color.BLACK));
        getGameScene().addUINode(accuracyHudText);
        updateAccuracyHud();

        int legendY = accuracyY + 18;
        Text coinCaption = new Text(I18n.tr("hud.coins"));
        coinCaption.setFont(Font.font("Monospace", FontWeight.BOLD, 8));
        coinCaption.setFill(Color.rgb(160, 160, 170));
        coinCaption.setTranslateX(xOffset);
        coinCaption.setTranslateY(legendY);
        getGameScene().addUINode(coinCaption);
        legendY += 16;
        Color[] legendColors = {
            Color.rgb(240, 240, 240),
            Color.rgb(100, 220, 100),
            Color.rgb(100, 150, 255),
            Color.rgb(200, 100, 255)
        };
        int[] legendValues = {10, 50, 100, 200};
        for (int i = 0; i < 4; i++) {
            int col = i % 2;
            int row = i / 2;
            double cx = xOffset + 4 + col * 30;
            double cy = legendY + row * 16;
            Circle dot = new Circle(3.4);
            dot.setFill(legendColors[i]);
            dot.setCenterX(cx);
            dot.setCenterY(cy);
            getGameScene().addUINode(dot);

            Text valueText = new Text("$" + legendValues[i]);
            valueText.setFont(Font.font("Monospace", FontWeight.BOLD, 8));
            valueText.setFill(Color.GOLD);
            valueText.setTranslateX(cx + 6);
            valueText.setTranslateY(cy + 3);
            valueText.setEffect(new DropShadow(1, Color.BLACK));
            getGameScene().addUINode(valueText);
        }

        getip(GameVars.ACTIVE_BULLETS).addListener((obs, o, n) -> updateBars());
        getip(GameVars.AMMO_CAP).addListener((obs, o, n) -> updateBars());
        getip(GameVars.WEAPON_GRADE).addListener((obs, o, n) -> updateBars());
        getip(GameVars.LIVES).addListener((obs, o, n) -> updateBars());
        getip(GameVars.SHOTS_FIRED).addListener((obs, o, n) -> updateAccuracyHud());
        getip(GameVars.HITS_LANDED).addListener((obs, o, n) -> updateAccuracyHud());
        getbp(GameVars.AUTOFIRE).addListener((obs, o, n) -> updateAutoLamp());
        updateBars();
        updateAutoLamp();
        updateAccuracyHud();

        int playX = railWidth;
        int playW = getAppWidth() - 2 * railWidth;

        Rectangle hudStrip = new Rectangle(playW, 40);
        hudStrip.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(6, 12, 22, 0.82)),
                new Stop(1, Color.rgb(6, 12, 22, 0.35))));
        hudStrip.setTranslateX(playX);
        hudStrip.setTranslateY(4);
        getGameScene().addUINode(hudStrip);

        Rectangle hudEdge = new Rectangle(playW, 1);
        hudEdge.setFill(Color.rgb(60, 210, 255, 0.45));
        hudEdge.setTranslateX(playX);
        hudEdge.setTranslateY(43);
        getGameScene().addUINode(hudEdge);

        Text scoreCaption = new Text(I18n.tr("hud.score"));
        scoreCaption.setFont(Font.font("Monospace", FontWeight.BOLD, 9));
        scoreCaption.setFill(Color.rgb(90, 210, 240));
        scoreCaption.setTranslateX(playX + 12);
        scoreCaption.setTranslateY(16);
        getGameScene().addUINode(scoreCaption);

        Text scoreLabel = new Text();
        scoreLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 20));
        scoreLabel.setFill(Color.WHITE);
        scoreLabel.setTranslateX(playX + 12);
        scoreLabel.setTranslateY(36);
        scoreLabel.textProperty().bind(getip(GameVars.SCORE).asString("%06d"));
        DropShadow scoreGlow = new DropShadow(12, Color.CYAN);
        scoreGlow.setSpread(0.15);
        scoreLabel.setEffect(scoreGlow);
        getGameScene().addUINode(scoreLabel);

        int comboPanelW = 92;
        int comboX = playX + (playW - comboPanelW) / 2;

        comboPlate = new Rectangle(comboPanelW, 30);
        comboPlate.setArcWidth(6);
        comboPlate.setArcHeight(6);
        comboPlate.setFill(Color.rgb(10, 18, 32, 0.9));
        comboPlate.setStroke(Color.rgb(255, 160, 40, 0.45));
        comboPlate.setStrokeWidth(1.2);
        comboPlate.setTranslateX(comboX);
        comboPlate.setTranslateY(8);
        getGameScene().addUINode(comboPlate);

        Text comboCaption = new Text(I18n.tr("hud.combo"));
        comboCaption.setFont(Font.font("Monospace", FontWeight.BOLD, 8));
        comboCaption.setFill(Color.rgb(255, 170, 60));
        comboCaption.setTranslateX(comboX + 8);
        comboCaption.setTranslateY(19);
        getGameScene().addUINode(comboCaption);

        Text comboLabel = new Text();
        comboLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        comboLabel.setFill(Color.rgb(255, 200, 90));
        comboLabel.setTranslateX(comboX + 52);
        comboLabel.setTranslateY(31);
        comboLabel.textProperty().bind(getip(GameVars.SQUAD_COMBOS).asString("x%d"));
        DropShadow comboGlow = new DropShadow(10, Color.ORANGE);
        comboGlow.setSpread(0.2);
        comboLabel.setEffect(comboGlow);
        getGameScene().addUINode(comboLabel);

        int wavePanelW = 86;
        int waveX = playX + playW - wavePanelW - 8;

        Rectangle wavePlate = new Rectangle(wavePanelW, 30);
        wavePlate.setArcWidth(6);
        wavePlate.setArcHeight(6);
        wavePlate.setFill(Color.rgb(10, 18, 32, 0.9));
        wavePlate.setStroke(Color.rgb(255, 210, 70, 0.7));
        wavePlate.setStrokeWidth(1.2);
        wavePlate.setTranslateX(waveX);
        wavePlate.setTranslateY(8);
        getGameScene().addUINode(wavePlate);

        Text waveCaption = new Text(I18n.tr("hud.wave"));
        waveCaption.setFont(Font.font("Monospace", FontWeight.BOLD, 8));
        waveCaption.setFill(Color.rgb(255, 210, 70));
        waveCaption.setTranslateX(waveX + 8);
        waveCaption.setTranslateY(19);
        getGameScene().addUINode(waveCaption);

        Text levelLabel = new Text();
        levelLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        levelLabel.setFill(Color.rgb(255, 230, 120));
        levelLabel.setTranslateX(waveX + 44);
        levelLabel.setTranslateY(31);
        levelLabel.textProperty().bind(getip(GameVars.LEVEL).asString("%02d"));
        DropShadow waveGlow = new DropShadow(10, Color.GOLD);
        waveGlow.setSpread(0.2);
        levelLabel.setEffect(waveGlow);
        getGameScene().addUINode(levelLabel);

        Text livesLabel = new Text();
        livesLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        livesLabel.setFill(Color.LIMEGREEN);
        livesLabel.setTranslateX(xOffset);
        livesLabel.setTranslateY(getAppHeight() - 20);
        livesLabel.textProperty().bind(getip(GameVars.LIVES).asString("\u2665 %d"));

        DropShadow livesShadow = new DropShadow(3, Color.BLACK);
        livesLabel.setEffect(livesShadow);
        getGameScene().addUINode(livesLabel);
    }

    private Rectangle createBarBackground(int x, int y, int width, int height) {
        Rectangle bg = new Rectangle(width, height);
        bg.setFill(Color.rgb(20, 25, 35));
        bg.setStroke(Color.rgb(80, 90, 110));
        bg.setStrokeWidth(1);
        bg.setTranslateX(x);
        bg.setTranslateY(y);
        bg.setArcWidth(4);
        bg.setArcHeight(4);
        return bg;
    }

    private Rectangle createStatusBar(int x, int y, int width, int height, Color color) {
        LinearGradient gradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, color.brighter()),
                new Stop(0.5, color),
                new Stop(1, color.darker()));

        Rectangle bar = new Rectangle(width, height);
        bar.setFill(gradient);
        bar.setTranslateX(x);
        bar.setTranslateY(y);
        bar.setArcWidth(3);
        bar.setArcHeight(3);

        Glow glow = new Glow(0.3);
        bar.setEffect(glow);

        return bar;
    }

    private Group createAnimatedLetterSlot(char letter, int index, double x, double y) {
        Rectangle frame = new Rectangle(24, 24);
        frame.setFill(Color.rgb(25, 30, 40));
        frame.setStroke(Color.rgb(60, 70, 90));
        frame.setStrokeWidth(1);
        frame.setArcWidth(4);
        frame.setArcHeight(4);
        frame.setTranslateX(-12);
        frame.setTranslateY(-18);

        Text letterText = new Text(String.valueOf(letter));
        letterText.setFont(Font.font("Monospace", FontWeight.BOLD, 18));
        letterText.setFill(Color.rgb(45, 50, 60));
        letterText.setTranslateX(-6);
        letterText.setTranslateY(0);
        extraLetterTexts[index] = letterText;

        Group flipWrapper = new Group(letterText);
        extraFlipWrappers[index] = flipWrapper;

        Group letterGroup = new Group(frame, flipWrapper);
        letterGroup.setTranslateX(x);
        letterGroup.setTranslateY(y);

        startIdleFlipAnimation(index);

        return letterGroup;
    }

    private void startIdleFlipAnimation(int index) {
        Group flipWrapper = extraFlipWrappers[index];
        if (flipWrapper == null) return;

        double cycleDuration = 2.5 + index * 0.3;

        Timeline flipTimeline = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(flipWrapper.scaleXProperty(), 1.0)),
            new KeyFrame(Duration.seconds(cycleDuration * 0.25),
                new KeyValue(flipWrapper.scaleXProperty(), 0.18)),
            new KeyFrame(Duration.seconds(cycleDuration * 0.5),
                new KeyValue(flipWrapper.scaleXProperty(), -1.0)),
            new KeyFrame(Duration.seconds(cycleDuration * 0.75),
                new KeyValue(flipWrapper.scaleXProperty(), -0.18)),
            new KeyFrame(Duration.seconds(cycleDuration),
                new KeyValue(flipWrapper.scaleXProperty(), 1.0))
        );
        flipTimeline.setCycleCount(Animation.INDEFINITE);
        flipTimeline.setDelay(Duration.millis(index * 400));
        flipTimeline.play();
        extraLetterAnimations.add(flipTimeline);
    }

    private void updateExtraLetter(int idx, boolean lit, Color litColor) {
        Text letterText = extraLetterTexts[idx];
        Group letterGroup = extraLetterGroups[idx];

        if (lit) {
            letterText.setFill(litColor);

            Rectangle frame = (Rectangle) letterGroup.getChildren().get(0);
            frame.setStroke(litColor.darker());

        } else {
            letterText.setFill(Color.rgb(45, 50, 60));

            Rectangle frame = (Rectangle) letterGroup.getChildren().get(0);
            frame.setStroke(Color.rgb(60, 70, 90));
        }
    }

    private void updateBars() {
        int railWidth = GameVars.RAIL_WIDTH;
        int barWidth = railWidth - 28;

        int available = geti(GameVars.AMMO_CAP) - geti(GameVars.ACTIVE_BULLETS);
        int cap = geti(GameVars.AMMO_CAP);
        double ammoRatio = cap > 0 ? (double) available / cap : 0;
        ammoBar.setWidth(Math.max(1, barWidth * ammoRatio));

        int weapon = geti(GameVars.WEAPON_GRADE);
        double weaponRatio = (double) weapon / GameVars.MAX_WEAPON_GRADE;
        weaponBar.setWidth(Math.max(1, barWidth * weaponRatio));

        int lives = geti(GameVars.LIVES);
        double livesRatio = Math.min(1.0, (double) lives / 5);
        livesBar.setWidth(Math.max(1, barWidth * livesRatio));
        updateExtraTimeBar();
    }

    private void updateAccuracyHud() {
        if (accuracyHudText == null) {
            return;
        }
        int shots = Math.max(0, geti(GameVars.SHOTS_FIRED));
        int hits = Math.max(0, geti(GameVars.HITS_LANDED));
        if (shots <= 0) {
            accuracyHudText.setText(I18n.tr("hud.accuracyEmpty"));
            return;
        }
        int pct = (int) Math.round(100.0 * Math.min(1.0, (double) hits / (double) shots));
        accuracyHudText.setText(I18n.tr("hud.accuracy", pct));
    }

    private void refillWaveTimer() {
        extraTime = extraTimeMax;
        extraTimeWarned = false;
        hurryUfoDelay = 0;
        pendingHurryUfo = false;
        updateExtraTimeBar();
    }

    private void changeLevelTime(int delta) {
        if (delta > 0) {
            extraTimeMax += delta;
            extraTime += delta;
            if (extraTime > 5) {
                extraTimeWarned = false;
                hurryUfoDelay = 0;
                pendingHurryUfo = false;
            }
        } else if (delta < 0) {
            extraTimeMax = Math.max(GameVars.LEVEL_TIME_MIN, extraTimeMax + delta);
            extraTime = Math.max(0, extraTime + delta);
            if (!extraTimeWarned && extraTime <= 5) {
                triggerHurryUpWarning();
            }
            if (extraTime <= 0) {
                if (!extraTimeWarned) {
                    triggerHurryUpWarning();
                } else {
                    hurryUfoDelay = Math.max(hurryUfoDelay, GameVars.HURRY_UFO_DELAY);
                }
            }
        }
        updateExtraTimeBar();
    }

    private void updateExtraTimeBar() {
        if (extraTimeMaxText != null) {
            extraTimeMaxText.setText(String.valueOf(extraTimeMax));
        }
        if (extraTimeBar == null) {
            return;
        }
        boolean show = !isBossWave();
        extraTimeBar.setVisible(show);
        if (timeBarBg != null) {
            timeBarBg.setVisible(show);
        }
        if (extraTimeMaxText != null) {
            extraTimeMaxText.setVisible(show);
        }
        if (timeLabelNode != null) {
            timeLabelNode.setVisible(show);
        }
        double ratio = extraTimeMax > 0 ? extraTime / extraTimeMax : 0;
        extraTimeBar.setWidth(Math.max(1, extraTimeBarMaxWidth * ratio));
        if (extraTime > 0 && extraTime <= 5) {
            extraTimeBar.setOpacity(0.35 + 0.65 * (0.5 + 0.5 * Math.sin(extraTimePulse * 14)));
        } else {
            extraTimeBar.setOpacity(1);
        }
    }

    private void updateRankHud() {
        if (rankDots == null) {
            return;
        }
        int mask = 0;
        int rank = 0;
        try {
            mask = geti(GameVars.RANK_MASK);
            rank = geti(GameVars.RANK);
        } catch (Exception ignored) {
            return;
        }
        if (rankTitleText != null) {
            rankTitleText.setText(RankNames.shortTitle(rank));
        }
        for (int i = 0; i < rankDots.length; i++) {
            if (rankDots[i] == null) {
                continue;
            }
            styleRankDot(rankDots[i], i, (mask & (1 << i)) != 0);
        }
    }

    private void styleRankDot(Circle dot, int colorIndex, boolean collected) {
        Color color = RankMarkerComponent.COLORS[colorIndex];
        if (collected) {
            dot.setFill(color);
            dot.setStroke(Color.WHITE);
            DropShadow glow = new DropShadow(6, color);
            glow.setSpread(0.22);
            dot.setEffect(glow);
        } else {
            dot.setFill(Color.rgb(10, 10, 14, 0.85));
            dot.setStroke(color);
            dot.setEffect(null);
        }
        dot.setOpacity(1);
    }

    private void updateShieldHud() {
        if (shieldHudText == null) {
            return;
        }
        double remaining = 0;
        boolean missileOn = false;
        if (player != null && player.isActive() && player.hasComponent(PlayerComponent.class)) {
            PlayerComponent pc = player.getComponent(PlayerComponent.class);
            remaining = pc.getShieldRemaining();
            missileOn = pc.isMissileShielded();
        }
        if (missileOn && remaining <= 0) {
            shieldHudText.setText("∞");
            shieldHudText.setFill(Color.AQUA);
            shieldHudText.setOpacity(1);
            return;
        }
        int shown = (int) Math.ceil(remaining);
        shieldHudText.setText(String.valueOf(shown));
        if (shown <= 0) {
            shieldHudText.setFill(Color.rgb(60, 80, 90));
            shieldHudText.setOpacity(0.55);
        } else if (remaining <= 3) {
            shieldHudText.setFill(Color.ORANGERED);
            shieldHudText.setOpacity(0.4 + 0.6 * (0.5 + 0.5 * Math.sin(remaining * 14)));
        } else {
            shieldHudText.setFill(Color.AQUA);
            shieldHudText.setOpacity(1);
        }
    }

    private void updateAutoLamp() {
        boolean autoOn = getb(GameVars.AUTOFIRE);
        if (autoLamp != null) {
            if (autoOn) {
                autoLamp.setFill(Color.CYAN);
                autoLamp.setStroke(Color.WHITE);
                DropShadow glow = new DropShadow(8, Color.CYAN);
                autoLamp.setEffect(glow);
            } else {
                autoLamp.setFill(Color.rgb(40, 40, 40));
                autoLamp.setStroke(Color.rgb(80, 80, 80));
                autoLamp.setEffect(null);
            }
        }
        if (autoLampText != null) {
            autoLampText.setFill(autoOn ? Color.CYAN : Color.rgb(60, 60, 60));
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
