package net.kicraft.kicraft.network;

import net.kicraft.kicraft.core.player.KiStat;
import net.kicraft.kicraft.core.player.KiTechnique;
import net.kicraft.kicraft.core.player.PlayerData;
import net.kicraft.kicraft.server.KiActionUtil;
import net.kicraft.kicraft.server.PlayerDataEvents;
import net.kicraft.kicraft.server.TechniqueSystem;
import net.kicraft.kicraft.server.capability.PlayerDataCapability;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public record KiSensePacket() {
    private static final double MIN_COST = 3.0;
    private static final double COST_RATIO = 0.025;
    private static final int HIGHLIGHT_TICKS = 100;
    private static final String COOLDOWN_TAG = "kicraftKiSenseCooldownUntil";

    public KiSensePacket(FriendlyByteBuf buffer) {
        this();
    }

    public void encode(FriendlyByteBuf buffer) {
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> scan(context.getSender()));
        context.setPacketHandled(true);
        return true;
    }

    public static void scan(ServerPlayer player) {
        if (player == null || !player.isAlive() || player.isSpectator()) {
            return;
        }
        long now = player.level().getGameTime();
        if (now < player.getPersistentData().getLong(COOLDOWN_TAG)) return;

        player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
            if (!TechniqueSystem.require(player, data, KiTechnique.KI_SENSE)) {
                return;
            }

            double cost = Math.max(MIN_COST, data.maxKi() * COST_RATIO);
            if (!data.race().hasInfiniteEnergy()) {
                if (data.ki() < cost) {
                    return;
                }
                data.setKi(data.ki() - cost);
            }
            player.getPersistentData().putLong(COOLDOWN_TAG, now + 20);

            double range = Math.min(128.0, 32.0 + Math.sqrt(data.maxKi()) * 0.8 + data.stat(KiStat.KI_CONTROL) * 0.45);
            ServerLevel level = player.serverLevel();
            List<LivingEntity> sensed = collectSensedEntities(level, player, range)
                    .stream()
                    .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
                    .limit(KiSenseHighlightPacket.MAX_ENTRIES)
                    .toList();

            double selfPower = Math.max(1.0, data.powerLevel());
            KicraftNetwork.sendToPlayer(player, new KiSenseHighlightPacket(level.dimension().location(),
                    HIGHLIGHT_TICKS, sensed.stream().map(entity -> new KiSenseHighlightPacket.Entry(
                    entity.getUUID(), senseColor(selfPower, powerScore(entity)).getColor())).toList()));
            for (LivingEntity entity : sensed) {
                level.sendParticles(player, ParticleTypes.SOUL_FIRE_FLAME, true, entity.getX(), entity.getY() + entity.getBbHeight() * 0.65, entity.getZ(), 3, 0.25, 0.35, 0.25, 0.01);
            }

            if (sensed.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.kicraft.ki_sense.none"), true);
            } else {
                LivingEntity strongest = sensed.stream()
                        .max(Comparator.comparingDouble(KiSensePacket::powerScore))
                        .orElse(sensed.get(0));
                ChatFormatting color = senseColor(selfPower, powerScore(strongest));
                player.displayClientMessage(Component.translatable("message.kicraft.ki_sense.strongest", strongest.getDisplayName()).withStyle(color), true);
            }

            data.train(KiStat.KI_CONTROL, 0.012);
            data.trainFocus(0.012);
            PlayerDataEvents.sync(player);
            data.clearDirty();
        });
    }


    private static double powerScore(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            return player.getCapability(PlayerDataCapability.PLAYER_DATA)
                    .map(PlayerData::powerLevel)
                    .orElseGet(() -> KiActionUtil.powerScore(entity));
        }
        return KiActionUtil.powerScore(entity);
    }

    private static List<LivingEntity> collectSensedEntities(ServerLevel level, ServerPlayer player, double range) {
        Map<UUID, LivingEntity> entities = new LinkedHashMap<>();
        AABB scanArea = player.getBoundingBox().inflate(range);
        level.getEntitiesOfClass(LivingEntity.class, scanArea,
                        entity -> canSense(player, entity) && entity.distanceToSqr(player) <= range * range)
                .forEach(entity -> entities.put(entity.getUUID(), entity));

        double rangeSqr = range * range;
        for (ServerPlayer other : level.players()) {
            if (canSense(player, other) && other.distanceToSqr(player) <= rangeSqr) {
                entities.put(other.getUUID(), other);
            }
        }

        AABB dragonScanArea = player.getBoundingBox().inflate(range + 160.0);
        double dragonRange = range + 160.0;
        double dragonRangeSqr = dragonRange * dragonRange;
        level.getEntitiesOfClass(EnderDragon.class, dragonScanArea, dragon -> canSense(player, dragon) && dragon.distanceToSqr(player) <= dragonRangeSqr)
                .forEach(dragon -> entities.put(dragon.getUUID(), dragon));

        return List.copyOf(entities.values());
    }

    private static boolean canSense(ServerPlayer player, LivingEntity entity) {
        return entity != player && entity.isAlive() && !entity.isSpectator();
    }

    private static ChatFormatting senseColor(double selfPower, double targetPower) {
        double ratio = targetPower / Math.max(1.0, selfPower);
        if (ratio >= 200.0) {
            return ChatFormatting.BLACK;
        }
        if (ratio >= 1.25) {
            return ChatFormatting.RED;
        }
        if (ratio >= 0.85) {
            return ChatFormatting.GRAY;
        }
        return ChatFormatting.GREEN;
    }
}
