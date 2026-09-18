package net.kicraft.kicraft.worldgen;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.kicraft.kicraft.Kicraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Stage-two surface pass for Paozu. It runs after vanilla carvers, so gravel is
 * only accepted when its final generated position has stable support.
 */
public final class PaozuSurfacePainter {
    private static final ResourceLocation SURFACE_RANDOM =
            new ResourceLocation(Kicraft.MODID, "paozu_surface_clusters");

    private static final int HEIGHT_BORDER = 2;
    private static final int HEIGHT_GRID_SIZE = 16 + HEIGHT_BORDER * 2;

    private static final BlockState[] ROCK_PALETTE = {
            Blocks.STONE.defaultBlockState(),
            Blocks.COBBLESTONE.defaultBlockState(),
            Blocks.ANDESITE.defaultBlockState(),
            Blocks.TUFF.defaultBlockState(),
            Blocks.DEAD_TUBE_CORAL_BLOCK.defaultBlockState(),
            Blocks.DEAD_BUBBLE_CORAL_BLOCK.defaultBlockState(),
            Blocks.DEAD_HORN_CORAL_BLOCK.defaultBlockState(),
            Blocks.DEAD_FIRE_CORAL_BLOCK.defaultBlockState(),
            Blocks.DEAD_BRAIN_CORAL_BLOCK.defaultBlockState(),
            Blocks.GRAVEL.defaultBlockState()
    };

    private PaozuSurfacePainter() {
    }

    public static void paint(WorldGenRegion region,
                             ChunkAccess chunk,
                             BiomeSource biomeSource,
                             RandomState randomState,
                             NoiseGeneratorSettings settings) {
        ChunkPos chunkPos = chunk.getPos();
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int[][] heights = collectHeights(region, minX, minZ);
        PositionalRandomFactory randomFactory = randomState.getOrCreateRandomFactory(SURFACE_RANDOM);
        RockClusterSampler rockClusters = new RockClusterSampler(randomFactory);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos();
        boolean changed = false;

        for (int localZ = 0; localZ < 16; localZ++) {
            int worldZ = minZ + localZ;
            for (int localX = 0; localX < 16; localX++) {
                int worldX = minX + localX;
                int surfaceY = heights[localX + HEIGHT_BORDER][localZ + HEIGHT_BORDER];
                if (surfaceY <= settings.seaLevel()) {
                    continue;
                }
                if (!isPaozu(biomeSource, randomState, worldX, surfaceY, worldZ)) {
                    continue;
                }

                cursor.set(worldX, surfaceY + 1, worldZ);
                if (!region.getFluidState(cursor).isEmpty()) {
                    continue;
                }

                SurfaceSlope slope = measureSlope(heights, localX + HEIGHT_BORDER,
                        localZ + HEIGHT_BORDER, surfaceY);
                double transition = topPatchValue(randomFactory, worldX, worldZ, 911);
                double rockThreshold = 0.70 + transition * 0.18;
                boolean rocky = slope.gradient() >= rockThreshold || slope.exposedDrop() >= 3;

                if (rocky) {
                    int rockDepth = clamp(
                            3 + slope.exposedDrop() + (int) Math.ceil(slope.gradient() * 3.0),
                            4,
                            96
                    );
                    changed |= paintRockColumn(region, chunk, rockClusters, cursor, below,
                            worldX, surfaceY, worldZ, rockDepth);
                } else {
                    changed |= paintGreenColumn(chunk, randomFactory, cursor,
                            worldX, surfaceY, worldZ, slope.gradient());
                }
            }
        }

        if (changed) {
            chunk.setUnsaved(true);
        }
    }

