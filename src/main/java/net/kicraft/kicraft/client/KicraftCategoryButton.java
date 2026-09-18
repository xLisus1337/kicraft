package net.kicraft.kicraft.client;

import net.kicraft.kicraft.Kicraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Large category card for the character area of the pause screen. */
public final class KicraftCategoryButton extends Button {
    public enum Category {
        STATS,
        FORMS,
        TECHNIQUES,
        KI_ATTACKS,
        STORY
    }

    private static final int OUTLINE = 0xFF5F3012;
    private static final int OUTLINE_HOVER = 0xFFFFBD43;
    private static final int SURFACE = 0xFF170B05;
    private static final int SURFACE_HOVER = 0xFF3B1F0B;
    private static final int TEXT = 0xFFFFE8BA;
    private static final int ICON = 0xFFFFC85E;
    private static final ResourceLocation STATS_ICON = new ResourceLocation(Kicraft.MODID, "textures/gui/category/stats.png");
    private static final ResourceLocation FORMS_ICON = new ResourceLocation(Kicraft.MODID, "textures/gui/category/forms.png");
    private static final int FORMS_ICON_SOURCE_SIZE = 64;
    private static final int FORMS_ICON_DRAW_SIZE = 32;

    private final Category category;

    public KicraftCategoryButton(int x, int y, int width, int height, Category category, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.category = category;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean highlighted = isHoveredOrFocused();
        int outline = highlighted ? OUTLINE_HOVER : OUTLINE;
        int surface = highlighted ? SURFACE_HOVER : SURFACE;
        drawCutPanel(graphics, getX() + 2, getY() + 3, width, height, 8, 0x66000000);
        drawCutPanel(graphics, getX(), getY(), width, height, 7, outline);
        drawCutPanel(graphics, getX() + 2, getY() + 2, width - 4, height - 4, 6, surface);
        graphics.fill(getX() + 9, getY() + 8, getX() + 11, getY() + height - 8, highlighted ? 0xFFFFDF89 : 0xFFC67525);
        graphics.fill(getX() + 17, getY() + 7, getX() + width - 14, getY() + 8, highlighted ? 0x66FFE19A : 0x335B2B0E);

        int iconX = getX() + 31;
        int iconY = getY() + height / 2;
        if (category == Category.STATS || category == Category.FORMS) {
            drawTextureFrame(graphics, iconX, iconY, highlighted);
            drawCategoryTexture(graphics, iconX, iconY, category == Category.STATS ? STATS_ICON : FORMS_ICON);
        } else {
            drawIconFrame(graphics, iconX, iconY, highlighted);
            drawCategoryIcon(graphics, iconX, iconY, highlighted ? 0xFFFFE7AF : ICON);
        }
        graphics.drawString(Minecraft.getInstance().font, getMessage(), getX() + 56, getY() + height / 2 - 4, TEXT, true);
    }

    private static void drawIconFrame(GuiGraphics graphics, int x, int y, boolean highlighted) {
        int frame = highlighted ? 0xFF91601F : 0xFF4D2810;
        graphics.fill(x - 12, y - 12, x + 13, y + 13, 0xFF090503);
        graphics.fill(x - 10, y - 10, x + 11, y + 11, frame);
        graphics.fill(x - 8, y - 8, x + 9, y + 9, 0xFF1A0C05);
    }

    private static void drawTextureFrame(GuiGraphics graphics, int x, int y, boolean highlighted) {
        int frame = highlighted ? 0xFFFFC65D : 0xFF704018;
        graphics.fill(x - 17, y - 17, x + 18, y + 18, 0xFF080402);
        graphics.fill(x - 16, y - 16, x + 17, y + 17, frame);
        graphics.fill(x - 14, y - 14, x + 15, y + 15, 0xFF130803);
    }

