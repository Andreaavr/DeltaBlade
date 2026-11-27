package deltablade;

/**
 * Todos los objetos que se pueden comprar en la tienda, ordenados del más caro al más barato en {@link #catalog()}.
 */
public enum ShopItem {
    SWEEP_LASER(12_000, Kind.WEAPON, ShopWeapon.SWEEP_LASER, 0, ColorTag.ORANGE),
    EXTRA_LIFE(10_000, Kind.CONSUMABLE, null, 0, ColorTag.PINK),
    ARC_LANCE(8_000, Kind.WEAPON, ShopWeapon.ARC_LANCE, 0, ColorTag.GOLD),
    PLASMA_ORB(5_000, Kind.WEAPON, ShopWeapon.PLASMA_ORB, 0, ColorTag.VIOLET),
    TWIN_BEAM(2_800, Kind.WEAPON, ShopWeapon.TWIN_BEAM, 0, ColorTag.AQUA),
    QUAD(2_200, Kind.GRADE, null, 4, ColorTag.GOLD),
    TRIPLE(1_400, Kind.GRADE, null, 3, ColorTag.GOLD),
    NEEDLE_LASER(1_200, Kind.WEAPON, ShopWeapon.NEEDLE_LASER, 0, ColorTag.CYAN),
    AUTOFIRE(800, Kind.AUTOFIRE, null, 0, ColorTag.ORANGE),
    DOUBLE(700, Kind.GRADE, null, 2, ColorTag.GOLD),
    EXTRA_TIME(250, Kind.CONSUMABLE, null, 0, ColorTag.LIME),
    EXTRA_AMMO(200, Kind.CONSUMABLE, null, 0, ColorTag.CYAN);

    public enum Kind { WEAPON, CONSUMABLE, GRADE, AUTOFIRE }

    public enum ColorTag { CYAN, AQUA, VIOLET, GOLD, ORANGE, PINK, LIME }

    private final int price;
    private final Kind kind;
    private final ShopWeapon weapon;
    private final int targetGrade;
    private final ColorTag colorTag;

    ShopItem(int price, Kind kind, ShopWeapon weapon, int targetGrade, ColorTag colorTag) {
        this.price = price;
        this.kind = kind;
        this.weapon = weapon;
        this.targetGrade = targetGrade;
        this.colorTag = colorTag;
    }

    public int price() {
        return price;
    }

    /**
     * Nombre del objeto en el idioma actual. Las armas usan el nombre de su {@link ShopWeapon}.
     *
     * @return el nombre traducido
     */
    public String displayName() {
        return weapon != null ? weapon.displayName() : I18n.tr("item." + name() + ".name");
    }

    /**
     * Etiqueta corta del tipo de objeto en el idioma actual.
     *
     * @return la etiqueta traducida
     */
    public String typeTag() {
        return weapon != null ? weapon.typeTag() : I18n.tr("item." + name() + ".tag");
    }

    /**
     * Descripción del objeto en el idioma actual.
     *
     * @return la descripción traducida
     */
    public String description() {
        return weapon != null ? weapon.description() : I18n.tr("item." + name() + ".desc");
    }

    public Kind kind() {
        return kind;
    }

    public ShopWeapon weapon() {
        return weapon;
    }

    public int targetGrade() {
        return targetGrade;
    }

    public ColorTag colorTag() {
        return colorTag;
    }

    /** Orden del catálogo: primero los más caros. */
    public static ShopItem[] catalog() {
        return values();
    }

    public boolean isUnlocked(int rank, int wave, int meteorWins, int cognitiveWins,
                              int squadCombos, int bossKills) {
        if (kind != Kind.WEAPON || weapon == null) {
            return true;
        }
        return weapon.isUnlocked(rank, wave, meteorWins, cognitiveWins, squadCombos, bossKills);
    }

    public String unlockHint() {
        if (kind != Kind.WEAPON || weapon == null) {
            return "";
        }
        return weapon.unlockHint();
    }
}
