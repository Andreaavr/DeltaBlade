package deltablade.minigames;

import deltablade.I18n;
import com.almasb.fxgl.entity.Entity;
import deltablade.EmbeddedTextures;
import deltablade.GameVars;
import deltablade.SoundHelper;
import deltablade.SpriteSheets;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static com.almasb.fxgl.dsl.FXGL.geti;
import static com.almasb.fxgl.dsl.FXGL.inc;

/**
 * Minijuego de esquivar a pantalla completa: recorre un laberinto con solución desde
 * el centro inferior hasta el centro superior mientras las torretas del borde disparan
 * balas lentas hacia dentro. El disparo único silencia una nave del borde; el escudo
 * da 10 s de invulnerabilidad (acumulables).
 */
public class MazeEscapeGame implements Minigame {

    private static final int COLS = 15;
    private static final int ROWS = 11;
    private static final double CELL = 36;
    private static final double WALL = 4;
    private static final double RIM = 32;
    private static final int RIM_SHIP = 28;
    private static final int BONUS_ICON = 22;
    /** Radio para chocar con las paredes: más pequeño que el sprite para que los pasillos parezcan amplios. */
    private static final double PLAYER_WALL_R = 9.0;
    /** Radio para las balas de las torretas: una zona de impacto justa. */
    private static final double PLAYER_HURT_R = 14.0;
    /** Distancia al centro (px) desde la que todavía se atrae al jugador hacia la entrada de un pasillo. */
    private static final double GAP_ASSIST = 14.0;
    /** Velocidad de atracción hacia el centro del pasillo al entrar en un hueco. */
    private static final double GAP_ASSIST_SPEED = 280;
    private static final double PLAYER_SPEED = 155;
    private static final double BULLET_SPEED = 78;
    private static final double BULLET_R = 4.5;
    /** Rango de espera entre disparos del borde (segundos), sorteado en cada disparo. */
    private static final double FIRE_GAP_MIN = 2.8;
    private static final double FIRE_GAP_MAX = 5.8;
    private static final double SHOT_BOLT_SPEED = 420;
    private static final double SHOT_BOLT_R = 5;
    private static final double TIME_LIMIT = 48;
    private static final double FIRE_GRACE = 2.0;
    /** Mostrar HURRY UP una vez cuando el tiempo baja de este valor (segundos). */
    private static final double HURRY_THRESHOLD = 9.0;
    /** Multiplicador de recarga del borde con prisa (< 1 = más rápido). */
    private static final double HURRY_FIRE_SCALE = 0.78;
    private static final int PLAYER_ICON = 34;
    private static final int EXTRA_LINKS = 10;
    private static final int PICKUP_MONEY = 25;
    private static final double PICKUP_TIME = 10;
    private static final int PICKUP_WEAPON_SCORE = 100;
    private static final int PICKUP_AMMO_SCORE = 60;
    private static final double SETTLEMENT_SECS = 4.6;
    private static final double SETTLEMENT_SKIP_AFTER = 1.2;
    private static final int SETTLE_ICON = 20;
    private static final Random RANDOM = new Random();

    private enum BonusKind { MONEY, TIME, SHIELD, SHOT, WEAPON, AMMO }

    private MinigameHost host;
    private boolean active;
    private boolean finished;

    private double originX;
    private double originY;
    private double mazeW;
    private double mazeH;

    /** True = hay una pared que bloquea el paso entre casillas o en el borde. */
    private boolean[][] vWall; // [COLS+1][ROWS]
    private boolean[][] hWall; // [COLS][ROWS+1]

    private int startCol;
    private int startRow;
    private int exitCol;
    private int exitRow;

    private double timeLeft;
    private int moneyCollected;
    private int shotsLoaded;
    private double shieldTime;
    // Bonus secreto: se han recogido todos los extras del laberinto.
    private boolean perfectRun;
    private int perfectScore;
    private static final int SCORE_MULT = 10;
    private int perfectMoney;
    private Text perfectText;
    private static final double SHIELD_SECONDS = GameVars.SHIELD_DURATION;

    private boolean shieldUp() {
        return shieldTime > 0;
    }
    private Circle shieldBubble;
    private double extraPulse;
    private double shieldFlash;
    private double fireGraceLeft;
    private boolean hurryWarned;
    private double hudPulse;
    private boolean fireWasHeld;
    private double noShotBannerCd;
    private ShotBolt activeBolt;
    private final List<BonusKind> collectedKinds = new ArrayList<>();
    private boolean awaitingSettlement;
    private double settlementTimer;
    private double settlementShownFor;
    private int pendingScore;
    private int pendingMoney;
    private boolean pendingWon;
    private String pendingBanner;
    private Color pendingColor;
    private Group settlementRoot;

    private double savedPlayerX;
    private double savedPlayerY;
    private double savedPlayerRotation;
    private Point2D savedRotationOrigin;

    private final List<RimShip> rimShips = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Bonus> bonuses = new ArrayList<>();
    private final List<javafx.scene.Node> ui = new ArrayList<>();

    private Group mazeLayer;
    private Group bulletLayer;
    private Group bonusLayer;
    private Group rimLayer;
    private Group playerLayer;
    private ImageView playerShipView;
    private Text readyText;
    private Text hudTime;
    private Text hudMoney;
    private Text hudShot;
    private Text hudShield;
    private Rectangle exitMarker;

    private static final class RimShip {
        final double x;
        final double y;
        final double nx;
        final double ny;
        double cooldown;
        boolean silenced;
        Group view;

        RimShip(double x, double y, double nx, double ny, double initialCd) {
            this.x = x;
            this.y = y;
            this.nx = nx;
            this.ny = ny;
            this.cooldown = initialCd;
        }
    }

    private static final class Bullet {
        double x;
        double y;
        final double vx;
        final double vy;
        Circle view;

