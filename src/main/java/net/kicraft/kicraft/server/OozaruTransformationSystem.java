package net.kicraft.kicraft.server;

import net.kicraft.kicraft.core.player.KiRace;
import net.kicraft.kicraft.core.player.KiTransformation;
import net.kicraft.kicraft.core.player.PlayerData;
import net.kicraft.kicraft.network.KicraftNetwork;
import net.kicraft.kicraft.network.OozaruFocusPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-owned, transient gaze sequence; no unfinished transformation is saved to disk. */
public final class OozaruTransformationSystem {
    public static final int DURATION_TICKS = 60;
    private static final double MIN_GAZE_DOT = Math.cos(Math.toRadians(8.0));
    private static final Map<UUID, Focus> FOCUS = new HashMap<>();

    private OozaruTransformationSystem() { }

    /** Matches LevelRenderer's Y(-90 degrees) * X(timeOfDay * 360) moon at (0,-100,0). */
    public static Vec3 moonDirection(Level level) {
        double angle = level.getTimeOfDay(1.0F) * Math.PI * 2.0;
        return new Vec3(Math.sin(angle), -Math.cos(angle), 0.0);
    }

    static boolean isFullMoonNight(Level level) {
        long time = Math.floorMod(level.getDayTime(), 24000L);
        return level.dimensionType().hasSkyLight() && !level.dimensionType().hasFixedTime()
                && level.dimensionType().effectsLocation().toString().equals("minecraft:overworld")
                && time >= 13000L && time <= 23000L && level.getMoonPhase() == 0;
    }

    static boolean isLookingAtMoon(ServerPlayer player) {
        Vec3 direction = moonDirection(player.level());
        if (direction.y <= 0.0 || player.getLookAngle().dot(direction) < MIN_GAZE_DOT
                || player.level().getRainLevel(1.0F) >= 1.0F) return false;
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(direction.scale(128.0));
        // Check toward the moon, not just the block vertically above the player.
        return player.level().clip(new ClipContext(start, end, ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS
                && player.level().canSeeSky(BlockPos.containing(end));
    }

    public static boolean isFocusing(ServerPlayer player) { return FOCUS.containsKey(player.getUUID()); }

    public static boolean tick(ServerPlayer player, PlayerData data) {
        player.getPersistentData().remove("kicraftOozaruGazeTicks");
        boolean eligible = player.isAlive() && !player.isSpectator() && !player.isSleeping()
                && !data.isLegendarySaiyan() && data.hasTail()
                && (data.race() == KiRace.SAIYAN || data.race() == KiRace.HALF_SAIYAN);
        boolean fullMoon = isFullMoonNight(player.level());
        boolean oozaru = data.activeTransformation() == KiTransformation.OOZARU
                || data.activeTransformation() == KiTransformation.GOLDEN_OOZARU;
        if (!eligible || !fullMoon || !data.activeTransformation().isBase()) {
            clear(player);
            // A roof no longer ends the full moon or an already completed transformation.
            if (oozaru && (!eligible || !fullMoon)) {
                data.activateTransformation(KiTransformation.BASE);
                return true;
            }
            return false;
        }

        Focus focus = FOCUS.get(player.getUUID());
        if (focus == null) {
            if (!isLookingAtMoon(player)) return false;
            focus = new Focus(player.getYRot(), player.getXRot(), player.level());
            FOCUS.put(player.getUUID(), focus);
            LockOnState.clear(player);
            player.displayClientMessage(Component.translatable("message.kicraft.oozaru_focus"), true);
            send(player, focus);
        } else if (focus.level != player.level()) {
            clear(player);
            return false;
        }
        player.setYRot(focus.yaw);
        player.setXRot(focus.pitch);
        player.setYHeadRot(focus.yaw);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 4, 6, false, false, false));
        if (--focus.remaining <= 0) {
            data.activateTransformation(KiTransformation.OOZARU);
            clear(player);
            player.displayClientMessage(Component.translatable("message.kicraft.oozaru_triggered"), true);
            PlayerDataEvents.sync(player);
        } else if (focus.remaining % 20 == 0) {
            send(player, focus);
        }
        return true;
    }

    public static void clear(ServerPlayer player) {
        player.getPersistentData().remove("kicraftOozaruGazeTicks");
        if (FOCUS.remove(player.getUUID()) != null) {
            KicraftNetwork.sendToPlayer(player, new OozaruFocusPacket(player.getId(), 0, 0, 0));
        }
    }

    public static void clearAll() { FOCUS.clear(); }

    private static void send(ServerPlayer player, Focus focus) {
        KicraftNetwork.sendToPlayer(player,
                new OozaruFocusPacket(player.getId(), focus.remaining, focus.yaw, focus.pitch));
    }

    private static final class Focus {
        final float yaw, pitch;
        final Level level;
        int remaining = DURATION_TICKS;
        Focus(float yaw, float pitch, Level level) { this.yaw = yaw; this.pitch = pitch; this.level = level; }
    }
}
