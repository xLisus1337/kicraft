package net.kicraft.kicraft.client;

import net.kicraft.kicraft.core.player.KiTechnique;
import net.kicraft.kicraft.core.player.PlayerData;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TechniqueLibraryScreen extends RadialMenuScreen {
    public TechniqueLibraryScreen(Screen returnScreen) {
        super(Component.translatable("screen.kicraft.techniques.title"), returnScreen);
    }

    @Override
    protected List<RadialEntry> entries(PlayerData data) {
        List<KiTechnique> techniques = new ArrayList<>(data.techniques());
        techniques.sort(Comparator.comparingInt(Enum::ordinal));
        List<RadialEntry> entries = new ArrayList<>();
        for (KiTechnique technique : techniques) {
            entries.add(new RadialEntry(technique.name(), Component.translatable(technique.translationKey()), Component.translatable("screen.kicraft.techniques.unlocked")));
        }
        return entries;
    }

    @Override
    protected List<Component> coreLines(PlayerData data) {
        return List.of(Component.translatable("screen.kicraft.techniques.title"), Component.literal(Integer.toString(data.techniques().size())));
    }
}
