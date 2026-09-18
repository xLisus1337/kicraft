package net.kicraft.kicraft.client;

import net.kicraft.kicraft.core.player.PlayerData;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Placeholder for the future saga and replay archive. */
public final class StoryScreen extends RadialMenuScreen {
    public StoryScreen(Screen returnScreen) {
        super(Component.translatable("screen.kicraft.story.title"), returnScreen);
    }

    @Override
    protected List<RadialEntry> entries(PlayerData data) {
        return List.of();
    }

    @Override
    protected List<Component> coreLines(PlayerData data) {
        return List.of(Component.translatable("screen.kicraft.story.coming_soon"));
    }
}
