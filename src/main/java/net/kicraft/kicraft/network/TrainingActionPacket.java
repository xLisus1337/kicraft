package net.kicraft.kicraft.network;

import net.kicraft.kicraft.core.training.TrainingActivity;
import net.kicraft.kicraft.server.ServerTrainingState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record TrainingActionPacket(Action action, TrainingActivity activity) {
    public TrainingActionPacket(Action action) {
        this(action, TrainingActivity.PUSH_UPS);
    }

    public TrainingActionPacket(FriendlyByteBuf buffer) {
        this(buffer.readEnum(Action.class), buffer.readEnum(TrainingActivity.class));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(action);
        buffer.writeEnum(activity);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            if (action == Action.STOP) {
                ServerTrainingState.stop(player);
                return;
            }
            if (!player.isAlive() || player.isSpectator()) return;

            switch (action) {
                case START -> ServerTrainingState.start(player, activity);
                case STOP -> ServerTrainingState.stop(player);
                case SPAWN_DUMMY -> ServerTrainingState.spawnDummy(player);
            }
        });
        context.setPacketHandled(true);
        return true;
    }

    public enum Action {
        START,
        STOP,
        SPAWN_DUMMY
    }
}
