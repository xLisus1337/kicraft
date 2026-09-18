package net.kicraft.kicraft.network;

import net.kicraft.kicraft.core.player.FlightMovement;
import net.kicraft.kicraft.server.ServerKiState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Sends camera-directed flight input for server validation and multiplayer motion sync. */
public record FlightControlPacket(float forward, float left, float vertical,
                                  float directionX, float directionY, float directionZ,
                                  float leftX, float leftY, float leftZ,
                                  float attitudeYaw, float attitudePitch, boolean boosted) {
    public FlightControlPacket(FriendlyByteBuf buffer) {
        this(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat(), buffer.readBoolean());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeFloat(forward);
        buffer.writeFloat(left);
        buffer.writeFloat(vertical);
        buffer.writeFloat(directionX);
        buffer.writeFloat(directionY);
        buffer.writeFloat(directionZ);
        buffer.writeFloat(leftX);
        buffer.writeFloat(leftY);
        buffer.writeFloat(leftZ);
        buffer.writeFloat(attitudeYaw);
        buffer.writeFloat(attitudePitch);
        buffer.writeBoolean(boosted);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.isAlive() || player.isSpectator() || player.onGround()
                    || !ServerKiState.isFlying(player)
                    || !Float.isFinite(forward) || !Float.isFinite(left) || !Float.isFinite(vertical)
                    || !finiteOrientation()) {
                return;
            }

            float safeForward = Mth.clamp(forward, -1.0F, 1.0F);
            float safeLeft = Mth.clamp(left, -1.0F, 1.0F);
            float safeVertical = Mth.clamp(vertical, -1.0F, 1.0F);
            Vec3 look = new Vec3(directionX, directionY, directionZ);
            Vec3 leftBasis = new Vec3(leftX, leftY, leftZ);
            double lookLength = look.length();
            double leftLength = leftBasis.length();
            if (lookLength < 0.8 || lookLength > 1.2 || leftLength < 0.8 || leftLength > 1.2) {
                return;
            }
            look = look.normalize();
            leftBasis = leftBasis.normalize();
            if (Math.abs(look.dot(leftBasis)) > 0.08) {
                return;
            }
            Vec3 velocity = FlightMovement.calculateVelocity(
                    player.getDeltaMovement(), look, leftBasis,
                    safeForward, safeLeft, safeVertical, boosted && safeForward > 0.0F);
            player.setDeltaMovement(velocity);
            player.fallDistance = 0.0F;
            player.hurtMarked = true;
            player.hasImpulse = true;
            ServerKiState.setFlightBoosting(player, boosted && safeForward > 0.0F);
            KicraftNetwork.sendToTrackingAndSelf(player, new FlightOrientationSyncPacket(
                    player.getUUID(), Mth.wrapDegrees(attitudeYaw), Mth.wrapDegrees(attitudePitch)));
        });
        context.setPacketHandled(true);
        return true;
    }

    private boolean finiteOrientation() {
        return Float.isFinite(directionX) && Float.isFinite(directionY) && Float.isFinite(directionZ)
                && Float.isFinite(leftX) && Float.isFinite(leftY) && Float.isFinite(leftZ)
                && Float.isFinite(attitudeYaw) && Float.isFinite(attitudePitch);
    }
}
