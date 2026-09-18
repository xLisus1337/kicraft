package net.kicraft.kicraft.core.player;

import java.util.Arrays;
import java.util.Locale;

public enum KiTechnique {
    FLIGHT("Flight", "flight", "fly"),
    KI_BLAST("Ki Blast", "ki_blast", "kiblast", "blast"),
    KI_SENSE("Ki Sense", "ki_sense", "kisense", "sense"),
    KI_TRANSFER("Ki Transfer", "ki_transfer", "kitransfer", "transfer");

    private final String displayName;
    private final String commandName;
    private final String[] aliases;

    KiTechnique(String displayName, String commandName, String... aliases) {
        this.displayName = displayName;
        this.commandName = commandName;
        this.aliases = aliases;
    }

    public String displayName() {
        return displayName;
    }

    public String commandName() {
        return commandName;
    }

    public String translationKey() {
        return "technique.kicraft." + name().toLowerCase(Locale.ROOT);
    }

    public boolean isKiAttack() {
        return this == KI_BLAST;
    }

    public static KiTechnique byName(String name) {
        String key = normalize(name);
        for (KiTechnique technique : values()) {
            if (normalize(technique.name()).equals(key)
                    || normalize(technique.displayName).equals(key)
                    || normalize(technique.commandName).equals(key)
                    || Arrays.stream(technique.aliases).anyMatch(alias -> normalize(alias).equals(key))) {
                return technique;
            }
        }
        return null;
    }

    public static String[] suggestions() {
        return Arrays.stream(values())
                .map(technique -> technique.commandName)
                .toArray(String[]::new);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase().replace("_", "").replace("-", "").replace(" ", "");
    }
}
