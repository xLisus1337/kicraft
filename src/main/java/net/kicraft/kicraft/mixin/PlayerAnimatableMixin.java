package net.kicraft.kicraft.mixin;

import net.kicraft.kicraft.capability.PlayerDataProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

@Mixin(Player.class)
public abstract class PlayerAnimatableMixin extends LivingEntity implements GeoEntity {

    @Unique
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected PlayerAnimatableMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Kontroler ruchu (nogi/ręce)
        controllers.add(new AnimationController<>(this, "movement", 5, event -> {
            if (event.isMoving()) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            }
            return PlayState.STOP;
        }));

        // Kontroler oddechu (idle gracza)
        controllers.add(new AnimationController<>(this, "breathing", 5, event -> {
            return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));

        // KONTROLER OGONA (Tylko dla Saiyan)
        controllers.add(new AnimationController<>(this, "tail_controller", 5, event -> {
            return this.getCapability(PlayerDataProvider.PLAYER_DATA).map(data -> {
                String race = data.getRace();
                if (race.equalsIgnoreCase("Saiyan") || race.equalsIgnoreCase("Half-Saiyan")) {
                    // Puszcza Twoją animację falowania zapisaną w JSON
                    return event.setAndContinue(RawAnimation.begin().thenLoop("animation.saiyan_tail.idle"));
                }
                return PlayState.STOP;
            }).orElse(PlayState.STOP);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}