package net.kicraft.kicraft.mixin;

import net.kicraft.kicraft.worldgen.PaozuTerrainShaper;
import net.kicraft.kicraft.worldgen.PaozuSurfacePainter;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Shapes Paozu while a chunk is still at the NOISE stage. Vanilla surface rules
 * and carvers therefore run afterwards, just as they do in an ordinary world.
 */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class PaozuNoiseChunkGeneratorMixin {
    @Inject(method = "fillFromNoise", at = @At("RETURN"), cancellable = true)
    private void kicraft$shapePaozuTerrain(Executor executor,
                                           Blender blender,
                                           RandomState randomState,
                                           StructureManager structureManager,
                                           ChunkAccess chunk,
                                           CallbackInfoReturnable<CompletableFuture<ChunkAccess>> callback) {
        NoiseBasedChunkGenerator generator = (NoiseBasedChunkGenerator) (Object) this;
        CompletableFuture<ChunkAccess> vanillaTerrain = callback.getReturnValue();

        callback.setReturnValue(vanillaTerrain.thenApply(generatedChunk -> {
            PaozuTerrainShaper.shape(
                    generatedChunk,
                    generator.getBiomeSource(),
                    randomState,
                    generator.generatorSettings().value()
            );
            return generatedChunk;
        }));
    }

    @Inject(method = "applyCarvers", at = @At("RETURN"))
    private void kicraft$paintPaozuSurface(WorldGenRegion region,
                                           long worldSeed,
                                           RandomState randomState,
                                           BiomeManager biomeManager,
                                           StructureManager structureManager,
                                           ChunkAccess chunk,
                                           GenerationStep.Carving carvingStep,
                                           CallbackInfo callback) {
        if (carvingStep != GenerationStep.Carving.AIR) {
            return;
        }

        NoiseBasedChunkGenerator generator = (NoiseBasedChunkGenerator) (Object) this;
        PaozuSurfacePainter.paint(
                region,
                chunk,
                generator.getBiomeSource(),
                randomState,
                generator.generatorSettings().value()
        );
    }
}
