package deltablade;

import javafx.scene.input.KeyCode;

import java.util.Optional;

/**
 * Debug codes: tap the same number key three times
 * (000 Missile, 111 Meteor Challenge, 222 Cognitive, 333 Shield, 444 Warp, 555 UFO, 666 Bonus, 777 Attack, 888 Scoop, 999 Swarm).
 * Sequence 890 = Money cheat. Sequence 456 = Maze Escape minigame.
 * 789 Alien Endurance minigame.
 * Wellen-Tests: 505 Boss sofort, 147 Kreisel, 258 Zange, 369 Wasserfall, 159 Sturm.
 */
public final class TestMode {

    public record Drop(String spawnName, String banner) {}

    private static final long TIMEOUT_MS = 900;
    private static final int TAPS = 3;
    private static final int[] MONEY_CODE = {8, 9, 0};
    private static final int[] MAZE_CODE = {4, 5, 6};

    private static int lastDigit;
    private static int count;
    private static long lastAt;

    private static int moneySeq;
    private static long moneySeqAt;
    private static int mazeSeq;
    private static long mazeSeqAt;

    private TestMode() {}

    public static Optional<Drop> feed(KeyCode code) {
        Integer digit = toDigit(code);
        if (digit == null) {
            return Optional.empty();
        }
        long now = System.currentTimeMillis();
        if (digit == lastDigit && now - lastAt < 40) {
            return Optional.empty();
        }

        Optional<Drop> money = feedMoneySequence(digit, now);
        if (money.isPresent()) {
            lastDigit = digit;
            lastAt = now;
            count = 0;
            return money;
        }
        for (int i = 0; i < WAVE_CODES.length; i++) {
            Optional<Drop> w = feedWaveSequence(i, digit, now);
            if (w.isPresent()) {
                lastDigit = digit;
                lastAt = now;
                count = 0;
                return w;
            }
        }
        Optional<Drop> maze = feedMazeSequence(digit, now);
        if (maze.isPresent()) {
            lastDigit = digit;
            lastAt = now;
            count = 0;
            return maze;
        }

        if (digit != lastDigit || now - lastAt > TIMEOUT_MS) {
            lastDigit = digit;
            count = 1;
        } else {
            count++;
        }
        lastAt = now;
        if (count < TAPS) {
            return Optional.empty();
        }
        count = 0;
        Drop drop = dropFor(digit);
        if (drop == null) {
            return Optional.of(new Drop(null, "TEST  " + digit + digit + digit + "  ?"));
        }
        return Optional.of(drop);
    }

    private static Optional<Drop> feedMoneySequence(int digit, long now) {
        if (now - moneySeqAt > TIMEOUT_MS) {
            moneySeq = 0;
        }
        moneySeqAt = now;
        if (digit == MONEY_CODE[moneySeq]) {
            moneySeq++;
            if (moneySeq >= MONEY_CODE.length) {
                moneySeq = 0;
                return Optional.of(new Drop("MONEY", "TEST  +$15000"));
            }
            return Optional.empty();
        }
        moneySeq = (digit == MONEY_CODE[0]) ? 1 : 0;
        return Optional.empty();
    }


    /** Wellen-Test-Codes: Ziffernfolge -> Wellen-Art. */
    private static final int[][] WAVE_CODES = {
            {5, 0, 5}, {1, 4, 7}, {2, 5, 8}, {3, 6, 9}, {1, 5, 9}, {7, 8, 9}
    };
    private static final String[] WAVE_NAMES = {"BOSS", "KREISEL", "ZANGE", "WASSERFALL", "STURM", "ALIEN_ENDURANCE"};
    private static final String[] WAVE_LABELS = {"BOSS", "SPINNER", "PINCER", "CASCADE", "RUSH", "ALIEN ENDURANCE"};
    private static final int[] waveSeq = new int[WAVE_CODES.length];
    private static final long[] waveSeqAt = new long[WAVE_CODES.length];

    private static Optional<Drop> feedWaveSequence(int i, int digit, long now) {
        int[] code = WAVE_CODES[i];
        if (now - waveSeqAt[i] > TIMEOUT_MS) {
            waveSeq[i] = 0;
        }
        waveSeqAt[i] = now;
        if (digit == code[waveSeq[i]]) {
            waveSeq[i]++;
            if (waveSeq[i] >= code.length) {
                waveSeq[i] = 0;
                String n = WAVE_NAMES[i];
                return Optional.of(new Drop(n.startsWith("ALIEN") ? n : "WAVE:" + n, "TEST  " + WAVE_LABELS[i]));
            }
            return Optional.empty();
        }
        waveSeq[i] = (digit == code[0]) ? 1 : 0;
        return Optional.empty();
    }

    private static Optional<Drop> feedMazeSequence(int digit, long now) {
        if (now - mazeSeqAt > TIMEOUT_MS) {
            mazeSeq = 0;
        }
        mazeSeqAt = now;
        if (digit == MAZE_CODE[mazeSeq]) {
            mazeSeq++;
            if (mazeSeq >= MAZE_CODE.length) {
                mazeSeq = 0;
                return Optional.of(new Drop("MAZE_ESCAPE", "TEST  MAZE ESCAPE"));
            }
            return Optional.empty();
        }
        mazeSeq = (digit == MAZE_CODE[0]) ? 1 : 0;
        return Optional.empty();
    }

    public static Drop dropFor(int index) {
        return switch (index) {
            case 0 -> new Drop("missilePickup", "TEST DROP  MISSILE");
            case 1 -> new Drop("METEOR_CHALLENGE", "TEST  METEOR CHALLENGE");
            case 2 -> new Drop("cognitivePickup", "TEST DROP  COGNITIVE");
            case 3 -> new Drop("shieldPickup", "TEST DROP  SHIELD");
            case 4 -> new Drop("WARP", "TEST  WARP");
            case 5 -> new Drop("UFO", "TEST  UFO");
            case 6 -> new Drop("BONUS", "TEST  BONUS");
            case 7 -> new Drop("PEARL", "TEST  ATTACK");
            case 8 -> new Drop("SCOOP", "TEST  SCOOP");
            case 9 -> new Drop("MONSTER", "TEST  SWARM");
            default -> null;
        };
    }

    private static Integer toDigit(KeyCode code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case DIGIT0, NUMPAD0 -> 0;
            case DIGIT1, NUMPAD1 -> 1;
            case DIGIT2, NUMPAD2 -> 2;
            case DIGIT3, NUMPAD3 -> 3;
            case DIGIT4, NUMPAD4 -> 4;
            case DIGIT5, NUMPAD5 -> 5;
            case DIGIT6, NUMPAD6 -> 6;
            case DIGIT7, NUMPAD7 -> 7;
            case DIGIT8, NUMPAD8 -> 8;
            case DIGIT9, NUMPAD9 -> 9;
            default -> null;
        };
    }
}
