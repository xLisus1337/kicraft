package net.kicraft.kicraft.client;

import net.kicraft.kicraft.Kicraft;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Kicraft.MODID, value = Dist.CLIENT)
public final class VanillaHudEvents {
    private static double shoulderFov = 70.0D;
    private static float shoulderRoll;

    private VanillaHudEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void captureShoulderFov(ViewportEvent.ComputeFov event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (ShoulderCameraState.isActive(minecraft) && Double.isFinite(event.getFOV()) && event.getFOV() > 1.0D) {
            shoulderFov = event.getFOV();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void captureShoulderRoll(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        shoulderRoll = ShoulderCameraState.isActive(minecraft) && Float.isFinite(event.getRoll())
                ? event.getRoll() : 0.0F;
    }

    @SubscribeEvent
    public static void hideVanillaSurvivalBars(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() == VanillaGuiOverlay.PLAYER_HEALTH.type()
                || event.getOverlay() == VanillaGuiOverlay.FOOD_LEVEL.type()) {
            event.setCanceled(true);
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type() && !minecraft.options.hideGui
                && (minecraft.options.getCameraType().isFirstPerson() || ShoulderCameraState.isActive(minecraft))) {
            event.setCanceled(true);
            int width = event.getWindow().getGuiScaledWidth();
            int height = event.getWindow().getGuiScaledHeight();
            ScreenPoint point = ShoulderCameraState.isActive(minecraft)
                    ? projectPlayerAim(minecraft, width, height)
                    : new ScreenPoint(width / 2, height / 2);
            drawPlusCrosshair(event.getGuiGraphics(), point.x(), point.y());
        }
    }

    /**
     * The shoulder camera is physically beside the player, while Minecraft and the server
     * ray trace attacks from the player's eyes. Project that real ray hit into the offset
     * camera instead of pretending the centre of the screen is the attack direction.
     */
    private static ScreenPoint projectPlayerAim(Minecraft minecraft, int width, int height) {
        int centerX = width / 2;
        int centerY = height / 2;
        if (minecraft.player == null || width <= 0 || height <= 0) {
            return new ScreenPoint(centerX, centerY);
        }

        Camera camera = minecraft.gameRenderer.getMainCamera();
        if (!camera.isInitialized()) {
            return new ScreenPoint(centerX, centerY);
        }

        HitResult hit = minecraft.hitResult;
        Vec3 aimPoint = hit != null
                ? hit.getLocation()
                : minecraft.player.getEyePosition(1.0F).add(minecraft.player.getViewVector(1.0F)
                .scale(Math.max(4.0D, minecraft.player.getEntityReach())));
        Vec3 relative = aimPoint.subtract(camera.getPosition());

        Vec3 forward = Vec3.directionFromRotation(camera.getXRot(), camera.getYRot()).normalize();
        double yawRadians = Math.toRadians(camera.getYRot());
        Vec3 left = new Vec3(Math.cos(yawRadians), 0.0D, Math.sin(yawRadians));
        Vec3 up = forward.cross(left).normalize();
        double depth = relative.dot(forward);
        if (!Double.isFinite(depth) || depth <= 0.05D) {
            return new ScreenPoint(centerX, centerY);
        }

        // Camera coordinates before roll: positive X is screen-right and positive Y is screen-up.
        double viewX = -relative.dot(left);
        double viewY = relative.dot(up);
        double rollRadians = Math.toRadians(shoulderRoll);
        double rolledX = viewX * Math.cos(rollRadians) - viewY * Math.sin(rollRadians);
        double rolledY = viewX * Math.sin(rollRadians) + viewY * Math.cos(rollRadians);

        double verticalTan = Math.tan(Math.toRadians(shoulderFov) * 0.5D);
        double aspect = (double) minecraft.getWindow().getWidth()
                / Math.max(1.0D, minecraft.getWindow().getHeight());
        double horizontalTan = verticalTan * aspect;
        if (!Double.isFinite(verticalTan) || !Double.isFinite(horizontalTan)
                || verticalTan <= 0.0D || horizontalTan <= 0.0D) {
            return new ScreenPoint(centerX, centerY);
        }

        double projectedX = centerX + rolledX / (depth * horizontalTan) * width * 0.5D;
        double projectedY = centerY - rolledY / (depth * verticalTan) * height * 0.5D;
        if (!Double.isFinite(projectedX) || !Double.isFinite(projectedY)) {
            return new ScreenPoint(centerX, centerY);
        }

        int margin = 6;
        int x = Math.max(margin, Math.min(width - margin - 1, (int) Math.round(projectedX)));
        int y = Math.max(margin, Math.min(height - margin - 1, (int) Math.round(projectedY)));
        return new ScreenPoint(x, y);
    }

    private static void drawPlusCrosshair(GuiGraphics graphics, int centerX, int centerY) {
        graphics.fill(centerX - 5, centerY - 1, centerX + 6, centerY + 2, 0xC0000000);
        graphics.fill(centerX - 1, centerY - 5, centerX + 2, centerY + 6, 0xC0000000);
        graphics.fill(centerX - 4, centerY, centerX + 5, centerY + 1, 0xFFFFFFFF);
        graphics.fill(centerX, centerY - 4, centerX + 1, centerY + 5, 0xFFFFFFFF);
    }

    private record ScreenPoint(int x, int y) {
    }
}
