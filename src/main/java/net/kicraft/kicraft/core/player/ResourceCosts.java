package net.kicraft.kicraft.core.player;

public final class ResourceCosts {
    private static final double BASE_SPRINT_DRAIN = 0.10;
    private static final double BASE_MELEE_STAMINA_COST = 2.0;
    private static final double MIN_MELEE_STAMINA_COST = 0.45;

    private static final double BASIC_KI_BLAST_COST = 5.0;
    private static final double MIN_VISIBLE_KI_BLAST_COST = 0.05;
    private static final double STARTER_KI_LEVEL = 100.0;
    private static final double NEAR_FREE_KI_BLAST_LEVEL = 5000.0;
    private static final double BASE_FLIGHT_KI_DRAIN = 0.18;
    private static final double FLIGHT_IDLE_STAMINA_DRAIN = 0.010;
    private static final double FLIGHT_MOVING_STAMINA_DRAIN = 0.035;
    private static final double BOOST_FLIGHT_KI_MULTIPLIER = 2.25;
    private static final double BOOST_FLIGHT_STAMINA_MULTIPLIER = 1.85;

    private ResourceCosts() {
    }

    public static double sprintDrain(PlayerData data) {
        return BASE_SPRINT_DRAIN * staminaCostMultiplier(data);
    }

    public static double sprintTrainingAmount(double drain) {
        // Sprinting is the main repeatable Stamina exercise. A 400-block run at
        // starter speed should produce visible progress rather than a trace amount.
        return Math.max(0.0060, drain * 0.060);
    }

    public static double meleeStaminaCost(PlayerData data) {
        return Math.max(MIN_MELEE_STAMINA_COST, BASE_MELEE_STAMINA_COST * staminaCostMultiplier(data));
    }

    public static double meleeStaminaTrainingAmount(double staminaCost) {
        return 0.012 + Math.min(0.040, Math.log10(1.0 + Math.max(0.0, staminaCost)) * 0.010);
    }

    public static double basicKiBlastCost(PlayerData data) {
        double kiProgress = progress(data.maxKi(), STARTER_KI_LEVEL, NEAR_FREE_KI_BLAST_LEVEL);
        double controlProgress = progress(Math.log10(Math.max(1.0, data.stat(KiStat.KI_CONTROL))), 0.0, 3.0);
        double efficiency = clamp(kiProgress * 0.78 + controlProgress * 0.22, 0.0, 1.0);
        double cost = BASIC_KI_BLAST_COST * Math.pow(1.0 - efficiency, 2.35);
        return Math.max(MIN_VISIBLE_KI_BLAST_COST, cost);
    }

    public static double flightKiDrain(PlayerData data, boolean moving) {
        return flightKiDrain(data, moving, false);
    }

    public static double flightKiDrain(PlayerData data, boolean moving, boolean boosted) {
        double controlProgress = progress(Math.log10(Math.max(1.0, data.stat(KiStat.KI_CONTROL))), 0.0, 3.2);
        double maxKiProgress = progress(data.maxKi(), STARTER_KI_LEVEL, 12000.0);
        double efficiency = clamp(controlProgress * 0.7 + maxKiProgress * 0.3, 0.0, 0.92);
        double movementScale = moving ? 1.0 : 0.42;
        double cost = BASE_FLIGHT_KI_DRAIN * movementScale * Math.pow(1.0 - efficiency, 1.35);
        double regularCost = Math.max(moving ? 0.055 : 0.025, cost);
        return regularCost * (boosted && moving ? BOOST_FLIGHT_KI_MULTIPLIER : 1.0);
    }

    public static double flightStaminaDrain(PlayerData data, boolean moving) {
        return flightStaminaDrain(data, moving, false);
    }

    public static double flightStaminaDrain(PlayerData data, boolean moving, boolean boosted) {
        double base = moving ? FLIGHT_MOVING_STAMINA_DRAIN : FLIGHT_IDLE_STAMINA_DRAIN;
        double regularCost = base * staminaCostMultiplier(data);
        return regularCost * (boosted && moving ? BOOST_FLIGHT_STAMINA_MULTIPLIER : 1.0);
    }

    /**
     * Higher Stamina gives more actions and faster recovery, but never makes
     * sprinting, dodging or melee free.
     */
    public static double staminaCostMultiplier(PlayerData data) {
        double level = Math.max(1.0, data.stat(KiStat.STAMINA));
        double logarithmicProgress = Math.log10(level);
        return clamp(1.0 / (1.0 + logarithmicProgress * 0.34), 0.28, 1.0);
    }

    public static double staminaRecoveryMultiplier(PlayerData data) {
        double level = Math.max(1.0, data.stat(KiStat.STAMINA));
        return 1.0 + clamp(Math.log10(level) * 0.075, 0.0, 0.45);
    }

    private static double progress(double value, double start, double end) {
        if (end <= start) {
            return value >= end ? 1.0 : 0.0;
        }
        return clamp((value - start) / (end - start), 0.0, 1.0);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
