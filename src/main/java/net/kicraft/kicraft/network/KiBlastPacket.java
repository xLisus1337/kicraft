package net.kicraft.kicraft.network;

import net.kicraft.kicraft.core.player.KiStat;
import net.kicraft.kicraft.core.player.KiTechnique;
import net.kicraft.kicraft.core.player.ResourceCosts;
import net.kicraft.kicraft.server.KiActionUtil;
import net.kicraft.kicraft.server.KiBlastProjectileEvents;
import net.kicraft.kicraft.server.PlayerDataEvents;
import net.kicraft.kicraft.server.TechniqueSystem;
import net.kicraft.kicraft.server.capability.PlayerDataCapability;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record KiBlastPacket() {
    private static final String COOLDOWN_TAG = "kicraftKiBlastCooldown";
    private static final int BASE_COOLDOWN_TICKS = 7;
    private static final int LEGACY_COOLDOWN_GRACE_TICKS = 80;

    public KiBlastPacket(FriendlyByteBuf buffer) {
        this();
    }

    public void encode(FriendlyByteBuf buffer) {
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.isAlive() || player.isSpectator()
                    || net.kicraft.kicraft.server.MeleeCombat.actionLocked(player)) {
                return;
            }

            player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
                if (!TechniqueSystem.require(player, data, KiTechnique.KI_BLAST)) {
                    return;
                }

                CompoundTag persistentData = player.getPersistentData();
                long gameTime = player.level().getGameTime();
                long nextShotTime = persistentData.getLong(COOLDOWN_TAG);
                if (nextShotTime > gameTime + LEGACY_COOLDOWN_GRACE_TICKS) {
                    persistentData.remove(COOLDOWN_TAG);
                    nextShotTime = 0L;
                }
                if (gameTime < nextShotTime) {
                    return;
                }

                double cost = ResourceCosts.basicKiBlastCost(data);
                if (!data.race().hasInfiniteEnergy()) {
                    if (data.ki() < cost) {
                        return;
                    }
                    data.setKi(data.ki() - cost);
                }

                Vec3 start = player.getEyePosition().add(player.getLookAngle().scale(0.7));
                KiBlastProjectileEvents.spawn(player, start, player.getLookAngle(), KiActionUtil.kiTechniquePower(data));
                persistentData.putLong(COOLDOWN_TAG, gameTime + BASE_COOLDOWN_TICKS);

                data.train(KiStat.MAX_KI, 0.020);
                data.train(KiStat.KI_CONTROL, 0.018);
                PlayerDataEvents.sync(player);
                data.clearDirty();
            });
        });
        context.setPacketHandled(true);
        return true;
    }
}
