package net.kicraft.kicraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.kicraft.kicraft.Kicraft;
import net.kicraft.kicraft.core.player.KiTransformation;
import net.kicraft.kicraft.core.player.PlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

@Mod.EventBusSubscriber(modid = Kicraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class KicraftHud {
    private static final IGuiOverlay OVERLAY = (gui, graphics, partialTick, width, height) -> render(graphics);
    private static final HudStyle ACTIVE_STYLE = HudStyle.PULSE_ARRAY;

    private static final int HUD_X = 12;
    private static final int HUD_Y = 12;
    private static final int HUD_WIDTH = 218;
    private static final int HUD_HEIGHT = 76;

    private static final int SHADOW = 0x66000000;
    private static final int PANEL = 0xCC121015;
    private static final int PANEL_INNER = 0xDD1C1718;
    private static final int BORDER = 0xFFFFB35A;
    private static final int BORDER_DIM = 0xFF7B3A24;
    private static final int TEXT = 0xFFFFF0D2;
    private static final int TRACK = 0xFF332323;
    private static final int HP = 0xFFFF4056;
    private static final int KI = 0xFFFFA53A;
    private static final int STAMINA = 0xFFFFDD4A;
    private static final int RAGE = 0xFFFF7130;

    private static final int COOL_PANEL = 0xCC0A1420;
    private static final int COOL_PANEL_INNER = 0xDD101F31;
    private static final int COOL_BORDER = 0xFF5DE1FF;
    private static final int COOL_BORDER_DIM = 0xFF25506D;
    private static final int COOL_TEXT = 0xFFEAFBFF;
    private static final int COOL_TRACK = 0xFF172836;
    private static final int COOL_HP = 0xFFFF5B8B;
    private static final int COOL_KI = 0xFF45DFFF;
    private static final int COOL_STAMINA = 0xFF74FFB0;
    private static final int COOL_RAGE = 0xFFB56CFF;

    private static final int SCOUTER_CASING = 0xFFF0F3EF;
    private static final int SCOUTER_CASING_SHADE = 0xFFB8C3BE;
    private static final int SCOUTER_CASING_DARK = 0xFF4D5958;
    private static final int SCOUTER_SCREEN_BASE = 0xFF082A19;
    private static final int SCOUTER_SCREEN_DEEP = 0xFF04140D;
    private static final int SCOUTER_SCREEN_TRIM = 0xFF43B96A;
    private static final int SCOUTER_SCREEN_LINE = 0x403AFF8B;

    private KicraftHud() {
    }

    private enum HudStyle {
        WARM_PANEL,
        COOL_PANEL,
        ANGLED_COMBAT_HUD,
        SCOUTER_HUD,
        PULSE_ARRAY
    }

    @SubscribeEvent
    public static void register(RegisterGuiOverlaysEvent event) {
        // Vanilla renders F3 in DEBUG_TEXT. Keeping our HUD directly below that
        // layer means the whole diagnostic display stays visible without making
        // the normal HUD disappear.
        event.registerBelow(VanillaGuiOverlay.DEBUG_TEXT.id(), "ki_craft_hud", OVERLAY);
    }

    private static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        PlayerData data = ClientKiData.data();

        RenderSystem.enableBlend();
        int playerKiColor = transformationKiColor(data);
        int playerKiBorderDim = darken(darken(playerKiColor));
        int playerKiAccent = lighten(playerKiColor);
        int playerKiAccentDim = tint(playerKiColor, 0x88);
        graphics.pose().pushPose();
        graphics.pose().translate(ClientSettings.hudOffsetX(), ClientSettings.hudOffsetY(), 0.0F);
        float hudScale = ClientSettings.hudScale();
        graphics.pose().scale(hudScale, hudScale, 1.0F);
        if (ACTIVE_STYLE == HudStyle.PULSE_ARRAY) {
            renderPulseArrayHud(graphics, data, playerKiColor);
        } else if (ACTIVE_STYLE == HudStyle.SCOUTER_HUD) {
            renderScouterHud(graphics, data, playerKiColor, playerKiBorderDim, playerKiAccent);
        } else if (ACTIVE_STYLE == HudStyle.ANGLED_COMBAT_HUD) {
            renderAngledCombatHud(graphics, data, playerKiColor, playerKiBorderDim, playerKiAccent, playerKiAccentDim);
        } else if (ACTIVE_STYLE == HudStyle.COOL_PANEL) {
            renderPanelHud(graphics, data, COOL_PANEL, COOL_PANEL_INNER, playerKiColor, playerKiBorderDim, COOL_TEXT, COOL_TRACK,
                    COOL_HP, playerKiColor, COOL_STAMINA, COOL_RAGE, playerKiAccent, playerKiAccentDim);
        } else {
            renderPanelHud(graphics, data, PANEL, PANEL_INNER, playerKiColor, playerKiBorderDim, TEXT, TRACK,
                    HP, playerKiColor, STAMINA, RAGE, playerKiAccent, playerKiAccentDim);
        }
        renderTransformationCharge(graphics, minecraft, playerKiColor, playerKiBorderDim, playerKiAccent);
        graphics.pose().popPose();
        renderLockOn(graphics, minecraft);
        ClientStatXpNotifier.render(graphics);
        ClientStatLevelUpNotifier.render(graphics);
        RenderSystem.disableBlend();
    }

    private static void renderPulseArrayHud(GuiGraphics graphics, PlayerData data, int kiColor) {
        boolean showRage = ClientKiData.isInCombat();
        boolean showDragonRadar = data.hasDragonRadar() && ClientSettings.showDragonRadar();
        int y = HUD_Y + 6;
        int panelX = HUD_X;
        int panelY = y + 4;
        int panelWidth = 320;
        int panelHeight = showRage ? 84 : 68;
        int shell = 0xFF211208;
        int shellDim = 0xFF78401A;
        int inner = 0xF40D0804;
        int channelTrack = 0xFF2D1B0A;
        int accent = lerpColor(0xFFFFB02E, kiColor, 0.14F);
        int accentDim = 0xFF7B3A13;

        drawPulseShell(graphics, panelX, panelY, panelWidth, panelHeight, shell, shellDim, inner, accent, accentDim);
        drawWarmClouds(graphics, panelX + 96, panelY + 7, panelWidth - 112, panelHeight - 14);

        int channelX = panelX + 21;
        int channelWidth = panelWidth - (showDragonRadar ? 96 : 76);
        drawPulseChannel(graphics, channelX, panelY + 13, channelWidth, "HP", data.hp(), data.maxHp(), 0xFFFF4D60, channelTrack, accent, false);
        drawPulseChannel(graphics, channelX, panelY + 29, channelWidth, "KI", data.ki(), data.maxKi(), kiColor, channelTrack, accent, false);
        drawPulseChannel(graphics, channelX, panelY + 45, channelWidth, "ST", data.stamina(), data.maxStamina(), 0xFFFFD75A, channelTrack, accent, true);
        if (showRage) {
            drawPulseChannel(graphics, channelX, panelY + 61, channelWidth, "RG", data.rage(), 100.0, 0xFFFF8B3D, channelTrack, accent, false);
        }
        if (showDragonRadar) {
            drawDragonRadar(graphics, panelX + panelWidth - 34, panelY + panelHeight / 2, accentDim);
        }
    }

    private static void drawDragonRadar(GuiGraphics graphics, int centerX, int centerY, int accentDim) {
        int outer = 22;
        int inner = 18;
        drawRadarDisc(graphics, centerX, centerY, outer, 0xFF050A08);
        drawRadarDisc(graphics, centerX, centerY, outer - 1, 0xFF355345);
        drawRadarDisc(graphics, centerX, centerY, outer - 3, accentDim);
        drawRadarDisc(graphics, centerX, centerY, inner, 0xFF0C4B27);
        drawRadarGrid(graphics, centerX, centerY, inner - 1);

        drawRadarPlayerMarker(graphics, centerX, centerY);
        drawRadarSignal(graphics, centerX + 9, centerY - 8);
        drawRadarSignal(graphics, centerX - 12, centerY + 5);
        drawRadarSignal(graphics, centerX + 4, centerY + 12);
    }

    private static void drawRadarSignal(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 2, y - 2, x + 3, y + 3, 0xFF59300A);
        graphics.fill(x - 1, y - 1, x + 2, y + 2, 0xFFFFA82E);
        graphics.fill(x, y, x + 1, y + 1, 0xFFFFE1A0);
    }

    private static void drawRadarGrid(GuiGraphics graphics, int centerX, int centerY, int radius) {
        int radiusSquared = radius * radius;
        int gridColor = 0xFF1A7040;
        for (int offset = -radius + 3; offset < radius; offset += 6) {
            int halfWidth = (int) Math.sqrt(Math.max(0, radiusSquared - offset * offset));
            graphics.fill(centerX - halfWidth, centerY + offset, centerX + halfWidth + 1, centerY + offset + 1, gridColor);

            int halfHeight = (int) Math.sqrt(Math.max(0, radiusSquared - offset * offset));
            graphics.fill(centerX + offset, centerY - halfHeight, centerX + offset + 1, centerY + halfHeight + 1, gridColor);
        }
    }

    private static void drawRadarPlayerMarker(GuiGraphics graphics, int centerX, int centerY) {
        graphics.fill(centerX, centerY - 3, centerX + 1, centerY + 3, 0xFFFF4B38);
        graphics.fill(centerX - 2, centerY + 1, centerX + 3, centerY + 2, 0xFFFF4B38);
        graphics.fill(centerX - 1, centerY - 1, centerX + 2, centerY + 2, 0xFFFFB14B);
    }

    private static void drawRadarDisc(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        int radiusSquared = radius * radius;
        for (int yOffset = -radius; yOffset <= radius; yOffset++) {
            int halfWidth = (int) Math.sqrt(Math.max(0, radiusSquared - yOffset * yOffset));
            graphics.fill(centerX - halfWidth, centerY + yOffset, centerX + halfWidth + 1, centerY + yOffset + 1, color);
        }
    }

    private static void drawPulseShell(GuiGraphics graphics, int x, int y, int width, int height, int shell, int shellDim, int inner, int accent, int accentDim) {
        drawCutPanel(graphics, x, y, width, height, 7, shellDim);
        drawCutPanel(graphics, x + 2, y + 2, width - 4, height - 4, 6, shell);
        drawCutPanel(graphics, x + 6, y + 6, width - 12, height - 12, 4, inner);

        graphics.fill(x + 14, y + 3, x + 76, y + 5, accent);
        graphics.fill(x + 86, y + 3, x + width - 15, y + 5, accentDim);
        graphics.fill(x + 14, y + height - 5, x + 54, y + height - 3, accentDim);
        graphics.fill(x + 64, y + height - 5, x + width - 15, y + height - 3, accent);
    }

    private static void drawWarmClouds(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width < 30 || height < 20) {
            return;
        }
        drawCloudWisp(graphics, x + width - 62, y + 1, 2, 0x182B1203);
        drawCloudWisp(graphics, x + 18, y + height - 15, 2, 0x204E2307);
        drawCloudWisp(graphics, x + width - 110, y + height - 10, 1, 0x1E8A4310);
        graphics.fill(x + 5, y + height / 2, x + width - 7, y + height / 2 + 1, 0x1BFFB54B);
    }

    private static void drawCloudWisp(GuiGraphics graphics, int x, int y, int unit, int color) {
        int s = Math.max(1, unit);
        graphics.fill(x, y + s * 2, x + s * 12, y + s * 5, color);
        graphics.fill(x + s * 2, y + s, x + s * 9, y + s * 6, color);
        graphics.fill(x + s * 5, y, x + s * 7, y + s * 6, color);
        graphics.fill(x + s * 10, y + s * 3, x + s * 15, y + s * 5, color);
    }

    private static void drawPulseChannel(GuiGraphics graphics, int x, int y, int width, String label, double value, double max, int color, int track, int accent, boolean showExhaustionZone) {
        Minecraft minecraft = Minecraft.getInstance();
        double ratio = max <= 0.0 ? 0.0 : Math.max(0.0, Math.min(1.0, value / max));
        String valueText = resourceText(value, max);
        int valueSpace = ClientSettings.showResourceNumbers() ? 94 : 58;
        int barX = x + valueSpace;
        int barWidth = Math.max(24, width - valueSpace);
        boolean displayFull = Math.round(ratio * 100.0) >= 100;
        int filled = displayFull ? barWidth : (int) Math.round(barWidth * ratio);

        drawCutPanel(graphics, x, y, width, 10, 3, 0xFF050908);
        drawCutPanel(graphics, x + 1, y + 1, width - 2, 8, 2, 0xFF101917);
        graphics.fill(x + 3, y + 2, x + 5, y + 8, color);
        drawScaledString(graphics, label, x + 9, y + 2, 0xFFF1F7F2, 0.70F);
        drawScaledString(graphics, valueText, x + 28, y + 2, 0xFFD5DED8, 0.62F);

        graphics.fill(barX - 1, y + 1, barX + barWidth + 1, y + 9, 0xFF020504);
        graphics.fill(barX, y + 2, barX + barWidth, y + 8, track);
        if (showExhaustionZone) {
            int exhaustionEnd = barX + Math.max(2, (int) Math.round(barWidth * 0.10));
            graphics.fill(barX, y + 2, exhaustionEnd, y + 8, 0xFF3D2414);
            graphics.fill(exhaustionEnd - 1, y + 2, exhaustionEnd, y + 8, 0xFFFF9D2E);
        }
        if (filled > 0) {
            graphics.fill(barX, y + 2, barX + Math.max(1, filled), y + 8, color);
        }
        if (filled > 5) {
            graphics.fill(barX + 1, y + 3, barX + filled - 1, y + 4, lighten(color));
        }
        drawPulseTicks(graphics, barX, y + 2, barWidth, 6, displayFull ? accent : 0x55000000);
    }

    private static void drawPulseTicks(GuiGraphics graphics, int x, int y, int width, int height, int completedColor) {
        for (int offset = 18; offset < width; offset += 18) {
            graphics.fill(x + offset, y, x + offset + 1, y + height, 0x66000000);
            if (offset < width - 2) {
                graphics.fill(x + offset + 1, y + height - 1, x + offset + 4, y + height, completedColor);
            }
        }
    }

    private static void drawCutPanel(GuiGraphics graphics, int x, int y, int width, int height, int cut, int color) {
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

    private static void renderScouterHud(GuiGraphics graphics, PlayerData data, int kiColor, int kiBorderDim, int kiAccent) {
        boolean showRage = ClientKiData.isInCombat();
        int moduleX = HUD_X;
        int displayX = moduleX + 35;
        int displayY = HUD_Y + 10;
        int moduleY = displayY + 6;
        int displayWidth = 220;
        int displayHeight = showRage ? 58 : 46;
        int moduleHeight = displayHeight - 8;
        int scanAccent = lerpColor(SCOUTER_SCREEN_TRIM, kiColor, 0.12F);
        int scanHighlight = lerpColor(0xFFA6F7BA, kiAccent, 0.12F);

        drawScouterModule(graphics, moduleX, moduleY, moduleHeight, kiColor, scanHighlight);

        graphics.fill(displayX + 3, displayY + 4, displayX + displayWidth + 3, displayY + displayHeight + 4, 0x44000000);
        graphics.fill(displayX - 8, displayY + 13, displayX + 7, displayY + displayHeight - 10, SCOUTER_CASING_DARK);
        graphics.fill(displayX - 7, displayY + 14, displayX + 8, displayY + displayHeight - 11, SCOUTER_CASING);
        graphics.fill(displayX, displayY, displayX + displayWidth, displayY + displayHeight, SCOUTER_CASING_DARK);
        graphics.fill(displayX + 2, displayY + 2, displayX + displayWidth - 2, displayY + displayHeight - 2, SCOUTER_CASING_SHADE);
        graphics.fill(displayX + 4, displayY + 4, displayX + displayWidth - 4, displayY + displayHeight - 4, scanAccent);
        graphics.fill(displayX + 6, displayY + 6, displayX + displayWidth - 6, displayY + displayHeight - 6, SCOUTER_SCREEN_BASE);
        drawScouterDisplayDecorations(graphics, displayX + 7, displayY + 7, displayWidth - 14, displayHeight - 14, scanHighlight, scanAccent);

        int meterX = displayX + 13;
        int meterWidth = displayWidth - 26;
        drawScouterMeter(graphics, meterX, displayY + 8, meterWidth, "HP", data.hp(), data.maxHp(), 0xFFFF4056, scanHighlight);
        drawScouterMeter(graphics, meterX, displayY + 20, meterWidth, "KI", data.ki(), data.maxKi(), kiColor, scanHighlight);
        drawScouterMeter(graphics, meterX, displayY + 32, meterWidth, "ST", data.stamina(), data.maxStamina(), 0xFF74FFB0, scanHighlight);
        if (showRage) {
            drawScouterMeter(graphics, meterX, displayY + 44, meterWidth, "RG", data.rage(), 100.0, 0xFFFF7130, scanHighlight);
        }
    }

    private static void drawScouterModule(GuiGraphics graphics, int x, int y, int height, int lensColor, int lensHighlight) {
        graphics.fill(x + 2, y + 3, x + 42, y + height + 3, 0x44000000);
        graphics.fill(x, y, x + 39, y + height, SCOUTER_CASING_DARK);
        graphics.fill(x + 2, y + 2, x + 37, y + height - 2, SCOUTER_CASING_SHADE);
        graphics.fill(x + 4, y + 3, x + 35, y + height - 5, SCOUTER_CASING);
        graphics.fill(x + 6, y + 5, x + 31, y + 8, 0xFFFFFFFF);
        graphics.fill(x + 5, y + height - 8, x + 34, y + height - 5, 0xFFC6D0CA);
        graphics.fill(x + 28, y + 8, x + 34, y + height - 11, 0xFF68736F);
        for (int lineY = y + 11; lineY < y + height - 12; lineY += 5) {
            graphics.fill(x + 29, lineY, x + 34, lineY + 2, 0xFF28312F);
        }
        graphics.fill(x + 7, y + height - 13, x + 11, y + height - 8, 0xFFE74135);
        graphics.fill(x + 13, y + height - 13, x + 18, y + height - 8, 0xFF42504B);

        graphics.fill(x + 7, y + 11, x + 25, y + 28, SCOUTER_SCREEN_DEEP);
        graphics.fill(x + 8, y + 13, x + 26, y + 26, lensColor);
        graphics.fill(x + 10, y + 14, x + 24, y + 24, darken(lensColor));
        graphics.fill(x + 12, y + 15, x + 22, y + 19, lensHighlight);
        graphics.fill(x + 14, y + 16, x + 20, y + 18, 0xFFFFFFFF);
    }

    private static void drawScouterDisplayDecorations(GuiGraphics graphics, int x, int y, int width, int height, int accent, int dim) {
        graphics.fill(x + 4, y + 2, x + width - 4, y + 3, accent);
        graphics.fill(x + 4, y + height - 3, x + width - 4, y + height - 2, dim);
        for (int line = y + 7; line < y + height - 5; line += 12) {
            graphics.fill(x + 6, line, x + width - 12, line + 1, SCOUTER_SCREEN_LINE);
        }
        graphics.fill(x + width - 8, y + 8, x + width - 4, y + 12, accent);
        graphics.fill(x + width - 8, y + 16, x + width - 4, y + 20, dim);
    }

    private static void drawScouterMeter(GuiGraphics graphics, int x, int y, int width, String label, double value, double max, int color, int accent) {
        Minecraft minecraft = Minecraft.getInstance();
        double ratio = max <= 0.0 ? 0.0 : Math.max(0.0, Math.min(1.0, value / max));
        String percent = percentageText(value, max);
        int trackX = x + 46;
        int trackWidth = Math.max(12, width - 51);
        int filled = (int) Math.round(trackWidth * ratio);

        graphics.drawString(minecraft.font, label, x, y - 1, 0xFFFFFFFF, true);
        drawScaledString(graphics, percent, x + 19, y, 0xFFEAF7ED, 0.68F);
        graphics.fill(trackX - 1, y - 1, trackX + trackWidth + 1, y + 7, SCOUTER_SCREEN_DEEP);
        graphics.fill(trackX, y, trackX + trackWidth, y + 6, 0xFF123A23);
        if (filled > 0) {
            graphics.fill(trackX, y, trackX + Math.max(1, filled), y + 6, color);
        }
        if (filled > 4) {
            graphics.fill(trackX + 1, y + 1, trackX + filled - 1, y + 2, lighten(color));
        }
        drawSegments(graphics, trackX, y, trackWidth, 6, 17);
        graphics.fill(trackX + trackWidth + 4, y + 1, trackX + trackWidth + 7, y + 4, ratio >= 0.999 ? accent : darken(accent));
    }

    private static void renderTransformationCharge(GuiGraphics graphics, Minecraft minecraft, int border, int borderDim, int accent) {
        if (!ClientInputEvents.isTransformHoldActive()) {
            return;
        }

        float progress = Math.max(0.0F, Math.min(1.0F, ClientInputEvents.transformHoldProgress()));
        int width = 116;
        int height = 8;
        int x = 30;
        int y = HUD_Y + 86;
        int filled = Math.round((width - 2) * progress);
        graphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, 0x88000000);
        drawParallelogram(graphics, x - 1, y - 1, width + 2, height + 2, 3, borderDim);
        drawParallelogram(graphics, x, y, width, height, 3, 0xDD10151B);
        drawParallelogram(graphics, x + 1, y + 1, Math.max(1, filled), height - 2, 3, border);
        if (filled > 6) {
            drawParallelogram(graphics, x + 2, y + 1, filled - 4, 2, 3, accent);
        }
        drawScaledString(graphics, "TRANSFORM", x + width + 8, y - 1, 0xFFFFFFFF, 0.7F);
    }

    private static void renderAngledCombatHud(GuiGraphics graphics, PlayerData data, int border, int borderDim, int accent, int accentDim) {
        int x = HUD_X + 6;
        int y = HUD_Y + 2;
        int text = 0xFFFFFFFF;
        int panel = 0xAA08111A;
        int track = 0xDD10151B;

        drawAngledBar(graphics, x + 14, y + 16, 244, "HP", data.hp(), data.maxHp(), 0xFFFF4056, border, borderDim, text, track, panel, false);
        drawAngledBar(graphics, x + 14, y + 32, 244, "KI", data.ki(), data.maxKi(), border, border, borderDim, text, track, panel, false);
        drawAngledBar(graphics, x + 14, y + 52, 156, "ST", data.stamina(), data.maxStamina(), 0xFF74FFB0, border, borderDim, text, track, panel, true);
        drawAngledBar(graphics, x + 14, y + 64, 156, "RG", data.rage(), 100.0, 0xFFFF7130, border, borderDim, text, track, panel, true);
    }

    private static void renderPanelHud(GuiGraphics graphics, PlayerData data, int panel, int panelInner, int border, int borderDim,
                                       int text, int track, int hp, int ki, int stamina, int rage, int accent, int accentDim) {
        drawFrame(graphics, HUD_X, HUD_Y, panel, panelInner, border, borderDim, accent, accentDim);
        drawBar(graphics, HUD_X + 18, HUD_Y + 16, 176, "HP", data.hp(), data.maxHp(), hp, text, border, track);
        drawBar(graphics, HUD_X + 18, HUD_Y + 31, 176, "KI", data.ki(), data.maxKi(), ki, text, border, track);
        drawSmallBar(graphics, HUD_X + 18, HUD_Y + 51, 86, "ST", data.stamina(), data.maxStamina(), stamina, text, border, track);
        drawSmallBar(graphics, HUD_X + 112, HUD_Y + 51, 84, "RG", data.rage(), 100.0, rage, text, border, track);
        drawStatusDots(graphics, HUD_X + HUD_WIDTH - 14, HUD_Y + 14, data, hp, ki, stamina, rage, border, borderDim);
    }

    private static void drawFrame(GuiGraphics graphics, int x, int y, int panel, int panelInner, int border, int borderDim, int accent, int accentDim) {
        drawPanel(graphics, x + 4, y + 5, HUD_WIDTH, HUD_HEIGHT, SHADOW);
        drawPanel(graphics, x, y, HUD_WIDTH, HUD_HEIGHT, borderDim);
        drawPanel(graphics, x + 2, y + 2, HUD_WIDTH - 4, HUD_HEIGHT - 4, border);
        drawPanel(graphics, x + 5, y + 5, HUD_WIDTH - 10, HUD_HEIGHT - 10, panel);
        drawPanel(graphics, x + 9, y + 9, HUD_WIDTH - 18, HUD_HEIGHT - 18, panelInner);

        graphics.fill(x + 17, y + 7, x + 92, y + 9, accent);
        graphics.fill(x + 101, y + 7, x + 166, y + 8, accentDim);
        graphics.fill(x + HUD_WIDTH - 42, y + 7, x + HUD_WIDTH - 17, y + 9, accent);
        graphics.fill(x + 18, y + HUD_HEIGHT - 8, x + 92, y + HUD_HEIGHT - 6, borderDim);
        graphics.fill(x + 104, y + HUD_HEIGHT - 8, x + HUD_WIDTH - 18, y + HUD_HEIGHT - 6, border);
    }

    private static void drawBar(GuiGraphics graphics, int x, int y, int width, String label, double value, double max, int color, int text, int valueText, int track) {
        Minecraft minecraft = Minecraft.getInstance();
        double ratio = max <= 0.0 ? 0.0 : Math.max(0.0, Math.min(1.0, value / max));
        int labelWidth = 23;
        String percentText = percentageText(value, max);
        int percentX = x + labelWidth;
        int trackX = x + labelWidth + 34;
        int trackWidth = Math.max(8, width - (trackX - x));
        int filled = (int) Math.round(trackWidth * ratio);

        graphics.drawString(minecraft.font, label, x, y - 1, text, true);
        graphics.drawString(minecraft.font, percentText, percentX, y - 1, valueText, true);
        graphics.fill(trackX - 1, y - 2, trackX + trackWidth + 1, y + 8, 0xFF090606);
        graphics.fill(trackX, y - 1, trackX + trackWidth, y + 7, track);
        graphics.fill(trackX, y - 1, trackX + filled, y + 7, color);
        if (filled > 3) {
            graphics.fill(trackX + 1, y, trackX + filled - 1, y + 2, lighten(color));
            graphics.fill(trackX + 1, y + 5, trackX + filled - 1, y + 7, darken(color));
        }
        drawSegments(graphics, trackX, y - 1, trackWidth, 8, 18);
    }

    private static void drawSmallBar(GuiGraphics graphics, int x, int y, int width, String label, double value, double max, int color, int text, int valueText, int track) {
        Minecraft minecraft = Minecraft.getInstance();
        double ratio = max <= 0.0 ? 0.0 : Math.max(0.0, Math.min(1.0, value / max));
        int labelWidth = 18;
        String percentText = percentageText(value, max);
        int percentX = x + labelWidth;
        int trackX = x + 42;
        int trackWidth = Math.max(8, width - (trackX - x));
        int filled = (int) Math.round(trackWidth * ratio);

        graphics.drawString(minecraft.font, label, x, y - 2, text, true);
        drawScaledString(graphics, percentText, percentX, y - 2, valueText, 0.72F);
        graphics.fill(trackX - 1, y - 1, trackX + trackWidth + 1, y + 5, 0xFF090606);
        graphics.fill(trackX, y, trackX + trackWidth, y + 4, track);
        graphics.fill(trackX, y, trackX + filled, y + 4, color);
        drawSegments(graphics, trackX, y, trackWidth, 4, 12);
    }

    private static void drawStatusDots(GuiGraphics graphics, int x, int y, PlayerData data, int hp, int ki, int stamina, int rage, int border, int borderDim) {
        drawDot(graphics, x, y, data.hp() / Math.max(1.0, data.maxHp()) < 0.25 ? hp : border);
        drawDot(graphics, x, y + 10, ki);
        drawDot(graphics, x, y + 20, stamina);
        drawDot(graphics, x, y + 30, data.rage() > 50.0 ? rage : borderDim);
    }

    private static void drawDot(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x, y, x + 5, y + 5, 0xFF090606);
        graphics.fill(x + 1, y + 1, x + 4, y + 4, color);
    }

    private static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x + 5, y, x + width - 5, y + height, color);
        graphics.fill(x, y + 5, x + width, y + height - 5, color);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, color);
    }

    private static void drawSegments(GuiGraphics graphics, int x, int y, int width, int height, int spacing) {
        for (int sx = x + spacing; sx < x + width; sx += spacing) {
            graphics.fill(sx, y, sx + 1, y + height, 0x55000000);
        }
    }

    private static void drawAngledBar(GuiGraphics graphics, int x, int y, int width, String label, double value, double max, int color,
                                      int border, int borderDim, int text, int track, int panel, boolean compact) {
        Minecraft minecraft = Minecraft.getInstance();
        double ratio = max <= 0.0 ? 0.0 : Math.max(0.0, Math.min(1.0, value / max));
        String percentText = percentageText(value, max);
        int barHeight = compact ? 8 : 12;
        int slant = compact ? 3 : 4;
        int filled = (int) Math.round((width - 2) * ratio);
        int labelY = y + (compact ? 0 : 1);
        int percentX = x + (compact ? 27 : 38);

        drawParallelogram(graphics, x - 2, y - 2, width + 4, barHeight + 4, slant, 0x88000000);
        drawParallelogram(graphics, x - 1, y - 1, width + 2, barHeight + 2, slant, borderDim);
        drawParallelogram(graphics, x, y, width, barHeight, slant, panel);
        drawParallelogram(graphics, x + 1, y + 1, width - 2, barHeight - 2, slant, track);
        drawParallelogram(graphics, x + 1, y + 1, Math.max(1, filled), barHeight - 2, slant, color);
        if (filled > 5) {
            drawParallelogram(graphics, x + 2, y + 1, Math.max(1, filled - 3), 2, slant, lighten(color));
        }
        drawAngledSegments(graphics, x + 1, y + 1, width - 2, barHeight - 2, slant, compact ? 18 : 28);

        if (compact) {
            drawScaledString(graphics, label, x + 7, labelY + 1, text, 0.64F);
            drawScaledString(graphics, percentText, percentX, labelY + 1, text, 0.64F);
        } else {
            graphics.drawString(minecraft.font, label, x + 8, labelY, text, true);
            graphics.drawString(minecraft.font, percentText, percentX, labelY, text, true);
        }
    }

    private static void drawParallelogram(GuiGraphics graphics, int x, int y, int width, int height, int slant, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        for (int row = 0; row < height; row++) {
            int offset = Math.round((height - 1 - row) * slant / (float) Math.max(1, height - 1));
            graphics.fill(x + offset, y + row, x + offset + width, y + row + 1, color);
        }
    }

    private static void drawAngledSegments(GuiGraphics graphics, int x, int y, int width, int height, int slant, int spacing) {
        for (int sx = spacing; sx < width; sx += spacing) {
            drawSlantedLine(graphics, x + sx + slant, y, x + sx, y + height, 1, 0x55000000);
        }
    }

    private static void renderLockOn(GuiGraphics graphics, Minecraft minecraft) {
        if (!ClientLockOnData.isLocked()) return;
        String name = ClientLockOnData.targetName();
        var target = ClientLockOnData.targetEntity();
        if (name.isBlank() && target != null) {
            name = target.getName().getString();
        }
        if (name.isBlank()) name = Component.translatable("hud.kicraft.lock_on.target").getString();

        String label = Component.translatable("hud.kicraft.lock_on.compact", name).getString();
        float scale = 0.70F;
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int textWidth = Math.round(minecraft.font.width(label) * scale);
        drawScaledString(graphics, label, (screenWidth - textWidth) / 2, screenHeight - 52,
                0xFFEAFBFF, scale);
    }

    private static void drawSlantedLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int thickness, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps <= 0) {
            graphics.fill(x1, y1, x1 + thickness, y1 + thickness, color);
            return;
        }
        for (int i = 0; i <= steps; i++) {
            double progress = i / (double) steps;
            int x = (int) Math.round(x1 + (x2 - x1) * progress);
            int y = (int) Math.round(y1 + (y2 - y1) * progress);
            graphics.fill(x, y, x + thickness, y + thickness, color);
        }
    }

    private static void drawScaledString(GuiGraphics graphics, String value, int x, int y, int color, float scale) {
        Minecraft minecraft = Minecraft.getInstance();
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(minecraft.font, value, Math.round(x / scale), Math.round(y / scale), color, true);
        graphics.pose().popPose();
    }

    private static String percentageText(double value, double max) {
        if (max <= 0.0 || value <= 0.0) {
            return "0%";
        }
        double ratio = Math.max(0.0, Math.min(1.0, value / max));
        return Math.max(1L, Math.round(ratio * 100.0)) + "%";
    }

    private static String resourceText(double value, double max) {
        String percent = percentageText(value, max);
        if (!ClientSettings.showResourceNumbers()) {
            return percent;
        }
        return percent + " " + formatCompact(value) + "/" + formatCompact(max);
    }

    private static int lighten(int color) {
        int alpha = color & 0xFF000000;
        int red = Math.min(255, ((color >> 16) & 0xFF) + 52);
        int green = Math.min(255, ((color >> 8) & 0xFF) + 52);
        int blue = Math.min(255, (color & 0xFF) + 52);
        return alpha | (red << 16) | (green << 8) | blue;
    }

    private static int darken(int color) {
        int alpha = color & 0xFF000000;
        int red = Math.max(0, ((color >> 16) & 0xFF) - 52);
        int green = Math.max(0, ((color >> 8) & 0xFF) - 52);
        int blue = Math.max(0, (color & 0xFF) - 52);
        return alpha | (red << 16) | (green << 8) | blue;
    }

    private static int withAlpha(int color) {
        return 0xFF000000 | (color & 0xFFFFFF);
    }

    private static int transformationKiColor(PlayerData data) {
        int currentColor = data.activeTransformation().kiColor(data.kiColor());
        if (!ClientInputEvents.isTransformHoldActive()) {
            return withAlpha(currentColor);
        }

        KiTransformation target = ClientInputEvents.transformationChargeTarget();
        if (target == null || target == data.activeTransformation()) {
            return withAlpha(currentColor);
        }

        int targetColor = target.kiColor(data.kiColor());
        float progress = Math.max(0.0F, Math.min(1.0F, ClientInputEvents.transformHoldProgress()));
        float eased = progress * progress * (3.0F - 2.0F * progress);
        return withAlpha(lerpColor(currentColor, targetColor, eased));
    }

    private static int lerpColor(int from, int to, float progress) {
        int red = lerpChannel((from >> 16) & 0xFF, (to >> 16) & 0xFF, progress);
        int green = lerpChannel((from >> 8) & 0xFF, (to >> 8) & 0xFF, progress);
        int blue = lerpChannel(from & 0xFF, to & 0xFF, progress);
        return (red << 16) | (green << 8) | blue;
    }

    private static int lerpChannel(int from, int to, float progress) {
        return Math.round(from + (to - from) * progress);
    }

    private static int tint(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0xFFFFFF);
    }

    private static String formatCompact(double value) {
        double safe = Math.max(0.0, value);
        String[] suffixes = {
                "",
                "K",
                "M",
                "B",
                "T",
                "Qa",
                "Qi",
                "Sx"
        };
        int tier = 0;
        while (safe >= 1000.0 && tier < suffixes.length - 1) {
            safe /= 1000.0;
            tier++;
        }
        if (tier == suffixes.length - 1 && safe >= 999.5) {
            return "MAX";
        }
        return tier == 0 ? Long.toString(Math.round(safe)) : trimCompact(safe) + suffixes[tier];
    }

    private static String trimCompact(double value) {
        String formatted = String.format(Locale.ROOT, "%.1f", value);
        return formatted.endsWith(".0") ? formatted.substring(0, formatted.length() - 2) : formatted;
    }
}
