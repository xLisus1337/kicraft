package net.kicraft.kicraft.server;

import net.kicraft.kicraft.Kicraft;
import net.kicraft.kicraft.core.player.KiTransformation;
import net.kicraft.kicraft.core.player.KiStat;
import net.kicraft.kicraft.core.player.ResourceCosts;
import net.kicraft.kicraft.network.KicraftNetwork;
import net.kicraft.kicraft.network.StatXpGainPacket;
import net.kicraft.kicraft.server.capability.PlayerDataCapability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumMap;

@Mod.EventBusSubscriber(modid = Kicraft.MODID)
public final class TrainingEvents {
    private static final double EXHAUSTED_DAMAGE_MULTIPLIER = 0.15;
    private static final double EXHAUSTED_STAMINA_RATIO = 0.01;
    private static final double BLOCK_DAMAGE_REDUCTION = 0.70;

    private static final double DUMMY_STRENGTH_XP_PER_HIT = 0.75;
    private static final double DUMMY_STAMINA_XP_PER_HIT = 0.18;
    private static final double DUMMY_XP_VARIANCE = 0.20;

    private TrainingEvents() {
    }

    @SubscribeEvent
    public static void damageTraining(LivingDamageEvent event) {
        // Vanilla shields/absorption may have already reduced this hit to zero.
        if (event.getAmount() <= 0.0F) return;
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            attacker.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
                float originalDamage = event.getAmount();
                CombatState.markCombat(attacker, Math.max(0.1, originalDamage) * 0.10);

                if (isMeleePlayerAttack(event)) {
                    EnumMap<KiStat, Double> combatXp = new EnumMap<>(KiStat.class);
                    recordXp(combatXp, KiStat.STAMINA, drainMeleeStamina(attacker, data));
                    float strengthDamage = (float) Math.max(0.1, Math.min(Float.MAX_VALUE, originalDamage * data.meleeDamageMultiplier()));
                    float comboDamage = MeleeCombat.outgoingDamage(attacker, event.getEntity(), strengthDamage);
                    float adjustedDamage = applyStaminaDamagePenalty(comboDamage, data.stamina(), data.maxStamina());
                    event.setAmount(adjustedDamage);
                    applyStrengthKnockback(attacker, event.getEntity(), data.stat(KiStat.STRENGTH), data.stamina(), data.maxStamina());
                    if (ServerTrainingState.isTrainingDummy(event.getEntity())) {
                        // The dummy receives only cosmetic damage, so use a stable
                        // training reward rather than its deliberately tiny hit value.
                        recordXp(combatXp, KiStat.STRENGTH, data.train(KiStat.STRENGTH,
                                variedDummyXp(attacker, DUMMY_STRENGTH_XP_PER_HIT)));
                        recordXp(combatXp, KiStat.STAMINA, data.train(KiStat.STAMINA,
                                variedDummyXp(attacker, DUMMY_STAMINA_XP_PER_HIT)));
                    } else {
                        recordXp(combatXp, KiStat.STRENGTH,
                                data.train(KiStat.STRENGTH, Math.max(0.1, originalDamage) * 0.35));
                    }
                    if (!combatXp.isEmpty()) {
                        KicraftNetwork.sendToPlayer(attacker, new StatXpGainPacket(combatXp));
                    }
                    data.trainTransformation(data.activeTransformation(), transformationCombatMasteryAmount(data.activeTransformation(), adjustedDamage, true));
                    PlayerDataEvents.sync(attacker);
                    data.clearDirty();
                }
            });
        }

        if (event.getEntity() instanceof ServerPlayer victim) {
            victim.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
                event.setAmount(MeleeCombat.incomingDamage(victim, event.getSource(), event.getAmount()));
                float incoming = event.getAmount();
                applyBlock(victim, data, event);
                float afterBlock = event.getAmount();
                double durability = data.stat(KiStat.DURABILITY);
                float reduced = (float) Math.max(0.1, afterBlock * (1.0 - data.damageReduction()));
                event.setAmount(reduced);
                if (net.kicraft.kicraft.core.combat.CombatRules.CUSTOM_MELEE_ENABLED) {
                    applyStagger(victim, afterBlock, durability);
                }

                if (isDamageFromLivingEntity(event, victim)) {
                    MeleeCombat.interruptOnHit(victim);
                    CombatState.markCombat(victim, Math.max(0.1, incoming) * 0.12);
                }
                boolean trainsDurability = !event.getSource().is(DamageTypeTags.IS_FALL);
                if (trainsDurability) data.train(KiStat.DURABILITY, incoming * 0.28);
                data.trainTransformation(data.activeTransformation(), transformationCombatMasteryAmount(data.activeTransformation(), incoming, false));
                if (victim.getHealth() - reduced <= victim.getMaxHealth() * 0.35) {
                    if (trainsDurability) data.train(KiStat.DURABILITY, incoming * 0.18);
                    data.trainTransformation(data.activeTransformation(), transformationCombatMasteryAmount(data.activeTransformation(), incoming, false) * 0.65);
                }
                PlayerDataEvents.sync(victim);
                data.clearDirty();
            });
        }
    }

    @SubscribeEvent
    public static void scaleKnockback(LivingKnockBackEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer victim) {
            victim.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
                double durability = data.stat(KiStat.DURABILITY);
                float resistance = (float) (1.0 + Math.log10(1.0 + Math.max(0.0, durability - 1.0)) * 0.35);
                event.setStrength(Math.max(0.02F, event.getStrength() / resistance));
            });
        }
    }

    private static void applyBlock(ServerPlayer victim, net.kicraft.kicraft.core.player.PlayerData data, LivingDamageEvent event) {
        if (!CombatState.isBlocking(victim)
                || !isDamageFromLivingEntity(event, victim)
                || !MeleeCombat.fromFront(victim, event.getSource().getEntity())) {
            return;
        }

        float incoming = event.getAmount();
        if (incoming <= 0.0F) {
            return;
        }

        double effectiveness = 1.0;
        Entity attacker = event.getSource().getEntity();
        double cost = blockStaminaCost(victim, data, incoming, attacker) * MeleeCombat.guardPressure(attacker, victim);
        boolean guardBroken = MeleeCombat.powerfulStrike(attacker, victim);
        if (!data.race().hasInfiniteEnergy()) {
            if (data.stamina() <= 0.0) {
                return;
            }

            double available = data.stamina();
            guardBroken |= cost >= available;
            effectiveness = Math.min(1.0, available / Math.max(0.1, cost));
            StaminaSystem.spend(victim, data, Math.min(available, cost));
        }

        double reduction = guardBroken ? 0.20 : BLOCK_DAMAGE_REDUCTION * Math.max(0.20, effectiveness);
        event.setAmount((float) Math.max(0.1, incoming * (1.0 - reduction)));
        if (guardBroken) MeleeCombat.breakGuard(victim);
        data.train(KiStat.STAMINA, Math.max(0.01, cost * 0.004));
        if (!event.getSource().is(DamageTypeTags.IS_FALL)) {
            data.train(KiStat.DURABILITY, incoming * 0.055);
        }
        CombatState.markCombat(victim, incoming * 0.18);
    }

    private static double blockStaminaCost(ServerPlayer victim, net.kicraft.kicraft.core.player.PlayerData data, float damage, Entity source) {
        double attackerPower = source instanceof LivingEntity living ? CombatState.powerScore(living) : damage * 12.0;
        double pressure = Math.log10(1.0 + Math.max(1.0, attackerPower));
        double baseCost = Math.max(2.0, damage * (2.8 + pressure * 0.65));
        return baseCost * ResourceCosts.staminaCostMultiplier(data) * CombatState.staminaDrainScale(victim);
    }

    private static boolean isMeleePlayerAttack(LivingDamageEvent event) {
        return event.getSource().getEntity() instanceof ServerPlayer
                && "player".equals(event.getSource().getMsgId());
    }

    private static boolean isDamageFromLivingEntity(LivingDamageEvent event, ServerPlayer victim) {
        Entity attacker = event.getSource().getEntity();
        return attacker instanceof LivingEntity && attacker != victim && attacker.isAlive();
    }

    private static void recordXp(EnumMap<KiStat, Double> gains, KiStat stat, double amount) {
        if (amount > 0.0 && Double.isFinite(amount)) {
            gains.merge(stat, amount, Double::sum);
        }
    }

    private static double variedDummyXp(ServerPlayer player, double baseAmount) {
        return baseAmount * (0.80 + player.getRandom().nextDouble() * DUMMY_XP_VARIANCE * 2.0);
    }

    private static void applyStagger(ServerPlayer victim, float incomingDamage, double durability) {
        double resistance = Math.log10(1.0 + Math.max(0.0, durability - 1.0)) * 0.28;
        double damagePressure = Math.min(1.0, incomingDamage / Math.max(1.0F, victim.getMaxHealth() * 0.18F));
        int duration = (int) Math.round((8 + damagePressure * 10) / (1.0 + resistance));
        if (duration <= 2) {
            return;
        }

        int amplifier = damagePressure > 0.65 && durability < 25.0 ? 1 : 0;
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, amplifier, false, false, false));
    }

    private static void applyStrengthKnockback(ServerPlayer attacker, LivingEntity victim, double strength, double stamina, double maxStamina) {
        if (attacker == victim || victim.level().isClientSide()) {
            return;
        }

        double staminaRatio = maxStamina <= 0.0 ? 1.0 : Math.max(0.0, Math.min(1.0, stamina / maxStamina));
        double tier = Math.log10(1.0 + Math.max(0.0, strength - 1.0));
        double bonus = Math.pow(tier, 1.45) * 0.11 * (0.35 + staminaRatio * 0.65);
        if (bonus <= 0.001) {
            return;
        }

        Vec3 direction = victim.position().subtract(attacker.position());
        Vec3 horizontal = new Vec3(direction.x, 0.0, direction.z);
        if (horizontal.lengthSqr() < 0.0001) {
            horizontal = attacker.getLookAngle().multiply(1.0, 0.0, 1.0);
        }
        if (horizontal.lengthSqr() < 0.0001) {
            return;
        }

        Vec3 push = horizontal.normalize().scale(Math.min(7.0, bonus));
        victim.setDeltaMovement(victim.getDeltaMovement().add(push.x, Math.min(1.1, bonus * 0.18), push.z));
        victim.hasImpulse = true;
    }

    private static float applyStaminaDamagePenalty(float damage, double stamina, double maxStamina) {
        if (maxStamina <= 0.0) {
            return damage;
        }
        double staminaRatio = Math.max(0.0, Math.min(1.0, stamina / maxStamina));
        if (staminaRatio <= EXHAUSTED_STAMINA_RATIO) {
            return (float) Math.max(0.1F, damage * EXHAUSTED_DAMAGE_MULTIPLIER);
        }
        double recoveredRatio = (staminaRatio - EXHAUSTED_STAMINA_RATIO) / (1.0 - EXHAUSTED_STAMINA_RATIO);
        // The last percent is crippling, but 40-60% Stamina should still feel usable.
        double multiplier = EXHAUSTED_DAMAGE_MULTIPLIER
                + (1.0 - EXHAUSTED_DAMAGE_MULTIPLIER) * Math.pow(recoveredRatio, 0.35);
        return (float) Math.max(0.1F, damage * multiplier);
    }

    private static double drainMeleeStamina(ServerPlayer attacker, net.kicraft.kicraft.core.player.PlayerData data) {
        if (data.race().hasInfiniteEnergy()) {
            return 0.0;
        }

        double cost = MeleeCombat.paidForStrike(attacker) ? MeleeCombat.paidStamina(attacker)
                : ResourceCosts.meleeStaminaCost(data) * CombatState.staminaDrainScale(attacker);
        if (!MeleeCombat.paidForStrike(attacker)) StaminaSystem.spend(attacker, data, cost);
        return data.train(KiStat.STAMINA, ResourceCosts.meleeStaminaTrainingAmount(cost));
    }

    private static double transformationCombatMasteryAmount(KiTransformation transformation, float damage, boolean attacking) {
        if (transformation == null || transformation.isBase() || damage <= 0.0F) {
            return 0.0;
        }

        double damagePressure = Math.min(1.0, Math.log10(1.0 + Math.max(0.1F, damage)) / 2.0);
        double base = attacking ? 0.012 : 0.008;
        return base * (0.35 + damagePressure);
    }
}
