package net.kicraft.kicraft.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class ShoulderCameraState {
    private static final double SIDE_DISTANCE = 1.12;
    private static final double BACK_DISTANCE = -0.18;
    private static final double UP_DISTANCE = 0.10;
    private static final double POSITION_SMOOTHING = 0.18;
    private static final double SIDE_SWITCH_SMOOTHING = 0.12;
    private static final double LATERAL_VELOCITY_DEADZONE = 0.012;

    private static Mode mode = Mode.OFF;
    private static double currentSide;
    private static double currentBack;
    private static double currentUp;
    private static double autoSidePreference = -1.0;

    private ShoulderCameraState() {
    }

    public static void cycle(Minecraft minecraft) {
        mode = switch (mode) {
            case OFF -> Mode.RIGHT;
            case RIGHT -> Mode.LEFT;
            case LEFT -> Mode.AUTO;
            case AUTO -> Mode.OFF;
        };

        if (mode == Mode.OFF) {
            reset();
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        } else {
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }

        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.translatable(mode.messageKey()), true);
        }
    }

    public static Offset updateOffset(Minecraft minecraft) {
        if (minecraft.player == null || mode == Mode.OFF || minecraft.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
            reset();
            return Offset.ZERO;
        }

        double desiredSide = desiredSide(minecraft);
        currentSide = approach(currentSide, desiredSide, SIDE_SWITCH_SMOOTHING);
        currentBack = approach(currentBack, BACK_DISTANCE, POSITION_SMOOTHING);
        currentUp = approach(currentUp, UP_DISTANCE, POSITION_SMOOTHING);
        return new Offset(currentBack, currentUp, currentSide);
    }

    public static void reset() {
        currentSide = 0.0;
        currentBack = 0.0;
        currentUp = 0.0;
    }

    public static boolean isActive(Minecraft minecraft) {
        return mode != Mode.OFF
                && minecraft != null
                && minecraft.player != null
                && minecraft.options.getCameraType() == CameraType.THIRD_PERSON_BACK;
    }

    private static double desiredSide(Minecraft minecraft) {
        return switch (mode) {
            case OFF -> 0.0;
            case RIGHT -> -SIDE_DISTANCE;
            case LEFT -> SIDE_DISTANCE;
            case AUTO -> autoSide(minecraft);
        };
    }

    private static double autoSide(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return SIDE_DISTANCE * autoSidePreference;
        }

        double lateralVelocity = lateralVelocity(player);
        if (lateralVelocity > LATERAL_VELOCITY_DEADZONE) {
            autoSidePreference = 1.0;
        } else if (lateralVelocity < -LATERAL_VELOCITY_DEADZONE) {
            autoSidePreference = -1.0;
        } else if (minecraft.options.keyRight.isDown() && !minecraft.options.keyLeft.isDown()) {
            autoSidePreference = 1.0;
        } else if (minecraft.options.keyLeft.isDown() && !minecraft.options.keyRight.isDown()) {
            autoSidePreference = -1.0;
        }
        return SIDE_DISTANCE * autoSidePreference;
    }

    private static double lateralVelocity(LocalPlayer player) {
        Vec3 movement = player.getDeltaMovement();
        if (movement.horizontalDistanceSqr() < LATERAL_VELOCITY_DEADZONE * LATERAL_VELOCITY_DEADZONE) {
            return 0.0;
        }

        double yaw = Math.toRadians(player.getYRot());
        double rightX = Math.cos(yaw);
        double rightZ = Math.sin(yaw);
        return movement.x * rightX + movement.z * rightZ;
    }

    private static double approach(double current, double target, double smoothing) {
        if (!ClientSettings.smoothShoulderCamera()) {
            return target;
        }
        if (Math.abs(target - current) < 0.002) {
            return target;
        }
        return Mth.lerp(smoothing, current, target);
    }

    private enum Mode {
        OFF("message.kicraft.shoulder_camera.off"),
        RIGHT("message.kicraft.shoulder_camera.right"),
        LEFT("message.kicraft.shoulder_camera.left"),
        AUTO("message.kicraft.shoulder_camera.auto");

        private final String messageKey;

        Mode(String messageKey) {
            this.messageKey = messageKey;
        }

        private String messageKey() {
            return messageKey;
        }
    }

    public record Offset(double back, double up, double side) {
        private static final Offset ZERO = new Offset(0.0, 0.0, 0.0);

        public boolean isZero() {
            return Math.abs(back) < 0.001 && Math.abs(up) < 0.001 && Math.abs(side) < 0.001;
        }
    }
}
