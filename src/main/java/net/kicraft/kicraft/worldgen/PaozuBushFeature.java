package net.kicraft.kicraft.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** A compact, one-log oak bush used only on level green Paozu ground. */
public final class PaozuBushFeature extends Feature<NoneFeatureConfiguration> {
    private static final BlockState LOG = Blocks.OAK_LOG.defaultBlockState();
    private static final BlockState LEAVES = Blocks.OAK_LEAVES.defaultBlockState()
            .setValue(LeavesBlock.PERSISTENT, true);

    public PaozuBushFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();

        // Requiring a level 3x3 green footprint keeps the bush out of water,
        // cliff faces, overhangs and exposed rock shelves.
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                BlockPos leafPosition = origin.offset(dx, 0, dz);
                if (!isGreenGround(level.getBlockState(leafPosition.below()))
                        || !isEmptyAndDry(level, leafPosition)) {
                    return false;
                }
            }
        }

        BlockPos[] upperLeaves = {
                origin.above(),
                origin.above().north(),
                origin.above().south(),
                origin.above().east(),
                origin.above().west()
        };
        for (BlockPos leafPosition : upperLeaves) {
            if (!isEmptyAndDry(level, leafPosition)) {
                return false;
            }
        }

        level.setBlock(origin, LOG, 2);
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx != 0 || dz != 0) {
                    level.setBlock(origin.offset(dx, 0, dz), LEAVES, 2);
                }
            }
        }
        for (BlockPos leafPosition : upperLeaves) {
            level.setBlock(leafPosition, LEAVES, 2);
        }
        return true;
    }

    private static boolean isGreenGround(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MOSS_BLOCK);
    }

    private static boolean isEmptyAndDry(WorldGenLevel level, BlockPos position) {
        return level.getBlockState(position).isAir() && level.getFluidState(position).isEmpty();
    }
}
