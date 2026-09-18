package net.kicraft.kicraft.client;

import net.kicraft.kicraft.core.player.PlayerData;
import net.kicraft.kicraft.core.training.TrainingActivity;
import net.kicraft.kicraft.core.training.DailyTrainingTask;
import net.kicraft.kicraft.network.KicraftNetwork;
import net.kicraft.kicraft.network.TrainingActionPacket;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** First-pass training selector opened through M. */
public final class TrainingScreen extends RadialMenuScreen {
    private static final String DUMMY_ID = "training_dummy";

    public TrainingScreen() {
        super(Component.translatable("screen.kicraft.training"), null);
    }

    @Override
    protected List<RadialEntry> entries(PlayerData data) {
        List<RadialEntry> entries = new ArrayList<>();
        entries.add(new RadialEntry(DUMMY_ID,
                Component.translatable("screen.kicraft.training.dummy"),
                Component.translatable("screen.kicraft.training.dummy.detail")));
        for (TrainingActivity activity : TrainingActivity.values()) {
            DailyTrainingTask dailyTask = dailyTask(activity);
            Component label = Component.translatable(activity.translationKey());
            if (dailyTask != null) {
                label = label.copy().append(Component.literal(" " + (int) Math.floor(data.dailyTrainingProgress(dailyTask))
                        + "/" + (int) dailyTask.goal()));
            }
            entries.add(new RadialEntry(activity.id(),
                    label,
                    Component.translatable(activity.detailTranslationKey())));
        }
        return entries;
    }

    @Override
    protected void onEntrySelected(RadialEntry entry) {
        if (DUMMY_ID.equals(entry.id())) {
            KicraftNetwork.sendToServer(new TrainingActionPacket(TrainingActionPacket.Action.SPAWN_DUMMY));
        } else {
            for (TrainingActivity activity : TrainingActivity.values()) {
                if (activity.id().equals(entry.id())) {
                    KicraftNetwork.sendToServer(new TrainingActionPacket(TrainingActionPacket.Action.START, activity));
                    break;
                }
            }
        }
        minecraft.setScreen(null);
    }

    @Override
    protected List<Component> coreLines(PlayerData data) {
        if (data.hasClaimedDailyTrainingReward()) {
            return List.of(Component.translatable("daily_training.kicraft.complete"));
        }
        DailyTrainingTask nextTask = data.nextIncompleteDailyTrainingTask();
        if (nextTask == null) {
            return List.of(Component.translatable("daily_training.kicraft.progress",
                    data.completedDailyTrainingTasks(), DailyTrainingTask.values().length));
        }
        return List.of(Component.translatable("daily_training.kicraft.next",
                data.completedDailyTrainingTasks(), DailyTrainingTask.values().length,
                Component.translatable(nextTask.translationKey()),
                (int) Math.floor(data.dailyTrainingProgress(nextTask)),
                (int) nextTask.goal()));
    }

    private static DailyTrainingTask dailyTask(TrainingActivity activity) {
        return switch (activity) {
            case PUSH_UPS -> DailyTrainingTask.PUSH_UPS;
            case SIT_UPS -> DailyTrainingTask.SIT_UPS;
            case SQUATS -> DailyTrainingTask.SQUATS;
            case MEDITATION -> null;
        };
    }
}
