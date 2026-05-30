package net.kicraft.kicraft.mixin;

import net.kicraft.kicraft.client.KeyInputHandler;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    /**
     * Blokuje możliwość ręcznego obracania kamerą myszką, gdy aktywny jest system namierzania (Lock-On).
     * Używamy jawnego deskryptora (DD)V, aby uniknąć błędu 'Invalid descriptor'.
     */
    @Inject(method = {"turnPlayer", "m_91522_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void cancelMouseTurn(double x, double y, CallbackInfo ci) {
        if (net.kicraft.kicraft.client.KeyInputHandler.lockOnTarget != null) {
            ci.cancel();
        }
    }
}
