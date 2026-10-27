package net.kicraft.kicraft.server;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.kicraft.kicraft.core.player.KiRace;
import net.kicraft.kicraft.core.player.KiTransformation;
import net.kicraft.kicraft.core.player.PlayerData;
import net.kicraft.kicraft.network.OozaruFocusPacket;
import net.kicraft.kicraft.server.capability.PlayerDataCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder("kicraft")
@PrefixGameTestTemplate(false)
public final class OozaruGameTests {
    @GameTest(template = "melee_arena")
    public static void moonFollowsEastZenithWestAndRequiresCorrectAim(GameTestHelper h) {
        long originalTime = h.getLevel().getDayTime();
        FakePlayer player = player(h);
        player.setPos(player.getX(), h.getLevel().getMaxBuildHeight() + 2, player.getZ());
        try {
            for (long time : new long[]{13000L, 18000L, 23000L}) {
                h.getLevel().setDayTime(time);
                Vec3 moon = OozaruTransformationSystem.moonDirection(h.getLevel());
                if (time == 13000L) h.assertTrue(moon.x > 0.9 && moon.y > 0, "Moon must rise in the east");
                if (time == 18000L) h.assertTrue(moon.y > 0.999, "Moon must be overhead at midnight");
                if (time == 23000L) h.assertTrue(moon.x < -0.9 && moon.y > 0, "Moon must set in the west");
                aim(player, moon);
                h.assertTrue(OozaruTransformationSystem.isLookingAtMoon(player), "Actual moon direction must be accepted");
                player.setXRot(-30.0F);
                player.setYRot(0.0F);
                h.assertTrue(!OozaruTransformationSystem.isLookingAtMoon(player), "Looking up elsewhere must not count");
            }
            h.getLevel().setDayTime(6000L);
            h.assertTrue(!OozaruTransformationSystem.isFullMoonNight(h.getLevel()), "Daylight must not trigger Oozaru");
            h.getLevel().setDayTime(42000L);
            h.assertTrue(!OozaruTransformationSystem.isFullMoonNight(h.getLevel()), "Other moon phases must not trigger Oozaru");
            h.assertTrue(!OozaruTransformationSystem.isFullMoonNight(h.getLevel().getServer().getLevel(Level.END)),
                    "A dimension without the Overworld moon must not trigger Oozaru");
            h.succeed();
        } finally { h.getLevel().setDayTime(originalTime); OozaruTransformationSystem.clear(player); }
    }

    @GameTest(template = "melee_arena")
    public static void moonMustBeVisibleAndFocusCompletesWithLockedRotation(GameTestHelper h) {
        long originalTime = h.getLevel().getDayTime();
        FakePlayer player = player(h);
        PlayerData data = data(player);
        // Keep this sky test above terrain and the arena's existing ceiling.
        player.setPos(player.getX(), h.getLevel().getMaxBuildHeight() - 10, player.getZ());
        BlockPos roof = player.blockPosition().above(3);
        var originalRoof = h.getLevel().getBlockState(roof);
        try {
            h.getLevel().setDayTime(18000L);
            aim(player, OozaruTransformationSystem.moonDirection(h.getLevel()));
            h.getLevel().setBlockAndUpdate(roof, Blocks.STONE.defaultBlockState());
            OozaruTransformationSystem.tick(player, data);
            h.assertTrue(!OozaruTransformationSystem.isFocusing(player), "A roof between eyes and moon must block the trigger");
            h.getLevel().setBlockAndUpdate(roof, Blocks.AIR.defaultBlockState());
            float yaw = player.getYRot(), pitch = player.getXRot();
            OozaruTransformationSystem.tick(player, data);
            h.assertTrue(OozaruTransformationSystem.isFocusing(player), "Looking at the visible full moon must start focus");
            player.setYRot(45); player.setXRot(0);
            OozaruTransformationSystem.tick(player, data);
            h.assertTrue(player.getYRot() == yaw && player.getXRot() == pitch,
                    "The server must hold the initial view throughout focus");
            for (int tick = 2; tick < 59; tick++) OozaruTransformationSystem.tick(player, data);
            h.assertTrue(data.activeTransformation().isBase() && OozaruTransformationSystem.isFocusing(player),
                    "Transformation must wait the full sixty ticks");
            OozaruTransformationSystem.tick(player, data);
            h.assertTrue(data.activeTransformation() == KiTransformation.OOZARU
                            && !OozaruTransformationSystem.isFocusing(player),
                    "After three seconds the player must transform and release the view lock");
            h.getLevel().setBlockAndUpdate(roof, Blocks.STONE.defaultBlockState());
            OozaruTransformationSystem.tick(player, data);
            h.assertTrue(data.activeTransformation() == KiTransformation.OOZARU, "A roof must not cancel an existing Oozaru form");
            h.getLevel().setDayTime(0L);
            OozaruTransformationSystem.tick(player, data);
            h.assertTrue(data.activeTransformation().isBase(), "Dawn must still end Oozaru");
            h.succeed();
        } finally {
            OozaruTransformationSystem.clear(player);
            h.getLevel().setBlockAndUpdate(roof, originalRoof);
            h.getLevel().setDayTime(originalTime);
        }
    }

