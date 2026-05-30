package net.kicraft.kicraft.mixin;

import net.kicraft.kicraft.client.KeyInputHandler;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void move(float distanceOffset, float verticalOffset, float horizontalOffset);

    @Inject(method = "setup", at = @At("RETURN"))
    private void adjustCameraForShoulder(BlockGetter level, Entity entity, boolean detached, boolean mirrored, float partialTick, CallbackInfo ci) {
        if (KeyInputHandler.isShoulderCamActive && detached && !mirrored) {
            // Przesunięcie kamery w prawo (horizontal) i lekko w górę (vertical)
            // Minecraft Camera move: distance (do tyłu), vertical (góra), horizontal (prawo)
            this.move(0.0F, 0.2F, 0.8F);
        }
    }
}
