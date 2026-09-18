package net.kicraft.kicraft.client;

import net.kicraft.kicraft.Kicraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class KicraftTitleBackground {
    private static final ResourceLocation TEXTURE = new ResourceLocation(Kicraft.MODID,
            "textures/gui/main_menu_background.png");
    private static final int TEXTURE_WIDTH = 1672;
    private static final int TEXTURE_HEIGHT = 941;

    private KicraftTitleBackground() {
    }

    public static void render(GuiGraphics graphics, int screenWidth, int screenHeight) {
        float scale = Math.max(screenWidth / (float) TEXTURE_WIDTH, screenHeight / (float) TEXTURE_HEIGHT);
        float drawWidth = TEXTURE_WIDTH * scale;
        float drawHeight = TEXTURE_HEIGHT * scale;

        graphics.pose().pushPose();
        graphics.pose().translate((screenWidth - drawWidth) * 0.5F, (screenHeight - drawHeight) * 0.5F, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.blit(TEXTURE, 0, 0, 0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        graphics.pose().popPose();
    }
}