    @GameTest(template = "melee_arena")
    public static void interruptedFocusNeverSurvivesLifecycleOrTailLoss(GameTestHelper h) {
        long originalTime = h.getLevel().getDayTime();
        FakePlayer player = player(h);
        PlayerData data = data(player);
        player.setPos(player.getX(), h.getLevel().getMaxBuildHeight() + 2, player.getZ());
        try {
            h.getLevel().setDayTime(18000L);
            start(h, player, data);
            data.setHasTail(false);
            OozaruTransformationSystem.tick(player, data);
            h.assertTrue(!OozaruTransformationSystem.isFocusing(player) && data.activeTransformation().isBase(),
                    "Losing a tail must cancel focus without transforming");
            data.setHasTail(true);
            start(h, player, data);
            PlayerDataEvents.logout(new PlayerEvent.PlayerLoggedOutEvent(player));
            h.assertTrue(!OozaruTransformationSystem.isFocusing(player), "Logout must release focus");
            start(h, player, data);
            PlayerDataEvents.changedDimension(new PlayerEvent.PlayerChangedDimensionEvent(player, Level.OVERWORLD, Level.NETHER));
            h.assertTrue(!OozaruTransformationSystem.isFocusing(player), "Changing dimensions must release focus");
            start(h, player, data);
            PlayerDataEvents.death(new LivingDeathEvent(player, player.damageSources().generic()));
            h.assertTrue(!OozaruTransformationSystem.isFocusing(player), "Death must release focus");
            PlayerDataEvents.logout(new PlayerEvent.PlayerLoggedOutEvent(player));
            h.succeed();
        } finally { h.getLevel().setDayTime(originalTime); OozaruTransformationSystem.clear(player); }
    }

    @GameTest(template = "melee_arena")
    public static void focusPacketPreservesRotationAndRelease(GameTestHelper h) {
        for (int ticks : new int[]{60, 40, 20, 0}) {
            var expected = new OozaruFocusPacket(123, ticks, -90.0F, -23.5F);
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                expected.encode(buffer);
                h.assertTrue(expected.equals(new OozaruFocusPacket(buffer)), "Focus and unlock must survive the network round trip");
            } finally { buffer.release(); }
        }
        h.succeed();
    }

    private static void start(GameTestHelper h, FakePlayer player, PlayerData data) {
        aim(player, OozaruTransformationSystem.moonDirection(h.getLevel()));
        OozaruTransformationSystem.tick(player, data);
        h.assertTrue(OozaruTransformationSystem.isFocusing(player), "Setup must start focus");
    }

    private static FakePlayer player(GameTestHelper h) {
        FakePlayer player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "MoonTest"));
        Vec3 position = h.absoluteVec(new Vec3(2.5, 1, 2.5));
        player.setPos(position);
        player.setHealth(20);
        data(player).setRace(KiRace.SAIYAN);
        data(player).setLegendarySaiyan(false);
        data(player).setHasTail(true);
        return player;
    }

    private static PlayerData data(FakePlayer player) {
        return player.getCapability(PlayerDataCapability.PLAYER_DATA).orElseThrow(() -> new AssertionError("Missing capability"));
    }

    private static void aim(FakePlayer player, Vec3 direction) {
        player.setYRot((float) Math.toDegrees(Math.atan2(-direction.x, direction.z)));
        player.setXRot((float) -Math.toDegrees(Math.atan2(direction.y, direction.horizontalDistance())));
    }
}
