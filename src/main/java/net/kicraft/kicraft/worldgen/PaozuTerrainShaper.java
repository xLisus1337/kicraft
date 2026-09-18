package net.kicraft.kicraft.worldgen;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.kicraft.kicraft.Kicraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
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

import java.util.EnumSet;

/**
 * Stage-one Paozu terrain. This class only changes the solid base shape. Surface
 * blocks, decorations and caves remain the responsibility of vanilla worldgen.
 */
public final class PaozuTerrainShaper {
    private static final ResourceLocation TERRAIN_RANDOM =
            new ResourceLocation(Kicraft.MODID, "paozu_terrain_shape");

    private static final int BIOME_SAMPLE_Y = 80;

    // Biome-edge controls. Samples extend into both biomes, so the custom shape
    // starts fading before the exact biome line instead of ending at a hard wall.
    private static final int EDGE_TRANSITION_WIDTH = 96;
    private static final int EDGE_SAMPLE_STEP = 32;
    private static final double EDGE_HEIGHT_ATTENUATION = 1.08;
    private static final double EDGE_STEEPNESS_ATTENUATION = 1.30;
    private static final double EDGE_INTERIOR_RECOVERY_EXPONENT = 1.45;
    private static final double EDGE_FOOTHILL_RELIEF_SCALE = 0.38;
    private static final double EDGE_FOOTHILL_MAX_RELIEF = 42.0;

    // Broad shared noise groups all four formation scales into irregular mountain
    // regions. The floor keeps sparse regions hilly instead of completely empty.
    private static final double MOUNTAIN_CLUSTER_NOISE_SCALE = 620.0;
    private static final double MOUNTAIN_CLUSTER_DENSITY_FLOOR = 0.38;
    private static final double MOUNTAIN_CLUSTER_DENSITY_BOOST = 1.05;

    // Each hierarchy layer has an independent spacing and average activation rate.
    // These are deliberately top-level constants so visual tuning stays simple.
    private static final double SMALL_MOUNTAIN_SPACING = 74.0;
    private static final double SMALL_MOUNTAIN_FREQUENCY = 0.78;
    private static final double MEDIUM_MOUNTAIN_SPACING = 112.0;
    private static final double MEDIUM_MOUNTAIN_FREQUENCY = 0.63;
    private static final double LARGE_MOUNTAIN_SPACING = 190.0;
    private static final double LARGE_MOUNTAIN_FREQUENCY = 0.30;
    private static final double LANDMARK_MOUNTAIN_SPACING = 480.0;
    private static final double LANDMARK_MOUNTAIN_FREQUENCY = 0.10;

    // Local shape controls. These vary each formation and break the repeated
    // cylinder silhouette while retaining the cliff-and-shelf character.
    private static final double MOUNTAIN_SIDE_STEEPNESS_VARIATION = 0.24;
    private static final double SUMMIT_PLATEAU_MIN_RADIUS = 0.040;
    private static final double SUMMIT_PLATEAU_RADIUS_RANGE = 0.215;
    private static final double SUMMIT_PLATEAU_BIAS = 2.10;
    private static final double SUMMIT_VARIATION_STRENGTH = 0.085;
    private static final double ASYMMETRY_RADIUS_VARIATION = 0.22;
    private static final double ASYMMETRY_LEAN_STRENGTH = 0.18;
    private static final double SIDE_WIDTH_VARIATION = 0.052;
    private static final double LOCAL_SHELF_NOISE_THRESHOLD = 0.54;
    private static final double LOCAL_SHELF_FULL_THRESHOLD = 0.78;
    private static final double LOCAL_SHELF_MIN_BLEND = 0.04;
    private static final double LOCAL_SHELF_STRENGTH_SCALE = 0.72;
    private static final double LOCAL_SHELF_LEVEL_WARP = 0.10;
    private static final double SHOULDER_FREQUENCY = 0.36;
    private static final double FOOTHILL_FREQUENCY = 0.45;

