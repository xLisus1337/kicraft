package net.kicraft.kicraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.kicraft.kicraft.capability.PlayerDataProvider;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.object.Color;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

// DODANO: Prawidłowy import warstwy zbroi dla GeckoLib
import software.bernie.geckolib.renderer.layer.ItemArmorGeoLayer;

public class KicraftPlayerRenderer<T extends Player & GeoEntity> extends GeoEntityRenderer<T> {

    public KicraftPlayerRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new KicraftPlayerModel<T>());

        // POPRAWKA: Używamy dedykowanej warstwy GeckoLib (ItemArmorGeoLayer)
        // oraz poprawnej metody (addRenderLayer zamiast addLayer)
        this.addRenderLayer(new ItemArmorGeoLayer<>(this));
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {

        // Bezpieczne pobranie modelu - sprawdzenie czy istnieje
        if (this.getGeoModel() != null) {
            GeoBone tailBone = this.getGeoModel().getBone("tail_joint_1").orElse(null);
            if (tailBone != null) {
                // Logika ogona (możesz tu dodać sprawdzenie rasy, aby chować go u ludzi)
                tailBone.setHidden(false);
            }
        }

        poseStack.pushPose();
        // Skalowanie modelu
        float scale = 0.90f;
        poseStack.scale(scale, scale, scale);

        // Wywołanie renderowania modelu (GeckoLib renderuje warstwy automatycznie)
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        poseStack.popPose();
    }

    @Override
    public Color getRenderColor(T animatable, float partialTick, int packedLight) {
        // Logika koloru skóry/postaci
        int[] hex = {0xFFFFFF};
        animatable.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
            int currentColor = data.getSkinColors()[0];
            if (currentColor != 0 && currentColor != 0x000000) {
                hex[0] = currentColor;
            }
        });
        return Color.ofOpaque(hex[0]);
    }
}