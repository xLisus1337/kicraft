package net.kicraft.kicraft.check;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.kicraft.kicraft.content.KicraftBlocks;
import net.kicraft.kicraft.content.KicraftEntities;
import net.kicraft.kicraft.content.TrainingDummyEntity;
import net.kicraft.kicraft.core.player.FlightMovement;
import net.kicraft.kicraft.core.player.KiRace;
import net.kicraft.kicraft.core.player.KiStat;
import net.kicraft.kicraft.core.player.KiTechnique;
import net.kicraft.kicraft.core.player.PlayerData;
import net.kicraft.kicraft.core.player.ResourceCosts;
import net.kicraft.kicraft.core.training.TrainingActivity;
import net.kicraft.kicraft.network.KiSenseHighlightPacket;
import net.kicraft.kicraft.network.KiSensePacket;
import net.kicraft.kicraft.network.MeleeInputPacket;
import net.kicraft.kicraft.server.*;
import net.kicraft.kicraft.server.capability.PlayerDataCapability;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

@GameTestHolder("kicraft")
@PrefixGameTestTemplate(false)
public final class MultiplayerGameTests {
    private static final String ARENA = "melee_arena";

    @GameTest(template = ARENA)
    public static void blastCreditsOwnerAndSynchronizesMotion(GameTestHelper h) {
        Fighter a = fighter(h, 2, 2), b = fighter(h, 2, 4);
        double strengthXp = data(a).statXp(KiStat.STRENGTH);
        int hits = impact(a, b.getBoundingBox().getCenter().add(0, 0, -0.5), 100, b);
        h.assertTrue(b.getHealth() < 20, "Ki Blast must deal damage; accepted hits=" + hits
                + ", health=" + b.getHealth());
        h.assertTrue(b.getLastDamageSource() != null && b.getLastDamageSource().getEntity() == a,
                "Ki Blast must retain its owner for PvP and kill credit");
        h.assertTrue(b.hurtMarked && b.getDeltaMovement().lengthSqr() > 0,
                "Ki Blast knockback must be synchronized to the victim's client");
        h.assertTrue(data(a).statXp(KiStat.STRENGTH) == strengthXp, "Ki Blast must not become a physical melee hit");
        finish(h, a, b);
    }

    @GameTest(template = ARENA)
    public static void blastRespectsPvpAndTeams(GameTestHelper h) {
        Fighter a = fighter(h, 2, 2), b = fighter(h, 2, 4);
        var server = h.getLevel().getServer();
        boolean pvp = server.isPvpAllowed();
        try {
            server.setPvpAllowed(false);
            impact(a, b.getBoundingBox().getCenter(), 100, b);
            h.assertTrue(b.getHealth() == 20 && b.getDeltaMovement().equals(Vec3.ZERO),
                    "PvP off must prevent blast damage and knockback");
            server.setPvpAllowed(true);
            PlayerTeam team = team(h);
            team.setAllowFriendlyFire(false);
            var board = h.getLevel().getScoreboard();
            board.addPlayerToTeam(a.getScoreboardName(), team);
            board.addPlayerToTeam(b.getScoreboardName(), team);
            impact(a, b.getBoundingBox().getCenter(), 100, b);
            h.assertTrue(b.getHealth() == 20 && b.getDeltaMovement().equals(Vec3.ZERO),
                    "Friendly-fire protection must also prevent blast splash and motion");
            board.removePlayerTeam(team);
        } finally { server.setPvpAllowed(pvp); }
        finish(h, a, b);
    }

    @GameTest(template = ARENA)
    public static void protectedBlastHitDoesNotPushOrTrain(GameTestHelper h) {
        Fighter a = fighter(h, 2, 2), b = fighter(h, 2, 4);
        b.getPersistentData().putLong("kicraftRespawnProtectionUntil", h.getLevel().getGameTime() + 100);
        double xp = data(b).statXp(KiStat.DURABILITY);
        impact(a, b.getBoundingBox().getCenter().add(0, 0, -0.5), 100, b);
        h.assertTrue(b.getHealth() == 20 && b.getDeltaMovement().equals(Vec3.ZERO),
                "A rejected hit cannot push a spawn-protected player");
        h.assertTrue(data(b).statXp(KiStat.DURABILITY) == xp, "Protected hits cannot train Durability");
        finish(h, a, b);
    }

