package net.kicraft.kicraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.kicraft.kicraft.Kicraft;
import net.kicraft.kicraft.capability.PlayerDataProvider;
import net.kicraft.kicraft.network.PacketHandler;
import net.kicraft.kicraft.network.TurboModeC2SPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = Kicraft.MODID, value = Dist.CLIENT)
public class KeyInputHandler {
    public static final String CATEGORY = "Kicraft Controls";

    public static final KeyMapping CHARGE_KI_KEY = new KeyMapping("Charge Ki", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY);
    public static final KeyMapping INSTANT_TRANSMISSION_KEY = new KeyMapping("Instant Transmission", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);
    public static final KeyMapping KI_SENSE_SHORT_KEY = new KeyMapping("Ki Sense (Short)", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);
    public static final KeyMapping STAT_MENU_KEY = new KeyMapping("Open Stats Menu", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, CATEGORY);
    public static final KeyMapping TURBO_MODE_KEY = new KeyMapping("Toggle Turbo Mode", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);
    public static final KeyMapping LOCK_ON_KEY = new KeyMapping("Lock-On Target", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F, CATEGORY);
    public static final KeyMapping SHOULDER_CAM_KEY = new KeyMapping("Toggle Shoulder Cam", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY);

    private static boolean isKiSenseActive = false;
    private static int updateTickCounter = 0;
    public static boolean isShoulderCamActive = false;
    public static Entity lockOnTarget = null;

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        // --- LOCK-ON (F) ---
        if (LOCK_ON_KEY.consumeClick()) {
            if (lockOnTarget != null) {
                lockOnTarget = null;
                mc.player.displayClientMessage(Component.literal("§7Lock-On: §cDisabled"), true);
            } else {
                lockOnTarget = findLockOnTarget(mc);
                if (lockOnTarget != null) {
                    mc.player.displayClientMessage(Component.literal("§7Lock-On: §a" + lockOnTarget.getDisplayName().getString()), true);
                }
            }
        }

        // --- SHOULDER CAM (I) ---
        if (SHOULDER_CAM_KEY.consumeClick()) {
            isShoulderCamActive = !isShoulderCamActive;
            if (isShoulderCamActive) {
                mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
            } else {
                mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            }
        }

        // Menu statystyk (M)
        if (STAT_MENU_KEY.consumeClick()) {
            mc.setScreen(new CharacterMenuScreen());
        }

        // --- TURBO MODE TOGGLE (R) ---
        if (TURBO_MODE_KEY.consumeClick()) {
            mc.player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                boolean newState = !data.isTurboMode();
                PacketHandler.sendToServer(new TurboModeC2SPacket(newState));
                mc.player.displayClientMessage(Component.literal("§7TURBO MODE: " + (newState ? "§aON" : "§cOFF")), true);
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

    private static Entity findLockOnTarget(Minecraft mc) {
        double range = 30.0;
        Vec3 eyePos = mc.player.getEyePosition(1.0F);
        Vec3 viewVec = mc.player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(viewVec.scale(range));

        AABB area = mc.player.getBoundingBox().inflate(range);
        Entity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : mc.level.getEntities(mc.player, area, e -> e instanceof LivingEntity && e.isAlive())) {
            AABB hitBox = entity.getBoundingBox().inflate(0.3);
            Optional<Vec3> hit = hitBox.clip(eyePos, endPos);
            
            if (hit.isPresent() || mc.player.distanceTo(entity) < 5.0) {
                double dist = mc.player.distanceTo(entity);
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = entity;
                }
            }
        }
        return closest;
    }

    private static float lerpDegrees(float start, float end, float pct) {
        float diff = ((end - start + 180) % 360) - 180;
        return start + diff * pct;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || mc.player == null || mc.isPaused()) return;

        // Synchronizacja Lock-On (czyszczenie celu jeśli zniknął/zginął)
        if (lockOnTarget != null) {
            if (!lockOnTarget.isAlive() || mc.player.distanceTo(lockOnTarget) > 40.0) {
                lockOnTarget = null;
            }
        }

        if (isKiSenseActive) {
            updateTickCounter++;
            if (updateTickCounter >= 10) {
                updateTickCounter = 0;
                KiSenseHandlerClient.updateOutlines(mc.player);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.START || mc.player == null || mc.isPaused()) return;

        // PŁYNNA LOGIKA NAMIERZANIA (Lock-On) - wykonywana co klatkę (FPS)
        if (lockOnTarget != null) {
            double dx = lockOnTarget.getX() - mc.player.getX();
            double dy = (lockOnTarget.getY() + lockOnTarget.getEyeHeight() * 0.5) - (mc.player.getY() + mc.player.getEyeHeight());
            double dz = lockOnTarget.getZ() - mc.player.getZ();
            double distAt = Math.sqrt(dx * dx + dz * dz);

            float targetYaw = (float) (Math.atan2(dz, dx) * (180 / Math.PI)) - 90.0F;
            float targetPitch = (float) (-(Math.atan2(dy, distAt) * (180 / Math.PI)));

            // Płynna interpolacja (0.25F dla szybkości i braku laga)
            mc.player.setYRot(lerpDegrees(mc.player.getYRot(), targetYaw, 0.25F));
            mc.player.setXRot(lerpDegrees(mc.player.getXRot(), targetPitch, 0.25F));
            
            // Ustawiamy również rotację "poprzednią", żeby Minecraft nie próbował interpolować po swojemu
            mc.player.yRotO = mc.player.getYRot();
            mc.player.xRotO = mc.player.getXRot();
        }
    }
}
