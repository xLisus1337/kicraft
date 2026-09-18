package net.kicraft.kicraft.client;

import net.kicraft.kicraft.core.player.KiRace;
import net.kicraft.kicraft.network.KicraftNetwork;
import net.kicraft.kicraft.network.SelectRacePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class RaceSelectionScreen extends Screen {
    public RaceSelectionScreen() {
        super(Component.translatable("screen.kicraft.race_selection"));
    }

    @Override
    protected void init() {
        int buttonWidth = 150;
        int buttonHeight = 20;
        int gap = 6;
        int columns = 2;
        KiRace[] races = playableRaces();
        int totalWidth = columns * buttonWidth + gap;
        int startX = (width - totalWidth) / 2;
        int startY = height / 2 - 62;

        for (int i = 0; i < races.length; i++) {
            KiRace race = races[i];
            int x = startX + (i % columns) * (buttonWidth + gap);
            int y = startY + (i / columns) * (buttonHeight + gap);
            addRenderableWidget(Button.builder(Component.translatable(race.translationKey()), button -> selectRace(race))
                    .bounds(x, y, buttonWidth, buttonHeight)
                    .build());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, Component.translatable("screen.kicraft.race_selection.title"), width / 2, height / 2 - 96, 0xFFFFAA00);
        graphics.drawCenteredString(font, Component.translatable("screen.kicraft.race_selection.subtitle"), width / 2, height / 2 - 82, 0xFFCCCCCC);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void selectRace(KiRace race) {
        KicraftNetwork.sendToServer(new SelectRacePacket(race));
        onClose();
    }

    private static KiRace[] playableRaces() {
        return new KiRace[] {
                KiRace.SAIYAN,
                KiRace.HALF_SAIYAN,
                KiRace.HUMAN,
                KiRace.NAMEKIAN,
                KiRace.FROST_DEMON,
                KiRace.ANDROID,
                KiRace.BIO_ANDROID,
                KiRace.MAJIN
        };
    }
}
