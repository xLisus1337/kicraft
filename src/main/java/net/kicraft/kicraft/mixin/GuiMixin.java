package net.kicraft.kicraft.mixin;

import net.kicraft.kicraft.client.KeyInputHandler;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = {"renderCrosshair", "m_280054_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void onRenderCrosshair(GuiGraphics guiGraphics, CallbackInfo ci) {
        // Blokujemy rysowanie celownika, gdy aktywna jest kamera zza ramienia (Shoulder Cam)
        if (KeyInputHandler.isShoulderCamActive) {
            ci.cancel();
        }
    }
}