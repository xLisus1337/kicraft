package net.kicraft.kicraft.content;

import net.kicraft.kicraft.Kicraft;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class KicraftBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Kicraft.MODID);

    public static final RegistryObject<Block> TRAINING_MAT = BLOCKS.register("training_mat", () ->
            new TrainingMatBlock(Block.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(0.1F)
                    .sound(SoundType.WOOL)
                    .noOcclusion())
    );

    private KicraftBlocks() {
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
