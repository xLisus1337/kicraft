package net.kicraft.kicraft.client;

import net.kicraft.kicraft.core.player.PlayerData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Arrays;

/** The uncluttered top-level character command wheel opened with M. */
public final class KiMainMenuScreen extends Screen {
    private static final int BACKDROP = 0xB8000000;
    private static final int FRAME_DARK = 0xFF020303;
    private static final int FRAME = 0xFF101716;
    private static final int FRAME_EDGE = 0xFF4A5651;
    private static final int PANEL = 0xFF0A100F;
    private static final int PANEL_HOVER = 0xFF1A2623;
    private static final int ACCENT = 0xFFFFB35A;
    private static final int ACCENT_BRIGHT = 0xFFFFF0D2;
    private static final int TEXT = 0xFFF5F2E9;
    private static final int TEXT_DIM = 0xFFB7B7AD;
    private static final int SECTOR_SAMPLES = 14;

    private static final MenuSection[] SECTIONS = MenuSection.values();

    public KiMainMenuScreen() {
        super(Component.translatable("screen.kicraft.menu"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(0, 0, width, height, BACKDROP);

        int centerX = width / 2;
        int centerY = height / 2;
        int outerRadius = wheelRadius();
        int innerRadius = Math.max(50, outerRadius / 3);
        MenuSection hovered = sectionAt(mouseX, mouseY, centerX, centerY, innerRadius, outerRadius);

        drawWheelFrame(graphics, centerX, centerY, outerRadius);
        for (int index = 0; index < SECTIONS.length; index++) {
            MenuSection section = SECTIONS[index];
            double start = startAngle() + index * sectorSize();
            double end = start + sectorSize();
            boolean isHovered = section == hovered;
            drawSegment(graphics, centerX, centerY, innerRadius, outerRadius, start, end, isHovered);
            drawSectionLabel(graphics, section, centerX, centerY, innerRadius, outerRadius, (start + end) * 0.5, isHovered);
        }

        drawCore(graphics, centerX, centerY, innerRadius, ClientKiData.data());
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int outerRadius = wheelRadius();
            MenuSection selected = sectionAt(mouseX, mouseY, width / 2, height / 2,
                    Math.max(50, outerRadius / 3), outerRadius);
            if (selected != null) {
                open(selected);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int wheelRadius() {
        return Math.min(168, Math.min(width / 2 - 30, height / 2 - 30));
    }

    private void open(MenuSection section) {
        switch (section) {
            case STATS -> minecraft.setScreen(new StatsScreen(this));
            case FORMS -> minecraft.setScreen(new TransformationScreen(this));
            case TECHNIQUES -> minecraft.setScreen(new TechniqueLibraryScreen(this));
            case ATTACKS -> minecraft.setScreen(new AttackLoadoutScreen(this));
            case RACE -> minecraft.setScreen(new RaceProfileScreen(this));
        }
    }

    private static MenuSection sectionAt(double mouseX, double mouseY, int centerX, int centerY,
                                         int innerRadius, int outerRadius) {
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < innerRadius || distance > outerRadius) {
            return null;
        }
        double normalized = Math.atan2(dy, dx) - startAngle();
        while (normalized < 0.0) {
            normalized += Math.PI * 2.0;
        }
        int index = (int) Math.floor(normalized / sectorSize());
        return SECTIONS[Math.min(SECTIONS.length - 1, index)];
    }

    private static double startAngle() {
        return -Math.PI / 2.0 - sectorSize() * 0.5;
    }

    private static double sectorSize() {
        return Math.PI * 2.0 / SECTIONS.length;
    }

    private static void drawWheelFrame(GuiGraphics graphics, int centerX, int centerY, int radius) {
        drawDisc(graphics, centerX, centerY, radius, FRAME_DARK);
        drawDisc(graphics, centerX, centerY, radius - 3, FRAME_EDGE);
        drawDisc(graphics, centerX, centerY, radius - 6, FRAME);
    }

    private static void drawSegment(GuiGraphics graphics, int centerX, int centerY, int innerRadius, int outerRadius,
                                    double start, double end, boolean hovered) {
        int[] borderX = annularX(centerX, innerRadius - 2, outerRadius - 9, start, end);
        int[] borderY = annularY(centerY, innerRadius - 2, outerRadius - 9, start, end);
        fillPolygon(graphics, borderX, borderY, FRAME_DARK);

        int[] segmentX = annularX(centerX, innerRadius + 2, outerRadius - 13, start + 0.018, end - 0.018);
        int[] segmentY = annularY(centerY, innerRadius + 2, outerRadius - 13, start + 0.018, end - 0.018);
        fillPolygon(graphics, segmentX, segmentY, hovered ? PANEL_HOVER : PANEL);
        drawPolygonOutline(graphics, segmentX, segmentY, hovered ? ACCENT : 0xFF35403C);
    }

    private void drawSectionLabel(GuiGraphics graphics, MenuSection section, int centerX, int centerY,
                                  int innerRadius, int outerRadius, double angle, boolean hovered) {
        int labelRadius = innerRadius + (outerRadius - innerRadius) * 2 / 3;
        int x = centerX + (int) Math.round(Math.cos(angle) * labelRadius);
        int y = centerY + (int) Math.round(Math.sin(angle) * labelRadius);
        drawScaledCentered(graphics, Component.translatable(section.translationKey).getString(), x, y - 4,
                hovered ? ACCENT_BRIGHT : TEXT, 0.90F, 108);
    }

    private void drawCore(GuiGraphics graphics, int centerX, int centerY, int radius, PlayerData data) {
        drawDisc(graphics, centerX, centerY, radius + 4, FRAME_DARK);
        drawDisc(graphics, centerX, centerY, radius + 1, FRAME_EDGE);
        drawDisc(graphics, centerX, centerY, radius - 2, 0xFF080D0C);

        drawScaledCentered(graphics, "KI CRAFT", centerX, centerY - 16, ACCENT_BRIGHT, 0.86F, radius * 2 - 16);
        drawScaledCentered(graphics, Component.translatable(data.race().translationKey()).getString(),
                centerX, centerY - 2, TEXT, 0.68F, radius * 2 - 16);
        drawScaledCentered(graphics, Component.translatable(data.activeTransformation().translationKey()).getString(),
                centerX, centerY + 10, TEXT_DIM, 0.60F, radius * 2 - 16);
    }

    private static int[] annularX(int centerX, int innerRadius, int outerRadius, double start, double end) {
        int[] points = new int[(SECTOR_SAMPLES + 1) * 2];
        for (int index = 0; index <= SECTOR_SAMPLES; index++) {
            double angle = start + (end - start) * index / SECTOR_SAMPLES;
            points[index] = centerX + (int) Math.round(Math.cos(angle) * outerRadius);
            points[points.length - 1 - index] = centerX + (int) Math.round(Math.cos(angle) * innerRadius);
        }
        return points;
    }

    private static int[] annularY(int centerY, int innerRadius, int outerRadius, double start, double end) {
        int[] points = new int[(SECTOR_SAMPLES + 1) * 2];
        for (int index = 0; index <= SECTOR_SAMPLES; index++) {
            double angle = start + (end - start) * index / SECTOR_SAMPLES;
            points[index] = centerY + (int) Math.round(Math.sin(angle) * outerRadius);
            points[points.length - 1 - index] = centerY + (int) Math.round(Math.sin(angle) * innerRadius);
        }
        return points;
    }

    private static void drawDisc(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        int radiusSquared = radius * radius;
        for (int yOffset = -radius; yOffset <= radius; yOffset++) {
            int halfWidth = (int) Math.sqrt(Math.max(0, radiusSquared - yOffset * yOffset));
            graphics.fill(centerX - halfWidth, centerY + yOffset, centerX + halfWidth + 1, centerY + yOffset + 1, color);
        }
    }

    private static void fillPolygon(GuiGraphics graphics, int[] pointsX, int[] pointsY, int color) {
        int minY = Arrays.stream(pointsY).min().orElse(0);
        int maxY = Arrays.stream(pointsY).max().orElse(0);
        for (int y = minY; y <= maxY; y++) {
            int[] intersections = new int[pointsX.length];
            int count = 0;
            for (int index = 0; index < pointsX.length; index++) {
                int next = (index + 1) % pointsX.length;
                int y1 = pointsY[index];
                int y2 = pointsY[next];
                if ((y1 <= y && y2 > y) || (y2 <= y && y1 > y)) {
                    double fraction = (y - y1) / (double) (y2 - y1);
                    intersections[count++] = (int) Math.round(pointsX[index] + fraction * (pointsX[next] - pointsX[index]));
                }
            }
            Arrays.sort(intersections, 0, count);
            for (int index = 0; index + 1 < count; index += 2) {
                graphics.fill(intersections[index], y, intersections[index + 1] + 1, y + 1, color);
            }
        }
    }

    private static void drawPolygonOutline(GuiGraphics graphics, int[] pointsX, int[] pointsY, int color) {
        for (int index = 0; index < pointsX.length; index++) {
            int next = (index + 1) % pointsX.length;
            drawLine(graphics, pointsX[index], pointsY[index], pointsX[next], pointsY[next], color);
        }
    }

    private static void drawLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(1, Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1)));
        for (int step = 0; step <= steps; step++) {
            int x = x1 + Math.round((x2 - x1) * step / (float) steps);
            int y = y1 + Math.round((y2 - y1) * step / (float) steps);
            graphics.fill(x, y, x + 1, y + 1, color);
        }
    }

    private void drawScaledCentered(GuiGraphics graphics, String value, int centerX, int y,
                                    int color, float initialScale, int maxWidth) {
        float scale = Math.min(initialScale, maxWidth / (float) Math.max(1, font.width(value)));
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, value,
                Math.round((centerX - font.width(value) * scale / 2.0F) / scale), Math.round(y / scale), color, true);
        graphics.pose().popPose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum MenuSection {
        FORMS("screen.kicraft.menu.forms"),
        TECHNIQUES("screen.kicraft.menu.techniques"),
        ATTACKS("screen.kicraft.menu.attacks"),
        STATS("screen.kicraft.menu.stats"),
        RACE("screen.kicraft.menu.race");

        private final String translationKey;

        MenuSection(String translationKey) {
            this.translationKey = translationKey;
        }
    }
}
