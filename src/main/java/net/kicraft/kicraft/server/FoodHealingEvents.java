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
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) {
            return;
        }

        ItemStack eaten = event.getItem();
        FoodProperties food = eaten.getFoodProperties(player);
        if (food == null) {
            return;
        }

        player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
            double healRatio = healRatio(food);
            // Health may have changed since the last resource tick (damage or a potion).
            // Use the entity's current health so eating cannot restore a stale snapshot.
            float maxHealth = player.getMaxHealth();
            player.setHealth((float) Math.min(maxHealth, player.getHealth() + maxHealth * healRatio));
            data.setHp(data.maxHp() * player.getHealth() / Math.max(1.0F, maxHealth));
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
