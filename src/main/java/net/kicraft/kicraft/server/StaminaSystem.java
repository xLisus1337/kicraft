package net.kicraft.kicraft.server;

import net.kicraft.kicraft.core.player.PlayerData;
import net.kicraft.kicraft.core.player.ResourceCosts;
import net.kicraft.kicraft.core.player.StaminaRules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Server-owned rules for the 0-100 stamina action meter. */
public final class StaminaSystem {
    private static final String EXHAUSTED_TAG = "kicraftStaminaExhausted";
    private static final String RECOVERY_FLOOR_TAG = "kicraftStaminaRecoveryFloor";
    private static final String LAST_EXERTION_TICK_TAG = "kicraftLastStaminaExertionTick";
    private static final double LOW_ZONE_RATIO = 0.10;
    private static final double NORMAL_RECOVERY_SECONDS = 4.5;
    private static final double EMPTY_RECOVERY_SECONDS = 10.0;
    private static final int EXERTION_RECOVERY_DELAY_TICKS = 10;

    private StaminaSystem() {
    }

    public static void reset(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData();
        tag.remove(EXHAUSTED_TAG);
        tag.remove(RECOVERY_FLOOR_TAG);
        tag.remove(LAST_EXERTION_TICK_TAG);
    }

    public static boolean canStartExertion(ServerPlayer player, PlayerData data) {
        return data.race().hasInfiniteEnergy() || !isSprintLocked(player, data);
    }

    public static boolean isSprintLocked(ServerPlayer player, PlayerData data) {
        if (data.race().hasInfiniteEnergy()) {
            return false;
        }
        CompoundTag tag = player.getPersistentData();
        if (StaminaRules.isEmpty(data.stamina())) {
            tag.putBoolean(EXHAUSTED_TAG, true);
            return true;
        }
        return tag.getBoolean(EXHAUSTED_TAG);
    }

    public static boolean isAttackExhausted(PlayerData data) {
        return !data.race().hasInfiniteEnergy() && data.stamina() <= data.maxStamina() * 0.01;
    }

    public static boolean spend(ServerPlayer player, PlayerData data, double requestedCost) {
        if (data.race().hasInfiniteEnergy() || requestedCost <= 0.0) {
            return false;
        }

        markExertion(player);
        double previous = data.stamina();
        double remaining = Math.max(0.0, previous - requestedCost);
        if (remaining >= previous) {
            return false;
        }

        data.setStamina(remaining);
        rememberRecoveryFloor(player, data);
        if (StaminaRules.isEmpty(remaining)) {
            player.getPersistentData().putBoolean(EXHAUSTED_TAG, true);
            if (player.isSprinting()) {
                player.setSprinting(false);
            }
        }
        return true;
    }

    /** Marks any stamina-using action so recovery cannot occur between consecutive actions. */
    public static void markExertion(ServerPlayer player) {
        player.getPersistentData().putLong(LAST_EXERTION_TICK_TAG, player.level().getGameTime());
    }

    public static void enforceSprintLock(ServerPlayer player, PlayerData data) {
        if (data.race().hasInfiniteEnergy()) {
            reset(player);
            return;
        }

        CompoundTag tag = player.getPersistentData();
        if (tag.getBoolean(EXHAUSTED_TAG) && StaminaRules.canResumeSprint(data.stamina(), data.maxStamina())) {
            tag.remove(EXHAUSTED_TAG);
            return;
        }

        if (isSprintLocked(player, data)) {
            if (player.isSprinting()) {
                player.setSprinting(false);
            }
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 2, 1, false, false, false));
        }
    }

    public static boolean tick(ServerPlayer player, PlayerData data) {
        if (data.race().hasInfiniteEnergy()) {
            boolean changed = data.stamina() < data.maxStamina();
            if (changed) {
                data.setStamina(data.maxStamina());
            }
            reset(player);
            return changed;
        }

        enforceSprintLock(player, data);
        boolean sprinting = player.isSprinting();
        if (sprinting) {
            double drain = ResourceCosts.sprintDrain(data) * CombatState.staminaDrainScale(player);
            boolean changed = spend(player, data, drain);
            if (changed) {
                data.train(net.kicraft.kicraft.core.player.KiStat.STAMINA, ResourceCosts.sprintTrainingAmount(drain));
            }
            enforceSprintLock(player, data);
            return changed;
        }

        if (!canRecover(player, data)) {
            return false;
        }

        double recovery = recoveryPerTick(player, data) * CombatState.staminaRegenScale(player);
        if (recovery <= 0.0) {
            return false;
        }
        data.setStamina(Math.min(data.maxStamina(), data.stamina() + recovery));
        if (data.stamina() >= data.maxStamina() - 0.001) {
            player.getPersistentData().remove(RECOVERY_FLOOR_TAG);
        }
        enforceSprintLock(player, data);
        return true;
    }

    private static boolean canRecover(ServerPlayer player, PlayerData data) {
        if (!player.onGround() || data.stamina() >= data.maxStamina()) {
            return false;
        }

        boolean movementInput = Math.abs(player.xxa) > 0.001F || Math.abs(player.zza) > 0.001F;
        boolean moving = movementInput || player.getDeltaMovement().horizontalDistanceSqr() > 0.0004;
        if (moving
                || CombatState.isBlocking(player)
                || MeleeCombat.actionLocked(player)
                || ServerKiState.isFlying(player)
                || ServerKiState.isTransferring(player)
                || ServerTrainingState.isTraining(player)) {
            return false;
        }

        long lastExertionTick = player.getPersistentData().getLong(LAST_EXERTION_TICK_TAG);
        return player.level().getGameTime() - lastExertionTick >= EXERTION_RECOVERY_DELAY_TICKS;
    }

    private static void rememberRecoveryFloor(ServerPlayer player, PlayerData data) {
        double max = Math.max(1.0, data.maxStamina());
        double ratio = clamp(data.stamina() / max, 0.0, 1.0);
        if (ratio >= LOW_ZONE_RATIO) {
            return;
        }

        CompoundTag tag = player.getPersistentData();
        double floor = tag.contains(RECOVERY_FLOOR_TAG) ? tag.getDouble(RECOVERY_FLOOR_TAG) : LOW_ZONE_RATIO;
        tag.putDouble(RECOVERY_FLOOR_TAG, Math.min(floor, ratio));
    }

    private static double recoveryPerTick(ServerPlayer player, PlayerData data) {
        CompoundTag tag = player.getPersistentData();
        double floor = tag.contains(RECOVERY_FLOOR_TAG)
                ? clamp(tag.getDouble(RECOVERY_FLOOR_TAG), 0.0, LOW_ZONE_RATIO)
                : LOW_ZONE_RATIO;
        double recoverySeconds = NORMAL_RECOVERY_SECONDS
                + ((LOW_ZONE_RATIO - floor) / LOW_ZONE_RATIO) * (EMPTY_RECOVERY_SECONDS - NORMAL_RECOVERY_SECONDS);
        double totalToRecover = data.maxStamina() * (1.0 - floor);
        double basePerTick = totalToRecover / Math.max(1.0, recoverySeconds * 20.0);
        return basePerTick * ResourceCosts.staminaRecoveryMultiplier(data);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