    private static int[][] collectHeights(WorldGenRegion region, int chunkMinX, int chunkMinZ) {
        int[][] heights = new int[HEIGHT_GRID_SIZE][HEIGHT_GRID_SIZE];
        for (int gridX = 0; gridX < HEIGHT_GRID_SIZE; gridX++) {
            int worldX = chunkMinX + gridX - HEIGHT_BORDER;
            for (int gridZ = 0; gridZ < HEIGHT_GRID_SIZE; gridZ++) {
                int worldZ = chunkMinZ + gridZ - HEIGHT_BORDER;
                heights[gridX][gridZ] =
                        region.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, worldX, worldZ) - 1;
            }
        }
        return heights;
    }

    private static SurfaceSlope measureSlope(int[][] heights, int x, int z, int centerHeight) {
        double gradient = 0.0;
        int lowestNeighbour = centerHeight;

        for (int dz = -2; dz <= 2; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                int neighbourHeight = heights[x + dx][z + dz];
                double distance = Math.sqrt(dx * dx + dz * dz);
                gradient = Math.max(gradient,
                        Math.abs(neighbourHeight - centerHeight) / distance);
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                    lowestNeighbour = Math.min(lowestNeighbour, neighbourHeight);
                }
            }
        }
        return new SurfaceSlope(gradient, Math.max(0, centerHeight - lowestNeighbour));
    }

    private static boolean paintGreenColumn(ChunkAccess chunk,
                                            PositionalRandomFactory randomFactory,
                                            BlockPos.MutableBlockPos cursor,
                                            int x,
                                            int surfaceY,
                                            int z,
                                            double gradient) {
        cursor.set(x, surfaceY, z);
        BlockState surface = chunk.getBlockState(cursor);
        if (surface.isAir() || !surface.getFluidState().isEmpty()) {
            return false;
        }

        double topChoice = topPatchValue(randomFactory, x, z, 1201);
        chunk.setBlockState(cursor,
                topChoice < 0.70 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.MOSS_BLOCK.defaultBlockState(),
                false);

        double depthChoice = topPatchValue(randomFactory, x, z, 1301);
        int dirtDepth = (gradient < 0.28 ? 3 : 2) + (depthChoice < 0.45 ? 1 : 0);
        for (int depth = 1; depth <= dirtDepth; depth++) {
            cursor.set(x, surfaceY - depth, z);
            BlockState current = chunk.getBlockState(cursor);
            if (current.isAir() || !current.getFluidState().isEmpty()) {
                break;
            }
            chunk.setBlockState(cursor, Blocks.DIRT.defaultBlockState(), false);
        }
        return true;
    }

    private static boolean paintRockColumn(WorldGenRegion region,
                                           ChunkAccess chunk,
                                           RockClusterSampler rockClusters,
                                           BlockPos.MutableBlockPos cursor,
                                           BlockPos.MutableBlockPos below,
                                           int x,
                                           int surfaceY,
                                           int z,
                                           int depth) {
        boolean changed = false;
        int minimumY = Math.max(chunk.getMinBuildHeight(), surfaceY - depth + 1);

        for (int y = surfaceY; y >= minimumY; y--) {
            cursor.set(x, y, z);
            BlockState current = chunk.getBlockState(cursor);
            if (current.isAir() || !current.getFluidState().isEmpty()) {
                break;
            }

            BlockState replacement = rockClusters.materialAt(x, y, z);
            if (replacement.is(Blocks.GRAVEL)) {
                below.set(x, y - 1, z);
                BlockState support = region.getBlockState(below);
                if (support.isAir()
                        || !support.getFluidState().isEmpty()
                        || !support.isFaceSturdy(region, below, Direction.UP)) {
                    replacement = Blocks.TUFF.defaultBlockState();
                }
            }

            chunk.setBlockState(cursor, replacement, false);
            changed = true;
        }
        return changed;
    }

    /**
     * A jittered three-dimensional Voronoi grid makes coherent rock patches.
     * The material roll belongs to the winning cell, preserving the requested
     * 30/15/15/10/5/5/5/5/5/5 distribution over a large area.
     */
    private static BlockState materialForRoll(double roll) {
        if (roll < 0.30) return ROCK_PALETTE[0];
        if (roll < 0.45) return ROCK_PALETTE[1];
        if (roll < 0.60) return ROCK_PALETTE[2];
        if (roll < 0.70) return ROCK_PALETTE[3];
        if (roll < 0.75) return ROCK_PALETTE[4];
        if (roll < 0.80) return ROCK_PALETTE[5];
        if (roll < 0.85) return ROCK_PALETTE[6];
        if (roll < 0.90) return ROCK_PALETTE[7];
        if (roll < 0.95) return ROCK_PALETTE[8];
        return ROCK_PALETTE[9];
    }

    private static double topPatchValue(PositionalRandomFactory randomFactory, int x, int z, int salt) {
        final double scale = 15.0;
        int baseCellX = floor(x / scale);
        int baseCellZ = floor(z / scale);
        double bestDistance = Double.MAX_VALUE;
        double selectedValue = 0.0;

        for (int cellZ = baseCellZ - 1; cellZ <= baseCellZ + 1; cellZ++) {
            for (int cellX = baseCellX - 1; cellX <= baseCellX + 1; cellX++) {
                RandomSource cellRandom = randomFactory.at(cellX, salt, cellZ);
                double centerX = (cellX + 0.15 + cellRandom.nextDouble() * 0.70) * scale;
                double centerZ = (cellZ + 0.15 + cellRandom.nextDouble() * 0.70) * scale;
                double dx = x - centerX;
                double dz = z - centerZ;
                double distance = dx * dx + dz * dz;
                if (distance < bestDistance) {
                    bestDistance = distance;
                    selectedValue = cellRandom.nextDouble();
                }
            }
        }
        return selectedValue;
    }

    private static boolean isPaozu(BiomeSource biomeSource,
                                    RandomState randomState,
                                    int x,
                                    int y,
                                    int z) {
        Holder<Biome> biome = biomeSource.getNoiseBiome(
                QuartPos.fromBlock(x),
                QuartPos.fromBlock(y),
                QuartPos.fromBlock(z),
                randomState.sampler()
        );
        return biome.is(PaozuBiomes.PAOZU_MOUNTAINS);
    }

    private static int floor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class RockClusterSampler {
        private static final double HORIZONTAL_SCALE = 11.0;
        private static final double VERTICAL_SCALE = 8.0;

        private final PositionalRandomFactory randomFactory;
        private final Long2ObjectOpenHashMap<RockCell> cells = new Long2ObjectOpenHashMap<>();

        private RockClusterSampler(PositionalRandomFactory randomFactory) {
            this.randomFactory = randomFactory;
        }

        private BlockState materialAt(int x, int y, int z) {
            int baseCellX = floor(x / HORIZONTAL_SCALE);
            int baseCellY = floor(y / VERTICAL_SCALE);
            int baseCellZ = floor(z / HORIZONTAL_SCALE);
            double bestDistance = Double.MAX_VALUE;
            BlockState selectedMaterial = Blocks.STONE.defaultBlockState();

            for (int cellZ = baseCellZ - 1; cellZ <= baseCellZ + 1; cellZ++) {
                for (int cellY = baseCellY - 1; cellY <= baseCellY + 1; cellY++) {
                    for (int cellX = baseCellX - 1; cellX <= baseCellX + 1; cellX++) {
                        RockCell cell = cell(cellX, cellY, cellZ);
                        double dx = x - cell.centerX();
                        double dy = y - cell.centerY();
                        double dz = z - cell.centerZ();
                        double distance = dx * dx + dy * dy + dz * dz;
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            selectedMaterial = cell.material();
                        }
                    }
                }
            }
            return selectedMaterial;
        }

        private RockCell cell(int x, int y, int z) {
            long key = ((long) x & 0x7FFFFFL) << 41
                    | ((long) z & 0x7FFFFFL) << 18
                    | ((long) y & 0x3FFFFL);
            RockCell cached = cells.get(key);
            if (cached != null) {
                return cached;
            }

            RandomSource random = randomFactory.at(x, y, z);
            RockCell created = new RockCell(
                    (x + 0.15 + random.nextDouble() * 0.70) * HORIZONTAL_SCALE,
                    (y + 0.15 + random.nextDouble() * 0.70) * VERTICAL_SCALE,
                    (z + 0.15 + random.nextDouble() * 0.70) * HORIZONTAL_SCALE,
                    materialForRoll(random.nextDouble())
            );
            cells.put(key, created);
            return created;
        }
    }

    private record RockCell(double centerX, double centerY, double centerZ, BlockState material) {
    }

    private record SurfaceSlope(double gradient, int exposedDrop) {
    }
}
