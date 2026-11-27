package deltablade;

/**
 * Rangos de estilo naval, que se muestran tras completar cada serie de marcadores de rango.
 * El índice 0 es el rango inicial; el 1 es el primer ascenso.
 */
public final class RankNames {

    /** Número de rangos con nombre propio (claves rank.title.0 a rank.title.23 en los ficheros i18n). */
    static final int TITLE_COUNT = 24;

    private RankNames() {}

    /**
     * Nombre completo del rango en el idioma actual.
     * Por encima del último rango se añade un número romano (II, III...).
     *
     * @param rank índice del rango (los negativos se tratan como 0)
     * @return el nombre del rango traducido
     */
    public static String title(int rank) {
        int index = Math.max(0, rank);
        if (index < TITLE_COUNT) {
            return I18n.tr("rank.title." + index);
        }
        int extra = index - TITLE_COUNT + 1;
        return I18n.tr("rank.title." + (TITLE_COUNT - 1)) + " " + toRoman(extra + 1);
    }

    /**
     * Nombre abreviado del rango para el marcador lateral.
     *
     * @param rank índice del rango (los negativos se tratan como 0)
     * @return el nombre corto traducido
     */
    public static String shortTitle(int rank) {
        int index = Math.max(0, rank);
        if (index < TITLE_COUNT) {
            return I18n.tr("rank.short." + index);
        }
        return title(rank);
    }

    /**
     * Texto del aviso que se muestra al subir de rango.
     *
     * @param rank índice del rango
     * @return el aviso traducido, por ejemplo "RANGO  RECLUTA"
     */
    public static String banner(int rank) {
        return I18n.tr("rank.banner", title(rank));
    }

    private static String toRoman(int value) {
        return switch (value) {
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(value);
        };
    }
}
