package net.kicraft.kicraft.core.training;

/** Activities available from the first-pass training mat. */
public enum TrainingActivity {
    PUSH_UPS("push_ups"),
    SIT_UPS("sit_ups"),
    SQUATS("squats"),
    MEDITATION("meditation");

    private final String id;

    TrainingActivity(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "training.kicraft." + id;
    }

    public String detailTranslationKey() {
        return translationKey() + ".detail";
    }
}
