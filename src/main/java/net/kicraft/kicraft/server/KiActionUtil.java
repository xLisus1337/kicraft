package net.kicraft.kicraft.server;

import net.kicraft.kicraft.core.player.KiStat;
import net.kicraft.kicraft.core.player.PlayerData;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class KiActionUtil {
    private KiActionUtil() {
    }

    public static Optional<LivingEntity> findLookTarget(ServerPlayer player, double range, double cone) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        AABB area = player.getBoundingBox().inflate(range);
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(LivingEntity.class, area, entity ->
                entity.isAlive() && entity != player && !entity.isSpectator());

        return candidates.stream()
                .filter(entity -> isInsideCone(eye, look, entity, range, cone))
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)));
    }

    public static Optional<ServerPlayer> findLookPlayer(ServerPlayer player, double range, double cone) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        return player.serverLevel().players().stream()
                .filter(other -> other != player && other.isAlive() && !other.isSpectator())
                .filter(other -> isInsideCone(eye, look, other, range, cone))
                .min(Comparator.comparingDouble(other -> other.distanceToSqr(player)));
    }

    public static double kiTechniquePower(PlayerData data) {
        double kiTier = Math.log10(Math.max(1.0, data.maxKi()));
        double controlTier = Math.log10(Math.max(1.0, data.stat(KiStat.KI_CONTROL)));
        double focusTier = Math.log10(Math.max(1.0, data.focus()));
        return 1.8 + kiTier * 0.72 + controlTier * 0.54 + focusTier * 0.28;
    }

    public static void spawnTrail(ServerLevel level, Vec3 start, Vec3 end, ParticleOptions particle, int points) {
        Vec3 delta = end.subtract(start);
        int count = Math.max(2, points);
        for (int i = 0; i <= count; i++) {
            double ratio = i / (double) count;
            Vec3 pos = start.add(delta.scale(ratio));
            level.sendParticles(particle, pos.x, pos.y, pos.z, 1, 0.015, 0.015, 0.015, 0.0);
        }
    }

    public static double powerScore(LivingEntity entity) {
        double score = 4.0 + entity.getMaxHealth() * 0.09 + entity.getHealth() * 0.035;
        score += entity.getArmorValue() * 0.35;
        score += entity.getBbWidth() * entity.getBbHeight() * 0.15;
        if (entity instanceof Player) {
            score += 1.0;
        }
        if (entity instanceof EnderDragon) {
            score += 3500.0;
        } else if (entity instanceof WitherBoss) {
            score += 1200.0;
        }
        return score;
    }

    private static boolean isInsideCone(Vec3 eye, Vec3 look, Entity entity, double range, double cone) {
        Vec3 target = entity.getBoundingBox().getCenter();
        Vec3 toTarget = target.subtract(eye);
        double distance = toTarget.length();
        if (distance > range || distance < 0.001) {
            return false;
        }
        return look.dot(toTarget.normalize()) >= cone;
    }
}
