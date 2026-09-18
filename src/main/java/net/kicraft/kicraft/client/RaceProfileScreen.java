package net.kicraft.kicraft.client;

import net.kicraft.kicraft.core.player.KiRace;
import net.kicraft.kicraft.core.player.PlayerData;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class RaceProfileScreen extends RadialMenuScreen {
    public RaceProfileScreen(Screen returnScreen) {
        super(Component.translatable("screen.kicraft.race.title"), returnScreen);
    }

    @Override
    protected List<RadialEntry> entries(PlayerData data) {
        return List.of(
                new RadialEntry("race", Component.translatable(data.race().translationKey()), Component.literal(styleFor(data.race()))),
                new RadialEntry("lineage", Component.translatable("screen.kicraft.race.lineage"), data.isLegendarySaiyan() ? Component.translatable("screen.kicraft.race.legendary") : Component.translatable("screen.kicraft.race.standard")),
                new RadialEntry("forms", Component.translatable("screen.kicraft.race.forms"), Component.literal(Integer.toString(data.transformations().size())))
        );
    }

    @Override
    protected List<Component> coreLines(PlayerData data) {
        return List.of(Component.translatable("screen.kicraft.race.title"), Component.translatable(data.race().translationKey()));
    }

    private static String styleFor(KiRace race) {
        return switch (race) {
            case SAIYAN -> "COMBAT GROWTH";
            case HALF_SAIYAN -> "HYBRID POTENTIAL";
            case HUMAN -> "TECHNIQUE FLOW";
            case NAMEKIAN -> "TANK UTILITY";
            case FROST_DEMON -> "ENERGY SUPREMACY";
            case ANDROID -> "INFINITE REACTOR";
            case BIO_ANDROID -> "ADAPTIVE EVOLUTION";
            case MAJIN -> "REGEN MONSTER";
            default -> "UNREGISTERED";
        };
    }
}
