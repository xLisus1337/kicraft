package net.kicraft.kicraft.network;

import net.kicraft.kicraft.capability.PlayerDataProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;
import net.kicraft.kicraft.event.GameplayEvents;

import java.util.UUID;
import java.util.function.Supplier;

public class InstantTransmissionC2SPacket {
    private final UUID targetUUID;

    public InstantTransmissionC2SPacket(UUID targetUUID) {
        this.targetUUID = targetUUID;
    }

    public InstantTransmissionC2SPacket(FriendlyByteBuf buf) {
        this.targetUUID = buf.readUUID();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.targetUUID);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            // Szukamy gracza, do którego chcemy się teleportować
            ServerPlayer target = player.server.getPlayerList().getPlayer(targetUUID);

            if (target != null) {
                player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                    // Koszt Ki: Zwykła (500), Między planetami (2000)
                    double cost = (player.level().dimension() == target.level().dimension()) ? 500.0 : 2000.0;

                    // Sprawdzamy, czy gracza stać na skok
                    if (data.getKi() >= cost) {

                        // 1. Pobieramy opłatę (TYLKO RAZ)
                        data.setKi(data.getKi() - cost);

                        // 2. Dajemy nagrodę w postaci XP za użycie techniki
                        data.addKiXP(cost * 0.1);
                        data.addKiControlXP(10.0);

                        // 3. Efekt dźwiękowy przed skokiem
                        player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);

                        // 4. Teleportacja (nawet między wymiarami)
                        player.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());

                        // 5. Efekt dźwiękowy po skoku
                        player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);

                        // 6. Synchronizacja danych
                        GameplayEvents.sync(player);
                    }
                });
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}