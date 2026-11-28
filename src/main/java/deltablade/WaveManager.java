package deltablade;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import deltablade.components.EnemyComponent;
import javafx.geometry.Point2D;

import java.util.ArrayList;
import java.util.List;

import static com.almasb.fxgl.dsl.FXGL.*;

public class WaveManager {

    public enum WaveType {
        FIGHTERS,
        MIXED,
        RANK,
        PEARL,
        BONUS,
        BOSS,
        KAMIKAZE,
        MONSTER,
        METEOR,
        SPINNER,      // los grupos primero dan vueltas alrededor del centro
        PINCER,       // dos grupos a la vez, uno por la izquierda y otro por la derecha
        CASCADE,      // muchos grupos pequeños bajando en zigzag desde arriba
        RUSH          // las naves se lanzan en picado muy pronto y a menudo
    }

    /** Nuevo en cada partida: añade algo de azar al orden de las oleadas. */
    private static long runSeed = System.nanoTime();

    public WaveManager() {
        runSeed = System.nanoTime();
    }

    public enum FormationType {
        ROW,
        V_FORMATION,
        DIAMOND,
        TWO_COLUMNS,
        STAGGERED
    }

    private int currentLevel;
    private WaveType currentWaveType;
    private List<Squad> activeSquads = new ArrayList<>();
    private List<Point2D> formationSlots = new ArrayList<>();
    private int totalEnemiesInWave = 0;
    private int enemiesSpawned = 0;
    private int squadsToSpawn = 0;
    private double squadSpawnTimer = 0;
    private double squadSpawnDelay = 2.5;
    private double initialDelay = 0.2;
    private boolean initialDelayPassed = false;
    private boolean stopped = false;
    private int bonusPerfectStreak = 0;

    public static class Squad {
        public final int id;
        public final List<Entity> enemies = new ArrayList<>();
        public final int originalSize;
        public int destroyedWhileEntering = 0;
        public int escaped = 0;
        public boolean settled = false;
        public boolean bonusResolved = false;

        public Squad(int id, int size) {
            this.id = id;
            this.originalSize = size;
        }

        public void addEnemy(Entity enemy) {
            enemies.add(enemy);
        }

        public void onEnemyDestroyed(boolean wasEntering) {
            if (wasEntering && !settled) {
                destroyedWhileEntering++;
            }
        }

        public boolean isFullyDestroyed() {
            return enemies.stream().noneMatch(Entity::isActive);
        }

        public boolean earnedComboBonus() {
            return destroyedWhileEntering == originalSize && !settled;
        }

        public void markSettled() {
            settled = true;
        }
    }

    public void startWave(int level) {
        startWave(level, resolveWaveType(level));
    }

    public void startWave(int level, WaveType forcedType) {
        this.currentLevel = level;
        this.activeSquads.clear();
        this.formationSlots.clear();
        this.enemiesSpawned = 0;
        this.squadSpawnTimer = 0;
        this.initialDelayPassed = false;
        this.squadSpawnDelay = 2.5;
        this.bonusPerfectStreak = 0;
        this.stopped = false;
        this.groupDiveTimer = 3.0;
        this.monsterKills = 0;
        this.alienBossSpawned = false;

        currentWaveType = forcedType != null ? forcedType : resolveWaveType(level);

        switch (currentWaveType) {
            case FIGHTERS -> setupFightersWave(level);
            case MIXED -> setupMixedWave(level);
            case RANK -> setupRankWave(level);
            case PEARL -> setupPearlWave(level);
            case BONUS -> setupBonusWave(level);
            case BOSS -> setupBossWave(level);
            case KAMIKAZE -> setupKamikazeWave(level);
            case MONSTER -> setupMonsterWave(level);
            case METEOR -> setupMeteorWave(level);
            case SPINNER -> setupFlightWave(level, FormationType.DIAMOND, 3);
            case PINCER -> setupFlightWave(level, FormationType.TWO_COLUMNS, 4);
            case CASCADE -> {
                setupFlightWave(level, FormationType.STAGGERED, 5);
                squadSpawnDelay = 1.4;
            }
            case RUSH -> setupFlightWave(level, FormationType.V_FORMATION, 3);
        }

        if (currentWaveType == WaveType.METEOR) {
            spawnCoverMeteors();
        }

        set(GameVars.ENEMIES_REMAINING, totalEnemiesInWave);
        set(GameVars.EXTRA_LETTER_SPAWNED_THIS_WAVE, 0);
    }

