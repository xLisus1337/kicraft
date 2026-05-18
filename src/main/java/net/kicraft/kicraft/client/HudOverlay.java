package net.kicraft.kicraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.kicraft.kicraft.capability.PlayerDataProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class HudOverlay {
    private static float visualHp = -1;
    private static float visualStamina = -1;
    private static float visualKi = -1;

    public static final IGuiOverlay HUD_KICRAFT = (gui, guiGraphics, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        mc.player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
            float smoothFactor = 0.25f; // Szybka i płynna reakcja

            float targetHp = mc.player.getHealth();
            if (visualHp == -1) visualHp = targetHp;
            visualHp = Mth.lerp(smoothFactor, visualHp, targetHp);

            float targetStamina = (float)data.getStamina();
            if (visualStamina == -1) visualStamina = targetStamina;
            visualStamina = Mth.lerp(smoothFactor, visualStamina, targetStamina);

            float targetKi = (float)data.getKi();
            if (visualKi == -1) visualKi = targetKi;
            visualKi = Mth.lerp(smoothFactor, visualKi, targetKi);

            float hpFill = Mth.clamp(visualHp / mc.player.getMaxHealth(), 0, 1);
            float kiFill = Mth.clamp(visualKi / (float)data.getMaxKi(), 0, 1);
            float staminaFill = Mth.clamp(visualStamina / (float)data.getMaxStamina(), 0, 1);

            RenderSystem.enableBlend();
            int startX = 10, startY = 10, boxWidth = 150, boxHeight = 85;

            guiGraphics.fill(startX, startY, startX + boxWidth, startY + boxHeight, 0xEE111111);
            int neon = 0xFFFFAA00;
            guiGraphics.fill(startX - 2, startY - 2, startX + boxWidth + 2, startY, neon);
            guiGraphics.fill(startX - 2, startY + boxHeight, startX + boxWidth + 2, startY + boxHeight + 2, neon);
            guiGraphics.fill(startX - 2, startY, startX, startY + boxHeight, neon);
            guiGraphics.fill(startX + boxWidth, startY, startX + boxWidth + 2, startY + boxHeight, neon);

            renderBar(guiGraphics, mc, "HP", startX + 8, startY + 12, startX + 35, 105, 10, hpFill, 0xFFFF3333);
            renderBar(guiGraphics, mc, "KI", startX + 8, startY + 34, startX + 35, 105, 10, kiFill, 0xFF00FFFF);
            renderBar(guiGraphics, mc, "ST", startX + 8, startY + 56, startX + 35, 105, 10, staminaFill, 0xFFFFFF00);
            RenderSystem.disableBlend();
        });
    };

    private static void renderBar(GuiGraphics g, Minecraft mc, String label, int tx, int y, int bx, int w, int h, float fill, int color) {
        g.drawString(mc.font, label, tx, y + 1, 0xFFFFFF, true);
        g.fill(bx, y, bx + w, y + h, 0xFF333333);
        if (fill > 0) g.fill(bx, y, bx + (int)(w * fill), y + h, color | 0xFF000000);
        String pct = Math.round(fill * 100) + "%";
        g.drawString(mc.font, pct, bx + (w - mc.font.width(pct)) / 2, y + 1, 0xFFFFFF, true);
    }
}