    @GameTest(template = ARENA)
    public static void blastSplashStopsAtWallsAndRadius(GameTestHelper h) {
        Fighter a = fighter(h, 2, 1), b = fighter(h, 2, 4);
        h.setBlock(2, 1, 3, Blocks.STONE);
        h.setBlock(2, 2, 3, Blocks.STONE);
        impact(a, h.absoluteVec(new Vec3(2.5, 1.9, 2.99)), 100, null);
        h.assertTrue(b.getHealth() == 20 && b.getDeltaMovement().equals(Vec3.ZERO),
                "Nearby blast splash cannot pass through a solid wall");
        impact(a, b.position().add(1.8, 0.9, 1.8), 100, null);
        h.assertTrue(b.getHealth() == 20, "A target in the AABB corner but outside the sphere cannot be hit");
        finish(h, a, b);
    }

    @GameTest(template = ARENA)
    public static void sensePreservesTeamsAndGlobalGlow(GameTestHelper h) {
        Fighter a = fighter(h, 2, 2), b = fighter(h, 2, 4), c = fighter(h, 4, 4);
        PlayerTeam team = team(h);
        team.setAllowFriendlyFire(false);
        var board = h.getLevel().getScoreboard();
        board.addPlayerToTeam(a.getScoreboardName(), team);
        board.addPlayerToTeam(b.getScoreboardName(), team);
        b.setGlowingTag(true);
        double before = data(a).ki();
        KiSensePacket.scan(a);
        h.assertTrue(data(a).ki() < before, "The scan must actually execute");
        double after = data(a).ki();
        KiSensePacket.scan(a);
        h.assertTrue(data(a).ki() == after, "A repeated same-tick scan must be ignored");
        KiSensePacket.scan(b);
        h.assertTrue(a.getTeam() == team && b.getTeam() == team && c.getTeam() == null,
                "Overlapping viewers cannot change server teams");
        h.assertTrue(!a.canHarmPlayer(b), "Ki Sense cannot enable friendly fire");
        h.assertTrue(b.isCurrentlyGlowing() && !c.isCurrentlyGlowing(),
                "Scans cannot change real glowing flags or reveal targets to other viewers");
        board.removePlayerTeam(team);
        finish(h, a, b, c);
    }

    @GameTest(template = ARENA)
    public static void legacySenseRestoresWithoutOverwritingAdmin(GameTestHelper h) {
        Fighter a = fighter(h, 2, 2);
        var board = h.getLevel().getScoreboard();
        PlayerTeam real = team(h), admin = team(h);
        PlayerTeam legacy = board.getPlayerTeam("kcSenseGreen");
        if (legacy == null) legacy = board.addPlayerTeam("kcSenseGreen");
        board.addPlayerToTeam(a.getScoreboardName(), legacy);
        oldHighlight(a, real, false);
        KiSenseHighlightEvents.clearLegacy(a);
        h.assertTrue(a.getTeam() == real && !a.isCurrentlyGlowing(), "Old saved scan must restore its real team");
        oldHighlight(a, real, true);
        board.addPlayerToTeam(a.getScoreboardName(), admin);
        KiSenseHighlightEvents.clearLegacy(a);
        h.assertTrue(a.getTeam() == admin && a.isCurrentlyGlowing(), "Migration must preserve a later team and pre-existing glow");
        board.removePlayerTeam(real); board.removePlayerTeam(admin);
        finish(h, a);
    }

