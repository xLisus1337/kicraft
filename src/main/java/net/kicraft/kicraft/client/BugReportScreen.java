package net.kicraft.kicraft.client;

import net.kicraft.kicraft.network.BugReportPacket;
import net.kicraft.kicraft.network.KicraftNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Small client form; validation and Discord delivery always happen server-side. */
public final class BugReportScreen extends Screen {
    private final Screen parent;
    private EditBox reportField;

    public BugReportScreen(Screen parent) {
        super(Component.translatable("screen.kicraft.bug_report.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int fieldWidth = Math.min(320, width - 40);
        int fieldX = (width - fieldWidth) / 2;
        int fieldY = height / 2 - 12;

        reportField = addRenderableWidget(new EditBox(font, fieldX, fieldY, fieldWidth, 20,
                Component.translatable("screen.kicraft.bug_report.input")));
        reportField.setMaxLength(BugReportPacket.MAX_REPORT_LENGTH);
        reportField.setFocused(true);
        setInitialFocus(reportField);

        int buttonWidth = 150;
        int buttonY = fieldY + 32;
        addRenderableWidget(Button.builder(Component.translatable("screen.kicraft.bug_report.submit"), button -> submit())
                .bounds(fieldX, buttonY, buttonWidth, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.kicraft.bug_report.cancel"), button -> onClose())
                .bounds(fieldX + fieldWidth - buttonWidth, buttonY, buttonWidth, 20)
                .build());
    }

    private void submit() {
        KicraftNetwork.sendToServer(new BugReportPacket(reportField.getValue()));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 45, 0xFFFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.kicraft.bug_report.hint"),
                width / 2, height / 2 - 30, 0xFFAFAFAF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
