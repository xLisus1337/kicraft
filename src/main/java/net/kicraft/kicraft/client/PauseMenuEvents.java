package net.kicraft.kicraft.client;

import net.kicraft.kicraft.Kicraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Kicraft.MODID, value = Dist.CLIENT)
public final class PauseMenuEvents {
    private PauseMenuEvents() {
    }

    @SubscribeEvent
    public static void replaceVanillaPauseMenu(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof PauseScreen) {
            event.setNewScreen(new KicraftPauseScreen());
        }
    }
}
