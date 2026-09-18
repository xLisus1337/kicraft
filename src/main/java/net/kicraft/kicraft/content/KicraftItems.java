package net.kicraft.kicraft.content;

import net.kicraft.kicraft.Kicraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class KicraftItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Kicraft.MODID);

    public static final RegistryObject<Item> DRAGON_RADAR = ITEMS.register("dragon_radar", DragonRadarItem::new);
    public static final RegistryObject<Item> TRAINING_MAT = ITEMS.register("training_mat", () ->
            new BlockItem(KicraftBlocks.TRAINING_MAT.get(), new Item.Properties())
    );

    private KicraftItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
