package net.kicraft.kicraft.client;

import net.kicraft.kicraft.Kicraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.model.GeoModel;

public class KicraftPlayerModel<T extends Player & GeoEntity> extends GeoModel<T> {

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return new ResourceLocation(Kicraft.MODID, "geo/kicraft_player.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return new ResourceLocation(Kicraft.MODID, "textures/entity/kicraft_player.png");
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        // WAŻNE: Tu musi być jeden plik z połączonymi animacjami gracza i ogona!
        return new ResourceLocation(Kicraft.MODID, "animations/kicraft_player.animation.json");
    }
}