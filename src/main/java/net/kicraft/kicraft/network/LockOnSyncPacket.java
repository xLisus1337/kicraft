package net.kicraft.kicraft.network;

import net.kicraft.kicraft.client.ClientLockOnData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record LockOnSyncPacket(int targetId, String targetName, double targetPower) {
    public LockOnSyncPacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readUtf(64), buffer.readDouble());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(targetId);
        buffer.writeUtf(targetName, 64);
        buffer.writeDouble(targetPower);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientLockOnData.set(targetId, targetName, targetPower)
        ));
        context.setPacketHandled(true);
        return true;
    }
}
