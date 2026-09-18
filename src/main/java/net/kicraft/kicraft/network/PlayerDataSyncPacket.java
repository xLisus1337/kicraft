package net.kicraft.kicraft.network;

import net.kicraft.kicraft.client.ClientKiData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PlayerDataSyncPacket(CompoundTag data, boolean inCombat) {
    public PlayerDataSyncPacket(FriendlyByteBuf buffer) {
        this(buffer.readNbt(), buffer.readBoolean());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeNbt(data);
        buffer.writeBoolean(inCombat);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientKiData.load(data, inCombat)));
        context.setPacketHandled(true);
        return true;
    }
}