    private static void drawCategoryTexture(GuiGraphics graphics, int centerX, int centerY, ResourceLocation texture) {
        float scale = FORMS_ICON_DRAW_SIZE / (float) FORMS_ICON_SOURCE_SIZE;
        graphics.pose().pushPose();
        graphics.pose().translate(centerX - FORMS_ICON_DRAW_SIZE / 2.0F, centerY - FORMS_ICON_DRAW_SIZE / 2.0F, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.blit(texture, 0, 0, 0, 0, FORMS_ICON_SOURCE_SIZE, FORMS_ICON_SOURCE_SIZE,
                FORMS_ICON_SOURCE_SIZE, FORMS_ICON_SOURCE_SIZE);
        graphics.pose().popPose();
    }

    private void drawCategoryIcon(GuiGraphics graphics, int x, int y, int color) {
        switch (category) {
            case FORMS -> drawSuperSaiyanHair(graphics, x, y, color);
            case TECHNIQUES -> drawKamehamehaHands(graphics, x, y, color);
            case KI_ATTACKS -> drawKiProjectile(graphics, x, y, color);
            case STORY -> drawDragonBall(graphics, x, y);
        }
    }

    private static void drawSuperSaiyanHair(GuiGraphics graphics, int x, int y, int color) {
        int glow = 0xFFFFE6A0;
        graphics.fill(x - 7, y - 4, x - 3, y + 5, color);
        graphics.fill(x - 5, y - 9, x - 1, y + 3, color);
        graphics.fill(x - 1, y - 11, x + 3, y + 3, glow);
        graphics.fill(x + 3, y - 8, x + 7, y + 4, color);
        graphics.fill(x + 6, y - 3, x + 9, y + 5, color);
        graphics.fill(x - 5, y + 3, x + 7, y + 7, color);
        graphics.fill(x - 2, y + 4, x + 4, y + 8, 0xFF5A2D0A);
    }

    private static void drawKamehamehaHands(GuiGraphics graphics, int x, int y, int color) {
        int ki = 0xFF9CF4FF;
        int kiCore = 0xFFFFFFFF;
        graphics.fill(x - 9, y - 3, x - 4, y + 4, color);
        graphics.fill(x - 6, y - 6, x - 3, y - 1, color);
        graphics.fill(x - 5, y + 3, x - 1, y + 6, color);
        graphics.fill(x + 5, y - 3, x + 10, y + 4, color);
        graphics.fill(x + 4, y - 6, x + 7, y - 1, color);
        graphics.fill(x + 1, y + 3, x + 5, y + 6, color);
        graphics.fill(x - 3, y - 3, x + 4, y + 4, ki);
        graphics.fill(x - 1, y - 1, x + 2, y + 2, kiCore);
    }

    private static void drawKiProjectile(GuiGraphics graphics, int x, int y, int color) {
        int energy = 0xFF9AF5FF;
        graphics.fill(x - 10, y - 2, x - 4, y + 3, color);
        graphics.fill(x - 7, y - 5, x - 3, y + 6, color);
        graphics.fill(x - 4, y - 6, x + 6, y + 7, energy);
        graphics.fill(x + 5, y - 3, x + 9, y + 4, color);
        graphics.fill(x - 1, y - 3, x + 4, y + 4, 0xFFFFFFFF);
        graphics.fill(x - 13, y, x - 10, y + 1, 0xFFFFE6A0);
    }

    private static void drawDragonBall(GuiGraphics graphics, int x, int y) {
        int outer = 0xFFB94213;
        int orange = 0xFFFF8B25;
        int glow = 0xFFFFC95A;
        graphics.fill(x - 5, y - 8, x + 6, y + 9, outer);
        graphics.fill(x - 8, y - 5, x + 9, y + 6, outer);
        graphics.fill(x - 6, y - 6, x + 7, y + 7, orange);
        graphics.fill(x - 3, y - 6, x + 4, y - 4, glow);
        graphics.fill(x - 1, y - 3, x + 2, y + 3, 0xFF8E1B0C);
        graphics.fill(x - 3, y - 1, x + 4, y + 1, 0xFF8E1B0C);
    }

    private static void drawCutPanel(GuiGraphics graphics, int x, int y, int width, int height, int cut, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        graphics.fill(x + cut, y, x + width - cut, y + height, color);
        graphics.fill(x, y + cut, x + width, y + height - cut, color);
        for (int step = 1; step < cut; step++) {
            graphics.fill(x + cut - step, y + step, x + width - cut + step, y + step + 1, color);
            graphics.fill(x + cut - step, y + height - step - 1, x + width - cut + step, y + height - step, color);
        }
    }
}
