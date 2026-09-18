package net.kicraft.kicraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.kicraft.kicraft.Kicraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Draws the imported Blockbench body in place of the vanilla player body. */
public final class KicraftPlayerLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation PLACEHOLDER = new ResourceLocation(
            Kicraft.MODID, "textures/entity/kicraft_player_placeholder.png");
    private final KicraftPlayerModel model;

    public KicraftPlayerLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                              ModelPart bakedRoot) {
        super(parent);
        model = new KicraftPlayerModel(bakedRoot);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) return;
        model.attackTime = getParentModel().attackTime;
        model.riding = getParentModel().riding;
        model.young = getParentModel().young;
        model.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        boolean flying = ClientFlightStates.isFlying(player);
        float flightBank = flying
                ? ClientFlightVisuals.playerBankDegrees(player, partialTick) : 0.0F;
        poseStack.pushPose();
        if (flying) {
            ClientFlightOrientation.Attitude attitude = ClientFlightOrientation.attitude(player, partialTick);
            float renderedBodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
            float yawCorrection = Mth.wrapDegrees(attitude.yaw() - renderedBodyYaw);
            float flightTilt = ClientFlightVisuals.playerFlightTiltDegrees(player, partialTick);
            float directionInfluence = ClientFlightVisuals.flightDirectionInfluence(flightTilt);
            float bodyPitch = flightTilt + attitude.pitch() * directionInfluence;
            poseStack.translate(0.0F, -0.75F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-yawCorrection));
            poseStack.mulPose(Axis.XP.rotationDegrees(bodyPitch));
            poseStack.mulPose(Axis.ZP.rotationDegrees(flightBank));
            poseStack.translate(0.0F, 0.75F, 0.0F);
        }
        model.renderToBuffer(poseStack, bufferSource.getBuffer(RenderType.entityCutoutNoCull(PLACEHOLDER)),
                packedLight, LivingEntityRenderer.getOverlayCoords(player, 0), 1, 1, 1, 1);
        poseStack.popPose();
    }
}
