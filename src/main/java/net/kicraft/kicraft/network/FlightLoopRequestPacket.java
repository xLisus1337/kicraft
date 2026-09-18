package net.kicraft.kicraft.network;

import net.kicraft.kicraft.server.FlightManeuverSystem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Requests an upward loop after the client keeps steering steeply upward with the camera. */
public record FlightLoopRequestPacket() {
    public FlightLoopRequestPacket(FriendlyByteBuf buffer) {
        this();
    }

    public void encode(FriendlyByteBuf buffer) {
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                FlightManeuverSystem.tryStartLoop(player);
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
