package net.kicraft.kicraft.client;

import net.kicraft.kicraft.core.player.KiStat;
import net.kicraft.kicraft.core.player.PlayerData;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class StatsScreen extends RadialMenuScreen {
    public StatsScreen() {
        this(null);
    }

    public StatsScreen(Screen returnScreen) {
        super(Component.translatable("screen.kicraft.stats"), returnScreen);
    }

    @Override
    protected List<RadialEntry> entries(PlayerData data) {
        List<RadialEntry> entries = new ArrayList<>();
        for (KiStat stat : KiStat.values()) {
            double required = data.statXpRequired(stat);
            double progress = required <= 0.0 ? 0.0 : Math.max(0.0, Math.min(1.0, data.statXp(stat) / required));
            entries.add(new RadialEntry(stat.name(), Component.translatable(stat.translationKey()), Component.literal(fmt(data.stat(stat)) + "  " + Math.round(progress * 100.0) + "%")));
        }
        if (data.isGodKiUnlocked()) {
            entries.add(new RadialEntry("GOD_KI", Component.literal("God Ki"), Component.literal(fmt(data.godKi()))));
        }
        return entries;
    }

    @Override
    protected List<Component> coreLines(PlayerData data) {
        return List.of(Component.translatable("screen.kicraft.stats.title"));
    }

    private static String fmt(double value) {
        if (value >= PlayerData.MAX_LEVEL) {
            return "MAXLVL";
        }
        String[] suffixes = {"", "K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc", "Ud", "Dd", "Td", "Qad", "Qid", "Sxd", "Spd", "Ocd", "Nod", "Vg"};
        int tier = 0;
        while (Math.abs(value) >= 1000.0 && tier < suffixes.length - 1) {
            value /= 1000.0;
            tier++;
        }
        if (tier == 0) {
            return String.format(Locale.ROOT, "%.0f", value);
        }
        return String.format(Locale.ROOT, Math.abs(value) >= 100.0 ? "%.0f%s" : "%.1f%s", value, suffixes[tier]).replace(".0", "");
    }
}
