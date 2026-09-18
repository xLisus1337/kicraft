package net.kicraft.kicraft.mixin;

import net.kicraft.kicraft.client.ClientLockOnData;
import net.kicraft.kicraft.client.ClientFlightOrientation;
import net.kicraft.kicraft.client.ClientFlightStates;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Redirect(
            method = "turnPlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"),
            require = 0
    )
    private void kicraft$disableMouseLookDuringLockOn(LocalPlayer player, double yaw, double pitch) {
        if (ClientLockOnData.isLocked()) {
            return;
        }
        if (ClientFlightStates.isFlying(player)) {
            ClientFlightOrientation.turn(player, yaw, pitch);
        } else {
            player.turn(yaw, pitch);
        }
    }
}
