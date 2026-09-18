package net.kicraft.kicraft.network;

import net.kicraft.kicraft.client.ClientFlightOrientation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Replicates continuous pitch through vertical so remote players can also render upside down. */
public record FlightOrientationSyncPacket(UUID playerId, float yaw, float pitch) {
    public FlightOrientationSyncPacket(FriendlyByteBuf buffer) {
        this(buffer.readUUID(), buffer.readFloat(), buffer.readFloat());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(playerId);
        buffer.writeFloat(yaw);
        buffer.writeFloat(pitch);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientFlightOrientation.setRemote(playerId, yaw, pitch)));
        context.setPacketHandled(true);
        return true;
    }
}
