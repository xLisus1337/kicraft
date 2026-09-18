package net.kicraft.kicraft.core.training;

/** Starter daily training inspired by a simple disciplined training routine. */
public enum DailyTrainingTask {
    PUSH_UPS(40.0),
    SIT_UPS(40.0),
    SQUATS(40.0),
    RUN(400.0);

    private final double goal;

    DailyTrainingTask(double goal) {
        this.goal = goal;
    }

    public double goal() {
        return goal;
    }

    public String translationKey() {
        return "daily_training.kicraft." + name().toLowerCase();
    }
}
