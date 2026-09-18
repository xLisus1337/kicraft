package net.kicraft.kicraft.worldgen;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;

import java.util.List;
import java.util.function.Consumer;

public final class PaozuOverworldRegion extends Region {
    public PaozuOverworldRegion(ResourceLocation name, int weight) {
        super(name, RegionType.OVERWORLD, weight);
    }

    @Override
    public void addBiomes(Registry<Biome> registry,
                          Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        // A TerraBlender region contains many climate slices. Mapping only the
        // vanilla mountain slices made Paozu appear as tiny islands inside ocean
        // and plains slices. Mapping every surface slice turns the selected region
        // into one continuous Paozu area whose shortest useful span exceeds 500
        // blocks with overworld_region_size >= 4.
        List.of(
                Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.SNOWY_PLAINS, Biomes.ICE_SPIKES,
                Biomes.DESERT, Biomes.SWAMP, Biomes.MANGROVE_SWAMP,
                Biomes.FOREST, Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST,
                Biomes.OLD_GROWTH_BIRCH_FOREST, Biomes.DARK_FOREST,
                Biomes.TAIGA, Biomes.SNOWY_TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA,
                Biomes.OLD_GROWTH_SPRUCE_TAIGA,
                Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU,
                Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS,
                Biomes.WINDSWEPT_FOREST, Biomes.WINDSWEPT_SAVANNA,
                Biomes.JUNGLE, Biomes.SPARSE_JUNGLE, Biomes.BAMBOO_JUNGLE,
                Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS,
                Biomes.MEADOW, Biomes.CHERRY_GROVE, Biomes.GROVE, Biomes.SNOWY_SLOPES,
                Biomes.FROZEN_PEAKS, Biomes.JAGGED_PEAKS, Biomes.STONY_PEAKS,
                Biomes.RIVER, Biomes.FROZEN_RIVER,
                Biomes.BEACH, Biomes.SNOWY_BEACH, Biomes.STONY_SHORE,
                Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.COLD_OCEAN, Biomes.DEEP_COLD_OCEAN,
                Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN,
                Biomes.LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN, Biomes.WARM_OCEAN,
                Biomes.MUSHROOM_FIELDS
        ).forEach(source -> addBiomeSimilar(mapper, source, PaozuBiomes.PAOZU_MOUNTAINS));
    }
}
