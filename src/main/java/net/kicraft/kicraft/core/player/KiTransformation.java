package net.kicraft.kicraft.core.player;

import java.util.Arrays;
import java.util.Locale;

public enum KiTransformation {
    BASE("Base", TransformationFamily.NONE, 1.0, 1.0, 1.0, 0.0, 0.0, false, false, KiRace.values()),

    OOZARU("Oozaru", TransformationFamily.OOZARU, 10.0, 6.0, 0.65, 0.0, 0.026, false, false, KiRace.SAIYAN, KiRace.HALF_SAIYAN),
    GOLDEN_OOZARU("Golden Oozaru", TransformationFamily.OOZARU, 200.0, 90.0, 0.55, 0.18, 0.135, false, false, KiRace.SAIYAN, KiRace.HALF_SAIYAN),

    SSJ1("SSJ", TransformationFamily.SAIYAN, 50.0, 22.0, 1.12, 0.045, 0.018, false, true, KiRace.SAIYAN, KiRace.HALF_SAIYAN),
    SSJ2("SSJ2", TransformationFamily.SAIYAN, 100.0, 36.0, 1.18, 0.075, 0.030, false, true, KiRace.SAIYAN, KiRace.HALF_SAIYAN),
    SSJ3("SSJ3", TransformationFamily.SAIYAN, 400.0, 70.0, 1.24, 0.185, 0.112, false, true, KiRace.SAIYAN),
    SSJ4("SSJ4", TransformationFamily.SAIYAN, 900.0, 170.0, 1.16, 0.125, 0.045, false, true, KiRace.SAIYAN),
    SSG("Super Saiyan God", TransformationFamily.GOD, 1400.0, 260.0, 1.20, 0.115, 0.020, true, true, KiRace.SAIYAN),
    SSB("Super Saiyan Blue", TransformationFamily.GOD, 2600.0, 420.0, 1.24, 0.245, 0.038, true, true, KiRace.SAIYAN),

    IKARI("Ikari", TransformationFamily.LEGENDARY, 120.0, 80.0, 1.05, 0.105, 0.082, false, true, KiRace.SAIYAN),
    LSSJ1("LSSJ", TransformationFamily.LEGENDARY, 7200.0, 1600.0, 1.20, 0.385, 0.205, false, true, KiRace.SAIYAN),

    ULTIMATE_FORM("Ultimate Form", TransformationFamily.HALF_SAIYAN, 520.0, 180.0, 1.18, 0.052, 0.014, false, true, KiRace.HALF_SAIYAN),
    BEAST("Beast", TransformationFamily.HALF_SAIYAN, 2400.0, 520.0, 1.32, 0.205, 0.066, false, true, KiRace.HALF_SAIYAN),

    POTENTIAL_UNLEASHED("Potential Unleashed", TransformationFamily.HUMAN, 220.0, 120.0, 1.12, 0.026, 0.008, false, true, KiRace.HUMAN),
    MAX_POWER("Max Power", TransformationFamily.HUMAN, 120.0, 180.0, 0.92, 0.014, 0.068, false, true, KiRace.HUMAN),
    MARTIAL_ARTS_MODE("Martial Arts Mode", TransformationFamily.HUMAN, 90.0, 80.0, 1.22, 0.008, 0.032, false, true, KiRace.HUMAN),

    GIANT_NAMEKIAN("Giant Namekian", TransformationFamily.NAMEKIAN, 160.0, 360.0, 0.72, 0.032, 0.086, false, true, KiRace.NAMEKIAN),
    SUPER_NAMEKIAN("Super Namekian", TransformationFamily.NAMEKIAN, 380.0, 260.0, 1.05, 0.052, 0.022, false, true, KiRace.NAMEKIAN),
    NAMEKIAN_FUSION("Namekian Fusion", TransformationFamily.NAMEKIAN, 760.0, 480.0, 1.08, 0.072, 0.026, false, true, KiRace.NAMEKIAN),