    public static WaveType resolveWaveType(int level) {
        if (level > 0 && level % GameVars.BOSS_EVERY == 0) {
            return WaveType.BOSS;
        }
        if (level >= GameVars.MONSTER_WAVE_MIN_LEVEL && level % 8 == 6) {
            return WaveType.MONSTER;
        }
        if (level <= 2) {
            return level == 1 ? WaveType.FIGHTERS : WaveType.MIXED;
        }
        // Orden menos rígido: se elige de una bolsa que crece con el nivel.
        List<WaveType> pool = new ArrayList<>(List.of(
                WaveType.FIGHTERS, WaveType.MIXED, WaveType.PEARL, WaveType.SPINNER, WaveType.CASCADE));
        if (level >= 4) {
            pool.add(WaveType.PINCER);
            pool.add(WaveType.RANK);
        }
        if (level >= 6) {
            pool.add(WaveType.RUSH);
            pool.add(WaveType.BONUS);
            pool.add(WaveType.KAMIKAZE);
        }
        java.util.Random rnd = new java.util.Random(runSeed * 31 + level * 7919L);
        WaveType pick = pool.get(rnd.nextInt(pool.size()));
        WaveType prev = level - 1 > 0 && (level - 1) % GameVars.BOSS_EVERY != 0
                ? resolveWaveTypeSimple(level - 1) : null;
        if (pick == prev) {
            pick = pool.get((pool.indexOf(pick) + 1) % pool.size());
        }
        return pick;
    }

    /** Como resolveWaveType, pero sin comparar con la oleada anterior (evita una recursión larga). */
    private static WaveType resolveWaveTypeSimple(int level) {
        if (level <= 2) {
            return level == 1 ? WaveType.FIGHTERS : WaveType.MIXED;
        }
        if (level >= GameVars.MONSTER_WAVE_MIN_LEVEL && level % 8 == 6) {
            return WaveType.MONSTER;
        }
        List<WaveType> pool = new ArrayList<>(List.of(
                WaveType.FIGHTERS, WaveType.MIXED, WaveType.PEARL, WaveType.SPINNER, WaveType.CASCADE));
        if (level >= 4) {
            pool.add(WaveType.PINCER);
            pool.add(WaveType.RANK);
        }
        if (level >= 6) {
            pool.add(WaveType.RUSH);
            pool.add(WaveType.BONUS);
            pool.add(WaveType.KAMIKAZE);
        }
        java.util.Random rnd = new java.util.Random(runSeed * 31 + level * 7919L);
        return pool.get(rnd.nextInt(pool.size()));
    }

    @SuppressWarnings("unused")
    private static WaveType oldFixedOrder(int level) {
        int slot = ((level - 1) % 5) + 1;
        return switch (slot) {
            case 1 -> WaveType.FIGHTERS;
            case 2 -> WaveType.MIXED;
            case 3 -> WaveType.PEARL;
            case 4 -> {
                int pack = (level - 1) / 5;
                yield switch (pack % 3) {
                    case 0 -> WaveType.RANK;
                    case 1 -> WaveType.BONUS;
                    default -> WaveType.KAMIKAZE;
                };
            }
            default -> WaveType.FIGHTERS;
        };
    }

    private void setupFlightWave(int level, FormationType formation, int squads) {
        int cycle = (level - 1) / 4 + 1;
        int count = Math.min(10 + cycle * 2, 16);
        generateFormationSlots(formation, count);
        totalEnemiesInWave = formationSlots.size();
        squadsToSpawn = squads;
    }

    private void setupFightersWave(int level) {
        int cycle = (level - 1) / 4 + 1;
        int baseCount = level == 1 ? 6 : Math.min(8 + cycle * 2, 14);
        FormationType formation = cycle % 2 == 1 ? FormationType.ROW : FormationType.V_FORMATION;
        generateFormationSlots(formation, baseCount);
        totalEnemiesInWave = formationSlots.size();
        squadsToSpawn = Math.min(2 + cycle / 2, 3);
    }

    private void setupMixedWave(int level) {
        int cycle = (level - 1) / 4 + 1;
        int baseCount = Math.min(10 + cycle * 2, 16);
        FormationType formation = switch (cycle % 3) {
            case 1 -> FormationType.DIAMOND;
            case 2 -> FormationType.TWO_COLUMNS;
            default -> FormationType.STAGGERED;
        };
        generateFormationSlots(formation, baseCount);
        totalEnemiesInWave = formationSlots.size();
        squadsToSpawn = Math.min(3 + cycle / 2, 4);
    }

    private void setupRankWave(int level) {
        int cycle = (level - 1) / 4 + 1;
        int baseCount = Math.min(6 + cycle, 10);
        generateFormationSlots(FormationType.ROW, baseCount);
        totalEnemiesInWave = formationSlots.size();
        squadsToSpawn = 2;
    }

    private void setupPearlWave(int level) {
        int cycle = (level - 1) / 5 + 1;
        int total = Math.min(18 + cycle * 4, 28);
        if (total % 2 != 0) {
            total++;
        }
        generatePearlSlots(total);
        totalEnemiesInWave = formationSlots.size();
        squadsToSpawn = 1;
    }

