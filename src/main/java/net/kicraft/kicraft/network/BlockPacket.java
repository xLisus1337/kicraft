package net.kicraft.kicraft.network;

import net.kicraft.kicraft.server.CombatState;
import net.kicraft.kicraft.server.capability.PlayerDataCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record BlockPacket(boolean blocking) {
    public BlockPacket(FriendlyByteBuf buffer) {
        this(buffer.readBoolean());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(blocking);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            // A release must always clear an old guard, including after a game-mode change.
            if (!blocking) {
                CombatState.setBlocking(player, false);
                return;
            }
            boolean canBlock = player.isAlive() && !player.isSpectator()
                    && player.getCapability(PlayerDataCapability.PLAYER_DATA)
                    .map(data -> data.isRaceChosen())
                    .orElse(false);
            CombatState.setBlocking(player, canBlock);
        });
        context.setPacketHandled(true);
        return true;
    }
}
