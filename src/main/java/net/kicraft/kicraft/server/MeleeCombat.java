package net.kicraft.kicraft.server;

import net.kicraft.kicraft.Kicraft;
import net.kicraft.kicraft.core.combat.CombatFeedback;
import net.kicraft.kicraft.core.combat.LightCombo;
import net.kicraft.kicraft.core.combat.MeleeTimeline;
import net.kicraft.kicraft.core.player.PlayerData;
import net.kicraft.kicraft.core.player.ResourceCosts;
import net.kicraft.kicraft.network.KicraftNetwork;
import net.kicraft.kicraft.network.MeleeInputPacket;
import net.kicraft.kicraft.network.MeleeStatePacket;
import net.kicraft.kicraft.server.capability.PlayerDataCapability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** All targeting, costs, parries and recovery are resolved on the logical server. */
@Mod.EventBusSubscriber(modid = Kicraft.MODID)
public final class MeleeCombat {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final String MOB_STAGGER = "kicraftParriedUntil";
    private static ServerPlayer resolvingAttack;
    private MeleeCombat() { }

    private static Session session(ServerPlayer player) {
        return SESSIONS.computeIfAbsent(player.getUUID(), id -> new Session());
    }
    private static long now(ServerPlayer player) { return player.level().getGameTime(); }
    public static void clear(ServerPlayer player) { SESSIONS.remove(player.getUUID()); }
    public static void block(ServerPlayer player, boolean held) { session(player).time.block(now(player), held); }
    public static boolean canGuard(ServerPlayer player) {
        Session state = session(player);
        return !state.executing && state.time.canGuard(now(player));
    }
    public static boolean recovering(ServerPlayer player) { return session(player).time.recovering(now(player)); }
    public static int comboStep(ServerPlayer player) { return session(player).combo.step(now(player)); }
    public static boolean actionLocked(ServerPlayer player) {
        return recovering(player) || FlightManeuverSystem.isLooping(player);
    }

    private static boolean ready(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.isSleeping()
                && player.containerMenu == player.inventoryMenu && !player.isUsingItem()
                && player.getCapability(PlayerDataCapability.PLAYER_DATA).map(PlayerData::isRaceChosen).orElse(false);
    }

