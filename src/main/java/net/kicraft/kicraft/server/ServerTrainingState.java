package net.kicraft.kicraft.server;

import net.kicraft.kicraft.Kicraft;
import net.kicraft.kicraft.content.KicraftBlocks;
import net.kicraft.kicraft.content.KicraftEntities;
import net.kicraft.kicraft.content.TrainingDummyEntity;
import net.kicraft.kicraft.core.player.KiStat;
import net.kicraft.kicraft.core.player.PlayerData;
import net.kicraft.kicraft.core.training.TrainingActivity;
import net.kicraft.kicraft.network.KicraftNetwork;
import net.kicraft.kicraft.network.StatXpGainPacket;
import net.kicraft.kicraft.server.capability.PlayerDataCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.lang.ref.WeakReference;

/** Server-owned, low-frequency training logic. No client input is trusted here. */
@Mod.EventBusSubscriber(modid = Kicraft.MODID)
public final class ServerTrainingState {
    private static final int TRAINING_INTERVAL_TICKS = 20;
    private static final double MAX_ANCHOR_DISTANCE_SQR = 0.16;
    private static final String DUMMY_TAG = "kicraftTrainingDummy";
    private static final String DUMMY_OWNER_TAG = "kicraftTrainingDummyOwner";
    private static final float DUMMY_VISIBLE_HIT_DAMAGE = 0.01F;
    private static final double TRAINING_XP_VARIANCE = 0.20;
    private static final Map<UUID, TrainingSession> SESSIONS = new HashMap<>();
    // Includes loaded entities in inactive chunks, which ServerLevel.getEntity cannot find.
    // Weak references do not keep unloaded chunks/entities alive. Tracking-end events alone
    // cannot clear this cache: an entity may remain loaded in a temporarily inactive chunk.
    private static final Map<UUID, WeakReference<Entity>> LOADED_DUMMIES = new HashMap<>();

    private ServerTrainingState() {
    }

