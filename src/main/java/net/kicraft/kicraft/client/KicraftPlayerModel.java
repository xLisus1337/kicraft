package net.kicraft.kicraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;

import java.util.Map;
import java.util.WeakHashMap;

/** Blockbench kicraft_player rig with separate walk, sprint and running-jump cycles. */
public final class KicraftPlayerModel extends EntityModel<AbstractClientPlayer> {
    private static final float DEG = Mth.PI / 180.0F;

    // Eight evenly spaced poses: contact, recoil, passing, high point, then the mirrored half-cycle.
    private static final float[] THIGH_POSES = {-24, -19, -5, 14, 23, 16, 0, -19};
    private static final float[] KNEE_POSES = {5, 13, 8, 7, 12, 31, 29, 15};
    private static final float[] FOOT_WORLD_POSES = {9, 3, -2, -13, -14, 7, 7, 10};
    private static final float[] LEG_LIFT_POSES = {0.00F, 0.03F, 0.10F, 0.17F, 0.04F, 0.00F, 0.00F, 0.00F};
    private static final float[] PELVIS_Y_POSES = {0.04F, 0.22F, 0.02F, -0.10F, 0.04F, 0.22F, 0.02F, -0.10F};
    private static final float[] PELVIS_X_POSES = {-0.07F, -0.12F, -0.09F, -0.03F, 0.07F, 0.12F, 0.09F, 0.03F};
    private static final float[] PELVIS_YAW_POSES = {2.2F, 2.7F, 0.9F, -1.3F, -2.2F, -2.7F, -0.9F, 1.3F};
    private static final float[] PELVIS_ROLL_POSES = {0.9F, 1.7F, 2.0F, 0.9F, -0.9F, -1.7F, -2.0F, -0.9F};
    private static final float[] BODY_PITCH_POSES = {-2.5F, -1.5F, -1.0F, -2.0F, -2.5F, -1.5F, -1.0F, -2.0F};
    private static final float[] ARM_POSES = {8, 5, -2, -10, -15, -12, -4, 4};
    private static final float[] ELBOW_POSES = {-12, -13, -15, -17, -19, -18, -15, -13};
    private static final float[] HEAD_BOB_POSES = {0.02F, 0.05F, 0.0F, -0.04F, 0.02F, 0.05F, 0.0F, -0.04F};

    // A run needs its own flight phase, stronger knee drive and compact arm pump.
    private static final float[] RUN_THIGH_POSES = {-40, -29, -4, 27, 38, 21, -10, -33};
    private static final float[] RUN_KNEE_POSES = {8, 21, 13, 14, 19, 48, 44, 22};
    private static final float[] RUN_FOOT_WORLD_POSES = {11, 4, -5, -19, -16, 12, 9, 12};
    private static final float[] RUN_LEG_LIFT_POSES = {0.00F, 0.06F, 0.20F, 0.34F, 0.08F, 0.00F, 0.00F, 0.00F};
    private static final float[] RUN_PELVIS_Y_POSES = {0.03F, 0.28F, -0.09F, -0.22F, 0.03F, 0.28F, -0.09F, -0.22F};
    private static final float[] RUN_PELVIS_X_POSES = {-0.05F, -0.09F, -0.06F, -0.02F, 0.05F, 0.09F, 0.06F, 0.02F};
    private static final float[] RUN_PELVIS_YAW_POSES = {3.4F, 4.2F, 1.6F, -2.4F, -3.4F, -4.2F, -1.6F, 2.4F};
    private static final float[] RUN_PELVIS_ROLL_POSES = {1.1F, 2.0F, 2.4F, 1.0F, -1.1F, -2.0F, -2.4F, -1.0F};
    private static final float[] RUN_BODY_PITCH_POSES = {10, 12, 13, 11, 10, 12, 13, 11};
    private static final float[] RUN_ARM_POSES = {22, 14, -6, -31, -37, -19, 5, 22};
    private static final float[] RUN_ELBOW_POSES = {-34, -38, -46, -52, -48, -42, -37, -34};
    private static final float[] RUN_HEAD_BOB_POSES = {0.02F, 0.09F, -0.03F, -0.11F, 0.02F, 0.09F, -0.03F, -0.11F};
    private final ModelPart root;
    private final ModelPart pelvis;
    private final ModelPart rightHip;
    private final ModelPart leftHip;
    private final ModelPart body;
    private final ModelPart chest;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart rightShoulder;
    private final ModelPart rightArm;
    private final ModelPart rightForearm;
    private final ModelPart rightHand;
    private final ModelPart leftShoulder;
    private final ModelPart leftArm;
    private final ModelPart leftForearm;
    private final ModelPart leftHand;
    private final ModelPart rightThigh;
    private final ModelPart rightCalf;
    private final ModelPart rightFoot;
    private final ModelPart leftThigh;
    private final ModelPart leftCalf;
    private final ModelPart leftFoot;
    private final ModelPart tail1;
    private final ModelPart tail2;
    private final ModelPart tail3;
    private final ModelPart tail4;
    private final Map<AbstractClientPlayer, AnimationState> animationStates = new WeakHashMap<>();

    private static final class AnimationState {
        private float lastAge = Float.NaN;
        private float locomotionBlend;
        private float runBlend;
        private float airBlend;
        private float jumpBlend;
        private float flightBlend;
        private float flightStartTicks;
        private float crouchBlend;
        private float ridingBlend;
        private float landingBlend;
        private float tailMotion;
        private float forwardDirection;
        private float strafeDirection;
        private float horizontalSpeed;
        private float turnDirection;
        private float lastBodyYaw = Float.NaN;
        private float lastVerticalSpeed;
        private boolean airborne;
        private boolean grounded = true;
        private boolean runningTakeoff;
        private boolean flying;
        private boolean rightLead;
    }

