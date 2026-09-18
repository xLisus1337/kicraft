package net.kicraft.kicraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.kicraft.kicraft.Kicraft;
import net.kicraft.kicraft.core.player.FlightMovement;
import net.kicraft.kicraft.core.player.KiTransformation;
import net.kicraft.kicraft.network.BlockPacket;
import net.kicraft.kicraft.network.DashPacket;
import net.kicraft.kicraft.network.FlightControlPacket;
import net.kicraft.kicraft.network.KiChargePacket;
import net.kicraft.kicraft.network.KicraftNetwork;
import net.kicraft.kicraft.network.LockOnPacket;
import net.kicraft.kicraft.network.TransformationPacket;
import net.kicraft.kicraft.network.ToggleFlightPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Kicraft.MODID, value = Dist.CLIENT)
public final class ClientInputEvents {
    private static final double STAMINA_SPRINT_EMPTY_RATIO = 0.0;
    private static final double STAMINA_SPRINT_RESUME_RATIO = 0.05;
    private static final int DOUBLE_JUMP_WINDOW_TICKS = 7;
    private static final int TRANSFORM_HOLD_TICKS = 60;
    private static final int TRANSFORM_OFF_HOLD_TICKS = 20;
    private static final float LOCK_ON_PLAYER_YAW_STEP = 9.0F;
    private static final float LOCK_ON_PLAYER_PITCH_STEP = 7.0F;
    private static final float LOCK_ON_DEADZONE_DEGREES = 0.35F;
    private static boolean lastCharging;
    private static LocalPlayer inputPlayer;
    private static ClientLevel inputLevel;
    private static boolean lastBlocking;
    private static boolean lastJumpDown;
    private static int lastJumpPressTick = -1000;
    private static boolean staminaExhausted;
    private static boolean waitingForSprintRelease;
    private static boolean raceScreenOpened;
    private static int transformHoldTicks;
    private static int transformOffHoldTicks;
    private static KiTransformation selectedTransformation = KiTransformation.BASE;

    private ClientInputEvents() {
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != inputPlayer || minecraft.level != inputLevel) {
            inputPlayer = minecraft.player;
            inputLevel = minecraft.level;
            lastCharging = false;
            lastBlocking = false;
            lastJumpDown = false;
            lastJumpPressTick = -1000;
            staminaExhausted = false;
            waitingForSprintRelease = false;
            transformHoldTicks = 0;
            transformOffHoldTicks = 0;
            ClientLockOnData.clear();
            ClientFlightVisuals.reset();
            ClientFlightOrientation.reset();
        }
        if (minecraft.player == null) {
            ClientKiData.reset();
            raceScreenOpened = false;
            lastCharging = false;
            lastBlocking = false;
            lastJumpDown = false;
            lastJumpPressTick = -1000;
            staminaExhausted = false;
            waitingForSprintRelease = false;
            transformHoldTicks = 0;
            transformOffHoldTicks = 0;
            selectedTransformation = KiTransformation.BASE;
            ClientLockOnData.clear();
            ClientFlightVisuals.reset();
            ClientFlightOrientation.reset();
            return;
        }

        if (event.phase == TickEvent.Phase.START) {
            ClientFlightOrientation.beginTick(minecraft.player);
            return;
        }
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (ClientKiData.hasReceivedInitialSync() && !ClientKiData.data().isRaceChosen() && minecraft.screen == null && !raceScreenOpened) {
            raceScreenOpened = true;
            minecraft.setScreen(new RaceSelectionScreen());
            return;
        }

        if (ClientKiData.data().isRaceChosen()) {
            raceScreenOpened = false;
            if (minecraft.screen instanceof RaceSelectionScreen) {
                minecraft.setScreen(null);
            }
        }

        if (ClientKiData.data().race().hasInfiniteEnergy()) {
            staminaExhausted = false;
            waitingForSprintRelease = false;
        } else {
            double stamina = ClientKiData.data().stamina();
            double maxStamina = ClientKiData.data().maxStamina();
            if (stamina <= maxStamina * STAMINA_SPRINT_EMPTY_RATIO) {
                staminaExhausted = true;
                waitingForSprintRelease = true;
            } else if (stamina >= maxStamina * STAMINA_SPRINT_RESUME_RATIO && !isSprintPhysicallyHeld(minecraft)) {
                staminaExhausted = false;
                waitingForSprintRelease = false;
            }

            if (staminaExhausted || waitingForSprintRelease) {
                if (minecraft.player.isSprinting()) {
                    minecraft.player.setSprinting(false);
                }
                minecraft.options.keySprint.setDown(false);
            }
        }