    private void generatePearlSlots(int count) {
        double left = GameVars.RAIL_WIDTH + 24;
        double right = getAppWidth() - GameVars.RAIL_WIDTH - 24;
        double width = right - left;
        int rows = count > 20 ? 3 : 2;
        int perRow = (int) Math.ceil(count / (double) rows);
        double gap = Math.max(44, Math.min(58, width / perRow));
        double used = Math.max(0, perRow - 1) * gap;
        double startX = left + Math.max(0, (width - used) / 2.0);
        int created = 0;
        for (int row = 0; row < rows && created < count; row++) {
            int inRow = Math.min(perRow, count - created);
            double rowStart = startX + (perRow - inRow) * gap / 2.0;
            double y = clampFormationY(52 + row * 50);
            for (int col = 0; col < inRow; col++) {
                formationSlots.add(new Point2D(rowStart + col * gap, y));
                created++;
            }
        }
    }

    private void setupBonusWave(int level) {
        int cycle = (level - 1) / 4 + 1;
        int total = Math.min(10 + cycle * 2, 16);
        placeFlythroughSlots(total, 70, 48);
        totalEnemiesInWave = total;
        squadsToSpawn = 3 + cycle % 2;
        squadSpawnDelay = 1.8;
    }

    private void setupBossWave(int level) {
        int escortCount = 2 + Math.min((level - 1) / 4, 2);

        double size = GameVars.BOSS_DRAW_SIZE;
        double centerX = getAppWidth() / 2.0 - size / 2.0;
        formationSlots.add(new Point2D(centerX, 50));

        double spacing = 70;
        double escortY = clampFormationY(50 + size + 10);
        for (int i = 0; i < escortCount; i++) {
            double offset = (i % 2 == 0 ? -1 : 1) * spacing * ((i / 2) + 1);
            formationSlots.add(new Point2D(getAppWidth() / 2.0 - 24 + offset, escortY));
        }

        totalEnemiesInWave = 1 + escortCount;
        squadsToSpawn = 1;
    }

    private void setupMonsterWave(int level) {
        int cycle = Math.max(1, (level - 5) / 8 + 1);
        int total = Math.min(8 + cycle * 2, 14);
        generateFormationSlots(cycle % 2 == 0 ? FormationType.STAGGERED : FormationType.V_FORMATION, total);
        totalEnemiesInWave = formationSlots.size();
        monsterTotal = totalEnemiesInWave;
        squadsToSpawn = 2 + cycle % 2;
        squadSpawnDelay = 2.1;
    }

    private void setupKamikazeWave(int level) {
        int cycle = (level - 1) / 4 + 1;
        int totalKamikaze = Math.min(8 + cycle * 2, 14);
        placeFlythroughSlots(totalKamikaze, 60, 52);
        totalEnemiesInWave = totalKamikaze;
        squadsToSpawn = 2 + cycle % 2;
        squadSpawnDelay = 1.5;
    }

    private void setupMeteorWave(int level) {
        int cycle = Math.max(1, (level - 1) / 8 + 1);
        int total = Math.min(6 + cycle, 8);
        placeMeteorHideSlots(total);
        totalEnemiesInWave = formationSlots.size();
        squadsToSpawn = 2;
        squadSpawnDelay = 2.4;
    }

    private void spawnCoverMeteors() {
        List<double[]> rocks = coverMeteorLayout();
        for (int i = 0; i < rocks.size(); i++) {
            double[] rock = rocks.get(i);
            double spin = (i % 2 == 0 ? -1 : 1) * (6 + i * 2);
            spawn("coverMeteor", new SpawnData(rock[1], rock[2])
                    .put("variant", (int) rock[0])
                    .put("scale", rock[5])
                    .put("spin", spin));
        }
    }

    /** variante, x, y, ancho, alto, escala, cx, cy */
    private List<double[]> coverMeteorLayout() {
        double left = GameVars.RAIL_WIDTH + 18;
        double playW = getAppWidth() - 2.0 * GameVars.RAIL_WIDTH - 36;
        double playH = getAppHeight();
        // variante, nx, ny, escala: el grupo se sitúa en mitad del campo para que las naves puedan esconderse detrás
        double[][] spec = {
                {1, 0.18, 0.24, 0.92},
                {0, 0.50, 0.20, 1.15},
                {3, 0.84, 0.23, 1.35},
                {2, 0.34, 0.35, 1.45},
                {1, 0.70, 0.37, 0.72},
                {0, 0.12, 0.48, 1.05},
                {3, 0.48, 0.50, 1.25},
                {2, 0.86, 0.46, 1.55}
        };
        List<double[]> rocks = new ArrayList<>();
        for (double[] row : spec) {
            int variant = (int) row[0];
            double scale = row[3];
            double[] nativeSize = switch (variant) {
                case 0 -> new double[] {81, 69};
                case 1 -> new double[] {125, 118};
                case 2 -> new double[] {46, 39};
                default -> new double[] {52, 47};
            };
            double w = nativeSize[0] * scale;
            double h = nativeSize[1] * scale;
            double cx = left + row[1] * playW;
            double cy = row[2] * playH;
            rocks.add(new double[] {variant, cx - w / 2.0, cy - h / 2.0, w, h, scale, cx, cy});
        }
        return rocks;
    }

