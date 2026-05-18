package net.kicraft.kicraft.event;

import net.kicraft.kicraft.Kicraft;
import net.kicraft.kicraft.client.HudOverlay;
import net.kicraft.kicraft.client.KeyInputHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class ClientEvents {

    @Mod.EventBusSubscriber(modid = Kicraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            event.register(KeyInputHandler.CHARGE_KI_KEY);
            event.register(KeyInputHandler.INSTANT_TRANSMISSION_KEY);
            event.register(KeyInputHandler.KI_SENSE_SHORT_KEY);
            event.register(KeyInputHandler.STAT_MENU_KEY);
            event.register(KeyInputHandler.TURBO_MODE_KEY);
        }

        @SubscribeEvent
        public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("hud_kicraft", HudOverlay.HUD_KICRAFT);
        }
    }

    @Mod.EventBusSubscriber(modid = Kicraft.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeBusEvents {
        @SubscribeEvent
        public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event) {
            if (event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) event.setCanceled(true);
            if (event.getOverlay().id().equals(VanillaGuiOverlay.FOOD_LEVEL.id())) event.setCanceled(true);
            if (event.getOverlay().id().equals(VanillaGuiOverlay.ARMOR_LEVEL.id())) event.setCanceled(true);

            if (event.getOverlay().id().equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())) {
                event.setCanceled(true);
            }
        }
    }
}