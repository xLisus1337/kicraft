package net.kicraft.kicraft.core.combat;

/** Server-owned four-hit light combo. A step is committed only after a real, unblocked hit. */
public final class LightCombo {
    public static final int MAX_STEPS = 4;
    public static final int WINDOW_TICKS = 16;
    private int step;
    private int targetId = -1;
    private long expiresAt;

    public int preview(long now, int newTargetId) {
        if (step <= 0 || newTargetId != targetId || now > expiresAt) return 1;
        return Math.min(MAX_STEPS, step + 1);
    }

    public int land(long now, int newTargetId) {
        int landedStep = preview(now, newTargetId);
        if (landedStep >= MAX_STEPS) {
            reset();
        } else {
            step = landedStep;
            targetId = newTargetId;
            expiresAt = now + WINDOW_TICKS;
        }
        return landedStep;
    }

    public void reset() {
        step = 0;
        targetId = -1;
        expiresAt = 0;
    }

    public int step(long now) {
        if (step > 0 && now > expiresAt) reset();
        return step;
    }

    public static double damageMultiplier(int step) {
        return switch (step) {
            case 2 -> 1.08;
            case 3 -> 1.16;
            case 4 -> 1.35;
            default -> 1.0;
        };
    }

    public static double knockbackMultiplier(int step) {
        return step >= MAX_STEPS ? 1.85 : 1.0;
    }
}
