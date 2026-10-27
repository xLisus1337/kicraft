package net.kicraft.kicraft.server;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder("kicraft")
@PrefixGameTestTemplate(false)
public final class BugReportGameTests {
    @GameTest(template = "melee_arena")
    public static void reportCooldownSurvivesLogoutAndExpires(GameTestHelper h) {
        UUID owner = UUID.randomUUID(), other = UUID.randomUUID();
        FakePlayer original = new FakePlayer(h.getLevel(), new GameProfile(owner, "Reporter"));
        try {
            DiscordBugReportService.recordReport(owner, 100L, 1200);
            PlayerDataEvents.logout(new PlayerEvent.PlayerLoggedOutEvent(original));
            FakePlayer reconnected = new FakePlayer(h.getLevel(), new GameProfile(owner, "Reporter"));
            h.assertTrue(DiscordBugReportService.remainingCooldownTicks(reconnected.getUUID(), 120L) == 1180L,
                    "Reconnecting with the same UUID must retain the report cooldown");
            h.assertTrue(DiscordBugReportService.remainingCooldownTicks(other, 120L) == 0L,
                    "One player's report must not block another player");
            h.assertTrue(DiscordBugReportService.remainingCooldownTicks(owner, 1299L) == 1L
                            && DiscordBugReportService.remainingCooldownTicks(owner, 1300L) == 0L,
                    "The report cooldown must expire at its original deadline");
            DiscordBugReportService.recordReport(other, 1301L, 300);
            h.assertTrue(DiscordBugReportService.remainingCooldownTicks(owner, 1301L) == 0L,
                    "Pruning expired reports must allow the original player to report again");
            DiscordBugReportService.clearAll();
            h.assertTrue(DiscordBugReportService.remainingCooldownTicks(other, 0L) == 0L,
                    "Server shutdown cleanup must not carry deadlines into a new server clock");
            h.succeed();
        } finally {
            DiscordBugReportService.clearAll();
        }
    }

    @GameTest(template = "melee_arena")
    public static void reportPayloadPreservesNewlinesAndEscapesUserText(GameTestHelper h) {
        String report = "First line\nSecond \"quoted\" line\\path\t@everyone";
        var payload = JsonParser.parseString(DiscordBugReportService.reportPayload("Player`Name", report))
                .getAsJsonObject();
        h.assertTrue(payload.get("content").getAsString().equals(
                        "**Ki Craft bug report**\n**Player:** `Player'Name`\n\n" + report),
                "Decoded Discord content must contain real line breaks and unchanged report text");
        h.assertTrue(payload.getAsJsonObject("allowed_mentions").getAsJsonArray("parse").isEmpty(),
                "Player-written reports must not enable Discord mentions");
        h.succeed();
    }
}
