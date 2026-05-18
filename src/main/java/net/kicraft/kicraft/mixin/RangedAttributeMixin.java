package net.kicraft.kicraft.mixin;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RangedAttribute.class)
public abstract class RangedAttributeMixin {

    @Shadow @Final @Mutable private double maxValue;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(String descriptionId, double defaultValue, double minValue, double maxValue, CallbackInfo ci) {
        if (descriptionId.equals("attribute.name.generic.max_health") ||
                descriptionId.equals("attribute.name.generic.attack_damage")) {

            // POPRAWKA: Float.MAX_VALUE to najwyższa matematycznie wartość,
            // jakiej Minecraft (używający float dla HP/DMG) może przetworzyć bez
            // wyrzucenia błędu błędu 'Infinity' i crashowania UI.
            this.maxValue = Float.MAX_VALUE;
        }
    }
}