    FROST_SECOND_FORM("Second Form", TransformationFamily.FROST_DEMON, 28.0, 24.0, 1.02, 0.010, 0.010, false, true, KiRace.FROST_DEMON),
    FROST_THIRD_FORM("Third Form", TransformationFamily.FROST_DEMON, 55.0, 38.0, 1.06, 0.018, 0.014, false, true, KiRace.FROST_DEMON),
    FROST_FINAL_FORM("Final Form", TransformationFamily.FROST_DEMON, 65.0, 55.0, 1.14, 0.026, 0.016, false, true, KiRace.FROST_DEMON),
    FROST_100_PERCENT("100% Power", TransformationFamily.FROST_DEMON, 420.0, 260.0, 0.88, 0.086, 0.110, false, true, KiRace.FROST_DEMON),
    GOLDEN_FROST("Golden Form", TransformationFamily.FROST_DEMON, 1600.0, 480.0, 1.18, 0.150, 0.045, false, true, KiRace.FROST_DEMON),
    BLACK_FROST("Black Form", TransformationFamily.FROST_DEMON, 3300.0, 760.0, 1.26, 0.230, 0.052, false, true, KiRace.FROST_DEMON),

    ANDROID_OVERCLOCK("Overclock", TransformationFamily.ANDROID, 90.0, 70.0, 1.20, 0.0, 0.0, false, true, KiRace.ANDROID),
    REACTOR_RELEASE("Reactor Release", TransformationFamily.ANDROID, 240.0, 120.0, 1.12, 0.0, 0.0, false, true, KiRace.ANDROID),
    ABSORPTION_MODE("Absorption Mode", TransformationFamily.ANDROID, 70.0, 180.0, 0.98, 0.0, 0.0, false, true, KiRace.ANDROID),

    BIO_IMPERFECT("Imperfect Form", TransformationFamily.BIO_ANDROID, 120.0, 110.0, 1.08, 0.030, 0.016, false, true, KiRace.BIO_ANDROID),
    BIO_SEMI_PERFECT("Semi-Perfect Form", TransformationFamily.BIO_ANDROID, 280.0, 200.0, 1.12, 0.044, 0.022, false, true, KiRace.BIO_ANDROID),
    BIO_PERFECT("Perfect Form", TransformationFamily.BIO_ANDROID, 900.0, 420.0, 1.18, 0.074, 0.030, false, true, KiRace.BIO_ANDROID),
    BIO_SUPER_PERFECT("Super Perfect Form", TransformationFamily.BIO_ANDROID, 2100.0, 650.0, 1.24, 0.130, 0.040, false, true, KiRace.BIO_ANDROID),

    EVIL_MAJIN("Evil Majin", TransformationFamily.MAJIN, 70.0, 160.0, 1.02, 0.024, 0.024, false, true, KiRace.MAJIN),
    KID_MAJIN("Kid Majin", TransformationFamily.MAJIN, 520.0, 420.0, 1.18, 0.068, 0.046, false, true, KiRace.MAJIN),
    PURE_MAJIN("Pure Majin", TransformationFamily.MAJIN, 1700.0, 760.0, 1.12, 0.118, 0.050, false, true, KiRace.MAJIN),
    ANCIENT_MAJIN("Ancient Majin", TransformationFamily.MAJIN, 2600.0, 1100.0, 1.08, 0.170, 0.065, false, true, KiRace.MAJIN);

    private final String displayName;
    private final TransformationFamily family;
    private final double strengthMultiplier;
    private final double durabilityMultiplier;
    private final double movementMultiplier;
    private final double kiDrainPerTick;
    private final double staminaDrainPerTick;
    private final boolean requiresGodKi;
    private final boolean menuVisible;
    private final KiRace[] allowedRaces;

