package net.kicraft.kicraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.kicraft.kicraft.capability.PlayerDataProvider;
import net.kicraft.kicraft.util.KicraftUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class CharacterMenuScreen extends Screen {

    // --- WYMIARY KONSOLI ---
    private final int totalWidth = 600;
    private final int totalHeight = 360;

    private final int techBoxWidth = 110;
    private final int modelBoxWidth = 110;
    private final int statsBoxWidth = 220;
    private final int attacksBoxWidth = 100;

    private final int topBoxesHeight = 220;
    private final int formsBoxHeight = 70;
    private final int boxSpacing = 10;

    public CharacterMenuScreen() {
        super(Component.literal("Scouter Console"));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int guiX = (this.width - totalWidth) / 2;
        int guiY = (this.height - totalHeight) / 2;
        int boxesStartY = guiY + 35;

        RenderSystem.enableBlend();

        // --- TYTUŁ KONSOLI (v1.2 WYWALONE) ---
        guiGraphics.drawCenteredString(this.font, "§d§lSCOUTER CONSOLE", guiX + (totalWidth / 2), guiY + 12, 0xFFFFFF);

        // ==========================================
        // --- 1. RAMKA: TECHNIQUES (LEWA) ---
        // ==========================================
        int techX = guiX;
        drawBorderedBox(guiGraphics, techX, boxesStartY, techBoxWidth, topBoxesHeight);
        guiGraphics.drawCenteredString(this.font, "§bTECHNIQUES", techX + (techBoxWidth / 2), boxesStartY + 12, 0xFFFFFF);

        int techY = boxesStartY + 40;
        String[] techs = {"§f- Flight", "§f- Kaioken", "§f- Ki Sense", "§f- Meditation"};
        for(String t : techs) {
            guiGraphics.drawString(this.font, t, techX + 10, techY, 0xFFFFFF);
            techY += 15;
        }

        // ==========================================
        // --- 2. RAMKA: ANALYSIS (MODEL) ---
        // ==========================================
        int modelX = techX + techBoxWidth + boxSpacing;
        drawBorderedBox(guiGraphics, modelX, boxesStartY, modelBoxWidth, topBoxesHeight);
        guiGraphics.drawCenteredString(this.font, "§6ANALYSIS", modelX + (modelBoxWidth / 2), boxesStartY + 12, 0xFFFFFF);

        if (Minecraft.getInstance().player != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    guiGraphics, modelX + (modelBoxWidth / 2), boxesStartY + topBoxesHeight - 15,
                    70, (float)(modelX + modelBoxWidth / 2) - mouseX, (float)(boxesStartY + topBoxesHeight - 110) - mouseY,
                    Minecraft.getInstance().player
            );
        }

        // ==========================================
        // --- 3. RAMKA: TRAINING STATUS ---
        // ==========================================
        int statsX = modelX + modelBoxWidth + boxSpacing;
        drawBorderedBox(guiGraphics, statsX, boxesStartY, statsBoxWidth, topBoxesHeight);
        guiGraphics.drawCenteredString(this.font, "§6TRAINING STATUS", statsX + (statsBoxWidth / 2), boxesStartY + 12, 0xFFFFFF);

        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                int startY = boxesStartY + 40;
                int spacing = 28;
                renderStat(guiGraphics, statsX + 10, startY, "Strength", data.getStrength(), data.getStrengthXP(), data.getReqStrengthXP(), 0xFFFF8800, statsBoxWidth - 20, mouseX, mouseY);
                renderStat(guiGraphics, statsX + 10, startY + spacing, "Durability", data.getDurability(), data.getDurabilityXP(), data.getReqDurabilityXP(), 0xFF0088FF, statsBoxWidth - 20, mouseX, mouseY);
                renderStat(guiGraphics, statsX + 10, startY + spacing * 2, "Max HP", (long)data.getMaxHP(), data.getMaxHpXP(), data.getReqMaxHpXP(), 0xFFFF3333, statsBoxWidth - 20, mouseX, mouseY);
                renderStat(guiGraphics, statsX + 10, startY + spacing * 3, "Max Stamina", (long)data.getMaxStamina(), data.getStaminaXP(), data.getReqStaminaXP(), 0xFFFFFF00, statsBoxWidth - 20, mouseX, mouseY);
                renderStat(guiGraphics, statsX + 10, startY + spacing * 4, "Max Ki", (long)data.getMaxKi(), data.getKiXP(), data.getReqKiXP(), 0xFF00FFFF, statsBoxWidth - 20, mouseX, mouseY);
                renderStat(guiGraphics, statsX + 10, startY + spacing * 5, "Ki Control", data.getKiControl(), data.getKiControlXP(), data.getReqKiControlXP(), 0xFFAA00FF, statsBoxWidth - 20, mouseX, mouseY);
            });
        }

        // ==========================================
        // --- 4. RAMKA: ATTACKS ---
        // ==========================================
        int attacksX = statsX + statsBoxWidth + boxSpacing;
        drawBorderedBox(guiGraphics, attacksX, boxesStartY, attacksBoxWidth, topBoxesHeight);
        guiGraphics.drawCenteredString(this.font, "§9ATTACKS", attacksX + (attacksBoxWidth / 2), boxesStartY + 12, 0xFFFFFF);

        int slotSize = 24;
        for (int i = 0; i < 4; i++) {
            int sX = attacksX + (attacksBoxWidth - slotSize) / 2;
            int sY = boxesStartY + 50 + (i * (slotSize + 10));
            drawBorderedBox(guiGraphics, sX, sY, slotSize, slotSize);
            guiGraphics.drawCenteredString(this.font, "§7"+(i+1), sX + (slotSize / 2), sY + (slotSize / 2) - 4, 0xAAAAAA);
        }

        // ==========================================
        // --- DOLNA RAMKA: FORMS ---
        // ==========================================
        int formsStartY = boxesStartY + topBoxesHeight + boxSpacing;
        drawBorderedBox(guiGraphics, guiX, formsStartY, totalWidth, formsBoxHeight);
        guiGraphics.drawString(this.font, "§d> UNLOCKED FORMS", guiX + 15, formsStartY + 10, 0xFFFFFF);

        RenderSystem.disableBlend();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void drawBorderedBox(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, 0xCC111111);
        int colorTopBottom = 0xFFFFAA00;
        int colorSides = 0xFFCC8800;
        guiGraphics.fill(x - 1, y - 1, x + width + 1, y, colorTopBottom);
        guiGraphics.fill(x - 1, y + height, x + width + 1, y + height + 1, colorTopBottom);
        guiGraphics.fill(x - 1, y, x, y + height, colorSides);
        guiGraphics.fill(x + width, y, x + width + 1, y + height, colorSides);
    }

    private void renderStat(GuiGraphics guiGraphics, int x, int y, String name, long level, double xp, double req, int color, int barWidth, int mx, int my) {
        String levelStr = " Lvl " + KicraftUtil.formatNumber(level);
        guiGraphics.drawString(this.font, "§7" + name + ":" + levelStr, x, y, 0xFFFFFF);

        int barY = y + 11;
        float fill = (float)Math.min(1.0, xp / Math.max(1.0, req));

        guiGraphics.fill(x, barY, x + barWidth, barY + 4, 0xFF333333);
        if (fill > 0) guiGraphics.fill(x, barY, x + (int)(barWidth * fill), barY + 4, color | 0xFF000000);

        if (mx >= x && mx <= x + barWidth && my >= y && my <= barY + 5) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("§6§l" + name));
            tooltip.add(Component.literal("§fProgress: §e" + String.format("%.1f", fill * 100) + "%"));
            tooltip.add(Component.literal("§8XP: " + String.format("%,.0f", xp) + " / " + String.format("%,.0f", req)));
            guiGraphics.renderComponentTooltip(this.font, tooltip, mx, my);
        }
    }

    @Override
    public boolean isPauseScreen() { return true; }
}