    public static boolean start(ServerPlayer player, TrainingActivity activity) {
        if (activity == null || !canTrain(player)) {
            return false;
        }
        if (!isStandingOnTrainingMat(player)) {
            player.displayClientMessage(Component.translatable("message.kicraft.training_mat_required")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        SESSIONS.put(player.getUUID(), new TrainingSession(activity, player.position(), player.level().dimension()));
        return true;
    }

    public static void stop(ServerPlayer player) {
        SESSIONS.remove(player.getUUID());
    }

    public static boolean isTraining(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    public static void spawnDummy(ServerPlayer player) {
        if (!canTrain(player)) return;
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        TrainingDummyEntity dummy = KicraftEntities.TRAINING_DUMMY.get().create(level);
        if (dummy == null) {
            return;
        }
        moveDummyInFrontOfPlayer(dummy, player);
        dummy.setInvulnerable(false);
        dummy.setCustomName(Component.translatable("entity.kicraft.training_dummy"));
        dummy.setCustomNameVisible(true);
        dummy.getPersistentData().putBoolean(DUMMY_TAG, true);
        dummy.getPersistentData().putUUID(DUMMY_OWNER_TAG, player.getUUID());
        TrainingDummyRegistry registry = TrainingDummyRegistry.get(player.server);
        UUID previousId = registry.current(player.getUUID());
        WeakReference<Entity> previousReference = LOADED_DUMMIES.get(player.getUUID());
        Entity previousLoaded = previousReference == null ? null : previousReference.get();
        registry.replace(player.getUUID(), dummy.getUUID());
        if (!level.addFreshEntity(dummy)) {
            // A cancelled spawn must not invalidate the player's existing dummy.
            registry.replace(player.getUUID(), previousId);
            if (previousLoaded != null && !previousLoaded.isRemoved()) {
                LOADED_DUMMIES.put(player.getUUID(), new WeakReference<>(previousLoaded));
            } else {
                LOADED_DUMMIES.remove(player.getUUID());
            }
            return;
        }
        if (previousLoaded != null && previousLoaded != dummy) previousLoaded.discard();
        if (previousId != null) {
            for (ServerLevel world : player.server.getAllLevels()) {
                Entity previous = world.getEntity(previousId);
                if (isTrainingDummy(previous)
                        && previous.getPersistentData().hasUUID(DUMMY_OWNER_TAG)
                        && player.getUUID().equals(previous.getPersistentData().getUUID(DUMMY_OWNER_TAG))) {
                    previous.discard();
                }
            }
        }
        // An unloaded predecessor is invalidated in saved data and rejected before it rejoins.
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void rejectReplacedDummy(EntityJoinLevelEvent event) {
        Entity dummy = event.getEntity();
        if (!(event.getLevel() instanceof ServerLevel level) || !isTrainingDummy(dummy)
                || !dummy.getPersistentData().hasUUID(DUMMY_OWNER_TAG)) return;
        UUID owner = dummy.getPersistentData().getUUID(DUMMY_OWNER_TAG);
        if (!TrainingDummyRegistry.get(level.getServer()).acceptOrAdopt(owner, dummy.getUUID())) {
            event.setCanceled(true);
        } else {
            LOADED_DUMMIES.put(owner, new WeakReference<>(dummy));
        }
    }

    public static boolean isTrainingDummy(Entity entity) {
        return (entity instanceof TrainingDummyEntity || entity instanceof ArmorStand)
                && entity.getPersistentData().getBoolean(DUMMY_TAG);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }

        TrainingSession session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }
        if (!canTrain(player) || !session.dimension.equals(player.level().dimension())
                || !isStandingOnTrainingMat(player) || movedAwayFromAnchor(player, session.anchor())) {
            stop(player);
            return;
        }

        session.ticksSinceLastReward++;
        if (session.ticksSinceLastReward < TRAINING_INTERVAL_TICKS) {
            return;
        }
        session.ticksSinceLastReward = 0;

        player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> applyTrainingCycle(player, data, session.activity()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void preserveDummyWhileShowingHit(LivingDamageEvent event) {
        if (isTrainingDummy(event.getEntity())) {
            // Do not cancel the hit. Vanilla then shows the hurt animation and applies knockback.
            // The small amount is restored by TrainingDummyEntity on its next server tick.
            event.setAmount(Math.min(event.getAmount(), DUMMY_VISIBLE_HIT_DAMAGE));
        }
    }

    @SubscribeEvent
    public static void interruptTrainingOnDamage(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAmount() > 0.0F) {
            stop(player);
        }
    }

    @SubscribeEvent
    public static void removeOwnedDummy(PlayerInteractEvent.EntityInteract event) {
        Entity dummy = event.getTarget();
        if (!isTrainingDummy(dummy)
                || !event.getEntity().isShiftKeyDown()
                || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (dummy.getPersistentData().hasUUID(DUMMY_OWNER_TAG)
                && dummy.getPersistentData().getUUID(DUMMY_OWNER_TAG).equals(player.getUUID())) {
            TrainingDummyRegistry.get(player.server).remove(player.getUUID(), dummy.getUUID());
            dummy.discard();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void clearPlayerState(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SESSIONS.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        SESSIONS.clear();
        LOADED_DUMMIES.clear();
    }

    private static boolean canTrain(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.isSleeping()
                && player.getCapability(PlayerDataCapability.PLAYER_DATA).map(PlayerData::isRaceChosen).orElse(false);
    }

    private static void applyTrainingCycle(ServerPlayer player, PlayerData data, TrainingActivity activity) {
        if (!data.isRaceChosen()) {
            stop(player);
            return;
        }

        if (activity != TrainingActivity.MEDITATION && !data.race().hasInfiniteEnergy()) {
            double physicalCost = Math.max(1.0, data.maxStamina() * 0.012);
            if (data.stamina() < physicalCost) {
                stop(player);
                return;
            }
            StaminaSystem.spend(player, data, physicalCost);
        }

        EnumMap<KiStat, Double> xpGains = new EnumMap<>(KiStat.class);
        switch (activity) {
            case PUSH_UPS -> {
                recordXp(xpGains, KiStat.STRENGTH, data.train(KiStat.STRENGTH, variedXp(player, 0.45)));
                recordXp(xpGains, KiStat.STAMINA, data.train(KiStat.STAMINA, variedXp(player, 0.12)));
            }
            case SIT_UPS -> {
                recordXp(xpGains, KiStat.DURABILITY, data.train(KiStat.DURABILITY, variedXp(player, 0.38)));
                recordXp(xpGains, KiStat.STAMINA, data.train(KiStat.STAMINA, variedXp(player, 0.15)));
            }
            case SQUATS -> {
                recordXp(xpGains, KiStat.STAMINA, data.train(KiStat.STAMINA, variedXp(player, 0.42)));
                recordXp(xpGains, KiStat.STRENGTH, data.train(KiStat.STRENGTH, variedXp(player, 0.12)));
            }
            case MEDITATION -> {
                recordXp(xpGains, KiStat.KI_CONTROL, data.train(KiStat.KI_CONTROL, variedXp(player, 0.28)));
                data.trainFocus(0.12);
                data.setKi(Math.min(data.maxKi(), data.ki() + Math.max(0.5, data.maxKi() * 0.008)));
            }
        }

        DailyTrainingState.recordExercise(player, data, activity);
        if (!xpGains.isEmpty()) {
            KicraftNetwork.sendToPlayer(player, new StatXpGainPacket(xpGains));
        }

        PlayerDataEvents.sync(player);
        data.clearDirty();
    }

    private static void recordXp(Map<KiStat, Double> gains, KiStat stat, double amount) {
        if (amount > 0.0 && Double.isFinite(amount)) {
            gains.merge(stat, amount, Double::sum);
        }
    }

    private static double variedXp(ServerPlayer player, double baseAmount) {
        double multiplier = 1.0 - TRAINING_XP_VARIANCE + player.getRandom().nextDouble() * TRAINING_XP_VARIANCE * 2.0;
        return baseAmount * multiplier;
    }

    private static void moveDummyInFrontOfPlayer(Entity dummy, ServerPlayer player) {
        Vec3 direction = player.getLookAngle().multiply(1.0, 0.0, 1.0);
        if (direction.lengthSqr() < 0.0001) {
            direction = new Vec3(0.0, 0.0, 1.0);
        }
        direction = direction.normalize();
        Vec3 position = player.position().add(direction.scale(2.2));
        dummy.moveTo(position.x, player.getY(), position.z, player.getYRot() + 180.0F, 0.0F);
    }

    private static boolean isStandingOnTrainingMat(ServerPlayer player) {
        BlockPos feet = BlockPos.containing(player.getX(), player.getBoundingBox().minY - 0.02, player.getZ());
        return player.level().getBlockState(feet).is(KicraftBlocks.TRAINING_MAT.get());
    }

    private static boolean movedAwayFromAnchor(ServerPlayer player, Vec3 anchor) {
        double x = player.getX() - anchor.x;
        double z = player.getZ() - anchor.z;
        return x * x + z * z > MAX_ANCHOR_DISTANCE_SQR;
    }

    private static final class TrainingSession {
        private final TrainingActivity activity;
        private final Vec3 anchor;
        private final ResourceKey<Level> dimension;
        private int ticksSinceLastReward;

        private TrainingSession(TrainingActivity activity, Vec3 anchor, ResourceKey<Level> dimension) {
            this.activity = activity;
            this.anchor = anchor;
            this.dimension = dimension;
        }

        private TrainingActivity activity() {
            return activity;
        }

        private Vec3 anchor() {
            return anchor;
        }
    }
}
