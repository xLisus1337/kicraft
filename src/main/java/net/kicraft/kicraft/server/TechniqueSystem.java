package net.kicraft.kicraft.server;

import net.kicraft.kicraft.core.player.KiTechnique;
import net.kicraft.kicraft.core.player.PlayerData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

public final class TechniqueSystem {
    private TechniqueSystem() {
    }

    public static boolean require(ServerPlayer player, PlayerData data, KiTechnique technique) {
        return player.isAlive() && !player.isSpectator() && !player.isSleeping()
                && data.isRaceChosen() && data.hasTechnique(technique);
    }

    public static void unlock(ServerPlayer player, PlayerData data, KiTechnique technique) {
        data.unlockTechnique(technique);
        sync(player, data);
    }

    public static void unlockAll(ServerPlayer player, PlayerData data) {
        data.unlockAllTechniques();
        sync(player, data);
    }

    public static void lock(ServerPlayer player, PlayerData data, KiTechnique technique) {
        data.lockTechnique(technique);
        sync(player, data);
    }

    public static void clear(ServerPlayer player, PlayerData data) {
        data.clearTechniques();
        sync(player, data);
    }

    public static Component name(KiTechnique technique) {
        return Component.translatable(technique.translationKey());
    }

    public static Component list(Set<KiTechnique> techniques) {
        if (techniques.isEmpty()) {
            return Component.translatable("common.kicraft.none");
        }

        MutableComponent result = Component.empty();
        boolean first = true;
        for (KiTechnique technique : techniques) {
            if (!first) {
                result.append(Component.literal(", "));
            }
            result.append(name(technique));
            first = false;
        }
        return result;
    }

    private static void sync(ServerPlayer player, PlayerData data) {
        PlayerDataEvents.sync(player);
        data.clearDirty();
    }
}
