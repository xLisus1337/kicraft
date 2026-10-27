package net.kicraft.kicraft.client;

import net.kicraft.kicraft.Kicraft;
import net.kicraft.kicraft.core.combat.CombatFeedback;
import net.kicraft.kicraft.network.KicraftNetwork;
import net.kicraft.kicraft.network.MeleeInputPacket;
import net.kicraft.kicraft.network.MeleeStatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Kicraft.MODID, value = Dist.CLIENT)
public final class MeleeInput {
    // The custom mouse-combat input path remains disabled independently of world setup.
    private static final boolean CUSTOM_MOUSE_PUNCHES_ENABLED = net.kicraft.kicraft.core.combat.CombatRules.CUSTOM_MELEE_ENABLED;
    private static final int PREDICTED_RECOVERY_TICKS = 6;
    private static int recoveryTicks;
    private static int exposedTicks;
    private static int counterTicks;
    private static int feedbackTicks;
    private static CombatFeedback feedback = CombatFeedback.NONE;
    private static ClientLevel level;
    private static int playerId = -1;

    private MeleeInput() { }

    public static boolean recovering() { return recoveryTicks > 0; }
    public static boolean exposed() { return exposedTicks > 0; }
    public static boolean counterReady() { return counterTicks > 0; }
    public static int recoveryTicks() { return recoveryTicks; }
    public static CombatFeedback feedback() { return feedbackTicks > 0 ? feedback : CombatFeedback.NONE; }

    public static void receive(MeleeStatePacket packet) {
        recoveryTicks = Math.max(0, packet.recovery());
        exposedTicks = Math.max(0, packet.exposed());
        counterTicks = Math.max(0, packet.counter());
        if (packet.feedback() != CombatFeedback.NONE) {
            feedback = packet.feedback();
            feedbackTicks = switch (feedback) {
                case PARRY, COUNTER, POWERFUL_HIT -> 12;
                case MISS, PARRIED, GUARD_BREAK -> Math.max(1, packet.exposed());
                default -> 20;
            };
        }
    }

    private static boolean rightPunchAvailable() {
        // Ctrl + RMB belongs to the Ki-attack bar; plain RMB remains the right punch.
        return !TechniqueHotbar.isUseModifierDown();
    }

    private static boolean acceptsInput(Minecraft minecraft) {
        return minecraft.player != null && minecraft.player.isAlive() && !minecraft.player.isSpectator()
                && !minecraft.player.isSleeping() && !minecraft.player.isHandsBusy() && !minecraft.player.isUsingItem()
                && ClientKiData.hasReceivedInitialSync() && ClientKiData.data().isRaceChosen()
                && minecraft.screen == null && minecraft.getOverlay() == null
                && minecraft.isWindowActive() && !minecraft.isPaused();
    }

    private static boolean canPunchHere(Minecraft minecraft) {
        // Mining and block/entity interactions on the item bar keep vanilla behavior.
        return minecraft.hitResult != null && (minecraft.hitResult.getType() == HitResult.Type.MISS
                || minecraft.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity);
    }

    private static boolean punch(Minecraft minecraft, MeleeInputPacket.AttackSide side) {
        if (!acceptsInput(minecraft) || recovering() || !canPunchHere(minecraft)) return false;
        if (minecraft.gameMode != null) minecraft.gameMode.stopDestroyBlock();
        InteractionHand hand = side.interactionHand(minecraft.player.getMainArm());
        minecraft.player.swing(hand);
        KicraftNetwork.sendToServer(new MeleeInputPacket(side));
        recoveryTicks = PREDICTED_RECOVERY_TICKS;
        return true;
    }

    /** Mouse presses attack immediately; releasing or holding the button has no combat effect. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void mouseButton(InputEvent.MouseButton.Post event) {
        if (!CUSTOM_MOUSE_PUNCHES_ENABLED) return;
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        Minecraft minecraft = Minecraft.getInstance();
        MeleeInputPacket.AttackSide side;
        if (minecraft.options.keyAttack.matchesMouse(event.getButton())) {
            side = MeleeInputPacket.AttackSide.LEFT;
        } else if (minecraft.options.keyUse.matchesMouse(event.getButton()) && rightPunchAvailable()) {
            side = MeleeInputPacket.AttackSide.RIGHT;
        } else {
            return;
        }
        if (!acceptsInput(minecraft) || !canPunchHere(minecraft)) return;
        // The raw callback owns this mouse press, so its queued key click cannot repeat it next tick.
        if (side == MeleeInputPacket.AttackSide.LEFT) {
            while (minecraft.options.keyAttack.consumeClick()) { }
        } else {
            while (minecraft.options.keyUse.consumeClick()) { }
        }
        punch(minecraft, side);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft minecraft = Minecraft.getInstance();
        int currentId = minecraft.player == null ? -1 : minecraft.player.getId();
        if (level != minecraft.level || playerId != currentId || minecraft.player == null || !minecraft.player.isAlive()) {
            recoveryTicks = exposedTicks = counterTicks = feedbackTicks = 0;
            level = minecraft.level;
            playerId = currentId;
            return;
        }
        if (!minecraft.isPaused()) {
            recoveryTicks = Math.max(0, recoveryTicks - 1);
            exposedTicks = Math.max(0, exposedTicks - 1);
            counterTicks = Math.max(0, counterTicks - 1);
            feedbackTicks = Math.max(0, feedbackTicks - 1);
        }
        if (!CUSTOM_MOUSE_PUNCHES_ENABLED) return;
        if (!acceptsInput(minecraft) || !canPunchHere(minecraft)) return;

        boolean leftClicked = false;
        while (minecraft.options.keyAttack.consumeClick()) leftClicked = true;
        if (leftClicked) {
            punch(minecraft, MeleeInputPacket.AttackSide.LEFT);
            return;
        }
        if (!rightPunchAvailable()) return;
        boolean rightClicked = false;
        while (minecraft.options.keyUse.consumeClick()) rightClicked = true;
        if (rightClicked) punch(minecraft, MeleeInputPacket.AttackSide.RIGHT);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interaction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!CUSTOM_MOUSE_PUNCHES_ENABLED) return;
        Minecraft minecraft = Minecraft.getInstance();
        boolean meleeInput = acceptsInput(minecraft) && canPunchHere(minecraft);
        if (event.isAttack() && meleeInput) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
        if (event.isUseItem() && rightPunchAvailable() && meleeInput) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }
}
