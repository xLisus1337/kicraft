package net.kicraft.kicraft.server;

import net.kicraft.kicraft.Kicraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Removes persisted highlights from older versions; new scans exist only on the viewer's client. */
@Mod.EventBusSubscriber(modid = Kicraft.MODID)
public final class KiSenseHighlightEvents {
    private static final String EXPIRES_AT_TAG = "KicraftKiSenseHighlightExpiresAt";
    private static final String WAS_GLOWING_TAG = "KicraftKiSenseWasGlowing";
    private static final String PREVIOUS_TEAM_TAG = "KicraftKiSensePreviousTeam";

    private KiSenseHighlightEvents() { }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) { clearLegacy(event.getEntity()); }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) { clearLegacy(event.getEntity()); }

    public static void clearLegacy(LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        Scoreboard scoreboard = entity.level().getScoreboard();
        PlayerTeam current = scoreboard.getPlayersTeam(entity.getScoreboardName());
        boolean legacyTeam = current != null && isKiSenseTeam(current.getName());
        if (legacyTeam) scoreboard.removePlayerFromTeam(entity.getScoreboardName(), current);
        if (!entity.getPersistentData().contains(EXPIRES_AT_TAG)) return;

        // Never overwrite a real team assigned while a legacy scan was active.
        if (current == null || legacyTeam) {
            PlayerTeam previous = scoreboard.getPlayerTeam(entity.getPersistentData().getString(PREVIOUS_TEAM_TAG));
            if (previous != null && !isKiSenseTeam(previous.getName()))
                scoreboard.addPlayerToTeam(entity.getScoreboardName(), previous);
        }
        if (!entity.getPersistentData().getBoolean(WAS_GLOWING_TAG)) entity.setGlowingTag(false);
        entity.getPersistentData().remove(EXPIRES_AT_TAG);
        entity.getPersistentData().remove(WAS_GLOWING_TAG);
        entity.getPersistentData().remove(PREVIOUS_TEAM_TAG);
    }

    private static boolean isKiSenseTeam(String name) {
        return "kcSenseGreen".equals(name) || "kcSenseGray".equals(name)
                || "kcSenseRed".equals(name) || "kcSenseBlack".equals(name);
    }
}
