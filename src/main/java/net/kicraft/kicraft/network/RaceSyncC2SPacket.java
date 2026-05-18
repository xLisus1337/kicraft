package net.kicraft.kicraft.network;

import net.kicraft.kicraft.capability.PlayerDataProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RaceSyncC2SPacket {
    private final String race;

    public RaceSyncC2SPacket(String race) {
        this.race = race;
    }

    public RaceSyncC2SPacket(FriendlyByteBuf buf) {
        this.race = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.race);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                    data.setRace(this.race);
                    data.setHasChosenRace(true);

                    // Po wyborze rasy synchronizujemy dane nowym sposobem
                    CompoundTag nbt = new CompoundTag();
                    data.saveNBTData(nbt);
                    PacketHandler.sendToPlayer(new PlayerDataSyncS2CPacket(nbt), player);
                });
            }
        });
        return true;
    }
}