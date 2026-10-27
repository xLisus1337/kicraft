package net.kicraft.kicraft.server;

import net.kicraft.kicraft.Kicraft;
import net.kicraft.kicraft.core.player.KiRace;
import net.kicraft.kicraft.core.player.KiStat;
import net.kicraft.kicraft.core.player.PlayerData;
import net.kicraft.kicraft.server.capability.PlayerDataCapability;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Kicraft.MODID)
public final class RaceIdentityEvents {
    private static final String ZENKAI_READY_TICK_TAG = "kicraftZenkaiReadyTick";
    private static final String ZENKAI_COOLDOWN_TICK_TAG = "kicraftZenkaiCooldownTick";
    private static final String ZENKAI_PRESSURE_TAG = "kicraftZenkaiPressure";
    private static final String ZENKAI_ENTITY_DAMAGE_TAG = "kicraftZenkaiEntityDamage";
    private static final String LAST_ENTITY_HIT_TICK_TAG = "kicraftLastEntityHitTick";
    private static final String LAST_RAGE_COMBAT_TICK_TAG = "kicraftLastRageCombatTick";
    private static final int ZENKAI_DELAY_TICKS = 160;
    private static final int ZENKAI_COOLDOWN_TICKS = 2400;
    private static final int RAGE_COMBAT_MEMORY_TICKS = 100;
    private static final int RAGE_RESET_DELAY_TICKS = 600;

