package net.kicraft.kicraft.server;

import net.kicraft.kicraft.core.player.KiStat;
import net.kicraft.kicraft.core.player.ResourceCosts;
import net.kicraft.kicraft.network.FlightManeuverSyncPacket;
import net.kicraft.kicraft.network.KicraftNetwork;
import net.kicraft.kicraft.server.capability.PlayerDataCapability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative trajectory and cost rules for aerial acrobatics. */
public final class FlightManeuverSystem {
    public static final int LOOP_DURATION_TICKS = 32;
    private static final String LOOP_COOLDOWN_TAG = "kicraftFlightLoopCooldown";
    private static final int LOOP_COOLDOWN_TICKS = 18;
    private static final double LOOP_STAMINA_COST = 12.0;
    private static final double MIN_ENTRY_SPEED = 0.065;
    private static final double MIN_LOOP_SPEED = 0.30;
    private static final double MAX_LOOP_SPEED = 0.48;
    private static final Map<UUID, LoopState> ACTIVE_LOOPS = new HashMap<>();

    private FlightManeuverSystem() {
    }

    public static boolean tryStartLoop(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || player.onGround()
                || !ServerKiState.isFlying(player) || MeleeCombat.actionLocked(player)
                || ACTIVE_LOOPS.containsKey(player.getUUID())) {
            return false;
        }

        long gameTime = player.level().getGameTime();
        if (gameTime < player.getPersistentData().getLong(LOOP_COOLDOWN_TAG)) {
            return false;
        }

        Vec3 entryVelocity = player.getDeltaMovement();
        double entrySpeed = entryVelocity.length();
        if (entrySpeed < MIN_ENTRY_SPEED) {
            return false;
        }

        Vec3 horizontal = entryVelocity.multiply(1.0, 0.0, 1.0);
        if (horizontal.lengthSqr() < 0.000001) {
            horizontal = Vec3.directionFromRotation(0.0F, player.getYRot())
                    .multiply(1.0, 0.0, 1.0);
        }
        Vec3 forward = horizontal.normalize();
        double loopSpeed = Mth.clamp(entrySpeed, MIN_LOOP_SPEED, MAX_LOOP_SPEED);
        double startAngle = Math.atan2(entryVelocity.y, Math.max(0.0001, horizontal.length()));

        final boolean[] started = {false};
        player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
            double cost = LOOP_STAMINA_COST * ResourceCosts.staminaCostMultiplier(data)
                    * CombatState.staminaDrainScale(player);
            if (!data.race().hasInfiniteEnergy()) {
                if (!StaminaSystem.canStartExertion(player, data) || data.stamina() < cost) {
                    return;
                }
                StaminaSystem.spend(player, data, cost);
            }

            LoopState state = new LoopState(gameTime, forward, loopSpeed, startAngle);
            ACTIVE_LOOPS.put(player.getUUID(), state);
            player.getPersistentData().putLong(LOOP_COOLDOWN_TAG,
                    gameTime + LOOP_DURATION_TICKS + LOOP_COOLDOWN_TICKS);
            data.train(KiStat.STAMINA, 0.032);
            PlayerDataEvents.sync(player);
            data.clearDirty();
            sync(player, state);
            started[0] = true;
        });
        return started[0];
    }

    public static void tick(ServerPlayer player) {
        LoopState state = ACTIVE_LOOPS.get(player.getUUID());
        if (state == null) {
            return;
        }
        if (!player.isAlive() || player.isSpectator() || !ServerKiState.isFlying(player)) {
            cancel(player);
            return;
        }

        long elapsed = player.level().getGameTime() - state.startGameTime;
        if (elapsed >= LOOP_DURATION_TICKS) {
            player.setDeltaMovement(state.forward.scale(state.speed));
            player.hurtMarked = true;
            player.hasImpulse = true;
            cancel(player);
            return;
        }

        double angle = state.startAngle + Mth.TWO_PI * (elapsed + 1.0) / LOOP_DURATION_TICKS;
        Vec3 velocity = state.forward.scale(Math.cos(angle) * state.speed)
                .add(0.0, Math.sin(angle) * state.speed, 0.0);
        if (!player.level().noCollision(player, player.getBoundingBox().move(velocity))) {
            player.setDeltaMovement(state.forward.scale(state.speed * 0.32));
            player.hurtMarked = true;
            player.hasImpulse = true;
            cancel(player);
            return;
        }

        player.setDeltaMovement(velocity);
        player.hurtMarked = true;
        player.hasImpulse = true;
        player.fallDistance = 0.0F;
        if ((elapsed & 1L) == 0L) {
            player.serverLevel().sendParticles(ParticleTypes.CLOUD,
                    player.getX(), player.getY() + player.getBbHeight() * 0.45, player.getZ(),
                    2, 0.16, 0.16, 0.16, 0.012);
        }
    }

    public static boolean isLooping(ServerPlayer player) {
        return ACTIVE_LOOPS.containsKey(player.getUUID());
    }

    public static void cancel(ServerPlayer player) {
        if (ACTIVE_LOOPS.remove(player.getUUID()) != null) {
            KicraftNetwork.sendToTrackingAndSelf(player,
                    new FlightManeuverSyncPacket(player.getUUID(), false, 0L, 0,
                            0.0, 0.0, 0.0, 0.0));
        }
    }

    public static void clear(ServerPlayer player) {
        ACTIVE_LOOPS.remove(player.getUUID());
    }

    public static void clearAll() {
        ACTIVE_LOOPS.clear();
    }

    public static void syncTo(ServerPlayer viewer, ServerPlayer target) {
        LoopState state = ACTIVE_LOOPS.get(target.getUUID());
        if (state != null) {
            KicraftNetwork.sendToPlayer(viewer, new FlightManeuverSyncPacket(target.getUUID(), true,
                    state.startGameTime, LOOP_DURATION_TICKS,
                    state.forward.x, state.forward.z, state.speed, state.startAngle));
        }
    }

    private static void sync(ServerPlayer player, LoopState state) {
        KicraftNetwork.sendToTrackingAndSelf(player, new FlightManeuverSyncPacket(player.getUUID(), true,
                state.startGameTime, LOOP_DURATION_TICKS,
                state.forward.x, state.forward.z, state.speed, state.startAngle));
    }

    private record LoopState(long startGameTime, Vec3 forward, double speed, double startAngle) {
    }
}
