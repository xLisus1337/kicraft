package net.kicraft.kicraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.kicraft.kicraft.Kicraft;
import net.kicraft.kicraft.capability.PlayerDataProvider;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class SaiyanTailLayer<T extends GeoEntity> extends GeoRenderLayer<T> {
    private static final ResourceLocation TAIL_TEXTURE = new ResourceLocation(Kicraft.MODID, "textures/entity/saiyan_tail.png");

    // Używamy GeoModel zamiast DefaultedEntityGeoModel
    private final GeoModel<T> tailModel;

    public SaiyanTailLayer(GeoRenderer<T> entityRenderer) {
        super(entityRenderer);

        // Definiujemy DOKŁADNE ścieżki do Twoich plików
        this.tailModel = new GeoModel<T>() {
            @Override
            public ResourceLocation getModelResource(T animatable) {
                return new ResourceLocation(Kicraft.MODID, "geo/saiyan_tail.geo.json");
            }

            @Override
            public ResourceLocation getTextureResource(T animatable) {
                return TAIL_TEXTURE;
            }

            @Override
            public ResourceLocation getAnimationResource(T animatable) {
                // Ogon ciągnie animacje z Twojego połączonego pliku!
                return new ResourceLocation(Kicraft.MODID, "animations/kicraft_player.animation.json");
            }
        };
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {

        if (animatable instanceof Player player) {
            player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                String race = data.getRace();
                if (race.equalsIgnoreCase("Saiyan") || race.equalsIgnoreCase("Half-Saiyan")) {

                    // Szukamy kości pelvis
                    GeoBone targetBone = getRenderer().getGeoModel().getBone("pelvis").orElse(null);

                    if (targetBone != null) {
                        BakedGeoModel bakedTail = this.tailModel.getBakedModel(this.tailModel.getModelResource(animatable));
                        RenderType tailRenderType = RenderType.entityCutoutNoCull(TAIL_TEXTURE);

                        poseStack.pushPose();

                        // Doczepiamy model ogona
                        getRenderer().reRender(
                                bakedTail,
                                poseStack,
                                bufferSource,
                                animatable,
                                tailRenderType,
                                bufferSource.getBuffer(tailRenderType),
                                partialTick,
                                packedLight,
                                packedOverlay,
                                1.0F, 1.0F, 1.0F, 1.0F
                        );

                        poseStack.popPose();
                    }
                }
            });
        }
    }
}