    // Narrow zero crossings remain as passes and future river corridors even in
    // dense clusters. Raising the outer threshold widens these valleys.
    private static final double VALLEY_NOISE_SCALE = 300.0;
    private static final double VALLEY_INNER_WIDTH = 0.018;
    private static final double VALLEY_OUTER_WIDTH = 0.090;
    private static final double VALLEY_BASE_CARVE = 7.0;
    private static final double VALLEY_RELIEF_CARVE = 0.52;

    private static final double MAX_RELIEF = 230.0;

    private static final FormationLayer SMALL_MOUNTAINS = new FormationLayer(
            101, SMALL_MOUNTAIN_SPACING, SMALL_MOUNTAIN_FREQUENCY,
            10.0, 34.0, 1.05,
            14.0, 11.0, 0.17
    );
    private static final FormationLayer MEDIUM_MOUNTAINS = new FormationLayer(
            211, MEDIUM_MOUNTAIN_SPACING, MEDIUM_MOUNTAIN_FREQUENCY,
            26.0, 92.0, 1.15,
            22.0, 17.0, 0.16
    );
    private static final FormationLayer LARGE_MOUNTAINS = new FormationLayer(
            307, LARGE_MOUNTAIN_SPACING, LARGE_MOUNTAIN_FREQUENCY,
            68.0, 165.0, 1.28,
            36.0, 22.0, 0.13
    );
    private static final FormationLayer LANDMARK_MOUNTAINS = new FormationLayer(
            419, LANDMARK_MOUNTAIN_SPACING, LANDMARK_MOUNTAIN_FREQUENCY,
            145.0, MAX_RELIEF, 1.45,
            55.0, 25.0, 0.11
    );

    private PaozuTerrainShaper() {
    }

    public static void shape(ChunkAccess chunk,
                             BiomeSource biomeSource,
                             RandomState randomState,
                             NoiseGeneratorSettings settings) {
        ChunkPos chunkPos = chunk.getPos();
        int minY = chunk.getMinBuildHeight();
        int maxY = minY + chunk.getHeight() - 1;
        int seaLevel = settings.seaLevel();
        PositionalRandomFactory randomFactory = randomState.getOrCreateRandomFactory(TERRAIN_RANDOM);
        TerrainSampler terrainSampler = new TerrainSampler(randomFactory, seaLevel, maxY);
        BlockState defaultBlock = settings.defaultBlock();
        BlockState defaultFluid = settings.defaultFluid();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        boolean changed = false;

        for (int localZ = 0; localZ < 16; localZ++) {
            int worldZ = chunkPos.getMinBlockZ() + localZ;
            for (int localX = 0; localX < 16; localX++) {
                int worldX = chunkPos.getMinBlockX() + localX;
                double biomeWeight = biomeWeight(biomeSource, randomState, worldX, worldZ);
                if (biomeWeight <= 0.01) {
                    continue;
                }

                int vanillaSurface = findSolidSurface(chunk, cursor, worldX, worldZ, minY, maxY);
                double easedWeight = Math.pow(
                        smootherStep(biomeWeight),
                        EDGE_HEIGHT_ATTENUATION
                );
                double interiorStrength = Math.pow(
                        easedWeight,
                        EDGE_INTERIOR_RECOVERY_EXPONENT
                );
                int paozuSurface = terrainSampler.heightAt(
                        worldX, worldZ, interiorStrength);
                int targetSurface = clamp(
                        (int) Math.round(lerp(vanillaSurface, paozuSurface, easedWeight)),
                        minY + 1,
                        maxY - 1
                );

                if (targetSurface > vanillaSurface) {
                    for (int y = vanillaSurface + 1; y <= targetSurface; y++) {
                        cursor.set(worldX, y, worldZ);
                        chunk.setBlockState(cursor, defaultBlock, false);
                    }
                    changed = true;
                } else if (targetSurface < vanillaSurface) {
                    for (int y = vanillaSurface; y > targetSurface; y--) {
                        cursor.set(worldX, y, worldZ);
                        BlockState replacement = y < seaLevel ? defaultFluid : Blocks.AIR.defaultBlockState();
                        chunk.setBlockState(cursor, replacement, false);
                    }
                    changed = true;
                }
            }
        }

        if (changed) {
            Heightmap.primeHeightmaps(chunk, EnumSet.of(
                    Heightmap.Types.WORLD_SURFACE_WG,
                    Heightmap.Types.OCEAN_FLOOR_WG
            ));
            chunk.setUnsaved(true);
        }
    }

