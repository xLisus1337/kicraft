package net.kicraft.kicraft; // Upewnij się, że to pasuje do folderu, w którym to stworzyłeś (np. dodaj .network na końcu, jeśli wrzuciłeś to do network)

import net.kicraft.kicraft.network.KiSenseUpdateC2SPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class KiSenseHandlerServer {

    // Nazwy drużyn
    private static final String TEAM_WEAK = "ki_weak";
    private static final String TEAM_STRONG = "ki_strong";
    private static final String TEAM_EQUAL = "ki_equal";

    // Śledzimy, które podmioty są obecnie "kolorowane"
    private static final Set<Integer> activelyColoredEntities = new HashSet<>();

    public static void handleUpdate(ServerPlayer player, Map<Integer, KiSenseUpdateC2SPacket.SenseCategory> entitiesMap) {
        Scoreboard scoreboard = player.level().getScoreboard();

        // 1. Upewniamy się, że drużyny istnieją
        createTeamIfMissing(scoreboard, TEAM_WEAK, ChatFormatting.GREEN);
        createTeamIfMissing(scoreboard, TEAM_STRONG, ChatFormatting.RED);
        createTeamIfMissing(scoreboard, TEAM_EQUAL, ChatFormatting.GRAY); // Szary dla podobnego poziomu

        // 2. Przetwarzamy nowe podmioty
        Set<Integer> currentFrameEntities = new HashSet<>();

        for (Map.Entry<Integer, KiSenseUpdateC2SPacket.SenseCategory> entry : entitiesMap.entrySet()) {
            int entityId = entry.getKey();
            KiSenseUpdateC2SPacket.SenseCategory category = entry.getValue();
            Entity target = player.level().getEntity(entityId);

            if (target instanceof LivingEntity living) {
                String teamName;
                switch (category) {
                    case STRONG: teamName = TEAM_STRONG; break;
                    case WEAK: teamName = TEAM_WEAK; break;
                    default: teamName = TEAM_EQUAL; break;
                }

                // Przypisujemy do drużyny i włączamy glowing tag
                scoreboard.addPlayerToTeam(living.getScoreboardName(), scoreboard.getPlayerTeam(teamName));
                living.setGlowingTag(true);

                currentFrameEntities.add(entityId);
                activelyColoredEntities.add(entityId);
            }
        }

        // 3. Czyścimy podmioty, które zniknęły z radaru w tym ticku
        Set<Integer> entitiesToRemove = new HashSet<>(activelyColoredEntities);
        entitiesToRemove.removeAll(currentFrameEntities);

        for (Integer entityId : entitiesToRemove) {
            Entity oldTarget = player.level().getEntity(entityId);
            if (oldTarget instanceof LivingEntity living) {
                living.setGlowingTag(false);
                if (living.getTeam() != null && living.getTeam().getName().startsWith("ki_")) {
                    scoreboard.removePlayerFromTeam(living.getScoreboardName(), (PlayerTeam) living.getTeam());
                }
            }
            activelyColoredEntities.remove(entityId);
        }
    }

    private static void createTeamIfMissing(Scoreboard scoreboard, String name, ChatFormatting color) {
        if (scoreboard.getPlayerTeam(name) == null) {
            PlayerTeam team = scoreboard.addPlayerTeam(name);
            team.setColor(color);
            team.setSeeFriendlyInvisibles(false);
        }
    }
}