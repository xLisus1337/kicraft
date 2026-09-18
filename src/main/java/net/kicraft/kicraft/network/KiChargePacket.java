package net.kicraft.kicraft.network;

import net.kicraft.kicraft.server.ServerKiState;
import net.kicraft.kicraft.server.capability.PlayerDataCapability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record KiChargePacket(boolean charging) {
    public KiChargePacket(FriendlyByteBuf buffer) {
        this(buffer.readBoolean());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(charging);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            // Releases still apply after death or a game-mode change.
            if (!charging || !player.isAlive() || player.isSpectator() || player.isSleeping()) {
                ServerKiState.setCharging(player, false);
                return;
            }
            player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
                if (data.isRaceChosen()) {
                    ServerKiState.setCharging(player, charging && !net.kicraft.kicraft.server.MeleeCombat.actionLocked(player));
                } else {
                    ServerKiState.setCharging(player, false);
                }
            });
        });
        context.setPacketHandled(true);
        return true;
    }
}
