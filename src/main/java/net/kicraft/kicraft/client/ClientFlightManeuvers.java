package net.kicraft.kicraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Time-synchronized visual state for server-authoritative flight maneuvers. */
public final class ClientFlightManeuvers {
    private static final Map<UUID, LoopState> LOOPS = new HashMap<>();

    private ClientFlightManeuvers() {
    }

    public static void startLoop(UUID playerId, long startGameTime, int durationTicks,
                                 double forwardX, double forwardZ, double speed, double startAngle) {
        if (durationTicks <= 0) {
            LOOPS.remove(playerId);
            return;
        }
        LOOPS.put(playerId, new LoopState(startGameTime, durationTicks,
                forwardX, forwardZ, speed, startAngle));
    }

    public static void stop(UUID playerId) {
        LOOPS.remove(playerId);
    }

    public static boolean isLooping(AbstractClientPlayer player) {
        return progress(player, 0.0F) < 1.0F && LOOPS.containsKey(player.getUUID());
    }

    public static float progress(AbstractClientPlayer player, float partialTick) {
        LoopState state = LOOPS.get(player.getUUID());
        Minecraft minecraft = Minecraft.getInstance();
        if (state == null || minecraft.level == null) {
            return 1.0F;
        }
        float elapsed = minecraft.level.getGameTime() + Mth.clamp(partialTick, 0.0F, 1.0F)
                - state.startGameTime;
        float progress = Mth.clamp(elapsed / state.durationTicks, 0.0F, 1.0F);
        if (progress >= 1.0F) {
            LOOPS.remove(player.getUUID());
        }
        return progress;
    }

    public static float loopRotationDegrees(AbstractClientPlayer player, float partialTick) {
        if (!LOOPS.containsKey(player.getUUID())) {
            return 0.0F;
        }
        float progress = progress(player, partialTick);
        return progress >= 1.0F ? 0.0F : -360.0F * progress;
    }

    public static void applyPredictedMotion(LocalPlayer player) {
        LoopState state = LOOPS.get(player.getUUID());
        Minecraft minecraft = Minecraft.getInstance();
        if (state == null || minecraft.level == null) {
            return;
        }
        float progress = progress(player, 0.0F);
        if (progress >= 1.0F) {
            return;
        }
        double angle = state.startAngle + Mth.TWO_PI * Math.min(1.0,
                (minecraft.level.getGameTime() - state.startGameTime + 1.0) / state.durationTicks);
        player.setDeltaMovement(state.forwardX * Math.cos(angle) * state.speed,
                Math.sin(angle) * state.speed,
                state.forwardZ * Math.cos(angle) * state.speed);
        player.fallDistance = 0.0F;
    }

    public static void reset() {
        LOOPS.clear();
    }

    private record LoopState(long startGameTime, int durationTicks,
                             double forwardX, double forwardZ, double speed, double startAngle) {
    }
}
