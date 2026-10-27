package net.kicraft.kicraft.server;

import net.kicraft.kicraft.Kicraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Validates and forwards player-written reports without exposing a webhook to clients. */
public final class DiscordBugReportService {
    private static final int MAX_REPORT_LENGTH = 900;
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    private static final Map<UUID, Long> NEXT_REPORT_TICK = new HashMap<>();

    private DiscordBugReportService() {
    }

    public static void submit(ServerPlayer player, String rawReport) {
        String report = normalize(rawReport);
        if (report.isBlank()) {
            player.sendSystemMessage(Component.translatable("message.kicraft.bug_report.empty"));
            return;
        }
        if (!ServerSettings.discordBugReportsEnabled()) {
            player.sendSystemMessage(Component.translatable("message.kicraft.bug_report.unavailable"));
            return;
        }

        long gameTime = player.server.overworld().getGameTime();
        long remaining = remainingCooldownTicks(player.getUUID(), gameTime);
        if (remaining > 0L) {
            long seconds = Math.max(1L, (remaining + 19L) / 20L);
            player.sendSystemMessage(Component.translatable("message.kicraft.bug_report.cooldown", seconds));
            return;
        }

        URI webhook = validatedWebhook(ServerSettings.discordWebhookUrl());
        if (webhook == null) {
            Kicraft.LOGGER.warn("Discord bug reports are enabled, but the configured webhook URL is invalid.");
            player.sendSystemMessage(Component.translatable("message.kicraft.bug_report.unavailable"));
            return;
        }

        recordReport(player.getUUID(), gameTime, ServerSettings.bugReportCooldownTicks());
        HttpRequest request = HttpRequest.newBuilder(webhook)
                .timeout(Duration.ofSeconds(12))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(reportPayload(player.getGameProfile().getName(), report)))
                .build();

        HTTP.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .whenComplete((response, error) -> {
                    if (error != null) {
                        Kicraft.LOGGER.warn("Could not deliver Ki Craft bug report to Discord.", error);
                    } else if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        Kicraft.LOGGER.warn("Discord rejected a Ki Craft bug report with HTTP {}.", response.statusCode());
                    }
                });
        player.sendSystemMessage(Component.translatable("message.kicraft.bug_report.queued"));
    }

    // Cooldowns belong to UUIDs, not connections: logging out must not reset them.
    static long remainingCooldownTicks(UUID playerId, long gameTime) {
        return Math.max(0L, NEXT_REPORT_TICK.getOrDefault(playerId, 0L) - gameTime);
    }

    static void recordReport(UUID playerId, long gameTime, int cooldownTicks) {
        pruneExpiredReports(gameTime);
        NEXT_REPORT_TICK.put(playerId, gameTime + cooldownTicks);
    }

    public static void clearAll() {
        NEXT_REPORT_TICK.clear();
    }

    static String reportPayload(String playerName, String report) {
        String content = "**Ki Craft bug report**\n"
                + "**Player:** `" + escapeInline(playerName) + "`\n\n"
                + report;
        return toDiscordPayload(content);
    }

    private static void pruneExpiredReports(long gameTime) {
        Iterator<Map.Entry<UUID, Long>> iterator = NEXT_REPORT_TICK.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue() <= gameTime) {
                iterator.remove();
            }
        }
    }

    private static String normalize(String rawReport) {
        if (rawReport == null) {
            return "";
        }
        StringBuilder normalized = new StringBuilder(Math.min(rawReport.length(), MAX_REPORT_LENGTH));
        for (int index = 0; index < rawReport.length() && normalized.length() < MAX_REPORT_LENGTH; index++) {
            char character = rawReport.charAt(index);
            if (character == '\n' || character == '\r' || character == '\t' || !Character.isISOControl(character)) {
                normalized.append(character == '\r' ? '\n' : character);
            }
        }
        return normalized.toString().trim();
    }

    private static URI validatedWebhook(String value) {
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                return null;
            }
            String host = uri.getHost().toLowerCase(java.util.Locale.ROOT);
            return (host.equals("discord.com") || host.endsWith(".discord.com") || host.equals("discordapp.com"))
                    ? uri : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String escapeInline(String value) {
        return value.replace("`", "'");
    }

    private static String toDiscordPayload(String content) {
        return "{\"username\":\"Ki Craft Reports\",\"allowed_mentions\":{\"parse\":[]},\"content\":\""
                + escapeJson(content) + "\"}";
    }

    private static String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> escaped.append(character);
            }
        }
        return escaped.toString();
    }
}
