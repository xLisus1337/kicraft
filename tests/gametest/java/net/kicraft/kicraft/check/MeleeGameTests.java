package net.kicraft.kicraft.check;

import com.mojang.authlib.GameProfile;
import net.kicraft.kicraft.core.combat.MeleeTimeline;
import net.kicraft.kicraft.core.player.KiRace;
import net.kicraft.kicraft.core.player.KiStat;
import net.kicraft.kicraft.core.player.PlayerData;
import net.kicraft.kicraft.network.MeleeInputPacket;
import net.kicraft.kicraft.server.CombatState;
import net.kicraft.kicraft.server.MeleeCombat;
import net.kicraft.kicraft.server.capability.PlayerDataCapability;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder("kicraft")
@PrefixGameTestTemplate(false)
public final class MeleeGameTests {
    static final String ARENA = "melee_arena";

    @GameTest(template = ARENA)
    public static void clickAttacksImmediatelyAndRecoveryRejectsSpam(GameTestHelper h) {
        Fighter a = fighter(h, 2, 2, 0), b = fighter(h, 2, 4, 180);
        float hp = b.getHealth();
        double stamina = data(a).stamina();
        strike(a);
        h.assertTrue(b.getHealth() < hp && a.swinging, "One click must damage and swing immediately");
        float after = b.getHealth();
        strike(a);
        h.assertTrue(b.getHealth() == after, "Recovery must reject duplicate click spam");
        h.assertTrue(data(a).stamina() < stamina && data(a).stamina() >= stamina - 2.01,
                "A regular strike must pay stamina exactly once");
        finish(h, a, b);
    }