    KiTransformation(String displayName, TransformationFamily family, double strengthMultiplier, double durabilityMultiplier,
                     double movementMultiplier, double kiDrainPerTick, double staminaDrainPerTick,
                     boolean requiresGodKi, boolean menuVisible, KiRace... allowedRaces) {
        this.displayName = displayName;
        this.family = family;
        this.strengthMultiplier = strengthMultiplier;
        this.durabilityMultiplier = durabilityMultiplier;
        this.movementMultiplier = movementMultiplier;
        this.kiDrainPerTick = kiDrainPerTick;
        this.staminaDrainPerTick = staminaDrainPerTick;
        this.requiresGodKi = requiresGodKi;
        this.menuVisible = menuVisible;
        this.allowedRaces = allowedRaces;
    }

    public String displayName() {
        return displayName;
    }

    public String translationKey() {
        return "transformation.kicraft." + name().toLowerCase(Locale.ROOT);
    }

    public TransformationFamily family() {
        return family;
    }

    public boolean isBase() {
        return this == BASE;
    }

    public boolean menuVisible() {
        return menuVisible;
    }

    public boolean hasMastery() {
        return this != BASE && family != TransformationFamily.OOZARU;
    }

    public boolean requiresGodKi() {
        return requiresGodKi;
    }

    public int kiColor(int fallbackColor) {
        return switch (this) {
            case OOZARU -> 0xB45B2C;
            case GOLDEN_OOZARU -> 0xFFB83D;
            case SSJ1 -> 0xFFE66D;
            case SSJ2 -> 0xF0C24A;
            case SSJ3 -> 0xC9972F;
            case SSJ4 -> 0xD9A23A;
            case SSG -> 0xD91F2E;
            case SSB -> 0x24CFFF;
            case IKARI -> 0xA9FF62;
            case LSSJ1 -> 0x7DFF47;
            case ULTIMATE_FORM -> 0xF4E8FF;
            case BEAST -> 0xD8C7FF;
            case POTENTIAL_UNLEASHED -> 0xFFF7D6;
            case MAX_POWER -> 0xFF8A32;
            case MARTIAL_ARTS_MODE -> 0x9DEBFF;
            case GIANT_NAMEKIAN -> 0x4FBA45;
            case SUPER_NAMEKIAN -> 0x9DFF69;
            case NAMEKIAN_FUSION -> 0x48FFC2;
            case FROST_SECOND_FORM -> 0xCFA6FF;
            case FROST_THIRD_FORM -> 0x9E63FF;
            case FROST_FINAL_FORM -> 0xE8DDFF;
            case FROST_100_PERCENT -> 0xD24AFF;
            case GOLDEN_FROST -> 0xFFCF4A;
            case BLACK_FROST -> 0x5C35FF;
            case ANDROID_OVERCLOCK -> 0x5DFFB1;
            case REACTOR_RELEASE -> 0x56F3FF;
            case ABSORPTION_MODE -> 0x39E36A;
            case BIO_IMPERFECT -> 0x8CFF5D;
            case BIO_SEMI_PERFECT -> 0x55FF9A;
            case BIO_PERFECT -> 0xB7FF3E;
            case BIO_SUPER_PERFECT -> 0xFFF45D;
            case EVIL_MAJIN -> 0xFF5BC8;
            case KID_MAJIN -> 0xFF8FE3;
            case PURE_MAJIN -> 0xD85BFF;
            case ANCIENT_MAJIN -> 0xFF3D73;
            default -> fallbackColor;
        };
    }

    public double strengthMultiplier(double masteryPercent) {
        if (!hasMastery()) {
            return strengthMultiplier;
        }
        if (this == LSSJ1) {
            return scaledBetween(2600.0, strengthMultiplier, masteryPercent);
        }
        return scaled(strengthMultiplier, masteryPercent);
    }