        while (KicraftKeyMappings.OPEN_TRANSFORMATIONS.consumeClick()) {
            if (acceptsGameplayInput(minecraft)) minecraft.setScreen(new TransformationScreen());
        }

        while (KicraftKeyMappings.OPEN_TRAINING.consumeClick()) {
            if (acceptsGameplayInput(minecraft)) {
                minecraft.setScreen(new TrainingScreen());
            }
        }

        while (KicraftKeyMappings.DASH.consumeClick()) {
            if (!acceptsGameplayInput(minecraft)) continue;
            float forward = 0.0F;
            float strafe = 0.0F;
            if (minecraft.options.keyUp.isDown()) forward += 1.0F;
            if (minecraft.options.keyDown.isDown()) forward -= 1.0F;
            if (minecraft.options.keyRight.isDown()) strafe += 1.0F;
            if (minecraft.options.keyLeft.isDown()) strafe -= 1.0F;
            KicraftNetwork.sendToServer(new DashPacket(forward, strafe));
        }

        boolean blocking = isBlockRequested();
        if (blocking != lastBlocking) {
            KicraftNetwork.sendToServer(new BlockPacket(blocking));
            lastBlocking = blocking;
        }

        while (KicraftKeyMappings.LOCK_ON.consumeClick()) {
            if (acceptsGameplayInput(minecraft)) KicraftNetwork.sendToServer(new LockOnPacket(LockOnPacket.Action.TOGGLE));
        }

        while (KicraftKeyMappings.CYCLE_LOCK_ON.consumeClick()) {
            if (acceptsGameplayInput(minecraft) && ClientLockOnData.isLocked()) {
                KicraftNetwork.sendToServer(new LockOnPacket(LockOnPacket.Action.CYCLE));
            }
        }

        while (KicraftKeyMappings.SHOULDER_CAMERA.consumeClick()) {
            if (acceptsGameplayInput(minecraft)) ShoulderCameraState.cycle(minecraft);
        }

        handleTransformationHold(minecraft);

        handleFlightDoubleJump(minecraft);
        if (!ClientFlightStates.isFlying(minecraft.player)) {
            ClientFlightVisuals.stop(minecraft.player);
        }

        boolean charging = KicraftKeyMappings.CHARGE_KI.isDown() && acceptsGameplayInput(minecraft)
                && !MeleeInput.recovering();
        if (charging != lastCharging) {
            KicraftNetwork.sendToServer(new KiChargePacket(charging));
            lastCharging = charging;
        }

