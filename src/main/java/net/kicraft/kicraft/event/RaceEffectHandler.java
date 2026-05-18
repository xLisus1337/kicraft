package net.kicraft.kicraft.event;

import net.kicraft.kicraft.capability.PlayerDataProvider;
import net.kicraft.kicraft.network.PacketHandler;
import net.kicraft.kicraft.network.PlayerDataSyncS2CPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public class RaceEffectHandler {
    public static void sync(ServerPlayer player) {
        player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
            CompoundTag nbt = new CompoundTag();
            data.saveNBTData(nbt);
            PacketHandler.sendToPlayer(new PlayerDataSyncS2CPacket(nbt), player);
        });
    }
}