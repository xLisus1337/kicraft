package net.kicraft.kicraft.mixin;

import net.kicraft.kicraft.client.ClientKiSenseHighlights;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class KiSenseMinecraftMixin {
    // This project has no generated refmap. Cover both official development and Forge SRG names.
    @Inject(method = {"shouldEntityAppearGlowing", "m_91314_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void kicraft$senseOutline(Entity entity, CallbackInfoReturnable<Boolean> result) {
        if (ClientKiSenseHighlights.color(entity) >= 0) result.setReturnValue(true);
    }
}