    private static final class TerrainSampler {
        private final PositionalRandomFactory randomFactory;
        private final int seaLevel;
        private final int maxBuildY;
        private final Long2ObjectOpenHashMap<MountainCell> smallCells = new Long2ObjectOpenHashMap<>();
        private final Long2ObjectOpenHashMap<MountainCell> mediumCells = new Long2ObjectOpenHashMap<>();
        private final Long2ObjectOpenHashMap<MountainCell> largeCells = new Long2ObjectOpenHashMap<>();
        private final Long2ObjectOpenHashMap<MountainCell> landmarkCells = new Long2ObjectOpenHashMap<>();

        private TerrainSampler(PositionalRandomFactory randomFactory, int seaLevel, int maxBuildY) {
            this.randomFactory = randomFactory;
            this.seaLevel = seaLevel;
            this.maxBuildY = maxBuildY;
        }

        private int heightAt(int x, int z, double interiorStrength) {
            // Gentle coordinate warping gives every footprint an irregular edge,
            // while its large scale avoids per-block spikes and floating pieces.
            double warpX = fractal(randomFactory, x, z, 520.0, 3, 11) * 48.0;
            double warpZ = fractal(randomFactory, x, z, 520.0, 3, 29) * 48.0;
            double sampleX = x + warpX;
            double sampleZ = z + warpZ;

            // Dense small and medium layers fill the view around the less common
            // large formations. Maximum composition preserves local valleys and
            // prevents overlapping mountains from adding into one giant massif.
            double mountainRelief = sampleLayer(
                    SMALL_MOUNTAINS, smallCells, sampleX, sampleZ);
            mountainRelief = Math.max(mountainRelief, sampleLayer(
                    MEDIUM_MOUNTAINS, mediumCells, sampleX, sampleZ));
            mountainRelief = Math.max(mountainRelief, sampleLayer(
                    LARGE_MOUNTAINS, largeCells, sampleX, sampleZ));
            mountainRelief = Math.max(mountainRelief, sampleLayer(
                    LANDMARK_MOUNTAINS, landmarkCells, sampleX, sampleZ));

            // Broad zero crossings cut occasional passes through neighbouring
            // formations. The field changes slowly enough to form valleys, not cracks.
            double divideField = Math.abs(fractal(randomFactory,
                    sampleX + 431.0, sampleZ - 277.0, VALLEY_NOISE_SCALE, 3, 907));
            double divide = 1.0 - smootherStep(remapClamped(
                    divideField, VALLEY_INNER_WIDTH, VALLEY_OUTER_WIDTH));
            mountainRelief = Math.max(0.0,
                    mountainRelief - divide
                            * (VALLEY_BASE_CARVE + mountainRelief * VALLEY_RELIEF_CARVE));

            // Low rolling ground keeps valleys from becoming a featureless plane.
            // It stays far below the independent mountain relief distribution.
            double rolling = fractal(randomFactory, sampleX, sampleZ, 138.0, 3, 1201) * 5.5;
            double lowHillField = unit(fractal(randomFactory,
                    sampleX - 193.0, sampleZ + 317.0, 112.0, 3, 1237));
            double lowHills = 3.0 + 14.0 * Math.pow(
                    smootherStep(remapClamped(lowHillField, 0.34, 0.84)),
                    1.22
            );

            // Near a biome border, the same footprint becomes a low foothill. Full
            // relief returns progressively toward the interior, suppressing a tall
            // cliff or landmark directly against plains/forest terrain.
            double edgeFoothillRelief = Math.min(
                    mountainRelief * EDGE_FOOTHILL_RELIEF_SCALE,
                    EDGE_FOOTHILL_MAX_RELIEF
            );
            double reliefRecovery = Math.pow(
                    clamp(interiorStrength, 0.0, 1.0),
                    EDGE_STEEPNESS_ATTENUATION
            );
            double blendedRelief = lerp(
                    edgeFoothillRelief,
                    mountainRelief,
                    reliefRecovery
            );

            double height = seaLevel + 6.0 + rolling + lowHills + blendedRelief;
            int minimumValleyY = seaLevel + 3;
            int highestAllowedY = Math.min(seaLevel + (int) MAX_RELIEF, maxBuildY - 1);
            return clamp((int) Math.round(height), minimumValleyY, highestAllowedY);
        }

