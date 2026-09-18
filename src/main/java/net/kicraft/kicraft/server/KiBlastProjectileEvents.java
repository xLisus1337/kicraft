package net.kicraft.kicraft.server;

import net.kicraft.kicraft.Kicraft;
import net.kicraft.kicraft.core.player.KiTransformation;
import net.kicraft.kicraft.server.capability.PlayerDataCapability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Kicraft.MODID)
public final class KiBlastProjectileEvents {
    private static final List<KiBlast> ACTIVE_BLASTS = new ArrayList<>();
    private static final double HIT_RADIUS = 0.75;
    private static final double SPLASH_RADIUS = 1.6;
    private static final float PLAYER_DIRECT_MAX_HEALTH_RATIO = 0.22F;
    private static final float PLAYER_SPLASH_MAX_HEALTH_RATIO = 0.10F;

    private KiBlastProjectileEvents() {
    }

    public static void spawn(ServerPlayer owner, Vec3 start, Vec3 direction, double damage) {
        if (!owner.isAlive() || owner.isSpectator() || !Double.isFinite(damage) || damage <= 0.0
                || !finite(start) || !finite(direction) || direction.lengthSqr() < 0.0001) return;
        Vec3 velocity = direction.normalize().scale(1.55);
        ACTIVE_BLASTS.add(new KiBlast(owner.getUUID(), owner.level().dimension(), start, velocity, damage, 0, 32));
        owner.serverLevel().playSound(null, owner.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.55F, 1.45F);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIVE_BLASTS.isEmpty()) {
            return;
        }

        MinecraftServer server = event.getServer();
        Iterator<KiBlast> iterator = ACTIVE_BLASTS.iterator();
        while (iterator.hasNext()) {
            KiBlast blast = iterator.next();
            ServerLevel level = server.getLevel(blast.dimension);
            ServerPlayer owner = server.getPlayerList().getPlayer(blast.ownerId);
            if (blast.cancelled || level == null || owner == null || !owner.isAlive() || owner.isSpectator()
                    || owner.serverLevel() != level || blast.ticks >= blast.maxTicks) {
                iterator.remove();
                continue;
            }

            Vec3 previous = blast.position;
            Vec3 next = previous.add(blast.velocity);
            HitResult blockHit = level.clip(new ClipContext(previous, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
            Vec3 end = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : next;
            EntityImpact hit = findHitEntity(level, owner, previous, end);
            if (hit != null) {
                impact(level, owner, hit.position(), blast.damage, hit.entity());
                iterator.remove();
                continue;
            }
            if (blockHit.getType() == HitResult.Type.BLOCK) {
                // Stay outside the wall so the splash visibility ray starts on the impact side.
                impact(level, owner, end.subtract(blast.velocity.normalize().scale(0.01)), blast.damage, null);
                iterator.remove();
                continue;
            }

            blast.position = next;
            blast.ticks++;
            trail(level, previous, next);
        }
    }

    private static EntityImpact findHitEntity(ServerLevel level, ServerPlayer owner, Vec3 previous, Vec3 next) {
        AABB search = sweptBox(previous, next).inflate(HIT_RADIUS);
        EntityImpact nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, search,
                entity -> canHit(owner, entity))) {
            AABB box = entity.getBoundingBox().inflate(HIT_RADIUS);
            Vec3 point = box.contains(previous) ? previous : box.clip(previous, next).orElse(null);
            // The segment already ends at the first solid block, so any clipped entity is visible.
            if (point == null) continue;
            double distance = previous.distanceToSqr(point);
            if (distance < nearestDistance) {
                nearest = new EntityImpact(entity, point);
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static int impact(ServerLevel level, ServerPlayer owner, Vec3 position, double damage, LivingEntity directHit) {
        level.sendParticles(ParticleTypes.EXPLOSION, position.x, position.y, position.z, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, position.x, position.y, position.z, 18, 0.28, 0.28, 0.28, 0.08);
        level.sendParticles(ParticleTypes.END_ROD, position.x, position.y, position.z, 10, 0.22, 0.22, 0.22, 0.04);
        level.playSound(null, position.x, position.y, position.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.35F, 1.75F);

        AABB splash = new AABB(position.x, position.y, position.z, position.x, position.y, position.z).inflate(SPLASH_RADIUS);
        double masteryPressure = 0.0;
        int hitCount = 0;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, splash,
                entity -> canHit(owner, entity))) {
            AABB box = entity.getBoundingBox();
            Vec3 closest = new Vec3(Math.max(box.minX, Math.min(box.maxX, position.x)),
                    Math.max(box.minY, Math.min(box.maxY, position.y)),
                    Math.max(box.minZ, Math.min(box.maxZ, position.z)));
            double distance = closest.distanceTo(position);
            boolean direct = entity == directHit;
            // A direct collision was already clipped against blocks during flight. Only splash
            // targets need another line-of-sight test from the explosion point.
            if (!direct && (distance > SPLASH_RADIUS || !visible(level, owner, position, entity))) continue;
            float scaledDamage = (float) (direct ? damage : damage * Math.max(0.25, 1.0 - distance / SPLASH_RADIUS) * 0.45);
            scaledDamage = capPlayerBlastDamage(entity, scaledDamage, direct);
            if (scaledDamage <= 0.0F) {
                continue;
            }
            // Keep the owner for PvP rules, kill credit, guarding and combat progression.
            if (!entity.hurt(owner.damageSources().indirectMagic(owner, owner), scaledDamage)) continue;
            hitCount++;
            masteryPressure += scaledDamage * (direct ? 1.0 : 0.35);
            Vec3 push = entity.position().subtract(position).multiply(1.0, 0.0, 1.0);
            if (push.lengthSqr() > 0.0001) {
                entity.setDeltaMovement(entity.getDeltaMovement().add(push.normalize().scale(0.35)).add(0.0, 0.08, 0.0));
                entity.hasImpulse = true;
                entity.hurtMarked = true;
            }
        }
        trainOwnerTransformation(owner, masteryPressure);
        return hitCount;
    }

