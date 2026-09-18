package net.kicraft.kicraft.worldgen;

import net.kicraft.kicraft.Kicraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

public final class PaozuBiomes {
    public static final ResourceKey<Biome> PAOZU_MOUNTAINS = ResourceKey.create(
            Registries.BIOME,
            new ResourceLocation(Kicraft.MODID, "paozu_mountains")
    );

    private PaozuBiomes() {
    }
}
