package net.kicraft.kicraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.kicraft.kicraft.network.InstantTransmissionC2SPacket;
import net.kicraft.kicraft.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.UUID;

public class SenseKiScreen extends Screen {
    private int phase = 0; // 0 = Wybór celu, 1 = Minigierka namierzania
    private UUID selectedTarget = null;
    private String selectedName = "";

    // Zmienne do minigierki
    private float targetX, targetY;
    private float lockProgress = 0.0f;
    private long tickCount = 0;

    // Wymiary konsoli
    private final int totalWidth = 300;
    private final int totalHeight = 200;

    public SenseKiScreen() {
        super(Component.literal("Scouter Interstellar Sense"));
    }

    @Override
    protected void init() {
        this.clearWidgets();

        if (phase == 0) {
            int y = 60;
            boolean foundAnyone = false;
            Minecraft mc = Minecraft.getInstance();

            if (mc.getConnection() != null) {
                for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
                    // TUTAJ POPRAWKA: getProfile()
                    if (mc.player != null && info.getProfile().getId().equals(mc.player.getUUID())) continue;

                    foundAnyone = true;
                    // TUTAJ POPRAWKA: getProfile()
                    String playerName = info.getProfile().getName();

                    // Customowe przyciski na liście
                    this.addRenderableWidget(Button.builder(Component.literal(""), (b) -> {
                        // TUTAJ POPRAWKA: getProfile()
                        this.selectedTarget = info.getProfile().getId();
                        this.selectedName = playerName;
                        this.phase = 1;
                        this.init();
                    }).bounds(this.width / 2 - 120, y, 240, 20).build());

                    y += 25;
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        tickCount++;

        int x = this.width / 2;
        RenderSystem.enableBlend();

        if (phase == 0) {
            // Tytuł w technical magenta box
            int titleY = 25;
            int titleBoxW = 260;
            int titleBoxH = 20;
            drawScouterBox(guiGraphics, x - (titleBoxW / 2), titleY, titleBoxW, titleBoxH, 0xCC111111);
            guiGraphics.drawCenteredString(this.font, "§dSELECT KI SIGNATURE TO LOCK ON", x, titleY + 6, 0xFFFFFF);

            int y = 60;
            boolean foundAnyone = false;
            Minecraft mc = Minecraft.getInstance();

            if (mc.getConnection() != null) {
                for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
                    // TUTAJ POPRAWKA: getProfile()
                    if (mc.player != null && info.getProfile().getId().equals(mc.player.getUUID())) continue;

                    foundAnyone = true;
                    // TUTAJ POPRAWKA: getProfile()
                    String playerName = info.getProfile().getName();

                    // Rysowanie technical list items
                    int itemW = 240;
                    int itemH = 20;
                    int itemX = x - (itemW / 2);
                    drawScouterBox(guiGraphics, itemX, y, itemW, itemH, 0xCC111111);

                    guiGraphics.drawString(this.font, "§bSignature: §f" + playerName, itemX + 15, y + 6, 0xFFFFFF);
                    guiGraphics.drawString(this.font, "§8Loc: Overworld", itemX + itemW - 80, y + 6, 0xFFFFFF);

                    y += 25;
                }
            }

            if (!foundAnyone) {
                // Technical NO KI box
                int noKiBoxW = 260;
                int noKiBoxH = 20;
                drawScouterBox(guiGraphics, x - (noKiBoxW / 2), 60, noKiBoxW, noKiBoxH, 0xCC111111);
                guiGraphics.drawCenteredString(this.font, "§cNO KI SIGNATURES DETECTED", x, 66, 0xFFFFFF);
            }
        }
        else if (phase == 1) {
            // Locking On header
            int titleY = 25;
            int titleBoxW = 260;
            int titleBoxH = 20;
            drawScouterBox(guiGraphics, x - (titleBoxW / 2), titleY, titleBoxW, titleBoxH, 0xCC111111);
            guiGraphics.drawCenteredString(this.font, "§cLOCKING ON: §f" + selectedName, x, titleY + 6, 0xFFFFFF);

            // Movement math
            float centerX = this.width / 2.0f;
            float centerY = this.height / 2.0f;
            targetX = centerX + (float) Math.sin(tickCount * 0.05) * 100 + (float) Math.cos(tickCount * 0.08) * 40;
            targetY = centerY + (float) Math.cos(tickCount * 0.06) * 70 + (float) Math.sin(tickCount * 0.09) * 30;

            int targetRadius = 15;

            // --- STYLIZOWANY CEL KI ---
            // Rysowanie koncentrycznych aur Ki
            int circles = 4;
            for (int i = 0; i < circles; i++) {
                int radius = targetRadius - i * 3;
                if (radius <= 0) break;
                // Scouter Orange/Gold pulsujące
                int alpha = (int)Mth.lerp((float)i / circles, 136, 32);
                int color = (alpha << 24) | 0xFFAA00;
                renderCircle(guiGraphics, targetX, targetY, radius, color);
            }
            guiGraphics.drawCenteredString(this.font, "§bKI", (int)targetX, (int)targetY - 4, 0xFFFFFF);

            // --- PROFESJONALNY CELOWNIK ---
            int chSize = 10;
            int chThickness = 1;
            int chColor = 0xFFFFFFFF; // Smooth white lines
            guiGraphics.fill(mouseX - chThickness, mouseY - chSize, mouseX + chThickness, mouseY + chSize, chColor);
            guiGraphics.fill(mouseX - chSize, mouseY - chThickness, mouseX + chSize, mouseY + chThickness, chColor);

            // Locking Logic
            double dist = Math.hypot(mouseX - targetX, mouseY - targetY);
            if (dist < targetRadius * 1.5) {
                lockProgress += 0.015f;
                guiGraphics.drawCenteredString(this.font, "§aLOCKING...", x, this.height - 40, 0xFFFFFF);
            } else {
                lockProgress -= 0.01f;
                guiGraphics.drawCenteredString(this.font, "§cTRACK TARGET!", x, this.height - 40, 0xFFFFFF);
            }
            lockProgress = Mth.clamp(lockProgress, 0.0f, 1.0f);

            // --- STYLIZOWANY PASEK POSTĘPU ---
            int barWidth = 200;
            int barHeight = 10;
            int barX = x - (barWidth / 2);
            int barY = this.height - 20;

            // Technical Box with Scouter Orange Neon Border
            guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xEE111111);
            int neonColor = 0xFFFFAA00;
            guiGraphics.fill(barX - 2, barY - 2, barX + barWidth + 2, barY, neonColor);
            guiGraphics.fill(barX - 2, barY + barHeight, barX + barWidth + 2, barY + barHeight + 2, neonColor);
            guiGraphics.fill(barX - 2, barY, barX, barY + barHeight, neonColor);
            guiGraphics.fill(barX + barWidth, barY, barX + barWidth + 2, barY + barHeight, neonColor);

            // Smooth Fill (Gradient Smooth Green)
            if (lockProgress > 0) {
                guiGraphics.fill(barX, barY, barX + (int)(barWidth * lockProgress), barY + barHeight, 0xFF00FF00 | 0xFF000000);
            }
            String pct = Math.round(lockProgress * 100) + "%";
            guiGraphics.drawString(this.font, pct, barX + (barWidth - this.font.width(pct)) / 2, barY + 1, 0xFFFFFF, true);

            // Victory Logic
            if (lockProgress >= 1.0f) {
                PacketHandler.sendToServer(new InstantTransmissionC2SPacket(selectedTarget));
                this.onClose();
            }
        }

        RenderSystem.disableBlend();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    // --- HELPER: Scouter Box (Neon Border, Filled) ---
    private void drawScouterBox(GuiGraphics guiGraphics, int x, int y, int width, int height, int fillColor) {
        guiGraphics.fill(x, y, x + width, y + height, fillColor);
        int neonColor = 0xFFFFAA00;
        guiGraphics.fill(x - 1, y - 1, x + width + 1, y, neonColor);
        guiGraphics.fill(x - 1, y + height, x + width + 1, y + height + 1, neonColor);
        guiGraphics.fill(x - 1, y, x, y + height, neonColor);
        guiGraphics.fill(x + width, y, x + width + 1, y + height, neonColor);
    }

    // --- HELPER: Drawing Filled Circles (Approximate) ---
    private void renderCircle(GuiGraphics g, float x, float y, int radius, int color) {
        for (int i = 0; i < radius; i++) {
            g.fill((int)(x - i), (int)(y - i), (int)(x + i), (int)(y + i), color);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}