    private void placeMeteorHideSlots(int total) {
        List<double[]> rocks = new ArrayList<>(coverMeteorLayout());
        rocks.sort((a, b) -> Double.compare(b[3] * b[4], a[3] * a[4]));
        int n = Math.min(total, rocks.size());
        for (int i = 0; i < n; i++) {
            double[] rock = rocks.get(i);
            formationSlots.add(new Point2D(rock[6] - 24, rock[7] - 24));
        }
    }

    private void placeFlythroughSlots(int total, double startY, double spacingY) {
        double playableLeft = GameVars.RAIL_WIDTH + 50;
        double playableWidth = getAppWidth() - 2 * GameVars.RAIL_WIDTH - 100;
        int cols = 4;
        for (int i = 0; i < total; i++) {
            int col = i % cols;
            int row = i / cols;
            double x = playableLeft + (col + 0.5) * playableWidth / cols;
            double y = clampFormationY(startY + row * spacingY);
            formationSlots.add(new Point2D(x, y));
        }
    }

    private void generateFormationSlots(FormationType type, int targetCount) {
        double playableLeft = GameVars.RAIL_WIDTH + 40;
        double playableRight = getAppWidth() - GameVars.RAIL_WIDTH - 40;
        double playableWidth = playableRight - playableLeft;
        double minGap = 96;

        switch (type) {
            case ROW -> {
                int perRow = Math.max(1, Math.min(targetCount, (int) (playableWidth / minGap)));
                int rows = (int) Math.ceil((double) targetCount / perRow);
                int created = 0;
                for (int row = 0; row < rows && created < targetCount; row++) {
                    int inThisRow = Math.min(perRow, targetCount - created);
                    addSpreadRow(playableLeft, playableWidth, inThisRow, clampFormationY(56 + row * 72));
                    created += inThisRow;
                }
            }
            case V_FORMATION -> {
                double centerX = getAppWidth() / 2.0;
                double spacingX = 92;
                double spacingY = 72;
                int created = 0;
                int row = 0;
                while (created < targetCount) {
                    int inThisRow = Math.min(row + 1, targetCount - created);
                    for (int i = 0; i < inThisRow && created < targetCount; i++) {
                        double offsetX = (i - (inThisRow - 1) / 2.0) * spacingX;
                        formationSlots.add(new Point2D(centerX + offsetX - 24, clampFormationY(56 + row * spacingY)));
                        created++;
                    }
                    row++;
                    if (row > 5) break;
                }
            }
            case DIAMOND -> {
                double centerX = getAppWidth() / 2.0 - 24;
                double spacingX = 96;
                double spacingY = 76;
                int[] rowCounts = {1, 2, 3, 2, 1};
                int created = 0;
                for (int row = 0; row < rowCounts.length && created < targetCount; row++) {
                    int inThisRow = Math.min(rowCounts[row], targetCount - created);
                    for (int i = 0; i < inThisRow && created < targetCount; i++) {
                        double offsetX = (i - (inThisRow - 1) / 2.0) * spacingX;
                        formationSlots.add(new Point2D(centerX + offsetX, clampFormationY(56 + row * spacingY)));
                        created++;
                    }
                }
            }
            case TWO_COLUMNS -> {
                double leftX = playableLeft + playableWidth * 0.22;
                double rightX = playableLeft + playableWidth * 0.78;
                double spacingY = 76;
                int perColumn = (targetCount + 1) / 2;
                double maxY = formationMaxY();
                if (perColumn > 1) {
                    spacingY = Math.min(spacingY, (maxY - 56) / (perColumn - 1));
                }
                int created = 0;
                for (int i = 0; i < perColumn && created < targetCount; i++) {
                    formationSlots.add(new Point2D(leftX, clampFormationY(56 + i * spacingY)));
                    created++;
                    if (created < targetCount) {
                        formationSlots.add(new Point2D(rightX, clampFormationY(56 + i * spacingY)));
                        created++;
                    }
                }
            }
            case STAGGERED -> {
                int perRow = Math.max(1, Math.min(4, (int) (playableWidth / minGap)));
                int rows = (int) Math.ceil((double) targetCount / perRow);
                int created = 0;
                for (int row = 0; row < rows && created < targetCount; row++) {
                    int inThisRow = Math.min(perRow, targetCount - created);
                    double inset = (row % 2 == 1) ? minGap * 0.35 : 0;
                    addSpreadRow(playableLeft + inset, playableWidth - inset * 2, inThisRow, clampFormationY(56 + row * 72));
                    created += inThisRow;
                }
            }
        }
    }

