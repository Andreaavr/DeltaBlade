package deltablade;

/**
 * Shop-exclusive special weapons. Ordinal 0 = NONE (grade bullets).
 * Prices and unlock gates are tunable constants.
 */
public enum ShopWeapon {
    NONE(0, "", "", "", ColorTag.CYAN),
    NEEDLE_LASER(1200, "NEEDLE LASER", "DAUERSTRAHL",
            "Schmaler Hochleistungs-Laser. Ammo-frei, starker Tick-Schaden, kurzes Heat.",
            ColorTag.CYAN),
    TWIN_BEAM(2800, "TWIN BEAM", "DOPPELSTRAHL",
            "Zwei parallele Beams. Formation-Clear klar ueber Grade 4.",
            ColorTag.AQUA),
    PLASMA_ORB(5000, "PLASMA ORB", "PLASMA",
            "Schwere Plasma-Kugel mit Splash und Pierce. TOUGH-Killer.",
            ColorTag.VIOLET),
    ARC_LANCE(8000, "ARC LANCE", "BLITZ",
            "Kettenblitz: springt zwischen Gegnern und wipt Squads.",
            ColorTag.GOLD),
    SWEEP_LASER(12000, "SWEEP LASER", "SWEEP",
            "Schwenkender Breitstrahl. Boss- und Clear-Finisher.",
            ColorTag.ORANGE);

    public enum ColorTag { CYAN, AQUA, VIOLET, GOLD, ORANGE }

    private final int price;
    private final String displayName;
    private final String typeTag;
    private final String description;
    private final ColorTag colorTag;

    ShopWeapon(int price, String displayName, String typeTag, String description, ColorTag colorTag) {
        this.price = price;
        this.displayName = displayName;
        this.typeTag = typeTag;
        this.description = description;
        this.colorTag = colorTag;
    }

    public int price() {
        return price;
    }

    public String displayName() {
        return displayName;
    }

    public String typeTag() {
        return typeTag;
    }

    public String description() {
        return description;
    }

    public ColorTag colorTag() {
        return colorTag;
    }

    public static ShopWeapon fromId(int id) {
        ShopWeapon[] values = values();
        if (id < 0 || id >= values.length) {
            return NONE;
        }
        return values[id];
    }

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

    public String unlockHint() {
        return switch (this) {
            case NONE, NEEDLE_LASER -> "";
            case TWIN_BEAM -> "RANK 3  ODER  WAVE 8";
            case PLASMA_ORB -> "RANK 6  +  METEOR  +  COGNITIVE";
            case ARC_LANCE -> "RANK 10  +  15 COMBOS";
            case SWEEP_LASER -> "RANK 15  +  BOSS  +  WAVE 20";
        };
    }

    public static ShopWeapon[] shopItems() {
        return new ShopWeapon[] {
                SWEEP_LASER, ARC_LANCE, PLASMA_ORB, TWIN_BEAM, NEEDLE_LASER
        };
    }
}