    public KicraftPlayerModel(ModelPart root) {
        this.root = root;
        pelvis = root.getChild("pelvis");
        body = pelvis.getChild("body");
        chest = body.getChild("chest");
        neck = chest.getChild("neck");
        head = neck.getChild("head");
        rightShoulder = chest.getChild("right_shoulder");
        rightArm = rightShoulder.getChild("right_arm");
        rightForearm = rightArm.getChild("right_forearm");
        rightHand = rightForearm.getChild("right_hand");
        leftShoulder = chest.getChild("left_shoulder");
        leftArm = leftShoulder.getChild("left_arm");
        leftForearm = leftArm.getChild("left_forearm");
        leftHand = leftForearm.getChild("left_hand");
        rightHip = pelvis.getChild("right_hip");
        rightThigh = rightHip.getChild("right_thigh");
        rightCalf = rightThigh.getChild("right_calf");
        rightFoot = rightCalf.getChild("right_foot");
        leftHip = pelvis.getChild("left_hip");
        leftThigh = leftHip.getChild("left_thigh");
        leftCalf = leftThigh.getChild("left_calf");
        leftFoot = leftCalf.getChild("left_foot");
        tail1 = pelvis.getChild("tail_1");
        tail2 = tail1.getChild("tail_2");
        tail3 = tail2.getChild("tail_3");
        tail4 = tail3.getChild("tail_4");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition pelvis = root.addOrReplaceChild("pelvis", CubeListBuilder.create(), PartPose.offset(0, 12, 0));

        CubeListBuilder lowerTorso = CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -3.0F, -2.0F, 8.0F, 3.02F, 4.0F)
                .texOffs(0, 0).addBox(-3.9F, -6.0F, -2.0F, 7.8F, 3.02F, 4.0F,
                        new CubeDeformation(0.01F));
        PartDefinition body = pelvis.addOrReplaceChild("body", lowerTorso, PartPose.ZERO);
        CubeListBuilder upperTorso = CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.95F, -3.0F, -2.0F, 7.9F, 3.02F, 4.0F,
                        new CubeDeformation(0.02F))
                .texOffs(0, 0).addBox(-4.1F, -6.0F, -2.0F, 8.2F, 3.02F, 4.0F,
                        new CubeDeformation(0.03F));
        PartDefinition chest = body.addOrReplaceChild("chest", upperTorso,
                PartPose.offset(0, -6, 0));
        PartDefinition neck = chest.addOrReplaceChild("neck", CubeListBuilder.create(),
                PartPose.offset(0, -6, 0));
        neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4, -8, -4, 8, 8, 8), PartPose.ZERO);

        PartDefinition rightShoulder = chest.addOrReplaceChild("right_shoulder",
                CubeListBuilder.create(), PartPose.offset(-4, -5, 0));
        PartDefinition rightArm = rightShoulder.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4, -0.92F, -2, 4, 6.02F, 4), PartPose.ZERO);
        PartDefinition rightForearm = rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-2, -0.02F, -2, 4, 4.02F, 4), PartPose.offset(-2, 5, 0));
        rightForearm.addOrReplaceChild("right_hand", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-2, -0.02F, -2, 4, 1.02F, 4), PartPose.offset(0, 4, 0));

        PartDefinition leftShoulder = chest.addOrReplaceChild("left_shoulder",
                CubeListBuilder.create(), PartPose.offset(4, -5, 0));
        PartDefinition leftArm = leftShoulder.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(0, -0.92F, -2, 4, 6.02F, 4), PartPose.ZERO);
        PartDefinition leftForearm = leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-2, 0.08F, -2, 4, 4.02F, 4), PartPose.offset(2, 5, 0));
        leftForearm.addOrReplaceChild("left_hand", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-2, -0.02F, -2, 4, 1.02F, 4), PartPose.offset(0, 4, 0));

        PartDefinition rightHip = pelvis.addOrReplaceChild("right_hip", CubeListBuilder.create(),
                PartPose.offset(-2, 0, 0));
        PartDefinition rightThigh = rightHip.addOrReplaceChild("right_thigh",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-2, -0.02F, -2, 4, 6.02F, 4), PartPose.ZERO);
        PartDefinition rightCalf = rightThigh.addOrReplaceChild("right_calf", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-2, -0.02F, -2, 4, 4.02F, 4), PartPose.offset(0, 6, 0));
        rightCalf.addOrReplaceChild("right_foot", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-2, -0.02F, -2, 4, 2.02F, 4), PartPose.offset(0, 4, 0));

        PartDefinition leftHip = pelvis.addOrReplaceChild("left_hip", CubeListBuilder.create(),
                PartPose.offset(2, 0, 0));
        PartDefinition leftThigh = leftHip.addOrReplaceChild("left_thigh",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-2, -0.02F, -2, 4, 6.02F, 4), PartPose.ZERO);
        PartDefinition leftCalf = leftThigh.addOrReplaceChild("left_calf", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-2, -0.02F, -2, 4, 4.02F, 4), PartPose.offset(0, 6, 0));
        leftCalf.addOrReplaceChild("left_foot", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-2, -0.02F, -2, 4, 2.02F, 4), PartPose.offset(0, 4, 0));

        PartDefinition tail1 = pelvis.addOrReplaceChild("tail_1", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-0.75F, -0.75F, 0, 1.5F, 1.5F, 5), PartPose.offset(0, -2, 0.2F));
        PartDefinition tail2 = tail1.addOrReplaceChild("tail_2", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-0.75F, -0.75F, 0, 1.5F, 1.5F, 5), PartPose.offset(0, 0, 4.2F));
        PartDefinition tail3 = tail2.addOrReplaceChild("tail_3", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-0.75F, -0.75F, 0, 1.5F, 1.5F, 5), PartPose.offset(0, 0, 4.2F));
        tail3.addOrReplaceChild("tail_4", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-0.75F, -0.75F, 0, 1.5F, 1.5F, 5), PartPose.offset(0, 0, 4.2F));
        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    public void setupAnim(AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        float rawMovement = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        float movementTarget = smoothStep(Mth.clamp(rawMovement * 2.6F, 0.0F, 1.0F));
        float pace = Mth.clamp(rawMovement * 1.8F, 0.0F, 1.0F);
        float cycle = limbSwing * 0.6662F;
        float phase = cycle / (Mth.PI * 2.0F);
        phase -= Mth.floor(phase);
        float oppositePhase = phase + 0.5F;
        float shoulderPhase = phase - 0.045F;
        float oppositeShoulderPhase = shoulderPhase + 0.5F;
        AnimationState animationState = animationStates.computeIfAbsent(player, ignored -> new AnimationState());
        float elapsedTicks = Float.isNaN(animationState.lastAge)
                ? 1.0F
                : Mth.clamp(ageInTicks - animationState.lastAge, 0.0F, 1.5F);
        animationState.lastAge = ageInTicks;
        boolean isFlying = ClientFlightStates.isFlying(player)
                && !player.isPassenger()
                && !player.isSwimming()
                && !player.isFallFlying();
        boolean justLanded = player.onGround() && !animationState.grounded;
        if (justLanded) {
            float impact = Mth.clamp(-animationState.lastVerticalSpeed * 2.4F, 0.24F, 1.0F);
            animationState.landingBlend = Math.max(animationState.landingBlend, impact);
        }
        animationState.grounded = player.onGround();
        boolean wantsToRun = player.isSprinting() && rawMovement > 0.03F && !isFlying;
        boolean isAirborne = !player.onGround()
                && !player.isPassenger()
                && !player.isSwimming()
                && !player.isFallFlying()
                && !isFlying;
        if (isFlying && !animationState.flying) {
            animationState.flightStartTicks = 0.0F;
        }
        animationState.flying = isFlying;
        if (isFlying) {
            animationState.flightStartTicks = Math.min(14.0F,
                    animationState.flightStartTicks + elapsedTicks);
        }
        if (isAirborne && !animationState.airborne) {
            float[] takeoffPoses = wantsToRun ? RUN_THIGH_POSES : THIGH_POSES;
            float rightTakeoffPose = sampleLoop(takeoffPoses, phase);
            float leftTakeoffPose = sampleLoop(takeoffPoses, oppositePhase);
            animationState.rightLead = Math.abs(rightTakeoffPose - leftTakeoffPose) < 0.5F
                    ? (player.tickCount & 1) == 0
                    : rightTakeoffPose < leftTakeoffPose;
            animationState.runningTakeoff = wantsToRun;
        }
        animationState.airborne = isAirborne;
        boolean wantsRunningJump = isAirborne && animationState.runningTakeoff;
        animationState.runBlend = damp(animationState.runBlend, wantsToRun ? 1.0F : 0.0F,
                0.32F, elapsedTicks);
        animationState.airBlend = damp(animationState.airBlend, isAirborne ? 1.0F : 0.0F,
                isAirborne ? 0.52F : 0.36F, elapsedTicks);
        animationState.jumpBlend = damp(animationState.jumpBlend, wantsRunningJump ? 1.0F : 0.0F,
                wantsRunningJump ? 0.48F : 0.34F, elapsedTicks);
        animationState.flightBlend = damp(animationState.flightBlend, isFlying ? 1.0F : 0.0F,
                isFlying ? 0.42F : 0.30F, elapsedTicks);
        animationState.crouchBlend = damp(animationState.crouchBlend,
                player.isCrouching() && !isFlying ? 1.0F : 0.0F, 0.40F, elapsedTicks);
        animationState.ridingBlend = damp(animationState.ridingBlend,
                riding ? 1.0F : 0.0F, 0.46F, elapsedTicks);
        animationState.locomotionBlend = damp(animationState.locomotionBlend, movementTarget,
                movementTarget > animationState.locomotionBlend ? 0.44F : 0.30F, elapsedTicks);
        float movement = animationState.locomotionBlend;
        if (!isAirborne && animationState.airBlend < 0.01F) {
            animationState.runningTakeoff = false;
        }
        if (!isFlying && animationState.flightBlend < 0.01F) {
            animationState.flightStartTicks = 0.0F;
        }
        animationState.tailMotion = damp(animationState.tailMotion, movement, 0.16F, elapsedTicks);
        float landingBlend = animationState.landingBlend;
        animationState.landingBlend = damp(animationState.landingBlend, 0.0F, 0.24F, elapsedTicks);
        animationState.lastVerticalSpeed = (float) player.getDeltaMovement().y;
        updateMovementDirection(player, animationState, elapsedTicks);
        float directionLength = Mth.sqrt(animationState.forwardDirection * animationState.forwardDirection
                + animationState.strafeDirection * animationState.strafeDirection);
        float forwardDirection = directionLength > 0.001F
                ? animationState.forwardDirection / directionLength : 0.0F;
        float strafeDirection = directionLength > 0.001F
                ? animationState.strafeDirection / directionLength : 0.0F;
        float turnDirection = animationState.turnDirection;
        float backwardBlend = Mth.clamp(-forwardDirection, 0.0F, 1.0F);
        float sagittalScale = Mth.lerp(Math.abs(forwardDirection), 0.52F, 1.0F);
        float runBlend = animationState.runBlend;
        float poseScale = Mth.lerp(pace, 0.86F, 1.05F) * movement;

        // Sample authored contact/recoil/passing/high poses instead of swinging straight legs.
        float rightThighPose = sampleDirectional(THIGH_POSES, RUN_THIGH_POSES, phase, runBlend, backwardBlend);
        float leftThighPose = sampleDirectional(THIGH_POSES, RUN_THIGH_POSES, oppositePhase, runBlend, backwardBlend);
        rightThigh.xRot = rightThighPose * DEG * poseScale * sagittalScale;
        leftThigh.xRot = leftThighPose * DEG * poseScale * sagittalScale;
        rightThigh.yRot = Mth.lerp(runBlend, -1.5F, -2.5F) * DEG * movement;
        leftThigh.yRot = Mth.lerp(runBlend, 1.5F, 2.5F) * DEG * movement;
        rightCalf.xRot = sampleDirectional(KNEE_POSES, RUN_KNEE_POSES, phase, runBlend, backwardBlend)
                * DEG * poseScale;
        leftCalf.xRot = sampleDirectional(KNEE_POSES, RUN_KNEE_POSES, oppositePhase, runBlend, backwardBlend)
                * DEG * poseScale;

        // Counter-rotation keeps each foot close to the ground plane while the leg is planted.
        float rightFootWorld = sampleDirectional(FOOT_WORLD_POSES, RUN_FOOT_WORLD_POSES,
                phase, runBlend, backwardBlend) * DEG * poseScale;
        float leftFootWorld = sampleDirectional(FOOT_WORLD_POSES, RUN_FOOT_WORLD_POSES,
                oppositePhase, runBlend, backwardBlend) * DEG * poseScale;
        rightFoot.xRot = rightFootWorld - rightThigh.xRot - rightCalf.xRot;
        leftFoot.xRot = leftFootWorld - leftThigh.xRot - leftCalf.xRot;
        rightThigh.y = -sampleDirectional(LEG_LIFT_POSES, RUN_LEG_LIFT_POSES,
                phase, runBlend, backwardBlend) * movement;
        leftThigh.y = -sampleDirectional(LEG_LIFT_POSES, RUN_LEG_LIFT_POSES,
                oppositePhase, runBlend, backwardBlend) * movement;

        pelvis.x = sampleDirectional(PELVIS_X_POSES, RUN_PELVIS_X_POSES,
                phase, runBlend, backwardBlend) * movement;
        pelvis.y = 12.0F + sampleDirectional(PELVIS_Y_POSES, RUN_PELVIS_Y_POSES,
                phase, runBlend, backwardBlend) * movement;
        pelvis.yRot = sampleDirectional(PELVIS_YAW_POSES, RUN_PELVIS_YAW_POSES,
                phase, runBlend, backwardBlend) * DEG * movement;
        pelvis.zRot = sampleDirectional(PELVIS_ROLL_POSES, RUN_PELVIS_ROLL_POSES,
                phase, runBlend, backwardBlend) * DEG * movement;
        float strafeLean = strafeDirection * Mth.lerp(runBlend, 3.8F, 5.6F) * DEG * movement;
        pelvis.zRot -= strafeLean;
        pelvis.yRot += turnDirection * Mth.lerp(runBlend, 3.5F, 5.5F) * DEG * movement;
        pelvis.zRot -= turnDirection * Mth.lerp(runBlend, 1.8F, 3.0F) * DEG * movement;
        rightThigh.zRot = -pelvis.zRot * 0.10F + rightThighPose * DEG * poseScale * strafeDirection * 0.58F;
        leftThigh.zRot = -pelvis.zRot * 0.10F + leftThighPose * DEG * poseScale * strafeDirection * 0.58F;
        rightFoot.zRot = -rightThigh.zRot * 0.82F;
        leftFoot.zRot = -leftThigh.zRot * 0.82F;
        rightCalf.zRot = -rightThigh.zRot * 0.34F;
        leftCalf.zRot = -leftThigh.zRot * 0.34F;

        float breathing = Mth.sin(ageInTicks * 0.09F) * 0.55F * DEG * (1.0F - movement * 0.82F);
        body.xRot = sampleDirectional(BODY_PITCH_POSES, RUN_BODY_PITCH_POSES,
                phase, runBlend, backwardBlend) * DEG * movement
                + breathing - backwardBlend * 4.0F * DEG * movement;
        float shoulderYaw = sampleDirectional(PELVIS_YAW_POSES, RUN_PELVIS_YAW_POSES,
                shoulderPhase, runBlend, backwardBlend) * DEG * movement;
        float shoulderRoll = sampleDirectional(PELVIS_ROLL_POSES, RUN_PELVIS_ROLL_POSES,
                shoulderPhase, runBlend, backwardBlend) * DEG * movement;
        body.yRot = -shoulderYaw * 0.62F + strafeDirection * 1.8F * DEG * movement;
        body.zRot = -shoulderRoll * 0.58F + strafeLean * 0.34F;
        body.yRot -= turnDirection * Mth.lerp(runBlend, 5.5F, 8.0F) * DEG * movement;
        body.zRot += turnDirection * Mth.lerp(runBlend, 1.2F, 2.2F) * DEG * movement;

        // Mirrored arm keyframes keep the hands low and add a small delay behind the legs.
        float armSwingScale = Mth.lerp(Math.abs(forwardDirection), 0.68F, 1.0F);
        rightArm.xRot = sampleDirectional(ARM_POSES, RUN_ARM_POSES,
                shoulderPhase, runBlend, backwardBlend) * DEG * poseScale * armSwingScale;
        leftArm.xRot = sampleDirectional(ARM_POSES, RUN_ARM_POSES,
                oppositeShoulderPhase, runBlend, backwardBlend) * DEG * poseScale * armSwingScale;
        rightArm.yRot = Mth.lerp(runBlend, -1.5F, -5.0F) * DEG * movement;
        leftArm.yRot = Mth.lerp(runBlend, 1.5F, 5.0F) * DEG * movement;
        rightArm.zRot = Mth.lerp(runBlend, 3.5F, 5.5F) * DEG + pelvis.zRot * 0.30F;
        leftArm.zRot = -Mth.lerp(runBlend, 3.5F, 5.5F) * DEG + pelvis.zRot * 0.30F;
        float armBalance = strafeDirection * 4.5F * DEG * movement;
        rightArm.zRot -= armBalance;
        leftArm.zRot -= armBalance;
        float shoulderLift = Mth.sin(cycle - 0.25F) * Mth.lerp(runBlend, 0.06F, 0.16F) * movement;
        rightArm.y = shoulderLift;
        leftArm.y = -shoulderLift;
        rightForearm.xRot = sampleDirectional(ELBOW_POSES, RUN_ELBOW_POSES,
                shoulderPhase, runBlend, backwardBlend) * DEG
                * (0.70F + movement * 0.30F);
        leftForearm.xRot = sampleDirectional(ELBOW_POSES, RUN_ELBOW_POSES,
                oppositeShoulderPhase, runBlend, backwardBlend) * DEG
                * (0.70F + movement * 0.30F);

        animateGroundedSecondary(ageInTicks, movement, animationState.airBlend,
                animationState.flightBlend);

        float regularAirBlend = animationState.airBlend * (1.0F - animationState.jumpBlend);
        if (regularAirBlend > 0.001F) {
            animateAirborne(player, animationState.rightLead, regularAirBlend, movement, ageInTicks,
                    forwardDirection, strafeDirection);
        }
        if (animationState.jumpBlend > 0.001F) {
            animateRunningJump(player, animationState.rightLead, animationState.jumpBlend, ageInTicks,
                    strafeDirection);
        }
        if (animationState.flightBlend > 0.001F) {
            float flightStartProgress = smoothStep(Mth.clamp(animationState.flightStartTicks / 9.0F,
                    0.0F, 1.0F));
            animateFlight(player, animationState.flightBlend, flightStartProgress, ageInTicks,
                    forwardDirection, strafeDirection, animationState.horizontalSpeed);
        }
        if (landingBlend > 0.001F && !isFlying) {
            animateLanding(landingBlend);
        }
        if (animationState.ridingBlend > 0.001F) {
            animateRiding(animationState.ridingBlend);
        }
        float visibleCrouchBlend = animationState.crouchBlend * (1.0F - animationState.ridingBlend);
        if (visibleCrouchBlend > 0.001F) {
            animateCrouching(visibleCrouchBlend);
        }

        animateRigSecondary(ageInTicks, cycle, movement, runBlend, animationState.airBlend,
                animationState.flightBlend, strafeDirection, turnDirection);
        animateAttack(player);
        head.y = sampleDirectional(HEAD_BOB_POSES, RUN_HEAD_BOB_POSES,
                phase, runBlend, backwardBlend) * movement;
        head.yRot = (isFlying ? 0.0F : netHeadYaw * DEG)
                - body.yRot - chest.yRot - neck.yRot - pelvis.yRot * 0.86F;
        head.xRot = (isFlying ? 0.0F : headPitch * DEG)
                - body.xRot * 0.65F - chest.xRot * 0.82F - neck.xRot - pelvis.xRot * 0.82F;
        head.zRot = -body.zRot - chest.zRot - neck.zRot - pelvis.zRot * 0.84F;

        animateTail(ageInTicks, cycle, animationState.tailMotion, runBlend, animationState.airBlend,
                animationState.flightBlend, animationState.horizontalSpeed,
                (float) player.getDeltaMovement().y);
    }

    private void animateGroundedSecondary(float ageInTicks, float movement, float airBlend,
                                          float flightBlend) {
        float idleWeight = (1.0F - movement) * (1.0F - airBlend) * (1.0F - flightBlend);
        if (idleWeight <= 0.001F) return;

        float breathing = Mth.sin(ageInTicks * 0.085F);
        float weightShift = Mth.sin(ageInTicks * 0.043F + 0.8F);
        pelvis.y += breathing * 0.035F * idleWeight;
        pelvis.x += weightShift * 0.025F * idleWeight;
        body.yRot += weightShift * 0.32F * DEG * idleWeight;
        body.zRot += weightShift * 0.24F * DEG * idleWeight;
        rightArm.xRot += breathing * 0.65F * DEG * idleWeight;
        leftArm.xRot += breathing * 0.65F * DEG * idleWeight;
        rightForearm.xRot -= breathing * 0.35F * DEG * idleWeight;
        leftForearm.xRot -= breathing * 0.35F * DEG * idleWeight;
    }

    private void animateLanding(float blend) {
        float impact = smoothStep(Mth.clamp(blend, 0.0F, 1.0F));
        pelvis.y += impact * 0.72F;
        pelvis.xRot -= impact * 3.5F * DEG;
        body.xRot += impact * 8.0F * DEG;
        rightThigh.xRot -= impact * 13.0F * DEG;
        leftThigh.xRot -= impact * 13.0F * DEG;
        rightCalf.xRot += impact * 24.0F * DEG;
        leftCalf.xRot += impact * 24.0F * DEG;
        rightFoot.xRot -= impact * 11.0F * DEG;
        leftFoot.xRot -= impact * 11.0F * DEG;
        rightArm.xRot += impact * 8.0F * DEG;
        leftArm.xRot += impact * 8.0F * DEG;
        rightArm.zRot += impact * 2.0F * DEG;
        leftArm.zRot -= impact * 2.0F * DEG;
    }

    private void animateCrouching(float blend) {
        float eased = smoothStep(blend);
        body.xRot += eased * 24.0F * DEG;
        pelvis.y += eased * 2.0F;
        rightThigh.xRot -= eased * 0.35F;
        leftThigh.xRot -= eased * 0.35F;
        rightCalf.xRot += eased * 0.55F;
        leftCalf.xRot += eased * 0.55F;
        rightFoot.xRot -= eased * 0.20F;
        leftFoot.xRot -= eased * 0.20F;
        rightArm.xRot += eased * 4.0F * DEG;
        leftArm.xRot += eased * 4.0F * DEG;
    }

    private void animateRiding(float blend) {
        float eased = smoothStep(blend);
        rightThigh.xRot = Mth.lerp(eased, rightThigh.xRot, -1.4137F);
        leftThigh.xRot = Mth.lerp(eased, leftThigh.xRot, -1.4137F);
        rightThigh.yRot = Mth.lerp(eased, rightThigh.yRot, 0.3142F);
        leftThigh.yRot = Mth.lerp(eased, leftThigh.yRot, -0.3142F);
        rightCalf.xRot = Mth.lerp(eased, rightCalf.xRot, 0.35F);
        leftCalf.xRot = Mth.lerp(eased, leftCalf.xRot, 0.35F);
        rightFoot.xRot = Mth.lerp(eased, rightFoot.xRot, 0.10F);
        leftFoot.xRot = Mth.lerp(eased, leftFoot.xRot, 0.10F);
        pelvis.y = Mth.lerp(eased, pelvis.y, 12.35F);
    }

    private void animateFlight(AbstractClientPlayer player, float blend, float startProgress,
                               float ageInTicks, float forwardDirection, float strafeDirection,
                               float horizontalSpeed) {
        float verticalSpeed = Mth.clamp((float) player.getDeltaMovement().y * 4.0F, -1.0F, 1.0F);
        float forwardFlight = Math.max(forwardDirection, 0.0F) * horizontalSpeed;
        float backwardFlight = Math.max(-forwardDirection, 0.0F) * horizontalSpeed;
        float sideFlight = strafeDirection * horizontalSpeed;
        float bodyLaunchPulse = Mth.sin(smoothStep(Mth.clamp(startProgress / 0.82F,
                0.0F, 1.0F)) * Mth.PI);
        float legLaunchPulse = Mth.sin(smoothStep(Mth.clamp((startProgress - 0.035F) / 0.88F,
                0.0F, 1.0F)) * Mth.PI);
        float armLaunchPulse = Mth.sin(smoothStep(Mth.clamp((startProgress - 0.08F) / 0.84F,
                0.0F, 1.0F)) * Mth.PI);
        float hoverWave = Mth.sin(ageInTicks * 0.105F);
        float airWave = Mth.sin(ageInTicks * 0.16F + 0.7F);

        float targetPelvisPitch = startProgress
                * (forwardFlight * 4.0F - backwardFlight * 10.0F)
                + bodyLaunchPulse * 11.0F;
        float targetPelvisRoll = -sideFlight * 6.5F + airWave * 0.45F * (1.0F - horizontalSpeed);
        pelvis.xRot = Mth.lerp(blend, pelvis.xRot, targetPelvisPitch * DEG);
        pelvis.yRot = Mth.lerp(blend, pelvis.yRot, -sideFlight * 3.5F * DEG);
        pelvis.zRot = Mth.lerp(blend, pelvis.zRot, targetPelvisRoll * DEG);
        pelvis.x = Mth.lerp(blend, pelvis.x, sideFlight * 0.10F);
        pelvis.y = Mth.lerp(blend, pelvis.y,
                12.0F + hoverWave * 0.055F - bodyLaunchPulse * 0.30F);

        float targetBodyPitch = -targetPelvisPitch * 0.10F
                + forwardFlight * 4.0F + bodyLaunchPulse * 3.5F + hoverWave * 0.45F;
        body.xRot = Mth.lerp(blend, body.xRot, targetBodyPitch * DEG);
        body.yRot = Mth.lerp(blend, body.yRot,
                (sideFlight * 2.5F + airWave * 0.35F) * DEG);
        body.zRot = Mth.lerp(blend, body.zRot, -targetPelvisRoll * 0.42F * DEG);

        float legTrail = startProgress * (forwardFlight * 13.0F - backwardFlight * 5.0F)
                + legLaunchPulse * 21.0F;
        float legSplit = airWave * (1.5F + horizontalSpeed * 1.8F);
        float kneeBend = 7.0F + forwardFlight * 9.0F + backwardFlight * 5.0F
                + legLaunchPulse * 7.0F;
        float rightThighTarget = (legTrail + legSplit - sideFlight * 2.0F) * DEG;
        float leftThighTarget = (legTrail - legSplit + sideFlight * 2.0F) * DEG;
        float rightKneeTarget = (kneeBend - airWave * 1.3F) * DEG;
        float leftKneeTarget = (kneeBend + airWave * 1.3F) * DEG;
        rightThigh.xRot = Mth.lerp(blend, rightThigh.xRot, rightThighTarget);
        leftThigh.xRot = Mth.lerp(blend, leftThigh.xRot, leftThighTarget);
        rightThigh.yRot = Mth.lerp(blend, rightThigh.yRot, -sideFlight * 3.0F * DEG);
        leftThigh.yRot = Mth.lerp(blend, leftThigh.yRot, sideFlight * 3.0F * DEG);
        rightThigh.zRot = Mth.lerp(blend, rightThigh.zRot, sideFlight * 4.0F * DEG);
        leftThigh.zRot = Mth.lerp(blend, leftThigh.zRot, sideFlight * 4.0F * DEG);
        rightCalf.xRot = Mth.lerp(blend, rightCalf.xRot, rightKneeTarget);
        leftCalf.xRot = Mth.lerp(blend, leftCalf.xRot, leftKneeTarget);
        float footWorld = (-2.0F + forwardFlight * 3.0F + legLaunchPulse * 2.0F) * DEG;
        rightFoot.xRot = Mth.lerp(blend, rightFoot.xRot,
                footWorld - rightThighTarget - rightKneeTarget);
        leftFoot.xRot = Mth.lerp(blend, leftFoot.xRot,
                footWorld - leftThighTarget - leftKneeTarget);
        rightFoot.zRot = Mth.lerp(blend, rightFoot.zRot, -sideFlight * 2.0F * DEG);
        leftFoot.zRot = Mth.lerp(blend, leftFoot.zRot, -sideFlight * 2.0F * DEG);

        float armSweep = startProgress * (forwardFlight * 31.0F - backwardFlight * 14.0F
                + Math.max(verticalSpeed, 0.0F) * 7.0F) + armLaunchPulse * 40.0F;
        float armCounter = airWave * (1.0F + horizontalSpeed * 1.2F);
        rightArm.xRot = Mth.lerp(blend, rightArm.xRot,
                (armSweep + armCounter - sideFlight * 5.0F) * DEG);
        leftArm.xRot = Mth.lerp(blend, leftArm.xRot,
                (armSweep - armCounter + sideFlight * 5.0F) * DEG);
        rightArm.yRot = Mth.lerp(blend, rightArm.yRot, (-4.0F - sideFlight * 3.0F) * DEG);
        leftArm.yRot = Mth.lerp(blend, leftArm.yRot, (4.0F - sideFlight * 3.0F) * DEG);
        float armSpread = 4.0F + horizontalSpeed * 2.0F + armLaunchPulse * 2.0F;
        rightArm.zRot = Mth.lerp(blend, rightArm.zRot, armSpread * DEG);
        leftArm.zRot = Mth.lerp(blend, leftArm.zRot, -armSpread * DEG);
        float flightElbow = (-11.0F - forwardFlight * 4.0F - armLaunchPulse * 3.0F) * DEG;
        rightForearm.xRot = Mth.lerp(blend, rightForearm.xRot, flightElbow - airWave * DEG);
        leftForearm.xRot = Mth.lerp(blend, leftForearm.xRot, flightElbow + airWave * DEG);
    }

    private void animateAirborne(AbstractClientPlayer player, boolean rightLeads, float blend,
                                 float movement, float ageInTicks, float forwardDirection,
                                 float strafeDirection) {
        float verticalSpeed = Mth.clamp((float) player.getDeltaMovement().y, -0.55F, 0.45F);
        float rising = smoothStep(Mth.clamp(verticalSpeed * 4.0F, 0.0F, 1.0F));
        float falling = smoothStep(Mth.clamp(-verticalSpeed * 2.8F, 0.0F, 1.0F));
        float stride = Mth.lerp(movement, 0.35F, 1.0F);
        float airDrift = Mth.sin(ageInTicks * 0.18F) * 1.25F;

        float leadThigh = (-12.0F - rising * 4.0F + falling * 5.0F - 9.0F * stride + airDrift) * DEG;
        float rearThigh = (-4.0F - rising * 2.0F + falling * 2.0F + 14.0F * stride - airDrift) * DEG;
        float leadKnee = (31.0F + rising * 7.0F + falling * 6.0F + 5.0F * stride) * DEG;
        float rearKnee = (25.0F + rising * 3.0F + falling * 11.0F - 3.0F * stride) * DEG;
        float leadFootWorld = (5.0F - rising * 2.0F + falling * 2.0F) * DEG;
        float rearFootWorld = (-3.0F - rising * 5.0F + falling * 8.0F) * DEG;

        ModelPart leadLeg = rightLeads ? rightThigh : leftThigh;
        ModelPart leadCalf = rightLeads ? rightCalf : leftCalf;
        ModelPart leadFoot = rightLeads ? rightFoot : leftFoot;
        ModelPart rearLeg = rightLeads ? leftThigh : rightThigh;
        ModelPart rearCalf = rightLeads ? leftCalf : rightCalf;
        ModelPart rearFoot = rightLeads ? leftFoot : rightFoot;
        leadLeg.xRot = Mth.lerp(blend, leadLeg.xRot, leadThigh);
        rearLeg.xRot = Mth.lerp(blend, rearLeg.xRot, rearThigh);
        leadCalf.xRot = Mth.lerp(blend, leadCalf.xRot, leadKnee);
        rearCalf.xRot = Mth.lerp(blend, rearCalf.xRot, rearKnee);
        leadFoot.xRot = Mth.lerp(blend, leadFoot.xRot, leadFootWorld - leadThigh - leadKnee);
        rearFoot.xRot = Mth.lerp(blend, rearFoot.xRot, rearFootWorld - rearThigh - rearKnee);
        float airStrafeLeg = strafeDirection * 8.0F * DEG;
        leadLeg.yRot = Mth.lerp(blend, leadLeg.yRot, (rightLeads ? -1.8F : 1.8F) * DEG);
        rearLeg.yRot = Mth.lerp(blend, rearLeg.yRot, (rightLeads ? 1.8F : -1.8F) * DEG);
        leadLeg.zRot = Mth.lerp(blend, leadLeg.zRot, airStrafeLeg * 0.55F);
        rearLeg.zRot = Mth.lerp(blend, rearLeg.zRot, -airStrafeLeg * 0.35F);
        leadFoot.zRot = Mth.lerp(blend, leadFoot.zRot, -airStrafeLeg * 0.35F);
        rearFoot.zRot = Mth.lerp(blend, rearFoot.zRot, airStrafeLeg * 0.22F);

        pelvis.x = Mth.lerp(blend, pelvis.x, 0.0F);
        pelvis.y = Mth.lerp(blend, pelvis.y, 11.88F - rising * 0.08F + falling * 0.18F);
        pelvis.yRot = Mth.lerp(blend, pelvis.yRot, (rightLeads ? -1.4F : 1.4F) * DEG * stride);
        pelvis.zRot = Mth.lerp(blend, pelvis.zRot,
                ((rightLeads ? 0.8F : -0.8F) + airDrift * 0.12F - strafeDirection * 5.0F) * DEG);
        body.xRot = Mth.lerp(blend, body.xRot,
                (5.0F + rising * 2.0F + falling * 6.0F - Math.min(forwardDirection, 0.0F) * 3.0F) * DEG);
        body.yRot = Mth.lerp(blend, body.yRot, (rightLeads ? 1.8F : -1.8F) * DEG * stride);
        body.zRot = Mth.lerp(blend, body.zRot,
                -pelvis.zRot * 0.50F + strafeDirection * 1.5F * DEG);

        float baseArm = -18.0F - rising * 12.0F + falling * 16.0F;
        float frontArmPose = (baseArm - 8.0F * stride + airDrift * 0.55F) * DEG;
        float rearArmPose = (baseArm + 11.0F * stride - airDrift * 0.55F) * DEG;
        ModelPart frontArm = rightLeads ? leftArm : rightArm;
        ModelPart frontForearm = rightLeads ? leftForearm : rightForearm;
        ModelPart rearArm = rightLeads ? rightArm : leftArm;
        ModelPart rearForearm = rightLeads ? rightForearm : leftForearm;
        frontArm.xRot = Mth.lerp(blend, frontArm.xRot, frontArmPose);
        rearArm.xRot = Mth.lerp(blend, rearArm.xRot, rearArmPose);
        frontForearm.xRot = Mth.lerp(blend, frontForearm.xRot, (-28.0F - rising * 7.0F) * DEG);
        rearForearm.xRot = Mth.lerp(blend, rearForearm.xRot, (-22.0F - rising * 5.0F) * DEG);
        float armSpread = (8.0F + falling * 6.0F) * DEG;
        rightArm.yRot = Mth.lerp(blend, rightArm.yRot, -4.0F * DEG);
        leftArm.yRot = Mth.lerp(blend, leftArm.yRot, 4.0F * DEG);
        rightArm.zRot = Mth.lerp(blend, rightArm.zRot, armSpread);
        leftArm.zRot = Mth.lerp(blend, leftArm.zRot, -armSpread);
    }

    private void animateRunningJump(AbstractClientPlayer player, boolean rightLeads, float blend,
                                    float ageInTicks, float strafeDirection) {
        float verticalSpeed = Mth.clamp((float) player.getDeltaMovement().y, -0.45F, 0.45F);
        float rising = smoothStep(Mth.clamp(verticalSpeed * 4.0F, 0.0F, 1.0F));
        float falling = smoothStep(Mth.clamp(-verticalSpeed * 3.2F, 0.0F, 1.0F));
        float airDrift = Mth.sin(ageInTicks * 0.20F) * 1.15F;

        float leadThigh = (-32.0F - rising * 6.0F + falling * 6.0F + airDrift) * DEG;
        float rearThigh = (23.0F + rising * 3.0F - falling * 8.0F - airDrift) * DEG;
        float leadKnee = (44.0F + rising * 6.0F + falling * 3.0F) * DEG;
        float rearKnee = (25.0F - rising * 4.0F + falling * 18.0F) * DEG;
        float leadFootWorld = Mth.lerp(falling, 8.0F, 2.0F) * DEG;
        float rearFootWorld = Mth.lerp(falling, -14.0F, 6.0F) * DEG;

        ModelPart leadLeg = rightLeads ? rightThigh : leftThigh;
        ModelPart leadCalf = rightLeads ? rightCalf : leftCalf;
        ModelPart leadFoot = rightLeads ? rightFoot : leftFoot;
        ModelPart rearLeg = rightLeads ? leftThigh : rightThigh;
        ModelPart rearCalf = rightLeads ? leftCalf : rightCalf;
        ModelPart rearFoot = rightLeads ? leftFoot : rightFoot;
        leadLeg.xRot = Mth.lerp(blend, leadLeg.xRot, leadThigh);
        rearLeg.xRot = Mth.lerp(blend, rearLeg.xRot, rearThigh);
        leadCalf.xRot = Mth.lerp(blend, leadCalf.xRot, leadKnee);
        rearCalf.xRot = Mth.lerp(blend, rearCalf.xRot, rearKnee);
        leadFoot.xRot = Mth.lerp(blend, leadFoot.xRot, leadFootWorld - leadThigh - leadKnee);
        rearFoot.xRot = Mth.lerp(blend, rearFoot.xRot, rearFootWorld - rearThigh - rearKnee);
        leadLeg.yRot = Mth.lerp(blend, leadLeg.yRot, (rightLeads ? -2.5F : 2.5F) * DEG);
        rearLeg.yRot = Mth.lerp(blend, rearLeg.yRot, (rightLeads ? 2.5F : -2.5F) * DEG);
        leadLeg.zRot = Mth.lerp(blend, leadLeg.zRot, strafeDirection * 6.0F * DEG);
        rearLeg.zRot = Mth.lerp(blend, rearLeg.zRot, -strafeDirection * 3.5F * DEG);

        pelvis.x = Mth.lerp(blend, pelvis.x, 0.0F);
        pelvis.y = Mth.lerp(blend, pelvis.y, 11.78F + falling * 0.24F + airDrift * 0.018F);
        pelvis.yRot = Mth.lerp(blend, pelvis.yRot, (rightLeads ? -2.2F : 2.2F) * DEG);
        pelvis.zRot = Mth.lerp(blend, pelvis.zRot,
                ((rightLeads ? 1.2F : -1.2F) - strafeDirection * 4.5F) * DEG);
        body.xRot = Mth.lerp(blend, body.xRot, (16.0F - rising * 2.0F + falling * 5.0F) * DEG);
        body.yRot = Mth.lerp(blend, body.yRot, (rightLeads ? 3.2F : -3.2F) * DEG);
        body.zRot = Mth.lerp(blend, body.zRot,
                -pelvis.zRot * 0.55F + strafeDirection * 1.4F * DEG);

        float rearArmPose = (22.0F - falling * 9.0F - airDrift * 0.65F) * DEG;
        float frontArmPose = (-42.0F + falling * 16.0F + airDrift * 0.65F) * DEG;
        ModelPart rearArm = rightLeads ? rightArm : leftArm;
        ModelPart rearForearm = rightLeads ? rightForearm : leftForearm;
        ModelPart frontArm = rightLeads ? leftArm : rightArm;
        ModelPart frontForearm = rightLeads ? leftForearm : rightForearm;
        rearArm.xRot = Mth.lerp(blend, rearArm.xRot, rearArmPose);
        frontArm.xRot = Mth.lerp(blend, frontArm.xRot, frontArmPose);
        rearForearm.xRot = Mth.lerp(blend, rearForearm.xRot, (-41.0F - rising * 4.0F) * DEG);
        frontForearm.xRot = Mth.lerp(blend, frontForearm.xRot, (-50.0F + falling * 8.0F) * DEG);
        rightArm.yRot = Mth.lerp(blend, rightArm.yRot, -5.0F * DEG);
        leftArm.yRot = Mth.lerp(blend, leftArm.yRot, 5.0F * DEG);
        rightArm.zRot = Mth.lerp(blend, rightArm.zRot, 6.0F * DEG);
        leftArm.zRot = Mth.lerp(blend, leftArm.zRot, -6.0F * DEG);
    }

    private void animateTail(float ageInTicks, float walkCycle, float movement, float runBlend,
                             float airBlend, float flightBlend, float horizontalSpeed,
                             float verticalSpeed) {
        float idleCycle = ageInTicks * 0.055F;
        float tailMovementCycle = walkCycle * 0.32F;
        float verticalCycle = tailMovementCycle * 1.15F + 0.35F;
        float motionEnergy = movement * (1.0F + runBlend * 0.08F);

        // The tail has much more inertia than the limbs. Each segment follows slowly instead of
        // reversing direction on every footstep.
        tail1.xRot = 24.0F * DEG
                + Mth.sin(idleCycle + 0.7F) * 4.5F * DEG
                + Mth.sin(verticalCycle) * 6.0F * DEG * motionEnergy;
        tail2.xRot = 3.0F * DEG
                + Mth.sin(idleCycle + 0.20F) * 3.8F * DEG
                + Mth.sin(verticalCycle - 0.34F) * 4.8F * DEG * motionEnergy;
        tail3.xRot = 2.0F * DEG
                + Mth.sin(idleCycle - 0.30F) * 3.2F * DEG
                + Mth.sin(verticalCycle - 0.68F) * 3.9F * DEG * motionEnergy;
        tail4.xRot = 1.0F * DEG
                + Mth.sin(idleCycle - 0.80F) * 2.8F * DEG
                + Mth.sin(verticalCycle - 1.02F) * 3.3F * DEG * motionEnergy;

        float walkSway = 6.5F * DEG * motionEnergy;
        tail1.yRot = Mth.sin(idleCycle) * 6.0F * DEG
                + Mth.sin(tailMovementCycle - 0.15F) * walkSway;
        tail2.yRot = Mth.sin(idleCycle - 0.48F) * 5.0F * DEG
                + Mth.sin(tailMovementCycle - 0.52F) * walkSway * 0.82F;
        tail3.yRot = Mth.sin(idleCycle - 0.96F) * 4.5F * DEG
                + Mth.sin(tailMovementCycle - 0.89F) * walkSway * 0.68F;
        tail4.yRot = Mth.sin(idleCycle - 1.44F) * 4.0F * DEG
                + Mth.sin(tailMovementCycle - 1.26F) * walkSway * 0.56F;

        if (airBlend > 0.001F) {
            float rise = Mth.clamp(verticalSpeed * 3.0F, -1.0F, 1.0F);
            float airWave = Mth.sin(ageInTicks * 0.13F);
            tail1.xRot += (7.0F - rise * 4.0F + airWave * 1.4F) * DEG * airBlend;
            tail2.xRot += (4.5F - rise * 3.0F + airWave * 1.1F) * DEG * airBlend;
            tail3.xRot += (2.8F - rise * 1.8F + airWave * 0.8F) * DEG * airBlend;
            tail1.yRot *= Mth.lerp(airBlend, 1.0F, 0.72F);
            tail2.yRot *= Mth.lerp(airBlend, 1.0F, 0.78F);
            tail3.yRot *= Mth.lerp(airBlend, 1.0F, 0.84F);
            tail4.yRot *= Mth.lerp(airBlend, 1.0F, 0.90F);
        }

        if (flightBlend > 0.001F) {
            float flightWave = Mth.sin(ageInTicks * (0.085F + horizontalSpeed * 0.025F));
            float rise = Mth.clamp(verticalSpeed * 3.0F, -1.0F, 1.0F);
            tail1.xRot = Mth.lerp(flightBlend, tail1.xRot,
                    (18.0F - rise * 4.0F + flightWave * 2.2F) * DEG);
            tail2.xRot = Mth.lerp(flightBlend, tail2.xRot,
                    (4.0F - rise * 2.5F + Mth.sin(ageInTicks * 0.085F - 0.38F) * 1.8F) * DEG);
            tail3.xRot = Mth.lerp(flightBlend, tail3.xRot,
                    (3.0F - rise * 1.5F + Mth.sin(ageInTicks * 0.085F - 0.76F) * 1.4F) * DEG);
            tail4.xRot = Mth.lerp(flightBlend, tail4.xRot,
                    (2.0F + Mth.sin(ageInTicks * 0.085F - 1.14F) * 1.1F) * DEG);
            float flightSway = (3.5F + horizontalSpeed * 1.8F) * DEG;
            tail1.yRot = Mth.lerp(flightBlend, tail1.yRot, flightWave * flightSway);
            tail2.yRot = Mth.lerp(flightBlend, tail2.yRot,
                    Mth.sin(ageInTicks * 0.085F - 0.42F) * flightSway * 0.82F);
            tail3.yRot = Mth.lerp(flightBlend, tail3.yRot,
                    Mth.sin(ageInTicks * 0.085F - 0.84F) * flightSway * 0.66F);
            tail4.yRot = Mth.lerp(flightBlend, tail4.yRot,
                    Mth.sin(ageInTicks * 0.085F - 1.26F) * flightSway * 0.52F);
        }
    }

    private static float smoothStep(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private static void updateMovementDirection(AbstractClientPlayer player, AnimationState state,
                                                float elapsedTicks) {
        double motionX = player.getX() - player.xOld;
        double motionZ = player.getZ() - player.zOld;
        double horizontalSpeed = Math.sqrt(motionX * motionX + motionZ * motionZ);
        if (horizontalSpeed < 0.0001D) {
            motionX = player.getDeltaMovement().x;
            motionZ = player.getDeltaMovement().z;
            horizontalSpeed = Math.sqrt(motionX * motionX + motionZ * motionZ);
        }

        float targetForward = 0.0F;
        float targetStrafe = 0.0F;
        if (horizontalSpeed >= 0.0001D) {
            float bodyYaw = player.yBodyRot * DEG;
            float forwardX = -Mth.sin(bodyYaw);
            float forwardZ = Mth.cos(bodyYaw);
            float rightX = Mth.cos(bodyYaw);
            float rightZ = Mth.sin(bodyYaw);
            targetForward = (float) ((motionX * forwardX + motionZ * forwardZ) / horizontalSpeed);
            targetStrafe = (float) ((motionX * rightX + motionZ * rightZ) / horizontalSpeed);
        }

        state.forwardDirection = damp(state.forwardDirection, targetForward, 0.42F, elapsedTicks);
        state.strafeDirection = damp(state.strafeDirection, targetStrafe, 0.42F, elapsedTicks);
        float targetTurn = 0.0F;
        if (!Float.isNaN(state.lastBodyYaw)) {
            targetTurn = Mth.clamp(Mth.wrapDegrees(player.yBodyRot - state.lastBodyYaw) / 8.0F,
                    -1.0F, 1.0F);
        }
        state.lastBodyYaw = player.yBodyRot;
        state.turnDirection = damp(state.turnDirection, targetTurn,
                Math.abs(targetTurn) > Math.abs(state.turnDirection) ? 0.46F : 0.24F, elapsedTicks);
        float targetSpeed = Mth.clamp((float) horizontalSpeed * 7.5F, 0.0F, 1.0F);
        state.horizontalSpeed = damp(state.horizontalSpeed, targetSpeed,
                targetSpeed > state.horizontalSpeed ? 0.38F : 0.24F, elapsedTicks);
    }

    private static float damp(float current, float target, float responsePerTick, float elapsedTicks) {
        if (elapsedTicks <= 0.0F) return current;
        float blend = 1.0F - (float) Math.pow(1.0F - responsePerTick, elapsedTicks);
        return Mth.lerp(blend, current, target);
    }

    private static float sampleBlended(float[] walkPoses, float[] runPoses, float phase, float runBlend) {
        return Mth.lerp(runBlend, sampleLoop(walkPoses, phase), sampleLoop(runPoses, phase));
    }

    private static float sampleDirectional(float[] walkPoses, float[] runPoses, float phase,
                                           float runBlend, float backwardBlend) {
        float forwardPose = sampleBlended(walkPoses, runPoses, phase, runBlend);
        float backwardPose = sampleBlended(walkPoses, runPoses, -phase, runBlend);
        return Mth.lerp(backwardBlend, forwardPose, backwardPose);
    }

    private static float sampleLoop(float[] poses, float phase) {
        phase -= Mth.floor(phase);
        float frame = phase * poses.length;
        int current = Mth.floor(frame);
        float progress = frame - current;
        int previous = (current - 1 + poses.length) % poses.length;
        int next = (current + 1) % poses.length;
        int afterNext = (current + 2) % poses.length;
        float p0 = poses[previous];
        float p1 = poses[current];
        float p2 = poses[next];
        float p3 = poses[afterNext];
        float tangentScale = 0.35F;
        float m1 = (p2 - p0) * tangentScale;
        float m2 = (p3 - p1) * tangentScale;
        float t2 = progress * progress;
        float t3 = t2 * progress;
        return (2.0F * t3 - 3.0F * t2 + 1.0F) * p1
                + (t3 - 2.0F * t2 + progress) * m1
                + (-2.0F * t3 + 3.0F * t2) * p2
                + (t3 - t2) * m2;
    }

    /**
     * Spreads a pose across the additional rig joints. The upper body keeps the authored direction,
     * but bends through the waist, chest, shoulders and hips instead of behaving as one cuboid.
     */
    private void animateRigSecondary(float ageInTicks, float cycle, float movement, float runBlend,
                                     float airBlend, float flightBlend, float strafeDirection,
                                     float turnDirection) {
        float grounded = (1.0F - airBlend) * (1.0F - flightBlend);
        float locomotion = movement * grounded;
        float strideWave = Mth.sin(cycle - 0.32F);
        float doubleStep = Mth.sin(cycle * 2.0F + 0.45F);

        float authoredPitch = body.xRot;
        float authoredYaw = body.yRot;
        float authoredRoll = body.zRot;
        body.xRot = authoredPitch * 0.58F;
        body.yRot = authoredYaw * 0.46F;
        body.zRot = authoredRoll * 0.55F;
        chest.xRot = authoredPitch * 0.42F
                + doubleStep * Mth.lerp(runBlend, 0.35F, 0.85F) * DEG * locomotion;
        chest.yRot = authoredYaw * 0.54F
                + strideWave * Mth.lerp(runBlend, 0.75F, 1.65F) * DEG * locomotion;
        chest.zRot = authoredRoll * 0.45F
                + strafeDirection * Mth.lerp(runBlend, 0.55F, 1.15F) * DEG * locomotion;

        float breathing = Mth.sin(ageInTicks * 0.085F);
        float idle = (1.0F - movement) * grounded;
        chest.xRot += breathing * 0.32F * DEG * idle;
        neck.xRot = -breathing * 0.12F * DEG * idle;
        neck.yRot = -turnDirection * 0.55F * DEG * locomotion;
        neck.zRot = -strafeDirection * 0.35F * DEG * locomotion;

        float shoulderDrive = strideWave * Mth.lerp(runBlend, 0.8F, 2.2F) * DEG * locomotion;
        float shoulderRise = doubleStep * Mth.lerp(runBlend, 0.25F, 0.70F) * DEG * locomotion;
        rightShoulder.xRot = shoulderDrive * 0.18F;
        leftShoulder.xRot = -shoulderDrive * 0.18F;
        rightShoulder.yRot = shoulderDrive;
        leftShoulder.yRot = -shoulderDrive;
        rightShoulder.zRot = (1.1F * DEG + shoulderRise) * grounded;
        leftShoulder.zRot = (-1.1F * DEG + shoulderRise) * grounded;

        float hipDrive = strideWave * Mth.lerp(runBlend, 0.55F, 1.25F) * DEG * locomotion;
        rightHip.yRot = -hipDrive;
        leftHip.yRot = hipDrive;
        rightHip.zRot = (-0.35F - strafeDirection * 0.70F) * DEG * locomotion;
        leftHip.zRot = (0.35F - strafeDirection * 0.70F) * DEG * locomotion;

        float flightRelax = flightBlend * (0.5F + 0.5F * Mth.sin(ageInTicks * 0.11F));
        rightShoulder.zRot += flightRelax * 0.65F * DEG;
        leftShoulder.zRot -= flightRelax * 0.65F * DEG;
        rightHand.xRot = -rightForearm.xRot * 0.08F + strideWave * 0.45F * DEG * locomotion;
        leftHand.xRot = -leftForearm.xRot * 0.08F - strideWave * 0.45F * DEG * locomotion;
        rightHand.zRot = (0.7F + flightBlend * 0.8F) * DEG;
        leftHand.zRot = (-0.7F - flightBlend * 0.8F) * DEG;
    }

    private void animateAttack(AbstractClientPlayer player) {
        if (attackTime <= 0) return;
        float strike = Mth.sin(Mth.sqrt(attackTime) * Mth.PI);
        float returnMotion = Mth.sin((1.0F - (1.0F - attackTime) * (1.0F - attackTime)) * Mth.PI);
        boolean right = player.swingingArm == InteractionHand.MAIN_HAND
                ? player.getMainArm() == HumanoidArm.RIGHT
                : player.getMainArm() != HumanoidArm.RIGHT;
        ModelPart arm = right ? rightArm : leftArm;
        ModelPart forearm = right ? rightForearm : leftForearm;
        ModelPart shoulder = right ? rightShoulder : leftShoulder;
        ModelPart guardArm = right ? leftArm : rightArm;
        ModelPart guardForearm = right ? leftForearm : rightForearm;
        ModelPart guardShoulder = right ? leftShoulder : rightShoulder;
        float side = right ? -1.0F : 1.0F;
        pelvis.yRot -= side * strike * 0.045F;
        pelvis.zRot += side * strike * 0.025F;
        body.xRot += strike * 0.022F;
        body.yRot += side * strike * 0.070F;
        body.zRot -= side * strike * 0.014F;
        chest.xRot += strike * 0.052F;
        chest.yRot += side * strike * 0.145F;
        chest.zRot -= side * strike * 0.030F;
        shoulder.xRot -= strike * 0.18F;
        shoulder.yRot += side * strike * 0.12F;
        shoulder.zRot += side * strike * 0.045F;
        arm.xRot -= strike * 1.65F + returnMotion * 0.35F;
        arm.yRot += side * strike * 0.17F;
        arm.zRot += side * strike * 0.10F;
        forearm.xRot -= strike * 0.36F;
        guardShoulder.xRot -= strike * 0.025F;
        guardShoulder.yRot -= side * strike * 0.025F;
        guardArm.xRot -= strike * 0.10F;
        guardArm.zRot -= side * strike * 0.055F;
        guardForearm.xRot -= strike * 0.18F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        root.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
