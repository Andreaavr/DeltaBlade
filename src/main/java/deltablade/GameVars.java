package deltablade;

public final class GameVars {
    public static final String SCORE = "score";
    public static final String LIVES = "lives";
    public static final String LEVEL = "level";
    public static final String WEAPON_GRADE = "weaponGrade";
    public static final String AMMO_CAP = "ammoCap";
    public static final String ACTIVE_BULLETS = "activeBullets";
    public static final String ENEMIES_REMAINING = "enemiesRemaining";
    public static final String MONEY = "money";
    public static final String EXTRA_E = "extraE";
    public static final String EXTRA_X = "extraX";
    public static final String EXTRA_T = "extraT";
    public static final String EXTRA_R = "extraR";
    public static final String EXTRA_A = "extraA";
    public static final String AUTOFIRE = "autofire";
    public static final String EXTRA_LETTER_SPAWNED_THIS_WAVE = "extraLetterSpawnedThisWave";
    public static final String SQUAD_COMBOS = "squadCombos";
    public static final String COGNITIVE_WINS = "cognitiveWins";
    public static final String METEOR_WINS = "meteorWins";
    public static final String MAZE_WINS = "mazeWins";
    public static final String RANK = "rank";
    public static final String RANK_MASK = "rankMask";
    public static final String SHOTS_FIRED = "shotsFired";
    public static final String HITS_LANDED = "hitsLanded";
    public static final String SHOP_WEAPON = "shopWeapon";
    public static final String BOSS_KILLS = "bossKills";
    public static final String BONUS_PERFECT_CLEARS = "bonusPerfectClears";

    public static final int INITIAL_LIVES = 3;
    public static final int INITIAL_AMMO_CAP = 5;
    public static final int MAX_AMMO_CAP = 12;
    public static final int MAX_WEAPON_GRADE = 4;

    public static final int RAIL_WIDTH = 64;
    /** Y más baja para la patrulla lateral. Por debajo solo hay picados y kamikazes. */
    public static final double FORMATION_MAX_Y_RATIO = 0.38;
    /** FAST/TOUGH disparan en línea recta hasta esta oleada, para que el primer grupo sea fácil de leer. */
    public static final int ANGLED_FIRE_MIN_LEVEL = 5;
    public static final int MONSTER_WAVE_MIN_LEVEL = 6;
    public static final int MONSTER_SPLIT_MIN_LEVEL = 8;

    static final char[] EXTRA_LETTERS = {'E', 'X', 'T', 'R', 'A'};
    static final String[] EXTRA_VARS = {EXTRA_E, EXTRA_X, EXTRA_T, EXTRA_R, EXTRA_A};

    public static final double EXTRA_LETTER_DROP_CHANCE = 0.025;
    public static final double PICKUP_DROP_CHANCE = 0.03;
    public static final double AUTOFIRE_DROP_CHANCE = 0.01;
    public static final double MINIGAME_DROP_CHANCE = 0.01;
    public static final double TIME_PICKUP_DROP_CHANCE = 0.025;
    public static final double SKULL_DROP_CHANCE = 0.012;
    public static final double SHIELD_DROP_CHANCE = 0.02;
    public static final double SHIELD_DURATION = 10.0;
    public static final double SCOOP_DROP_CHANCE = 0.02;
    public static final int SCOOP_COUNT = 2;
    public static final double MISSILE_DROP_CHANCE = 0.02;
    public static final double MISSILE_SPEED = 130;
    public static final double MISSILE_TURN_RATE = 110;
    public static final double MISSILE_BLAST_RADIUS = 112;
    public static final int MISSILE_BLAST_HITS = 2;
    public static final double MISSILE_DOUBLE_TAP = 0.28;
    public static final int METEOR_HITS = 3;
    public static final double METEOR_FIRE_SCALE = 0.32;
    public static final double METEOR_MISSILE_GAP = 1.15;
    public static final int LEVEL_TIME_INITIAL = 30;
    public static final int LEVEL_TIME_PICKUP = 1;
    public static final int LEVEL_TIME_DEATH_PENALTY = 5;
    public static final int LEVEL_TIME_MIN = 20;
    /** Después de HURRY UP, tiempo de espera antes de que pueda aparecer el ovni. */
    public static final double HURRY_UFO_DELAY = 4.0;
    public static final double RANK_MARKER_DROP_CHANCE = 0.2;
    public static final int RANK_COLOR_COUNT = 6;
    public static final int RANK_UP_SCORE = 5000;
    public static final int BONUS_PERFECT_BASE = 2000;
    public static final int BONUS_ROUND_PERFECT_SCORE = 20000;
    public static final int SETTLEMENT_RANK = 5000;
    public static final int SETTLEMENT_COMBO = 5000;
    public static final int SETTLEMENT_METEOR = 3000;
    public static final int SETTLEMENT_COGNITIVE = 3000;
    public static final int SETTLEMENT_MAZE = 4000;
    public static final int SETTLEMENT_WAVE = 1000;
    public static final int SETTLEMENT_CASH_MULT = 10;
    public static final int SETTLEMENT_PERFECT = 2500;
    public static final int SETTLEMENT_ACCURACY_CAP = 100_000;

    public static final double COIN_WHITE_DROP_CHANCE = 0.08;
    public static final double COIN_GREEN_DROP_CHANCE = 0.025;
    public static final double COIN_BLUE_DROP_CHANCE = 0.008;
    public static final double COIN_VIOLET_DROP_CHANCE = 0.005;

    public static final int BOSS_BASE_HEALTH = 10;
    public static final int BOSS_HEALTH_PER_CYCLE = 2;
    public static final int BOSS_SCORE = 1000;
    /** Cada cuántos niveles aparece un jefe grande. */
    public static final int BOSS_EVERY = 5;
    /** Cada cuántos niveles llegan el salto y la tienda (como antes: 10, 20, 30...). */
    public static final int WARP_EVERY = 10;
    /** Jefe = imagen de enemigo normal, pero mucho más grande. */
    public static final int BOSS_DRAW_SIZE = 160;
    public static final int BOSS_HP_BASE = 25;
    public static final int BOSS_HP_PER_BOSS = 20;
    public static final int BOSS_KILL_SCORE = 5000;
    public static final int BOSS_KILL_MONEY = 300;

    public static int bossNumber(int level) {
        return Math.max(1, level / BOSS_EVERY);
    }

    public static final double CURSE_REVERSE_SECONDS = 8.0;
    public static final double CURSE_JAM_SECONDS = 5.0;

    private GameVars() {}
}
