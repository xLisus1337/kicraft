package net.kicraft.kicraft.server;

import net.kicraft.kicraft.Kicraft;
import net.kicraft.kicraft.network.FlightStateSyncPacket;
import net.kicraft.kicraft.core.player.PlayerData;
import net.kicraft.kicraft.network.KicraftNetwork;
import net.kicraft.kicraft.network.PlayerDataSyncPacket;
import net.kicraft.kicraft.network.StatLevelUpPacket;
import net.kicraft.kicraft.server.capability.PlayerDataCapability;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Kicraft.MODID)
public final class PlayerDataEvents {
    private static final Map<UUID, net.minecraft.nbt.CompoundTag> DEATH_SNAPSHOTS = new HashMap<>();
    private static final Map<UUID, ServerPlayer> PENDING_SYNCS = new HashMap<>();
    private static final Map<UUID, Long> LAST_SYNC_TICKS = new HashMap<>();
    private static final int RESOURCE_SYNC_INTERVAL_TICKS = 4;
    private static final String RESPAWN_PROTECTION_UNTIL_TAG = "kicraftRespawnProtectionUntil";
    private static final int RESPAWN_PROTECTION_TICKS = 100;

    private PlayerDataEvents() {
    }

    @SubscribeEvent
    public static void attachPlayerData(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            PlayerDataCapability.Provider provider = PlayerDataCapability.provider();
            event.addCapability(new ResourceLocation(Kicraft.MODID, "player_data"), provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(oldData -> {
            event.getEntity().getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(newData -> newData.load(oldData.save()));
        });
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerKiState.clear(player);
            LockOnState.clear(player);
            FlightManeuverSystem.clear(player);
            player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
                validatePlayerData(player, data, "login");
                if (data.isLegendarySaiyan()) {
                    KicraftAdvancements.awardBerserk(player);
                }
                if (data.isGodKiUnlocked()) {
                    KicraftAdvancements.awardGodKi(player);
                }
            });
            sync(player);
            syncFlightState(player);
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerKiState.clear(player);
            LockOnState.clear(player);
            FlightManeuverSystem.clear(player);
            DiscordBugReportService.clear(player);
            DEATH_SNAPSHOTS.remove(player.getUUID());
            PENDING_SYNCS.remove(player.getUUID());
            LAST_SYNC_TICKS.remove(player.getUUID());
            if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }
    }

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerKiState.clear(player);
            LockOnState.clear(player);
            FlightManeuverSystem.cancel(player);
            syncFlightState(player);
            player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
                // Keep a server-side copy through the old-player/new-player respawn handoff.
                DEATH_SNAPSHOTS.put(player.getUUID(), data.save().copy());
                data.setHp(0.0);
                sync(player);
                data.clearDirty();
            });

            if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerKiState.clear(player);
            LockOnState.clear(player);
            FlightManeuverSystem.cancel(player);
            player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
                net.minecraft.nbt.CompoundTag deathSnapshot = DEATH_SNAPSHOTS.remove(player.getUUID());
                if (deathSnapshot != null) {
                    data.load(deathSnapshot);
                }
                validatePlayerData(player, data, "respawn");
                data.setHp(data.maxHp());
                data.setKi(data.maxKi());
                data.setStamina(data.maxStamina());
                StaminaSystem.reset(player);
                restoreVanillaHealth(player, data.maxHp());
                player.getPersistentData().putLong(RESPAWN_PROTECTION_UNTIL_TAG,
                        player.level().getGameTime() + RESPAWN_PROTECTION_TICKS);
                data.clearDirty();
            });
            sync(player);
            syncFlightState(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void protectFreshRespawn(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        long protectionUntil = player.getPersistentData().getLong(RESPAWN_PROTECTION_UNTIL_TAG);
        if (protectionUntil <= 0L) {
            return;
        }
        if (player.level().getGameTime() < protectionUntil) {
            event.setCanceled(true);
        } else {
            player.getPersistentData().remove(RESPAWN_PROTECTION_UNTIL_TAG);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerKiState.clear(player);
            LockOnState.clear(player);
            FlightManeuverSystem.cancel(player);
            player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
                validatePlayerData(player, data, "dimension change");
                sync(player);
            });
            syncFlightState(player);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        DEATH_SNAPSHOTS.clear();
        PENDING_SYNCS.clear();
        LAST_SYNC_TICKS.clear();
        FlightManeuverSystem.clearAll();
    }

    /** Coalesce continuously changing resources; read their latest values only when sending. */
    public static void requestSync(ServerPlayer player) {
        PENDING_SYNCS.put(player.getUUID(), player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void flushResourceSyncs(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING_SYNCS.isEmpty()) return;
        long now = event.getServer().overworld().getGameTime();
        // sync() removes the pending entry, so iterate over a snapshot.
        for (ServerPlayer player : new ArrayList<>(PENDING_SYNCS.values())) {
            UUID id = player.getUUID();
            if (player.isRemoved() || player.server != event.getServer()) {
                PENDING_SYNCS.remove(id);
                LAST_SYNC_TICKS.remove(id);
                continue;
            }
            Long last = LAST_SYNC_TICKS.get(id);
            if (last == null || now < last || now - last >= RESOURCE_SYNC_INTERVAL_TICKS) {
                sync(player);
            }
        }
    }

    /** Immediate sync for actions, login, respawn and other discrete state changes. */
    public static void sync(ServerPlayer player) {
        player.getCapability(PlayerDataCapability.PLAYER_DATA)
                .ifPresent(data -> {
                    data.validateAndRepair();
                    KicraftNetwork.sendToPlayer(player, new PlayerDataSyncPacket(data.save(), CombatState.isInCombat(player)));
                    Map<net.kicraft.kicraft.core.player.KiStat, Double> levelUps = data.consumeLevelUpNotifications();
                    if (!levelUps.isEmpty()) {
                        KicraftNetwork.sendToPlayer(player, new StatLevelUpPacket(levelUps));
                    }
                    PENDING_SYNCS.remove(player.getUUID());
                    LAST_SYNC_TICKS.put(player.getUUID(), player.server.overworld().getGameTime());
                    data.clearDirty();
                });
    }

    public static void syncFlightState(ServerPlayer player) {
        KicraftNetwork.sendToTrackingAndSelf(player,
                new FlightStateSyncPacket(player.getUUID(), ServerKiState.isFlying(player)));
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer viewer) || !(event.getTarget() instanceof ServerPlayer target)) {
            return;
        }

        KicraftNetwork.sendToPlayer(viewer,
                new FlightStateSyncPacket(target.getUUID(), ServerKiState.isFlying(target)));
        FlightManeuverSystem.syncTo(viewer, target);
    }

    private static void validatePlayerData(ServerPlayer player, PlayerData data, String context) {
        if (data.validateAndRepair()) {
            Kicraft.LOGGER.warn("Repaired Ki Craft data for {} during {}.", player.getGameProfile().getName(), context);
        }
    }

    private static void restoreVanillaHealth(ServerPlayer player, double maxHp) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        double clamped = Math.min(1024.0, Math.max(1.0, maxHp));
        if (maxHealth != null) {
            maxHealth.setBaseValue(clamped);
        }
        player.setHealth((float) clamped);
    }

}
