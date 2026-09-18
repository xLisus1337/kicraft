package net.kicraft.kicraft.client;

import net.kicraft.kicraft.Kicraft;
import net.kicraft.kicraft.core.player.KiStat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Kicraft.MODID, value = Dist.CLIENT)
public final class ClientStatLevelUpNotifier {
    private static final int DISPLAY_TICKS = 34;
    private static final Deque<LevelUpNotice> QUEUE = new ArrayDeque<>();
    private static LevelUpNotice current;

    private ClientStatLevelUpNotifier() {
    }

    public static void enqueue(Map<KiStat, Double> levels) {
        if (levels == null || levels.isEmpty()) {
            return;
        }

        for (KiStat stat : KiStat.values()) {
            Double level = levels.get(stat);
            if (level != null) {
                QUEUE.addLast(new LevelUpNotice(stat, level));
            }
        }
        startNextIfNeeded();
    }

    public static void reset() {
        QUEUE.clear();
        current = null;
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || current == null) {
            return;
        }

        current.remainingTicks--;
        if (current.remainingTicks <= 0) {
            current = null;
            startNextIfNeeded();
        }
    }

    public static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (current == null || minecraft.player == null || minecraft.options.hideGui || minecraft.screen != null) {
            return;
        }

        float life = current.remainingTicks / (float) DISPLAY_TICKS;
        float alpha = Math.min(1.0F, Math.min((1.0F - life) * 9.0F, life * 7.0F));
        if (alpha <= 0.02F) {
            return;
        }

        int centerX = minecraft.getWindow().getGuiScaledWidth() / 2;
        int centerY = minecraft.getWindow().getGuiScaledHeight() / 2 - 30;
        int color = withAlpha(colorFor(current.stat), alpha);
        int muted = withAlpha(0xFFF7E8C7, alpha);
        int shadow = withAlpha(0xFF080604, alpha * 0.72F);
        String title = Component.translatable("hud.kicraft.stat_level_up", Component.translatable(current.stat.translationKey())).getString();
        String level = Component.translatable("hud.kicraft.stat_level", formatLevel(current.level)).getString();

        graphics.fill(centerX - 118, centerY - 8, centerX + 118, centerY + 29, shadow);
        graphics.fill(centerX - 88, centerY - 10, centerX + 88, centerY - 8, color);
        drawCentered(graphics, title, centerX, centerY, color, 1.18F);
        drawCentered(graphics, level, centerX, centerY + 17, muted, 0.82F);
    }

    private static void startNextIfNeeded() {
        if (current != null || QUEUE.isEmpty()) {
            return;
        }

        current = QUEUE.removeFirst();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.playSound(SoundEvents.PLAYER_LEVELUP, 0.62F, 1.10F);
        }
    }

    private static void drawCentered(GuiGraphics graphics, String text, int centerX, int y, int color, float scale) {
        Minecraft minecraft = Minecraft.getInstance();
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        int scaledCenterX = Math.round(centerX / scale);
        graphics.drawCenteredString(minecraft.font, text, scaledCenterX, Math.round(y / scale), color);
        graphics.pose().popPose();
    }

    private static int colorFor(KiStat stat) {
        return switch (stat) {
            case STRENGTH -> 0xFFFF8A3D;
            case DURABILITY -> 0xFF68B7FF;
            case MAX_KI -> 0xFF47E9FF;
            case KI_CONTROL -> 0xFFC384FF;
            case STAMINA -> 0xFF81FF9A;
        };
    }

    private static String formatLevel(double level) {
        if (level >= 1.0E21) {
            return "MAXLVL";
        }
        return Long.toString(Math.max(1L, (long) Math.floor(level)));
    }

    private static int withAlpha(int color, float alpha) {
        int channel = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
        return (channel << 24) | (color & 0xFFFFFF);
    }

    private static final class LevelUpNotice {
        private final KiStat stat;
        private final double level;
        private int remainingTicks = DISPLAY_TICKS;

        private LevelUpNotice(KiStat stat, double level) {
            this.stat = stat;
            this.level = level;
        }
    }
}
