package net.kicraft.kicraft.network;

import net.kicraft.kicraft.core.player.KiRace;
import net.kicraft.kicraft.server.KicraftAdvancements;
import net.kicraft.kicraft.server.PlayerDataEvents;
import net.kicraft.kicraft.server.capability.PlayerDataCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SelectRacePacket(KiRace race) {
    public SelectRacePacket(FriendlyByteBuf buffer) {
        this(buffer.readEnum(KiRace.class));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(race);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() == null || race == KiRace.NONE) {
                return;
            }
            ServerPlayer player = context.getSender();
            player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
                // Race selection is a one-time character creation action. Admin race changes use /kicraft setrace.
                if (data.isRaceChosen()) {
                    return;
                }
                data.setRace(race);
                if (race.canRollLegendarySaiyan() && player.getRandom().nextDouble() < 0.01) {
                    data.setLegendarySaiyan(true);
                    KicraftAdvancements.awardBerserk(player);
                }
                data.initializeRaceStats(player.getRandom());
                PlayerDataEvents.sync(player);
                data.clearDirty();
            });
        });
        context.setPacketHandled(true);
        return true;
    }
}
