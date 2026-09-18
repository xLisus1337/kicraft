package net.kicraft.kicraft.server;

import net.minecraftforge.common.ForgeConfigSpec;

/** Server-owned settings. Secrets such as Discord webhooks never reach clients. */
public final class ServerSettings {
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue DISCORD_BUG_REPORTS_ENABLED;
    private static final ForgeConfigSpec.ConfigValue<String> DISCORD_WEBHOOK_URL;
    private static final ForgeConfigSpec.IntValue BUG_REPORT_COOLDOWN_SECONDS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("discord");
        DISCORD_BUG_REPORTS_ENABLED = builder
                .comment("Allows players to submit Ki Craft bug reports to the configured Discord webhook.")
                .define("bugReportsEnabled", false);
        DISCORD_WEBHOOK_URL = builder
                .comment("Discord webhook URL. Keep this server-side file private and never share the URL with players.")
                .define("bugReportWebhookUrl", "");
        BUG_REPORT_COOLDOWN_SECONDS = builder
                .comment("Minimum delay between reports from the same player.")
                .defineInRange("bugReportCooldownSeconds", 60, 15, 3600);
        builder.pop();

        SPEC = builder.build();
    }

    private ServerSettings() {
    }

    public static boolean discordBugReportsEnabled() {
        return DISCORD_BUG_REPORTS_ENABLED.get() && !discordWebhookUrl().isBlank();
    }

    public static String discordWebhookUrl() {
        return DISCORD_WEBHOOK_URL.get().trim();
    }

    public static int bugReportCooldownTicks() {
        return BUG_REPORT_COOLDOWN_SECONDS.get() * 20;
    }
}
