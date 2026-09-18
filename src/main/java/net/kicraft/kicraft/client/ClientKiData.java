package net.kicraft.kicraft.client;

import net.kicraft.kicraft.core.player.PlayerData;
import net.minecraft.nbt.CompoundTag;

public final class ClientKiData {
    private static PlayerData data = new PlayerData();
    private static boolean inCombat;
    private static boolean receivedInitialSync;

    private ClientKiData() {
    }

    public static PlayerData data() {
        return data;
    }

    public static boolean isInCombat() {
        return inCombat;
    }

    public static boolean hasReceivedInitialSync() {
        return receivedInitialSync;
    }

    public static void load(CompoundTag tag, boolean combat) {
        data.load(tag);
        data.clearDirty();
        inCombat = combat;
        receivedInitialSync = true;
    }

    public static void reset() {
        data = new PlayerData();
        inCombat = false;
        receivedInitialSync = false;
        ClientFlightStates.reset();
        ClientFlightOrientation.reset();
        ClientStatLevelUpNotifier.reset();
    }
}
