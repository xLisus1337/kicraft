package net.kicraft.kicraft.server;

import net.kicraft.kicraft.server.capability.PlayerDataCapability;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class CombatState {
    private static final String BLOCKING_TAG = "kicraftBlocking";
    private static final String LAST_COMBAT_TICK_TAG = "kicraftLastCombatTick";
    private static final String COMBAT_FATIGUE_TAG = "kicraftCombatFatigue";
    // Kept only to clean up combo data written by older mod versions.
    private static final String LEGACY_COMBO_TARGET_TAG = "kicraftComboTarget";
    private static final String LEGACY_COMBO_HITS_TAG = "kicraftComboHits";
    private static final String LEGACY_COMBO_LAST_TICK_TAG = "kicraftComboLastTick";
    private static final int COMBAT_RECOVERY_DELAY_TICKS = 140;
    private static final double FATIGUE_DECAY_PER_TICK = 0.0045;

    private CombatState() {
    }

    public static void setBlocking(ServerPlayer player, boolean blocking) {
        MeleeCombat.block(player, blocking);
        player.getPersistentData().putBoolean(BLOCKING_TAG, blocking);
    }

    public static boolean isBlocking(ServerPlayer player) {
        return player.getPersistentData().getBoolean(BLOCKING_TAG) && MeleeCombat.canGuard(player);
    }

    public static void markCombat(ServerPlayer player, double pressure) {
        CompoundTag tag = player.getPersistentData();
        tag.putLong(LAST_COMBAT_TICK_TAG, player.level().getGameTime());
        double fatigue = Math.min(1.0, tag.getDouble(COMBAT_FATIGUE_TAG) + Math.max(0.0, pressure) * 0.012);
        tag.putDouble(COMBAT_FATIGUE_TAG, fatigue);
    }

    public static boolean tickFatigue(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData();
        double fatigue = tag.getDouble(COMBAT_FATIGUE_TAG);
        if (fatigue <= 0.0) {
            return false;
        }

        long lastCombatTick = tag.getLong(LAST_COMBAT_TICK_TAG);
        if (player.level().getGameTime() - lastCombatTick <= COMBAT_RECOVERY_DELAY_TICKS) {
            return false;
        }

        fatigue = Math.max(0.0, fatigue - FATIGUE_DECAY_PER_TICK);
        tag.putDouble(COMBAT_FATIGUE_TAG, fatigue);
        return true;
    }

    public static boolean isInCombat(ServerPlayer player) {
        long lastCombatTick = player.getPersistentData().getLong(LAST_COMBAT_TICK_TAG);
        return lastCombatTick > 0L
                && player.level().getGameTime() - lastCombatTick <= COMBAT_RECOVERY_DELAY_TICKS;
    }

    public static double staminaDrainScale(ServerPlayer player) {
        return 1.0 + fatigue(player) * 0.70;
    }

    public static double staminaRegenScale(ServerPlayer player) {
        return Math.max(0.42, 1.0 - fatigue(player) * 0.48);
    }

    public static double powerScore(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            return player.getCapability(PlayerDataCapability.PLAYER_DATA)
                    .map(data -> Math.max(1.0, data.powerLevel()))
                    .orElseGet(() -> KiActionUtil.powerScore(entity));
        }
        return KiActionUtil.powerScore(entity);
    }

    public static void clear(ServerPlayer player) {
        MeleeCombat.clear(player);
        CompoundTag tag = player.getPersistentData();
        tag.remove(BLOCKING_TAG);
        tag.remove(LEGACY_COMBO_TARGET_TAG);
        tag.remove(LEGACY_COMBO_HITS_TAG);
        tag.remove(LEGACY_COMBO_LAST_TICK_TAG);
        tag.remove(LAST_COMBAT_TICK_TAG);
        tag.remove(COMBAT_FATIGUE_TAG);
    }

    private static double fatigue(ServerPlayer player) {
        return Math.max(0.0, Math.min(1.0, player.getPersistentData().getDouble(COMBAT_FATIGUE_TAG)));
    }

}
