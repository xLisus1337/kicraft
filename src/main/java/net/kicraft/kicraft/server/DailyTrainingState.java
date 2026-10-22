package net.kicraft.kicraft.server;

import net.kicraft.kicraft.Kicraft;
import net.kicraft.kicraft.core.player.KiStat;
import net.kicraft.kicraft.core.player.PlayerData;
import net.kicraft.kicraft.core.training.DailyTrainingTask;
import net.kicraft.kicraft.core.training.TrainingActivity;
import net.kicraft.kicraft.server.capability.PlayerDataCapability;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Tracks one lightweight daily routine without trusting any client-side progress. */
@Mod.EventBusSubscriber(modid = Kicraft.MODID)
public final class DailyTrainingState {
    private static final int RUN_SYNC_INTERVAL_TICKS = 10;
    private static final double MAX_TRACKED_RUN_PER_TICK = 0.45;
    private static final Map<UUID, Vec3> LAST_POSITIONS = new HashMap<>();

    private DailyTrainingState() {
    }

    public static void recordExercise(ServerPlayer player, PlayerData data, TrainingActivity activity) {
        DailyTrainingTask task = switch (activity) {
            case PUSH_UPS -> DailyTrainingTask.PUSH_UPS;
            case SIT_UPS -> DailyTrainingTask.SIT_UPS;
            case SQUATS -> DailyTrainingTask.SQUATS;
            case MEDITATION -> null;
        };
        if (task == null) {
            return;
        }

        long day = currentDay(player);
        prepareDailyTraining(player, data, day);
        boolean wasComplete = data.dailyTrainingProgress(task) >= task.goal();
        if (data.addDailyTrainingProgress(task, 1.0, day)) {
            rewardCompletedTask(player, data, task, wasComplete);
            finishIfReady(player, data, day);
        }
    }

    @SubscribeEvent
    public static void initializeDailyTraining(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data ->
                prepareDailyTraining(player, data, currentDay(player)));
    }

    @SubscribeEvent
    public static void trackRunning(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide() || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        Vec3 current = player.position();
        Vec3 previous = LAST_POSITIONS.put(player.getUUID(), current);
        if (player.tickCount % 20 == 0) {
            player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data ->
                    prepareDailyTraining(player, data, currentDay(player)));
        }
        if (previous == null || !player.isAlive() || player.isSpectator() || !player.isSprinting() || !player.onGround()
                || player.isPassenger() || player.getAbilities().flying) {
            return;
        }

        Vec3 delta = current.subtract(previous);
        double distance = Math.min(MAX_TRACKED_RUN_PER_TICK, Math.sqrt(delta.x * delta.x + delta.z * delta.z));
        if (distance <= 0.001) {
            return;
        }

        player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
            if (!data.isRaceChosen()) {
                return;
            }
            long day = currentDay(player);
            prepareDailyTraining(player, data, day);
            boolean wasComplete = data.dailyTrainingProgress(DailyTrainingTask.RUN) >= DailyTrainingTask.RUN.goal();
            if (!data.addDailyTrainingProgress(DailyTrainingTask.RUN, distance, day)) {
                return;
            }
            rewardCompletedTask(player, data, DailyTrainingTask.RUN, wasComplete);
            finishIfReady(player, data, day);
            if (player.tickCount % RUN_SYNC_INTERVAL_TICKS == 0 || data.hasClaimedDailyTrainingReward()) {
                PlayerDataEvents.requestSync(player);
                data.clearDirty();
            }
        });
    }

    @SubscribeEvent
    public static void clearCachedPosition(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_POSITIONS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void clearCachedPosition(PlayerEvent.PlayerChangedDimensionEvent event) {
        LAST_POSITIONS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void clearCachedPosition(PlayerEvent.PlayerRespawnEvent event) {
        LAST_POSITIONS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        LAST_POSITIONS.clear();
    }

    private static void finishIfReady(ServerPlayer player, PlayerData data, long day) {
        if (!data.claimDailyTrainingReward(day)) {
            return;
        }

        data.train(KiStat.STRENGTH, 3.5);
        data.train(KiStat.DURABILITY, 3.5);
        data.train(KiStat.STAMINA, 5.0);
        player.displayClientMessage(Component.translatable("daily_training.kicraft.complete")
                .withStyle(ChatFormatting.GOLD), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.65F, 1.10F);
    }

    private static void rewardCompletedTask(ServerPlayer player, PlayerData data, DailyTrainingTask task, boolean wasComplete) {
        if (wasComplete || data.dailyTrainingProgress(task) < task.goal()) {
            return;
        }

        switch (task) {
            case PUSH_UPS -> {
                data.train(KiStat.STRENGTH, 5.0);
                data.train(KiStat.STAMINA, 1.0);
            }
            case SIT_UPS -> {
                data.train(KiStat.DURABILITY, 5.0);
                data.train(KiStat.STAMINA, 1.0);
            }
            case SQUATS -> {
                data.train(KiStat.STAMINA, 5.0);
                data.train(KiStat.STRENGTH, 1.0);
            }
            case RUN -> data.train(KiStat.STAMINA, 8.0);
        }

        player.displayClientMessage(Component.translatable("daily_training.kicraft.task_complete",
                Component.translatable(task.translationKey())).withStyle(ChatFormatting.YELLOW), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS, 0.55F, 1.0F);
    }

    private static void prepareDailyTraining(ServerPlayer player, PlayerData data, long day) {
        if (!data.prepareDailyTraining(day)) {
            return;
        }
        PlayerDataEvents.sync(player);
        data.clearDirty();
    }

    private static long currentDay(ServerPlayer player) {
        return Math.floorDiv(player.serverLevel().getServer().overworld().getDayTime(), 24000L);
    }
}
