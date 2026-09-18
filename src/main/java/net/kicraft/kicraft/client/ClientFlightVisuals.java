package net.kicraft.kicraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Smooth local flight attitude shared by the player model, camera and FOV effects. */
public final class ClientFlightVisuals {
    private static final float HOVER_TILT_DEGREES = 8.0F;
    private static final float CRUISE_TILT_DEGREES = 32.0F;
    private static final float BOOST_TILT_DEGREES = 82.0F;
    private static UUID playerId;
    private static float previousBank;
    private static float bank;
    private static float previousBoost;
    private static float boost;
    private static float previousTilt;
    private static float tilt;
    private static float lastYaw;
    private static boolean hasYaw;
    private static final Map<AbstractClientPlayer, RemoteTiltState> REMOTE_TILTS = new WeakHashMap<>();

    private ClientFlightVisuals() {
    }

    public static void update(LocalPlayer player, float forward, float left, boolean boosted) {
        if (!player.getUUID().equals(playerId)) {
            reset(player);
        }

        previousBank = bank;
        previousBoost = boost;
        previousTilt = tilt;
        float yawDelta = hasYaw ? Mth.wrapDegrees(player.getYRot() - lastYaw) : 0.0F;
        lastYaw = player.getYRot();
        hasYaw = true;

        float input = Mth.clamp(Mth.sqrt(forward * forward + left * left), 0.0F, 1.0F);
        float turnBank = Mth.clamp(-yawDelta * 1.35F, -15.0F, 15.0F);
        float strafeBank = -left * (forward > 0.0F ? 15.0F : 11.0F);
        float targetBank = (turnBank + strafeBank) * input;
        bank = damp(bank, targetBank, targetBank * bank < 0.0F ? 0.36F : 0.25F);
        boost = damp(boost, boosted && forward > 0.0F ? 1.0F : 0.0F, boosted ? 0.18F : 0.30F);
        float targetTilt = input < 0.045F
                ? HOVER_TILT_DEGREES
                : (boosted && forward > 0.0F ? BOOST_TILT_DEGREES : CRUISE_TILT_DEGREES);
        float tiltResponse = targetTilt > tilt ? 0.13F : 0.105F;
        tilt = damp(tilt, targetTilt, tiltResponse);
    }

    public static void stop(LocalPlayer player) {
        if (playerId == null || !player.getUUID().equals(playerId)) {
            reset(player);
            return;
        }
        previousBank = bank;
        previousBoost = boost;
        previousTilt = tilt;
        bank = damp(bank, 0.0F, 0.34F);
        boost = damp(boost, 0.0F, 0.34F);
        tilt = damp(tilt, 0.0F, 0.18F);
        lastYaw = player.getYRot();
    }

    public static float playerBankDegrees(AbstractClientPlayer player, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && player.getUUID().equals(minecraft.player.getUUID())) {
            return Mth.lerp(Mth.clamp(partialTick, 0.0F, 1.0F), previousBank, bank);
        }

        Vec3 velocity = player.getDeltaMovement();
        double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (horizontal < 0.025) return 0.0F;
        float bodyYaw = player.yBodyRot * Mth.DEG_TO_RAD;
        double rightX = Mth.cos(bodyYaw);
        double rightZ = Mth.sin(bodyYaw);
        float side = (float) ((velocity.x * rightX + velocity.z * rightZ) / horizontal);
        return Mth.clamp(-side * 13.0F, -13.0F, 13.0F);
    }

    public static float cameraBankDegrees(float partialTick) {
        return Mth.lerp(Mth.clamp(partialTick, 0.0F, 1.0F), previousBank, bank) * 0.42F;
    }

    public static float playerFlightTiltDegrees(AbstractClientPlayer player, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && player.getUUID().equals(minecraft.player.getUUID())) {
            return Mth.lerp(Mth.clamp(partialTick, 0.0F, 1.0F), previousTilt, tilt);
        }

        float speed = (float) player.getDeltaMovement().length();
        float targetTilt;
        if (speed < 0.06F) {
            targetTilt = HOVER_TILT_DEGREES;
        } else if (speed <= 0.66F) {
            targetTilt = CRUISE_TILT_DEGREES;
        } else {
            float boostProgress = Mth.clamp((speed - 0.66F) / 0.46F, 0.0F, 1.0F);
            targetTilt = Mth.lerp(boostProgress, CRUISE_TILT_DEGREES, BOOST_TILT_DEGREES);
        }

        RemoteTiltState state = REMOTE_TILTS.computeIfAbsent(player, ignored -> new RemoteTiltState());
        float renderTime = player.tickCount + Mth.clamp(partialTick, 0.0F, 1.0F);
        float elapsed = Float.isNaN(state.lastRenderTime)
                ? 1.0F
                : Mth.clamp(renderTime - state.lastRenderTime, 0.0F, 1.5F);
        state.lastRenderTime = renderTime;
        state.tilt = dampOverTime(state.tilt, targetTilt,
                targetTilt > state.tilt ? 0.13F : 0.105F, elapsed);
        return state.tilt;
    }

    public static float flightDirectionInfluence(float tiltDegrees) {
        return Mth.clamp((tiltDegrees - HOVER_TILT_DEGREES)
                / (BOOST_TILT_DEGREES - HOVER_TILT_DEGREES), 0.0F, 1.0F);
    }

    public static float boost(float partialTick) {
        return Mth.lerp(Mth.clamp(partialTick, 0.0F, 1.0F), previousBoost, boost);
    }

    public static void reset() {
        playerId = null;
        previousBank = bank = 0.0F;
        previousBoost = boost = 0.0F;
        previousTilt = tilt = 0.0F;
        lastYaw = 0.0F;
        hasYaw = false;
        REMOTE_TILTS.clear();
    }

    private static void reset(LocalPlayer player) {
        reset();
        playerId = player.getUUID();
        lastYaw = player.getYRot();
        hasYaw = true;
    }

    private static float damp(float current, float target, float response) {
        return Mth.lerp(response, current, target);
    }

    private static float dampOverTime(float current, float target, float responsePerTick, float elapsedTicks) {
        if (elapsedTicks <= 0.0F) return current;
        float response = 1.0F - (float) Math.pow(1.0F - responsePerTick, elapsedTicks);
        return Mth.lerp(response, current, target);
    }

    private static final class RemoteTiltState {
        private float lastRenderTime = Float.NaN;
        private float tilt;
    }
}
