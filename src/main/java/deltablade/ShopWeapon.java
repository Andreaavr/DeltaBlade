package deltablade;

/**
 * Armas especiales exclusivas de la tienda. Ordinal 0 = NONE (balas según el grado).
 * Los precios y las condiciones de desbloqueo son constantes ajustables.
 */
public enum ShopWeapon {
    NONE(0, "", "", "", ColorTag.CYAN),
    NEEDLE_LASER(1200, "NEEDLE LASER", "BEAM",
            "Thin laser. No ammo needed, overheats fast.",
            ColorTag.CYAN),
    TWIN_BEAM(2800, "TWIN BEAM", "TWIN BEAM",
            "Two parallel beams. Clears formations.",
            ColorTag.AQUA),
    PLASMA_ORB(5000, "PLASMA ORB", "PLASMA",
            "Heavy plasma orb with splash and pierce. Tough-killer.",
            ColorTag.VIOLET),
    ARC_LANCE(8000, "ARC LANCE", "CHAIN",
            "Chain lightning: jumps between enemies, wipes squads.",
            ColorTag.GOLD),
    SWEEP_LASER(12000, "SWEEP LASER", "SWEEP",
            "Sweeping wide beam. Boss and wave finisher.",
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
            case TWIN_BEAM -> "RANK 3  OR  WAVE 8";
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
