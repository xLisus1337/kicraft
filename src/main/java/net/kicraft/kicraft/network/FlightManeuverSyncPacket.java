package net.kicraft.kicraft.network;

import net.kicraft.kicraft.client.ClientFlightManeuvers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Synchronizes the start and interruption of an aerial loop. */
public record FlightManeuverSyncPacket(UUID playerId, boolean looping, long startGameTime, int durationTicks,
                                       double forwardX, double forwardZ, double speed, double startAngle) {
    public FlightManeuverSyncPacket(FriendlyByteBuf buffer) {
        this(buffer.readUUID(), buffer.readBoolean(), buffer.readLong(), buffer.readVarInt(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(playerId);
        buffer.writeBoolean(looping);
        buffer.writeLong(startGameTime);
        buffer.writeVarInt(durationTicks);
        buffer.writeDouble(forwardX);
        buffer.writeDouble(forwardZ);
        buffer.writeDouble(speed);
        buffer.writeDouble(startAngle);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (looping) {
                ClientFlightManeuvers.startLoop(playerId, startGameTime, durationTicks,
                        forwardX, forwardZ, speed, startAngle);
            } else {
                ClientFlightManeuvers.stop(playerId);
            }
        }));
        context.setPacketHandled(true);
        return true;
    }
}
