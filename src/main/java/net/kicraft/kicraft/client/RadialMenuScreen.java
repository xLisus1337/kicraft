package net.kicraft.kicraft.client;

import net.kicraft.kicraft.core.player.PlayerData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared character-section screen. The class name remains for compatibility with
 * old callers, but the wheel itself is now a consistent panel grid.
 */
public abstract class RadialMenuScreen extends Screen {
    private static final int BACKDROP = 0xB8000000;
    private static final int SHADOW = 0x9A000000;
    private static final int FRAME_DARK = 0xFF130803;
    private static final int FRAME = 0xFF25130A;
    private static final int FRAME_EDGE = 0xFF75401A;
    private static final int PANEL = 0xFF140A05;
    private static final int PANEL_LINE = 0xFF512710;
    private static final int CARD = 0xFF1B0D06;
    private static final int CARD_HOVER = 0xFF45240E;
    private static final int ACCENT = 0xFFFFB13B;
    private static final int ACCENT_BRIGHT = 0xFFFFE3A1;
    private static final int TEXT = 0xFFFFF0D3;
    private static final int TEXT_DIM = 0xFFCFB89A;

    private final Screen returnScreen;

    protected RadialMenuScreen(Component title, Screen returnScreen) {
        super(title);
        this.returnScreen = returnScreen;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, BACKDROP);

        PlayerData data = ClientKiData.data();
        List<RadialEntry> entries = entries(data);
        SectionLayout layout = layout(entries);

