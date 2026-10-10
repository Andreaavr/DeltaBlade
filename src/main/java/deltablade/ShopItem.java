package deltablade;

/**
 * All buyable shop entries, listed expensive-first in {@link #catalog()}.
 */
public enum ShopItem {
    SWEEP_LASER(12_000, "SWEEP LASER", "SWEEP",
            "Sweeping wide beam. Boss and wave finisher.",
            Kind.WEAPON, ShopWeapon.SWEEP_LASER, 0, ColorTag.ORANGE),
    EXTRA_LIFE(10_000, "EXTRA LIFE", "EXTRA",
            "+1 life. Can be bought again.",
            Kind.CONSUMABLE, null, 0, ColorTag.PINK),
    ARC_LANCE(8_000, "ARC LANCE", "CHAIN",
            "Chain lightning: jumps between enemies, wipes squads.",
            Kind.WEAPON, ShopWeapon.ARC_LANCE, 0, ColorTag.GOLD),
    PLASMA_ORB(5_000, "PLASMA ORB", "PLASMA",
            "Heavy plasma orb with splash and pierce. Tough-killer.",
            Kind.WEAPON, ShopWeapon.PLASMA_ORB, 0, ColorTag.VIOLET),
    TWIN_BEAM(2_800, "TWIN BEAM", "TWIN BEAM",
            "Two parallel beams. Clears formations.",
            Kind.WEAPON, ShopWeapon.TWIN_BEAM, 0, ColorTag.AQUA),
    QUAD(2_200, "QUAD SHOT", "WEAPON",
            "Quad shot. Sets main gun to grade 4.",
            Kind.GRADE, null, 4, ColorTag.GOLD),
    TRIPLE(1_400, "TRIPLE SHOT", "WEAPON",
            "Triple shot. Sets main gun to grade 3.",
            Kind.GRADE, null, 3, ColorTag.GOLD),
    NEEDLE_LASER(1_200, "NEEDLE LASER", "BEAM",
            "Thin laser. No ammo needed, overheats fast.",
            Kind.WEAPON, ShopWeapon.NEEDLE_LASER, 0, ColorTag.CYAN),
    AUTOFIRE(800, "AUTOFIRE", "EXTRA",
            "Hold fire to auto-repeat. Lost when hit.",
            Kind.AUTOFIRE, null, 0, ColorTag.ORANGE),
    DOUBLE(700, "DOUBLE SHOT", "WEAPON",
            "Double shot. Sets main gun to grade 2.",
            Kind.GRADE, null, 2, ColorTag.GOLD),
    EXTRA_TIME(250, "EXTRA TIME", "EXTRA",
            "Level time +5 seconds.",
            Kind.CONSUMABLE, null, 0, ColorTag.LIME),
    EXTRA_AMMO(200, "EXTRA AMMO", "EXTRA",
            "Ammo cap +1 (max 12).",
            Kind.CONSUMABLE, null, 0, ColorTag.CYAN);

    public enum Kind { WEAPON, CONSUMABLE, GRADE, AUTOFIRE }

    public enum ColorTag { CYAN, AQUA, VIOLET, GOLD, ORANGE, PINK, LIME }

    private final int price;
    private final String displayName;
    private final String typeTag;
    private final String description;
    private final Kind kind;
    private final ShopWeapon weapon;
    private final int targetGrade;
    private final ColorTag colorTag;

    ShopItem(int price, String displayName, String typeTag, String description,
             Kind kind, ShopWeapon weapon, int targetGrade, ColorTag colorTag) {
        this.price = price;
        this.displayName = displayName;
        this.typeTag = typeTag;
        this.description = description;
        this.kind = kind;
        this.weapon = weapon;
        this.targetGrade = targetGrade;
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

    /** Catalog order: teuerste zuerst. */
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
