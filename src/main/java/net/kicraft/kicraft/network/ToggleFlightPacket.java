package net.kicraft.kicraft.network;

import net.kicraft.kicraft.server.PlayerDataEvents;
import net.kicraft.kicraft.server.ServerKiState;
import net.kicraft.kicraft.server.TechniqueSystem;
import net.kicraft.kicraft.server.capability.PlayerDataCapability;
import net.kicraft.kicraft.core.player.KiTechnique;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ToggleFlightPacket() {
    public ToggleFlightPacket(FriendlyByteBuf buffer) {
        this();
    }

    public void encode(FriendlyByteBuf buffer) {
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.isAlive() || player.isSpectator() || player.isCreative()) {
                return;
            }

            player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
                boolean next = !ServerKiState.isFlying(player);
                if (next && !TechniqueSystem.require(player, data, KiTechnique.FLIGHT)) {
                    return;
                }
                if (next && !data.race().hasInfiniteEnergy() && (data.ki() <= 0.0 || data.stamina() <= 0.0)) {
                    return;
                }
                ServerKiState.setFlying(player, next);
                player.getAbilities().mayfly = next || player.isCreative();
                if (next) {
                    player.getAbilities().flying = true;
                } else if (!player.isCreative()) {
                    player.getAbilities().flying = false;
                }
                player.onUpdateAbilities();
                PlayerDataEvents.syncFlightState(player);
                PlayerDataEvents.sync(player);
            });
        });
        context.setPacketHandled(true);
        return true;
    }
}
