package net.kicraft.kicraft.client;

import net.kicraft.kicraft.core.player.KiTransformation;
import net.kicraft.kicraft.core.player.PlayerData;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TransformationScreen extends RadialMenuScreen {
    public TransformationScreen() {
        this(null);
    }

    public TransformationScreen(Screen returnScreen) {
        super(Component.translatable("screen.kicraft.transformations"), returnScreen);
    }

    @Override
    protected List<RadialEntry> entries(PlayerData data) {
        List<KiTransformation> forms = unlockedForms(data);
        List<RadialEntry> entries = new ArrayList<>();
        for (KiTransformation form : forms) {
            Component detail = data.activeTransformation() == form
                    ? Component.translatable("screen.kicraft.transformations.active_label")
                    : Component.translatable("screen.kicraft.transformations.mastery", Math.round(data.transformationMastery(form)));
            entries.add(new RadialEntry(form.name(), Component.translatable(form.translationKey()), detail));
        }
        return entries;
    }

    @Override
    protected void onEntrySelected(RadialEntry entry) {
        KiTransformation form = KiTransformation.byName(entry.id());
        if (form != null) {
            ClientInputEvents.selectTransformation(form);
            minecraft.setScreen(null);
        }
    }

    @Override
    protected List<Component> coreLines(PlayerData data) {
        return List.of(Component.translatable("screen.kicraft.transformations"), Component.translatable(data.activeTransformation().translationKey()));
    }

    private static List<KiTransformation> unlockedForms(PlayerData data) {
        List<KiTransformation> forms = new ArrayList<>(data.transformations());
        forms.removeIf(form -> !form.menuVisible() || !form.allowedFor(data));
        forms.sort(Comparator.comparingInt(KiTransformation::ordinal));
        return forms;
    }
}
