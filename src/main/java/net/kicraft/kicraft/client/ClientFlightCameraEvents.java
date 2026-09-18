package net.kicraft.kicraft.client;

import net.kicraft.kicraft.Kicraft;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Small camera cues that communicate banking and acceleration without fighting mouse control. */
@Mod.EventBusSubscriber(modid = Kicraft.MODID, value = Dist.CLIENT)
public final class ClientFlightCameraEvents {
    private ClientFlightCameraEvents() {
    }

    @SubscribeEvent
    public static void cameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !ClientFlightStates.isFlying(minecraft.player)) {
            return;
        }
        event.setRoll(event.getRoll() + ClientFlightVisuals.cameraBankDegrees((float) event.getPartialTick()));
    }

    @SubscribeEvent
    public static void fieldOfView(ViewportEvent.ComputeFov event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !ClientFlightStates.isFlying(minecraft.player)) {
            return;
        }
        event.setFOV(event.getFOV() + ClientFlightVisuals.boost((float) event.getPartialTick()) * 4.5D);
    }
}