    private static boolean canHit(ServerPlayer owner, LivingEntity entity) {
        return entity != owner && entity.isAlive() && !entity.isSpectator()
                && (!(entity instanceof Player player)
                || (owner.getServer().isPvpAllowed() && owner.canHarmPlayer(player)));
    }

    private static boolean visible(ServerLevel level, ServerPlayer owner, Vec3 origin, LivingEntity entity) {
        return level.clip(new ClipContext(origin, entity.getBoundingBox().getCenter(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner)).getType() == HitResult.Type.MISS;
    }

    private static boolean finite(Vec3 vector) {
        return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { ACTIVE_BLASTS.clear(); }

    public static void clear(ServerPlayer player) {
        // Death callbacks can run inside impact(), while the projectile list is being iterated.
        for (KiBlast blast : ACTIVE_BLASTS) {
            if (blast.ownerId.equals(player.getUUID())) blast.cancelled = true;
        }
    }

    private record EntityImpact(LivingEntity entity, Vec3 position) { }

    private static void trainOwnerTransformation(ServerPlayer owner, double masteryPressure) {
        if (masteryPressure <= 0.0) {
            return;
        }

        owner.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
            KiTransformation transformation = data.activeTransformation();
            if (transformation.isBase()) {
                return;
            }

            double pressure = Math.min(1.0, Math.log10(1.0 + masteryPressure) / 2.4);
            data.trainTransformation(transformation, 0.010 * (0.30 + pressure));
            PlayerDataEvents.sync(owner);
            data.clearDirty();
        });
    }

    private static float capPlayerBlastDamage(LivingEntity entity, float damage, boolean direct) {
        if (!(entity instanceof ServerPlayer player)) {
            return damage;
        }

        float maxHealth = Math.max(1.0F, player.getMaxHealth());
        float cap = maxHealth * (direct ? PLAYER_DIRECT_MAX_HEALTH_RATIO : PLAYER_SPLASH_MAX_HEALTH_RATIO);
        return Math.max(0.1F, Math.min(damage, cap));
    }

    private static void trail(ServerLevel level, Vec3 previous, Vec3 next) {
        Vec3 delta = next.subtract(previous);
        for (int i = 0; i < 4; i++) {
            double ratio = i / 4.0;
            Vec3 point = previous.add(delta.scale(ratio));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 1, 0.025, 0.025, 0.025, 0.01);
            if (i % 2 == 0) {
                level.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0.015, 0.015, 0.015, 0.006);
            }
        }
    }

    private static AABB sweptBox(Vec3 from, Vec3 to) {
        return new AABB(
                Math.min(from.x, to.x),
                Math.min(from.y, to.y),
                Math.min(from.z, to.z),
                Math.max(from.x, to.x),
                Math.max(from.y, to.y),
                Math.max(from.z, to.z)
        );
    }

    private static final class KiBlast {
        private final UUID ownerId;
        private final ResourceKey<Level> dimension;
        private Vec3 position;
        private final Vec3 velocity;
        private final double damage;
        private int ticks;
        private final int maxTicks;
        private boolean cancelled;

        private KiBlast(UUID ownerId, ResourceKey<Level> dimension, Vec3 position, Vec3 velocity, double damage, int ticks, int maxTicks) {
            this.ownerId = ownerId;
            this.dimension = dimension;
            this.position = position;
            this.velocity = velocity;
            this.damage = damage;
            this.ticks = ticks;
            this.maxTicks = maxTicks;
        }
    }
}
