package net.kicraft.kicraft.network;

import net.kicraft.kicraft.client.ClientFlightStates;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Synchronizes the custom flight animation for the owner and nearby players. */
public record FlightStateSyncPacket(UUID playerId, boolean flying) {
    public FlightStateSyncPacket(FriendlyByteBuf buffer) {
        this(buffer.readUUID(), buffer.readBoolean());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(playerId);
        buffer.writeBoolean(flying);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientFlightStates.set(playerId, flying)));
        context.setPacketHandled(true);
        return true;
    }
}
