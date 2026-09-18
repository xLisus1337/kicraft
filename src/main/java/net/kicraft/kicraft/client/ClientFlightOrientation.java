package net.kicraft.kicraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Continuous flight attitude. Pitch wraps through vertical instead of stopping at Minecraft's +/-90 degrees. */
public final class ClientFlightOrientation {
    private static final Map<UUID, RemoteAttitude> REMOTE = new HashMap<>();
    private static UUID localPlayerId;
    private static float previousYaw;
    private static float yaw;
    private static float previousPitch;
    private static float pitch;
    private static boolean active;

    private ClientFlightOrientation() {
    }

    public static void beginTick(LocalPlayer player) {
        boolean flying = ClientFlightStates.isFlying(player);
        if (!flying) {
            active = false;
            localPlayerId = player.getUUID();
            return;
        }
        if (!active || !player.getUUID().equals(localPlayerId)) {
            initialize(player);
        }
        previousYaw = yaw;
        previousPitch = pitch;
        applyEquivalentVanillaRotation(player);
    }

    public static void turn(LocalPlayer player, double yawDelta, double pitchDelta) {
        if (!active || !player.getUUID().equals(localPlayerId)) {
            initialize(player);
        }
        float inversion = Mth.cos(pitch * Mth.DEG_TO_RAD) < 0.0F ? -1.0F : 1.0F;
        yaw = Mth.wrapDegrees(yaw + (float) yawDelta * 0.15F * inversion);
        pitch = Mth.wrapDegrees(pitch + (float) pitchDelta * 0.15F);
        applyEquivalentVanillaRotation(player);
    }

    public static void followVanilla(LocalPlayer player) {
        if (!ClientFlightStates.isFlying(player)) return;
        if (!active || !player.getUUID().equals(localPlayerId)) initialize(player);
        yaw = player.getYRot();
        pitch = player.getXRot();
        applyEquivalentVanillaRotation(player);
    }

    public static void stabilizeRenderedBody(LocalPlayer player) {
        if (!active || !player.getUUID().equals(localPlayerId)
                || !ClientFlightStates.isFlying(player)) {
            return;
        }
        player.yBodyRotO = yaw;
        player.yBodyRot = yaw;
        player.yHeadRotO = yaw;
        player.yHeadRot = yaw;
    }

    public static Vec3 forward(LocalPlayer player) {
        if (!active || !player.getUUID().equals(localPlayerId)) initialize(player);
        return Vec3.directionFromRotation(pitch, yaw).normalize();
    }

    public static Vec3 left(LocalPlayer player) {
        if (!active || !player.getUUID().equals(localPlayerId)) initialize(player);
        float yawRadians = yaw * Mth.DEG_TO_RAD;
        return new Vec3(Mth.cos(yawRadians), 0.0, Mth.sin(yawRadians));
    }

    public static Attitude attitude(AbstractClientPlayer player, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        float partial = Mth.clamp(partialTick, 0.0F, 1.0F);
        if (minecraft.player != null && player.getUUID().equals(minecraft.player.getUUID())
                && active && player.getUUID().equals(localPlayerId)) {
            return new Attitude(
                    Mth.rotLerp(partial, previousYaw, yaw),
                    previousPitch + Mth.wrapDegrees(pitch - previousPitch) * partial
            );
        }
        RemoteAttitude remote = REMOTE.get(player.getUUID());
        if (remote != null) {
            return new Attitude(
                    Mth.rotLerp(partial, remote.previousYaw, remote.yaw),
                    remote.previousPitch + Mth.wrapDegrees(remote.pitch - remote.previousPitch) * partial
            );
        }

        Vec3 velocity = player.getDeltaMovement();
        if (velocity.lengthSqr() > 0.0001) {
            double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            return new Attitude(
                    (float) Math.toDegrees(Math.atan2(-velocity.x, velocity.z)),
                    (float) -Math.toDegrees(Math.atan2(velocity.y, horizontal))
            );
        }
        return new Attitude(player.getYRot(), player.getXRot());
    }

    public static void setRemote(UUID playerId, float remoteYaw, float remotePitch) {
        if (!Float.isFinite(remoteYaw) || !Float.isFinite(remotePitch)) return;
        RemoteAttitude state = REMOTE.get(playerId);
        float safeYaw = Mth.wrapDegrees(remoteYaw);
        float safePitch = Mth.wrapDegrees(remotePitch);
        if (state == null) {
            REMOTE.put(playerId, new RemoteAttitude(safeYaw, safePitch, safeYaw, safePitch));
        } else {
            REMOTE.put(playerId, new RemoteAttitude(state.yaw, state.pitch, safeYaw, safePitch));
        }
    }

    public static void stop(UUID playerId) {
        REMOTE.remove(playerId);
        if (playerId.equals(localPlayerId)) active = false;
    }

    public static void reset() {
        REMOTE.clear();
        localPlayerId = null;
        previousYaw = yaw = 0.0F;
        previousPitch = pitch = 0.0F;
        active = false;
    }

    private static void initialize(LocalPlayer player) {
        localPlayerId = player.getUUID();
        previousYaw = yaw = player.getYRot();
        previousPitch = pitch = player.getXRot();
        active = true;
    }

    private static void applyEquivalentVanillaRotation(LocalPlayer player) {
        Vec3 direction = Vec3.directionFromRotation(pitch, yaw);
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        if (horizontal > 0.00001) {
            float equivalentYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
            player.setYRot(equivalentYaw);
        }
        player.setXRot((float) -Math.toDegrees(Math.atan2(direction.y, horizontal)));

        // Vanilla flips its equivalent yaw by 180 degrees after the look vector crosses
        // vertical. Keeping the rendered body on the free-flight yaw prevents that
        // implementation detail from making the model spin during a loop.
        stabilizeRenderedBody(player);
    }

    public record Attitude(float yaw, float pitch) {
    }

    private record RemoteAttitude(float previousYaw, float previousPitch, float yaw, float pitch) {
    }
}
