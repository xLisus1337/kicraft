package net.kicraft.kicraft.client;

import net.kicraft.kicraft.capability.PlayerDataProvider;
import net.kicraft.kicraft.network.KiSenseUpdateC2SPacket;
import net.kicraft.kicraft.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KiSenseHandlerClient {

    public static void updateOutlines(Player player) {
        player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
            // 1.TWOJE BP
            double myBP = (data.getMaxKi() + data.getMaxStamina()) * (1 + data.getStrength() * 0.1);

            // 2.SZUKAMY (48 bloków)
            double range = 48.0;
            AABB area = player.getBoundingBox().inflate(range);
            List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, area);

            // Mapa do wysłania
            Map<Integer, KiSenseUpdateC2SPacket.SenseCategory> entitiesMap = new HashMap<>();

            for (LivingEntity target : entities) {
                if (target == player) continue;

                double targetBP = 0;

                // 3.GRACZ CAPABILITY
                if (target instanceof Player targetPlayer) {
                    targetBP = targetPlayer.getCapability(PlayerDataProvider.PLAYER_DATA).map(tData ->
                            (tData.getMaxKi() + tData.getMaxStamina()) * (1 + tData.getStrength() * 0.1)
                    ).orElse(0.0);
                }
                // 4.MOB BP
                else {
                    double hp = target.getMaxHealth();
                    double dmg = 2.0;
                    if (target.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
                        dmg = target.getAttributeValue(Attributes.ATTACK_DAMAGE);
                    }
                    targetBP = (hp * 10) * (1 + dmg * 0.2);
                }

                // 5.PORÓWNANIE I KATEGORIA
                KiSenseUpdateC2SPacket.SenseCategory category;
                if (targetBP > myBP * 1.5) {
                    category = KiSenseUpdateC2SPacket.SenseCategory.STRONG;
                } else if (targetBP < myBP * 0.5) {
                    category = KiSenseUpdateC2SPacket.SenseCategory.WEAK;
                } else {
                    category = KiSenseUpdateC2SPacket.SenseCategory.EQUAL;
                }

                entitiesMap.put(target.getId(), category);
            }

            // --- WYSYŁAMY PAKIET Z MAPĄ DO SERWERA ---
            PacketHandler.sendToServer(new KiSenseUpdateC2SPacket(entitiesMap));
        });
    }

    // Pakiet cleanup (pusta mapa), żeby serwer wyłączył wszystko
    public static void sendCleanup(Player player) {
        PacketHandler.sendToServer(new KiSenseUpdateC2SPacket(new HashMap<>()));
    }
}