package net.kicraft.kicraft.server;

import net.kicraft.kicraft.Kicraft;
import net.kicraft.kicraft.core.player.KiRace;
import net.kicraft.kicraft.core.player.KiStat;
import net.kicraft.kicraft.core.player.KiTechnique;
import net.kicraft.kicraft.core.player.KiTransformation;
import net.kicraft.kicraft.core.player.ResourceCosts;
import net.kicraft.kicraft.server.capability.PlayerDataCapability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = Kicraft.MODID)
public final class ResourceTickEvents {
    private static final String TRANSFORMATION_DESTABILIZE_TICKS_TAG = "kicraftTransformationDestabilizeTicks";
    private static final UUID EXHAUSTED_ATTACK_SPEED_MODIFIER_ID = UUID.fromString("f347b78f-b0c1-4be3-b1c4-02a2549ca28f");
    private static final int TRANSFORMATION_DESTABILIZE_LIMIT_TICKS = 50;
    private static final UUID TRANSFORMATION_SPEED_MODIFIER_ID = UUID.fromString("f106a651-8d2e-4d45-89e6-8a00513d5c67");

    private ResourceTickEvents() {
    }

    public static void clearTransformationInstability(ServerPlayer player) {
        player.getPersistentData().remove(TRANSFORMATION_DESTABILIZE_TICKS_TAG);
    }

