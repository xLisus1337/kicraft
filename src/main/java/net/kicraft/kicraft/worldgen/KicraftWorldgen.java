package net.kicraft.kicraft.worldgen;

import net.kicraft.kicraft.Kicraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import terrablender.api.Regions;
import terrablender.config.TerraBlenderConfig;
import terrablender.core.TerraBlender;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

public final class KicraftWorldgen {
    private static final int MINIMUM_OVERWORLD_REGION_SIZE = 4;
    private static final Pattern OVERWORLD_REGION_SIZE = Pattern.compile(
            "(?m)^(\\s*overworld_region_size\\s*=\\s*)(\\d+)(\\s*(?:#.*)?)$");
    private static final ResourceLocation PAOZU_REGION =
            new ResourceLocation(Kicraft.MODID, "paozu_mountain_region");

    private KicraftWorldgen() {
    }

    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ensureLargeOverworldRegions();
            Regions.register(new PaozuOverworldRegion(PAOZU_REGION, 2));
        });
    }

    /**
     * Uses TerraBlender's 1024-block-scale region layout for Paozu.  A size of four
     * leaves at least a 500-block span even after TerraBlender softens region edges.
     */
    private static void ensureLargeOverworldRegions() {
        Path configPath = FMLPaths.CONFIGDIR.get().resolve("terrablender.toml");

        try {
            String config = Files.readString(configPath, StandardCharsets.UTF_8);
            String updatedConfig = OVERWORLD_REGION_SIZE.matcher(config).replaceFirst(match -> {
                int configuredSize = Integer.parseInt(match.group(2));
                return match.group(1) + Math.max(configuredSize, MINIMUM_OVERWORLD_REGION_SIZE)
                        + match.group(3);
            });

            if (!updatedConfig.equals(config)) {
                Files.writeString(configPath, updatedConfig, StandardCharsets.UTF_8);
            }

            TerraBlender.setConfig(new TerraBlenderConfig(configPath));
        } catch (IOException exception) {
            Kicraft.LOGGER.warn("Could not set TerraBlender's overworld region size for Paozu", exception);
        }
    }
}
