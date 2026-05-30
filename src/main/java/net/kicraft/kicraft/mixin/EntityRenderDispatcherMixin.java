package net.kicraft.kicraft.mixin;

import net.kicraft.kicraft.event.ClientEvents;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Shadow @Final private Map<EntityType<?>, EntityRenderer<?>> renderers;

    /**
     * Wymusza użycie naszego KicraftPlayerRenderer dla wszystkich graczy.
     * Minecraft domyślnie szuka renderera w mapie 'playerRenderers' (default/slim),
     * co omija rejestrację przez EntityType.PLAYER.
     */
    @Inject(method = "getRenderer", at = @At("HEAD"), cancellable = true)
    public <E extends Entity> void getKicraftPlayerRenderer(E entity, CallbackInfoReturnable<EntityRenderer<? super E>> cir) {
        if (entity instanceof Player player) {
            player.getCapability(net.kicraft.kicraft.capability.PlayerDataProvider.PLAYER_DATA).ifPresent(data -> {
                String race = data.getRace();
                if (race.equalsIgnoreCase("Human") || race.equalsIgnoreCase("Half-Saiyan")) {
                    // Używamy statycznej instancji z ClientEvents, jeśli istnieje
                    if (ClientEvents.kicraftRenderer != null) {
                        cir.setReturnValue((EntityRenderer<? super E>) ClientEvents.kicraftRenderer);
                    } else {
                        // Rezerwowo szukamy w mapie
                        EntityRenderer<? super E> renderer = (EntityRenderer<? super E>) this.renderers.get(EntityType.PLAYER);
                        if (renderer != null) {
                            cir.setReturnValue(renderer);
                        }
                    }
                }
            });
        }
    }
}