    @GameTest(template = ARENA)
    public static void powerfulStrikeNeedsOverSixtyPercentAndFourPercentRoll(GameTestHelper h) {
        PlayerData data = new PlayerData();
        data.setRace(KiRace.HUMAN);
        data.setStamina(data.maxStamina() * 0.60);
        h.assertTrue(!MeleeCombat.rollPowerfulStrike(data, 0.0F),
                "Exactly 60% stamina must not enable a powerful strike");
        data.setStamina(data.maxStamina() * 0.6001);
        h.assertTrue(MeleeCombat.rollPowerfulStrike(data, 0.0F)
                        && MeleeCombat.rollPowerfulStrike(data, 0.039999F),
                "Every roll inside the first 4% must produce a powerful strike");
        h.assertTrue(!MeleeCombat.rollPowerfulStrike(data, 0.04F)
                        && !MeleeCombat.rollPowerfulStrike(data, 0.9F),
                "A roll at or above 4% must remain a regular strike");

        MeleeTimeline.Strike normal = new MeleeTimeline.Strike(false);
        MeleeTimeline.Strike powerful = new MeleeTimeline.Strike(true);
        h.assertTrue(powerful.damageMultiplier() > normal.damageMultiplier()
                        && powerful.guardMultiplier() > normal.guardMultiplier()
                        && powerful.staminaMultiplier() > normal.staminaMultiplier(),
                "The rare strike must retain stronger damage, guard pressure and stamina cost");
        h.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void fourRegularHitsBuildAndCloseCombo(GameTestHelper h) {
        Fighter a = fighter(h, 2, 2, 0), b = fighter(h, 2, 4, 180);
        b.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        b.setHealth(100);
        float[] damage = new float[4];

        float hp = b.getHealth();
        strike(a);
        damage[0] = hp - b.getHealth();
        h.assertTrue(MeleeCombat.comboStep(a) == 1, "First regular hit must open the combo");

        h.runAfterDelay(10, () -> {
            float before = b.getHealth();
            a.readyAttack();
            strike(a);
            damage[1] = before - b.getHealth();
            h.assertTrue(MeleeCombat.comboStep(a) == 2, "Second regular hit must advance the combo");
        });
        h.runAfterDelay(20, () -> {
            float before = b.getHealth();
            a.readyAttack();
            strike(a);
            damage[2] = before - b.getHealth();
            h.assertTrue(MeleeCombat.comboStep(a) == 3, "Third regular hit must advance the combo");
        });
        h.runAfterDelay(30, () -> {
            float before = b.getHealth();
            a.readyAttack();
            strike(a);
            damage[3] = before - b.getHealth();
            h.assertTrue(MeleeCombat.comboStep(a) == 0, "Fourth-hit finisher must close the combo");
            h.assertTrue(damage[1] > damage[0] && damage[2] > damage[1] && damage[3] > damage[2],
                    "Every combo step must deal progressively more damage");
            h.assertTrue(b.getDeltaMovement().horizontalDistanceSqr() > 0.05,
                    "Fourth-hit finisher must knock the target away");
            finish(h, a, b);
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void defenseMissAndIncomingHitResetCombo(GameTestHelper h) {
        Fighter a = fighter(h, 2, 2, 0), b = fighter(h, 2, 4, 180);
        strike(a);
        h.assertTrue(MeleeCombat.comboStep(a) == 1, "A landed regular hit must open the combo");

        h.runAfterDelay(10, () -> {
            CombatState.setBlocking(b, true);
            a.readyAttack();
            strike(a);
            h.assertTrue(MeleeCombat.comboStep(a) == 0, "A defended hit must reset the combo");
            CombatState.setBlocking(b, false);
        });
        h.runAfterDelay(20, () -> {
            a.readyAttack();
            strike(a);
            h.assertTrue(MeleeCombat.comboStep(a) == 1, "A new hit must reopen the combo");
        });
        h.runAfterDelay(30, () -> {
            a.setYRot(180);
            a.readyAttack();
            strike(a);
            h.assertTrue(MeleeCombat.comboStep(a) == 0, "A miss must reset the combo");
        });
        h.runAfterDelay(40, () -> {
            a.setYRot(0);
            a.readyAttack();
            strike(a);
            h.assertTrue(MeleeCombat.comboStep(a) == 1, "A landed hit must reopen the combo");
        });
        h.runAfterDelay(50, () -> {
            b.readyAttack();
            b.attack(a);
            h.assertTrue(MeleeCombat.comboStep(a) == 0, "Taking a melee hit must reset the combo");
            finish(h, a, b);
        });
    }

    @GameTest(template = ARENA)
    public static void parryCreatesImmediateCounter(GameTestHelper h) {
        Fighter a = fighter(h, 2, 2, 0), b = fighter(h, 2, 4, 180);
        CombatState.setBlocking(b, true);
        strike(a);
        h.assertTrue(b.getHealth() == 20, "Parry must prevent all damage");
        h.assertTrue(MeleeCombat.recovering(a), "Parried attacker must be staggered");
        h.assertTrue(data(b).stamina() >= 58.9 && data(b).stamina() < 60,
                "An actual parry must spend a small stamina cost");
        float hp = a.getHealth();
        strike(b);
        h.assertTrue(hp - a.getHealth() > 1.3, "Counter must boost damage against the parried attacker");
        finish(h, a, b);
    }

    @GameTest(template = ARENA, timeoutTicks = 30)
    public static void missPaysWithoutTrainingStrength(GameTestHelper h) {
        Fighter a = fighter(h, 2, 2, 180);
        double xp = data(a).statXp(KiStat.STRENGTH);
        strike(a);
        h.assertTrue(data(a).stamina() < 60, "A missed swing must still consume regular stamina");
        h.assertTrue(data(a).statXp(KiStat.STRENGTH) == xp, "Swinging at air cannot train Strength");
        h.assertTrue(MeleeCombat.recovering(a), "A miss must use normal recovery");
        h.runAfterDelay(7, () -> {
            h.assertTrue(!MeleeCombat.recovering(a), "Normal recovery must expire after six ticks");
            finish(h, a);
        });
    }

    @GameTest(template = ARENA)
    public static void wallPreventsImmediateHit(GameTestHelper h) {
        Fighter a = fighter(h, 2, 2, 0), b = fighter(h, 2, 4, 180);
        h.setBlock(2, 1, 3, Blocks.STONE);
        h.setBlock(2, 2, 3, Blocks.STONE);
        strike(a);
        h.assertTrue(b.getHealth() == 20, "Immediate strikes cannot pass through walls");
        finish(h, a, b);
    }

    @GameTest(template = ARENA)
    public static void emptyStaminaCannotRollPowerfulButCanPunch(GameTestHelper h) {
        Fighter a = fighter(h, 2, 2, 0), b = fighter(h, 2, 4, 180);
        data(a).setStamina(0);
        h.assertTrue(!MeleeCombat.rollPowerfulStrike(data(a), 0.0F),
                "Empty stamina must disable the rare powerful strike");
        strike(a);
        h.assertTrue(b.getHealth() < 20, "Lack of stamina must not turn a normal click into a charged-action rejection");
        h.assertTrue(data(a).stamina() == 0, "A regular punch cannot create negative stamina");
        finish(h, a, b);
    }

    private static Fighter fighter(GameTestHelper h, int x, int z, float yaw) {
        h.getLevel().getServer().setPvpAllowed(true);
        Fighter player = new Fighter(h.getLevel());
        Vec3 p = h.absoluteVec(new Vec3(x + 0.5, 1, z + 0.5));
        player.moveTo(p.x, p.y, p.z, yaw, 0);
        player.setGameMode(GameType.SURVIVAL);
        player.setNoGravity(true);
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);
        player.setHealth(20);
        data(player).setRace(KiRace.HUMAN);
        // Exactly 60% disables random powerful hits so ordinary combat tests cannot be flaky.
        data(player).setStamina(60);
        h.getLevel().addNewPlayer(player);
        return player;
    }

    private static PlayerData data(Fighter p) {
        return p.getCapability(PlayerDataCapability.PLAYER_DATA)
                .orElseThrow(() -> new AssertionError("missing capability"));
    }

    private static void strike(Fighter p) {
        MeleeCombat.input(p, MeleeInputPacket.AttackSide.RIGHT);
    }

    private static void finish(GameTestHelper h, Fighter... fighters) {
        for (Fighter p : fighters) {
            CombatState.clear(p);
            p.discard();
        }
        h.succeed();
    }

    private static final class Fighter extends FakePlayer {
        Fighter(ServerLevel level) {
            super(level, new GameProfile(UUID.randomUUID(), "MeleeCheck"));
            attackStrengthTicker = 100;
            try {
                var protection = net.minecraft.server.level.ServerPlayer.class
                        .getDeclaredField("spawnInvulnerableTime");
                protection.setAccessible(true);
                protection.setInt(this, 0);
            } catch (ReflectiveOperationException error) {
                throw new AssertionError("Could not clear test-player spawn protection", error);
            }
        }

        void readyAttack() { attackStrengthTicker = 100; }

        @Override
        public boolean isInvulnerableTo(DamageSource source) { return false; }

        @Override
        public boolean canHarmPlayer(Player other) {
            return getTeam() == null || !getTeam().isAlliedTo(other.getTeam()) || getTeam().isAllowFriendlyFire();
        }
    }
}