    private void addSpreadRow(double left, double width, int count, double y) {
        if (count <= 0) {
            return;
        }
        if (count == 1) {
            formationSlots.add(new Point2D(left + width / 2.0 - 24, clampFormationY(y)));
            return;
        }
        double spacing = width / (count + 1);
        spacing = Math.max(96, spacing);
        double used = (count - 1) * spacing;
        double startX = left + Math.max(0, (width - used) / 2.0);
        for (int col = 0; col < count; col++) {
            formationSlots.add(new Point2D(startX + col * spacing, clampFormationY(y)));
        }
    }

    private static double formationMaxY() {
        return getAppHeight() * GameVars.FORMATION_MAX_Y_RATIO;
    }

    private static double clampFormationY(double y) {
        return Math.min(y, formationMaxY());
    }

    public void stop() {
        stopped = true;
    }

    // Movimiento común de la formación: balanceo + "respiración".
    private static double formationClock = 0;

    public static double formationSwayX() {
        return Math.sin(formationClock * 0.9) * 30;
    }

    public static double formationBreath() {
        return 1.0 + 0.06 * Math.sin(formationClock * 1.4);
    }

    private double groupDiveTimer = 3.0;

    /** Paso 6: cada cierto tiempo, 1-3 naves vecinas se lanzan juntas en picado. */
    private void updateGroupDives(double tpf) {
        groupDiveTimer -= tpf;
        if (groupDiveTimer > 0) {
            return;
        }
        double gap = Math.max(1.3, 4.6 - currentLevel * 0.15);
        if (currentWaveType == WaveType.RUSH) {
            gap = Math.max(0.8, gap * 0.45);
        }
        groupDiveTimer = gap * (0.8 + Math.random() * 0.4);

        List<Entity> ready = new ArrayList<>();
        for (Entity e : getGameWorld().getEntitiesByType(EntityType.ENEMY)) {
            if (e.isActive() && e.hasComponent(EnemyComponent.class)
                    && e.getComponent(EnemyComponent.class).canGroupDive()) {
                ready.add(e);
            }
        }
        if (ready.isEmpty()) {
            return;
        }
        int maxGroup = currentLevel < 3 ? 1 : currentLevel < 6 ? 2 : 3;
        if (currentWaveType == WaveType.RUSH) {
            maxGroup = 3;
        }
        int size = 1 + (int) (Math.random() * maxGroup);
        Entity leader = ready.get((int) (Math.random() * ready.size()));
        ready.sort((a, b) -> Double.compare(a.distance(leader), b.distance(leader)));
        int side = leader.getCenter().getX() < getAppWidth() / 2.0 ? -1 : 1;
        for (int i = 0; i < Math.min(size, ready.size()); i++) {
            ready.get(i).getComponent(EnemyComponent.class).scheduleGroupDive(i * 0.22, side);
        }
    }

    public void update(double tpf) {
        formationClock += tpf;
        if (stopped) {
            return;
        }
        updateGroupDives(tpf);
        if (!initialDelayPassed) {
            squadSpawnTimer += tpf;
            if (squadSpawnTimer >= initialDelay) {
                initialDelayPassed = true;
                squadSpawnTimer = 0;
                spawnNextSquad();
            }
            return;
        }

        if (enemiesSpawned < totalEnemiesInWave) {
            squadSpawnTimer += tpf;
            double delayNow = currentWaveType == WaveType.PINCER && activeSquads.size() % 2 == 1
                    ? 0.35 : squadSpawnDelay;
            if (squadSpawnTimer >= delayNow) {
                squadSpawnTimer = 0;
                if (squadsToSpawn <= 0) {
                    squadsToSpawn = 1;
                }
                spawnNextSquad();
            }
        }

        if (currentWaveType == WaveType.BONUS) {
            checkBonusPerfects();
        } else {
            checkSquadBonuses();
        }
    }

