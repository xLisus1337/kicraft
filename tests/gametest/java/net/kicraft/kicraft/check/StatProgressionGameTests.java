package net.kicraft.kicraft.check;

import net.kicraft.kicraft.core.player.*;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.Set;

@GameTestHolder("kicraft")
@PrefixGameTestTemplate(false)
public final class StatProgressionGameTests {
    @GameTest(template = "melee_arena")
    public static void raceBonusesNeverCountAsTrainedLevels(GameTestHelper h) {
        for (KiRace race : KiRace.values()) {
            PlayerData data = new PlayerData();
            data.setRace(race);
            for (KiStat stat : KiStat.values()) {
                h.assertTrue(data.trainedStat(stat) == 1, "Starting trained level must be one for " + race);
                close(h, data.stat(stat), 1 + data.racialStatBonus(stat), "Race bonus must remain separate");
            }
            h.assertTrue(data.weakestStats().size() == 5, "All untrained stats must tie regardless of race");
            data.validateAndRepair();
            h.assertTrue(data.weakestStats().size() == 5, "Validation must never bake bonuses into levels");
        }
        h.succeed();
    }

    @GameTest(template = "melee_arena")
    public static void versionFourMigrationPreservesProgressAndResources(GameTestHelper h) {
        for (KiRace race : KiRace.values()) {
            PlayerData original = new PlayerData(); original.setRace(race);
            original.setAllStats(12); original.unlockAllTechniques();
            original.prepareDailyTraining(123); original.unlockDragonRadar();
            CompoundTag legacy = original.save();
            legacy.putInt("DataVersion", 4);
            for (KiStat stat : KiStat.values()) {
                legacy.getCompound("Stats").putDouble(stat.name(), original.stat(stat));
                legacy.getCompound("StatXp").putDouble(stat.name(), 7.25);
            }
            CompoundTag before = legacy.copy();
            PlayerData migrated = new PlayerData(); migrated.load(legacy);
            for (KiStat stat : KiStat.values()) {
                close(h, migrated.trainedStat(stat), 12, "Migration must remove only the racial bonus");
                close(h, migrated.stat(stat), original.stat(stat), "Existing effective stats must be preserved");
                close(h, migrated.statXp(stat), 7.25, "XP must survive migration unchanged");
            }
            close(h, migrated.maxHp(), original.maxHp(), "Max health must survive migration");
            close(h, migrated.maxKi(), original.maxKi(), "Max Ki must survive migration");
            h.assertTrue(migrated.techniques().equals(original.techniques()) && migrated.hasDragonRadar()
                    && migrated.dailyTrainingDay() == 123, "Unrelated character progress must be preserved");
            h.assertTrue(legacy.equals(before), "Loading must not mutate a version-four input tag");
            for (int cycle = 0; cycle < 3; cycle++) {
                migrated.load(migrated.save());
                close(h, migrated.trainedStat(KiStat.STRENGTH), 12, "Migration must be idempotent");
                close(h, migrated.statXp(KiStat.STRENGTH), 7.25, "XP must survive repeat loads");
            }
        }
        h.succeed();
    }

    @GameTest(template = "melee_arena")
    public static void transformationChangesCombatButNotProgress(GameTestHelper h) {
        PlayerData data = new PlayerData(); data.setRace(KiRace.SAIYAN);
        data.setAllStats(10); data.setStat(KiStat.STRENGTH, 3); data.setHasTail(true);
        data.train(KiStat.STRENGTH, 2);
        double level = data.trainedStat(KiStat.STRENGTH), xp = data.statXp(KiStat.STRENGTH);
        double damage = data.meleeDamageMultiplier(), reduction = data.damageReduction();
        h.assertTrue(data.activateTransformation(KiTransformation.OOZARU), "Setup must activate Oozaru");
        h.assertTrue(data.meleeDamageMultiplier() > damage && data.damageReduction() > reduction,
                "Shared final formulas must still apply form bonuses");
        h.assertTrue(data.weakestStats().equals(Set.of(KiStat.STRENGTH)), "A form must not hide the weakest trained stat");
        PlayerData loaded = new PlayerData(); loaded.load(data.save());
        close(h, loaded.trainedStat(KiStat.STRENGTH), level, "Saving in a form must not save its bonus as training");
        close(h, loaded.statXp(KiStat.STRENGTH), xp, "A form must not modify training XP");
        loaded.activateTransformation(KiTransformation.BASE);
        close(h, loaded.meleeDamageMultiplier(), damage, "Leaving a form must restore the original damage multiplier");
        h.succeed();
    }

    @GameTest(template = "melee_arena")
    public static void weakestStatsKeepTiesAndTrainingUsesUnmodifiedLevels(GameTestHelper h) {
        PlayerData data = new PlayerData(); data.setRace(KiRace.HUMAN);
        data.setAllStats(5); data.setStat(KiStat.KI_CONTROL, 1); data.setStat(KiStat.STRENGTH, 1);
        h.assertTrue(data.weakestStats().equals(Set.of(KiStat.STRENGTH, KiStat.KI_CONTROL)),
                "Human control bonus must not break a training-level tie");
        close(h, data.statXpRequired(KiStat.KI_CONTROL), 20, "Racial bonuses must not increase level XP requirements");
        data.train(KiStat.KI_CONTROL, 20); // Human training multiplier credits 25.6 XP.
        close(h, data.trainedStat(KiStat.KI_CONTROL), 2, "Training must increment only the trained level");
        close(h, data.stat(KiStat.KI_CONTROL), 2.18, "Race bonus must be applied exactly once");
        close(h, data.statXp(KiStat.KI_CONTROL), 5.6, "Remaining XP must use the trained-level requirement");
        h.assertTrue(data.weakestStats().equals(Set.of(KiStat.STRENGTH)), "Weakest set must update after training");
        h.succeed();
    }

    @GameTest(template = "melee_arena")
    public static void damagedAndPartialStatSavesAreRepairedWithoutCompoundingBonuses(GameTestHelper h) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("DataVersion", 5); tag.putInt("BalanceVersion", 2); tag.putString("Race", "NAMEKIAN");
        CompoundTag stats = new CompoundTag(); stats.putDouble("DURABILITY", Double.NaN);
        stats.putDouble("STRENGTH", -100); tag.put("Stats", stats);
        PlayerData data = new PlayerData(); data.load(tag);
        close(h, data.trainedStat(KiStat.DURABILITY), 1, "Invalid trained level must repair to one");
        close(h, data.stat(KiStat.DURABILITY), 1.25, "Repaired level must still receive the separate race bonus");
        data.validateAndRepair(); data.load(data.save());
        close(h, data.stat(KiStat.DURABILITY), 1.25, "Repeated repair must not compound bonuses");
        h.assertTrue(data.weakestStats().size() == 5, "Missing stats must be initialized as untrained");
        h.succeed();
    }

    private static void close(GameTestHelper h, double actual, double expected, String message) {
        h.assertTrue(Math.abs(actual - expected) < 0.000001, message + ": " + actual + " vs " + expected);
    }
}