        private double sampleLayer(FormationLayer layer,
                                   Long2ObjectOpenHashMap<MountainCell> cells,
                                   double sampleX,
                                   double sampleZ) {
            int baseCellX = floor(sampleX / layer.spacing());
            int baseCellZ = floor(sampleZ / layer.spacing());
            double layerRelief = 0.0;

            for (int cellZ = baseCellZ - 1; cellZ <= baseCellZ + 1; cellZ++) {
                for (int cellX = baseCellX - 1; cellX <= baseCellX + 1; cellX++) {
                    layerRelief = Math.max(layerRelief,
                            cell(layer, cells, cellX, cellZ)
                                    .reliefAt(randomFactory, sampleX, sampleZ));
                }
            }
            return layerRelief;
        }

        private MountainCell cell(FormationLayer layer,
                                  Long2ObjectOpenHashMap<MountainCell> cells,
                                  int cellX,
                                  int cellZ) {
            long key = ((long) cellX << 32) ^ (cellZ & 0xFFFFFFFFL);
            MountainCell cached = cells.get(key);
            if (cached != null) {
                return cached;
            }

            RandomSource random = randomFactory.at(cellX, layer.salt(), cellZ);
            double centerX = (cellX + 0.12 + random.nextDouble() * 0.76) * layer.spacing();
            double centerZ = (cellZ + 0.12 + random.nextDouble() * 0.76) * layer.spacing();

            // Every scale reads the same macro field, forming clusters. Independent
            // secondary fields scatter mountains naturally inside each cluster.
            double clusterStrength = smootherStep(unit(fractal(randomFactory,
                    centerX, centerZ, MOUNTAIN_CLUSTER_NOISE_SCALE, 4, 421)));
            double secondaryPlacement = unit(fractal(randomFactory,
                    centerX + layer.salt() * 0.71,
                    centerZ - layer.salt() * 0.53,
                    layer.spacing() * 3.4,
                    3,
                    layer.salt() + 37));
            double clusterMultiplier = MOUNTAIN_CLUSTER_DENSITY_FLOOR
                    + MOUNTAIN_CLUSTER_DENSITY_BOOST * clusterStrength;
            double secondaryMultiplier = 0.72 + secondaryPlacement * 0.56;
            double activeChance = clamp(
                    layer.frequency() * clusterMultiplier * secondaryMultiplier,
                    0.02,
                    0.96
            );
            if (random.nextDouble() > activeChance) {
                MountainCell empty = MountainCell.empty(centerX, centerZ);
                cells.put(key, empty);
                return empty;
            }

            // Scale noise controls height independently from cluster placement.
            // Overlapping ranges avoid four visibly discrete height categories.
            double heightNoise = unit(fractal(randomFactory,
                    centerX + 173.0,
                    centerZ - 239.0,
                    layer.spacing() * 4.7,
                    3,
                    layer.salt() + 457));
            double heightRoll = clamp(
                    random.nextDouble() * 0.70 + heightNoise * 0.30,
                    0.0,
                    1.0
            );
            double relief = layer.minimumRelief()
                    + (layer.maximumRelief() - layer.minimumRelief())
                    * Math.pow(heightRoll, layer.heightExponent());

            double baseRadius = layer.minimumRadius()
                    + random.nextDouble() * layer.randomRadiusRange()
                    + relief * layer.heightRadiusFactor();
            double westRadius = baseRadius * asymmetricRadiusMultiplier(random);
            double eastRadius = baseRadius * asymmetricRadiusMultiplier(random);
            double northRadius = baseRadius * asymmetricRadiusMultiplier(random);
            double southRadius = baseRadius * asymmetricRadiusMultiplier(random);

            double angle = random.nextDouble() * Math.PI * 2.0;

            int terraceBands = 2 + (int) Math.floor(relief / 72.0) + random.nextInt(2);
            double shelfActivation = 0.22 + 0.78
                    * smootherStep(remapClamped(relief, 26.0, 95.0));
            double terraceStrength = shelfActivation * (0.78 + random.nextDouble() * 0.18);

            // Small plateaus are common; broad flat discs remain possible but rare.
            double summitPlateauRadius = SUMMIT_PLATEAU_MIN_RADIUS
                    + SUMMIT_PLATEAU_RADIUS_RANGE
                    * Math.pow(random.nextDouble(), SUMMIT_PLATEAU_BIAS);
            double profileExponent = 1.0 + (random.nextDouble() * 2.0 - 1.0)
                    * MOUNTAIN_SIDE_STEEPNESS_VARIATION;
            double summitVariation = SUMMIT_VARIATION_STRENGTH
                    * (0.55 + random.nextDouble() * 0.90);

            double shoulderChance = SHOULDER_FREQUENCY
                    * (0.25 + 0.75 * smootherStep(remapClamped(relief, 35.0, 125.0)));
            Shoulder shoulder = Shoulder.none();
            if (random.nextDouble() < shoulderChance) {
                double shoulderAngle = random.nextDouble() * Math.PI * 2.0;
                double shoulderDistance = baseRadius * (0.42 + random.nextDouble() * 0.20);
                double shoulderRadius = baseRadius * (0.32 + random.nextDouble() * 0.22);
                shoulder = new Shoulder(
                        centerX + Math.cos(shoulderAngle) * shoulderDistance,
                        centerZ + Math.sin(shoulderAngle) * shoulderDistance,
                        shoulderRadius * (0.82 + random.nextDouble() * 0.32),
                        shoulderRadius * (0.82 + random.nextDouble() * 0.32),
                        0.34 + random.nextDouble() * 0.35
                );
            }

            double foothillChance = FOOTHILL_FREQUENCY
                    * (0.35 + 0.65 * smootherStep(remapClamped(relief, 30.0, 110.0)));
            double foothillStrength = random.nextDouble() < foothillChance
                    ? 0.09 + random.nextDouble() * 0.11
                    : 0.0;
            double foothillRadius = 1.08 + random.nextDouble() * 0.22;

            MountainCell created = new MountainCell(
                    true,
                    centerX,
                    centerZ,
                    westRadius,
                    eastRadius,
                    northRadius,
                    southRadius,
                    relief,
                    summitPlateauRadius,
                    profileExponent,
                    summitVariation,
                    terraceBands,
                    terraceStrength,
                    random.nextDouble() * 2.0 - 1.0,
                    random.nextDouble() * 2.0 - 1.0,
                    foothillStrength,
                    foothillRadius,
                    shoulder,
                    Math.cos(angle),
                    Math.sin(angle),
                    random.nextInt(1_000_000)
            );
            cells.put(key, created);
            return created;
        }

