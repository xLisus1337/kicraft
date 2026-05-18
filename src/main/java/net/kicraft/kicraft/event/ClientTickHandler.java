package net.kicraft.kicraft.event;

import net.kicraft.kicraft.Kicraft;
import net.kicraft.kicraft.capability.PlayerDataProvider;
import net.kicraft.kicraft.client.KeyInputHandler;
import net.kicraft.kicraft.network.ChargeKiC2SPacket;
import net.kicraft.kicraft.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Kicraft.MODID, value = Dist.CLIENT)
public class ClientTickHandler {

    private static boolean wasCharging = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            // 1. Blokada sprintu (zostaje bez zmian)
            mc.player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                if (data.getStamina() <= 0 && mc.player.isSprinting()) {
                    mc.player.setSprinting(false);
                }
            });

            // 2. Jeśli otwarte menu - tylko Ki przerywamy (Ki musi być na trzymanie)
            if (mc.screen != null) {
                if (wasCharging) {
                    PacketHandler.sendToServer(new ChargeKiC2SPacket(false));
                    wasCharging = false;
                }
                return;
            }

            // 3. Obsługa ładowania KI (C) - to zostaje tutaj, bo to HOLD
            boolean isChargingNow = KeyInputHandler.CHARGE_KI_KEY.isDown();
            if (isChargingNow != wasCharging) {
                PacketHandler.sendToServer(new ChargeKiC2SPacket(isChargingNow));
                wasCharging = isChargingNow;
            }

            // LOGIKA TURBO (R) ZOSTAŁA PRZENIESIONA DO KeyInputHandler.java
            // DZIĘKI TEMU DZIAŁA JAKO PRZEŁĄCZNIK (TOGGLE)
        }
    }
}