package net.kicraft.kicraft.client;

import net.kicraft.kicraft.core.player.KiStat;
import net.kicraft.kicraft.core.player.PlayerData;
import net.kicraft.kicraft.core.player.ResourceCosts;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.Locale;

/** Shows trained progress separately from the bonuses used by gameplay. */
public final class StatDetailsScreen extends RadialMenuScreen {
    private final KiStat stat;

    public StatDetailsScreen(Screen parent, KiStat stat) {
        super(Component.translatable(stat.translationKey()), parent);
        this.stat = stat;
    }

    @Override
    protected List<RadialEntry> entries(PlayerData data) {
        var values = data.statBreakdown(stat);
        String effect = switch (stat) {
            case STRENGTH -> "×" + number(data.meleeDamageMultiplier());
            case DURABILITY -> number(data.maxHp()) + " HP / " + number(data.damageReduction() * 100) + "%";
            case MAX_KI -> number(data.maxKi());
            case KI_CONTROL -> number(ResourceCosts.basicKiBlastCost(data));
            case STAMINA -> number(data.maxStamina()) + " / ×" + number(ResourceCosts.staminaCostMultiplier(data));
        };
        return List.of(
                entry("trained", number(values.trainedLevel())),
                entry("race_bonus", "+" + number(values.racialBonus())),
                entry("base_value", number(values.baseValue())),
                entry("form_factor", "×" + number(values.formMultiplier())),
                entry("xp", number(data.statXp(stat)) + " / " + number(data.statXpRequired(stat))),
                new RadialEntry("effect", Component.translatable("screen.kicraft.stats.effect." + stat.name().toLowerCase(Locale.ROOT)), Component.literal(effect))
        );
    }

    @Override
    protected List<Component> coreLines(PlayerData data) {
        return List.of(Component.translatable(data.weakestStats().contains(stat)
                ? "screen.kicraft.stats.weakest" : "screen.kicraft.stats.form_hint"));
    }

    private static RadialEntry entry(String id, String value) {
        return new RadialEntry(id, Component.translatable("screen.kicraft.stats." + id), Component.literal(value));
    }

    private static String number(double value) {
        if (!Double.isFinite(value)) return "MAX";
        return String.format(Locale.ROOT, value >= 1000000 ? "%.2e" : "%.2f", value);
    }
}