    public double durabilityMultiplier(double masteryPercent) {
        if (!hasMastery()) {
            return durabilityMultiplier;
        }
        if (this == LSSJ1) {
            return scaledBetween(420.0, durabilityMultiplier, masteryPercent);
        }
        return scaled(durabilityMultiplier, masteryPercent);
    }

    public double movementMultiplier(double masteryPercent) {
        if (!hasMastery()) {
            return movementMultiplier;
        }
        return 1.0 + (movementMultiplier - 1.0) * (0.70 + masteryRatio(masteryPercent) * 0.30);
    }

    public double kiDrainPerTick(double masteryPercent, double kiControl) {
        if (!hasMastery()) {
            return scaledDrain(kiDrainPerTick, 0.0, kiControl);
        }
        return scaledDrain(kiDrainPerTick, masteryPercent, kiControl);
    }

    public double staminaDrainPerTick(double masteryPercent, double kiControl) {
        if (!hasMastery()) {
            return scaledDrain(staminaDrainPerTick, 0.0, kiControl);
        }
        return scaledDrain(staminaDrainPerTick, masteryPercent, kiControl);
    }

    public boolean allowedFor(PlayerData data) {
        if (this == BASE) {
            return true;
        }
        if (data == null) {
            return false;
        }
        if (family == TransformationFamily.LEGENDARY && !data.isLegendarySaiyan()) {
            return false;
        }
        if (data.isLegendarySaiyan() && family != TransformationFamily.LEGENDARY) {
            return false;
        }
        if (requiresGodKi && (!data.isGodKiUnlocked() || data.godKi() <= 0.0)) {
            return false;
        }
        for (KiRace race : allowedRaces) {
            if (data.race() == race) {
                return true;
            }
        }
        return false;
    }

    public boolean belongsToRace(KiRace race, boolean legendarySaiyan) {
        if (this == BASE) {
            return true;
        }
        if (family == TransformationFamily.LEGENDARY) {
            return legendarySaiyan && race == KiRace.SAIYAN;
        }
        if (legendarySaiyan) {
            return false;
        }
        return Arrays.asList(allowedRaces).contains(race);
    }

    public static KiTransformation byName(String value) {
        String key = normalize(value);
        for (KiTransformation transformation : values()) {
            if (normalize(transformation.name()).equals(key) || normalize(transformation.displayName).equals(key)) {
                return transformation;
            }
        }
        return null;
    }

    public static String[] suggestions() {
        return Arrays.stream(values())
                .filter(transformation -> transformation != BASE && transformation.menuVisible())
                .map(transformation -> transformation.name().toLowerCase(Locale.ROOT))
                .toArray(String[]::new);
    }

    private double scaled(double base, double masteryPercent) {
        double ratio = masteryRatio(masteryPercent);
        return 1.0 + (base - 1.0) * (0.72 + ratio * 0.28);
    }

    private static double scaledBetween(double start, double end, double masteryPercent) {
        double ratio = masteryRatio(masteryPercent);
        double curve = Math.pow(ratio, 0.72);
        return start + (end - start) * curve;
    }

    private double scaledDrain(double base, double masteryPercent, double kiControl) {
        if (base <= 0.0) {
            return 0.0;
        }
        double masteryEfficiency = 1.0 - masteryRatio(masteryPercent) * 0.55;
        double controlEfficiency = 1.0 / (1.0 + Math.log10(Math.max(1.0, kiControl)) * 0.42);
        return Math.max(base * 0.08, base * masteryEfficiency * controlEfficiency);
    }

    private static double masteryRatio(double masteryPercent) {
        return Math.max(0.0, Math.min(1.0, masteryPercent / 100.0));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(" ", "").replace("%", "");
    }

    public enum TransformationFamily {
        NONE,
        OOZARU,
        SAIYAN,
        LEGENDARY,
        GOD,
        HALF_SAIYAN,
        HUMAN,
        NAMEKIAN,
        FROST_DEMON,
        ANDROID,
        BIO_ANDROID,
        MAJIN
    }
}
