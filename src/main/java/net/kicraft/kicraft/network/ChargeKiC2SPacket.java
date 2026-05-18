package net.kicraft.kicraft.network;

import net.kicraft.kicraft.capability.PlayerDataProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ChargeKiC2SPacket {
    private final boolean isCharging;

    public ChargeKiC2SPacket(boolean isCharging) {
        this.isCharging = isCharging;
    }

    public ChargeKiC2SPacket(FriendlyByteBuf buf) {
        this.isCharging = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(this.isCharging);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                    data.setChargingKi(isCharging);
                    sync(player);
                });
            }
        });
        return true;
    }

    private static void sync(ServerPlayer player) {
        player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
            CompoundTag nbt = new CompoundTag();
            data.saveNBTData(nbt);
            PacketHandler.sendToPlayer(new PlayerDataSyncS2CPacket(nbt), player);
        });
    }
}