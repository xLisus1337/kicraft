package net.kicraft.kicraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.kicraft.kicraft.content.TrainingDummyEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renders the dummy with a normal player-shaped model and a black silhouette. */
public final class TrainingDummyRenderer extends MobRenderer<TrainingDummyEntity, HumanoidModel<TrainingDummyEntity>> {
    private static final ResourceLocation PLAYER_TEXTURE = new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png");

    public TrainingDummyRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.0F);
    }

    @Override
    public void render(TrainingDummyEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        RenderSystem.setShaderColor(0.025F, 0.025F, 0.035F, 1.0F);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(TrainingDummyEntity entity) {
        return PLAYER_TEXTURE;
    }
}