    public static void input(ServerPlayer player, MeleeInputPacket.AttackSide side) {
        Session state = session(player);
        if (!ready(player)) {
            state.combo.reset();
            sync(player, CombatFeedback.INTERRUPTED);
            return;
        }
        if (state.time.recovering(now(player))) {
            sync(player, CombatFeedback.NONE);
            return;
        }
        state.attackHand = side.interactionHand(player.getMainArm());
        ServerKiState.setCharging(player, false);
        ServerKiState.setTransferring(player, false);
        player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> execute(player, data, state));
    }

    private static void execute(ServerPlayer player, PlayerData data, Session state) {
        LivingEntity target = findTarget(player);
        boolean powerful = target != null && rollPowerfulStrike(data, player.getRandom().nextFloat());
        MeleeTimeline.Strike strike = new MeleeTimeline.Strike(powerful);
        double cost = ResourceCosts.meleeStaminaCost(data) * strike.staminaMultiplier() * CombatState.staminaDrainScale(player);
        if (strike.powerful() && !data.race().hasInfiniteEnergy()
                && (!StaminaSystem.canStartExertion(player, data) || data.stamina() < cost)) {
            state.combo.reset();
            state.time.recover(now(player), 6, false);
            sync(player, CombatFeedback.NO_STAMINA);
            return;
        }
        // Pay on the attempt, including misses and parries; XP still requires a landed hit.
        StaminaSystem.spend(player, data, cost);
        state.paidStamina = cost;
        state.strike = strike;
        state.target = target;
        state.connected = false;
        state.blocked = target instanceof ServerPlayer victim && CombatState.isBlocking(victim)
                && fromFront(victim, player);
        state.comboStage = !strike.powerful() && target != null && !state.blocked
                ? state.combo.preview(now(player), target.getId()) : 0;
        state.executing = true;
        state.counter = state.time.consumeCounter(now(player), target == null ? -1 : target.getId());
        // The initiating client predicts its own swing; broadcast it to the other clients only.
        player.swing(state.attackHand, false);
        ServerPlayer previousAttack = resolvingAttack;
        resolvingAttack = player;
        try {
            if (target != null) {
                // Our recovery already rate-limits melee. Clear vanilla's longer hurt immunity so 0.3 s combo hits connect.
                target.invulnerableTime = 0;
                player.attack(target);
            }
            else player.resetAttackStrengthTicker();
        } finally {
            resolvingAttack = previousAttack;
            state.executing = false;
        }
        boolean interrupted = state.time.exposed(now(player)); // e.g. this strike was parried.
        if (!interrupted) {
            state.time.recover(now(player), strike.hitRecoveryTicks(), false);
            sync(player, state.connected && state.counter ? CombatFeedback.COUNTER
                    : state.connected && strike.powerful() ? CombatFeedback.POWERFUL_HIT : CombatFeedback.NONE);
        }
        if (strike.powerful() || !state.connected || state.blocked || target == null) {
            state.combo.reset();
        } else {
            int landedStage = state.combo.land(now(player), target.getId());
            showCombo(player, landedStage);
        }
        if (state.connected && strike.powerful() && target != null) {
            player.serverLevel().sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(),
                    12, 0.25, 0.25, 0.25, 0.15);
            player.level().playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                    SoundSource.PLAYERS, 0.85F, 0.7F);
        } else if (state.connected && state.comboStage == LightCombo.MAX_STEPS && target != null) {
            player.serverLevel().sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(),
                    10, 0.22, 0.22, 0.22, 0.12);
            player.level().playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                    SoundSource.PLAYERS, 0.8F, 0.9F);
            target.knockback(0.7, player.getX() - target.getX(), player.getZ() - target.getZ());
            target.hurtMarked = true;
        }
        state.strike = null;
        state.target = null;
        state.blocked = false;
        state.comboStage = 0;
        CombatState.markCombat(player, strike.powerful() ? 0.8 : 0.25);
        PlayerDataEvents.sync(player);
        data.clearDirty();
    }

    public static boolean rollPowerfulStrike(PlayerData data, float roll) {
        return data.stamina() > data.maxStamina() * 0.60 && roll >= 0.0F && roll < 0.04F;
    }

    /** Ray against entity boxes, clipped by the first solid block, using server-side reach and aim. */
    private static LivingEntity findTarget(ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        double reach = Math.min(12.0, Math.max(0.0, player.getEntityReach()));
        Vec3 end = start.add(player.getLookAngle().scale(reach));
        Vec3 clipped = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player)).getLocation();
        double nearest = start.distanceToSqr(clipped);
        LivingEntity result = null;
        for (LivingEntity candidate : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0))) {
            if (candidate == player || !candidate.isAlive() || candidate.isSpectator() || !candidate.isPickable()
                    || !candidate.isAttackable() || !player.hasLineOfSight(candidate)
                    || candidate instanceof ServerPlayer other && !player.canHarmPlayer(other)) continue;
            var box = candidate.getBoundingBox().inflate(candidate.getPickRadius());
            Vec3 hit = box.contains(start) ? start : box.clip(start, clipped).orElse(null);
            if (hit != null && start.distanceToSqr(hit) < nearest) {
                nearest = start.distanceToSqr(hit);
                result = candidate;
            }
        }
        return result;
    }

    public static boolean paidForStrike(ServerPlayer player) { return session(player).executing; }
    public static double paidStamina(ServerPlayer player) { return session(player).paidStamina; }

    public static float outgoingDamage(ServerPlayer player, LivingEntity target, float damage) {
        Session state = session(player);
        if (state.executing && state.target == target) {
            state.connected = true;
            return (float) Math.min(Float.MAX_VALUE, damage * state.strike.damageMultiplier()
                    * LightCombo.damageMultiplier(state.comboStage) * (state.counter ? 1.35 : 1.0));
        }
        if (state.time.consumeCounter(now(player), target.getId())) {
            sync(player, CombatFeedback.COUNTER);
            return (float) Math.min(Float.MAX_VALUE, damage * 1.35);
        }
        return damage;
    }

    public static double guardPressure(Entity attacker, LivingEntity target) {
        if (!(attacker instanceof ServerPlayer player)) return 1.0;
        Session state = session(player);
        return state.executing && state.target == target ? state.strike.guardMultiplier() : 1.0;
    }

    public static boolean powerfulStrike(Entity attacker, LivingEntity target) {
        if (!(attacker instanceof ServerPlayer player)) return false;
        Session state = session(player);
        return state.executing && state.target == target && state.strike.powerful();
    }

    public static float incomingDamage(ServerPlayer victim, DamageSource source, float damage) {
        Session state = session(victim);
        return source.getEntity() instanceof LivingEntity && state.time.exposed(now(victim)) ? damage * 1.25F : damage;
    }

    public static void breakGuard(ServerPlayer victim) {
        session(victim).time.recover(now(victim), MeleeTimeline.GUARD_BREAK_TICKS, true);
        sync(victim, CombatFeedback.GUARD_BREAK);
        victim.serverLevel().sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY(0.7), victim.getZ(),
                16, 0.35, 0.35, 0.35, 0.2);
        victim.level().playSound(null, victim.blockPosition(), SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 0.8F, 0.85F);
    }

    public static void interruptOnHit(ServerPlayer player) {
        session(player).combo.reset();
        // Immediate punches have no held action to interrupt.
    }

    public static boolean fromFront(ServerPlayer victim, Entity attacker) {
        if (attacker == null) return false;
        Vec3 direction = attacker.position().subtract(victim.position()).multiply(1, 0, 1);
        Vec3 look = victim.getLookAngle().multiply(1, 0, 1);
        return direction.lengthSqr() < 0.0001 || look.lengthSqr() < 0.0001
                || look.normalize().dot(direction.normalize()) > 0.1;
    }

    private static boolean directMelee(DamageSource source) {
        String id = source.getMsgId();
        return source.getDirectEntity() == source.getEntity() && source.getEntity() instanceof LivingEntity
                && ("player".equals(id) || "mob".equals(id) || "mobNoAggro".equals(id));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void adjustMeleeKnockback(LivingKnockBackEvent event) {
        if (event.getEntity().level().isClientSide() || resolvingAttack == null) return;
        Session state = session(resolvingAttack);
        // Vanilla knocks sweep bystanders back before hurt() reaches our secondary-hit filter.
        if (state.executing && event.getEntity() != state.target && event.getEntity() != resolvingAttack)
            event.setCanceled(true);
        else if (state.executing && event.getEntity() == state.target && state.strike != null && !state.strike.powerful())
            event.setStrength((float) (event.getStrength() * LightCombo.knockbackMultiplier(state.comboStage)));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void parry(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide() || !directMelee(event.getSource()) || event.getAmount() <= 0) return;
        LivingEntity attacker = (LivingEntity) event.getSource().getEntity();
        if (attacker instanceof ServerPlayer player) {
            Session state = session(player);
            if (state.time.recovering(now(player)) || state.executing && state.target != event.getEntity()) {
                event.setCanceled(true);
                return;
            }
        } else if (attacker.level().getGameTime() < attacker.getPersistentData().getLong(MOB_STAGGER)) {
            event.setCanceled(true);
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer victim) || !victim.isAlive() || victim.isCreative()
                || !CombatState.isBlocking(victim) || !fromFront(victim, attacker)) return;
        Session guard = session(victim);
        victim.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
            double cost = Math.max(0.5, ResourceCosts.meleeStaminaCost(data) * 0.5);
            if (!data.race().hasInfiniteEnergy() && data.stamina() < cost) return;
            if (!guard.time.parry(now(victim), attacker.getId())) return;
            event.setCanceled(true);
            StaminaSystem.spend(victim, data, cost);
            if (attacker instanceof ServerPlayer player) {
                session(player).time.recover(now(player), MeleeTimeline.PARRIED_TICKS, true);
                sync(player, CombatFeedback.PARRIED);
                CombatState.markCombat(player, 0.6);
            } else {
                attacker.getPersistentData().putLong(MOB_STAGGER, attacker.level().getGameTime() + MeleeTimeline.PARRIED_TICKS);
            }
            attacker.knockback(0.25, victim.getX() - attacker.getX(), victim.getZ() - attacker.getZ());
            attacker.hurtMarked = true;
            victim.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK, victim.getX(), victim.getY(0.7), victim.getZ(),
                    12, 0.3, 0.2, 0.3, 0.1);
            victim.level().playSound(null, victim.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.25F, 1.8F);
            CombatState.markCombat(victim, 0.35);
            sync(victim, CombatFeedback.PARRY);
            PlayerDataEvents.sync(victim);
            data.clearDirty();
        });
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void preventRecoveryAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !session(player).executing
                && (actionLocked(player) || CombatState.isBlocking(player))) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void staggerMob(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide() && mob.getPersistentData().contains(MOB_STAGGER)) {
            if (mob.level().getGameTime() >= mob.getPersistentData().getLong(MOB_STAGGER)) mob.getPersistentData().remove(MOB_STAGGER);
            else mob.getNavigation().stop();
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
            CombatState.setBlocking(player, false);
            sync(player, CombatFeedback.NONE);
        }
    }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player);
            CombatState.setBlocking(player, false);
            sync(player, CombatFeedback.NONE);
        }
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) { SESSIONS.clear(); }

    private static void sync(ServerPlayer player, CombatFeedback feedback) {
        MeleeTimeline time = session(player).time;
        KicraftNetwork.sendToPlayer(player, new MeleeStatePacket(time.recoveryTicks(now(player)),
                time.exposedTicks(now(player)), time.counterTicks(now(player)), feedback));
    }

    private static void showCombo(ServerPlayer player, int stage) {
        if (stage < 2) return;
        Component message = Component.translatable(stage == LightCombo.MAX_STEPS
                        ? "combat.kicraft.combo_finisher" : "combat.kicraft.combo", stage, LightCombo.MAX_STEPS)
                .withStyle(stage == LightCombo.MAX_STEPS ? ChatFormatting.GOLD : ChatFormatting.YELLOW);
        player.displayClientMessage(message, true);
    }

    private static final class Session {
        final MeleeTimeline time = new MeleeTimeline();
        InteractionHand attackHand = InteractionHand.MAIN_HAND;
        MeleeTimeline.Strike strike;
        LivingEntity target;
        boolean executing;
        boolean connected;
        boolean blocked;
        boolean counter;
        int comboStage;
        double paidStamina;
        final LightCombo combo = new LightCombo();
    }
}
