package net.kicraft.kicraft.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerFoodMixin {
    /** Food restores Ki Craft health even though the hunger bar stays full. */
    @Inject(method = "canEat", at = @At("RETURN"), cancellable = true)
    private void kicraft$allowFoodWhenHurt(boolean alwaysEdible, CallbackInfoReturnable<Boolean> result) {
        Player player = (Player) (Object) this;
        if (!result.getReturnValue() && !player.isSpectator() && player.isHurt()) {
            result.setReturnValue(true);
        }
    }

    /** Suppress only passive peaceful healing, leaving potions and other healing intact. */
    @Redirect(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;heal(F)V"))
    private void kicraft$disablePeacefulHealing(Player player, float amount) {
    }
}
