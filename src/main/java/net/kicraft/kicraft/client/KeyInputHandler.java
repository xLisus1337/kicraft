package net.kicraft.kicraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.kicraft.kicraft.Kicraft;
import net.kicraft.kicraft.capability.PlayerDataProvider;
import net.kicraft.kicraft.network.PacketHandler;
import net.kicraft.kicraft.network.TurboModeC2SPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Kicraft.MODID, value = Dist.CLIENT)
public class KeyInputHandler {
        public static final String CATEGORY = "Kicraft Controls";

        public static final KeyMapping CHARGE_KI_KEY = new KeyMapping("Charge Ki", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY);
        public static final KeyMapping INSTANT_TRANSMISSION_KEY = new KeyMapping("Instant Transmission", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);
        public static final KeyMapping KI_SENSE_SHORT_KEY = new KeyMapping("Ki Sense (Short)", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);
        public static final KeyMapping STAT_MENU_KEY = new KeyMapping("Open Stats Menu", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, CATEGORY);
        public static final KeyMapping TURBO_MODE_KEY = new KeyMapping("Toggle Turbo Mode", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);

        private static boolean isKiSenseActive = false;
        private static int updateTickCounter = 0;

        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null || mc.screen != null) return;

                // Menu statystyk (M)
                if (STAT_MENU_KEY.consumeClick()) {
                        mc.setScreen(new CharacterMenuScreen());
                }

                // --- TURBO MODE TOGGLE (R) ---
                if (TURBO_MODE_KEY.consumeClick()) {
                        mc.player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                                boolean newState = !data.isTurboMode();
                                PacketHandler.sendToServer(new TurboModeC2SPacket(newState));

                                String msg = newState ? "§c§lTURBO MODE: ON" : "§7§lTURBO MODE: OFF";
                                mc.player.displayClientMessage(Component.literal(msg), true);
                        });
                }

                // --- LOGIKA INSTANT TRANSMISSION (B) ---
                while (INSTANT_TRANSMISSION_KEY.consumeClick()) {
                        mc.player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                                if (!data.hasInstantTransmission()) {
                                        mc.player.displayClientMessage(Component.literal("§cYou haven't mastered Instant Transmission!"), true);
                                        return;
                                }
                                if (mc.player.isCrouching()) {
                                        mc.setScreen(new SenseKiScreen());
                                } else {
                                        mc.player.displayClientMessage(Component.literal("You must crouch (Shift) to focus!"), true);
                                }
                        });
                }

                // --- LOGIKA KI SENSE SHORT (V) ---
                while (KI_SENSE_SHORT_KEY.consumeClick()) {
                        mc.player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                                if (!data.hasKiSense()) {
                                        mc.player.displayClientMessage(Component.literal("§cYour Ki sensing is not developed yet!"), true);
                                        return;
                                }
                                isKiSenseActive = !isKiSenseActive;
                                if (isKiSenseActive) KiSenseHandlerClient.updateOutlines(mc.player);
                                else KiSenseHandlerClient.sendCleanup(mc.player);
                        });
                }
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
                Minecraft mc = Minecraft.getInstance();
                if (event.phase != TickEvent.Phase.END || mc.player == null || mc.isPaused()) return;

                if (isKiSenseActive) {
                        updateTickCounter++;
                        if (updateTickCounter >= 10) {
                                updateTickCounter = 0;
                                KiSenseHandlerClient.updateOutlines(mc.player);
                        }
                }
        }
}