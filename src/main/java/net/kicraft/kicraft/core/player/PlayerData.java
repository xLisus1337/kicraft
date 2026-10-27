package net.kicraft.kicraft.core.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.kicraft.kicraft.core.training.DailyTrainingTask;

import java.util.EnumSet;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public final class PlayerData {
    private static final double DEFAULT_MAX_HP = 20.0;
    private static final double DEFAULT_MAX_KI = 15.0;
    private static final double DEFAULT_MAX_STAMINA = 100.0;
    private static final double KI_PER_MAX_KI_LEVEL = 15.0;
    private static final int DEFAULT_KI_COLOR = 0x45DFFF;
    private static final int BALANCE_VERSION = 2;
    private static final int DATA_VERSION = 5;
    public static final double MAX_LEVEL = 1.0E21;
    private static final double LEGENDARY_SAIYAN_KI_CONTROL_TRAINING_MULTIPLIER = 0.40;

    private KiRace race = KiRace.NONE;
    private boolean raceChosen;
    private boolean legendarySaiyan;
    private boolean hasTail;
    private KiTransformation activeTransformation = KiTransformation.BASE;
    private double hp = DEFAULT_MAX_HP;
    private double ki = DEFAULT_MAX_KI;
    private double stamina = DEFAULT_MAX_STAMINA;
    private double rage;
    private double godKi;
    private boolean godKiUnlocked;
    private boolean dragonRadarUnlocked;
    private long dailyTrainingDay = -1L;
    private boolean dailyTrainingRewardClaimed;
    private double focus = 1.0;
    private double rageControl = 1.0;
    private double berserkControl;
    private int kiColor = DEFAULT_KI_COLOR;
    private int balanceVersion = BALANCE_VERSION;
    private int dataVersion = DATA_VERSION;
    private boolean dirty = true;

    // DataVersion 5: trained levels only. Computed racial/form bonuses are never saved here.
    private final EnumMap<KiStat, Double> stats = new EnumMap<>(KiStat.class);
    private final EnumMap<KiStat, Double> statXp = new EnumMap<>(KiStat.class);
    private final EnumMap<KiStat, Integer> pendingLevelUps = new EnumMap<>(KiStat.class);
    private final EnumMap<KiTransformation, Double> transformationMastery = new EnumMap<>(KiTransformation.class);
    private final EnumMap<DailyTrainingTask, Double> dailyTrainingProgress = new EnumMap<>(DailyTrainingTask.class);
    private final EnumSet<KiTechnique> techniques = EnumSet.noneOf(KiTechnique.class);
    private final EnumSet<KiTransformation> unlockedTransformations = EnumSet.noneOf(KiTransformation.class);

    public PlayerData() {
        for (KiStat stat : KiStat.values()) {
            stats.put(stat, 1.0);
            statXp.put(stat, 0.0);
            pendingLevelUps.put(stat, 0);
        }
        for (KiTransformation transformation : KiTransformation.values()) {
            transformationMastery.put(transformation, 0.0);
        }
        for (DailyTrainingTask task : DailyTrainingTask.values()) {
            dailyTrainingProgress.put(task, 0.0);
        }
    }

    public KiRace race() {
        return race;
    }

    public void setRace(KiRace race) {
        this.race = race == null ? KiRace.NONE : race;
        if (!this.race.canRollLegendarySaiyan()) {
            legendarySaiyan = false;
            rageControl = 1.0;
            berserkControl = 0.0;
        }
        if (this.race != KiRace.SAIYAN && this.race != KiRace.HALF_SAIYAN) {
            hasTail = false;
        }
        raceChosen = this.race != KiRace.NONE;
        clearTransformationState();
        applyRaceIdentityBaseline();
        markDirty();
    }

    public void clearRaceChoice() {
        race = KiRace.NONE;
        raceChosen = false;
        legendarySaiyan = false;
        hasTail = false;
        rageControl = 1.0;
        berserkControl = 0.0;
        clearTransformationState();
        markDirty();
    }

    public boolean isRaceChosen() {
        return raceChosen;
    }

    public boolean isLegendarySaiyan() {
        return legendarySaiyan && race == KiRace.SAIYAN;
    }

    public void setLegendarySaiyan(boolean legendarySaiyan) {
        this.legendarySaiyan = legendarySaiyan && race == KiRace.SAIYAN;
        if (this.legendarySaiyan) {
            godKi = 0.0;
            godKiUnlocked = false;
            removeNonRaceTransformations();
        }
        markDirty();
    }

    public boolean hasTail() {
        return hasTail;
    }

    public void setHasTail(boolean hasTail) {
        this.hasTail = hasTail && (race == KiRace.SAIYAN || race == KiRace.HALF_SAIYAN);
        markDirty();
    }

    public double hp() {
        return hp;
    }

    public double maxHp() {
        double durability = stat(KiStat.DURABILITY);
        double trainedDurability = Math.max(0.0, durability - 1.0);
        return DEFAULT_MAX_HP + Math.sqrt(trainedDurability) * 3.0 + trainedDurability * 0.08;
    }

    public void setHp(double hp) {
        this.hp = clamp(hp, 0.0, maxHp());
        markDirty();
    }

    public double ki() {
        return ki;
    }

    public int kiColor() {
        return kiColor;
    }

    public void setKiColor(int rgb) {
        kiColor = rgb & 0xFFFFFF;
        markDirty();
    }

    public double maxKi() {
        return Math.max(DEFAULT_MAX_KI, stat(KiStat.MAX_KI) * KI_PER_MAX_KI_LEVEL);
    }

    public void setKi(double ki) {
        this.ki = clamp(ki, 0.0, maxKi());
        markDirty();
    }

    public double stamina() {
        return stamina;
    }

    public double maxStamina() {
        // Stamina is an action meter. Its level improves efficiency, never its size.
        return DEFAULT_MAX_STAMINA;
    }

    public void setStamina(double stamina) {
        this.stamina = clamp(stamina, 0.0, maxStamina());
        markDirty();
    }

    public long dailyTrainingDay() {
        return dailyTrainingDay;
    }

    public double dailyTrainingProgress(DailyTrainingTask task) {
        return task == null ? 0.0 : dailyTrainingProgress.getOrDefault(task, 0.0);
    }

    public int completedDailyTrainingTasks() {
        int completed = 0;
        for (DailyTrainingTask task : DailyTrainingTask.values()) {
            if (dailyTrainingProgress(task) >= task.goal()) {
                completed++;
            }
        }
        return completed;
    }

    public DailyTrainingTask nextIncompleteDailyTrainingTask() {
        for (DailyTrainingTask task : DailyTrainingTask.values()) {
            if (dailyTrainingProgress(task) < task.goal()) {
                return task;
            }
        }
        return null;
    }

    public boolean hasClaimedDailyTrainingReward() {
        return dailyTrainingRewardClaimed;
    }

    public boolean prepareDailyTraining(long day) {
        if (dailyTrainingDay == day) {
            return false;
        }
        dailyTrainingDay = day;
        dailyTrainingRewardClaimed = false;
        for (DailyTrainingTask task : DailyTrainingTask.values()) {
            dailyTrainingProgress.put(task, 0.0);
        }
        markDirty();
        return true;
    }

    public boolean addDailyTrainingProgress(DailyTrainingTask task, double amount, long day) {
        if (task == null || amount <= 0.0) {
            return false;
        }
        prepareDailyTraining(day);
        if (dailyTrainingRewardClaimed) {
            return false;
        }
        double current = dailyTrainingProgress(task);
        double next = clamp(current + amount, 0.0, task.goal());
        if (Double.compare(current, next) == 0) {
            return false;
        }
        dailyTrainingProgress.put(task, next);
        markDirty();
        return true;
    }

    public boolean claimDailyTrainingReward(long day) {
        prepareDailyTraining(day);
        if (dailyTrainingRewardClaimed || completedDailyTrainingTasks() < DailyTrainingTask.values().length) {
            return false;
        }
        dailyTrainingRewardClaimed = true;
        markDirty();
        return true;
    }

    public double rage() {
        return rage;
    }

    public void setRage(double rage) {
        this.rage = clamp(rage, 0.0, 100.0);
        markDirty();
    }

    public boolean isGodKiUnlocked() {
        return godKiUnlocked;
    }

    public boolean hasDragonRadar() {
        return dragonRadarUnlocked;
    }

    public void unlockDragonRadar() {
        dragonRadarUnlocked = true;
        markDirty();
    }

    public void unlockGodKi() {
        if (isLegendarySaiyan()) {
            return;
        }
        godKiUnlocked = true;
        markDirty();
    }

    public double godKi() {
        return godKi;
    }

    public boolean setGodKi(double godKi) {
        if (isLegendarySaiyan()) {
            this.godKi = 0.0;
            godKiUnlocked = false;
            markDirty();
            return false;
        }
        this.godKi = capSpecialLevel(godKi);
        godKiUnlocked = true;
        markDirty();
        return true;
    }

    public double focus() {
        return focus;
    }

    public double rageControl() {
        return isLegendarySaiyan() ? rageControl : 0.0;
    }

    public double berserkControl() {
        return isLegendarySaiyan() ? berserkControl : 0.0;
    }

    /** Gameplay base value: trained level plus race bonus, before form-specific formulas. */
    public double stat(KiStat stat) {
        return capStatLevel(trainedStat(stat) + racialStatBonus(stat));
    }

    /** Only earned levels; race and active form never modify this value. */
    public double trainedStat(KiStat stat) {
        return stats.getOrDefault(stat, 1.0);
    }

    public double racialStatBonus(KiStat stat) {
        return racialBaseStat(stat) - 1.0;
    }

    public StatBreakdown statBreakdown(KiStat stat) {
        double form = switch (stat) {
            case STRENGTH -> activeStrengthMultiplier();
            case DURABILITY -> activeDurabilityMultiplier();
            default -> 1.0;
        };
        return new StatBreakdown(trainedStat(stat), racialStatBonus(stat), stat(stat), form);
    }

    /** All equally weakest levels. The daily selector will later apply its history tie-break. */
    public Set<KiStat> weakestStats() {
        double minimum = Double.POSITIVE_INFINITY;
        EnumSet<KiStat> weakest = EnumSet.noneOf(KiStat.class);
        for (KiStat stat : KiStat.values()) {
            int comparison = Double.compare(trainedStat(stat), minimum);
            if (comparison < 0) {
                minimum = trainedStat(stat);
                weakest.clear();
            }
            if (comparison <= 0) weakest.add(stat);
        }
        return Set.copyOf(weakest);
    }

    /** Form multipliers are converted by combat formulas, not multiplied into stored levels. */
    public record StatBreakdown(double trainedLevel, double racialBonus, double baseValue, double formMultiplier) { }

    public double meleeDamageMultiplier() {
        double tier = Math.log10(Math.max(1.0, stat(KiStat.STRENGTH)));
        return (1.0 + Math.pow(tier, 2.70) * 0.62) * Math.sqrt(Math.max(1.0, activeStrengthMultiplier()));
    }

    public double damageReduction() {
        double statReduction = Math.log10(Math.max(1.0, stat(KiStat.DURABILITY))) * 0.075;
        double formReduction = Math.log10(Math.max(1.0, activeDurabilityMultiplier())) * 0.075;
        return Math.min(0.72, statReduction + formReduction);
    }

    public boolean hasTechnique(KiTechnique technique) {
        return technique != null && techniques.contains(technique);
    }

    public boolean unlockTechnique(KiTechnique technique) {
        if (technique != null && techniques.add(technique)) {
            markDirty();
            return true;
        }
        return false;
    }

    public boolean lockTechnique(KiTechnique technique) {
        if (technique != null && techniques.remove(technique)) {
            markDirty();
            return true;
        }
        return false;
    }

    public void unlockAllTechniques() {
        techniques.addAll(EnumSet.allOf(KiTechnique.class));
        markDirty();
    }

    public boolean clearTechniques() {
        if (techniques.isEmpty()) {
            return false;
        }
        techniques.clear();
        markDirty();
        return true;
    }

    public Set<KiTechnique> techniques() {
        return techniques.isEmpty() ? EnumSet.noneOf(KiTechnique.class) : EnumSet.copyOf(techniques);
    }

    public KiTransformation activeTransformation() {
        return activeTransformation;
    }

    public boolean hasTransformation(KiTransformation transformation) {
        return transformation != null && (transformation == KiTransformation.BASE
                || unlockedTransformations.contains(transformation)
                || hasTail && transformation.family() == KiTransformation.TransformationFamily.OOZARU);
    }

    public boolean unlockTransformation(KiTransformation transformation) {
        if (transformation == null || transformation == KiTransformation.BASE || !transformation.menuVisible() || !transformation.belongsToRace(race, isLegendarySaiyan())) {
            return false;
        }
        if (unlockedTransformations.add(transformation)) {
            markDirty();
            return true;
        }
        return false;
    }

    public boolean lockTransformation(KiTransformation transformation) {
        if (transformation == null || transformation == KiTransformation.BASE) {
            return false;
        }
        boolean removed = unlockedTransformations.remove(transformation);
        if (activeTransformation == transformation) {
            activeTransformation = KiTransformation.BASE;
            removed = true;
        }
        if (removed) {
            markDirty();
        }
        return removed;
    }

    public void unlockAllRaceTransformations() {
        for (KiTransformation transformation : KiTransformation.values()) {
            if (transformation.menuVisible() && transformation.belongsToRace(race, isLegendarySaiyan())) {
                unlockedTransformations.add(transformation);
            }
        }
        markDirty();
    }

    public boolean activateTransformation(KiTransformation transformation) {
        KiTransformation next = transformation == null ? KiTransformation.BASE : transformation;
        if (next != KiTransformation.BASE && (!hasTransformation(next) || !next.allowedFor(this))) {
            return false;
        }
        activeTransformation = next;
        markDirty();
        return true;
    }

    public Set<KiTransformation> transformations() {
        return unlockedTransformations.isEmpty() ? EnumSet.noneOf(KiTransformation.class) : EnumSet.copyOf(unlockedTransformations);
    }

    public double transformationMastery(KiTransformation transformation) {
        if (transformation == null || !transformation.hasMastery()) {
            return 0.0;
        }
        return transformationMastery.getOrDefault(transformation, 0.0);
    }

    public void setTransformationMastery(KiTransformation transformation, double value) {
        if (transformation == null || !transformation.hasMastery()) {
            return;
        }
        transformationMastery.put(transformation, clamp(value, 0.0, 100.0));
        markDirty();
    }

    public void trainTransformation(KiTransformation transformation, double amount) {
        if (transformation == null || !transformation.hasMastery() || amount <= 0.0) {
            return;
        }
        double current = transformationMastery(transformation);
        if (current >= 100.0) {
            return;
        }
        double progress = current / 100.0;
        double formDifficulty = 1.0 + Math.log10(Math.max(1.0, transformation.strengthMultiplier(current))) * 2.35;
        double lateMasteryDifficulty = 1.0 + Math.pow(progress, 2.8) * 85.0;
        double nearPerfectDifficulty = current < 95.0 ? 1.0 : 1.0 + Math.pow((current - 95.0) / 5.0, 2.0) * 140.0;
        double gain = Math.min(0.035, amount / (formDifficulty * lateMasteryDifficulty * nearPerfectDifficulty));
        transformationMastery.put(transformation, clamp(current + gain, 0.0, 100.0));
        markDirty();
    }

    public double activeStrengthMultiplier() {
        return activeTransformation.strengthMultiplier(transformationMastery(activeTransformation));
    }

    public double activeDurabilityMultiplier() {
        return activeTransformation.durabilityMultiplier(transformationMastery(activeTransformation));
    }

    public double activeMovementMultiplier() {
        return activeTransformation.movementMultiplier(transformationMastery(activeTransformation));
    }

    public String techniqueNames() {
        if (techniques.isEmpty()) {
            return "None";
        }
        StringJoiner joiner = new StringJoiner(", ");
        for (KiTechnique technique : techniques) {
            joiner.add(technique.displayName());
        }
        return joiner.toString();
    }

    /** Sets the trained level, without race or form bonuses. */
    public void setStat(KiStat stat, double value) {
        boolean hpWasFull = isHpFull();
        boolean kiWasFull = isKiFull();
        boolean staminaWasFull = isStaminaFull();
        double safeValue = capStatLevel(value);
        stats.put(stat, safeValue);
        statXp.put(stat, 0.0);
        pendingLevelUps.put(stat, 0);
        preserveFullResources(hpWasFull, kiWasFull, staminaWasFull);
        markDirty();
    }

    public void setAllStats(double value) {
        for (KiStat stat : KiStat.values()) {
            setStat(stat, value);
        }
        markDirty();
    }

    public void initializeRaceStats(RandomSource random) {
        if (race == KiRace.NONE) {
            return;
        }

        resetTrainableStats();
        clearTransformationState();
        applyRaceIdentityBaseline();

        if (race == KiRace.SAIYAN) {
            hasTail = true;
        } else if (race == KiRace.HALF_SAIYAN) {
            hasTail = random != null && random.nextDouble() < 0.50;
        } else {
            hasTail = false;
        }

        hp = maxHp();
        ki = maxKi();
        stamina = maxStamina();
        markDirty();
    }

    public double statXp(KiStat stat) {
        return statXp.getOrDefault(stat, 0.0);
    }

    public double statXpRequired(KiStat stat) {
        double value = Math.max(1.0, trainedStat(stat));
        if (value >= MAX_LEVEL) {
            return Double.POSITIVE_INFINITY;
        }
        // Starter progression must react to normal combat, training and travel.
        // The curve still becomes demanding at high levels instead of flattening out.
        return 15.0 + Math.pow(value, 1.16) * 5.0;
    }

    /** Returns the XP actually credited after the race's training multiplier. */
    public double train(KiStat stat, double amount) {
        if (amount <= 0.0) {
            return 0.0;
        }
        if (trainedStat(stat) >= MAX_LEVEL) {
            statXp.put(stat, 0.0);
            return 0.0;
        }
        boolean hpWasFull = isHpFull();
        boolean kiWasFull = isKiFull();
        boolean staminaWasFull = isStaminaFull();
        double raceMultiplier = trainingMultiplier(stat);
        double creditedXp = amount * raceMultiplier;
        statXp.put(stat, statXp(stat) + creditedXp);
        int gainedLevels = processTraining(stat);
        if (gainedLevels > 0) {
            pendingLevelUps.merge(stat, gainedLevels, Integer::sum);
            preserveFullResources(hpWasFull, kiWasFull, staminaWasFull);
        }
        markDirty();
        return creditedXp;
    }

    /**
     * Returns the final levels that should be shown to the owning client.
     * These notices are intentionally transient: they are not character data
     * and must not reappear after a relog or a respawn.
     */
    public Map<KiStat, Double> consumeLevelUpNotifications() {
        EnumMap<KiStat, Double> notifications = new EnumMap<>(KiStat.class);
        for (KiStat stat : KiStat.values()) {
            if (pendingLevelUps.getOrDefault(stat, 0) > 0) {
                notifications.put(stat, trainedStat(stat));
                pendingLevelUps.put(stat, 0);
            }
        }
        return notifications;
    }

    public void trainFocus(double amount) {
        if (amount <= 0.0) return;
        focus = capStatLevel(focus + amount / (150.0 + Math.pow(focus, 1.18) * 24.0));
        markDirty();
    }

    public void trainRageControl(double amount) {
        if (!isLegendarySaiyan() || amount <= 0.0) return;
        rageControl = capStatLevel(rageControl + amount / (180.0 + Math.pow(rageControl, 1.22) * 30.0));
        markDirty();
    }

    public void trainBerserkControl(double amount) {
        if (!isLegendarySaiyan() || amount <= 0.0) return;
        berserkControl = capSpecialLevel(berserkControl + amount / (220.0 + Math.pow(berserkControl + 1.0, 1.25) * 36.0));
        markDirty();
    }

    public double powerLevel() {
        double combatBody = Math.max(0.0, stat(KiStat.STRENGTH) - racialBaseStat(KiStat.STRENGTH)) * 0.55
                + Math.max(0.0, stat(KiStat.DURABILITY) - racialBaseStat(KiStat.DURABILITY)) * 0.45;
        double energy = Math.max(0.0, stat(KiStat.MAX_KI) - racialBaseStat(KiStat.MAX_KI))
                + Math.max(0.0, stat(KiStat.STAMINA) - racialBaseStat(KiStat.STAMINA))
                + Math.max(0.0, stat(KiStat.KI_CONTROL) - racialBaseStat(KiStat.KI_CONTROL)) * 0.35;
        double hiddenControl = Math.max(0.0, focus - racialBaseFocus()) * 0.20
                + (isLegendarySaiyan() ? Math.max(0.0, rageControl - 1.0) * 0.20 + berserkControl * 0.25 : 0.0);
        double basePower = Math.max(1.0, 1.0 + combatBody + energy + hiddenControl);
        double formPower = activeTransformation.isBase()
                ? 1.0
                : Math.sqrt(activeStrengthMultiplier() * activeDurabilityMultiplier());
        return Math.min(MAX_LEVEL, basePower * formPower);
    }

    public boolean isDirty() {
        return dirty;
    }

    public void clearDirty() {
        dirty = false;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Race", race.name());
        tag.putBoolean("RaceChosen", raceChosen);
        tag.putBoolean("LegendarySaiyan", legendarySaiyan);
        tag.putBoolean("HasTail", hasTail);
        tag.putString("ActiveTransformation", activeTransformation.name());
        tag.putInt("DataVersion", dataVersion);
        tag.putDouble("Hp", hp);
        tag.putDouble("Ki", ki);
        tag.putDouble("Stamina", stamina);
        tag.putDouble("Rage", rage);
        tag.putDouble("GodKi", godKi);
        tag.putBoolean("GodKiUnlocked", godKiUnlocked);
        tag.putBoolean("DragonRadarUnlocked", dragonRadarUnlocked);
        tag.putLong("DailyTrainingDay", dailyTrainingDay);
        tag.putBoolean("DailyTrainingRewardClaimed", dailyTrainingRewardClaimed);
        tag.putDouble("Focus", focus);
        tag.putDouble("RageControl", rageControl);
        tag.putDouble("BerserkControl", berserkControl);
        tag.putInt("KiColor", kiColor);
        tag.putInt("BalanceVersion", BALANCE_VERSION);

        CompoundTag statsTag = new CompoundTag();
        CompoundTag xpTag = new CompoundTag();
        for (Map.Entry<KiStat, Double> entry : stats.entrySet()) {
            statsTag.putDouble(entry.getKey().name(), entry.getValue());
            xpTag.putDouble(entry.getKey().name(), statXp(entry.getKey()));
        }
        tag.put("Stats", statsTag);
        tag.put("StatXp", xpTag);

        CompoundTag dailyTrainingTag = new CompoundTag();
        for (DailyTrainingTask task : DailyTrainingTask.values()) {
            double progress = dailyTrainingProgress(task);
            if (progress > 0.0) {
                dailyTrainingTag.putDouble(task.name(), progress);
            }
        }
        tag.put("DailyTrainingProgress", dailyTrainingTag);

        CompoundTag techniquesTag = new CompoundTag();
        for (KiTechnique technique : techniques) {
            techniquesTag.putBoolean(technique.name(), true);
        }
        tag.put("Techniques", techniquesTag);

        CompoundTag transformationsTag = new CompoundTag();
        CompoundTag masteryTag = new CompoundTag();
        for (KiTransformation transformation : unlockedTransformations) {
            transformationsTag.putBoolean(transformation.name(), true);
        }
        for (KiTransformation transformation : KiTransformation.values()) {
            if (!transformation.hasMastery()) {
                continue;
            }
            double mastery = transformationMastery(transformation);
            if (mastery > 0.0) {
                masteryTag.putDouble(transformation.name(), mastery);
            }
        }
        tag.put("Transformations", transformationsTag);
        tag.put("TransformationMastery", masteryTag);
        return tag;
    }

    public void load(CompoundTag tag) {
        resetForLoad();
        int storedDataVersion = tag.contains("DataVersion") ? tag.getInt("DataVersion") : 0;
        race = tag.contains("Race") ? KiRace.byName(tag.getString("Race")) : KiRace.NONE;
        // A non-empty race is always a completed character creation state.
        raceChosen = race != KiRace.NONE;
        legendarySaiyan = tag.getBoolean("LegendarySaiyan") && race == KiRace.SAIYAN;
        hasTail = tag.getBoolean("HasTail") && (race == KiRace.SAIYAN || race == KiRace.HALF_SAIYAN);
        activeTransformation = KiTransformation.byName(tag.getString("ActiveTransformation"));
        if (activeTransformation == null) {
            activeTransformation = KiTransformation.BASE;
        }
        rage = clamp(tag.getDouble("Rage"), 0.0, 100.0);
        godKi = capSpecialLevel(tag.getDouble("GodKi"));
        godKiUnlocked = tag.getBoolean("GodKiUnlocked");
        dragonRadarUnlocked = tag.getBoolean("DragonRadarUnlocked");
        dailyTrainingDay = tag.contains("DailyTrainingDay") ? tag.getLong("DailyTrainingDay") : -1L;
        dailyTrainingRewardClaimed = tag.getBoolean("DailyTrainingRewardClaimed");
        if (legendarySaiyan) {
            godKi = 0.0;
            godKiUnlocked = false;
        }
        focus = capStatLevel(tag.getDouble("Focus"));
        rageControl = capStatLevel(tag.getDouble("RageControl"));
        berserkControl = capSpecialLevel(tag.getDouble("BerserkControl"));
        kiColor = tag.contains("KiColor") ? tag.getInt("KiColor") & 0xFFFFFF : DEFAULT_KI_COLOR;
        balanceVersion = tag.contains("BalanceVersion") ? tag.getInt("BalanceVersion") : 0;

        CompoundTag statsTag = tag.getCompound("Stats");
        CompoundTag xpTag = tag.getCompound("StatXp");
        for (KiStat stat : KiStat.values()) {
            if (statsTag.contains(stat.name())) {
                stats.put(stat, capStatLevel(statsTag.getDouble(stat.name())));
            }
            if (xpTag.contains(stat.name())) {
                statXp.put(stat, clamp(xpTag.getDouble(stat.name()), 0.0, Double.MAX_VALUE));
            }
        }
        CompoundTag dailyTrainingTag = tag.getCompound("DailyTrainingProgress");
        for (DailyTrainingTask task : DailyTrainingTask.values()) {
            dailyTrainingProgress.put(task, dailyTrainingTag.contains(task.name())
                    ? clamp(dailyTrainingTag.getDouble(task.name()), 0.0, task.goal())
                    : 0.0);
        }
        migrateStarterBalanceIfNeeded();
        // Version 4 stored the additive racial starting bonus inside Stats.
        // Remove it once; XP and all unrelated character progress stay intact.
        if (storedDataVersion < 5) {
            for (KiStat stat : KiStat.values()) {
                stats.put(stat, capStatLevel(trainedStat(stat) - racialStatBonus(stat)));
            }
        }
        migrateDataIfNeeded(storedDataVersion, tag);
        techniques.clear();
        CompoundTag techniquesTag = tag.getCompound("Techniques");
        for (KiTechnique technique : KiTechnique.values()) {
            if (techniquesTag.getBoolean(technique.name())) {
                techniques.add(technique);
            }
        }
        unlockedTransformations.clear();
        CompoundTag transformationsTag = tag.getCompound("Transformations");
        for (KiTransformation transformation : KiTransformation.values()) {
            if (transformation.menuVisible() && transformationsTag.getBoolean(transformation.name()) && transformation.belongsToRace(race, isLegendarySaiyan())) {
                unlockedTransformations.add(transformation);
            }
        }
        CompoundTag masteryTag = tag.getCompound("TransformationMastery");
        for (KiTransformation transformation : KiTransformation.values()) {
            transformationMastery.put(transformation, transformation.hasMastery() && masteryTag.contains(transformation.name()) ? clamp(masteryTag.getDouble(transformation.name()), 0.0, 100.0) : 0.0);
        }
        if (activeTransformation != KiTransformation.BASE && (!hasTransformation(activeTransformation) || !activeTransformation.allowedFor(this))) {
            activeTransformation = KiTransformation.BASE;
        }
        hp = clamp(tag.contains("Hp") ? tag.getDouble("Hp") : maxHp(), 0.0, maxHp());
        ki = clamp(tag.contains("Ki") ? tag.getDouble("Ki") : maxKi(), 0.0, maxKi());
        stamina = clamp(tag.contains("Stamina") ? tag.getDouble("Stamina") : maxStamina(), 0.0, maxStamina());
        validateAndRepair();
        markDirty();
    }

    private void migrateStarterBalanceIfNeeded() {
        if (balanceVersion >= BALANCE_VERSION) {
            return;
        }

        switch (race) {
            case SAIYAN -> {
                capOldStarterStat(KiStat.STRENGTH, 3.0, 1.15);
                capOldStarterStat(KiStat.STAMINA, 115.0, 110.0);
                capOldStarterStat(KiStat.KI_CONTROL, 1.0, 1.0);
            }
            case HALF_SAIYAN -> {
                capOldStarterStat(KiStat.STRENGTH, 2.0, 1.08);
                capOldStarterStat(KiStat.KI_CONTROL, 2.0, 1.12);
                capOldStarterStat(KiStat.STAMINA, 110.0, 108.0);
            }
            case HUMAN -> {
                capOldStarterStat(KiStat.KI_CONTROL, 3.0, 1.18);
                if (focus <= 3.0) {
                    focus = 1.35;
                }
            }
            case NAMEKIAN -> {
                capOldStarterStat(KiStat.DURABILITY, 4.0, 1.25);
                capOldStarterStat(KiStat.MAX_KI, 120.0, 112.0);
                capOldStarterStat(KiStat.KI_CONTROL, 2.0, 1.12);
            }
            case FROST_DEMON -> {
                capOldStarterStat(KiStat.STRENGTH, 3.0, 1.12);
                capOldStarterStat(KiStat.MAX_KI, 130.0, 118.0);
                capOldStarterStat(KiStat.KI_CONTROL, 2.0, 1.12);
            }
            case ANDROID -> {
                capOldStarterStat(KiStat.STRENGTH, 2.0, 1.08);
                capOldStarterStat(KiStat.DURABILITY, 3.0, 1.15);
                capOldStarterStat(KiStat.STAMINA, 100.0, 100.0);
                capOldStarterStat(KiStat.MAX_KI, 100.0, 100.0);
            }
            case BIO_ANDROID -> {
                capOldStarterStat(KiStat.STRENGTH, 2.0, 1.08);
                capOldStarterStat(KiStat.DURABILITY, 2.0, 1.10);
                capOldStarterStat(KiStat.MAX_KI, 120.0, 110.0);
            }
            case MAJIN -> {
                capOldStarterStat(KiStat.DURABILITY, 4.0, 1.25);
                capOldStarterStat(KiStat.STAMINA, 120.0, 112.0);
                capOldStarterStat(KiStat.KI_CONTROL, 1.0, 1.0);
            }
            default -> {
            }
        }
        if (balanceVersion < 2) {
            convertLegacyResourceLevels();
        }
        balanceVersion = BALANCE_VERSION;
        markDirty();
    }

    /**
     * Keeps serialized player data forward-compatible. Each migration is
     * idempotent, so reopening an older character can never erase valid
     * progress by applying a conversion twice.
     */
    private void migrateDataIfNeeded(int storedDataVersion, CompoundTag tag) {
        int version = Math.max(0, storedDataVersion);
        if (version < 1) {
            raceChosen = race != KiRace.NONE;
        }
        if (version < 2) {
            // Global Level/Exp belonged to the first prototype. Progress now
            // lives exclusively in per-stat levels and XP.
            tag.remove("Level");
            tag.remove("Exp");
        }
        if (version < 3) {
            // Daily training did not exist in the oldest saves. A reward
            // without a valid training day must never be claimable.
            if (dailyTrainingDay < 0L) {
                dailyTrainingDay = -1L;
                dailyTrainingRewardClaimed = false;
            }
        }
        if (version < 4) {
            tag.remove("HairStyle");
        }
        dataVersion = DATA_VERSION;
    }

    /**
     * Repairs values which may come from an older, partial or manually edited
     * player save. This is intentionally idempotent so it can run on every
     * login, respawn and dimension transition without changing valid data.
     *
     * @return true when any stored value had to be repaired
     */
    public boolean validateAndRepair() {
        boolean changed = false;

        if (race == null) {
            race = KiRace.NONE;
            changed = true;
        }
        boolean shouldHaveRaceChoice = race != KiRace.NONE;
        if (raceChosen != shouldHaveRaceChoice) {
            raceChosen = shouldHaveRaceChoice;
            changed = true;
        }
        if (legendarySaiyan && race != KiRace.SAIYAN) {
            legendarySaiyan = false;
            changed = true;
        }
        if (hasTail && race != KiRace.SAIYAN && race != KiRace.HALF_SAIYAN) {
            hasTail = false;
            changed = true;
        }
        if (legendarySaiyan && (godKi != 0.0 || godKiUnlocked)) {
            godKi = 0.0;
            godKiUnlocked = false;
            changed = true;
        }

        for (KiStat stat : KiStat.values()) {
            double safeLevel = capStatLevel(trainedStat(stat));
            if (Double.compare(safeLevel, trainedStat(stat)) != 0) {
                stats.put(stat, safeLevel);
                changed = true;
            }
            double safeXp = clamp(statXp(stat), 0.0, Double.MAX_VALUE);
            if (Double.compare(safeXp, statXp(stat)) != 0) {
                statXp.put(stat, safeXp);
                changed = true;
            }
        }

        double safeFocus = capStatLevel(focus);
        double safeRageControl = capStatLevel(rageControl);
        double safeBerserkControl = capSpecialLevel(berserkControl);
        double safeRage = clamp(rage, 0.0, 100.0);
        double safeGodKi = capSpecialLevel(godKi);
        if (Double.compare(focus, safeFocus) != 0) {
            focus = safeFocus;
            changed = true;
        }
        if (Double.compare(rageControl, safeRageControl) != 0) {
            rageControl = safeRageControl;
            changed = true;
        }
        if (Double.compare(berserkControl, safeBerserkControl) != 0) {
            berserkControl = safeBerserkControl;
            changed = true;
        }
        if (Double.compare(rage, safeRage) != 0) {
            rage = safeRage;
            changed = true;
        }
        if (Double.compare(godKi, safeGodKi) != 0) {
            godKi = safeGodKi;
            changed = true;
        }

        if (dailyTrainingDay < -1L) {
            dailyTrainingDay = -1L;
            changed = true;
        }
        if (dailyTrainingDay < 0L && dailyTrainingRewardClaimed) {
            dailyTrainingRewardClaimed = false;
            changed = true;
        }
        for (DailyTrainingTask task : DailyTrainingTask.values()) {
            double currentProgress = dailyTrainingProgress.getOrDefault(task, 0.0);
            double safeProgress = clamp(currentProgress, 0.0, task.goal());
            if (Double.compare(currentProgress, safeProgress) != 0) {
                dailyTrainingProgress.put(task, safeProgress);
                changed = true;
            }
        }

        if (race == KiRace.NONE) {
            if (hasTail || legendarySaiyan || activeTransformation != KiTransformation.BASE || !unlockedTransformations.isEmpty()) {
                hasTail = false;
                legendarySaiyan = false;
                clearTransformationState();
                changed = true;
            }
        } else if (enforceRaceIdentityBaseline()) {
            changed = true;
        }

        if (unlockedTransformations.removeIf(transformation -> !transformation.menuVisible()
                || !transformation.belongsToRace(race, isLegendarySaiyan()))) {
            changed = true;
        }
        for (KiTransformation transformation : KiTransformation.values()) {
            double currentMastery = transformationMastery.getOrDefault(transformation, 0.0);
            double safeMastery = transformation.hasMastery() ? clamp(currentMastery, 0.0, 100.0) : 0.0;
            if (Double.compare(currentMastery, safeMastery) != 0) {
                transformationMastery.put(transformation, safeMastery);
                changed = true;
            }
        }
        if (activeTransformation == null || (activeTransformation != KiTransformation.BASE
                && (!hasTransformation(activeTransformation) || !activeTransformation.allowedFor(this)))) {
            activeTransformation = KiTransformation.BASE;
            changed = true;
        }

        double safeHp = clamp(hp, 0.0, maxHp());
        double safeKi = clamp(ki, 0.0, maxKi());
        double safeStamina = clamp(stamina, 0.0, maxStamina());
        if (Double.compare(hp, safeHp) != 0) {
            hp = safeHp;
            changed = true;
        }
        if (Double.compare(ki, safeKi) != 0) {
            ki = safeKi;
            changed = true;
        }
        if (Double.compare(stamina, safeStamina) != 0) {
            stamina = safeStamina;
            changed = true;
        }
        if (dataVersion != DATA_VERSION) {
            dataVersion = DATA_VERSION;
            changed = true;
        }
        if (balanceVersion != BALANCE_VERSION) {
            balanceVersion = BALANCE_VERSION;
            changed = true;
        }
        if (changed) {
            markDirty();
        }
        return changed;
    }

    private void convertLegacyResourceLevels() {
        stats.put(KiStat.MAX_KI, capStatLevel(trainedStat(KiStat.MAX_KI) / 100.0));
        stats.put(KiStat.STAMINA, capStatLevel(trainedStat(KiStat.STAMINA) / 100.0));
        statXp.put(KiStat.MAX_KI, 0.0);
        statXp.put(KiStat.STAMINA, 0.0);
    }

    private void capOldStarterStat(KiStat stat, double oldStarterMaximum, double newStarterValue) {
        if (trainedStat(stat) <= oldStarterMaximum) {
            stats.put(stat, capStatLevel(newStarterValue));
            statXp.put(stat, 0.0);
        }
    }

    private int processTraining(KiStat stat) {
        int gainedLevels = 0;
        int guard = 0;
        while (trainedStat(stat) < MAX_LEVEL && statXp(stat) >= statXpRequired(stat) && guard++ < 1000) {
            statXp.put(stat, statXp(stat) - statXpRequired(stat));
            stats.put(stat, Math.min(MAX_LEVEL, trainedStat(stat) + 1.0));
            gainedLevels++;
        }
        if (trainedStat(stat) >= MAX_LEVEL) {
            statXp.put(stat, 0.0);
        }
        return gainedLevels;
    }

    private double trainingMultiplier(KiStat stat) {
        if (isLegendarySaiyan() && stat == KiStat.KI_CONTROL) {
            return LEGENDARY_SAIYAN_KI_CONTROL_TRAINING_MULTIPLIER;
        }

        double racialMultiplier = switch (race) {
            case SAIYAN -> switch (stat) {
                case STRENGTH, STAMINA -> 1.22;
                case DURABILITY -> 1.08;
                case KI_CONTROL -> 0.78;
                default -> 0.98;
            };
            case HALF_SAIYAN -> switch (stat) {
                case KI_CONTROL -> 1.24;
                default -> 1.14;
            };
            case HUMAN -> switch (stat) {
                case KI_CONTROL -> 1.28;
                case MAX_KI -> 1.10;
                case STRENGTH -> 0.92;
                default -> 1.0;
            };
            case NAMEKIAN -> switch (stat) {
                case DURABILITY -> 1.30;
                case MAX_KI, KI_CONTROL -> 1.18;
                case STRENGTH -> 0.88;
                default -> 1.0;
            };
            case FROST_DEMON -> switch (stat) {
                case MAX_KI, KI_CONTROL -> 1.18;
                case STRENGTH -> 1.12;
                case STAMINA -> 0.88;
                default -> 0.94;
            };
            case ANDROID -> switch (stat) {
                case STAMINA, MAX_KI, KI_CONTROL -> 0.45;
                case DURABILITY, STRENGTH -> 1.10;
                default -> 1.0;
            };
            case BIO_ANDROID -> switch (stat) {
                case DURABILITY, MAX_KI, STRENGTH -> 1.12;
                case KI_CONTROL -> 1.02;
                default -> 1.06;
            };
            case MAJIN -> switch (stat) {
                case DURABILITY, STAMINA -> 1.26;
                case KI_CONTROL -> 0.82;
                default -> 0.98;
            };
            default -> 1.0;
        };
        return racialMultiplier * (isLegendarySaiyan() ? 1.065 : 1.0);
    }

    private void resetTrainableStats() {
        for (KiStat stat : KiStat.values()) {
            stats.put(stat, 1.0);
            statXp.put(stat, 0.0);
            pendingLevelUps.put(stat, 0);
        }
        focus = 1.0;
        rageControl = 1.0;
        berserkControl = 0.0;
    }

    private void resetForLoad() {
        race = KiRace.NONE;
        raceChosen = false;
        legendarySaiyan = false;
        hasTail = false;
        activeTransformation = KiTransformation.BASE;
        hp = DEFAULT_MAX_HP;
        ki = DEFAULT_MAX_KI;
        stamina = DEFAULT_MAX_STAMINA;
        rage = 0.0;
        godKi = 0.0;
        godKiUnlocked = false;
        dragonRadarUnlocked = false;
        dailyTrainingDay = -1L;
        dailyTrainingRewardClaimed = false;
        focus = 1.0;
        rageControl = 1.0;
        berserkControl = 0.0;
        kiColor = DEFAULT_KI_COLOR;
        balanceVersion = BALANCE_VERSION;
        dataVersion = DATA_VERSION;
        techniques.clear();
        unlockedTransformations.clear();
        for (KiStat stat : KiStat.values()) {
            stats.put(stat, 1.0);
            statXp.put(stat, 0.0);
            pendingLevelUps.put(stat, 0);
        }
        for (KiTransformation transformation : KiTransformation.values()) {
            transformationMastery.put(transformation, 0.0);
        }
        for (DailyTrainingTask task : DailyTrainingTask.values()) {
            dailyTrainingProgress.put(task, 0.0);
        }
    }

    private double racialBaseStat(KiStat stat) {
        return switch (race) {
            case SAIYAN -> switch (stat) {
                case STRENGTH -> 1.15;
                case STAMINA -> 1.10;
                default -> 1.0;
            };
            case HALF_SAIYAN -> switch (stat) {
                case STRENGTH, STAMINA -> 1.08;
                case KI_CONTROL -> 1.12;
                default -> 1.0;
            };
            case HUMAN -> stat == KiStat.KI_CONTROL ? 1.18 : 1.0;
            case NAMEKIAN -> switch (stat) {
                case DURABILITY -> 1.25;
                case MAX_KI, KI_CONTROL -> 1.12;
                default -> 1.0;
            };
            case FROST_DEMON -> switch (stat) {
                case STRENGTH -> 1.12;
                case MAX_KI -> 1.18;
                case KI_CONTROL -> 1.12;
                default -> 1.0;
            };
            case ANDROID -> switch (stat) {
                case STRENGTH -> 1.08;
                case DURABILITY -> 1.15;
                default -> 1.0;
            };
            case BIO_ANDROID -> switch (stat) {
                case STRENGTH -> 1.08;
                case DURABILITY, MAX_KI -> 1.10;
                default -> 1.0;
            };
            case MAJIN -> switch (stat) {
                case DURABILITY -> 1.25;
                case STAMINA -> 1.12;
                default -> 1.0;
            };
            default -> 1.0;
        };
    }

    private double racialBaseFocus() {
        return race == KiRace.HUMAN ? 1.35 : 1.0;
    }

    private void clearTransformationState() {
        activeTransformation = KiTransformation.BASE;
        unlockedTransformations.clear();
        for (KiTransformation transformation : KiTransformation.values()) {
            transformationMastery.put(transformation, 0.0);
        }
    }

    private void removeNonRaceTransformations() {
        unlockedTransformations.removeIf(transformation -> !transformation.belongsToRace(race, isLegendarySaiyan()));
        if (!activeTransformation.belongsToRace(race, isLegendarySaiyan())) {
            activeTransformation = KiTransformation.BASE;
        }
    }

    private void applyRaceIdentityBaseline() {
        enforceRaceIdentityBaseline();
        hp = maxHp();
        ki = maxKi();
        stamina = maxStamina();
    }

    private boolean enforceRaceIdentityBaseline() {
        // The five stat bonuses are computed, never written into trained levels.
        if (race == KiRace.HUMAN && focus < 1.35) {
            focus = 1.35;
            return true;
        }
        return false;
    }

    public static double capStatLevel(double value) {
        return clamp(value, 1.0, MAX_LEVEL);
    }

    public static double capSpecialLevel(double value) {
        return clamp(value, 0.0, MAX_LEVEL);
    }

    private boolean isHpFull() {
        return maxHp() <= 0.0 || hp >= maxHp() - 0.001;
    }

    private boolean isKiFull() {
        return maxKi() <= 0.0 || ki >= maxKi() - 0.001;
    }

    private boolean isStaminaFull() {
        return maxStamina() <= 0.0 || stamina >= maxStamina() - 0.001;
    }

    private void preserveFullResources(boolean hpWasFull, boolean kiWasFull, boolean staminaWasFull) {
        hp = hpWasFull ? maxHp() : clamp(hp, 0.0, maxHp());
        ki = kiWasFull ? maxKi() : clamp(ki, 0.0, maxKi());
        stamina = staminaWasFull ? maxStamina() : clamp(stamina, 0.0, maxStamina());
    }

    private void markDirty() {
        dirty = true;
    }

    private static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
