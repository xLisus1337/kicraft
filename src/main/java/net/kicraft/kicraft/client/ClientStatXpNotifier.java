package net.kicraft.kicraft.client;

import net.kicraft.kicraft.Kicraft;
import net.kicraft.kicraft.core.player.KiStat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumMap;
import java.util.Map;

/** Small, client-only training gain readout above the vanilla experience bar. */
@Mod.EventBusSubscriber(modid = Kicraft.MODID, value = Dist.CLIENT)
public final class ClientStatXpNotifier {
    private static final int DISPLAY_TICKS = 30;
    private static GainNotice current;

    private ClientStatXpNotifier() {
    }

    public static void show(Map<KiStat, Double> gains) {
        if (gains == null || gains.isEmpty()) {
            return;
        }

        // Each training action replaces the previous popup. This keeps the
        // number equal to the single action that just happened, not a running sum.
        current = new GainNotice(gains);
    }

    public static void reset() {
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
        }
    }

    public static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (current == null || minecraft.player == null || minecraft.options.hideGui || minecraft.screen != null) {
            return;
        }

        float life = current.remainingTicks / (float) DISPLAY_TICKS;
        float alpha = Math.min(1.0F, Math.min((1.0F - life) * 8.0F, life * 6.0F));
        if (alpha <= 0.02F) {
            return;
        }

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        int centerX = width / 2;
        int lift = Math.round((1.0F - life) * 6.0F);
        int y = height - 52 - lift;

        String text = current.text();
        int textWidth = minecraft.font.width(text);
        int panelWidth = Math.max(92, textWidth + 22);
        int panelX = centerX - panelWidth / 2;
        int panelColor = withAlpha(0xFF080B0A, alpha * 0.86F);
        int borderColor = withAlpha(0xFFFFB33C, alpha);

        graphics.fill(panelX, y - 3, panelX + panelWidth, y + 11, panelColor);
        graphics.fill(panelX + 4, y - 4, panelX + panelWidth - 4, y - 3, borderColor);
        graphics.fill(panelX + 4, y + 11, panelX + panelWidth - 4, y + 12, withAlpha(0xFF5C3210, alpha));

        int cursorX = panelX + 10;
        for (KiStat stat : KiStat.values()) {
            double amount = current.gain(stat);
            if (amount <= 0.0) {
                continue;
            }

            String amountText = "+" + format(amount) + " XP ";
            graphics.drawString(minecraft.font, amountText, cursorX, y, withAlpha(0xFFE8FFF0, alpha), true);
            cursorX += minecraft.font.width(amountText);

            String statText = Component.translatable(stat.translationKey()).getString();
            graphics.drawString(minecraft.font, statText, cursorX, y, withAlpha(colorFor(stat), alpha), true);
            cursorX += minecraft.font.width(statText) + 10;
        }
    }

    private static int colorFor(KiStat stat) {
        return switch (stat) {
            case STRENGTH -> 0xFFFF9A4C;
            case DURABILITY -> 0xFF75BBFF;
            case MAX_KI -> 0xFF57E8FF;
            case KI_CONTROL -> 0xFFC890FF;
            case STAMINA -> 0xFF8DFF9F;
        };
    }

    private static String format(double amount) {
        return amount >= 10.0
                ? Long.toString(Math.round(amount))
                : String.format(java.util.Locale.ROOT, "%.2f", amount);
    }

    private static int withAlpha(int color, float alpha) {
        int channel = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
        return (channel << 24) | (color & 0xFFFFFF);
    }

    private static final class GainNotice {
        private final EnumMap<KiStat, Double> gains = new EnumMap<>(KiStat.class);
        private int remainingTicks = DISPLAY_TICKS;

        private GainNotice(Map<KiStat, Double> gains) {
            merge(gains);
        }

        private void merge(Map<KiStat, Double> updates) {
            for (Map.Entry<KiStat, Double> entry : updates.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0.0 && Double.isFinite(entry.getValue())) {
                    gains.merge(entry.getKey(), entry.getValue(), Double::sum);
                }
            }
        }

        private double gain(KiStat stat) {
            return gains.getOrDefault(stat, 0.0);
        }

        private String text() {
            StringBuilder builder = new StringBuilder();
            for (KiStat stat : KiStat.values()) {
                double amount = gain(stat);
                if (amount <= 0.0) {
                    continue;
                }
                if (!builder.isEmpty()) {
                    builder.append("  ");
                }
                builder.append('+').append(format(amount)).append(" XP ")
                        .append(Component.translatable(stat.translationKey()).getString());
            }
            return builder.toString();
        }
    }
}