    private void spawnNextSquad() {
        if (stopped || enemiesSpawned >= totalEnemiesInWave || squadsToSpawn <= 0) return;

        squadsToSpawn--;

        int remaining = totalEnemiesInWave - enemiesSpawned;
        int squadSize;

        if (currentWaveType == WaveType.BOSS && activeSquads.isEmpty()) {
            squadSize = remaining;
        } else if (currentWaveType == WaveType.PEARL) {
            squadSize = remaining;
        } else {
            int baseSize = Math.max(3, remaining / Math.max(squadsToSpawn + 1, 1));
            squadSize = Math.min(baseSize, 5);
            squadSize = Math.min(squadSize, remaining);
        }

        if (squadSize <= 0) return;

        EntryPath path = generateEntryPath();
        if (currentWaveType == WaveType.PEARL && path.pairGap > 0.5 && squadSize % 2 == 1 && remaining > squadSize) {
            squadSize++;
        }

        Squad squad = new Squad(activeSquads.size(), squadSize);
        activeSquads.add(squad);

        for (int i = 0; i < squadSize && enemiesSpawned < totalEnemiesInWave; i++) {
            Point2D targetSlot = formationSlots.get(enemiesSpawned);

            EnemyComponent.EnemyType type = determineEnemyType(enemiesSpawned);

            final int index = i;
            final int squadIdFinal = squad.id;
            final EnemyComponent.EnemyType finalType = type;

            boolean paired = currentWaveType == WaveType.PEARL && path.pairGap > 0.5;
            int pairIndex = paired ? index / 2 : index;
            double laneShift = 0;
            if (paired) {
                laneShift = (index % 2 == 0 ? -0.5 : 0.5) * path.pairGap;
            } else if (path.flight != null) {
                laneShift = 0;
            } else if (currentWaveType != WaveType.PEARL) {
                laneShift = (index - (squadSize - 1) / 2.0) * 42;
            }
            double startX = path.startX + laneShift;
            double startY;
            if (path.flight != null) {
                startY = path.startY;
            } else if (currentWaveType == WaveType.PEARL) {
                double trail = pairIndex * 44;
                startY = path.type == EntryPath.Type.FROM_BELOW ? path.startY + trail : path.startY - trail;
            } else {
                startY = path.startY + (path.type == EntryPath.Type.FROM_BELOW ? -index * 18 : index * 18);
            }

            Runnable spawnEnemy = () -> {
                if (stopped) {
                    return;
                }
                SpawnData spawnData = new SpawnData(startX, startY)
                        .put("enemyType", finalType)
                        .put("level", currentLevel)
                        .put("targetX", targetSlot.getX())
                        .put("targetY", targetSlot.getY())
                        .put("entryPath", path)
                        .put("squadId", squadIdFinal)
                        .put("entering", true);

                if (currentWaveType == WaveType.KAMIKAZE) {
                    spawnData.put("kamikaze", true);
                }
                if (currentWaveType == WaveType.BONUS) {
                    spawnData.put("bonus", true);
                }
                if (currentWaveType == WaveType.PEARL) {
                    spawnData.put("entrySpeed", 140.0);
                }
                if (finalType == EnemyComponent.EnemyType.MONSTER) {
                    int sprite = SpriteSheets.monsterTileFor(index);
                    spawnData.put("spriteIndex", sprite);
                    spawnData.put("canSplit", currentLevel >= GameVars.MONSTER_SPLIT_MIN_LEVEL
                            && (sprite == 0 || sprite == 2 || sprite == 4));
                }

                Entity enemy = spawn("enemy", spawnData);
                squad.addEnemy(enemy);
            };

            if (currentWaveType == WaveType.PEARL || index == 0) {
                spawnEnemy.run();
            } else {
                double delay = path.flight != null ? index * 0.30
                        : currentWaveType == WaveType.KAMIKAZE || currentWaveType == WaveType.BONUS
                        ? index * 0.28
                        : index * 0.42;
                runOnce(spawnEnemy, javafx.util.Duration.seconds(delay));
            }

            enemiesSpawned++;
        }
    }

    private EntryPath generateEntryPath() {
        int squadIndex = Math.max(0, activeSquads.size() - 1);
        return switch (currentWaveType) {
            case FIGHTERS, MIXED, BONUS, MONSTER -> mixedPath(squadIndex);
            case METEOR -> new EntryPath(getAppWidth() / 2.0, -50, EntryPath.Type.FROM_CENTER);
            case SPINNER -> EntryPath.flight(squadIndex % 2 == 0 ? FlightPaths.Kind.CIRCLE : FlightPaths.Kind.SPIRAL,
                    squadIndex % 2 == 1);
            case PINCER -> EntryPath.flight(FlightPaths.Kind.ARC, squadIndex % 2 == 1);
            case CASCADE -> EntryPath.flight(squadIndex % 3 == 2 ? FlightPaths.Kind.WAVE : FlightPaths.Kind.S_CURVE,
                    squadIndex % 2 == 1);
            case RUSH -> {
                FlightPaths.Kind[] k = {FlightPaths.Kind.HOOK, FlightPaths.Kind.LOOP, FlightPaths.Kind.ARC};
                yield EntryPath.flight(k[squadIndex % k.length], squadIndex % 2 == 1);
            }
            case PEARL -> pearlPath(squadIndex);
            case RANK -> squadIndex % 2 == 0
                    ? new EntryPath(getAppWidth() / 2.0, -50, EntryPath.Type.FROM_CENTER)
                    : new EntryPath(getAppWidth() / 2.0, -50, EntryPath.Type.FROM_TOP_SPLIT);
            case BOSS -> new EntryPath(getAppWidth() / 2.0, -80, EntryPath.Type.FROM_TOP_SPLIT);
            case KAMIKAZE -> {
                boolean fromLeft = squadIndex % 2 == 0;
                double y = 50 + (squadIndex % 3) * 30;
                yield fromLeft
                    ? new EntryPath(-50, y, EntryPath.Type.FROM_LEFT_CURVE)
                    : new EntryPath(getAppWidth() + 50, y, EntryPath.Type.FROM_RIGHT_CURVE);
            }
        };
    }

