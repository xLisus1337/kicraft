package net.kicraft.kicraft.network;

import net.kicraft.kicraft.core.player.KiStat;
import net.kicraft.kicraft.core.player.ResourceCosts;
import net.kicraft.kicraft.server.CombatState;
import net.kicraft.kicraft.server.PlayerDataEvents;
import net.kicraft.kicraft.server.StaminaSystem;
import net.kicraft.kicraft.server.capability.PlayerDataCapability;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record DashPacket(float forward, float strafe) {
    private static final String DASH_COOLDOWN_TAG = "kicraftDashCooldown";
    private static final double BASE_COST = 8.0;
    private static final int BASE_COOLDOWN_TICKS = 12;
    private static final int LEGACY_COOLDOWN_GRACE_TICKS = 80;

    public DashPacket(FriendlyByteBuf buffer) {
        this(buffer.readFloat(), buffer.readFloat());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeFloat(forward);
        buffer.writeFloat(strafe);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.isAlive() || player.isSpectator()
                    || net.kicraft.kicraft.server.MeleeCombat.actionLocked(player)) {
                return;
            }
            if (!Float.isFinite(forward) || !Float.isFinite(strafe)) {
                return;
            }

            player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
                if (!data.isRaceChosen()) {
                    return;
                }
                CompoundTag persistentData = player.getPersistentData();
                long gameTime = player.level().getGameTime();
                long nextDashTime = persistentData.getLong(DASH_COOLDOWN_TAG);
                if (nextDashTime > gameTime + LEGACY_COOLDOWN_GRACE_TICKS) {
                    persistentData.remove(DASH_COOLDOWN_TAG);
                    nextDashTime = 0L;
                }
                if (gameTime < nextDashTime) {
                    return;
                }

                if (!data.race().hasInfiniteEnergy()) {
                    if (!StaminaSystem.canStartExertion(player, data)) {
                        return;
                    }
                    double cost = BASE_COST * ResourceCosts.staminaCostMultiplier(data) * CombatState.staminaDrainScale(player);
                    if (data.stamina() < cost) {
                        return;
                    }
                    StaminaSystem.spend(player, data, cost);
                }

                Vec3 dodge = dashDirection(player, forward, strafe);
                double dodgeDistance = 1.0;
                Vec3 safeTarget = safeDodgeTarget(player, dodge, dodgeDistance);
                Vec3 movement = safeTarget.subtract(player.position());
                player.setDeltaMovement(movement.x * 0.82, player.onGround() ? 0.06 : 0.0, movement.z * 0.82);
                player.hurtMarked = true;
                player.hasImpulse = true;

                persistentData.putLong(DASH_COOLDOWN_TAG, gameTime + BASE_COOLDOWN_TICKS);

                data.train(KiStat.STAMINA, 0.020);
                PlayerDataEvents.sync(player);
                data.clearDirty();
            });
        });
        context.setPacketHandled(true);
        return true;
    }

    private static Vec3 safeDodgeTarget(ServerPlayer player, Vec3 direction, double distance) {
        Vec3 start = player.position();
        Vec3 end = start.add(direction.scale(distance));
        HitResult hit = player.level().clip(new ClipContext(
                player.getEyePosition(),
                player.getEyePosition().add(direction.scale(distance + 0.35)),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));
        if (hit.getType() == HitResult.Type.BLOCK) {
            end = start.add(direction.scale(Math.max(0.0, hit.getLocation().distanceTo(player.getEyePosition()) - 0.75)));
        }
        return end;
    }

    private static Vec3 dashDirection(ServerPlayer player, float forward, float strafe) {
        Vec3 look = player.getLookAngle();
        Vec3 flatForward = new Vec3(look.x, 0.0, look.z);
        if (flatForward.lengthSqr() < 0.0001) {
            flatForward = Vec3.directionFromRotation(0.0F, player.getYRot()).multiply(1.0, 0.0, 1.0);
        }
        flatForward = flatForward.normalize();
        Vec3 flatRight = new Vec3(-flatForward.z, 0.0, flatForward.x);

        Vec3 desired = flatForward.scale(forward).add(flatRight.scale(strafe));
        if (desired.lengthSqr() < 0.0001) {
            desired = flatForward;
        }
        return desired.normalize();
    }
}
