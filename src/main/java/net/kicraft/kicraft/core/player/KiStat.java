package net.kicraft.kicraft.core.player;

import java.util.Locale;

public enum KiStat {
    STRENGTH("Strength"),
    DURABILITY("Durability"),
    MAX_KI("Max Ki"),
    KI_CONTROL("Ki Control"),
    STAMINA("Stamina");

    private final String displayName;

    KiStat(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public String translationKey() {
        return "stat.kicraft." + name().toLowerCase(Locale.ROOT);
    }
}
