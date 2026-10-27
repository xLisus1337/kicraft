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
import net.kicraft.kicraft.core.player.StaminaRules;
import net.kicraft.kicraft.core.training.TrainingActivity;
import net.kicraft.kicraft.network.KiSenseHighlightPacket;
import net.kicraft.kicraft.network.KiSensePacket;
import net.kicraft.kicraft.network.MeleeInputPacket;
import net.kicraft.kicraft.network.PlayerDataSyncPacket;
import net.kicraft.kicraft.network.StatLevelUpPacket;
import net.kicraft.kicraft.server.*;
import net.kicraft.kicraft.server.capability.PlayerDataCapability;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

@GameTestHolder("kicraft")
@PrefixGameTestTemplate(false)
public final class MultiplayerGameTests {
    private static final String ARENA = "melee_arena";

    @GameTest(template = ARENA)
    public static void vanillaCombatRejectsCustomPacketsAndKeepsNormalCooldown(GameTestHelper h) {
        Fighter a = fighter(h, 2, 2), b = fighter(h, 2, 4);
        double stamina = data(a).stamina();
        new MeleeInputPacket(MeleeInputPacket.AttackSide.RIGHT).apply(a);
        new net.kicraft.kicraft.network.BlockPacket(true).apply(b);
        h.assertTrue(b.getHealth() == 20 && data(a).stamina() == stamina && !CombatState.isBlocking(b),
                "Custom punch and bare-handed guard packets must be inactive in vanilla combat mode");
        a.attack(b);
        h.assertTrue(b.getHealth() < 20 && a.getAttackStrengthScale(0) < 0.1F,
                "A vanilla attack must hit and start Minecraft's normal cooldown");
        h.assertTrue(data(a).statXp(KiStat.STRENGTH) > 0 && data(a).stamina() < stamina,
                "Vanilla hits must still train mod stats and use stamina");
        float health = b.getHealth();
        a.attack(b);
        h.assertTrue(b.getHealth() == health, "Repeating a hit must preserve vanilla hurt immunity");
        var speed = a.getAttribute(Attributes.ATTACK_SPEED);
        UUID legacy = UUID.fromString("f347b78f-b0c1-4be3-b1c4-02a2549ca28f");
        speed.addTransientModifier(new AttributeModifier(legacy, "Old exhaustion", -0.75, AttributeModifier.Operation.MULTIPLY_TOTAL));
        data(a).setStamina(0);
        ResourceTickEvents.playerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, a));
        h.assertTrue(speed.getModifier(legacy) == null && speed.getValue() == 4.0,
                "Exhaustion must not replace Minecraft's attack cooldown");
        finish(h, a, b);
    }

    @GameTest(template = ARENA)
    public static void vanillaShieldStillBlocksAndStatsStillScaleDamage(GameTestHelper h) {
        Fighter a = fighter(h, 2, 2), b = fighter(h, 2, 4);
        b.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
        b.setYHeadRot(b.getYRot());
        b.startUsingItem(InteractionHand.OFF_HAND);
        for (int tick = 0; tick < 6; tick++) b.tickUsingItem();
        h.assertTrue(b.isBlocking(), "Setup must raise the vanilla shield");
        h.assertTrue(b.isDamageSourceBlocked(a.damageSources().playerAttack(a)), "Shield must face the attacker");
        a.attack(b);
        h.assertTrue(b.getHealth() == 20, "A vanilla shield must stop a frontal attack");
        b.stopUsingItem();
        data(a).setStamina(data(a).maxStamina());
        data(a).setStat(KiStat.STRENGTH, 100);
        var hit = new net.minecraftforge.event.entity.living.LivingDamageEvent(b, a.damageSources().playerAttack(a), 2.0F);
        TrainingEvents.damageTraining(hit);
        h.assertTrue(hit.getAmount() > 2.0F, "Mod strength must still scale ordinary melee damage");
        h.assertTrue(!b.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Vanilla combat must not add custom hit stagger");
        finish(h, a, b);
    }

    @GameTest(template = ARENA)
    public static void fullyAbsorbedVanillaHitCannotRegainDamageFromStats(GameTestHelper h) {
        Fighter a = fighter(h, 2, 2), b = fighter(h, 2, 4);
        b.setAbsorptionAmount(20.0F);
        data(a).setStat(KiStat.STRENGTH, 100);
        a.attack(b);
        h.assertTrue(b.getHealth() == 20.0F && b.getAbsorptionAmount() < 20.0F,
                "A hit fully absorbed by golden hearts must not become positive HP damage through stat scaling");
        finish(h, a, b);
    }

    @GameTest(template = ARENA)
    public static void resourceTicksPreserveExternalReachAndMeleeUsesIt(GameTestHelper h) {
        Fighter a = fighter(h, 2, 2), b = fighter(h, 2, 6);
        var reach = a.getAttribute(ForgeMod.ENTITY_REACH.get());
        h.assertTrue(reach != null && Math.abs(reach.getBaseValue() - 3.0) < 0.001,
                "Unmodified players must retain Forge's normal base reach");
        UUID itemId = UUID.randomUUID(), effectId = UUID.randomUUID();
        reach.setBaseValue(4.0);
        reach.addPermanentModifier(new AttributeModifier(itemId, "External item reach", 1.0,
                AttributeModifier.Operation.ADDITION));
        reach.addTransientModifier(new AttributeModifier(effectId, "External effect reach", 0.20,
                AttributeModifier.Operation.MULTIPLY_TOTAL));
        for (int i = 0; i < 3; i++) {
            ResourceTickEvents.playerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, a));
        }
        h.assertTrue(reach.getBaseValue() == 4.0 && reach.getModifier(itemId) != null
                        && reach.getModifier(effectId) != null && Math.abs(reach.getValue() - 6.0) < 0.001,
                "Ki Craft must not overwrite external base reach or permanent/transient modifiers");
        data(a).setStamina(60);
        MeleeCombat.input(a, MeleeInputPacket.AttackSide.RIGHT);
        h.assertTrue(b.getHealth() < 20.0F, "Server melee targeting must honor the preserved reach");
        finish(h, a, b);
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void periodicSyncCoalescesLatestResourcesAndKeepsLevelUps(GameTestHelper h) {
        Fighter a = fighter(h, 2, 2);
        SyncRecorder recorder = new SyncRecorder(a);
        PlayerDataEvents.sync(a);
        for (int i = 0; i < 12; i++) {
            data(a).setKi(2.0 + i * 0.1);
            ResourceTickEvents.playerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, a));
            PlayerDataEvents.requestSync(a);
        }
        double previousStrength = data(a).stat(KiStat.STRENGTH);
        // Human strength training credits only 92% of the supplied XP.
        data(a).train(KiStat.STRENGTH, 2.0 * data(a).statXpRequired(KiStat.STRENGTH));
        h.assertTrue(data(a).stat(KiStat.STRENGTH) > previousStrength, "Setup must earn a level");
        PlayerDataEvents.requestSync(a);
        h.assertTrue(recorder.states.size() == 1, "Periodic requests must not send immediately");
        h.runAfterDelay(2, () -> {
            h.assertTrue(recorder.states.size() == 1, "Full resource packets must respect the four-tick interval");
            data(a).setKi(5.0);
            PlayerDataEvents.requestSync(a);
            data(a).clearDirty();
        });
        h.runAfterDelay(6, () -> {
            h.assertTrue(recorder.states.size() == 2 && recorder.last().data().getDouble("Ki") == 5.0,
                    "Many requests must produce one packet with the latest values, even after dirty is cleared");
            h.assertTrue(recorder.levelUps.size() == 1
                            && recorder.levelUps.get(0).levels().containsKey(KiStat.STRENGTH),
                    "Deferred sync must deliver the earned level notification exactly once");
            finish(h, a);
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void continuousSyncIsBoundedAndFlushesFinalChange(GameTestHelper h) {
        Fighter a = fighter(h, 2, 2);
        SyncRecorder recorder = new SyncRecorder(a);
        PlayerDataEvents.sync(a);
        for (int tick = 1; tick <= 20; tick++) {
            final int step = tick;
            h.runAfterDelay(tick, () -> {
                data(a).setStamina(80.0 - step);
                for (int request = 0; request < 3; request++) PlayerDataEvents.requestSync(a);
                data(a).clearDirty();
            });
        }
        h.runAfterDelay(25, () -> {
            h.assertTrue(recorder.states.size() <= 7 && recorder.states.size() >= 5,
                    "Continuous changes must be sampled about five times per second, not for every request");
            for (int i = 1; i < recorder.ticks.size(); i++) {
                h.assertTrue(recorder.ticks.get(i) - recorder.ticks.get(i - 1) >= 4,
                        "Periodic full-state packets must be at least four ticks apart");
            }
            h.assertTrue(recorder.last().data().getDouble("Stamina") == 60.0,
                    "The final queued update must arrive even after resource changes stop");
            finish(h, a);
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void respawnSyncBypassesQueueAndLogoutCancelsPendingSync(GameTestHelper h) {
        Fighter a = fighter(h, 2, 2), b = fighter(h, 4, 2);
        SyncRecorder first = new SyncRecorder(a), second = new SyncRecorder(b);
        PlayerDataEvents.sync(a); PlayerDataEvents.sync(b);
        data(a).setKi(1.0); data(b).setKi(1.0);
        PlayerDataEvents.requestSync(a); PlayerDataEvents.requestSync(b);
        PlayerDataEvents.respawn(new PlayerEvent.PlayerRespawnEvent(a, false));
        h.assertTrue(first.states.size() == 2 && first.last().data().getDouble("Ki") == data(a).maxKi(),
                "Respawn must send restored resources immediately despite the throttle");
        PlayerDataEvents.logout(new PlayerEvent.PlayerLoggedOutEvent(b));
        h.runAfterDelay(6, () -> {
            h.assertTrue(first.states.size() == 2, "An immediate sync must replace its obsolete queued update");
            h.assertTrue(second.states.size() == 1, "Logout must cancel queued packets for the old player");
            finish(h, a, b);
        });
    }

    @GameTest(template = ARENA)
    public static void placingDummyReplacesOnlyItsOwnersPreviousDummy(GameTestHelper h) {
        Fighter a = fighter(h, 2, 2), b = fighter(h, 4, 2);
        ServerTrainingState.spawnDummy(a);
        TrainingDummyEntity first = ownedDummy(a);
        ServerTrainingState.spawnDummy(b);
        TrainingDummyEntity other = ownedDummy(b);
        a.setYRot(90.0F);
        ServerTrainingState.spawnDummy(a);
        TrainingDummyEntity replacement = ownedDummy(a);
        h.assertTrue(first.isRemoved() && !first.getUUID().equals(replacement.getUUID()),
                "Placing another dummy must delete the previous entity and create a new one");
        h.assertTrue(!other.isRemoved() && ownedDummy(b) == other,
                "Replacing a dummy must not delete another player's dummy");
        Vec3 expected = a.position().add(a.getLookAngle().multiply(1, 0, 1).normalize().scale(2.2));
        h.assertTrue(replacement.position().distanceToSqr(expected) < 0.001,
                "The replacement must appear in front of the owner's current position and aim");
        replacement.discard(); other.discard();
        finish(h, a, b);
    }

    @GameTest(template = ARENA)
    public static void replacingDummyRemovesPredecessorInAnotherDimension(GameTestHelper h) {
        Fighter a = fighter(h, 2, 2);
        ServerLevel otherWorld = h.getLevel().getServer().getLevel(Level.END);
        h.assertTrue(otherWorld != null, "The fixture needs a second dimension");
        // Add directly: the player does not need to enter Ki Craft's disabled dimensions.
        TrainingDummyEntity previous = legacyDummy(otherWorld, a.getUUID(), a.position());
        h.assertTrue(otherWorld.addFreshEntity(previous), "The legacy dummy should be adopted");
        ServerTrainingState.spawnDummy(a);
        TrainingDummyEntity replacement = ownedDummy(a);
        h.assertTrue(previous.isRemoved() && replacement.level() == a.level(),
                "The per-player limit must cover all dimensions, not just the current one");
        replacement.discard();
        finish(h, a);
    }

    @GameTest(template = ARENA)
    public static void replacedUnloadedDummyCannotReturnFromSavedEntity(GameTestHelper h) {
        Fighter a = fighter(h, 2, 2);
        ServerTrainingState.spawnDummy(a);
        TrainingDummyEntity first = ownedDummy(a);
        CompoundTag oldEntity = new CompoundTag();
        first.save(oldEntity);
        first.discard(); // Simulate its absence from loaded entities while retaining its disk snapshot.
        ServerTrainingState.spawnDummy(a);
        TrainingDummyEntity replacement = ownedDummy(a);
        TrainingDummyEntity returning = KicraftEntities.TRAINING_DUMMY.get().create(h.getLevel());
        returning.load(oldEntity);
        EntityJoinLevelEvent diskLoad = new EntityJoinLevelEvent(returning, h.getLevel(), true);
        ServerTrainingState.rejectReplacedDummy(diskLoad);
        h.assertTrue(diskLoad.isCanceled() && !h.getLevel().addFreshEntity(returning),
                "An old chunk snapshot must not restore a replaced dummy");
        TrainingDummyRegistry restored = TrainingDummyRegistry.load(
                TrainingDummyRegistry.get(a.server).save(new CompoundTag()));
        h.assertTrue(restored.acceptOrAdopt(a.getUUID(), replacement.getUUID())
                        && !restored.acceptOrAdopt(a.getUUID(), first.getUUID()),
                "Saving and reloading the registry must preserve which dummy is current");
        replacement.discard();
        finish(h, a);
    }

    @GameTest(template = ARENA)
    public static void legacyDuplicatesAndManuallyRemovedDummiesCannotReturn(GameTestHelper h) {
        Fighter a = fighter(h, 2, 2);
        TrainingDummyEntity first = legacyDummy(h.getLevel(), a.getUUID(), a.position().add(0, 0, 2));
        TrainingDummyEntity duplicate = legacyDummy(h.getLevel(), a.getUUID(), a.position().add(2, 0, 0));
        h.assertTrue(h.getLevel().addFreshEntity(first) && !h.getLevel().addFreshEntity(duplicate),
                "Old worlds must adopt one owned dummy and reject their pre-existing duplicates");
        a.setShiftKeyDown(true);
        PlayerInteractEvent.EntityInteract interaction = new PlayerInteractEvent.EntityInteract(
                a, InteractionHand.MAIN_HAND, first);
        ServerTrainingState.removeOwnedDummy(interaction);
        h.assertTrue(interaction.isCanceled() && first.isRemoved(),
                "Sneak interaction must still remove the owned dummy");
        TrainingDummyRegistry restored = TrainingDummyRegistry.load(
                TrainingDummyRegistry.get(a.server).save(new CompoundTag()));
        h.assertTrue(!restored.acceptOrAdopt(a.getUUID(), first.getUUID())
                        && !restored.acceptOrAdopt(a.getUUID(), duplicate.getUUID()),
                "Removal must survive a restart without adopting an older unloaded duplicate");
        a.setShiftKeyDown(false);
        ServerTrainingState.spawnDummy(a);
        TrainingDummyEntity replacement = ownedDummy(a);
        h.assertTrue(!replacement.isRemoved(), "Manual removal must not prevent a later placement");
        replacement.discard();
        finish(h, a);
    }

    @GameTest(template = ARENA)
    public static void exhaustedSprintResumesAtSharedTenPercentThreshold(GameTestHelper h) {
        Fighter a = fighter(h, 2, 2);
        data(a).setStamina(0.0005);
        h.assertTrue(StaminaRules.isEmpty(data(a).stamina()) && StaminaSystem.isSprintLocked(a, data(a)),
                "Client and server must both treat near-zero stamina as exhausted");
        for (double stamina : new double[] {5.0, 9.999}) {
            data(a).setStamina(stamina);
            StaminaSystem.enforceSprintLock(a, data(a));
            h.assertTrue(!StaminaRules.canResumeSprint(stamina, data(a).maxStamina())
                            && StaminaSystem.isSprintLocked(a, data(a)),
                    "Sprint must stay locked below 10%, including the former client threshold of 5%");
        }
        data(a).setStamina(10.0);
        StaminaSystem.enforceSprintLock(a, data(a));
        h.assertTrue(StaminaRules.canResumeSprint(10.0, data(a).maxStamina())
                        && !StaminaSystem.isSprintLocked(a, data(a)),
                "Exactly 10% must release server exhaustion and allow client sprint prediction");
        finish(h, a);
    }

    @GameTest(template = ARENA)
    public static void foodHealsAtFullHungerAndConsumesOneItem(GameTestHelper h) {
        Fighter a = fighter(h, 2, 2);
        a.getFoodData().setFoodLevel(20);
        a.setHealth(5.0F);
        // Deliberately leave capability HP full: eating can happen before its next sync.
        a.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD));
        var result = a.getMainHandItem().use(a.level(), a, InteractionHand.MAIN_HAND);
        h.assertTrue(result.getResult().consumesAction() && a.isUsingItem(),
                "A wounded survival player must be able to start eating at full hunger");
        h.assertTrue(a.getHealth() == 5.0F, "Starting to eat must not heal early");
        a.finishFood();
        h.assertTrue(a.getMainHandItem().isEmpty(), "Finishing the last bread must consume it");
        h.assertTrue(Math.abs(a.getHealth() - 9.8F) < 0.01F,
                "Bread must heal 24% of max HP from current health, not stale capability HP");
        h.assertTrue(Math.abs(data(a).hp() - data(a).maxHp() * a.getHealth() / a.getMaxHealth()) < 0.01,
                "Food must synchronize Ki Craft HP with the healed entity");
        finish(h, a);
    }

    @GameTest(template = ARENA)
    public static void healthyPlayersKeepVanillaFoodRestrictions(GameTestHelper h) {
        Fighter a = fighter(h, 2, 2);
        a.getFoodData().setFoodLevel(20);
        a.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD));
        h.assertTrue(!a.getMainHandItem().use(a.level(), a, InteractionHand.MAIN_HAND)
                        .getResult().consumesAction() && !a.isUsingItem(),
                "Full health and full hunger must still reject ordinary food");
        a.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GOLDEN_APPLE));
        h.assertTrue(a.getMainHandItem().use(a.level(), a, InteractionHand.MAIN_HAND)
                        .getResult().consumesAction(), "Always-edible food must remain usable");
        a.stopUsingItem();
        a.getFoodData().setFoodLevel(15);
        h.assertTrue(a.canEat(false), "The vanilla hungry-player path must remain valid");
        finish(h, a);
    }

    @GameTest(template = ARENA)
    public static void potionsHealButHungerRegenerationStaysDisabled(GameTestHelper h) {
        Fighter a = fighter(h, 2, 2);
        a.setHealth(5.0F);
        a.getFoodData().setFoodLevel(20);
        a.getFoodData().setSaturation(5.0F);
        for (int i = 0; i < 10; i++) a.getFoodData().tick(a);
        h.assertTrue(a.getHealth() == 5.0F, "Saturation must not passively heal the player");
        a.getFoodData().setSaturation(0.0F);
        for (int i = 0; i < 80; i++) a.getFoodData().tick(a);
        h.assertTrue(a.getHealth() == 5.0F, "Full hunger must not passively heal the player");
        MobEffects.HEAL.applyInstantenousEffect(null, null, a, 0, 1.0);
        h.assertTrue(a.getHealth() == 9.0F, "Instant health must no longer be globally cancelled");
        MobEffects.REGENERATION.applyEffectTick(a, 0);
        h.assertTrue(a.getHealth() == 10.0F, "Regeneration effects must remain usable");
        finish(h, a);
    }

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
    public static void fallsNeverTrainDurabilityForAnyRace(GameTestHelper h) {
        Fighter victim = fighter(h, 2, 2), attacker = fighter(h, 2, 4);
        for (KiRace race : KiRace.values()) {
            data(victim).setRace(race);
            victim.setHealth(6);
            data(victim).setHp(data(victim).maxHp() * 0.25);
            double level = data(victim).trainedStat(KiStat.DURABILITY);
            double xp = data(victim).statXp(KiStat.DURABILITY);
            victim.invulnerableTime = 0;
            victim.hurt(victim.damageSources().fall(), 2.0F);
            h.assertTrue(victim.getHealth() < 6, "Fall damage must still hurt " + race);
            h.assertTrue(data(victim).trainedStat(KiStat.DURABILITY) == level
                            && data(victim).statXp(KiStat.DURABILITY) == xp,
                    "Falls must not train Durability, including racial and low-health bonuses: " + race);
            victim.invulnerableTime = 0;
            victim.hurt(victim.damageSources().playerAttack(attacker), 1.0F);
            h.assertTrue(data(victim).trainedStat(KiStat.DURABILITY) > level
                            || data(victim).statXp(KiStat.DURABILITY) > xp,
                    "Combat damage must still train Durability for " + race);
        }
        finish(h, victim, attacker);
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
                Vec3.directionFromRotation(-60.0F, 0.0F), 1.0F, 0.0F, 0.0F, false);
        Vec3 dive = FlightMovement.calculateVelocity(Vec3.ZERO,
                Vec3.directionFromRotation(60.0F, 0.0F), 1.0F, 0.0F, 0.0F, false);
        Vec3 left = FlightMovement.calculateVelocity(Vec3.ZERO,
                Vec3.directionFromRotation(0.0F, 0.0F), 0.0F, 1.0F, 0.0F, false);
        Vec3 inverted = FlightMovement.calculateVelocity(Vec3.ZERO,
                Vec3.directionFromRotation(-180.0F, 0.0F), new Vec3(1.0, 0.0, 0.0),
                1.0F, 0.0F, 0.0F, false);
        Vec3 regular = FlightMovement.calculateVelocity(Vec3.ZERO,
                new Vec3(0.0, 0.0, 1.0), new Vec3(1.0, 0.0, 0.0),
                1.0F, 0.0F, 0.0F, false);
        Vec3 boosted = FlightMovement.calculateVelocity(Vec3.ZERO,
                new Vec3(0.0, 0.0, 1.0), new Vec3(1.0, 0.0, 0.0),
                1.0F, 0.0F, 0.0F, true);
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
    private static TrainingDummyEntity ownedDummy(Fighter owner) {
        UUID id = TrainingDummyRegistry.get(owner.server).current(owner.getUUID());
        if (id != null && owner.serverLevel().getEntity(id) instanceof TrainingDummyEntity dummy) return dummy;
        throw new AssertionError("Missing current owned dummy");
    }
    private static TrainingDummyEntity legacyDummy(ServerLevel level, UUID owner, Vec3 position) {
        TrainingDummyEntity dummy = KicraftEntities.TRAINING_DUMMY.get().create(level);
        if (dummy == null) throw new AssertionError("Missing dummy entity type");
        dummy.setPos(position);
        dummy.getPersistentData().putBoolean("kicraftTrainingDummy", true);
        dummy.getPersistentData().putUUID("kicraftTrainingDummyOwner", owner);
        return dummy;
    }
    private static void finish(GameTestHelper h, Fighter... players) {
        for (Fighter p : players) { PlayerDataEvents.logout(new PlayerEvent.PlayerLoggedOutEvent(p)); p.discard(); }
        h.succeed();
    }
    private static final class SyncRecorder extends Connection {
        final List<PlayerDataSyncPacket> states = new ArrayList<>();
        final List<StatLevelUpPacket> levelUps = new ArrayList<>();
        final List<Long> ticks = new ArrayList<>();
        private final Fighter owner;

        SyncRecorder(Fighter player) {
            super(PacketFlow.SERVERBOUND);
            owner = player;
            // Forge's PLAYER distributor sends directly through the Connection.
            new ServerGamePacketListenerImpl(player.server, this, player);
        }

        PlayerDataSyncPacket last() { return states.get(states.size() - 1); }

        @Override public void send(Packet<?> packet) { send(packet, null); }

        @Override public void send(Packet<?> packet, PacketSendListener listener) {
            if (!(packet instanceof ClientboundCustomPayloadPacket payload)
                    || !payload.getIdentifier().toString().equals("kicraft:main")) return;
            FriendlyByteBuf buffer = payload.getData();
            try {
                // These are the first two registrations in KicraftNetwork.
                int id = buffer.readVarInt();
                if (id == 0) {
                    states.add(new PlayerDataSyncPacket(buffer));
                    ticks.add(owner.server.overworld().getGameTime());
                } else if (id == 1) {
                    levelUps.add(new StatLevelUpPacket(buffer));
                }
            } finally { buffer.release(); }
        }
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
        void finishFood() { completeUsingItem(); }
        void tickUsingItem() { updateUsingItem(getUseItem()); } // FakePlayer.tick() is deliberately empty.
        @Override public boolean canHarmPlayer(Player other) {
            return getTeam() == null || !getTeam().isAlliedTo(other.getTeam()) || getTeam().isAllowFriendlyFire();
        }
    }
}