    @SubscribeEvent
    public static void preventFlightFallDamage(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
            if (data.hasTechnique(KiTechnique.FLIGHT) && ServerKiState.isFlying(player)) {
                player.fallDistance = 0.0F;
                event.setDistance(0.0F);
                event.setDamageMultiplier(0.0F);
                if (event.isCancelable()) {
                    event.setCanceled(true);
                }
            }
        });
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.player.level().isClientSide() || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (!player.isAlive() || player.isDeadOrDying() || player.getHealth() <= 0.0F) {
            OozaruTransformationSystem.clear(player);
            ServerKiState.clear(player);
            return;
        }
        if (player.isSpectator()) {
            OozaruTransformationSystem.clear(player);
            ServerKiState.clear(player);
            return;
        }

        player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
            if (forceDeathIfNeeded(player, data)) {
                return;
            }

            if (event.phase == TickEvent.Phase.START) {
                stopSprintIfExhausted(player, data);
                return;
            }

            boolean changed = false;
            boolean wasInCombat = CombatState.isInCombat(player);
            changed |= CombatState.tickFatigue(player);
            changed |= wasInCombat != CombatState.isInCombat(player);
            if (!data.race().hasInfiniteEnergy()) {
                changed |= StaminaSystem.tick(player, data);
            } else {
                if (data.stamina() < data.maxStamina()) {
                    data.setStamina(data.maxStamina());
                    changed = true;
                }
                if (data.ki() < data.maxKi()) {
                    data.setKi(data.maxKi());
                    changed = true;
                }
            }

            syncExhaustedAttackSpeed(player, data);

            if (ServerKiState.isCharging(player)) {
                double charge = data.maxKi() * 0.0025;
                data.setKi(data.ki() + charge);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 4, 2, false, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 6, 0, false, false, false));
                if (player.tickCount % 2 == 0) {
                    ServerLevel level = player.serverLevel();
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.0, player.getZ(), 4, 0.45, 0.65, 0.45, 0.015);
                    level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.9, player.getZ(), 2, 0.3, 0.45, 0.3, 0.01);
                    level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.05, player.getZ(), 3, 0.55, 0.05, 0.55, 0.015);
                }
                if (player.tickCount % 18 == 0) {
                    player.level().playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.16F, 0.65F);
                }
                changed = true;
            }

            changed |= updateFlight(player, data);
            FlightManeuverSystem.tick(player);
            changed |= updateKiTransfer(player, data);
            changed |= OozaruTransformationSystem.tick(player, data);
            changed |= updateTransformation(player, data);
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(0.0F);
            syncKiHealthFromVanilla(player, data);
            syncVanillaAttributes(player, data);
            syncTransformationSpeed(player, data);
            if (changed || data.isDirty() || player.tickCount % 100 == 0) {
                PlayerDataEvents.requestSync(player);
                data.clearDirty();
            }
        });
    }

    private static boolean forceDeathIfNeeded(ServerPlayer player, net.kicraft.kicraft.core.player.PlayerData data) {
        if (player.isCreative() || player.isSpectator() || data.hp() > 0.0) {
            return false;
        }

        ServerKiState.clear(player);
        player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
        return true;
    }

    private static void stopSprintIfExhausted(ServerPlayer player, net.kicraft.kicraft.core.player.PlayerData data) {
        StaminaSystem.enforceSprintLock(player, data);
    }

    private static boolean updateFlight(ServerPlayer player, net.kicraft.kicraft.core.player.PlayerData data) {
        if (player.isCreative() || player.isSpectator()) {
            return false;
        }

        boolean enabled = ServerKiState.isFlying(player);
        if (!enabled) {
            if (player.getAbilities().mayfly || player.getAbilities().flying) {
                disableFlight(player);
            }
            return false;
        }

        if (!data.hasTechnique(KiTechnique.FLIGHT)) {
            ServerKiState.setFlying(player, false);
            disableFlight(player);
            return true;
        }

        if (player.onGround()) {
            ServerKiState.setFlying(player, false);
            disableFlight(player);
            return true;
        }

        player.getAbilities().mayfly = true;
        player.getAbilities().setFlyingSpeed(0.055F);

        boolean changed = false;
        if (!data.race().hasInfiniteEnergy()) {
            boolean moving = player.getDeltaMovement().lengthSqr() > 0.0025 || Math.abs(player.xxa) > 0.001F || Math.abs(player.zza) > 0.001F;
            boolean boosted = moving && ServerKiState.isFlightBoosting(player);
            double kiDrain = ResourceCosts.flightKiDrain(data, moving, boosted);
            double staminaDrain = ResourceCosts.flightStaminaDrain(data, moving, boosted);
            if (!StaminaSystem.canStartExertion(player, data)
                    || data.ki() < kiDrain || data.stamina() < staminaDrain || data.ki() <= 0.0 || data.stamina() <= 0.0) {
                ServerKiState.setFlying(player, false);
                disableFlight(player);
                return true;
            }

            data.setKi(data.ki() - kiDrain);
            StaminaSystem.spend(player, data, staminaDrain);
            double trainingScale = moving ? 1.0 : 0.35;
            data.train(KiStat.STAMINA, 0.0012 * trainingScale);
            data.train(KiStat.MAX_KI, 0.0008 * trainingScale);
            data.train(KiStat.KI_CONTROL, 0.0007 * trainingScale);
            if (player.tickCount % 3 == 0) {
                ServerLevel level = player.serverLevel();
                level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.12, player.getZ(), moving ? 3 : 1, 0.25, 0.05, 0.25, 0.01);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 0.45, player.getZ(), moving ? 2 : 1, 0.18, 0.16, 0.18, 0.006);
            }
            changed = true;
        }

        player.onUpdateAbilities();
        return changed;
    }

    private static boolean updateKiTransfer(ServerPlayer player, net.kicraft.kicraft.core.player.PlayerData data) {
        if (!ServerKiState.isTransferring(player) || player.isSpectator()) {
            return false;
        }

        if (!data.hasTechnique(KiTechnique.KI_TRANSFER)) {
            ServerKiState.setTransferring(player, false);
            return true;
        }

        return KiActionUtil.findLookPlayer(player, 7.5, 0.88)
                .map(target -> transferKi(player, data, target))
                .orElse(false);
    }

    private static void disableFlight(ServerPlayer player) {
        ServerKiState.setFlightBoosting(player, false);
        FlightManeuverSystem.cancel(player);
        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;
        player.getAbilities().setFlyingSpeed(0.05F);
        player.fallDistance = 0.0F;
        player.onUpdateAbilities();
        PlayerDataEvents.syncFlightState(player);
    }

    private static boolean transferKi(ServerPlayer giver, net.kicraft.kicraft.core.player.PlayerData giverData, ServerPlayer target) {
        final boolean[] changed = {false};
        target.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(targetData -> {
            if (targetData.ki() >= targetData.maxKi()) {
                return;
            }

            double reserve = Math.max(1.0, giverData.maxKi() * 0.02);
            double drain = Math.max(0.35, giverData.maxKi() * 0.0035);
            if (!giverData.race().hasInfiniteEnergy()) {
                if (giverData.ki() - drain < reserve) {
                    return;
                }
                giverData.setKi(giverData.ki() - drain);
            }

            double efficiency = Math.min(0.95, 0.62 + Math.log10(Math.max(1.0, giverData.stat(KiStat.KI_CONTROL))) * 0.08);
            double received = Math.min(targetData.maxKi() - targetData.ki(), drain * efficiency);
            if (received <= 0.0) {
                return;
            }

            targetData.setKi(targetData.ki() + received);
            giverData.train(KiStat.KI_CONTROL, 0.004);
            targetData.train(KiStat.MAX_KI, 0.002);

            if (giver.tickCount % 2 == 0) {
                ServerLevel level = giver.serverLevel();
                KiActionUtil.spawnTrail(level, giver.getEyePosition(), target.getEyePosition(), ParticleTypes.ELECTRIC_SPARK, 14);
                KiActionUtil.spawnTrail(level, giver.position().add(0.0, 0.9, 0.0), target.position().add(0.0, 0.9, 0.0), ParticleTypes.END_ROD, 10);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + target.getBbHeight() * 0.65, target.getZ(), 2, 0.22, 0.3, 0.22, 0.01);
            }

            PlayerDataEvents.requestSync(target);
            targetData.clearDirty();
            changed[0] = true;
        });
        return changed[0];
    }

    private static boolean updateTransformation(ServerPlayer player, net.kicraft.kicraft.core.player.PlayerData data) {
        KiTransformation transformation = data.activeTransformation();
        if (transformation.isBase()) {
            clearTransformationInstability(player);
            return false;
        }
        if (!data.hasTransformation(transformation) || !transformation.allowedFor(data)) {
            data.activateTransformation(KiTransformation.BASE);
            return true;
        }

        double mastery = data.transformationMastery(transformation);
        double activityMultiplier = transformationActivityMultiplier(player);
        double kiDrain = transformationKiDrain(data, transformation, mastery, activityMultiplier);
        double staminaDrain = transformationStaminaDrain(data, transformation, mastery, activityMultiplier);
        if (data.race().hasInfiniteEnergy()) {
            kiDrain = 0.0;
            staminaDrain = 0.0;
            player.getPersistentData().remove(TRANSFORMATION_DESTABILIZE_TICKS_TAG);
        }

        boolean unstableKi = kiDrain > 0.0 && data.ki() <= kiDrain;
        boolean unstableStamina = staminaDrain > 0.0 && data.stamina() <= staminaDrain;
        if (unstableKi || unstableStamina) {
            if (kiDrain > 0.0) {
                data.setKi(data.ki() - kiDrain);
            }
            if (staminaDrain > 0.0) {
                StaminaSystem.spend(player, data, staminaDrain);
            }

            int unstableTicks = player.getPersistentData().getInt(TRANSFORMATION_DESTABILIZE_TICKS_TAG) + 1;
            player.getPersistentData().putInt(TRANSFORMATION_DESTABILIZE_TICKS_TAG, unstableTicks);
            if (unstableStamina) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 12, 1, false, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 12, 0, false, false, false));
            }
            if (unstableTicks >= TRANSFORMATION_DESTABILIZE_LIMIT_TICKS || unstableKi && data.ki() <= 0.0 && unstableTicks >= 24) {
                data.activateTransformation(KiTransformation.BASE);
                player.getPersistentData().remove(TRANSFORMATION_DESTABILIZE_TICKS_TAG);
            }
            return true;
        }

        player.getPersistentData().remove(TRANSFORMATION_DESTABILIZE_TICKS_TAG);
        if (kiDrain > 0.0) {
            data.setKi(data.ki() - kiDrain);
        }
        if (staminaDrain > 0.0) {
            StaminaSystem.spend(player, data, staminaDrain);
        }
        if (player.tickCount % 20 == 0) {
            double masteryPressure = 0.00024 + Math.max(0.0, activityMultiplier - 1.0) * 0.00018;
            data.trainTransformation(transformation, masteryPressure);
        }
        if (player.tickCount % 6 == 0) {
            ServerLevel level = player.serverLevel();
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.0, player.getZ(), 2, 0.35, 0.55, 0.35, 0.006);
        }
        return kiDrain > 0.0 || staminaDrain > 0.0 || player.tickCount % 20 == 0;
    }

    private static double transformationActivityMultiplier(ServerPlayer player) {
        boolean sprinting = player.isSprinting();
        boolean flying = ServerKiState.isFlying(player);
        boolean moving = Math.abs(player.xxa) > 0.001F
                || Math.abs(player.zza) > 0.001F
                || player.getDeltaMovement().horizontalDistanceSqr() > 0.00005;

        double multiplier = 1.0;
        if (moving) {
            multiplier += 0.12;
        }
        if (sprinting) {
            multiplier += 0.42;
        }
        if (flying) {
            multiplier += moving ? 0.48 : 0.22;
        } else if (!player.onGround()) {
            multiplier += 0.10;
        }
        if (CombatState.isBlocking(player)) {
            multiplier += 0.18;
        }
        multiplier *= CombatState.staminaDrainScale(player);
        return clamp(multiplier, 0.75, 2.65);
    }

    private static double transformationKiDrain(net.kicraft.kicraft.core.player.PlayerData data, KiTransformation transformation,
                                                double mastery, double activityMultiplier) {
        double baseDrain = transformation.kiDrainPerTick(mastery, data.stat(KiStat.KI_CONTROL));
        if (baseDrain <= 0.0) {
            return 0.0;
        }

        double masteryRatio = clamp(mastery / 100.0, 0.0, 1.0);
        double control = Math.log10(Math.max(1.0, data.stat(KiStat.KI_CONTROL)));
        double controlRelief = 1.0 / (1.0 + control * 0.62);
        double maxKiPressure = Math.max(0.0, Math.sqrt(Math.max(1.0, data.maxKi()) / 100.0) - 1.0);
        maxKiPressure = Math.min(120.0, maxKiPressure);
        double hybridPressure = baseDrain * maxKiPressure * 0.045 * controlRelief * (1.0 - masteryRatio * 0.42);
        return (baseDrain + hybridPressure) * activityMultiplier * kiStressMultiplier(transformation);
    }

    private static double transformationStaminaDrain(net.kicraft.kicraft.core.player.PlayerData data, KiTransformation transformation,
                                                     double mastery, double activityMultiplier) {
        double baseDrain = transformation.staminaDrainPerTick(mastery, data.stat(KiStat.KI_CONTROL));
        if (baseDrain <= 0.0) {
            return 0.0;
        }

        return baseDrain * ResourceCosts.staminaCostMultiplier(data)
                * activityMultiplier * staminaStressMultiplier(transformation);
    }

    private static double kiStressMultiplier(KiTransformation transformation) {
        return switch (transformation) {
            case OOZARU -> 0.45;
            case GOLDEN_OOZARU -> 0.80;
            case SSJ1 -> 0.92;
            case SSJ2 -> 1.00;
            case SSJ3 -> 1.12;
            case SSJ4 -> 0.86;
            case SSG -> 1.18;
            case SSB -> 1.42;
            case IKARI -> 1.18;
            case LSSJ1 -> 1.55;
            case ULTIMATE_FORM -> 0.78;
            case BEAST -> 1.22;
            case POTENTIAL_UNLEASHED -> 0.62;
            case MAX_POWER -> 0.50;
            case MARTIAL_ARTS_MODE -> 0.38;
            case GIANT_NAMEKIAN -> 0.58;
            case SUPER_NAMEKIAN -> 0.78;
            case NAMEKIAN_FUSION -> 0.86;
            case FROST_SECOND_FORM -> 0.72;
            case FROST_THIRD_FORM -> 0.78;
            case FROST_FINAL_FORM -> 0.82;
            case FROST_100_PERCENT -> 0.94;
            case GOLDEN_FROST -> 1.15;
            case BLACK_FROST -> 1.32;
            case ANDROID_OVERCLOCK, REACTOR_RELEASE, ABSORPTION_MODE -> 0.0;
            case BIO_IMPERFECT -> 0.88;
            case BIO_SEMI_PERFECT -> 0.92;
            case BIO_PERFECT -> 0.98;
            case BIO_SUPER_PERFECT -> 1.10;
            case EVIL_MAJIN -> 0.72;
            case KID_MAJIN -> 0.88;
            case PURE_MAJIN -> 1.00;
            case ANCIENT_MAJIN -> 1.12;
            default -> 1.0;
        };
    }

    private static double staminaStressMultiplier(KiTransformation transformation) {
        return switch (transformation) {
            case OOZARU -> 1.40;
            case GOLDEN_OOZARU -> 1.60;
            case SSJ1 -> 0.88;
            case SSJ2 -> 1.00;
            case SSJ3 -> 1.90;
            case SSJ4 -> 0.70;
            case SSG -> 0.55;
            case SSB -> 0.62;
            case IKARI -> 1.35;
            case LSSJ1 -> 1.75;
            case ULTIMATE_FORM -> 0.62;
            case BEAST -> 1.05;
            case POTENTIAL_UNLEASHED -> 0.52;
            case MAX_POWER -> 1.45;
            case MARTIAL_ARTS_MODE -> 1.20;
            case GIANT_NAMEKIAN -> 1.55;
            case SUPER_NAMEKIAN -> 0.72;
            case NAMEKIAN_FUSION -> 0.78;
            case FROST_SECOND_FORM -> 0.75;
            case FROST_THIRD_FORM -> 0.82;
            case FROST_FINAL_FORM -> 0.75;
            case FROST_100_PERCENT -> 1.40;
            case GOLDEN_FROST -> 0.80;
            case BLACK_FROST -> 0.86;
            case ANDROID_OVERCLOCK, REACTOR_RELEASE, ABSORPTION_MODE -> 0.0;
            case BIO_IMPERFECT -> 0.85;
            case BIO_SEMI_PERFECT -> 0.90;
            case BIO_PERFECT -> 0.95;
            case BIO_SUPER_PERFECT -> 1.05;
            case EVIL_MAJIN -> 0.65;
            case KID_MAJIN -> 0.78;
            case PURE_MAJIN -> 0.86;
            case ANCIENT_MAJIN -> 0.94;
            default -> 1.0;
        };
    }

    private static void syncVanillaAttributes(ServerPlayer player, net.kicraft.kicraft.core.player.PlayerData data) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            double clamped = Math.min(1024.0, Math.max(1.0, data.maxHp()));
            double oldMax = Math.max(1.0, maxHealth.getBaseValue());
            double dataRatio = data.maxHp() <= 0.0 ? player.getHealth() / oldMax : data.hp() / data.maxHp();
            if (player.isAlive() && player.getHealth() > 0.0F && dataRatio <= 0.0) {
                dataRatio = player.getHealth() / oldMax;
                data.setHp(data.maxHp() * dataRatio);
            }
            dataRatio = Math.max(0.0, Math.min(1.0, dataRatio));
            boolean changedMax = Math.abs(maxHealth.getBaseValue() - clamped) > 0.01;
            if (changedMax) {
                maxHealth.setBaseValue(clamped);
                player.setHealth((float) Math.max(0.0, Math.min(clamped, clamped * dataRatio)));
            } else if (player.getHealth() > clamped) {
                player.setHealth((float) clamped);
            }
        }
    }

    private static void syncTransformationSpeed(ServerPlayer player, net.kicraft.kicraft.core.player.PlayerData data) {
        AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed == null) {
            return;
        }

        KiTransformation transformation = data.activeTransformation();
        if (transformation.isBase()) {
            removeModifier(movementSpeed, TRANSFORMATION_SPEED_MODIFIER_ID);
            return;
        }

        double bonus = Math.max(-0.45, Math.min(0.75, data.activeMovementMultiplier() - 1.0));
        if (Math.abs(bonus) < 0.001) {
            removeModifier(movementSpeed, TRANSFORMATION_SPEED_MODIFIER_ID);
            return;
        }
        AttributeModifier oldModifier = movementSpeed.getModifier(TRANSFORMATION_SPEED_MODIFIER_ID);
        if (oldModifier != null && Math.abs(oldModifier.getAmount() - bonus) < 0.0001) {
            return;
        }
        removeModifier(movementSpeed, TRANSFORMATION_SPEED_MODIFIER_ID);
        movementSpeed.addTransientModifier(new AttributeModifier(
                TRANSFORMATION_SPEED_MODIFIER_ID,
                "Ki Craft transformation movement",
                bonus,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        ));
    }

    private static void syncExhaustedAttackSpeed(ServerPlayer player, net.kicraft.kicraft.core.player.PlayerData data) {
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeed == null) {
            return;
        }

        boolean exhausted = net.kicraft.kicraft.core.combat.CombatRules.CUSTOM_MELEE_ENABLED
                && StaminaSystem.isAttackExhausted(data);
        AttributeModifier current = attackSpeed.getModifier(EXHAUSTED_ATTACK_SPEED_MODIFIER_ID);
        if (!exhausted) {
            if (current != null) {
                attackSpeed.removeModifier(current);
            }
            return;
        }

        if (current == null) {
            attackSpeed.addTransientModifier(new AttributeModifier(
                    EXHAUSTED_ATTACK_SPEED_MODIFIER_ID,
                    "Ki Craft exhausted attack speed",
                    -0.75,
                    AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void removeModifier(AttributeInstance attribute, UUID id) {
        AttributeModifier modifier = attribute.getModifier(id);
        if (modifier != null) {
            attribute.removeModifier(modifier);
        }
    }

    private static void syncKiHealthFromVanilla(ServerPlayer player, net.kicraft.kicraft.core.player.PlayerData data) {
        if (player.getMaxHealth() <= 0.0F) {
            return;
        }
        double vanillaRatio = Math.max(0.0, Math.min(1.0, player.getHealth() / player.getMaxHealth()));
        double expectedKiHp = data.maxHp() * vanillaRatio;
        if (Math.abs(data.hp() - expectedKiHp) > 0.01) {
            data.setHp(expectedKiHp);
        }
    }
}
