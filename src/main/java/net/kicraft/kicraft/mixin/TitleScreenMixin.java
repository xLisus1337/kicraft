package net.kicraft.kicraft.mixin;

import net.kicraft.kicraft.client.KicraftTitleBackground;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.PanoramaRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void kicraft$renderTitleBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
                                               CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        KicraftTitleBackground.render(graphics, minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight());
    }

    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/PanoramaRenderer;render(FF)V"), require = 0)
    private void kicraft$skipVanillaPanorama(PanoramaRenderer panorama, float partialTick, float fade) {
    }
}
