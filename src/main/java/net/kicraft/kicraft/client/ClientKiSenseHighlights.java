package net.kicraft.kicraft.client;

import net.kicraft.kicraft.Kicraft;
import net.kicraft.kicraft.network.KiSenseHighlightPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Kicraft.MODID, value = Dist.CLIENT)
public final class ClientKiSenseHighlights {
    private static final Map<UUID, Integer> COLORS = new HashMap<>();
    private static ClientLevel level;
    private static long expiresAt;

    private ClientKiSenseHighlights() { }

    public static void receive(KiSenseHighlightPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.level.dimension().location().equals(packet.dimension())) return;
        level = minecraft.level;
        expiresAt = level.getGameTime() + packet.durationTicks();
        COLORS.clear();
        for (KiSenseHighlightPacket.Entry entry : packet.entries()) COLORS.put(entry.entityId(), entry.color());
    }

    /** -1 leaves vanilla outlines (including potion effects and spectator mode) untouched. */
    public static int color(Entity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (level == null || minecraft.level != level || entity.level() != level
                || minecraft.player == null || !minecraft.player.isAlive() || minecraft.player.isSpectator()
                || !entity.isAlive() || level.getGameTime() >= expiresAt) return -1;
        return COLORS.getOrDefault(entity.getUUID(), -1);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || level == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != level || minecraft.player == null || !minecraft.player.isAlive()
                || minecraft.player.isSpectator() || level.getGameTime() >= expiresAt) {
            COLORS.clear();
            level = null;
        }
    }
}