        private static double asymmetricRadiusMultiplier(RandomSource random) {
            return 1.0 - ASYMMETRY_RADIUS_VARIATION
                    + random.nextDouble() * ASYMMETRY_RADIUS_VARIATION * 2.0;
        }
    }

    private record FormationLayer(int salt,
                                  double spacing,
                                  double frequency,
                                  double minimumRelief,
                                  double maximumRelief,
                                  double heightExponent,
                                  double minimumRadius,
                                  double randomRadiusRange,
                                  double heightRadiusFactor) {
    }

    private record MountainCell(boolean active,
                                double centerX,
                                double centerZ,
                                double westRadius,
                                double eastRadius,
                                double northRadius,
                                double southRadius,
                                double relief,
                                double summitPlateauRadius,
                                double profileExponent,
                                double summitVariation,
                                int terraceBands,
                                double terraceStrength,
                                double leanX,
                                double leanZ,
                                double foothillStrength,
                                double foothillRadius,
                                Shoulder shoulder,
                                double cosAngle,
                                double sinAngle,
                                int detailSalt) {
        private static MountainCell empty(double centerX, double centerZ) {
            return new MountainCell(false, centerX, centerZ,
                    1.0, 1.0, 1.0, 1.0,
                    0.0, 0.0, 1.0, 0.0, 1, 0.0,
                    0.0, 0.0, 0.0, 1.0, Shoulder.none(),
                    1.0, 0.0, 0);
        }

        private double reliefAt(PositionalRandomFactory randomFactory,
                                double sampleX,
                                double sampleZ) {
            if (!active) {
                return 0.0;
            }

            double dx = sampleX - centerX;
            double dz = sampleZ - centerZ;
            double rotatedX = dx * cosAngle - dz * sinAngle;
            double rotatedZ = dx * sinAngle + dz * cosAngle;
            double radiusX = rotatedX < 0.0 ? westRadius : eastRadius;
            double radiusZ = rotatedZ < 0.0 ? northRadius : southRadius;
            double normalizedX = rotatedX / radiusX;
            double normalizedZ = rotatedZ / radiusZ;
            double radialDistance = Math.sqrt(
                    normalizedX * normalizedX + normalizedZ * normalizedZ);
            double shoulderDistance = shoulder.distanceTo(sampleX, sampleZ);

            if (radialDistance > 1.38 && shoulderDistance > 1.12) {
                return 0.0;
            }

            // Coherent erosion changes the outline and wall angle over dozens of
            // blocks. It cannot create one-block needles or detached terrain.
            double erosion = fractal(randomFactory,
                    sampleX + detailSalt * 0.013,
                    sampleZ - detailSalt * 0.017,
                    76.0,
                    3,
                    detailSalt + 601);
            radialDistance += erosion * 0.105
                    * (0.38 + Math.min(radialDistance, 1.0) * 0.62);
            double mainRelief = radialDistance < 1.0
                    ? mainProfile(randomFactory, sampleX, sampleZ,
                            normalizedX, normalizedZ, radialDistance) * relief
                    : 0.0;
            double shoulderRelief = shoulderProfile(shoulderDistance) * relief;
            double foothillRelief = foothillProfile(
                    randomFactory, sampleX, sampleZ, radialDistance) * relief;

            return Math.max(mainRelief, Math.max(shoulderRelief, foothillRelief));
        }

        private double mainProfile(PositionalRandomFactory randomFactory,
                                   double sampleX,
                                   double sampleZ,
                                   double normalizedX,
                                   double normalizedZ,
                                   double radialDistance) {
            double profile = 1.0 - smootherStep(remapClamped(
                    radialDistance, summitPlateauRadius, 1.0));
            profile = Math.pow(clamp(profile, 0.0, 1.0), profileExponent);

            // Unequal footprint radii and a stronger directional lean allow a
            // cliff face on one side and a longer descending side opposite it.
            double directionalLean = clamp(
                    1.0 + ASYMMETRY_LEAN_STRENGTH
                            * (leanX * normalizedX + leanZ * normalizedZ),
                    0.68,
                    1.30
            );
            profile = clamp(profile * directionalLean, 0.0, 1.0);

            // A vertical band modulation changes the apparent width at different
            // heights. Its spatial patch term prevents a full circular ring.
            double wallMask = smootherStep(remapClamped(profile, 0.08, 0.30))
                    * (1.0 - smootherStep(remapClamped(profile, 0.82, 0.97)));
            double sidePatch = fractal(randomFactory,
                    sampleX + detailSalt * 0.007,
                    sampleZ - detailSalt * 0.009,
                    112.0,
                    2,
                    detailSalt + 733);
            double phase = (detailSalt & 1023) / 1023.0 * Math.PI * 2.0;
            double verticalBand = Math.sin(
                    (profile * (2.15 + terraceBands * 0.31)) * Math.PI * 2.0 + phase);
            profile += verticalBand * sidePatch * SIDE_WIDTH_VARIATION * wallMask;

            // Summit-only coherent noise produces sloped, split and locally raised
            // tops. The value is clamped, so it cannot increase the configured cap.
            double averageRadius = (westRadius + eastRadius + northRadius + southRadius) * 0.25;
            double summitNoise = fractal(randomFactory,
                    sampleX - detailSalt * 0.023,
                    sampleZ + detailSalt * 0.021,
                    Math.max(18.0, averageRadius * 0.42),
                    2,
                    detailSalt + 991);
            double summitMask = smootherStep(remapClamped(profile, 0.58, 0.94));
            profile = clamp(profile + summitNoise * summitVariation * summitMask, 0.0, 1.0);

            // Terracing is strong only inside coherent local patches. Elsewhere the
            // original slope remains, so ledges stop instead of circling the tower.
            double localShelfNoise = unit(fractal(randomFactory,
                    sampleX - detailSalt * 0.019,
                    sampleZ + detailSalt * 0.011,
                    118.0,
                    3,
                    detailSalt + 811));
            double shelfPatch = smootherStep(remapClamped(
                    localShelfNoise,
                    LOCAL_SHELF_NOISE_THRESHOLD,
                    LOCAL_SHELF_FULL_THRESHOLD
            ));
            double localTerraceStrength = terraceStrength
                    * (LOCAL_SHELF_MIN_BLEND
                    + (1.0 - LOCAL_SHELF_MIN_BLEND) * shelfPatch)
                    * LOCAL_SHELF_STRENGTH_SCALE;
            double shelfLevelWarp = fractal(randomFactory,
                    sampleX + detailSalt * 0.029,
                    sampleZ - detailSalt * 0.015,
                    82.0,
                    2,
                    detailSalt + 877) * LOCAL_SHELF_LEVEL_WARP;
            profile = lerp(profile,
                    terraceProfile(profile, terraceBands, shelfLevelWarp),
                    localTerraceStrength);
            return clamp(profile, 0.0, 1.0);
        }

        private double shoulderProfile(double shoulderDistance) {
            if (shoulder.heightFactor() <= 0.0 || shoulderDistance >= 1.0) {
                return 0.0;
            }

            double profile = 1.0 - smootherStep(remapClamped(shoulderDistance, 0.07, 1.0));
            double terraced = terraceProfile(profile, Math.max(2, terraceBands - 1));
            profile = lerp(profile, terraced, terraceStrength * 0.48);
            return shoulder.heightFactor() * clamp(profile, 0.0, 1.0);
        }

        private double foothillProfile(PositionalRandomFactory randomFactory,
                                       double sampleX,
                                       double sampleZ,
                                       double radialDistance) {
            if (foothillStrength <= 0.0 || radialDistance >= foothillRadius) {
                return 0.0;
            }

            double foothillPatch = unit(fractal(randomFactory,
                    sampleX + detailSalt * 0.031,
                    sampleZ - detailSalt * 0.027,
                    104.0,
                    2,
                    detailSalt + 1103));
            foothillPatch = smootherStep(remapClamped(foothillPatch, 0.42, 0.70));
            double baseProfile = 1.0 - smootherStep(remapClamped(
                    radialDistance, 0.62, foothillRadius));
            return foothillStrength * baseProfile * foothillPatch;
        }
    }

