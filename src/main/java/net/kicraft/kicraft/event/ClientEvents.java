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

import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraft.world.entity.EntityType;
import net.kicraft.kicraft.capability.PlayerDataProvider;
import net.kicraft.kicraft.client.KicraftPlayerRenderer;

public class ClientEvents {
    // Statyczna instancja renderera, aby nie tworzyć go co klatkę
    public static KicraftPlayerRenderer kicraftRenderer = null;

    @Mod.EventBusSubscriber(modid = Kicraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            event.register(KeyInputHandler.CHARGE_KI_KEY);
            event.register(KeyInputHandler.INSTANT_TRANSMISSION_KEY);
            event.register(KeyInputHandler.KI_SENSE_SHORT_KEY);
            event.register(KeyInputHandler.STAT_MENU_KEY);
            event.register(KeyInputHandler.TURBO_MODE_KEY);
            event.register(KeyInputHandler.LOCK_ON_KEY);
            event.register(KeyInputHandler.SHOULDER_CAM_KEY);
        }

        @SubscribeEvent
        public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("hud_kicraft", HudOverlay.HUD_KICRAFT);
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            // Rejestrujemy renderer dla EntityType.PLAYER. 
            // Forge wywoła to i dostarczy prawidłowy Context w parametrze.
            event.registerEntityRenderer(EntityType.PLAYER, context -> {
                kicraftRenderer = new KicraftPlayerRenderer(context);
                return kicraftRenderer;
            });
        }
    }

    @Mod.EventBusSubscriber(modid = Kicraft.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeBusEvents {

        @SubscribeEvent
        public static void onPlayerRender(RenderPlayerEvent.Pre event) {
            event.getEntity().getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                String race = data.getRace();
                if (race.equalsIgnoreCase("Human") || race.equalsIgnoreCase("Half-Saiyan")) {
                    // Anulujemy standardowe renderowanie Minecrafta
                    event.setCanceled(true);

                    // Rysujemy naszym rendererem GeckoLib, jeśli został już zainicjalizowany
                    if (kicraftRenderer != null) {
                        kicraftRenderer.render(event.getEntity(), event.getEntity().getYRot(), event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
                    }
                }
            });
        }

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