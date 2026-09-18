package net.kicraft.kicraft.core.player;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Shared client/server steering rules for camera-directed Ki flight. */
public final class FlightMovement {
    private static final double CRUISE_SPEED = 0.52;
    private static final double SPRINT_SPEED = 1.25;
    private static final double BACKWARD_SPEED_SCALE = 0.72;
    private static final double STRAFE_SPEED_SCALE = 0.80;
    private static final double VERTICAL_SPEED_SCALE = 0.80;
    private static final double START_RESPONSE = 0.42;
    private static final double ACCELERATION_RESPONSE = 0.20;
    private static final double BRAKING_RESPONSE = 0.34;
    private static final double STEERING_RESPONSE = 0.48;
    private static final double REVERSAL_RESPONSE = 0.42;
    private static final double HOVER_DAMPING = 0.70;
    private static final double INPUT_DEADZONE_SQR = 0.0004;

    private FlightMovement() {
    }

    public static Vec3 calculateVelocity(Vec3 current, Vec3 look, float forwardInput,
                                         float leftInput, float verticalInput, boolean sprinting) {
        Vec3 normalizedLook = look.lengthSqr() > 0.000001
                ? look.normalize()
                : new Vec3(0.0, 0.0, 1.0);
        Vec3 horizontalLeft = new Vec3(normalizedLook.z, 0.0, -normalizedLook.x);
        if (horizontalLeft.lengthSqr() < 0.000001) {
            horizontalLeft = new Vec3(1.0, 0.0, 0.0);
        }
        return calculateVelocity(current, normalizedLook, horizontalLeft.normalize(),
                forwardInput, leftInput, verticalInput, sprinting);
    }

    public static Vec3 calculateVelocity(Vec3 current, Vec3 forwardBasis, Vec3 leftBasis,
                                         float forwardInput, float leftInput, float verticalInput,
                                         boolean sprinting) {
        float forward = Mth.clamp(forwardInput, -1.0F, 1.0F);
        float left = Mth.clamp(leftInput, -1.0F, 1.0F);
        float vertical = Mth.clamp(verticalInput, -1.0F, 1.0F);
        if (forward * forward + left * left + vertical * vertical < INPUT_DEADZONE_SQR) {
            Vec3 slowed = current.scale(HOVER_DAMPING);
            return slowed.lengthSqr() < 0.000025 ? Vec3.ZERO : slowed;
        }

        Vec3 normalizedLook = forwardBasis.lengthSqr() > 0.000001
                ? forwardBasis.normalize()
                : new Vec3(0.0, 0.0, 1.0);
        Vec3 normalizedLeft = leftBasis.lengthSqr() > 0.000001
                ? leftBasis.normalize()
                : new Vec3(normalizedLook.z, 0.0, -normalizedLook.x).normalize();

        double forwardScale = forward >= 0.0F ? forward : forward * BACKWARD_SPEED_SCALE;
        Vec3 requested = normalizedLook.scale(forwardScale)
                .add(normalizedLeft.scale(left * STRAFE_SPEED_SCALE))
                .add(0.0, vertical * VERTICAL_SPEED_SCALE, 0.0);
        double inputStrength = Math.min(1.0, requested.length());
        if (inputStrength < 0.0001) {
            return Vec3.ZERO;
        }

        Vec3 requestedDirection = requested.scale(1.0 / inputStrength);
        double targetSpeed = (sprinting ? SPRINT_SPEED : CRUISE_SPEED) * inputStrength;
        double currentSpeed = current.length();
        if (currentSpeed < 0.0001) {
            return requestedDirection.scale(targetSpeed * START_RESPONSE);
        }

        Vec3 currentDirection = current.scale(1.0 / currentSpeed);
        double alignment = Mth.clamp(currentDirection.dot(requestedDirection), -1.0, 1.0);
        if (alignment < -0.20) {
            // Brake through a reversal instead of snapping the player through a zero-length vector.
            return current.lerp(requestedDirection.scale(targetSpeed), REVERSAL_RESPONSE);
        }

        double turnResponse = STEERING_RESPONSE * (1.0 + (1.0 - alignment) * 0.30);
        Vec3 steeredDirection = currentDirection.lerp(requestedDirection, turnResponse).normalize();
        double speedResponse = targetSpeed > currentSpeed ? ACCELERATION_RESPONSE : BRAKING_RESPONSE;
        double nextSpeed = Mth.lerp(speedResponse, currentSpeed, targetSpeed);
        return steeredDirection.scale(nextSpeed);
    }
}