    private record Shoulder(double centerX,
                            double centerZ,
                            double radiusX,
                            double radiusZ,
                            double heightFactor) {
        private static Shoulder none() {
            return new Shoulder(0.0, 0.0, 1.0, 1.0, 0.0);
        }

        private double distanceTo(double x, double z) {
            if (heightFactor <= 0.0) {
                return Double.MAX_VALUE;
            }
            double dx = (x - centerX) / radiusX;
            double dz = (z - centerZ) / radiusZ;
            return Math.sqrt(dx * dx + dz * dz);
        }
    }

    private static double terraceProfile(double profile, int bands) {
        return terraceProfile(profile, bands, 0.0);
    }

    private static double terraceProfile(double profile, int bands, double levelWarp) {
        double scaled = clamp(profile, 0.0, 1.0) * bands + levelWarp;
        int level = floor(scaled);
        if (profile >= 1.0) {
            return 1.0;
        }
        double fraction = scaled - level;
        double cliffTransition = smootherStep(remapClamped(fraction, 0.40, 0.60));
        return clamp((level + cliffTransition - levelWarp) / bands, 0.0, 1.0);
    }

    private static double biomeWeight(BiomeSource biomeSource,
                                      RandomState randomState,
                                      int x,
                                      int z) {
        double weight = 0.0;
        double totalWeight = 0.0;

        for (int dz = -EDGE_TRANSITION_WIDTH;
             dz <= EDGE_TRANSITION_WIDTH;
             dz += EDGE_SAMPLE_STEP) {
            double zWeight = edgeSampleWeight(dz);
            for (int dx = -EDGE_TRANSITION_WIDTH;
                 dx <= EDGE_TRANSITION_WIDTH;
                 dx += EDGE_SAMPLE_STEP) {
                double sampleWeight = edgeSampleWeight(dx) * zWeight;
                totalWeight += sampleWeight;
                Holder<Biome> biome = biomeSource.getNoiseBiome(
                        QuartPos.fromBlock(x + dx),
                        QuartPos.fromBlock(BIOME_SAMPLE_Y),
                        QuartPos.fromBlock(z + dz),
                        randomState.sampler()
                );
                if (biome.is(PaozuBiomes.PAOZU_MOUNTAINS)) {
                    weight += sampleWeight;
                }
            }
        }
        return totalWeight > 0.0 ? weight / totalWeight : 0.0;
    }

