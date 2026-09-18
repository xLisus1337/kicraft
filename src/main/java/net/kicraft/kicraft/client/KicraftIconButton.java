package net.kicraft.kicraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Compact icon control used by the vanilla-actions dock in the pause screen. */
public final class KicraftIconButton extends Button {
    public enum Icon {
        ADVANCEMENTS,
        STATISTICS,
        OPTIONS,
        MODS,
        FEEDBACK,
        BUG_REPORT,
        SHARE_LAN,
        PLAYER_REPORTING
    }

    private static final int OUTLINE = 0xFF704018;
    private static final int OUTLINE_HOVER = 0xFFFFC14F;
    private static final int SURFACE = 0xFF160B05;
    private static final int SURFACE_HOVER = 0xFF3B200A;
    private static final int ICON = 0xFFFFD981;
    private static final int ICON_HOVER = 0xFFFFF0C8;

    private final Icon icon;

    public KicraftIconButton(int x, int y, int size, Icon icon, Component message, OnPress onPress) {
        super(x, y, size, size, message, onPress, DEFAULT_NARRATION);
        this.icon = icon;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean highlighted = isHoveredOrFocused();
        int outline = highlighted ? OUTLINE_HOVER : OUTLINE;
        int surface = highlighted ? SURFACE_HOVER : SURFACE;
        drawOctagon(graphics, getX() + 1, getY() + 2, width, height, 0x77000000);
        drawOctagon(graphics, getX(), getY(), width, height, outline);
        drawOctagon(graphics, getX() + 2, getY() + 2, width - 4, height - 4, surface);
        if (highlighted) {
            graphics.fill(getX() + 5, getY() + 4, getX() + width - 5, getY() + 5, 0x66FFE29A);
        }
        drawIcon(graphics, getX() + width / 2, getY() + height / 2, highlighted ? ICON_HOVER : ICON);
    }

    private void drawIcon(GuiGraphics graphics, int centerX, int centerY, int color) {
        switch (icon) {
            case ADVANCEMENTS -> drawAdvancement(graphics, centerX, centerY, color);
            case STATISTICS -> drawStatistics(graphics, centerX, centerY, color);
            case OPTIONS -> drawGear(graphics, centerX, centerY, color);
            case MODS -> drawCube(graphics, centerX, centerY, color);
            case FEEDBACK -> drawSpeechBubble(graphics, centerX, centerY, color);
            case BUG_REPORT -> drawBug(graphics, centerX, centerY, color);
            case SHARE_LAN -> drawPeople(graphics, centerX, centerY, color);
            case PLAYER_REPORTING -> drawShield(graphics, centerX, centerY, color);
        }
    }

    private static void drawAdvancement(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x - 2, y - 7, x + 3, y + 6, color);
        graphics.fill(x - 6, y - 3, x + 7, y + 2, color);
        graphics.fill(x - 4, y - 5, x + 5, y + 4, color);
        graphics.fill(x - 1, y - 4, x + 2, y + 3, 0xFF5F3412);
    }

    private static void drawStatistics(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x - 7, y + 3, x - 3, y + 7, color);
        graphics.fill(x - 1, y - 1, x + 3, y + 7, color);
        graphics.fill(x + 5, y - 5, x + 9, y + 7, color);
        graphics.fill(x - 8, y + 8, x + 10, y + 9, color);
    }

    private static void drawGear(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x - 3, y - 8, x + 4, y + 9, color);
        graphics.fill(x - 8, y - 3, x + 9, y + 4, color);
        graphics.fill(x - 6, y - 6, x + 7, y + 7, color);
        graphics.fill(x - 2, y - 2, x + 3, y + 3, 0xFF4A2910);
    }

    private static void drawCube(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x - 6, y - 4, x, y - 1, color);
        graphics.fill(x + 1, y - 4, x + 7, y - 1, lighten(color));
        graphics.fill(x - 6, y, x, y + 6, darken(color));
        graphics.fill(x + 1, y, x + 7, y + 6, color);
        graphics.fill(x - 6, y - 5, x + 7, y - 4, 0xFFFFF0C8);
    }

    private static void drawSpeechBubble(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x - 8, y - 5, x + 9, y + 5, color);
        graphics.fill(x - 5, y + 5, x - 1, y + 8, color);
        graphics.fill(x - 5, y - 2, x + 6, y + 2, 0xFF41200C);
        graphics.fill(x - 3, y - 2, x - 1, y, color);
        graphics.fill(x + 1, y - 2, x + 3, y, color);
    }

    private static void drawBug(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x - 3, y - 5, x + 4, y + 6, color);
        graphics.fill(x - 6, y - 3, x - 3, y - 1, color);
        graphics.fill(x + 4, y - 3, x + 7, y - 1, color);
        graphics.fill(x - 7, y + 2, x - 3, y + 4, color);
        graphics.fill(x + 4, y + 2, x + 8, y + 4, color);
        graphics.fill(x - 1, y - 7, x + 2, y - 5, color);
        graphics.fill(x - 1, y - 2, x + 2, y + 4, 0xFF4A2910);
    }

    private static void drawPeople(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x - 7, y - 6, x - 2, y - 2, color);
        graphics.fill(x + 2, y - 5, x + 7, y - 1, lighten(color));
        graphics.fill(x - 9, y - 1, x, y + 7, color);
        graphics.fill(x, y, x + 9, y + 7, lighten(color));
        graphics.fill(x - 11, y + 6, x + 2, y + 8, color);
        graphics.fill(x - 1, y + 6, x + 11, y + 8, lighten(color));
    }

    private static void drawShield(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x - 6, y - 6, x + 7, y + 2, color);
        graphics.fill(x - 4, y + 2, x + 5, y + 5, color);
        graphics.fill(x - 2, y + 5, x + 3, y + 7, color);
        graphics.fill(x - 1, y - 4, x + 2, y + 4, 0xFF43210D);
        graphics.fill(x - 3, y - 1, x + 4, y + 2, 0xFF43210D);
    }

    private static void drawOctagon(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int cut = Math.min(5, Math.max(2, Math.min(width, height) / 4));
        graphics.fill(x + cut, y, x + width - cut, y + height, color);
        graphics.fill(x, y + cut, x + width, y + height - cut, color);
        for (int step = 1; step < cut; step++) {
            graphics.fill(x + cut - step, y + step, x + width - cut + step, y + step + 1, color);
            graphics.fill(x + cut - step, y + height - step - 1, x + width - cut + step, y + height - step, color);
        }
    }

    private static int lighten(int color) {
        return 0xFF000000
                | (Math.min(255, ((color >> 16) & 0xFF) + 42) << 16)
                | (Math.min(255, ((color >> 8) & 0xFF) + 42) << 8)
                | Math.min(255, (color & 0xFF) + 42);
    }

    private static int darken(int color) {
        return 0xFF000000
                | (Math.max(0, ((color >> 16) & 0xFF) - 54) << 16)
                | (Math.max(0, ((color >> 8) & 0xFF) - 54) << 8)
                | Math.max(0, (color & 0xFF) - 54);
    }
}
