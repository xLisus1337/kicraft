package net.kicraft.kicraft.core.combat;

/** Tick-based recovery, parry and strike rules. The server supplies the clock. */
public final class MeleeTimeline {
    public static final int PARRY_WINDOW_TICKS = 4;
    public static final int PARRY_COOLDOWN_TICKS = 12;
    public static final int COUNTER_WINDOW_TICKS = 16;
    public static final int GUARD_BREAK_TICKS = 20;
    public static final int PARRIED_TICKS = 10;
    private long recoveryUntil;
    private long exposedUntil;
    private long parryUntil;
    private long nextParry;
    private long counterUntil;
    private int counterTarget = -1;
    private boolean blockHeld;

    public boolean recovering(long now) { return now < recoveryUntil; }
    public boolean exposed(long now) { return now < exposedUntil; }
    public int recoveryTicks(long now) { return (int) Math.max(0, recoveryUntil - now); }
    public int exposedTicks(long now) { return (int) Math.max(0, exposedUntil - now); }
    public int counterTicks(long now) { return (int) Math.max(0, counterUntil - now); }

    public void block(long now, boolean held) {
        if (held && !blockHeld && !recovering(now) && now >= nextParry) {
            parryUntil = now + PARRY_WINDOW_TICKS;
            nextParry = now + PARRY_COOLDOWN_TICKS;
        }
        if (!held) parryUntil = 0;
        blockHeld = held;
    }

    public boolean canGuard(long now) { return !recovering(now); }
    public boolean canParry(long now) { return blockHeld && canGuard(now) && now < parryUntil; }

    public boolean parry(long now, int attackerId) {
        if (!canParry(now)) return false;
        parryUntil = 0; // One incoming hit per correctly timed press.
        counterUntil = now + COUNTER_WINDOW_TICKS;
        counterTarget = attackerId;
        return true;
    }

    public boolean consumeCounter(long now, int targetId) {
        boolean eligible = now < counterUntil && counterTarget == targetId;
        counterUntil = 0;
        counterTarget = -1;
        return eligible;
    }

    public void recover(long now, int ticks, boolean vulnerable) {
        parryUntil = 0;
        recoveryUntil = Math.max(recoveryUntil, now + ticks);
        if (vulnerable) {
            exposedUntil = Math.max(exposedUntil, now + ticks);
            counterUntil = 0;
            counterTarget = -1;
        }
    }

    public record Strike(boolean powerful) {
        public double damageMultiplier() { return powerful ? 2.4 : 1.0; }
        public double guardMultiplier() { return powerful ? 3.5 : 1.0; }
        public double staminaMultiplier() { return powerful ? 4.0 : 1.0; }
        public int hitRecoveryTicks() { return powerful ? 10 : 6; }
    }
}
