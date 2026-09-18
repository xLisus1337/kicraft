package net.kicraft.kicraft.network;

import net.kicraft.kicraft.core.player.KiTechnique;
import net.kicraft.kicraft.server.ServerKiState;
import net.kicraft.kicraft.server.TechniqueSystem;
import net.kicraft.kicraft.server.capability.PlayerDataCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record KiTransferPacket(boolean transferring) {
    public KiTransferPacket(FriendlyByteBuf buffer) {
        this(buffer.readBoolean());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(transferring);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                if (!transferring) {
                    ServerKiState.setTransferring(player, false);
                    return;
                }
                if (!player.isAlive() || player.isSpectator() || net.kicraft.kicraft.server.MeleeCombat.actionLocked(player)) {
                    ServerKiState.setTransferring(player, false);
                    return;
                }

                player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
                    if (!TechniqueSystem.require(player, data, KiTechnique.KI_TRANSFER)) {
                        ServerKiState.setTransferring(player, false);
                        return;
                    }
                    ServerKiState.setTransferring(player, true);
                });
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
