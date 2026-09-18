package net.kicraft.kicraft.mixin;

import net.kicraft.kicraft.client.ClientKiSenseHighlights;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class KiSenseEntityMixin {
    @Inject(method = {"getTeamColor", "m_19876_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void kicraft$senseColor(CallbackInfoReturnable<Integer> result) {
        int color = ClientKiSenseHighlights.color((Entity) (Object) this);
        if (color >= 0) result.setReturnValue(color);
    }
}