        Bullet(double x, double y, double vx, double vy) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
        }
    }

    /** Disparo único del jugador hacia una torreta del borde (capa de interfaz, como las balas enemigas). */
    private static final class ShotBolt {
        double x;
        double y;
        double vx;
        double vy;
        RimShip target;
        Circle view;
    }

    private static final class Bonus {
        final int col;
        final int row;
        final BonusKind kind;
        boolean taken;
        Group view;

        Bonus(int col, int row, BonusKind kind) {
            this.col = col;
            this.row = row;
            this.kind = kind;
        }
    }

    private record Cell(int c, int r) {}

    @Override
    public void start(MinigameHost host) {
        this.host = host;
        this.active = true;
        this.finished = false;
        this.timeLeft = TIME_LIMIT;
        this.moneyCollected = 0;
        this.shotsLoaded = 0;
        this.shieldTime = 0;
        this.fireGraceLeft = FIRE_GRACE;
        this.hurryWarned = false;
        this.hudPulse = 0;
        this.fireWasHeld = false;
        this.noShotBannerCd = 0;
        this.activeBolt = null;
        this.collectedKinds.clear();
        this.perfectRun = false;
        this.perfectText = null;
        this.awaitingSettlement = false;
        this.settlementRoot = null;

        // El jugador del mundo queda debajo de los nodos de interfaz de FXGL; el laberinto es interfaz opaca,
        // así que se oculta el sprite del mundo y se dibuja una copia en la interfaz, encima del laberinto.
        host.hidePlayer(true);
        generateMaze();
        layoutPlayfield();
        buildViews();
        placeBonuses();
        placeRimShips();
        placePlayerAtStart();
        syncPlayerShipView();

        host.displayBanner(I18n.tr("maze.title"), Color.MEDIUMPURPLE, 1.4);
    }

    @Override
    public boolean usesPlayerShip() {
        // Aquí se gestiona el movimiento en 4 direcciones (en los otros juegos el anfitrión solo controla izquierda/derecha).
        return false;
    }

    @Override
    public void update(double tpf) {
        if (!active) {
            return;
        }
        if (awaitingSettlement) {
            settlementTimer -= tpf;
            settlementShownFor += tpf;
            animatePerfectLine();
            if (settlementTimer <= 0) {
                completeSettlement();
            }
            return;
        }
        if (finished) {
            return;
        }

        movePlayer(tpf);
        syncPlayerShipView();
        animateExtras(tpf);
        pollFireKey();
        if (noShotBannerCd > 0) {
            noShotBannerCd = Math.max(0, noShotBannerCd - tpf);
        }
        updateShotBolt(tpf);

        if (fireGraceLeft > 0) {
            fireGraceLeft = Math.max(0, fireGraceLeft - tpf);
            refreshReadyBanner();
            if (fireGraceLeft <= 0) {
                armRimTurrets();
                if (readyText != null) {
                    readyText.setVisible(false);
                }
                host.displayBanner(I18n.tr("maze.go"), Color.LIMEGREEN, 0.7);
            }
            collectBonuses();
            checkExit();
            refreshHud();
            return;
        }

        updateRimFire(tpf);
        updateBullets(tpf);
        collectBonuses();
        checkBulletHits();
        checkExit();

        if (shieldTime > 0) {
            shieldTime = Math.max(0, shieldTime - tpf);
        }
        timeLeft -= tpf;
        hudPulse += tpf;
        maybeHurryUp();
        refreshHud();
        if (timeLeft <= 0) {
            fail(I18n.tr("maze.timeOut"));
        }
    }

    private void maybeHurryUp() {
        if (hurryWarned || fireGraceLeft > 0 || timeLeft > HURRY_THRESHOLD) {
            return;
        }
        hurryWarned = true;
        SoundHelper.play("hurry_warning.wav");
        host.displayBanner(I18n.tr("banner.hurryUp"), Color.ORANGERED, 1.2);
    }

    @Override
    public void onFirePress() {
        if (awaitingSettlement) {
            if (settlementShownFor >= SETTLEMENT_SKIP_AFTER) {
                completeSettlement();
            }
            return;
        }
        tryFireShot();
    }

    @Override
    public void onBonusKey() {
        if (awaitingSettlement && settlementShownFor >= SETTLEMENT_SKIP_AFTER) {
            completeSettlement();
        }
    }

    /** Espacio/X: una vez después de recoger el misil; si no, un breve NO SHOT. */
    private void tryFireShot() {
        if (!active || finished) {
            return;
        }
        if (activeBolt != null) {
            return;
        }
        if (shotsLoaded <= 0) {
            if (noShotBannerCd <= 0) {
                noShotBannerCd = 0.85;
                host.displayBanner(I18n.tr("maze.noShot"), Color.LIGHTGRAY, 0.7);
            }
            return;
        }
        fireSingleShot();
    }

    @Override
    public void cleanup() {
        active = false;
        restorePlayer();
        for (javafx.scene.Node node : ui) {
            host.removeUi(node);
        }
        ui.clear();
        rimShips.clear();
        bullets.clear();
        bonuses.clear();
        activeBolt = null;
        collectedKinds.clear();
        awaitingSettlement = false;
        if (settlementRoot != null) {
            host.removeUi(settlementRoot);
            settlementRoot = null;
        }
        mazeLayer = null;
        bulletLayer = null;
        bonusLayer = null;
        rimLayer = null;
        playerLayer = null;
        playerShipView = null;
        shieldBubble = null;
        readyText = null;
    }

    // --- generación del laberinto ------------------------------------------

    private void generateMaze() {
        vWall = new boolean[COLS + 1][ROWS];
        hWall = new boolean[COLS][ROWS + 1];
        for (int c = 0; c <= COLS; c++) {
            for (int r = 0; r < ROWS; r++) {
                vWall[c][r] = true;
            }
        }
        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r <= ROWS; r++) {
                hWall[c][r] = true;
            }
        }

        boolean[][] visited = new boolean[COLS][ROWS];
        ArrayDeque<Cell> stack = new ArrayDeque<>();
        Cell start = new Cell(COLS / 2, ROWS - 1);
        stack.push(start);
        visited[start.c][start.r] = true;

        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!stack.isEmpty()) {
            Cell cur = stack.peek();
            List<int[]> opts = new ArrayList<>();
            for (int[] d : dirs) {
                int nc = cur.c + d[0];
                int nr = cur.r + d[1];
                if (nc >= 0 && nc < COLS && nr >= 0 && nr < ROWS && !visited[nc][nr]) {
                    opts.add(d);
                }
            }
            if (opts.isEmpty()) {
                stack.pop();
                continue;
            }
            int[] d = opts.get(RANDOM.nextInt(opts.size()));
            int nc = cur.c + d[0];
            int nr = cur.r + d[1];
            knockWall(cur.c, cur.r, nc, nr);
            visited[nc][nr] = true;
            stack.push(new Cell(nc, nr));
        }

        // Conexiones extra para que el laberinto no sea solo un árbol de callejones sin salida.
        int links = 0;
        int guard = 0;
        while (links < EXTRA_LINKS && guard++ < 400) {
            int c = RANDOM.nextInt(COLS);
            int r = RANDOM.nextInt(ROWS);
            int[] d = dirs[RANDOM.nextInt(dirs.length)];
            int nc = c + d[0];
            int nr = r + d[1];
            if (nc < 0 || nc >= COLS || nr < 0 || nr >= ROWS) {
                continue;
            }
            if (wallBetween(c, r, nc, nr)) {
                knockWall(c, r, nc, nr);
                links++;
            }
        }

        startCol = COLS / 2;
        startRow = ROWS - 1;
        exitCol = COLS / 2;
        exitRow = 0;

        // Abrir un hueco en la entrada (abajo) y en la salida (arriba) del borde exterior del laberinto.
        hWall[startCol][ROWS] = false;
        hWall[exitCol][0] = false;

        if (!pathExists(startCol, startRow, exitCol, exitRow)) {
            // Muy poco probable; como alternativa, se fuerza un pasillo vertical.
            for (int r = 0; r < ROWS; r++) {
                if (r + 1 < ROWS) {
                    knockWall(startCol, r, startCol, r + 1);
                }
            }
            hWall[startCol][ROWS] = false;
            hWall[exitCol][0] = false;
        }
    }

    private void knockWall(int c, int r, int nc, int nr) {
        if (c == nc) {
            int row = Math.max(r, nr);
            hWall[c][row] = false;
        } else {
            int col = Math.max(c, nc);
            vWall[col][r] = false;
        }
    }

    private boolean wallBetween(int c, int r, int nc, int nr) {
        if (c == nc) {
            return hWall[c][Math.max(r, nr)];
        }
        return vWall[Math.max(c, nc)][r];
    }

    private boolean pathExists(int sc, int sr, int ec, int er) {
        boolean[][] seen = new boolean[COLS][ROWS];
        ArrayDeque<Cell> q = new ArrayDeque<>();
        q.add(new Cell(sc, sr));
        seen[sc][sr] = true;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!q.isEmpty()) {
            Cell cur = q.poll();
            if (cur.c == ec && cur.r == er) {
                return true;
            }
            for (int[] d : dirs) {
                int nc = cur.c + d[0];
                int nr = cur.r + d[1];
                if (nc < 0 || nc >= COLS || nr < 0 || nr >= ROWS || seen[nc][nr]) {
                    continue;
                }
                if (wallBetween(cur.c, cur.r, nc, nr)) {
                    continue;
                }
                seen[nc][nr] = true;
                q.add(new Cell(nc, nr));
            }
        }
        return false;
    }

    private Set<Long> shortestPathCells() {
        int[][] prevC = new int[COLS][ROWS];
        int[][] prevR = new int[COLS][ROWS];
        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r < ROWS; r++) {
                prevC[c][r] = -1;
                prevR[c][r] = -1;
            }
        }
        boolean[][] seen = new boolean[COLS][ROWS];
        ArrayDeque<Cell> q = new ArrayDeque<>();
        q.add(new Cell(startCol, startRow));
        seen[startCol][startRow] = true;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        boolean found = false;
        while (!q.isEmpty()) {
            Cell cur = q.poll();
            if (cur.c == exitCol && cur.r == exitRow) {
                found = true;
                break;
            }
            for (int[] d : dirs) {
                int nc = cur.c + d[0];
                int nr = cur.r + d[1];
                if (nc < 0 || nc >= COLS || nr < 0 || nr >= ROWS || seen[nc][nr]) {
                    continue;
                }
                if (wallBetween(cur.c, cur.r, nc, nr)) {
                    continue;
                }
                seen[nc][nr] = true;
                prevC[nc][nr] = cur.c;
                prevR[nc][nr] = cur.r;
                q.add(new Cell(nc, nr));
            }
        }
        Set<Long> path = new HashSet<>();
        if (!found) {
            return path;
        }
        int c = exitCol;
        int r = exitRow;
        while (c >= 0 && r >= 0) {
            path.add(pack(c, r));
            int pc = prevC[c][r];
            int pr = prevR[c][r];
            if (pc < 0) {
                break;
            }
            c = pc;
            r = pr;
        }
        return path;
    }

    private static long pack(int c, int r) {
        return (((long) c) << 32) | (r & 0xffffffffL);
    }

    // --- disposición / vistas ----------------------------------------------

    private void layoutPlayfield() {
        mazeW = COLS * CELL + (COLS + 1) * WALL;
        mazeH = ROWS * CELL + (ROWS + 1) * WALL;
        double playW = host.playRight() - host.playLeft();
        double playH = host.playBottom() - host.playTop();
        originX = host.playLeft() + (playW - mazeW) / 2.0;
        originY = host.playTop() + (playH - mazeH) / 2.0;
        // Dejar una franja alrededor del laberinto para las torretas.
        originX = Math.max(host.playLeft() + RIM, Math.min(originX, host.playRight() - RIM - mazeW));
        originY = Math.max(host.playTop() + RIM, Math.min(originY, host.playBottom() - RIM - mazeH));
    }

    private void buildViews() {
        // Marco atenuado del área de juego cerrada.
        Rectangle seal = new Rectangle(
                host.playRight() - host.playLeft(),
                host.playBottom() - host.playTop());
        seal.setTranslateX(host.playLeft());
        seal.setTranslateY(host.playTop());
        seal.setFill(Color.rgb(4, 8, 18, 0.55));
        seal.setStroke(Color.rgb(120, 80, 200, 0.45));
        seal.setStrokeWidth(2);
        seal.setMouseTransparent(true);
        host.addUi(seal);
        ui.add(seal);

        mazeLayer = new Group();
        Rectangle floor = new Rectangle(mazeW, mazeH);
        floor.setFill(Color.rgb(10, 18, 32, 0.88));
        floor.setStroke(Color.rgb(90, 60, 160, 0.7));
        floor.setStrokeWidth(1.5);
        mazeLayer.getChildren().add(floor);

        // Paredes verticales.
        for (int c = 0; c <= COLS; c++) {
            for (int r = 0; r < ROWS; r++) {
                if (!vWall[c][r]) {
                    continue;
                }
                Rectangle w = new Rectangle(WALL, CELL + WALL);
                w.setFill(Color.rgb(70, 120, 180, 0.85));
                w.setTranslateX(c * (CELL + WALL));
                w.setTranslateY(r * (CELL + WALL) + WALL);
                mazeLayer.getChildren().add(w);
            }
        }
        // Paredes horizontales.
        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r <= ROWS; r++) {
                if (!hWall[c][r]) {
                    continue;
                }
                Rectangle w = new Rectangle(CELL + WALL, WALL);
                w.setFill(Color.rgb(70, 120, 180, 0.85));
                w.setTranslateX(c * (CELL + WALL) + WALL);
                w.setTranslateY(r * (CELL + WALL));
                mazeLayer.getChildren().add(w);
            }
        }
        // Borde exterior cerrado (salvo los huecos de entrada y salida ya abiertos en las paredes).
        Rectangle outer = new Rectangle(mazeW, mazeH);
        outer.setFill(Color.TRANSPARENT);
        outer.setStroke(Color.rgb(140, 100, 220, 0.9));
        outer.setStrokeWidth(2);
        mazeLayer.getChildren().add(outer);

        exitMarker = new Rectangle(CELL - 4, WALL + 6);
        exitMarker.setFill(Color.rgb(80, 255, 140, 0.85));
        exitMarker.setTranslateX(WALL + exitCol * (CELL + WALL) + 2);
        exitMarker.setTranslateY(-2);
        mazeLayer.getChildren().add(exitMarker);
        Text exitLabel = new Text(I18n.tr("maze.exit"));
        exitLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 9));
        exitLabel.setFill(Color.rgb(180, 255, 200));
        exitLabel.setTranslateX(WALL + exitCol * (CELL + WALL) + 4);
        exitLabel.setTranslateY(-6);
        mazeLayer.getChildren().add(exitLabel);

        mazeLayer.setTranslateX(originX);
        mazeLayer.setTranslateY(originY);
        mazeLayer.setMouseTransparent(true);
        host.addUi(mazeLayer);
        ui.add(mazeLayer);

        bonusLayer = new Group();
        bonusLayer.setMouseTransparent(true);
        host.addUi(bonusLayer);
        ui.add(bonusLayer);

        rimLayer = new Group();
        rimLayer.setMouseTransparent(true);
        host.addUi(rimLayer);
        ui.add(rimLayer);

        bulletLayer = new Group();
        bulletLayer.setMouseTransparent(true);
        host.addUi(bulletLayer);
        ui.add(bulletLayer);

        // La copia del jugador va encima del laberinto, los extras y las balas para que nunca quede tapada.
        playerLayer = new Group();
        playerLayer.setMouseTransparent(true);
        playerShipView = createPlayerShipView();
        shieldBubble = new Circle(PLAYER_ICON * 0.75);
        shieldBubble.setFill(Color.rgb(60, 200, 255, 0.12));
        shieldBubble.setStroke(Color.rgb(170, 245, 255, 0.85));
        shieldBubble.setStrokeWidth(2.5);
        shieldBubble.setEffect(new DropShadow(10, Color.AQUA));
        shieldBubble.setVisible(false);
        shieldBubble.setMouseTransparent(true);
        playerLayer.getChildren().addAll(playerShipView, shieldBubble);
        host.addUi(playerLayer);
        ui.add(playerLayer);

        readyText = new Text(I18n.tr("maze.ready"));
        readyText.setFont(Font.font("Monospace", FontWeight.BOLD, 28));
        readyText.setFill(Color.GOLD);
        readyText.setEffect(new DropShadow(14, Color.BLACK));
        readyText.setTranslateX((host.playLeft() + host.playRight()) / 2.0 - 48);
        readyText.setTranslateY(host.playTop() + 56);
        host.addUi(readyText);
        ui.add(readyText);

        buildHud();
    }

    private ImageView createPlayerShipView() {
        Image img = EmbeddedTextures.getImage("player.png", PLAYER_ICON, PLAYER_ICON);
        ImageView view = new ImageView();
        if (img != null && !img.isError()) {
            view.setImage(img);
        }
        view.setFitWidth(PLAYER_ICON);
        view.setFitHeight(PLAYER_ICON);
        view.setPreserveRatio(true);
        view.setSmooth(false);
        view.setEffect(new DropShadow(12, Color.rgb(80, 200, 255, 0.85)));
        return view;
    }

    private void syncPlayerShipView() {
        if (playerShipView == null) {
            return;
        }
        Entity player = host.player();
        if (player == null || !player.isActive()) {
            playerShipView.setVisible(false);
            return;
        }
        playerShipView.setVisible(true);
        double cx = player.getX() + player.getWidth() / 2.0;
        double cy = player.getY() + player.getHeight() / 2.0;
        playerShipView.setTranslateX(cx - PLAYER_ICON / 2.0);
        playerShipView.setTranslateY(cy - PLAYER_ICON / 2.0);
        playerShipView.setRotate(player.getRotation());
        if (shieldBubble != null) {
            shieldBubble.setTranslateX(cx);
            shieldBubble.setTranslateY(cy);
        }
    }

    /** Pulso suave para los extras de misil y escudo, y para la burbuja del escudo. */
    private void animateExtras(double tpf) {
        extraPulse += tpf * 3.0;
        double wave = 0.5 + 0.5 * Math.sin(extraPulse);
        for (Bonus b : bonuses) {
            if (b.taken || b.view == null) {
                continue;
            }
            if (b.kind == BonusKind.SHIELD || b.kind == BonusKind.SHOT) {
                double sc = 1.0 + 0.10 * wave;
                b.view.setScaleX(sc);
                b.view.setScaleY(sc);
                b.view.setOpacity(0.78 + 0.22 * wave);
            }
        }
        if (shieldFlash > 0) {
            shieldFlash = Math.max(0, shieldFlash - tpf);
        }
        if (shieldBubble != null) {
            boolean show = (shieldUp() || shieldFlash > 0) && playerShipView != null && playerShipView.isVisible();
            shieldBubble.setVisible(show);
            if (show) {
                double f = shieldFlash / 0.35;
                if (shieldTime > 0 && shieldTime <= 3) {
                    // Últimos 3 segundos: parpadeo más rápido.
                    wave = 0.5 + 0.5 * Math.sin(shieldTime * 14);
                }
                double sc = 1.0 + 0.06 * wave + 0.25 * f;
                shieldBubble.setScaleX(sc);
                shieldBubble.setScaleY(sc);
                double shieldOpacity = shieldTime <= 3
                        ? Math.min(1.0, 0.25 + 0.75 * wave + f)
                        : Math.min(1.0, 0.6 + 0.3 * wave + f);
                shieldBubble.setOpacity(shieldUp() ? shieldOpacity : f);
            }
        }
    }

    private void refreshReadyBanner() {
        if (readyText == null) {
            return;
        }
        int secs = (int) Math.ceil(fireGraceLeft);
        readyText.setText(secs > 0 ? I18n.tr("maze.readyCount", secs) : I18n.tr("maze.go"));
        readyText.setVisible(true);
    }

    private void armRimTurrets() {
        // Primeros disparos irregulares: nada de ráfaga completa al empezar.
        for (int i = 0; i < rimShips.size(); i++) {
            RimShip ship = rimShips.get(i);
            if (!ship.silenced) {
                ship.cooldown = nextFireGap() * (0.45 + RANDOM.nextDouble() * 0.55);
            }
        }
    }

    private double nextFireGap() {
        double gap = FIRE_GAP_MIN + RANDOM.nextDouble() * (FIRE_GAP_MAX - FIRE_GAP_MIN);
        if (hurryWarned) {
            gap *= HURRY_FIRE_SCALE;
        }
        return gap;
    }

    private void buildHud() {
        double x = host.playLeft() + 10;
        double y = host.playTop() + 14;

        hudTime = hudText(x, y, Color.CYAN);
        hudMoney = hudText(x + 110, y, Color.GOLD);
        hudShot = hudText(x + 230, y, Color.ORANGE);
        hudShield = hudText(x + 360, y, Color.AQUA);
        refreshHud();
    }

    private Text hudText(double x, double y, Color color) {
        Text t = new Text();
        t.setFont(Font.font("Monospace", FontWeight.BOLD, 12));
        t.setFill(color);
        t.setEffect(new DropShadow(6, Color.BLACK));
        t.setTranslateX(x);
        t.setTranslateY(y);
        host.addUi(t);
        ui.add(t);
        return t;
    }

    private void refreshHud() {
        if (hudTime != null) {
            hudTime.setText(I18n.tr("minigame.time", String.format("%04.1f", Math.max(0, timeLeft))));
            if (timeLeft <= HURRY_THRESHOLD && fireGraceLeft <= 0) {
                double pulse = 0.55 + 0.45 * (0.5 + 0.5 * Math.sin(hudPulse * 10));
                hudTime.setFill(Color.ORANGERED);
                hudTime.setOpacity(pulse);
            } else {
                hudTime.setFill(Color.CYAN);
                hudTime.setOpacity(1.0);
            }
        }
        if (hudMoney != null) {
            hudMoney.setText("$" + moneyCollected);
        }
        if (hudShot != null) {
            hudShot.setText(shotsLoaded > 0 ? I18n.tr("maze.shot", shotsLoaded) : I18n.tr("maze.noShotHud"));
            hudShot.setFill(shotsLoaded > 0 ? Color.ORANGE : Color.rgb(120, 120, 140));
        }
        if (hudShield != null) {
            hudShield.setText(shieldUp() ? I18n.tr("maze.shield", (int) Math.ceil(shieldTime)) : I18n.tr("maze.noShield"));
            hudShield.setFill(shieldUp() ? Color.AQUA : Color.rgb(120, 120, 140));
        }
    }

    // --- extras / borde ---------------------------------------------------

    private void placeBonuses() {
        Set<Long> path = shortestPathCells();
        List<Cell> offPath = new ArrayList<>();
        List<Cell> onPath = new ArrayList<>();
        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r < ROWS; r++) {
                if ((c == startCol && r == startRow) || (c == exitCol && r == exitRow)) {
                    continue;
                }
                Cell cell = new Cell(c, r);
                if (path.contains(pack(c, r))) {
                    onPath.add(cell);
                } else {
                    offPath.add(cell);
                }
            }
        }
        Collections.shuffle(offPath, RANDOM);
        Collections.shuffle(onPath, RANDOM);

        List<BonusKind> kinds = new ArrayList<>(List.of(
                BonusKind.SHOT, BonusKind.SHOT, BonusKind.SHOT,
                BonusKind.SHIELD, BonusKind.SHIELD, BonusKind.SHIELD, BonusKind.TIME,
                BonusKind.MONEY, BonusKind.MONEY, BonusKind.MONEY,
                BonusKind.WEAPON, BonusKind.AMMO, BonusKind.TIME
        ));
        Collections.shuffle(kinds, RANDOM);

        List<Cell> pool = new ArrayList<>(offPath);
        pool.addAll(onPath);
        int n = Math.min(kinds.size(), pool.size());
        for (int i = 0; i < n; i++) {
            Cell cell = pool.get(i);
            addBonus(cell.c, cell.r, kinds.get(i));
        }
    }

    private void addBonus(int col, int row, BonusKind kind) {
        Bonus b = new Bonus(col, row, kind);
        javafx.scene.Node icon = switch (kind) {
            case MONEY -> coinIcon();
            case TIME -> textureIcon("pickup_time.png", BONUS_ICON, Color.LIMEGREEN);
            case SHIELD -> textureIcon("pickup_shield.png", BONUS_ICON, Color.AQUA);
            case SHOT -> textureIcon("pickup_missile.png", BONUS_ICON, Color.ORANGERED);
            case WEAPON -> textureIcon("pickup_weapon.png", BONUS_ICON, Color.GOLD);
            case AMMO -> textureIcon("pickup_ammo.png", BONUS_ICON, Color.CYAN);
        };
        Group g = new Group(icon);
        // Centrar el icono en la casilla (los iconos con textura se anclan en la esquina superior izquierda).
        g.setTranslateX(cellCenter(col, row)[0] - BONUS_ICON / 2.0);
        g.setTranslateY(cellCenter(col, row)[1] - BONUS_ICON / 2.0);
        if (kind == BonusKind.MONEY) {
            // coinIcon ya está centrado en (0,0) con radio 8
            g.setTranslateX(cellCenter(col, row)[0]);
            g.setTranslateY(cellCenter(col, row)[1]);
        }
        b.view = g;
        bonusLayer.getChildren().add(g);
        bonuses.add(b);
    }

    private static Group coinIcon() {
        Color coinColor = Color.rgb(240, 240, 240);
        Circle outer = new Circle(8);
        outer.setFill(new RadialGradient(
                0, 0, 0.3, 0.3, 0.8, true, CycleMethod.NO_CYCLE,
                new Stop(0, coinColor.brighter()),
                new Stop(0.5, coinColor),
                new Stop(1, coinColor.darker())
        ));
        Circle highlight = new Circle(3);
        highlight.setFill(Color.rgb(255, 255, 255, 0.7));
        highlight.setCenterX(-2);
        highlight.setCenterY(-2);
        return new Group(outer, highlight);
    }

    private static javafx.scene.Node textureIcon(String name, int size, Color fallback) {
        Image img = EmbeddedTextures.getImage(name, size, size);
        if (img != null && !img.isError()) {
            ImageView view = new ImageView(img);
            view.setFitWidth(size);
            view.setFitHeight(size);
            view.setSmooth(false);
            return view;
        }
        return EmbeddedTextures.createFallbackShip(size, fallback);
    }

    private void placeRimShips() {
        // Borde superior (dispara hacia abajo), inferior (hacia arriba), izquierdo (hacia la derecha), derecho (hacia la izquierda).
        int topCount = 5;
        int sideCount = 4;
        for (int i = 0; i < topCount; i++) {
            double t = (i + 1.0) / (topCount + 1.0);
            double x = originX + mazeW * t;
            double y = originY - RIM * 0.55;
            addRimShip(x, y, 0, 1, i * 0.35);
        }
        for (int i = 0; i < topCount; i++) {
            double t = (i + 1.0) / (topCount + 1.0);
            double x = originX + mazeW * t;
            double y = originY + mazeH + RIM * 0.55;
            addRimShip(x, y, 0, -1, i * 0.35 + 0.15);
        }
        for (int i = 0; i < sideCount; i++) {
            double t = (i + 1.0) / (sideCount + 1.0);
            double x = originX - RIM * 0.55;
            double y = originY + mazeH * t;
            addRimShip(x, y, 1, 0, i * 0.4 + 0.2);
        }
        for (int i = 0; i < sideCount; i++) {
            double t = (i + 1.0) / (sideCount + 1.0);
            double x = originX + mazeW + RIM * 0.55;
            double y = originY + mazeH * t;
            addRimShip(x, y, -1, 0, i * 0.4 + 0.3);
        }
    }

    private void addRimShip(double x, double y, double nx, double ny, double cd) {
        RimShip ship = new RimShip(x, y, nx, ny, cd);
        // Las texturas de las naves apuntan hacia arriba (0°). Se giran para que apunten hacia el laberinto.
        double facing = rimFacingDegrees(nx, ny);
        javafx.scene.Node body = rimShipSprite();
        body.setRotate(facing);
        Group g = new Group(body);
        g.setTranslateX(x - RIM_SHIP / 2.0);
        g.setTranslateY(y - RIM_SHIP / 2.0);
        ship.view = g;
        rimLayer.getChildren().add(g);
        rimShips.add(ship);
    }

    /** Grados en sentido horario desde "morro arriba" para disparar hacia dentro según la normal (nx, ny). */
    private static double rimFacingDegrees(double nx, double ny) {
        if (ny > 0) {
            return 180; // borde superior → mira hacia abajo
        }
        if (ny < 0) {
            return 0;   // borde inferior → mira hacia arriba
        }
        if (nx > 0) {
            return 90;  // borde izquierdo → mira a la derecha
        }
        return -90;     // borde derecho → mira a la izquierda
    }

    private javafx.scene.Node rimShipSprite() {
        String[] classic = {
                "enemy_basic.png", "enemy_fast.png", "enemy_tough.png",
                "enemy_alt.png", "enemy_yellow.png"
        };
        if (RANDOM.nextDouble() < 0.55) {
            String name = classic[RANDOM.nextInt(classic.length)];
            Color fb = switch (name) {
                case "enemy_fast.png" -> Color.LIME;
                case "enemy_tough.png" -> Color.MEDIUMPURPLE;
                case "enemy_yellow.png" -> Color.GOLD;
                default -> Color.CRIMSON;
            };
            return textureIcon(name, RIM_SHIP, fb);
        }
        int tile = SpriteSheets.shipTileFor(RANDOM.nextInt(SpriteSheets.SHIP_TILES));
        Image img = SpriteSheets.tile(SpriteSheets.SHIPS, tile);
        if (img != null && !img.isError()) {
            ImageView view = new ImageView(img);
            view.setFitWidth(RIM_SHIP);
            view.setFitHeight(RIM_SHIP);
            view.setPreserveRatio(true);
            view.setSmooth(false);
            return view;
        }
        return textureIcon("enemy_basic.png", RIM_SHIP, Color.CRIMSON);
    }

    private void placePlayerAtStart() {
        Entity player = host.player();
        if (player == null) {
            return;
        }
        savedPlayerX = player.getX();
        savedPlayerY = player.getY();
        savedPlayerRotation = player.getRotation();
        savedRotationOrigin = player.getTransformComponent().getRotationOrigin();
        player.getTransformComponent().setRotationOrigin(
                new Point2D(player.getWidth() / 2.0, player.getHeight() / 2.0));
        double[] ctr = cellCenter(startCol, startRow);
        // Se baja un poco del centro para que la nave quede en la casilla de inicio, con el morro hacia la salida.
        player.setX(ctr[0] - player.getWidth() / 2.0);
        player.setY(ctr[1] - player.getHeight() / 2.0 + 4);
        player.setRotation(0);
    }

    private void restorePlayer() {
        Entity player = host.player();
        if (player == null || !player.isActive()) {
            return;
        }
        player.setX(savedPlayerX);
        player.setY(savedPlayerY);
        player.setRotation(savedPlayerRotation);
        if (savedRotationOrigin != null) {
            player.getTransformComponent().setRotationOrigin(savedRotationOrigin);
        }
        player.getViewComponent().setOpacity(1.0);
    }

    private double[] cellCenter(int col, int row) {
        double x = originX + WALL + col * (CELL + WALL) + CELL / 2.0;
        double y = originY + WALL + row * (CELL + WALL) + CELL / 2.0;
        return new double[]{x, y};
    }

    // --- jugabilidad -------------------------------------------------------

    private void movePlayer(double tpf) {
        Entity player = host.player();
        if (player == null || !player.isActive()) {
            return;
        }

        double ix = 0;
        double iy = 0;
        if (host.movingLeft()) {
            ix -= 1;
        }
        if (host.movingRight()) {
            ix += 1;
        }
        if (host.movingUp()) {
            iy -= 1;
        }
        if (host.movingDown()) {
            iy += 1;
        }
        if (ix == 0 && iy == 0) {
            return;
        }
        // Sprite con el morro arriba: 0° = arriba, 90° = derecha, 180° = abajo, -90° = izquierda.
        player.setRotation(Math.toDegrees(Math.atan2(ix, -iy)));

        double len = Math.hypot(ix, iy);
        double dx = ix / len * PLAYER_SPEED * tpf;
        double dy = iy / len * PLAYER_SPEED * tpf;

        double x = player.getX() + player.getWidth() / 2.0;
        double y = player.getY() + player.getHeight() / 2.0;

        // Ayuda arcade para los huecos: al dirigirte a un pasillo, se te atrae hacia su centro
        // para que te "cueles" en la abertura sin tener que alinearte al píxel.
        boolean preferVert = Math.abs(iy) >= Math.abs(ix) && iy != 0;
        boolean preferHorz = Math.abs(ix) > Math.abs(iy) && ix != 0;
        if (preferVert) {
            x = assistAxis(x, y, true, tpf);
        }
        if (preferHorz) {
            y = assistAxis(x, y, false, tpf);
        }

        double nx = clampToPlay(x + dx, true);
        double ny = clampToPlay(y + dy, false);

        boolean movedX = false;
        boolean movedY = false;
        if (!circleHitsWall(nx, y, PLAYER_WALL_R, false)) {
            x = nx;
            movedX = true;
        }
        if (!circleHitsWall(x, ny, PLAYER_WALL_R, false)) {
            y = ny;
            movedY = true;
        }

        // ¿Bloqueado en el eje deseado? Se ajusta más hacia el centro y se reintenta una vez.
        if (preferVert && !movedY) {
            x = assistAxis(x, y, true, tpf * 2.5);
            ny = clampToPlay(y + dy, false);
            if (!circleHitsWall(x, ny, PLAYER_WALL_R, false)) {
                y = ny;
            }
        }
        if (preferHorz && !movedX) {
            y = assistAxis(x, y, false, tpf * 2.5);
            nx = clampToPlay(x + dx, true);
            if (!circleHitsWall(nx, y, PLAYER_WALL_R, false)) {
                x = nx;
            }
        }

        player.setX(x - player.getWidth() / 2.0);
        player.setY(y - player.getHeight() / 2.0);
    }

    /**
     * Acerca la coordenada del eje transversal al centro de la casilla más cercana cuando
     * está lo bastante cerca para colarse en un hueco. {@code towardX} true = ajustar X (al moverse en vertical).
     */
    private double assistAxis(double x, double y, boolean towardX, double tpf) {
        int col = colAt(x);
        int row = rowAt(y);
        double[] ctr = cellCenter(col, row);
        double target = towardX ? ctr[0] : ctr[1];
        double current = towardX ? x : y;
        double err = target - current;
        if (Math.abs(err) < 0.4 || Math.abs(err) > GAP_ASSIST) {
            return current;
        }
        double step = Math.signum(err) * Math.min(Math.abs(err), GAP_ASSIST_SPEED * tpf);
        double next = current + step;
        if (towardX) {
            next = clampToPlay(next, true);
            return circleHitsWall(next, y, PLAYER_WALL_R, false) ? current : next;
        }
        next = clampToPlay(next, false);
        return circleHitsWall(x, next, PLAYER_WALL_R, false) ? current : next;
    }

    private int colAt(double worldX) {
        double lx = worldX - originX - WALL;
        int col = (int) Math.floor(lx / (CELL + WALL));
        return Math.max(0, Math.min(COLS - 1, col));
    }

    private int rowAt(double worldY) {
        double ly = worldY - originY - WALL;
        int row = (int) Math.floor(ly / (CELL + WALL));
        return Math.max(0, Math.min(ROWS - 1, row));
    }

    private double clampToPlay(double v, boolean isX) {
        if (isX) {
            return Math.max(originX + PLAYER_WALL_R, Math.min(v, originX + mazeW - PLAYER_WALL_R));
        }
        return Math.max(originY + PLAYER_WALL_R, Math.min(v, originY + mazeH - PLAYER_WALL_R));
    }

    private boolean circleHitsWall(double cx, double cy, double radius, boolean bulletsIgnoreOuter) {
        // Coordenadas locales dentro del laberinto.
        double lx = cx - originX;
        double ly = cy - originY;
        if (!bulletsIgnoreOuter) {
            if (lx < radius || ly < radius || lx > mazeW - radius || ly > mazeH - radius) {
                // Permitir el hueco de salida arriba en el centro y el de entrada abajo en el centro.
                if (inExitNotch(cx, cy, radius) || inStartNotch(cx, cy, radius)) {
                    return false;
                }
                return true;
            }
        }

        // Tramos de pared verticales.
        for (int c = 0; c <= COLS; c++) {
            for (int r = 0; r < ROWS; r++) {
                if (!vWall[c][r]) {
                    continue;
                }
                if (bulletsIgnoreOuter && (c == 0 || c == COLS)) {
                    continue;
                }
                double wx = c * (CELL + WALL);
                double wy = r * (CELL + WALL) + WALL;
                if (circleRect(lx, ly, radius, wx, wy, WALL, CELL + WALL)) {
                    return true;
                }
            }
        }
        // Tramos de pared horizontales.
        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r <= ROWS; r++) {
                if (!hWall[c][r]) {
                    continue;
                }
                if (bulletsIgnoreOuter && (r == 0 || r == ROWS)) {
                    continue;
                }
                double wx = c * (CELL + WALL) + WALL;
                double wy = r * (CELL + WALL);
                if (circleRect(lx, ly, radius, wx, wy, CELL + WALL, WALL)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean inExitNotch(double cx, double cy, double radius) {
        double[] ctr = cellCenter(exitCol, exitRow);
        return Math.abs(cx - ctr[0]) < CELL * 0.45
                && cy < originY + WALL + radius + 2
                && cy > originY - RIM;
    }

    private boolean inStartNotch(double cx, double cy, double radius) {
        double[] ctr = cellCenter(startCol, startRow);
        return Math.abs(cx - ctr[0]) < CELL * 0.45
                && cy > originY + mazeH - WALL - radius - 2
                && cy < originY + mazeH + RIM;
    }

    private static boolean circleRect(double cx, double cy, double r,
                                      double rx, double ry, double rw, double rh) {
        double nx = Math.max(rx, Math.min(cx, rx + rw));
        double ny = Math.max(ry, Math.min(cy, ry + rh));
        double dx = cx - nx;
        double dy = cy - ny;
        return dx * dx + dy * dy < r * r;
    }

    private void updateRimFire(double tpf) {
        if (fireGraceLeft > 0) {
            return;
        }
        for (RimShip ship : rimShips) {
            if (ship.silenced) {
                continue;
            }
            ship.cooldown -= tpf;
            if (ship.cooldown > 0) {
                continue;
            }
            ship.cooldown = nextFireGap();
            spawnBullet(ship.x, ship.y, ship.nx * BULLET_SPEED, ship.ny * BULLET_SPEED);
        }
    }

    private void spawnBullet(double x, double y, double vx, double vy) {
        Bullet b = new Bullet(x, y, vx, vy);
        Circle c = new Circle(BULLET_R, Color.rgb(255, 120, 90));
        c.setStroke(Color.rgb(255, 220, 180));
        c.setStrokeWidth(1);
        c.setTranslateX(x);
        c.setTranslateY(y);
        b.view = c;
        bulletLayer.getChildren().add(c);
        bullets.add(b);
    }

    private void updateBullets(double tpf) {
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            b.x += b.vx * tpf;
            b.y += b.vy * tpf;
            b.view.setTranslateX(b.x);
            b.view.setTranslateY(b.y);

            // Atraviesan las paredes del laberinto; solo desaparecen en el borde cerrado del área de juego o en el extremo.
            boolean out = b.x < host.playLeft() || b.x > host.playRight()
                    || b.y < host.playTop() || b.y > host.playBottom();
            if (out) {
                bulletLayer.getChildren().remove(b.view);
                bullets.remove(i);
            }
        }
    }

    private void collectBonuses() {
        Entity player = host.player();
        if (player == null) {
            return;
        }
        double px = player.getX() + player.getWidth() / 2.0;
        double py = player.getY() + player.getHeight() / 2.0;
        for (Bonus bonus : bonuses) {
            if (bonus.taken) {
                continue;
            }
            double[] ctr = cellCenter(bonus.col, bonus.row);
            if (Math.hypot(px - ctr[0], py - ctr[1]) > 16) {
                continue;
            }
            bonus.taken = true;
            bonusLayer.getChildren().remove(bonus.view);
            collectedKinds.add(bonus.kind);
            switch (bonus.kind) {
                case MONEY -> {
                    moneyCollected += PICKUP_MONEY;
                    inc(GameVars.MONEY, PICKUP_MONEY);
                    SoundHelper.play("money.wav");
                }
                case TIME -> {
                    timeLeft += PICKUP_TIME;
                    SoundHelper.play("extra_time.wav");
                }
                case SHIELD -> {
                    shieldTime += SHIELD_SECONDS;
                    host.displayBanner(I18n.tr("banner.shield", (int) SHIELD_SECONDS), Color.AQUA, 0.8);
                    SoundHelper.play("shield.wav");
                }
                case SHOT -> {
                    shotsLoaded++;
                    SoundHelper.play("missile_launch.wav");
                }
                case WEAPON -> {
                    if (geti(GameVars.WEAPON_GRADE) < GameVars.MAX_WEAPON_GRADE) {
                        inc(GameVars.WEAPON_GRADE, 1);
                    }
                    inc(GameVars.SCORE, PICKUP_WEAPON_SCORE);
                    SoundHelper.play("weapon.wav");
                }
                case AMMO -> {
                    if (geti(GameVars.AMMO_CAP) < GameVars.MAX_AMMO_CAP) {
                        inc(GameVars.AMMO_CAP, 1);
                    }
                    inc(GameVars.SCORE, PICKUP_AMMO_SCORE);
                    SoundHelper.play("ammo.wav");
                }
            }
            refreshHud();
        }
    }

    private void checkBulletHits() {
        Entity player = host.player();
        if (player == null) {
            return;
        }
        double px = player.getX() + player.getWidth() / 2.0;
        double py = player.getY() + player.getHeight() / 2.0;
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            if (Math.hypot(px - b.x, py - b.y) > PLAYER_HURT_R + BULLET_R) {
                continue;
            }
            bulletLayer.getChildren().remove(b.view);
            bullets.remove(i);
            if (shieldUp()) {
                // El escudo bloquea cualquier impacto mientras le quede tiempo.
                shieldFlash = 0.35;
            } else {
                host.spawnExplosion(px, py, "ship");
                fail(I18n.tr("maze.hit"));
                return;
            }
        }
    }

    private void checkExit() {
        Entity player = host.player();
        if (player == null) {
            return;
        }
        double px = player.getX() + player.getWidth() / 2.0;
        double py = player.getY() + player.getHeight() / 2.0;
        double[] ctr = cellCenter(exitCol, exitRow);
        if (Math.abs(px - ctr[0]) < CELL * 0.4 && py < originY + WALL + CELL * 0.55) {
            win();
        }
    }

    private void pollFireKey() {
        boolean held = host.holdingFire();
        if (held && !fireWasHeld) {
            tryFireShot();
        }
        fireWasHeld = held;
    }

    private void fireSingleShot() {
        Entity player = host.player();
        if (player == null || bulletLayer == null) {
            return;
        }
        double px = player.getX() + player.getWidth() / 2.0;
        double py = player.getY() + player.getHeight() / 2.0;
        double rot = Math.toRadians(player.getRotation());
        // Sprite con el morro arriba: 0° → (0,-1)
        double fdx = Math.sin(rot);
        double fdy = -Math.cos(rot);

        RimShip best = pickRimTarget(px, py, fdx, fdy);
        if (best == null) {
            host.displayBanner(I18n.tr("maze.noTarget"), Color.LIGHTGRAY, 0.7);
            return;
        }

        shotsLoaded = Math.max(0, shotsLoaded - 1);
        refreshHud();
        SoundHelper.play("shot.wav");

        double dx = best.x - px;
        double dy = best.y - py;
        double dist = Math.hypot(dx, dy);
        if (dist < 1) {
            silenceTurret(best);
            return;
        }

        ShotBolt bolt = new ShotBolt();
        bolt.x = px;
        bolt.y = py;
        bolt.vx = dx / dist * SHOT_BOLT_SPEED;
        bolt.vy = dy / dist * SHOT_BOLT_SPEED;
        bolt.target = best;
        Circle c = new Circle(SHOT_BOLT_R, Color.rgb(255, 230, 90));
        c.setStroke(Color.WHITE);
        c.setStrokeWidth(1.5);
        c.setEffect(new DropShadow(8, Color.ORANGE));
        c.setTranslateX(px);
        c.setTranslateY(py);
        bolt.view = c;
        bulletLayer.getChildren().add(c);
        activeBolt = bolt;
    }

    private RimShip pickRimTarget(double px, double py, double fdx, double fdy) {
        RimShip bestFacing = null;
        double bestFacingScore = Double.MAX_VALUE;
        RimShip bestNear = null;
        double bestNearDist = Double.MAX_VALUE;
        for (RimShip ship : rimShips) {
            if (ship.silenced) {
                continue;
            }
            double dx = ship.x - px;
            double dy = ship.y - py;
            double dist = Math.hypot(dx, dy);
            if (dist < bestNearDist) {
                bestNearDist = dist;
                bestNear = ship;
            }
            if (dist < 1) {
                continue;
            }
            double dot = (dx / dist) * fdx + (dy / dist) * fdy;
            // Preferir las torretas que están más o menos delante (producto escalar > 0.25 ≈ dentro de ~75°).
            if (dot > 0.25) {
                double score = dist / (0.35 + dot);
                if (score < bestFacingScore) {
                    bestFacingScore = score;
                    bestFacing = ship;
                }
            }
        }
        return bestFacing != null ? bestFacing : bestNear;
    }

    private void updateShotBolt(double tpf) {
        if (activeBolt == null) {
            return;
        }
        ShotBolt bolt = activeBolt;
        bolt.x += bolt.vx * tpf;
        bolt.y += bolt.vy * tpf;
        bolt.view.setTranslateX(bolt.x);
        bolt.view.setTranslateY(bolt.y);

        boolean hit = false;
        if (bolt.target != null && !bolt.target.silenced) {
            if (Math.hypot(bolt.x - bolt.target.x, bolt.y - bolt.target.y) < RIM_SHIP * 0.55) {
                hit = true;
            }
        }
        boolean out = bolt.x < host.playLeft() - 20 || bolt.x > host.playRight() + 20
                || bolt.y < host.playTop() - 20 || bolt.y > host.playBottom() + 20;
        if (hit) {
            silenceTurret(bolt.target);
            clearShotBolt();
        } else if (out) {
            clearShotBolt();
        }
    }

    private void silenceTurret(RimShip ship) {
        if (ship == null || ship.silenced) {
            return;
        }
        ship.silenced = true;
        ship.view.setOpacity(0.25);
        host.spawnExplosion(ship.x, ship.y, "hit");
        SoundHelper.play("explode_ship.wav");
        host.displayBanner(I18n.tr("maze.turretDown"), Color.ORANGE, 0.9);
    }

    private void clearShotBolt() {
        if (activeBolt == null) {
            return;
        }
        if (bulletLayer != null && activeBolt.view != null) {
            bulletLayer.getChildren().remove(activeBolt.view);
        }
        activeBolt = null;
    }

    private void win() {
        if (finished) {
            return;
        }
        finished = true;
        int leftover = Math.max(0, (int) Math.ceil(timeLeft));
        int turrets = silencedTurretCount();
        int picked = collectedKinds.size();
        // Todos los puntos x10 (SCORE_MULT); el dinero se queda igual.
        int timeScore = leftover * 70 * SCORE_MULT;
        int turretScore = turrets * 280 * SCORE_MULT;
        int bonusScore = picked * 150 * SCORE_MULT;
        int score = 1800 * SCORE_MULT + timeScore + turretScore + bonusScore + moneyCollected * 4 * SCORE_MULT;
        int money = 60 + leftover * 2 + turrets * 12 + picked * 8;
        if (shieldUp()) {
            score += 350 * SCORE_MULT;
            money += 20;
        }
        // Súper bonus secreto: se han cogido de verdad TODOS los extras del laberinto.
        perfectRun = !bonuses.isEmpty() && bonuses.stream().allMatch(b -> b.taken);
        if (perfectRun) {
            perfectScore = score * 4 + 5000 * SCORE_MULT;
            perfectMoney = 500 + money * 3;
            score += perfectScore;
            money += perfectMoney;
        }
        inc(GameVars.MAZE_WINS, 1);
        beginSettlement(true, score, money, I18n.tr("maze.clear", score), Color.MEDIUMPURPLE,
                leftover, turrets, picked, timeScore, turretScore, bonusScore);
    }

    private void fail(String reason) {
        if (finished) {
            return;
        }
        finished = true;
        int turrets = silencedTurretCount();
        int picked = collectedKinds.size();
        boolean lite = turrets > 0 || picked > 0 || moneyCollected > 0;
        int score;
        int money;
        int turretPts = turrets * 80 * SCORE_MULT;
        int bonusPts = picked * 70 * SCORE_MULT;
        if (lite) {
            score = 200 * SCORE_MULT + turretPts + bonusPts + moneyCollected * SCORE_MULT;
            money = 20 + turrets * 5 + picked * 4 + moneyCollected / 2;
        } else {
            score = 100 * SCORE_MULT;
            money = 10;
            turretPts = 0;
            bonusPts = 0;
        }
        beginSettlement(false, score, money, reason + "  +" + score, Color.ORANGERED,
                0, turrets, picked, 0, turretPts, bonusPts);
    }

    private int silencedTurretCount() {
        int n = 0;
        for (RimShip ship : rimShips) {
            if (ship.silenced) {
                n++;
            }
        }
        return n;
    }

    private void beginSettlement(boolean won, int score, int money, String banner, Color color,
                                 int leftover, int turrets, int picked,
                                 int timeScore, int turretScore, int bonusScore) {
        pendingWon = won;
        pendingScore = score;
        pendingMoney = money;
        pendingBanner = banner;
        pendingColor = color;
        awaitingSettlement = true;
        settlementTimer = SETTLEMENT_SECS;
        settlementShownFor = 0;
        buildSettlementOverlay(won, leftover, turrets, picked, timeScore, turretScore, bonusScore, score, money);
    }

    private void buildSettlementOverlay(boolean won, int leftover, int turrets, int picked,
                                        int timeScore, int turretScore, int bonusScore,
                                        int totalScore, int totalMoney) {
        if (settlementRoot != null) {
            host.removeUi(settlementRoot);
        }

        double w = host.playRight() - host.playLeft();
        double panelW = Math.min(440, w - 24);

        Rectangle dim = new Rectangle(host.playRight() - host.playLeft() + 8,
                host.playBottom() - host.playTop() + 8);
        dim.setTranslateX(host.playLeft() - 4);
        dim.setTranslateY(host.playTop() - 4);
        dim.setFill(Color.rgb(0, 0, 0, 0.72));

        VBox panel = new VBox(7);
        panel.setAlignment(Pos.TOP_LEFT);
        panel.setPadding(new Insets(16, 20, 16, 20));
        panel.setPrefWidth(panelW);
        panel.setStyle(
                "-fx-background-color: rgb(12,18,34);"
                        + "-fx-border-color: #a070e0;"
                        + "-fx-border-width: 2;"
                        + "-fx-background-radius: 10;"
                        + "-fx-border-radius: 10;");

        Text title = new Text(won ? I18n.tr("maze.settlement") : I18n.tr("maze.result"));
        title.setFont(Font.font("Monospace", FontWeight.BOLD, 18));
        title.setFill(won ? Color.MEDIUMPURPLE.brighter() : Color.ORANGERED);
        title.setEffect(new DropShadow(10, Color.BLACK));

        panel.getChildren().add(title);
        panel.getChildren().add(settleLine(I18n.tr("maze.timeLeft"), leftover + "s", "+" + timeScore, Color.CYAN));
        panel.getChildren().add(settleLine(I18n.tr("maze.turretsDown"), String.valueOf(turrets), "+" + turretScore, Color.ORANGE));
        panel.getChildren().add(settleLine(I18n.tr("maze.bonuses"), String.valueOf(picked), "+" + bonusScore, Color.GOLD));

        Text lootCaption = new Text(I18n.tr("maze.collected"));
        lootCaption.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
        lootCaption.setFill(Color.rgb(180, 190, 210));
        panel.getChildren().add(lootCaption);

        HBox icons = new HBox(6);
        icons.setAlignment(Pos.CENTER_LEFT);
        if (collectedKinds.isEmpty()) {
            Text none = new Text(I18n.tr("maze.none"));
            none.setFont(Font.font("Monospace", FontWeight.BOLD, 11));
            none.setFill(Color.rgb(120, 130, 150));
            icons.getChildren().add(none);
        } else {
            for (BonusKind kind : collectedKinds) {
                icons.getChildren().add(settleIcon(kind));
            }
        }
        panel.getChildren().add(icons);

        if (won && perfectRun) {
            perfectText = new Text(I18n.tr("maze.perfect", perfectScore, perfectMoney));
            perfectText.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
            perfectText.setFill(Color.GOLD);
            perfectText.setEffect(new javafx.scene.effect.Glow(0.9));
            panel.getChildren().add(perfectText);
        }

        Text totals = new Text(I18n.tr("maze.totals", totalScore, totalMoney));
        totals.setFont(Font.font("Monospace", FontWeight.BOLD, 14));
        totals.setFill(Color.WHITE);
        panel.getChildren().add(totals);

        Text hint = new Text(I18n.tr("maze.hint"));
        hint.setFont(Font.font("Monospace", FontWeight.BOLD, 10));
        hint.setFill(Color.rgb(150, 170, 190));
        panel.getChildren().add(hint);

        panel.setTranslateX(host.playLeft() + (w - panelW) / 2.0);
        panel.setTranslateY(host.playTop() + 70);

        settlementRoot = new Group(dim, panel);
        settlementRoot.setMouseTransparent(true);
        host.addUi(settlementRoot);
        ui.add(settlementRoot);
    }

    /** Línea de PERFECTO que parpadea en colores y late. */
    private void animatePerfectLine() {
        if (perfectText == null) {
            return;
        }
        double t = settlementShownFor;
        perfectText.setFill(Color.hsb((t * 240) % 360, 0.85, 1.0));
        double sc = 1.0 + 0.08 * Math.sin(t * 9);
        perfectText.setScaleX(sc);
        perfectText.setScaleY(sc);
    }

    private HBox settleLine(String label, String value, String pts, Color accent) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Text l = new Text(String.format("%-14s %4s", label, value));
        l.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
        l.setFill(accent);
        Text p = new Text(pts);
        p.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
        p.setFill(Color.WHITE);
        row.getChildren().addAll(l, p);
        return row;
    }

    private javafx.scene.Node settleIcon(BonusKind kind) {
        return switch (kind) {
            case MONEY -> {
                Group g = coinIcon();
                g.setScaleX(SETTLE_ICON / 16.0);
                g.setScaleY(SETTLE_ICON / 16.0);
                yield g;
            }
            case TIME -> textureIcon("pickup_time.png", SETTLE_ICON, Color.LIMEGREEN);
            case SHIELD -> textureIcon("pickup_shield.png", SETTLE_ICON, Color.AQUA);
            case SHOT -> textureIcon("pickup_missile.png", SETTLE_ICON, Color.ORANGERED);
            case WEAPON -> textureIcon("pickup_weapon.png", SETTLE_ICON, Color.GOLD);
            case AMMO -> textureIcon("pickup_ammo.png", SETTLE_ICON, Color.CYAN);
        };
    }

    private void completeSettlement() {
        if (!awaitingSettlement) {
            return;
        }
        awaitingSettlement = false;
        if (settlementRoot != null) {
            host.removeUi(settlementRoot);
            ui.remove(settlementRoot);
            settlementRoot = null;
        }
        host.finishMinigame(pendingWon, pendingScore, pendingMoney, pendingBanner, pendingColor);
    }
}
