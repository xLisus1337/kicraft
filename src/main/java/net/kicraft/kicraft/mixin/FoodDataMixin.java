package net.kicraft.kicraft.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FoodData.class)
public abstract class FoodDataMixin {
    /** Ki Craft replaces hunger regeneration, not all LivingEntity.heal calls. */
    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;heal(F)V"), require = 2)
    private void kicraft$disableHungerHealing(Player player, float amount) {
    }
}
