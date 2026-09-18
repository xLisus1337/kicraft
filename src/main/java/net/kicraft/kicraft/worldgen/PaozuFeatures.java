package net.kicraft.kicraft.worldgen;

import net.kicraft.kicraft.Kicraft;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class PaozuFeatures {
    private static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(ForgeRegistries.FEATURES, Kicraft.MODID);

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> PAOZU_BUSH =
            FEATURES.register("paozu_bush",
                    () -> new PaozuBushFeature(NoneFeatureConfiguration.CODEC));

    private PaozuFeatures() {
    }

    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
    }
}
