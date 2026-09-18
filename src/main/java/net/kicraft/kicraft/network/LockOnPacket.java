package net.kicraft.kicraft.network;

import net.kicraft.kicraft.server.LockOnState;
import net.kicraft.kicraft.server.capability.PlayerDataCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record LockOnPacket(Action action) {
    public LockOnPacket(FriendlyByteBuf buffer) {
        this(buffer.readEnum(Action.class));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(action);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            boolean canLock = player.isAlive() && !player.isSpectator() && !player.isSleeping()
                    && player.getCapability(PlayerDataCapability.PLAYER_DATA)
                    .map(data -> data.isRaceChosen()).orElse(false);
            if (!canLock) {
                LockOnState.clear(player);
                return;
            }

            if (action == Action.CYCLE) {
                LockOnState.cycle(player);
            } else {
                LockOnState.toggle(player);
            }
        });
        context.setPacketHandled(true);
        return true;
    }

    public enum Action {
        TOGGLE,
        CYCLE
    }
}
