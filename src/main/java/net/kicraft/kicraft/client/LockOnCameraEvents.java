package net.kicraft.kicraft.client;

import net.kicraft.kicraft.Kicraft;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Kicraft.MODID, value = Dist.CLIENT)
public final class LockOnCameraEvents {
    private static final float CAMERA_SMOOTHING = 0.18F;
    private static final float CAMERA_DEADZONE_DEGREES = 0.06F;

    private static boolean initialized;
    private static float visualYaw;
    private static float visualPitch;

    private LockOnCameraEvents() {
    }

    @SubscribeEvent
    public static void computeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !ClientSettings.lockOnCameraTracking() || !ClientLockOnData.isLocked()) {
            reset();
            return;
        }

        Entity entity = ClientLockOnData.targetEntity();
        if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
            ClientLockOnData.clear();
            reset();
            return;
        }

        Vec3 cameraPosition = event.getCamera().getPosition();
        Vec3 targetPosition = ClientLockOnData.targetAimPoint(target, (float) event.getPartialTick());
        Vec3 delta = targetPosition.subtract(cameraPosition);
        if (delta.lengthSqr() < 0.0001) {
            return;
        }

        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float desiredYaw = (float) (Math.atan2(delta.z, delta.x) * (180.0 / Math.PI)) - 90.0F;
        float desiredPitch = (float) (-(Math.atan2(delta.y, horizontal) * (180.0 / Math.PI)));
        desiredPitch = Mth.clamp(desiredPitch, -85.0F, 85.0F);

        if (!initialized) {
            visualYaw = event.getYaw();
            visualPitch = event.getPitch();
            initialized = true;
        }

        // A shoulder camera is physically offset from the player. Aligning only
        // the player rotation aims beside the target, so calculate from the final
        // camera position and snap this visual correction to the exact aim point.
        float smoothing = ShoulderCameraState.isActive(minecraft) ? 1.0F : CAMERA_SMOOTHING;
        visualYaw = smoothDegrees(visualYaw, desiredYaw, smoothing);
        visualPitch = smoothLinear(visualPitch, desiredPitch, smoothing);

        event.setYaw(visualYaw);
        event.setPitch(visualPitch);
    }

    public static void reset() {
        initialized = false;
    }

    private static float smoothDegrees(float current, float target, float alpha) {
        float delta = Mth.wrapDegrees(target - current);
        if (Math.abs(delta) <= CAMERA_DEADZONE_DEGREES) {
            return current;
        }
        return current + delta * alpha;
    }

    private static float smoothLinear(float current, float target, float alpha) {
        float delta = target - current;
        if (Math.abs(delta) <= CAMERA_DEADZONE_DEGREES) {
            return current;
        }
        return current + delta * alpha;
    }
}
