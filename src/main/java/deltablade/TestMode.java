package deltablade;

import javafx.scene.input.KeyCode;

import java.util.Optional;

/**
 * Códigos de depuración: pulsa tres veces la misma tecla numérica
 * (000 misil, 111 desafío de meteoritos, 222 cognitivo, 333 escudo, 444 salto, 555 ovni, 666 bonus, 777 ataque, 888 scoop, 999 enjambre).
 * Secuencia 890 = truco de dinero. Secuencia 456 = minijuego del laberinto.
 * 789 minijuego Alien Endurance. 357 oleada de monstruos con jefe alienígena desde el principio.
 * Pruebas de oleadas: 505 jefe inmediato, 147 torbellino, 258 tenaza, 369 cascada, 159 asalto.
 */
public final class TestMode {

    /**
     * Elemento que hace aparecer un código de depuración.
     *
     * @param spawnName nombre de la entidad u oleada que se genera
     * @param banner mensaje que se muestra al activarlo
     */
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

    /**
     * Procesa una tecla pulsada y comprueba si completa algún código de depuración.
     *
     * @param code tecla pulsada
     * @return el elemento que hay que generar, o vacío si aún no se ha completado ningún código
     */
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
            return Optional.of(new Drop(null, I18n.tr("test.prefix", "" + digit + digit + digit + "  ?")));
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
                return Optional.of(new Drop("MONEY", I18n.tr("test.prefix", "+$15000")));
            }
            return Optional.empty();
        }
        moneySeq = (digit == MONEY_CODE[0]) ? 1 : 0;
        return Optional.empty();
    }


    /** Códigos de prueba de oleadas: secuencia de dígitos → tipo de oleada. */
    private static final int[][] WAVE_CODES = {
            {5, 0, 5}, {1, 4, 7}, {2, 5, 8}, {3, 6, 9}, {1, 5, 9}, {7, 8, 9}, {3, 5, 7}
    };
    private static final String[] WAVE_NAMES = {"BOSS", "SPINNER", "PINCER", "CASCADE", "RUSH", "ALIEN_ENDURANCE", "MONSTER_BOSS"};
    private static final int[] WAVE_SEQ = new int[WAVE_CODES.length];
    private static final long[] WAVE_SEQ_AT = new long[WAVE_CODES.length];

    private static Optional<Drop> feedWaveSequence(int i, int digit, long now) {
        int[] code = WAVE_CODES[i];
        if (now - WAVE_SEQ_AT[i] > TIMEOUT_MS) {
            WAVE_SEQ[i] = 0;
        }
        WAVE_SEQ_AT[i] = now;
        if (digit == code[WAVE_SEQ[i]]) {
            WAVE_SEQ[i]++;
            if (WAVE_SEQ[i] >= code.length) {
                WAVE_SEQ[i] = 0;
                String n = WAVE_NAMES[i];
                String spawnName = (n.startsWith("ALIEN") || n.startsWith("MONSTER_")) ? n : "WAVE:" + n;
                return Optional.of(new Drop(spawnName, I18n.tr("test.prefix", I18n.tr("test.wave." + i))));
            }
            return Optional.empty();
        }
        WAVE_SEQ[i] = (digit == code[0]) ? 1 : 0;
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
                return Optional.of(new Drop("MAZE_ESCAPE", I18n.tr("test.prefix", I18n.tr("test.maze"))));
            }
            return Optional.empty();
        }
        mazeSeq = (digit == MAZE_CODE[0]) ? 1 : 0;
        return Optional.empty();
    }

    /**
     * Devuelve el elemento que corresponde a un código de tres dígitos iguales.
     *
     * @param index dígito repetido (0 a 9)
     * @return elemento que hay que generar
     */
    public static Drop dropFor(int index) {
        return switch (index) {
            case 0 -> new Drop("missilePickup", I18n.tr("test.drop", I18n.tr("test.missile")));
            case 1 -> new Drop("METEOR_CHALLENGE", I18n.tr("test.prefix", I18n.tr("wave.meteor")));
            case 2 -> new Drop("cognitivePickup", I18n.tr("test.drop", I18n.tr("test.cognitive")));
            case 3 -> new Drop("shieldPickup", I18n.tr("test.drop", I18n.tr("test.shield")));
            case 4 -> new Drop("WARP", I18n.tr("test.prefix", I18n.tr("banner.warp")));
            case 5 -> new Drop("UFO", I18n.tr("test.prefix", I18n.tr("banner.ufo")));
            case 6 -> new Drop("BONUS", I18n.tr("test.prefix", I18n.tr("test.bonus")));
            case 7 -> new Drop("PEARL", I18n.tr("test.prefix", I18n.tr("test.attack")));
            case 8 -> new Drop("SCOOP", I18n.tr("test.prefix", I18n.tr("banner.scoop")));
            case 9 -> new Drop("MONSTER", I18n.tr("test.prefix", I18n.tr("test.swarm")));
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
