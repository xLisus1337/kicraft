package net.kicraft.kicraft.core.player;

/** Shared exhaustion boundaries for client prediction and server enforcement. */
public final class StaminaRules {
    private static final double EMPTY_THRESHOLD = 0.001;
    private static final double SPRINT_RESUME_RATIO = 0.10;

    private StaminaRules() { }

    public static boolean isEmpty(double stamina) {
        return stamina <= EMPTY_THRESHOLD;
    }

    public static boolean canResumeSprint(double stamina, double maxStamina) {
        return stamina >= maxStamina * SPRINT_RESUME_RATIO;
    }
}
