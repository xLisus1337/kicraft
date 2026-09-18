package net.kicraft.kicraft.server;

import net.minecraft.server.level.ServerPlayer;

public final class ServerKiState {
    private static final String CHARGING_TAG = "kicraftChargingKi";
    private static final String FLYING_TAG = "kicraftFlying";
    private static final String FLIGHT_BOOST_UNTIL_TAG = "kicraftFlightBoostUntil";
    private static final String TRANSFERRING_TAG = "kicraftTransferringKi";

    private ServerKiState() {
    }

    public static void setCharging(ServerPlayer player, boolean charging) {
        player.getPersistentData().putBoolean(CHARGING_TAG, charging);
    }

    public static boolean isCharging(ServerPlayer player) {
        return player.getPersistentData().getBoolean(CHARGING_TAG);
    }

    public static void setFlying(ServerPlayer player, boolean flying) {
        player.getPersistentData().putBoolean(FLYING_TAG, flying);
        if (!flying) {
            setFlightBoosting(player, false);
        }
    }

    public static boolean isFlying(ServerPlayer player) {
        return player.getPersistentData().getBoolean(FLYING_TAG);
    }

    public static void setFlightBoosting(ServerPlayer player, boolean boosting) {
        if (boosting) {
            // Flight input arrives every client tick. A short expiry prevents a stale
            // boost from surviving a lost focus, loading screen or interrupted client.
            player.getPersistentData().putLong(FLIGHT_BOOST_UNTIL_TAG,
                    player.level().getGameTime() + 3L);
        } else {
            player.getPersistentData().remove(FLIGHT_BOOST_UNTIL_TAG);
        }
    }

    public static boolean isFlightBoosting(ServerPlayer player) {
        long until = player.getPersistentData().getLong(FLIGHT_BOOST_UNTIL_TAG);
        return until > 0L && until >= player.level().getGameTime();
    }

    public static void setTransferring(ServerPlayer player, boolean transferring) {
        player.getPersistentData().putBoolean(TRANSFERRING_TAG, transferring);
    }

    public static boolean isTransferring(ServerPlayer player) {
        return player.getPersistentData().getBoolean(TRANSFERRING_TAG);
    }

    public static void clear(ServerPlayer player) {
        setCharging(player, false);
        setFlying(player, false);
        setFlightBoosting(player, false);
        setTransferring(player, false);
        CombatState.clear(player);
        ServerTrainingState.stop(player);
        KiBlastProjectileEvents.clear(player);
        player.getPersistentData().remove("kicraftKiSenseCooldownUntil");
    }
}
