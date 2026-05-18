package net.kicraft.kicraft.event;

import net.kicraft.kicraft.Kicraft;
import net.kicraft.kicraft.capability.PlayerDataProvider;
import net.kicraft.kicraft.network.PacketHandler;
import net.kicraft.kicraft.network.PlayerDataSyncS2CPacket;
import net.kicraft.kicraft.network.OpenRaceMenuS2CPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = Kicraft.MODID)
public class GameplayEvents {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide()) {
            ServerPlayer player = (ServerPlayer) event.player;
            player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {

                // --- 1. ATRYBUTY ---
                AttributeInstance hpAttr = player.getAttribute(Attributes.MAX_HEALTH);
                if (hpAttr != null && Math.abs(hpAttr.getBaseValue() - data.getMaxHP()) > 0.1) {
                    hpAttr.setBaseValue(data.getMaxHP());
                }
                AttributeInstance dmgAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
                if (dmgAttr != null) {
                    double targetDmg = 1.0 + (data.getStrength() * 0.5);
                    if (Math.abs(dmgAttr.getBaseValue() - targetDmg) > 0.1) dmgAttr.setBaseValue(targetDmg);
                }

                // --- 2. DETEKCJA RUCHU ---
                CompoundTag tag = player.getPersistentData();
                boolean isMoving = false;
                if (tag.contains("lastX")) {
                    double dx = Math.abs(player.getX() - tag.getDouble("lastX"));
                    double dz = Math.abs(player.getZ() - tag.getDouble("lastZ"));
                    if (dx > 0.001 || dz > 0.001) isMoving = true;
                }
                tag.putDouble("lastX", player.getX());
                tag.putDouble("lastZ", player.getZ());
                if (player.zza != 0 || player.xxa != 0) isMoving = true;

                boolean isSprinting = player.isSprinting();
                boolean onGround = player.onGround();
                boolean needsSync = false;

                // --- 3. LOGIKA STAMINY ---
                if (data.isTurboMode() || isSprinting || (!onGround && isMoving)) {
                    double penalty = data.getMaxStamina() * 0.00015;
                    double drain = isSprinting ? (0.25 + penalty) : (0.15 + penalty * 0.5);
                    if (data.isTurboMode()) drain = 0.7 + (penalty * 2.0);

                    data.setStamina(data.getStamina() - drain);
                    data.addStaminaXP(0.0005);

                    if (data.getStaminaXP() >= data.getReqStaminaXP()) {
                        data.setStaminaXP(0);
                        data.setMaxStamina(data.getMaxStamina() + 2.0);
                        needsSync = true;
                    }
                    if (player.tickCount % 5 == 0) needsSync = true;
                }
                else if (onGround && !isMoving && !data.isChargingKi()) {
                    if (data.getStamina() < data.getMaxStamina()) {
                        data.setStamina(data.getStamina() + 0.2 + (data.getMaxStamina() * 0.001));
                        if (player.tickCount % 10 == 0) needsSync = true;
                    }
                }

                // --- 4. ŁADOWANIE KI ---
                if (data.isChargingKi()) {
                    if (onGround && !isMoving) {
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 2, 3, false, false, false));

                        // Zawsze ładujemy Ki
                        data.addKi(5.0 + (data.getMaxKi() * 0.003));

                        if (data.isTurboMode()) {
                            // TURBO: Zżera staminę szybko. Daje znośny XP, ale tylko dopóki masz siłę!
                            if (data.getStamina() > 0) {
                                data.setStamina(data.getStamina() - 1.2); // Zwiększony drenaż staminy
                                data.addKiXP(0.4); // Zmniejszone z 1.5 na 0.4
                            } else {
                                // Brak staminy w Turbo = brak XP i kary
                                player.displayClientMessage(net.minecraft.network.chat.Component.literal("§cYou are too exhausted for Turbo!"), true);
                                data.setTurboMode(false);
                            }
                        } else {
                            // ZWYKŁE: Powoli regeneruje staminę, ale daje MIKROSKIPIJNE ilości XP.
                            // Służy do przygotowania się do walki, nie do wbijania poziomów.
                            data.setStamina(data.getStamina() + 0.5);
                            data.addKiXP(0.02); // Zmniejszone z 0.5 na 0.02 (prawie nic!)
                        }

                        needsSync = true;

                        // Awans poziomu Ki
                        if (data.getKiXP() >= data.getReqKiXP()) {
                            data.setKiXP(0);
                            data.setLevel(data.getLevel() + 1);
                            data.setMaxKi(data.getMaxKi() + 10.0);
                        }
                    }
                }

                // --- 5. LATANIE (Z BLOKADĄ TECHNIKI) ---
                if (!player.isCreative() && !player.isSpectator()) {
                    boolean canFly = data.hasFlight() && data.getKi() > 5.0 && data.getStamina() > 5.0;
                    if (data.isChargingKi() && isMoving) canFly = false;

                    if (canFly != player.getAbilities().mayfly) {
                        player.getAbilities().mayfly = canFly;
                        if (!canFly) player.getAbilities().flying = false;
                        player.onUpdateAbilities();
                        needsSync = true;
                    }

                    if (player.getAbilities().flying) {
                        double flyDrain = data.isTurboMode() ? 0.4 : 0.15;
                        data.setKi(data.getKi() - flyDrain);
                        data.setStamina(data.getStamina() - flyDrain);

                        float speed = data.isTurboMode() ? 0.15f : 0.05f;
                        if (data.isChargingKi()) speed = 0.0f;
                        player.getAbilities().setFlyingSpeed(speed);

                        // NOWOŚĆ: Trening poprzez lot!
                        // Prawdziwi wojownicy trenują aurę używając jej.
                        if (isMoving) {
                            data.addKiXP(data.isTurboMode() ? 0.8 : 0.2); // Szybki lot daje dużo XP
                            data.addKiControlXP(0.05); // Latanie uczy też kontroli Ki

                            // Sprawdzenie awansu z latania
                            if (data.getKiXP() >= data.getReqKiXP()) {
                                data.setKiXP(0);
                                data.setLevel(data.getLevel() + 1);
                                data.setMaxKi(data.getMaxKi() + 10.0);
                            }
                            if (data.getKiControlXP() >= data.getReqKiControlXP()) {
                                data.setKiControlXP(0);
                                data.setKiControl(data.getKiControl() + 1);
                            }
                        }
                    }
                }

                // --- 6. SYNC ---
                player.experienceLevel = (int) data.getLevel();
                player.experienceProgress = (float) Math.max(0, Math.min(1, (data.getKi() / data.getMaxKi())));

                // ZOPTYMALIZOWANE WYSYŁANIE PAKIETÓW SIECIOWYCH:
                // Sprawdzamy flagę "isDirty" z PlayerData ORAZ wysyłamy co 5 sekund (100 ticków) dla bezpieczeństwa.
                if (needsSync || data.isDirty() || player.tickCount % 100 == 0) {
                    sync(player);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onFallDamage(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            attacker.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(d -> {
                d.addStrengthXP(event.getAmount() * 0.5);
                if (d.getStrengthXP() >= d.getReqStrengthXP()) {
                    d.setStrengthXP(0);
                    d.setStrength(d.getStrength() + 1);
                }
                sync(attacker);
            });
        }
        if (event.getEntity() instanceof ServerPlayer victim) {
            victim.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(d -> {
                float originalDamage = event.getAmount();
                event.setAmount(Math.max(originalDamage / (1.0f + (float) d.getDurability() * 0.01f), 0.1f));
                d.addDurabilityXP(originalDamage * 0.2);
                if (d.getDurabilityXP() >= d.getReqDurabilityXP()) {
                    d.setDurabilityXP(0);
                    d.setDurability(d.getDurability() + 1);
                }
                d.addMaxHpXP(originalDamage * 0.3);
                if (d.getMaxHpXP() >= d.getReqMaxHpXP()) {
                    d.setMaxHpXP(0);
                    d.setMaxHP(d.getMaxHP() + 2.0);
                }
                sync(victim);
            });
        }
    }

    @SubscribeEvent
    public static void onNaturalHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.getPersistentData().getBoolean("kicraft_healing")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerEat(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getItem().isEdible()) {
            player.getPersistentData().putBoolean("kicraft_healing", true);
            player.heal(event.getItem().getFoodProperties(player).getNutrition() * 4.0f);
            player.getPersistentData().putBoolean("kicraft_healing", false);
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(old -> {
            event.getEntity().getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(n -> n.copyFrom(old));
        });
        event.getOriginal().invalidateCaps();
        if (event.getEntity() instanceof ServerPlayer sp) sync(sp);
    }

    @SubscribeEvent
    public static void onPlayerJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                AttributeInstance hpAttr = player.getAttribute(Attributes.MAX_HEALTH);
                if (hpAttr != null) hpAttr.setBaseValue(data.getMaxHP());
                sync(player);
                if (!data.hasChosenRace()) {
                    PacketHandler.sendToPlayer(new OpenRaceMenuS2CPacket(), player);
                }
            });
        }
    }

    public static void sync(@NotNull ServerPlayer player) {
        player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
            CompoundTag nbt = new CompoundTag();
            data.saveNBTData(nbt);
            PacketHandler.sendToPlayer(new PlayerDataSyncS2CPacket(nbt), player);

            // CZYŚCIMY FLAGĘ PO WYSŁANIU DANYCH!
            data.clearDirty();
        });
    }
}