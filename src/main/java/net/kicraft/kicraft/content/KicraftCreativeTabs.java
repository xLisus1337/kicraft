package net.kicraft.kicraft.content;

import net.kicraft.kicraft.Kicraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class KicraftCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Kicraft.MODID);

    public static final RegistryObject<CreativeModeTab> KICRAFT = TABS.register("kicraft", () ->
        CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.kicraft"))
            .icon(() -> new ItemStack(Items.COMPASS))
            .displayItems((parameters, output) -> {
                output.accept(KicraftItems.DRAGON_RADAR.get());
                output.accept(KicraftItems.TRAINING_MAT.get());
            })
            .build()
    );

    private KicraftCreativeTabs() {
    }

    public static void register(IEventBus eventBus) {
        TABS.register(eventBus);
    }
}
