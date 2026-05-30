package net.kicraft.kicraft.event;

import net.kicraft.kicraft.Kicraft;
import net.kicraft.kicraft.client.HudOverlay;
import net.kicraft.kicraft.client.KicraftPlayerRenderer;
import net.kicraft.kicraft.capability.PlayerDataProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Kicraft.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientEvents {

    public static KicraftPlayerRenderer kicraftRenderer;

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("hud_kicraft", HudOverlay.HUD_KICRAFT);
    }

    // W Forge 1.20.1 tylko event AddLayers posiada metodę getContext()
    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        kicraftRenderer = new KicraftPlayerRenderer(event.getContext());
    }

    @Mod.EventBusSubscriber(modid = Kicraft.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeBusEvents {

        @SubscribeEvent
        public static void onPlayerRender(RenderPlayerEvent.Pre event) {
            event.getEntity().getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                String race = data.getRace();
                if (race.equalsIgnoreCase("Human") || race.equalsIgnoreCase("Half-Saiyan")) {
                    event.setCanceled(true);

                    if (kicraftRenderer != null) {
                        kicraftRenderer.render(
                                event.getEntity(),
                                event.getEntity().getYRot(),
                                event.getPartialTick(),
                                event.getPoseStack(),
                                event.getMultiBufferSource(),
                                event.getPackedLight()
                        );
                    }
                }
            });
        }
    }
}