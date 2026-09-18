package net.kicraft.kicraft.server;

import net.kicraft.kicraft.Kicraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Kicraft.MODID)
public final class DimensionRestrictionEvents {
    private static final String LAST_BLOCK_MESSAGE_TICK = "kicraftLastBlockedDimensionMessage";
    private static final int MESSAGE_COOLDOWN_TICKS = 60;

    private DimensionRestrictionEvents() {
    }

    @SubscribeEvent
    public static void blockVanillaDimensionTravel(EntityTravelToDimensionEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof ServerPlayer player) || !isDisabledDimension(event.getDimension())) {
            return;
        }

        event.setCanceled(true);
        long gameTime = player.level().getGameTime();
        long lastMessage = player.getPersistentData().getLong(LAST_BLOCK_MESSAGE_TICK);
        if (gameTime - lastMessage >= MESSAGE_COOLDOWN_TICKS) {
            player.getPersistentData().putLong(LAST_BLOCK_MESSAGE_TICK, gameTime);
            player.displayClientMessage(Component.literal("Nether and The End are disabled in Ki Craft."), true);
        }
    }

    @SubscribeEvent
    public static void recoverPlayerFromDisabledDimension(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isDisabledDimension(player.level().dimension())) {
            moveToOverworldSpawn(player);
        }
    }

    private static boolean isDisabledDimension(ResourceKey<Level> dimension) {
        return Level.NETHER.equals(dimension) || Level.END.equals(dimension);
    }

    private static void moveToOverworldSpawn(ServerPlayer player) {
        ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return;
        }

        BlockPos spawn = overworld.getSharedSpawnPos();
        player.teleportTo(overworld, spawn.getX() + 0.5D, spawn.getY() + 1.0D, spawn.getZ() + 0.5D, player.getYRot(), player.getXRot());
        player.displayClientMessage(Component.literal("Returned to the Overworld. Nether and The End are disabled."), false);
    }
}