        alignPlayerToLockOnTarget(minecraft);
        ClientFlightOrientation.stabilizeRenderedBody(minecraft.player);
    }

    public static boolean isBlocking() {
        return isBlockRequested()
                && !MeleeInput.recovering()
                && (ClientKiData.data().race().hasInfiniteEnergy() || ClientKiData.data().stamina() > 0.0);
    }

    @SubscribeEvent
    public static void updateFlightMovement(MovementInputUpdateEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || event.getEntity() != minecraft.player
                || !ClientFlightStates.isFlying(minecraft.player)) {
            return;
        }

        float forward = Mth.clamp(event.getInput().forwardImpulse, -1.0F, 1.0F);
        float left = Mth.clamp(event.getInput().leftImpulse, -1.0F, 1.0F);
        float vertical = (event.getInput().jumping ? 1.0F : 0.0F)
                - (event.getInput().shiftKeyDown ? 1.0F : 0.0F);
        boolean boosted = forward > 0.0F
                && !staminaExhausted
                && isSprintPhysicallyHeld(minecraft);
        ClientFlightVisuals.update(minecraft.player, forward, left, boosted);
        Vec3 flightForward = ClientFlightOrientation.forward(minecraft.player);
        Vec3 flightLeft = ClientFlightOrientation.left(minecraft.player);
        Vec3 velocity = FlightMovement.calculateVelocity(
                minecraft.player.getDeltaMovement(),
                flightForward,
                flightLeft,
                forward,
                left,
                vertical,
                boosted
        );
        minecraft.player.setDeltaMovement(velocity);
        minecraft.player.fallDistance = 0.0F;

        ClientFlightOrientation.Attitude attitude = ClientFlightOrientation.attitude(minecraft.player, 1.0F);
        KicraftNetwork.sendToServer(new FlightControlPacket(
                forward,
                left,
                vertical,
                (float) flightForward.x,
                (float) flightForward.y,
                (float) flightForward.z,
                (float) flightLeft.x,
                (float) flightLeft.y,
                (float) flightLeft.z,
                attitude.yaw(),
                attitude.pitch(),
                boosted
        ));

        // Custom flight follows the camera. Suppress vanilla creative-flight input so
        // Space/Shift cannot add a second, unrelated vertical movement component.
        event.getInput().forwardImpulse = 0.0F;
        event.getInput().leftImpulse = 0.0F;
        event.getInput().up = false;
        event.getInput().down = false;
        event.getInput().left = false;
        event.getInput().right = false;
        event.getInput().jumping = false;
        event.getInput().shiftKeyDown = false;
    }

    private static boolean isBlockRequested() {
        return acceptsGameplayInput(Minecraft.getInstance()) && KicraftKeyMappings.BLOCK.isDown();
    }

    private static boolean acceptsGameplayInput(Minecraft minecraft) {
        return minecraft.player != null && minecraft.level != null && minecraft.player.isAlive()
                && !minecraft.player.isSleeping()
                && !minecraft.player.isSpectator() && minecraft.screen == null && minecraft.getOverlay() == null
                && minecraft.isWindowActive() && !minecraft.isPaused()
                && ClientKiData.hasReceivedInitialSync() && ClientKiData.data().isRaceChosen();
    }

    public static float transformHoldProgress() {
        return Math.max(
                transformHoldTicks / (float) TRANSFORM_HOLD_TICKS,
                transformOffHoldTicks / (float) TRANSFORM_OFF_HOLD_TICKS
        );
    }

    public static boolean isTransformHoldActive() {
        return transformHoldTicks > 0 || transformOffHoldTicks > 0;
    }

    public static void selectTransformation(KiTransformation transformation) {
        if (transformation == null || transformation == KiTransformation.BASE) {
            selectedTransformation = KiTransformation.BASE;
            return;
        }
        selectedTransformation = transformation;
    }

    public static KiTransformation selectedTransformation() {
        return selectedTransformation;
    }

    public static KiTransformation transformationChargeTarget() {
        if (transformOffHoldTicks > 0) {
            return KiTransformation.BASE;
        }
        if (transformHoldTicks <= 0) {
            return KiTransformation.BASE;
        }
        if (ClientKiData.data().activeTransformation() == KiTransformation.OOZARU) {
            return KiTransformation.GOLDEN_OOZARU;
        }
        if (ClientKiData.data().activeTransformation() == KiTransformation.GOLDEN_OOZARU
                && ClientKiData.data().hasTransformation(KiTransformation.SSJ3)
                && ClientKiData.data().transformationMastery(KiTransformation.SSJ3) >= 80.0) {
            return KiTransformation.SSJ4;
        }
        KiTransformation selected = selectedTransformTarget();
        return selected != KiTransformation.BASE ? selected : nextHigherTransformation();
    }

    private static boolean isSprintPhysicallyHeld(Minecraft minecraft) {
        InputConstants.Key key = minecraft.options.keySprint.getKey();
        if (key.getType() != InputConstants.Type.KEYSYM) {
            return minecraft.options.keySprint.isDown();
        }
        return InputConstants.isKeyDown(minecraft.getWindow().getWindow(), key.getValue());
    }

    private static void handleFlightDoubleJump(Minecraft minecraft) {
        if (!acceptsGameplayInput(minecraft)) {
            lastJumpDown = false;
            lastJumpPressTick = -1000;
            return;
        }

        Abilities abilities = minecraft.player.getAbilities();
        if (abilities.instabuild || minecraft.player.isSpectator()) {
            lastJumpDown = false;
            return;
        }

        boolean jumpDown = minecraft.options.keyJump.isDown();
        if (jumpDown && !lastJumpDown) {
            int tick = minecraft.player.tickCount;
            if (tick - lastJumpPressTick <= DOUBLE_JUMP_WINDOW_TICKS) {
                KicraftNetwork.sendToServer(new ToggleFlightPacket());
                lastJumpPressTick = -1000;
            } else {
                lastJumpPressTick = tick;
            }
        }
        lastJumpDown = jumpDown;
    }

    private static void handleTransformationHold(Minecraft minecraft) {
        if (!acceptsGameplayInput(minecraft)) {
            transformHoldTicks = 0;
            transformOffHoldTicks = 0;
            return;
        }

        if (KicraftKeyMappings.TRANSFORM.isDown() && hasTransformationTarget()) {
            transformHoldTicks++;
            transformOffHoldTicks = 0;
            if (transformHoldTicks == TRANSFORM_HOLD_TICKS) {
                KiTransformation target = selectedTransformTarget();
                if (target != KiTransformation.BASE) {
                    KicraftNetwork.sendToServer(new TransformationPacket(TransformationPacket.Action.SELECT, target));
                } else {
                    KicraftNetwork.sendToServer(new TransformationPacket(TransformationPacket.Action.NEXT));
                }
                transformHoldTicks = 0;
            }
        } else {
            transformHoldTicks = 0;
        }

        if (KicraftKeyMappings.TRANSFORM_OFF.isDown() && !ClientKiData.data().activeTransformation().isBase()) {
            transformOffHoldTicks++;
            transformHoldTicks = 0;
            if (transformOffHoldTicks == TRANSFORM_OFF_HOLD_TICKS) {
                KicraftNetwork.sendToServer(new TransformationPacket(TransformationPacket.Action.OFF));
                transformOffHoldTicks = 0;
            }
        } else {
            transformOffHoldTicks = 0;
        }
    }

    private static boolean hasTransformationTarget() {
        if (ClientKiData.data().activeTransformation() == KiTransformation.OOZARU
                || ClientKiData.data().activeTransformation() == KiTransformation.GOLDEN_OOZARU) {
            return true;
        }
        if (selectedTransformTarget() != KiTransformation.BASE) {
            return true;
        }
        return ClientKiData.data().transformations().stream()
                .filter(transformation -> transformation.menuVisible() && transformation.allowedFor(ClientKiData.data()))
                .anyMatch(transformation -> transformation.ordinal() > ClientKiData.data().activeTransformation().ordinal());
    }

    private static KiTransformation selectedTransformTarget() {
        if (selectedTransformation == null
                || selectedTransformation == KiTransformation.BASE
                || selectedTransformation == ClientKiData.data().activeTransformation()
                || !selectedTransformation.menuVisible()
                || !ClientKiData.data().hasTransformation(selectedTransformation)
                || !selectedTransformation.allowedFor(ClientKiData.data())) {
            return KiTransformation.BASE;
        }
        return selectedTransformation;
    }

    private static KiTransformation nextHigherTransformation() {
        return ClientKiData.data().transformations().stream()
                .filter(transformation -> transformation.menuVisible() && transformation.allowedFor(ClientKiData.data()))
                .filter(transformation -> transformation.ordinal() > ClientKiData.data().activeTransformation().ordinal())
                .min(java.util.Comparator.comparingInt(KiTransformation::ordinal))
                .orElse(KiTransformation.BASE);
    }

    private static void alignPlayerToLockOnTarget(Minecraft minecraft) {
        if (!acceptsGameplayInput(minecraft) || !ClientLockOnData.isLocked()) {
            return;
        }

        Entity entity = ClientLockOnData.targetEntity();
        if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
            ClientLockOnData.clear();
            return;
        }

        Vec3 eye = minecraft.player.getEyePosition();
        Vec3 targetEye = ClientLockOnData.targetAimPoint(target, 1.0F);
        Vec3 delta = targetEye.subtract(eye);
        if (delta.lengthSqr() < 0.0001) {
            return;
        }

        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float desiredYaw = (float) (Math.atan2(delta.z, delta.x) * (180.0 / Math.PI)) - 90.0F;
        float desiredPitch = (float) (-(Math.atan2(delta.y, horizontal) * (180.0 / Math.PI)));
        desiredPitch = Mth.clamp(desiredPitch, -85.0F, 85.0F);
        float yaw = stepDegrees(minecraft.player.getYRot(), desiredYaw, LOCK_ON_PLAYER_YAW_STEP);
        float pitch = stepLinear(minecraft.player.getXRot(), desiredPitch, LOCK_ON_PLAYER_PITCH_STEP);

        minecraft.player.setYRot(yaw);
        minecraft.player.setXRot(pitch);
        ClientFlightOrientation.followVanilla(minecraft.player);
    }

    private static float stepDegrees(float current, float target, float maxStep) {
        float delta = Mth.wrapDegrees(target - current);
        if (Math.abs(delta) <= LOCK_ON_DEADZONE_DEGREES) {
            return current;
        }
        return current + Mth.clamp(delta, -maxStep, maxStep);
    }

    private static float stepLinear(float current, float target, float maxStep) {
        float delta = target - current;
        if (Math.abs(delta) <= LOCK_ON_DEADZONE_DEGREES) {
            return current;
        }
        return current + Mth.clamp(delta, -maxStep, maxStep);
    }
}