    private EntryPath pearlPath(int squadIndex) {
        double mid = getAppWidth() / 2.0;
        double playable = getAppWidth() - 2.0 * GameVars.RAIL_WIDTH;
        int pattern = Math.floorMod(currentLevel + squadIndex, 3);
        return switch (pattern) {
            case 0 -> new EntryPath(mid, getAppHeight() + 8, EntryPath.Type.FROM_BELOW, 46, true);
            case 1 -> new EntryPath(mid, getAppHeight() + 8, EntryPath.Type.FROM_BELOW, playable * 0.56, true);
            default -> new EntryPath(mid, -40, EntryPath.Type.FROM_CENTER, 46, true);
        };
    }

    private EntryPath mixedPath(int squadIndex) {
        // A partir del nivel 2 (y uno de cada dos grupos en el nivel 1): trayectorias al estilo Galaga.
        if (currentLevel >= 2 || squadIndex % 2 == 1) {
            FlightPaths.Kind[] kinds = FlightPaths.Kind.values();
            FlightPaths.Kind kind = kinds[Math.floorMod(currentLevel * 3 + squadIndex, kinds.length)];
            return EntryPath.flight(kind, squadIndex % 2 == 1);
        }
        return switch (Math.floorMod(squadIndex + currentLevel, 4)) {
            case 0 -> new EntryPath(-50, 100, EntryPath.Type.FROM_LEFT_CURVE);
            case 1 -> new EntryPath(getAppWidth() + 50, 100, EntryPath.Type.FROM_RIGHT_CURVE);
            case 2 -> new EntryPath(getAppWidth() / 2.0, -50, EntryPath.Type.FROM_CENTER);
            default -> {
                boolean left = squadIndex % 2 == 0;
                double x = left
                        ? GameVars.RAIL_WIDTH + 16
                        : getAppWidth() - GameVars.RAIL_WIDTH - 64;
                yield new EntryPath(x, getAppHeight() + 40, EntryPath.Type.FROM_BELOW);
            }
        };
    }

    private EnemyComponent.EnemyType determineEnemyType(int slotIndex) {
        if (currentWaveType == WaveType.BOSS && slotIndex == 0) {
            return EnemyComponent.EnemyType.BOSS;
        }

        if (currentWaveType == WaveType.BOSS) {
            return slotIndex % 3 == 0 ? EnemyComponent.EnemyType.FAST : EnemyComponent.EnemyType.BASIC;
        }

        if (currentWaveType == WaveType.RANK) {
            return EnemyComponent.EnemyType.TOUGH;
        }

        if (currentWaveType == WaveType.KAMIKAZE) {
            return slotIndex % 3 == 0 ? EnemyComponent.EnemyType.FAST : EnemyComponent.EnemyType.BASIC;
        }

        if (currentWaveType == WaveType.BONUS) {
            return slotIndex % 4 == 1 ? EnemyComponent.EnemyType.FAST : EnemyComponent.EnemyType.BASIC;
        }

        if (currentWaveType == WaveType.PEARL) {
            if (slotIndex % 5 == 0 && currentLevel >= 4) {
                return EnemyComponent.EnemyType.TOUGH;
            }
            if (slotIndex % 4 == 1) {
                return EnemyComponent.EnemyType.FAST;
            }
            return EnemyComponent.EnemyType.BASIC;
        }

        if (currentWaveType == WaveType.MONSTER) {
            return EnemyComponent.EnemyType.MONSTER;
        }

        if (currentWaveType == WaveType.METEOR) {
            return slotIndex % 3 == 1 ? EnemyComponent.EnemyType.FAST : EnemyComponent.EnemyType.BASIC;
        }

        if (currentWaveType == WaveType.MIXED) {
            if (slotIndex % 5 == 0 && currentLevel >= 2) {
                return EnemyComponent.EnemyType.TOUGH;
            }
            if (slotIndex % 3 == 1) {
                return EnemyComponent.EnemyType.FAST;
            }
            return EnemyComponent.EnemyType.BASIC;
        }

        if (currentLevel >= 3 && slotIndex == 0) {
            return EnemyComponent.EnemyType.TOUGH;
        }
        if (currentLevel >= 2 && slotIndex % 4 == 1) {
            return EnemyComponent.EnemyType.FAST;
        }

        return EnemyComponent.EnemyType.BASIC;
    }

    public void checkKillBonuses() {
        if (currentWaveType == WaveType.BONUS) {
            checkBonusPerfects();
        } else {
            checkSquadBonuses();
        }
    }

    public void checkSquadBonuses() {
        for (Squad squad : activeSquads) {
            if (squad.isFullyDestroyed() && squad.earnedComboBonus()) {
                int bonus = 500 * currentLevel;
                inc(GameVars.SCORE, bonus);
                inc(GameVars.SQUAD_COMBOS, 1);
                showComboBonus(bonus);
                squad.settled = true;
            }
        }
    }

