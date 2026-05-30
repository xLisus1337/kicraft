package net.kicraft.kicraft.mixin;

import net.kicraft.kicraft.client.KeyInputHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Gui.class)
public abstract class GuiMixin {
    /**
     * Wymusza renderowanie celownika, gdy aktywna jest kamera nad ramieniem (Shoulder Cam).
     * Używamy aliasów nazw, aby trafić w odpowiednią metodę niezależnie od mapowań.
     */
    @Inject(method = {"shouldDrawCrosshair", "m_93033_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void showCrosshairInShoulderCam(CallbackInfoReturnable<Boolean> cir) {
        if (net.kicraft.kicraft.client.KeyInputHandler.isShoulderCamActive) {
            cir.setReturnValue(true);
        }
    }
}
