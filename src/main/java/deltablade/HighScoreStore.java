package deltablade;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;

/**
 * Top 10 de récords más el último nombre usado (1-10 letras). Se conserva al reiniciar gracias a Preferences.
 */
public final class HighScoreStore {

    public static final int MAX_ENTRIES = 10;
    public static final int NAME_LENGTH = 10;
    public static final String DEFAULT_NAME = "AAA";

    /**
     * Fila de la tabla de récords.
     *
     * @param name nombre del jugador
     * @param score puntuación conseguida
     * @param wave oleada alcanzada
     */
    public record Entry(String name, int score, int wave) {}

    private static final Preferences PREFS = Preferences.userNodeForPackage(HighScoreStore.class);
    private static final String KEY_TABLE = "highScores";
    private static final String KEY_LAST_NAME = "highScoreLastName";

    private HighScoreStore() {}

    /**
     * Devuelve la tabla local de récords, ordenada de mayor a menor puntuación.
     *
     * @return entradas de la tabla
     */
    public static List<Entry> entries() {
        return parse(PREFS.get(KEY_TABLE, ""));
    }

    /**
     * Devuelve el último nombre que usó el jugador, para proponerlo de nuevo.
     *
     * @return último nombre usado, o el nombre por defecto
     */
    public static String lastName() {
        return sanitizeNameOrDefault(PREFS.get(KEY_LAST_NAME, DEFAULT_NAME));
    }

    /**
     * Guarda el último nombre que usó el jugador.
     *
     * @param name nombre del jugador; se limpia antes de guardarlo
     */
    public static void setLastName(String name) {
        PREFS.put(KEY_LAST_NAME, sanitizeNameOrDefault(name));
    }

    /**
     * Indica si una puntuación entra en la tabla local de récords.
     *
     * @param score puntuación conseguida
     * @return {@code true} si entra en la tabla
     */
    public static boolean qualifies(int score) {
        return qualifies(score, entries());
    }

    /**
     * Indica si una puntuación entra en una tabla de récords concreta.
     *
     * @param score puntuación conseguida
     * @param table tabla de récords; si es {@code null} o no está llena, cualquier puntuación entra
     * @return {@code true} si entra en la tabla
     */
    public static boolean qualifies(int score, List<Entry> table) {
        if (table == null || table.size() < MAX_ENTRIES) {
            return true;
        }
        return score > table.getLast().score();
    }

    /**
     * Inserta una fila nueva y mantiene la lista ordenada y limitada.
     *
     * @param name nombre del jugador; se limpia antes de guardarlo
     * @param score puntuación conseguida (los negativos se guardan como 0)
     * @param wave oleada alcanzada (mínimo 1)
     * @return posición (empezando en 0) de la nueva entrada, o -1 si no entra en la tabla
     */
    public static int insert(String name, int score, int wave) {
        String clean = sanitizeNameOrDefault(name);
        List<Entry> table = new ArrayList<>(entries());
        table.add(new Entry(clean, Math.max(0, score), Math.max(1, wave)));
        table.sort(Comparator
                .comparingInt(Entry::score).reversed()
                .thenComparing(Comparator.comparingInt(Entry::wave).reversed()));
        if (table.size() > MAX_ENTRIES) {
            table = new ArrayList<>(table.subList(0, MAX_ENTRIES));
        }
        int rank = -1;
        for (int i = 0; i < table.size(); i++) {
            Entry entry = table.get(i);
            if (entry.name().equals(clean) && entry.score() == score && entry.wave() == Math.max(1, wave)) {
                rank = i;
                break;
            }
        }
        PREFS.put(KEY_TABLE, serialize(table));
        setLastName(clean);
        return rank;
    }

    /**
     * Limpia un nombre para la tabla de récords: lo pasa a mayúsculas y deja solo letras de la A a la Z
     * y espacios, hasta el máximo permitido.
     *
     * @param raw nombre tal como lo escribió el jugador
     * @return nombre limpio, que puede quedar vacío
     */
    public static String sanitizeName(String raw) {
        StringBuilder letters = new StringBuilder();
        if (raw != null) {
            for (int i = 0; i < raw.length(); i++) {
                char c = Character.toUpperCase(raw.charAt(i));
                if ((c >= 'A' && c <= 'Z') || c == ' ') {
                    letters.append(c);
                }
                if (letters.length() == NAME_LENGTH) {
                    break;
                }
            }
        }
        return letters.toString();
    }

    /**
     * Limpia un nombre para la tabla de récords y usa el nombre por defecto si queda vacío.
     *
     * @param raw nombre tal como lo escribió el jugador
     * @return nombre limpio, nunca vacío
     */
    public static String sanitizeNameOrDefault(String raw) {
        String clean = sanitizeName(raw);
        return clean.isEmpty() ? DEFAULT_NAME : clean;
    }

    private static List<Entry> parse(String packed) {
        List<Entry> table = new ArrayList<>();
        if (packed == null || packed.isBlank()) {
            return table;
        }
        for (String row : packed.split("\\|")) {
            String[] parts = row.split(":");
            if (parts.length != 3) {
                continue;
            }
            try {
                table.add(new Entry(
                        sanitizeNameOrDefault(parts[0]),
                        Integer.parseInt(parts[1]),
                        Math.max(1, Integer.parseInt(parts[2]))));
            } catch (NumberFormatException ignored) {
                // se salta la fila corrupta
            }
        }
        table.sort(Comparator
                .comparingInt(Entry::score).reversed()
                .thenComparing(Comparator.comparingInt(Entry::wave).reversed()));
        if (table.size() > MAX_ENTRIES) {
            return List.copyOf(table.subList(0, MAX_ENTRIES));
        }
        return List.copyOf(table);
    }

    private static String serialize(List<Entry> table) {
        StringBuilder out = new StringBuilder();
        for (Entry entry : table) {
            if (!out.isEmpty()) {
                out.append('|');
            }
            out.append(entry.name()).append(':').append(entry.score()).append(':').append(entry.wave());
        }
        return out.toString();
    }

    /**
     * Da formato a una puntuación con seis cifras (por ejemplo, 000925).
     *
     * @param score puntuación (los negativos se muestran como 0)
     * @return puntuación con formato
     */
    public static String formatScore(int score) {
        return String.format(Locale.ROOT, "%06d", Math.max(0, score));
    }
}
