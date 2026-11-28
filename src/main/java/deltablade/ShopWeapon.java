package deltablade;

/**
 * Armas especiales exclusivas de la tienda. Ordinal 0 = NONE (balas según el grado).
 * Los precios y las condiciones de desbloqueo son constantes ajustables.
 */
public enum ShopWeapon {
    NONE(0, ColorTag.CYAN),
    NEEDLE_LASER(1200, ColorTag.CYAN),
    TWIN_BEAM(2800, ColorTag.AQUA),
    PLASMA_ORB(5000, ColorTag.VIOLET),
    ARC_LANCE(8000, ColorTag.GOLD),
    SWEEP_LASER(12000, ColorTag.ORANGE);

    /**
     * Color con el que se muestra cada arma en la tienda.
     */
    public enum ColorTag { CYAN, AQUA, VIOLET, GOLD, ORANGE }

    private final int price;
    private final ColorTag colorTag;

    ShopWeapon(int price, ColorTag colorTag) {
        this.price = price;
        this.colorTag = colorTag;
    }

    /**
     * Devuelve el precio del arma.
     *
     * @return precio en dólares del juego
     */
    public int price() {
        return price;
    }

    /**
     * Nombre del arma en el idioma actual.
     *
     * @return el nombre traducido, o una cadena vacía para NONE
     */
    public String displayName() {
        return text("name");
    }

    /**
     * Etiqueta corta del tipo de arma en el idioma actual.
     *
     * @return la etiqueta traducida, o una cadena vacía para NONE
     */
    public String typeTag() {
        return text("tag");
    }

    /**
     * Descripción del arma en el idioma actual.
     *
     * @return la descripción traducida, o una cadena vacía para NONE
     */
    public String description() {
        return text("desc");
    }

    private String text(String field) {
        return this == NONE ? "" : I18n.tr("weapon." + name() + "." + field);
    }

    /**
     * Devuelve el color con el que se muestra el arma en la tienda.
     *
     * @return color del arma
     */
    public ColorTag colorTag() {
        return colorTag;
    }

    /**
     * Busca un arma por su identificador numérico.
     *
     * @param id identificador del arma (su posición en el enumerado)
     * @return arma correspondiente, o {@link #NONE} si el identificador no es válido
     */
    public static ShopWeapon fromId(int id) {
        ShopWeapon[] values = values();
        if (id < 0 || id >= values.length) {
            return NONE;
        }
        return values[id];
    }

    /**
     * Indica si el arma está desbloqueada según el progreso del jugador.
     *
     * @param rank rango actual del jugador
     * @param wave oleada más alta alcanzada
     * @param meteorWins victorias en el minijuego de meteoritos
     * @param cognitiveWins victorias en el test cognitivo
     * @param squadCombos escuadrones destruidos antes de llegar a la formación
     * @param bossKills jefes derrotados
     * @return {@code true} si el arma se puede comprar
     */
    public boolean isUnlocked(int rank, int wave, int meteorWins, int cognitiveWins,
                              int squadCombos, int bossKills) {
        return switch (this) {
            case NONE -> true;
            case NEEDLE_LASER -> true;
            case TWIN_BEAM -> rank >= 3 || wave >= 8;
            case PLASMA_ORB -> rank >= 6 && meteorWins >= 1 && cognitiveWins >= 1;
            case ARC_LANCE -> rank >= 10 && squadCombos >= 15;
            case SWEEP_LASER -> rank >= 15 && bossKills >= 1 && wave >= 20;
        };
    }

    /**
     * Devuelve la pista que explica cómo desbloquear el arma.
     *
     * @return texto de la pista, o una cadena vacía si está disponible desde el principio
     */
    public String unlockHint() {
        return switch (this) {
            case NONE, NEEDLE_LASER -> "";
            default -> text("hint");
        };
    }

    /**
     * Devuelve las armas que se venden en la tienda, de la más cara a la más barata.
     *
     * @return armas de la tienda
     */
    public static ShopWeapon[] shopItems() {
        return new ShopWeapon[] {
                SWEEP_LASER, ARC_LANCE, PLASMA_ORB, TWIN_BEAM, NEEDLE_LASER
        };
    }
}