    private static double edgeSampleWeight(int offset) {
        double distance = Math.abs(offset)
                / (double) (EDGE_TRANSITION_WIDTH + EDGE_SAMPLE_STEP);
        return 1.0 - smootherStep(distance);
    }

    private static int findSolidSurface(ChunkAccess chunk,
                                        BlockPos.MutableBlockPos cursor,
                                        int x,
                                        int z,
                                        int minY,
                                        int maxY) {
        for (int y = maxY; y >= minY; y--) {
            cursor.set(x, y, z);
            BlockState state = chunk.getBlockState(cursor);
            if (!state.isAir() && state.getFluidState().isEmpty()) {
                return y;
            }
        }
        return minY;
    }

    private static double fractal(PositionalRandomFactory randomFactory,
                                  double x,
                                  double z,
                                  double largestScale,
                                  int octaves,
                                  int salt) {
        double value = 0.0;
        double amplitude = 1.0;
        double amplitudeSum = 0.0;
        double scale = largestScale;

        for (int octave = 0; octave < octaves; octave++) {
            value += valueNoise(randomFactory, x, z, scale, salt + octave * 37) * amplitude;
            amplitudeSum += amplitude;
            amplitude *= 0.52;
            scale *= 0.5;
        }
        return value / amplitudeSum;
    }

