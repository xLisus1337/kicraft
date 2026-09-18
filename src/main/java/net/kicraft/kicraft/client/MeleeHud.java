package net.kicraft.kicraft.client;

import net.kicraft.kicraft.Kicraft;
import net.kicraft.kicraft.core.combat.CombatFeedback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Locale;

@Mod.EventBusSubscriber(modid = Kicraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MeleeHud {
    private MeleeHud() { }
    @SubscribeEvent
    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerBelow(VanillaGuiOverlay.DEBUG_TEXT.id(), "melee_combat", (gui, graphics, partialTick, width, height) -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || !minecraft.player.isAlive() || minecraft.player.isSpectator()
                    || minecraft.options.hideGui || minecraft.screen != null) return;
            int x = width / 2;
            int y = height / 2 + 14;
            int color = MeleeInput.exposed() ? 0xFFFF6559 : 0xFFFFCC52;
            Component text = null;
            if (MeleeInput.exposed()) {
                graphics.fill(x - 24, y, x + 24, y + 4, 0xC0000000);
                graphics.fill(x - 23, y + 1, x - 23 + Math.min(46, MeleeInput.recoveryTicks() * 46 / 20), y + 3, color);
                text = Component.translatable("combat.kicraft.exposed");
            } else if (MeleeInput.counterReady()) {
                text = Component.translatable("combat.kicraft.counter_ready");
            }
            CombatFeedback feedback = MeleeInput.feedback();
            if (feedback != CombatFeedback.NONE) {
                text = Component.translatable("combat.kicraft." + feedback.name().toLowerCase(Locale.ROOT));
            }
            if (text != null) TechniqueHotbarRenderer.drawFitted(graphics, minecraft.font, text, x, y + 8, width - 32, color);
        });
    }
}
