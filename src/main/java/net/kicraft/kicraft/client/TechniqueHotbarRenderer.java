package net.kicraft.kicraft.client;

import net.kicraft.kicraft.Kicraft;
import net.kicraft.kicraft.core.player.KiTechnique;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Kicraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TechniqueHotbarRenderer {
    private static final ResourceLocation VANILLA_WIDGETS =
            new ResourceLocation("minecraft", "textures/gui/widgets.png");

    private TechniqueHotbarRenderer() {
    }

    @SubscribeEvent
    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerBelow(VanillaGuiOverlay.DEBUG_TEXT.id(), "technique_hotbar", (gui, graphics, partialTick, width, height) -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (!TechniqueHotbar.visible() || minecraft.options.hideGui) {
                return;
            }
            int barWidth = TechniqueHotbar.SLOT_COUNT * 20 + 2;
            int x = Math.max(2, width - barWidth - 6);
            int y = height - 28;
            // Reuse the vanilla hotbar's left section and its one-pixel right cap.
            graphics.blit(VANILLA_WIDGETS, x, y, 0, 0, barWidth - 1, 22);
            graphics.blit(VANILLA_WIDGETS, x + barWidth - 1, y, 181, 0, 1, 22);
            graphics.blit(VANILLA_WIDGETS, x - 1 + TechniqueHotbar.selectedSlot() * 20,
                    y - 1, 0, 22, 24, 22);
            for (int slot = 0; slot < TechniqueHotbar.SLOT_COUNT; slot++) {
                KiTechnique technique = ClientSettings.techniqueSlot(slot);
                boolean available = technique != null && ClientKiData.data().hasTechnique(technique);
                renderIcon(graphics, x + 4 + slot * 20, y + 5, technique, available);
            }
            int nameAlpha = TechniqueHotbar.selectionNameAlpha(partialTick);
            if (nameAlpha > 3) {
                KiTechnique selected = ClientSettings.techniqueSlot(TechniqueHotbar.selectedSlot());
                Component label = Component.translatable("hud.kicraft.hotbar.selected", slotName(selected));
                drawFitted(graphics, minecraft.font, label, x + barWidth / 2, y - 12, barWidth + 40,
                        (nameAlpha << 24) | 0x00FFE3A1);
            }
        });
    }

    static Component slotName(KiTechnique technique) {
        if (technique == null) {
            return Component.translatable("screen.kicraft.hotbar.empty");
        }
        Component name = Component.translatable(technique.translationKey());
        return ClientKiData.data().hasTechnique(technique) ? name
                : Component.translatable("screen.kicraft.hotbar.locked_name", name);
    }

    static void renderSlot(GuiGraphics graphics, Font font, int x, int y, int width, int height,
                           int slot, KiTechnique technique, boolean selected, boolean hovered) {
        boolean available = technique != null && ClientKiData.data().hasTechnique(technique);
        int edge = selected ? 0xFFFFC451 : hovered ? 0xFFD38A36 : 0xFF6D3C1C;
        graphics.fill(x, y, x + width, y + height, edge);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, selected ? 0xFF42270F : 0xFF180E08);
        if (selected) {
            graphics.fill(x + 2, y + height - 2, x + width - 2, y + height - 1, 0xFFFFE3A1);
        }
        renderIcon(graphics, x + width / 2 - 6, y + height / 2 - 5, technique, available);
        graphics.pose().pushPose();
        graphics.pose().translate(x + 2, y + 2, 0);
        graphics.pose().scale(0.65F, 0.65F, 1.0F);
        graphics.drawString(font, Integer.toString(slot + 1), 0, 0, selected ? 0xFFFFFFFF : 0xFFCFB89A, true);
        graphics.pose().popPose();
        if (technique != null && !available) {
            graphics.fill(x + width - 7, y + height - 7, x + width - 3, y + height - 3, 0xFFB56859);
            graphics.fill(x + width - 6, y + height - 9, x + width - 4, y + height - 6, 0xFFB56859);
        }
    }

    /** Small pixel icons keep the bar readable at vanilla hotbar size. */
    private static void renderIcon(GuiGraphics graphics, int x, int y, KiTechnique technique, boolean available) {
        if (technique == null) {
            graphics.fill(x + 4, y + 6, x + 8, y + 7, 0xFF60442C);
            return;
        }
        int color = available ? switch (technique) {
            case KI_BLAST -> 0xFF62D9FF;
            case FLIGHT -> 0xFFB2F7DA;
            case KI_SENSE -> 0xFFFFD25A;
            case KI_TRANSFER -> 0xFFF19CEC;
        } : 0xFF685C50;
        switch (technique) {
            case KI_BLAST -> {
                graphics.fill(x + 4, y + 2, x + 10, y + 10, color);
                graphics.fill(x + 2, y + 4, x + 12, y + 8, color);
                graphics.fill(x, y + 9, x + 4, y + 11, color);
                graphics.fill(x + 5, y + 4, x + 8, y + 7, available ? 0xFFE9FBFF : color);
            }
            case FLIGHT -> {
                for (int step = 0; step < 5; step++) {
                    graphics.fill(x + step, y + 3 + step, x + step + 2, y + 6 + step, color);
                    graphics.fill(x + 10 - step, y + 3 + step, x + 12 - step, y + 6 + step, color);
                }
                graphics.fill(x + 5, y + 1, x + 7, y + 6, color);
            }
            case KI_SENSE -> {
                graphics.renderOutline(x + 1, y + 1, 11, 11, color);
                graphics.renderOutline(x + 4, y + 4, 5, 5, color);
                graphics.fill(x + 6, y, x + 7, y + 13, color);
            }
            case KI_TRANSFER -> {
                graphics.fill(x, y + 3, x + 10, y + 5, color);
                graphics.fill(x + 2, y + 8, x + 12, y + 10, color);
                for (int step = 0; step < 3; step++) {
                    graphics.fill(x + 8 + step, y + 1 + step, x + 9 + step, y + 7 - step, color);
                    graphics.fill(x + 3 - step, y + 6 + step, x + 4 - step, y + 12 - step, color);
                }
            }
        }
    }

    static void drawFitted(GuiGraphics graphics, Font font, Component text, int centerX, int y, int maxWidth, int color) {
        float scale = Math.min(1.0F, maxWidth / (float) Math.max(1, font.width(text)));
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, y, 0);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawCenteredString(font, text, 0, 0, color);
        graphics.pose().popPose();
    }
}
