package net.kicraft.kicraft.network;

import net.kicraft.kicraft.capability.PlayerDataProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PlayerDataSyncS2CPacket {
    private final CompoundTag nbt;

    public PlayerDataSyncS2CPacket(CompoundTag nbt) {
        this.nbt = nbt;
    }

    public PlayerDataSyncS2CPacket(FriendlyByteBuf buf) {
        this.nbt = buf.readNbt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(this.nbt);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                    data.loadNBTData(nbt);
                });
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}