        drawShell(graphics, layout);
        drawHeader(graphics, layout, data);
        if (entries.isEmpty()) {
            drawEmptyState(graphics, layout);
        } else {
            for (EntryLayout entryLayout : layout.entries()) {
                drawEntryCard(graphics, entryLayout, mouseX, mouseY);
            }
        }
        drawFooter(graphics, layout);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (EntryLayout entryLayout : layout(entries(ClientKiData.data())).entries()) {
                if (entryLayout.contains(mouseX, mouseY)) {
                    onEntrySelected(entryLayout.entry());
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    protected abstract List<RadialEntry> entries(PlayerData data);

    protected void onEntrySelected(RadialEntry entry) {
    }

    /** A short context line shown under the section title when it adds useful information. */
    protected List<Component> coreLines(PlayerData data) {
        return List.of();
    }

    private SectionLayout layout(List<RadialEntry> entries) {
        int panelWidth = Math.min(640, Math.max(300, width - 40));
        int panelHeight = Math.min(356, Math.max(238, height - 40));
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;
        int bodyX = panelX + 20;
        int bodyY = panelY + 76;
        int bodyWidth = panelWidth - 40;
        int bodyHeight = panelHeight - 116;

        int count = entries.size();
        int columns = count <= 2 ? Math.max(1, count) : Math.min(3, count);
        int rows = count == 0 ? 0 : (count + columns - 1) / columns;
        int gap = 8;
        int cardWidth = columns == 0 ? bodyWidth : (bodyWidth - gap * (columns - 1)) / columns;
        int cardHeight = rows == 0 ? 0 : Math.min(66, Math.max(44, (bodyHeight - gap * (rows - 1)) / rows));
        int gridHeight = rows == 0 ? 0 : rows * cardHeight + (rows - 1) * gap;
        int gridY = bodyY + Math.max(0, (bodyHeight - gridHeight) / 2);

        List<EntryLayout> entryLayouts = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            int column = index % columns;
            int row = index / columns;
            entryLayouts.add(new EntryLayout(entries.get(index), bodyX + column * (cardWidth + gap),
                    gridY + row * (cardHeight + gap), cardWidth, cardHeight));
        }
        return new SectionLayout(panelX, panelY, panelWidth, panelHeight, bodyX, bodyY, bodyWidth, bodyHeight, entryLayouts);
    }

    private void drawShell(GuiGraphics graphics, SectionLayout layout) {
        int x = layout.panelX();
        int y = layout.panelY();
        int w = layout.panelWidth();
        int h = layout.panelHeight();
        drawCutPanel(graphics, x + 5, y + 6, w, h, 12, SHADOW);
        drawCutPanel(graphics, x, y, w, h, 12, FRAME_DARK);
        drawCutPanel(graphics, x + 2, y + 2, w - 4, h - 4, 10, FRAME_EDGE);
        drawCutPanel(graphics, x + 5, y + 5, w - 10, h - 10, 8, FRAME);

        drawCloudBand(graphics, x + 14, y + 30, w - 28, 31);
        drawCutPanel(graphics, layout.bodyX() - 7, layout.bodyY() - 7,
                layout.bodyWidth() + 14, layout.bodyHeight() + 14, 7, 0xFF0B0503);
        drawCutPanel(graphics, layout.bodyX() - 5, layout.bodyY() - 5,
                layout.bodyWidth() + 10, layout.bodyHeight() + 10, 6, PANEL);
        graphics.fill(layout.bodyX(), layout.bodyY() - 3, layout.bodyX() + layout.bodyWidth(), layout.bodyY() - 2, PANEL_LINE);
    }

    private void drawHeader(GuiGraphics graphics, SectionLayout layout, PlayerData data) {
        int centerX = layout.panelX() + layout.panelWidth() / 2;
        graphics.fill(centerX - 2, layout.panelY() + 12, centerX + 3, layout.panelY() + 17, ACCENT_BRIGHT);
        drawScaledCentered(graphics, title.getString(), centerX, layout.panelY() + 25, ACCENT_BRIGHT, 1.0F, layout.panelWidth() - 110);

        Component context = contextLine(data);
        if (context != null && !context.getString().isEmpty()) {
            drawScaledCentered(graphics, context.getString(), centerX, layout.panelY() + 49, TEXT_DIM, 0.72F, layout.panelWidth() - 110);
        }
        graphics.fill(layout.panelX() + 19, layout.panelY() + 65, layout.panelX() + layout.panelWidth() - 19, layout.panelY() + 67, ACCENT);
        graphics.fill(layout.panelX() + 20, layout.panelY() + 68, layout.panelX() + 128, layout.panelY() + 69, 0xFFFFE2A4);
    }

    private Component contextLine(PlayerData data) {
        for (Component line : coreLines(data)) {
            if (!line.getString().equals(title.getString())) {
                return line;
            }
        }
        return null;
    }

    private void drawEntryCard(GuiGraphics graphics, EntryLayout layout, int mouseX, int mouseY) {
        boolean hovered = layout.contains(mouseX, mouseY);
        int x = layout.x();
        int y = layout.y();
        int w = layout.width();
        int h = layout.height();
        int outline = hovered ? ACCENT : FRAME_EDGE;
        int fill = hovered ? CARD_HOVER : CARD;

        drawCutPanel(graphics, x + 2, y + 3, w, h, 6, 0x77000000);
        drawCutPanel(graphics, x, y, w, h, 6, FRAME_DARK);
        drawCutPanel(graphics, x + 1, y + 1, w - 2, h - 2, 5, outline);
        drawCutPanel(graphics, x + 3, y + 3, w - 6, h - 6, 4, fill);
        graphics.fill(x + 10, y + 9, x + 13, y + h - 9, hovered ? ACCENT_BRIGHT : 0xFFC67525);
        graphics.fill(x + 20, y + h - 10, x + w - 15, y + h - 9, hovered ? 0x99FFE09B : 0x447A3C16);

        drawScaledText(graphics, layout.entry().label().getString(), x + 21, y + 13,
                hovered ? ACCENT_BRIGHT : TEXT, 0.90F, w - 38);
        String detail = layout.entry().detail() == null ? "" : layout.entry().detail().getString();
        if (!detail.isEmpty()) {
            drawScaledText(graphics, detail, x + 21, y + Math.max(30, h - 25),
                    hovered ? 0xFFFFC969 : TEXT_DIM, 0.70F, w - 38);
        }
    }

    private void drawEmptyState(GuiGraphics graphics, SectionLayout layout) {
        int centerX = layout.panelX() + layout.panelWidth() / 2;
        int centerY = layout.bodyY() + layout.bodyHeight() / 2;
        drawCutPanel(graphics, centerX - 136, centerY - 29, 272, 58, 7, 0xFF0D0604);
        drawCutPanel(graphics, centerX - 134, centerY - 27, 268, 54, 6, 0xFF1B0D06);
        graphics.fill(centerX - 96, centerY - 12, centerX + 96, centerY - 10, 0xFF7A3C16);
        drawScaledCentered(graphics, Component.translatable("screen.kicraft.section.empty").getString(),
                centerX, centerY + 1, TEXT, 0.80F, 240);
    }

    private void drawFooter(GuiGraphics graphics, SectionLayout layout) {
        int y = layout.panelY() + layout.panelHeight() - 19;
        int x = layout.panelX();
        int w = layout.panelWidth();
        graphics.fill(x + 20, y, x + w / 2 - 22, y + 2, 0xFF7A3C16);
        graphics.fill(x + w / 2 + 22, y, x + w - 20, y + 2, 0xFF7A3C16);
        graphics.fill(x + w / 2 - 14, y, x + w / 2 + 14, y + 2, ACCENT);
    }

    private static void drawCloudBand(GuiGraphics graphics, int x, int y, int width, int height) {
        int mist = 0x201F0C02;
        int ember = 0x269A4C16;
        drawCloud(graphics, x + 18, y + 11, 2, mist);
        drawCloud(graphics, x + width - 112, y + 5, 3, mist);
        drawCloud(graphics, x + width - 42, y + 16, 2, ember);
        graphics.fill(x + 10, y + height - 3, x + width - 10, y + height - 2, 0x448B461A);
    }

    private static void drawCloud(GuiGraphics graphics, int x, int y, int scale, int color) {
        graphics.fill(x, y + scale * 2, x + scale * 9, y + scale * 5, color);
        graphics.fill(x + scale * 2, y + scale, x + scale * 7, y + scale * 6, color);
        graphics.fill(x + scale * 4, y, x + scale * 6, y + scale * 5, color);
        graphics.fill(x + scale * 8, y + scale * 3, x + scale * 11, y + scale * 5, color);
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

    protected final void drawScaledCentered(GuiGraphics graphics, String value, int centerX, int y,
                                            int color, float initialScale, int maxWidth) {
        float scale = Math.min(initialScale, maxWidth / (float) Math.max(1, font.width(value)));
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, value,
                Math.round((centerX - font.width(value) * scale / 2.0F) / scale), Math.round(y / scale), color, true);
        graphics.pose().popPose();
    }

    private void drawScaledText(GuiGraphics graphics, String value, int x, int y, int color, float initialScale, int maxWidth) {
        float scale = Math.min(initialScale, maxWidth / (float) Math.max(1, font.width(value)));
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, value, Math.round(x / scale), Math.round(y / scale), color, true);
        graphics.pose().popPose();
    }

    @Override
    public boolean isPauseScreen() {
        return returnScreen instanceof KicraftPauseScreen;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(returnScreen);
    }

    protected record RadialEntry(String id, Component label, Component detail) {
    }

    private record EntryLayout(RadialEntry entry, int x, int y, int width, int height) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    private record SectionLayout(int panelX, int panelY, int panelWidth, int panelHeight,
                                 int bodyX, int bodyY, int bodyWidth, int bodyHeight,
                                 List<EntryLayout> entries) {
    }
}
