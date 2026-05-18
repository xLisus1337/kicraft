package net.kicraft.kicraft.event;

import net.kicraft.kicraft.Kicraft;
import net.kicraft.kicraft.capability.PlayerData;
import net.kicraft.kicraft.capability.PlayerDataProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Kicraft.MODID)
public class CapabilityEvents {

    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(PlayerDataProvider.PLAYER_DATA).isPresent()) {
                event.addCapability(new ResourceLocation(Kicraft.MODID, "properties"), new PlayerDataProvider());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            event.getOriginal().getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(oldStore -> {
                event.getEntity().getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(newStore -> {
                    newStore.copyFrom(oldStore);
                });
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GameplayEvents.sync(player);
        }
    }
}