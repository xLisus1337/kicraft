package net.kicraft.kicraft.content;

import net.kicraft.kicraft.Kicraft;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class KicraftEntities {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Kicraft.MODID);

    public static final RegistryObject<EntityType<TrainingDummyEntity>> TRAINING_DUMMY = ENTITY_TYPES.register("training_dummy", () ->
            EntityType.Builder.of(TrainingDummyEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build("training_dummy")
    );

    private KicraftEntities() {
    }

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
        eventBus.addListener(KicraftEntities::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        AttributeSupplier attributes = TrainingDummyEntity.createAttributes().build();
        event.put(TRAINING_DUMMY.get(), attributes);
    }
}
