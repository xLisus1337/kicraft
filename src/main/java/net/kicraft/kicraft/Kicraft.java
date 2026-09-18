package net.kicraft.kicraft;

import com.mojang.logging.LogUtils;
import net.kicraft.kicraft.client.ClientSettings;
import net.kicraft.kicraft.content.KicraftBlocks;
import net.kicraft.kicraft.content.KicraftCreativeTabs;
import net.kicraft.kicraft.content.KicraftEntities;
import net.kicraft.kicraft.content.KicraftItems;
import net.kicraft.kicraft.network.KicraftNetwork;
import net.kicraft.kicraft.server.ServerSettings;
import net.kicraft.kicraft.worldgen.KicraftWorldgen;
import net.kicraft.kicraft.worldgen.PaozuFeatures;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Kicraft.MODID)
public final class Kicraft {
    public static final String MODID = "kicraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Kicraft() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientSettings.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerSettings.SPEC);
        KicraftBlocks.register(modEventBus);
        KicraftEntities.register(modEventBus);
        KicraftItems.register(modEventBus);
        KicraftCreativeTabs.register(modEventBus);
        PaozuFeatures.register(modEventBus);
        modEventBus.addListener(KicraftWorldgen::commonSetup);
        KicraftNetwork.register();
        LOGGER.info("Kicraft clean rebuild started.");
    }
}
