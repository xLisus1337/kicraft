package net.kicraft.kicraft.network;

import net.kicraft.kicraft.capability.PlayerDataProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TurboModeC2SPacket {
    private final boolean isTurbo;

    public TurboModeC2SPacket(boolean isTurbo) {
        this.isTurbo = isTurbo;
    }

    public TurboModeC2SPacket(FriendlyByteBuf buf) {
        this.isTurbo = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(this.isTurbo);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                    data.setTurboMode(isTurbo);
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