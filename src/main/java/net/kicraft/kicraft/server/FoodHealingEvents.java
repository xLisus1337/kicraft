package net.kicraft.kicraft.server;

import net.kicraft.kicraft.Kicraft;
import net.kicraft.kicraft.server.capability.PlayerDataCapability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Kicraft.MODID)
public final class FoodHealingEvents {
    private static final double MIN_FOOD_HEAL_RATIO = 0.15;
    private static final double MAX_FOOD_HEAL_RATIO = 0.35;

    private FoodHealingEvents() {
    }

    @SubscribeEvent
    public static void healFromFood(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack eaten = event.getItem();
        FoodProperties food = eaten.getFoodProperties(player);
        if (food == null) {
            return;
        }

        player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
            double healRatio = healRatio(food);
            double newDataHp = Math.min(data.maxHp(), data.hp() + data.maxHp() * healRatio);
            data.setHp(newDataHp);

            float vanillaTarget = (float) (player.getMaxHealth() * (data.hp() / Math.max(1.0, data.maxHp())));
            if (vanillaTarget > player.getHealth()) {
                player.setHealth(Math.min(player.getMaxHealth(), vanillaTarget));
            }
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(0.0F);

            PlayerDataEvents.sync(player);
            data.clearDirty();
        });
    }

    private static double healRatio(FoodProperties food) {
        double ratio = MIN_FOOD_HEAL_RATIO
                + food.getNutrition() * 0.015
                + food.getSaturationModifier() * food.getNutrition() * 0.005;
        return Math.max(MIN_FOOD_HEAL_RATIO, Math.min(MAX_FOOD_HEAL_RATIO, ratio));
    }
}