    @GameTest(template = ARENA)
    public static void trainingAndActionsStopAcrossLifecycle(GameTestHelper h) {
        Fighter a = fighter(h, 2, 2);
        h.setBlock(2, 0, 2, KicraftBlocks.TRAINING_MAT.get());
        h.assertTrue(ServerTrainingState.start(a, TrainingActivity.PUSH_UPS), "Fixture must start training");
        ServerKiState.setCharging(a, true); ServerKiState.setTransferring(a, true);
        CombatState.setBlocking(a, true);
        PlayerDataEvents.changedDimension(new PlayerEvent.PlayerChangedDimensionEvent(a, Level.OVERWORLD, Level.NETHER));
        h.assertTrue(!ServerTrainingState.isTraining(a) && !ServerKiState.isCharging(a)
                && !ServerKiState.isTransferring(a) && !CombatState.isBlocking(a),
                "Dimension changes must clear training and held actions");
        h.assertTrue(ServerTrainingState.start(a, TrainingActivity.PUSH_UPS), "Training can be restarted");
        a.setGameMode(GameType.SPECTATOR);
        ServerTrainingState.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, a));
        h.assertTrue(!ServerTrainingState.isTraining(a) && !ServerTrainingState.start(a, TrainingActivity.MEDITATION),
                "Spectators must neither continue nor start training");
        h.assertTrue(!TechniqueSystem.require(a, data(a), KiTechnique.KI_SENSE), "Spectators cannot use techniques");
        a.setGameMode(GameType.SURVIVAL);
        ServerTrainingState.start(a, TrainingActivity.MEDITATION);
        PlayerDataEvents.death(new LivingDeathEvent(a, a.damageSources().generic()));
        h.assertTrue(!ServerTrainingState.isTraining(a), "Death must clear the old player's training session");
        finish(h, a);
    }

    @GameTest(template = ARENA)
    public static void strongMeleeCannotKillTrainingDummy(GameTestHelper h) {
        Fighter a = fighter(h, 2, 2);
        TrainingDummyEntity dummy = KicraftEntities.TRAINING_DUMMY.get().create(h.getLevel());
        h.assertTrue(dummy != null, "Training dummy must exist");
        dummy.setPos(a.position().add(0, 0, 2));
        dummy.getPersistentData().putBoolean("kicraftTrainingDummy", true);
        h.getLevel().addFreshEntity(dummy);
        data(a).setStat(KiStat.STRENGTH, 1.0e30);
        dummy.hurt(a.damageSources().playerAttack(a), 10000);
        h.assertTrue(dummy.isAlive() && dummy.getHealth() >= dummy.getMaxHealth() - 0.011,
                "Dummy's cosmetic damage cap must run after Strength multipliers");
        dummy.discard();
        finish(h, a);
    }

    @GameTest(template = ARENA)
    public static void swordSweepCannotPushBystanders(GameTestHelper h) {
        Fighter a = fighter(h, 2, 2), b = fighter(h, 2, 4), bystander = fighter(h, 3, 4);
        a.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        data(a).setStamina(60);
        a.setOnGround(true);
        MeleeCombat.input(a, MeleeInputPacket.AttackSide.RIGHT);
        h.assertTrue(b.getHealth() < 20, "The sword strike must hit its actual target");
        h.assertTrue(bystander.getHealth() == 20 && bystander.getDeltaMovement().equals(Vec3.ZERO),
                "Filtered sweep hits must not leave bystander knockback behind");
        finish(h, a, b, bystander);
    }

    @GameTest(template = ARENA)
    public static void duplicateMeleePacketsRespectRecovery(GameTestHelper h) {
        Fighter a = fighter(h, 2, 2), b = fighter(h, 2, 4);
        data(a).setStamina(60);
        MeleeCombat.input(a, MeleeInputPacket.AttackSide.RIGHT);
        float healthAfterFirstPacket = b.getHealth();
        MeleeCombat.input(a, MeleeInputPacket.AttackSide.LEFT);
        h.assertTrue(b.getHealth() == healthAfterFirstPacket,
                "Duplicate multiplayer input cannot bypass server recovery with the other hand");
        finish(h, a, b);
    }

    @GameTest(template = ARENA)
    public static void scanPacketRoundTripAndBounds(GameTestHelper h) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var packet = new KiSenseHighlightPacket(Level.OVERWORLD.location(), 100,
                    List.of(new KiSenseHighlightPacket.Entry(UUID.randomUUID(), 0x55FF55),
                            new KiSenseHighlightPacket.Entry(UUID.randomUUID(), 0xFF5555)));
            packet.encode(buffer);
            h.assertTrue(packet.equals(new KiSenseHighlightPacket(buffer)), "Network scan must preserve UUIDs, colors and dimension");
            buffer.clear();
            buffer.writeResourceLocation(Level.OVERWORLD.location()); buffer.writeVarInt(100); buffer.writeVarInt(Integer.MAX_VALUE);
            boolean rejected = false;
            try { new KiSenseHighlightPacket(buffer); } catch (IllegalArgumentException expected) { rejected = true; }
            h.assertTrue(rejected, "Invalid scan sizes must be rejected before allocating entries");
        } finally { buffer.release(); }
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void meleePacketPreservesAnatomicalAttackSide(GameTestHelper h) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var packet = new MeleeInputPacket(MeleeInputPacket.AttackSide.LEFT);
            packet.encode(buffer);
            h.assertTrue(packet.equals(new MeleeInputPacket(buffer)),
                    "Melee input must preserve the selected anatomical side over the network");
        } finally { buffer.release(); }

        h.assertTrue(MeleeInputPacket.AttackSide.LEFT.interactionHand(HumanoidArm.RIGHT)
                        == InteractionHand.OFF_HAND
                        && MeleeInputPacket.AttackSide.RIGHT.interactionHand(HumanoidArm.RIGHT)
                        == InteractionHand.MAIN_HAND,
                "A right-dominant player must punch left with off-hand and right with main-hand");
        h.assertTrue(MeleeInputPacket.AttackSide.LEFT.interactionHand(HumanoidArm.LEFT)
                        == InteractionHand.MAIN_HAND
                        && MeleeInputPacket.AttackSide.RIGHT.interactionHand(HumanoidArm.LEFT)
                        == InteractionHand.OFF_HAND,
                "Anatomical punch sides must remain correct for a left-dominant player");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void flightUnlockDoesNotGrantPermanentFallImmunity(GameTestHelper h) {
        Fighter a = fighter(h, 2, 2);
        ServerKiState.setFlying(a, false);
        LivingFallEvent normalFall = new LivingFallEvent(a, 12.0F, 1.0F);
        ResourceTickEvents.preventFlightFallDamage(normalFall);
        h.assertTrue(!normalFall.isCanceled() && normalFall.getDistance() == 12.0F,
                "Unlocking flight alone cannot disable all future fall damage");

        ServerKiState.setFlying(a, true);
        LivingFallEvent flightFall = new LivingFallEvent(a, 12.0F, 1.0F);
        ResourceTickEvents.preventFlightFallDamage(flightFall);
        h.assertTrue(flightFall.isCanceled() && flightFall.getDistance() == 0.0F,
                "An actively flying player must remain protected from fall damage");
        finish(h, a);
    }

    @GameTest(template = ARENA)
    public static void cameraDirectedFlightUsesPitchAndLocalStrafe(GameTestHelper h) {
        Vec3 climb = FlightMovement.calculateVelocity(Vec3.ZERO,
                Vec3.directionFromRotation(-60.0F, 0.0F), 1.0F, 0.0F, false);
        Vec3 dive = FlightMovement.calculateVelocity(Vec3.ZERO,
                Vec3.directionFromRotation(60.0F, 0.0F), 1.0F, 0.0F, false);
        Vec3 left = FlightMovement.calculateVelocity(Vec3.ZERO,
                Vec3.directionFromRotation(0.0F, 0.0F), 0.0F, 1.0F, false);
        Vec3 inverted = FlightMovement.calculateVelocity(Vec3.ZERO,
                Vec3.directionFromRotation(-180.0F, 0.0F), new Vec3(1.0, 0.0, 0.0),
                1.0F, 0.0F, false);
        Vec3 regular = FlightMovement.calculateVelocity(Vec3.ZERO,
                new Vec3(0.0, 0.0, 1.0), new Vec3(1.0, 0.0, 0.0),
                1.0F, 0.0F, false);
        Vec3 boosted = FlightMovement.calculateVelocity(Vec3.ZERO,
                new Vec3(0.0, 0.0, 1.0), new Vec3(1.0, 0.0, 0.0),
                1.0F, 0.0F, true);
        PlayerData costs = new PlayerData();

        h.assertTrue(climb.y > 0.15 && climb.horizontalDistanceSqr() > 0.01,
                "Looking up while flying forward must climb along the camera direction");
        h.assertTrue(dive.y < -0.15 && dive.horizontalDistanceSqr() > 0.01,
                "Looking down while flying forward must descend along the camera direction");
        h.assertTrue(left.x > 0.1 && Math.abs(left.y) < 0.001,
                "Left strafe must stay relative to the camera without adding vertical motion");
        h.assertTrue(inverted.z < -0.15 && Math.abs(inverted.y) < 0.001,
                "Forward control must remain valid after continuous pitch crosses upside down");
        h.assertTrue(boosted.length() > regular.length() * 1.8,
                "Holding sprint during forward flight must provide a clearly visible speed boost");
        h.assertTrue(ResourceCosts.flightKiDrain(costs, true, true)
                        > ResourceCosts.flightKiDrain(costs, true, false),
                "Boost flight must drain Ki faster than regular flight");
        h.assertTrue(ResourceCosts.flightStaminaDrain(costs, true, true)
                        > ResourceCosts.flightStaminaDrain(costs, true, false),
                "Boost flight must drain Stamina faster than regular flight");
        h.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 70)
    public static void flightLoopNeedsMomentumAndCompletesTrajectory(GameTestHelper h) {
        Fighter a = fighter(h, 2, 2);
        a.moveTo(a.getX(), a.getY() + 4.0, a.getZ(), 0.0F, 0.0F);
        a.setOnGround(false);
        ServerKiState.setFlying(a, true);
        a.getAbilities().mayfly = true;
        a.getAbilities().flying = true;

        a.setDeltaMovement(Vec3.ZERO);
        h.assertTrue(!FlightManeuverSystem.tryStartLoop(a),
                "A hovering player cannot start a loop without forward momentum");

        double staminaBefore = data(a).stamina();
        a.setDeltaMovement(0.0, 0.32, 0.0);
        h.assertTrue(FlightManeuverSystem.tryStartLoop(a),
                "A vertical camera-directed climb must be able to start a loop");
        h.assertTrue(data(a).stamina() < staminaBefore, "Starting a loop must spend stamina");
        FlightManeuverSystem.tick(a);
        h.assertTrue(a.getDeltaMovement().y > 0.0 && a.getDeltaMovement().horizontalDistanceSqr() > 0.0,
                "The first part of a loop must preserve forward momentum and begin climbing");

        h.runAfterDelay(FlightManeuverSystem.LOOP_DURATION_TICKS + 3, () -> {
            FlightManeuverSystem.tick(a);
            h.assertTrue(!FlightManeuverSystem.isLooping(a), "The loop must finish instead of locking flight controls");
            h.assertTrue(a.getDeltaMovement().horizontalDistanceSqr() > 0.0,
                    "Completing a loop must return forward momentum");
            finish(h, a);
        });
    }

    private static void oldHighlight(Fighter p, PlayerTeam previous, boolean wasGlowing) {
        p.getPersistentData().putLong("KicraftKiSenseHighlightExpiresAt", p.level().getGameTime() + 100);
        p.getPersistentData().putString("KicraftKiSensePreviousTeam", previous.getName());
        p.getPersistentData().putBoolean("KicraftKiSenseWasGlowing", wasGlowing);
        p.setGlowingTag(true);
    }
    private static PlayerTeam team(GameTestHelper h) {
        return h.getLevel().getScoreboard().addPlayerTeam("test" + UUID.randomUUID().toString().substring(0, 8));
    }
    private static int impact(Fighter owner, Vec3 position, double damage, LivingEntity direct) {
        try {
            var method = KiBlastProjectileEvents.class.getDeclaredMethod("impact", ServerLevel.class, ServerPlayer.class,
                    Vec3.class, double.class, LivingEntity.class);
            method.setAccessible(true);
            return (int) method.invoke(null, owner.serverLevel(), owner, position, damage, direct);
        } catch (ReflectiveOperationException error) { throw new AssertionError("Impact failed", error); }
    }
    private static Fighter fighter(GameTestHelper h, int x, int z) {
        h.getLevel().getServer().setPvpAllowed(true);
        Fighter p = new Fighter(h.getLevel());
        Vec3 pos = h.absoluteVec(new Vec3(x + 0.5, 1, z + 0.5));
        p.moveTo(pos.x, pos.y, pos.z, z >= 4 ? 180 : 0, 0);
        p.setGameMode(GameType.SURVIVAL); p.setNoGravity(true);
        p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20); p.setHealth(20);
        data(p).setRace(KiRace.HUMAN); data(p).setStamina(100);
        data(p).unlockAllTechniques(); data(p).setKi(data(p).maxKi());
        h.getLevel().addNewPlayer(p);
        return p;
    }
    private static PlayerData data(Fighter p) {
        return p.getCapability(PlayerDataCapability.PLAYER_DATA).orElseThrow(() -> new AssertionError("Missing player capability"));
    }
    private static void finish(GameTestHelper h, Fighter... players) {
        for (Fighter p : players) { PlayerDataEvents.logout(new PlayerEvent.PlayerLoggedOutEvent(p)); p.discard(); }
        h.succeed();
    }
    private static final class Fighter extends FakePlayer {
        Fighter(ServerLevel level) {
            super(level, new GameProfile(UUID.randomUUID(), "Mp" + UUID.randomUUID().toString().substring(0, 10)));
            attackStrengthTicker = 100;
            try {
                var protection = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
                protection.setAccessible(true); protection.setInt(this, 0);
            } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
        }
        @Override public boolean isInvulnerableTo(DamageSource source) { return false; }
        @Override public boolean canHarmPlayer(Player other) {
            return getTeam() == null || !getTeam().isAlliedTo(other.getTeam()) || getTeam().isAllowFriendlyFire();
        }
    }
}