    private static double valueNoise(PositionalRandomFactory randomFactory,
                                     double x,
                                     double z,
                                     double scale,
                                     int salt) {
        double scaledX = x / scale;
        double scaledZ = z / scale;
        int x0 = floor(scaledX);
        int z0 = floor(scaledZ);
        double tx = smootherStep(scaledX - x0);
        double tz = smootherStep(scaledZ - z0);

        double a = latticeValue(randomFactory, x0, z0, salt);
        double b = latticeValue(randomFactory, x0 + 1, z0, salt);
        double c = latticeValue(randomFactory, x0, z0 + 1, salt);
        double d = latticeValue(randomFactory, x0 + 1, z0 + 1, salt);
        return lerp(lerp(a, b, tx), lerp(c, d, tx), tz);
    }

    private static double latticeValue(PositionalRandomFactory randomFactory, int x, int z, int salt) {
        return randomFactory.at(x, salt, z).nextDouble() * 2.0 - 1.0;
    }

    private static int floor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }

    private static double unit(double value) {
        return clamp(value * 0.5 + 0.5, 0.0, 1.0);
    }

    private static double remapClamped(double value, double low, double high) {
        return clamp((value - low) / (high - low), 0.0, 1.0);
    }

    private static double smootherStep(double value) {
        double x = clamp(value, 0.0, 1.0);
        return x * x * x * (x * (x * 6.0 - 15.0) + 10.0);
    }

    private static double lerp(double a, double b, double amount) {
        return a + (b - a) * amount;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