    public void checkBonusPerfects() {
        for (Squad squad : activeSquads) {
            if (squad.bonusResolved) {
                continue;
            }
            if (squad.enemies.size() < squad.originalSize) {
                continue;
            }
            if (!squad.isFullyDestroyed()) {
                continue;
            }
            squad.bonusResolved = true;
            if (squad.escaped == 0) {
                bonusPerfectStreak++;
                inc(GameVars.BONUS_PERFECT_CLEARS, 1);
                int award = isBonusWaveFullyClean()
                        ? GameVars.BONUS_ROUND_PERFECT_SCORE
                        : GameVars.BONUS_PERFECT_BASE << Math.min(bonusPerfectStreak - 1, 6);
                inc(GameVars.SCORE, award);
                FXGL.<DeltaBladeApp>getAppCast().showBonusPerfect(award);
            } else {
                bonusPerfectStreak = 0;
            }
        }
    }

    private boolean isBonusWaveFullyClean() {
        if (currentWaveType != WaveType.BONUS || enemiesSpawned < totalEnemiesInWave) {
            return false;
        }
        if (geti(GameVars.ENEMIES_REMAINING) > 0 || activeSquads.isEmpty()) {
            return false;
        }
        for (Squad squad : activeSquads) {
            if (squad.escaped != 0 || !squad.isFullyDestroyed() || squad.enemies.size() < squad.originalSize) {
                return false;
            }
        }
        return true;
    }

    private void showComboBonus(int bonus) {
        FXGL.<DeltaBladeApp>getAppCast().showSquadCombo(bonus);
    }

    private int monsterKills;
    private boolean alienBossSpawned;
    private int monsterTotal;

    /** Oleada de monstruos: se vuelve true una vez, cuando han caído unos dos tercios de los monstruos. */
    public boolean noteMonsterKillAndCheckBoss() {
        if (currentWaveType != WaveType.MONSTER || alienBossSpawned) {
            return false;
        }
        monsterKills++;
        if (monsterKills >= Math.max(1, (int) Math.ceil(monsterTotal * 2.0 / 3.0))) {
            alienBossSpawned = true;
            return true;
        }
        return false;
    }

    public void markAlienBossSpawned() {
        alienBossSpawned = true;
    }

    public boolean alienBossThisWave() {
        return currentWaveType == WaveType.MONSTER && alienBossSpawned;
    }

    public static EntryPath bossEntry() {
        return new EntryPath(getAppWidth() / 2.0, -80, EntryPath.Type.FROM_TOP_SPLIT);
    }

    public void onEnemyDestroyed(int squadId, boolean wasEntering) {
        if (squadId >= 0 && squadId < activeSquads.size()) {
            activeSquads.get(squadId).onEnemyDestroyed(wasEntering);
        }
    }

    public void onEnemyEscaped(int squadId) {
        if (squadId >= 0 && squadId < activeSquads.size()) {
            activeSquads.get(squadId).escaped++;
        }
    }

    public void markSquadSettled(int squadId) {
        if (squadId >= 0 && squadId < activeSquads.size()) {
            activeSquads.get(squadId).markSettled();
        }
    }

    public boolean hasPendingSpawns() {
        return !stopped && enemiesSpawned < totalEnemiesInWave;
    }

    public boolean isWaveComplete() {
        return !stopped && enemiesSpawned >= totalEnemiesInWave && geti(GameVars.ENEMIES_REMAINING) <= 0;
    }

    public WaveType getCurrentWaveType() {
        return currentWaveType;
    }

    public static class EntryPath {
        public enum Type {
            FROM_LEFT_CURVE,
            FROM_RIGHT_CURVE,
            FROM_TOP_SPLIT,
            FROM_SIDE_SWOOP,
            FROM_CENTER,
            FROM_BELOW
        }

        public final double startX;
        public final double startY;
        public final Type type;
        public final double pairGap;
        public final boolean beadString;
        /** Trayectoria opcional (arco, rizo...). null = forma de entrada simple antigua. */
        public List<Point2D> flight;

        public static EntryPath flight(FlightPaths.Kind kind, boolean mirror) {
            List<Point2D> pts = FlightPaths.build(kind, mirror);
            Point2D first = pts.get(0);
            EntryPath p = new EntryPath(first.getX(), first.getY(), Type.FROM_CENTER);
            p.flight = pts;
            return p;
        }

        public EntryPath(double startX, double startY, Type type) {
            this(startX, startY, type, 0, false);
        }

        public EntryPath(double startX, double startY, Type type, double pairGap) {
            this(startX, startY, type, pairGap, false);
        }

        public EntryPath(double startX, double startY, Type type, double pairGap, boolean beadString) {
            this.startX = startX;
            this.startY = startY;
            this.type = type;
            this.pairGap = pairGap;
            this.beadString = beadString;
        }
    }
}