    private RaceIdentityEvents() {
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide() || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (!player.isAlive() || player.isDeadOrDying() || player.getHealth() <= 0.0F) {
            return;
        }

        player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
            boolean changed = false;
            changed |= applyPassiveIdentity(player, data);
            changed |= applyPendingZenkai(player, data);
            changed |= resetRageAfterCombat(player, data);

            if (changed || data.isDirty()) {
                PlayerDataEvents.requestSync(player);
                data.clearDirty();
            }
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void raceDamageIdentity(LivingDamageEvent event) {
        if (event.getAmount() <= 0.0F) return;
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            attacker.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
                rememberRageCombat(attacker);
                boolean changed = applyOffensiveIdentity(attacker, data, event.getAmount());
                if (changed) {
                    PlayerDataEvents.sync(attacker);
                    data.clearDirty();
                }
            });
        }

        if (event.getEntity() instanceof ServerPlayer victim) {
            victim.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
                boolean changed = false;
                boolean entityAttack = isDamageFromLivingEntity(event, victim);
                if (entityAttack) {
                    rememberEntityAttack(victim);
                    rememberRageCombat(victim);
                }
                changed |= queueZenkaiIfNeeded(victim, data, event.getAmount(), entityAttack);
                changed |= applyDefensiveIdentity(victim, data, event);
                if (changed) {
                    PlayerDataEvents.sync(victim);
                    data.clearDirty();
                }
            });
        }
    }

    private static boolean applyPassiveIdentity(ServerPlayer player, PlayerData data) {
        boolean changed = false;
        KiRace race = data.race();
        double hpRatio = data.maxHp() <= 0.0 ? 1.0 : data.hp() / data.maxHp();

        if ((race == KiRace.SAIYAN || race == KiRace.HALF_SAIYAN)
                && hpRatio <= 0.35
                && hasRecentEntityAttack(player)) {
            data.setRage(data.rage() + (race == KiRace.SAIYAN ? 0.045 : 0.025));
            changed = true;
        }

        if (race == KiRace.NAMEKIAN && player.tickCount % 20 == 0 && data.hp() < data.maxHp()) {
            double heal = data.maxHp() * 0.006;
            double kiCost = Math.max(0.35, data.maxKi() * 0.002);
            if (data.ki() >= kiCost) {
                data.setKi(data.ki() - kiCost);
                healKiHealth(player, data, heal);
                changed = true;
            }
        }

        if (race == KiRace.MAJIN && player.tickCount % 10 == 0 && data.hp() < data.maxHp()) {
            double staminaRatio = data.maxStamina() <= 0.0 ? 1.0 : data.stamina() / data.maxStamina();
            double heal = data.maxHp() * (0.005 + Math.max(0.0, staminaRatio) * 0.003);
            double staminaCost = Math.max(0.25, data.maxStamina() * 0.0015);
            if (data.stamina() >= staminaCost) {
                StaminaSystem.spend(player, data, staminaCost);
                healKiHealth(player, data, heal);
                changed = true;
            }
        }

        if (race == KiRace.BIO_ANDROID && player.tickCount % 40 == 0 && data.hp() < data.maxHp()) {
            healKiHealth(player, data, data.maxHp() * 0.0015);
            changed = true;
        }

        if (ServerKiState.isCharging(player)) {
            if (race == KiRace.HUMAN) {
                data.trainFocus(0.018);
                changed = true;
            } else if (race == KiRace.NAMEKIAN || race == KiRace.FROST_DEMON) {
                data.trainFocus(0.010);
                changed = true;
            }
        }

        return changed;
    }

    private static boolean resetRageAfterCombat(ServerPlayer player, PlayerData data) {
        if (data.rage() <= 0.0) {
            return false;
        }

        CompoundTag tag = player.getPersistentData();
        long lastCombatTick = tag.getLong(LAST_RAGE_COMBAT_TICK_TAG);
        if (lastCombatTick <= 0L || player.level().getGameTime() - lastCombatTick < RAGE_RESET_DELAY_TICKS) {
            return false;
        }

        data.setRage(0.0);
        tag.remove(LAST_RAGE_COMBAT_TICK_TAG);
        return true;
    }

    private static boolean applyOffensiveIdentity(ServerPlayer attacker, PlayerData data, float damage) {
        KiRace race = data.race();
        double trainingDamage = trainingPressure(damage);
        if (race == KiRace.HALF_SAIYAN) {
            double lowHpBoost = data.maxHp() > 0.0 && data.hp() / data.maxHp() <= 0.35 ? 1.85 : 1.0;
            data.train(KiStat.STRENGTH, trainingDamage * 0.07 * lowHpBoost);
            data.train(KiStat.STAMINA, trainingDamage * 0.018 * lowHpBoost);
            data.trainFocus(0.006 * lowHpBoost);
            return true;
        }

        if (race == KiRace.BIO_ANDROID) {
            data.train(KiStat.STRENGTH, trainingDamage * 0.09);
            data.train(KiStat.MAX_KI, trainingDamage * 0.025);
            if (attacker.tickCount % 20 == 0) {
                data.setKi(Math.min(data.maxKi(), data.ki() + Math.max(0.2, trainingDamage * 0.12)));
            }
            return true;
        }

        if (race == KiRace.ANDROID) {
            data.train(KiStat.STRENGTH, trainingDamage * 0.045);
            data.train(KiStat.KI_CONTROL, trainingDamage * 0.030);
            if (attacker.tickCount % 8 == 0) {
                attacker.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK, attacker.getX(), attacker.getY() + 1.0, attacker.getZ(), 3, 0.25, 0.35, 0.25, 0.02);
            }
            return true;
        }

        return false;
    }

    private static double trainingPressure(float damage) {
        return Math.max(0.1, Math.min(24.0, Math.sqrt(Math.max(0.1, damage)) * 2.0));
    }

    private static boolean applyDefensiveIdentity(ServerPlayer victim, PlayerData data, LivingDamageEvent event) {
        KiRace race = data.race();
        float damage = event.getAmount();
        boolean trainsDurability = !event.getSource().is(DamageTypeTags.IS_FALL);
        if (race == KiRace.HALF_SAIYAN && data.maxHp() > 0.0 && data.hp() / data.maxHp() <= 0.45) {
            if (trainsDurability) data.train(KiStat.DURABILITY, damage * 0.08);
            data.train(KiStat.STAMINA, damage * 0.05);
            return true;
        }

        if (race == KiRace.BIO_ANDROID) {
            if (trainsDurability) data.train(KiStat.DURABILITY, damage * 0.14);
            data.train(KiStat.MAX_KI, damage * 0.035);
            data.train(KiStat.KI_CONTROL, damage * 0.012);
            return true;
        }

        if (race == KiRace.ANDROID) {
            event.setAmount(Math.max(0.1F, damage * 0.92F));
            if (trainsDurability) data.train(KiStat.DURABILITY, damage * 0.045);
            data.train(KiStat.KI_CONTROL, damage * 0.035);
            healKiHealth(victim, data, damage * 0.06);
            if (victim.tickCount % 6 == 0) {
                victim.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK, victim.getX(), victim.getY() + 1.0, victim.getZ(), 4, 0.28, 0.35, 0.28, 0.02);
            }
            return true;
        }

        if (race == KiRace.MAJIN) {
            if (trainsDurability) data.train(KiStat.DURABILITY, damage * 0.06);
            return true;
        }

        return false;
    }

    private static boolean queueZenkaiIfNeeded(ServerPlayer player, PlayerData data, float damage, boolean entityAttack) {
        KiRace race = data.race();
        if (!entityAttack || (race != KiRace.SAIYAN && race != KiRace.HALF_SAIYAN)) {
            return false;
        }

        CompoundTag tag = player.getPersistentData();
        long gameTime = player.level().getGameTime();
        if (gameTime < tag.getLong(ZENKAI_COOLDOWN_TICK_TAG) || tag.getLong(ZENKAI_READY_TICK_TAG) > 0L) {
            return false;
        }

        double afterHealth = Math.max(0.0, player.getHealth() - damage);
        double afterRatio = player.getMaxHealth() <= 0.0F ? 1.0 : afterHealth / player.getMaxHealth();
        if (afterHealth <= 0.0 || afterRatio > 0.18) {
            return false;
        }

        tag.putLong(ZENKAI_READY_TICK_TAG, gameTime + ZENKAI_DELAY_TICKS);
        tag.putDouble(ZENKAI_PRESSURE_TAG, Math.max(1.0, damage));
        tag.putBoolean(ZENKAI_ENTITY_DAMAGE_TAG, true);
        return true;
    }

    private static boolean applyPendingZenkai(ServerPlayer player, PlayerData data) {
        CompoundTag tag = player.getPersistentData();
        long readyTick = tag.getLong(ZENKAI_READY_TICK_TAG);
        if (readyTick <= 0L || player.level().getGameTime() < readyTick) {
            return false;
        }

        if (!tag.getBoolean(ZENKAI_ENTITY_DAMAGE_TAG)
                || (data.race() != KiRace.SAIYAN && data.race() != KiRace.HALF_SAIYAN)) {
            clearZenkai(tag);
            return true;
        }

        double pressure = Math.max(1.0, tag.getDouble(ZENKAI_PRESSURE_TAG));
        double raceScale = data.race() == KiRace.SAIYAN ? 1.0 : 0.45;
        double boost = (6.0 + pressure * 0.75 + data.powerLevel() * 0.003) * raceScale;
        data.train(KiStat.STRENGTH, boost);
        data.train(KiStat.DURABILITY, boost * 0.85);
        data.train(KiStat.STAMINA, boost * 0.65);
        data.setRage(data.rage() + (data.race() == KiRace.SAIYAN ? 10.0 : 5.0));

        tag.putLong(ZENKAI_COOLDOWN_TICK_TAG, player.level().getGameTime() + ZENKAI_COOLDOWN_TICKS);
        clearZenkai(tag);
        player.displayClientMessage(Component.translatable("message.kicraft.zenkai_awakened"), true);
        return true;
    }

    private static void clearZenkai(CompoundTag tag) {
        tag.remove(ZENKAI_READY_TICK_TAG);
        tag.remove(ZENKAI_PRESSURE_TAG);
        tag.remove(ZENKAI_ENTITY_DAMAGE_TAG);
    }

    private static boolean isDamageFromLivingEntity(LivingDamageEvent event, ServerPlayer victim) {
        Entity attacker = event.getSource().getEntity();
        return attacker instanceof LivingEntity && attacker != victim && attacker.isAlive();
    }

    private static void rememberEntityAttack(ServerPlayer player) {
        player.getPersistentData().putLong(LAST_ENTITY_HIT_TICK_TAG, player.level().getGameTime());
    }

    private static void rememberRageCombat(ServerPlayer player) {
        player.getPersistentData().putLong(LAST_RAGE_COMBAT_TICK_TAG, player.level().getGameTime());
    }

    private static boolean hasRecentEntityAttack(ServerPlayer player) {
        long lastHitTick = player.getPersistentData().getLong(LAST_ENTITY_HIT_TICK_TAG);
        return lastHitTick > 0L && player.level().getGameTime() - lastHitTick <= RAGE_COMBAT_MEMORY_TICKS;
    }

    private static void healKiHealth(ServerPlayer player, PlayerData data, double amount) {
        if (amount <= 0.0) {
            return;
        }

        double before = data.hp();
        data.setHp(Math.min(data.maxHp(), data.hp() + amount));
        if (data.maxHp() > 0.0 && data.hp() > before) {
            float vanillaTarget = (float) (player.getMaxHealth() * (data.hp() / data.maxHp()));
            if (vanillaTarget > player.getHealth()) {
                player.setHealth(Math.min(player.getMaxHealth(), vanillaTarget));
            }
        }
    }
}
