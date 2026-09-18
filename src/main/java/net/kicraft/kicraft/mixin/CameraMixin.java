package net.kicraft.kicraft.mixin;

import net.kicraft.kicraft.client.ShoulderCameraState;
import net.kicraft.kicraft.client.ClientFlightOrientation;
import net.kicraft.kicraft.client.ClientFlightStates;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public abstract class CameraMixin {
    private static final double FLIGHT_CAMERA_DISTANCE = 5.25D;
    private static final double FLIGHT_CAMERA_UP_OFFSET = 2.00D;
    private static final float MAX_FLIGHT_CAMERA_LOOK_DOWN = 22.0F;

    @Shadow
    protected abstract void move(double forward, double up, double left);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Invoker("getMaxZoom")
    protected abstract double kicraft$getMaxZoom(double desiredDistance);

    @Inject(method = "setup", at = @At("TAIL"), require = 0)
    private void kicraft$applyShoulderCamera(BlockGetter level, Entity entity, boolean detached, boolean mirror, float partialTick, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player != null && entity == player && ClientFlightStates.isFlying(player)) {
            ClientFlightOrientation.Attitude attitude = ClientFlightOrientation.attitude(player, partialTick);
            float cameraYaw = mirror ? attitude.yaw() + 180.0F : attitude.yaw();
            float cameraPitch = mirror ? -attitude.pitch() : attitude.pitch();
            setRotation(cameraYaw, cameraPitch);
            setPosition(
                    Mth.lerp(partialTick, entity.xo, entity.getX()),
                    Mth.lerp(partialTick, entity.yo, entity.getY()) + entity.getEyeHeight(),
                    Mth.lerp(partialTick, entity.zo, entity.getZ())
            );
            if (detached) {
                double cameraDistance = kicraft$getMaxZoom(FLIGHT_CAMERA_DISTANCE);
                move(-cameraDistance, 0.0D, 0.0D);
                move(0.0D, FLIGHT_CAMERA_UP_OFFSET, 0.0D);

                // Merely lifting a parallel camera still shows the character almost
                // edge-on. Aim the raised camera back at the flight axis so the view
                // is genuinely from slightly above while keeping the model centered.
                float lookDown = (float) Math.toDegrees(Math.atan2(
                        FLIGHT_CAMERA_UP_OFFSET, Math.max(0.35D, cameraDistance)));
                lookDown = Math.min(lookDown, MAX_FLIGHT_CAMERA_LOOK_DOWN);
                setRotation(cameraYaw, cameraPitch + lookDown);
            }
        }

        if (!detached) {
            ShoulderCameraState.reset();
            return;
        }

        ShoulderCameraState.Offset offset = ShoulderCameraState.updateOffset(minecraft);
        if (!offset.isZero()) {
            move(offset.back(), offset.up(), offset.side());
        }
    }
}
