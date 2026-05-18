package net.kicraft.kicraft.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {

    // Uderzamy w metodę rysującą rękę z pierwszej osoby
    @Inject(method = "renderPlayerArm", at = @At("HEAD"), cancellable = true)
    private void cancelVanillaArm(PoseStack poseStack, MultiBufferSource buffer, int combinedLight, float equippedProgress, float swingProgress, HumanoidArm side, CallbackInfo ci) {

        // Jeśli renderer gracza NIE JEST waniliowy (tylko nasz GeckoLibowy)
        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(Minecraft.getInstance().player) instanceof PlayerRenderer)) {
            // Anuluj rysowanie ręki, żeby uniknąć crasha ClassCastException!
            ci.cancel();
        }
    }
}