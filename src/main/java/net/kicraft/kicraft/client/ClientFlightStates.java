package net.kicraft.kicraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Client-side flight state used by the custom player animation. */
public final class ClientFlightStates {
    private static final Set<UUID> FLYING_PLAYERS = new HashSet<>();

    private ClientFlightStates() {
    }

    public static boolean isFlying(AbstractClientPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && player.getUUID().equals(minecraft.player.getUUID())) {
            return player.getAbilities().flying || FLYING_PLAYERS.contains(player.getUUID());
        }
        return FLYING_PLAYERS.contains(player.getUUID());
    }

    public static void set(UUID playerId, boolean flying) {
        if (flying) {
            FLYING_PLAYERS.add(playerId);
        } else {
            FLYING_PLAYERS.remove(playerId);
            ClientFlightOrientation.stop(playerId);
        }
    }

    public static void reset() {
        FLYING_PLAYERS.clear();
        ClientFlightOrientation.reset();
    }
}
