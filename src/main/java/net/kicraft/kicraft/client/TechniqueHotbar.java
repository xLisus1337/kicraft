package net.kicraft.kicraft.client;

import net.kicraft.kicraft.Kicraft;
import net.kicraft.kicraft.core.player.KiTechnique;
import net.kicraft.kicraft.network.KiBlastPacket;
import net.kicraft.kicraft.network.KicraftNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Kicraft.MODID, value = Dist.CLIENT)
public final class TechniqueHotbar {
    public static final int SLOT_COUNT = TechniqueHotbarState.SLOT_COUNT;
    private static final TechniqueHotbarState STATE = new TechniqueHotbarState();
    private static LocalPlayer inputPlayer;
    private static ClientLevel inputLevel;
    private static int selectionNameTicks;

    private TechniqueHotbar() { }

    private static boolean playerReady(Minecraft minecraft) {
        return minecraft.player != null && minecraft.level != null && minecraft.player.isAlive()
                && !minecraft.player.isSpectator() && ClientKiData.hasReceivedInitialSync()
                && ClientKiData.data().isRaceChosen();
    }

    private static boolean acceptsInput(Minecraft minecraft) {
        return playerReady(minecraft) && minecraft.screen == null && minecraft.getOverlay() == null
                && minecraft.isWindowActive() && !minecraft.isPaused();
    }

    public static boolean visible() {
        return playerReady(Minecraft.getInstance());
    }

    public static boolean isUseModifierDown() {
        return Screen.hasControlDown();
    }

    public static int selectedSlot() {
        return STATE.selectedSlot();
    }

    private static void showSelectionName() {
        selectionNameTicks = (int) (40.0D * Minecraft.getInstance().options.notificationDisplayTime().get());
    }

    public static int selectionNameAlpha(float partialTick) {
        return Math.max(0, Math.min(255, (int) ((selectionNameTicks - partialTick) * 256.0F / 10.0F)));
    }

    public static void assign(int slot, KiTechnique technique) {
        if (technique != null && (!ClientSettings.isHotbarTechnique(technique)
                || !ClientKiData.data().hasTechnique(technique))) return;
        KiTechnique previous = ClientSettings.techniqueSlot(slot);
        ClientSettings.setTechniqueSlot(slot, technique);
        if (slot == STATE.selectedSlot() && previous != technique) showSelectionName();
    }

    @SubscribeEvent
    public static void scroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!acceptsInput(minecraft) || !isUseModifierDown() || event.getScrollDelta() == 0.0) return;
        event.setCanceled(true);
        int previous = STATE.selectedSlot();
        STATE.scroll(event.getScrollDelta());
        if (STATE.selectedSlot() != previous) showSelectionName();
    }

    private static void useSelected(Minecraft minecraft) {
        if (!acceptsInput(minecraft) || MeleeInput.recovering()) return;
        KiTechnique technique = ClientSettings.techniqueSlot(STATE.selectedSlot());
        if (technique == null || !ClientKiData.data().hasTechnique(technique)) {
            showSelectionName();
            return;
        }
        if (technique == KiTechnique.KI_BLAST) {
            KicraftNetwork.sendToServer(new KiBlastPacket());
        }
    }

    /** Ctrl + RMB uses the selected Ki attack immediately. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void mouseButton(InputEvent.MouseButton.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getAction() != GLFW.GLFW_PRESS || !isUseModifierDown()
                || !minecraft.options.keyUse.matchesMouse(event.getButton()) || !acceptsInput(minecraft)) return;
        while (minecraft.options.keyUse.consumeClick()) { }
        useSelected(minecraft);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != inputPlayer || minecraft.level != inputLevel) {
            inputPlayer = minecraft.player;
            inputLevel = minecraft.level;
            STATE.reset();
            selectionNameTicks = 0;
        }
        if (!minecraft.isPaused() && selectionNameTicks > 0) selectionNameTicks--;
        if (!acceptsInput(minecraft) || !isUseModifierDown()) return;

        boolean clicked = false;
        while (minecraft.options.keyUse.consumeClick()) clicked = true;
        if (clicked) useSelected(minecraft);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interaction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.isUseItem() && isUseModifierDown() && acceptsInput(minecraft)) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }
}
