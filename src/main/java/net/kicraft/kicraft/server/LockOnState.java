package net.kicraft.kicraft.server;

import net.kicraft.kicraft.Kicraft;
import net.kicraft.kicraft.core.player.PlayerData;
import net.kicraft.kicraft.network.KicraftNetwork;
import net.kicraft.kicraft.network.LockOnSyncPacket;
import net.kicraft.kicraft.server.capability.PlayerDataCapability;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Kicraft.MODID)
public final class LockOnState {
    private static final String TARGET_TAG = "KicraftLockOnTarget";
    private static final double ACQUIRE_RANGE = 48.0;
    private static final double HOLD_RANGE = 64.0;
    private static final double MIN_TARGET_DOT = 0.18;

    private LockOnState() {
    }

    public static void toggle(ServerPlayer player) {
        if (currentTarget(player).isPresent()) {
            clear(player);
            return;
        }

        acquire(player, false);
    }

    public static void cycle(ServerPlayer player) {
        if (currentTarget(player).isEmpty()) {
            // Cycling may only replace an existing lock. It must never acquire one.
            if (player.getPersistentData().hasUUID(TARGET_TAG)) {
                clear(player);
            }
            return;
        }

        acquire(player, true);
    }

    public static void clear(ServerPlayer player) {
        player.getPersistentData().remove(TARGET_TAG);
        sync(player, null);
    }

    public static Optional<LivingEntity> currentTarget(ServerPlayer player) {
        if (!player.getPersistentData().hasUUID(TARGET_TAG)) {
            return Optional.empty();
        }

        UUID id = player.getPersistentData().getUUID(TARGET_TAG);
        if (!(player.serverLevel().getEntity(id) instanceof LivingEntity target) || !isValidTarget(player, target, HOLD_RANGE)) {
            return Optional.empty();
        }
        return Optional.of(target);
    }

    public static double targetPower(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            return player.getCapability(PlayerDataCapability.PLAYER_DATA)
                    .map(PlayerData::powerLevel)
                    .orElseGet(() -> KiActionUtil.powerScore(entity));
        }
        return KiActionUtil.powerScore(entity);
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide() || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (!player.getPersistentData().hasUUID(TARGET_TAG)) {
            return;
        }

        if (!player.isAlive() || player.isSpectator() || player.isSleeping()
                || !player.getCapability(PlayerDataCapability.PLAYER_DATA)
                .map(PlayerData::isRaceChosen).orElse(false)) {
            clear(player);
            return;
        }

        Optional<LivingEntity> target = currentTarget(player);
        if (target.isEmpty()) {
            clear(player);
        } else if (player.tickCount % 20 == 0) {
            sync(player, target.get());
        }
    }

    private static void acquire(ServerPlayer player, boolean cycle) {
        List<LivingEntity> candidates = collectCandidates(player);
        if (candidates.isEmpty()) {
            clear(player);
            return;
        }

        UUID currentId = player.getPersistentData().hasUUID(TARGET_TAG) ? player.getPersistentData().getUUID(TARGET_TAG) : null;
        LivingEntity target = cycle ? nextTarget(candidates, currentId) : candidates.get(0);
        player.getPersistentData().putUUID(TARGET_TAG, target.getUUID());
        sync(player, target);
    }

    private static LivingEntity nextTarget(List<LivingEntity> candidates, UUID currentId) {
        if (currentId == null) {
            return candidates.get(0);
        }

        for (int i = 0; i < candidates.size(); i++) {
            if (currentId.equals(candidates.get(i).getUUID())) {
                return candidates.get((i + 1) % candidates.size());
            }
        }
        return candidates.get(0);
    }

    private static List<LivingEntity> collectCandidates(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Map<UUID, LivingEntity> entities = new LinkedHashMap<>();
        AABB area = player.getBoundingBox().inflate(ACQUIRE_RANGE);
        level.getEntitiesOfClass(LivingEntity.class, area, entity -> isValidTarget(player, entity, ACQUIRE_RANGE))
                .forEach(entity -> entities.put(entity.getUUID(), entity));
        for (ServerPlayer other : level.players()) {
            if (isValidTarget(player, other, ACQUIRE_RANGE)) {
                entities.put(other.getUUID(), other);
            }
        }

        return entities.values().stream()
                .filter(entity -> aimDot(player, entity) >= MIN_TARGET_DOT)
                .sorted(Comparator.comparingDouble((LivingEntity entity) -> targetScore(player, entity)).reversed())
                .toList();
    }

    private static boolean isValidTarget(ServerPlayer player, LivingEntity entity, double range) {
        return entity != player
                && entity.isAlive()
                && !entity.isSpectator()
                && entity.distanceToSqr(player) <= range * range;
    }

    private static double targetScore(ServerPlayer player, LivingEntity entity) {
        double distance = Math.sqrt(Math.max(0.001, entity.distanceToSqr(player)));
        double dot = aimDot(player, entity);
        double power = Math.log10(1.0 + Math.max(1.0, targetPower(entity)));
        double playerBonus = entity instanceof Player ? 7.0 : 0.0;
        return dot * 120.0 + power * 10.0 + playerBonus - distance * 1.15;
    }

    private static double aimDot(ServerPlayer player, LivingEntity entity) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 target = entity.getBoundingBox().getCenter();
        Vec3 toTarget = target.subtract(eye);
        if (toTarget.lengthSqr() < 0.0001) {
            return 1.0;
        }
        return look.dot(toTarget.normalize());
    }

    private static void sync(ServerPlayer player, LivingEntity target) {
        if (target == null) {
            KicraftNetwork.sendToPlayer(player, new LockOnSyncPacket(-1, "", 0.0));
            return;
        }
        KicraftNetwork.sendToPlayer(player, new LockOnSyncPacket(target.getId(), target.getDisplayName().getString(), targetPower(target)));
    }
}
