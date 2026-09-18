package net.kicraft.kicraft.core.player;

import java.util.Locale;

public enum KiRace {
    NONE("None"),
    SAIYAN("Saiyan"),
    HALF_SAIYAN("Half Saiyan"),
    HUMAN("Human"),
    NAMEKIAN("Namekian"),
    FROST_DEMON("Frost Demon"),
    ANDROID("Android"),
    BIO_ANDROID("Bio Android"),
    MAJIN("Majin");

    private final String displayName;

    KiRace(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public String translationKey() {
        return "race.kicraft." + name().toLowerCase(Locale.ROOT);
    }

    public boolean hasInfiniteEnergy() {
        return this == ANDROID || this == BIO_ANDROID;
    }

    public boolean canRollLegendarySaiyan() {
        return this == SAIYAN;
    }

    public static KiRace byName(String name) {
        if (name == null || name.isBlank()) {
            return NONE;
        }
        String key = name.trim().toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(" ", "");
        return switch (key) {
            case "saiyan" -> SAIYAN;
            case "halfsaiyan" -> HALF_SAIYAN;
            case "human" -> HUMAN;
            case "namekian" -> NAMEKIAN;
            case "frostdemon" -> FROST_DEMON;
            case "android" -> ANDROID;
            case "bioandroid" -> BIO_ANDROID;
            case "majin" -> MAJIN;
            default -> NONE;
        };
    }
}
