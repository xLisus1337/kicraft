package net.kicraft.kicraft.client;

import net.kicraft.kicraft.Kicraft;
import net.kicraft.kicraft.network.OozaruFocusPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Kicraft.MODID, value = Dist.CLIENT)
public final class ClientOozaruFocus {
    private static LocalPlayer owner;
    private static ClientLevel level;
    private static int timeout;
    private static float yaw, pitch;

    private ClientOozaruFocus() { }

    public static void receive(OozaruFocusPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || packet.playerId() != mc.player.getId()) return;
        if (packet.remainingTicks() <= 0) { clear(); return; }
        if (!Float.isFinite(packet.yaw()) || !Float.isFinite(packet.pitch())) return;
        owner = mc.player;
        level = mc.level;
        // Server sends completion/cancellation immediately; the lease is a lost-state fallback.
        timeout = Math.min(60, packet.remainingTicks()) + 40;
        yaw = packet.yaw();
        pitch = packet.pitch();
        ClientLockOnData.clear();
        applyRotation();
    }

    public static boolean isLocked() {
        Minecraft mc = Minecraft.getInstance();
        if (timeout <= 0 || owner == null || owner != mc.player || level != mc.level
                || !owner.isAlive() || owner.isSpectator()) {
            clear();
            return false;
        }
        return true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !isLocked() || Minecraft.getInstance().isPaused()) return;
        applyRotation();
        if (--timeout <= 0) clear();
    }

    private static void applyRotation() {
        owner.setYRot(yaw);
        owner.setXRot(pitch);
        owner.yRotO = yaw;
        owner.xRotO = pitch;
        ClientFlightOrientation.followVanilla(owner);
    }

    private static void clear() { owner = null; level = null; timeout = 0; }
}
