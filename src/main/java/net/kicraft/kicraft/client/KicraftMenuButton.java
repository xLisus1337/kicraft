package net.kicraft.kicraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Warm, cut-corner button shared by the Ki Craft pause menu. */
public final class KicraftMenuButton extends Button {
    private static final int OUTLINE = 0xFF5E2D11;
    private static final int OUTLINE_HOVER = 0xFFFFB13B;
    private static final int SURFACE = 0xFF170D07;
    private static final int SURFACE_HOVER = 0xFF36200D;
    private static final int TEXT = 0xFFFFE8BB;
    private static final int TEXT_DISABLED = 0xFF8D7762;

    public KicraftMenuButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean highlighted = isHoveredOrFocused();
        int outline = highlighted ? OUTLINE_HOVER : OUTLINE;
        int surface = highlighted ? SURFACE_HOVER : SURFACE;
        int cut = Math.min(5, Math.max(2, height / 3));

        drawCutPanel(graphics, getX() + 1, getY() + 2, width, height, cut, 0x77000000);
        drawCutPanel(graphics, getX(), getY(), width, height, cut, outline);
        drawCutPanel(graphics, getX() + 2, getY() + 2, width - 4, height - 4, Math.max(1, cut - 1), surface);
        graphics.fill(getX() + 5, getY() + 4, getX() + 7, getY() + height - 4, highlighted ? 0xFFFFD25A : 0xFFC47A29);
        if (highlighted) {
            graphics.fill(getX() + 9, getY() + 3, getX() + width - 8, getY() + 4, 0x55FFD477);
        }

        int color = active ? TEXT : TEXT_DISABLED;
        int textY = getY() + (height - 8) / 2;
        graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + width / 2 + 2, textY, color);
    }

    private static void drawCutPanel(GuiGraphics graphics, int x, int y, int width, int height, int cut, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (width <= cut * 2 || height <= cut * 2) {
            graphics.fill(x, y, x + width, y + height, color);
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
