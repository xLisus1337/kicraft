package net.kicraft.kicraft.mixin;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.function.mask.Mask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Makes WorldEdit 7.2.15 heightmap brushes work below Y=0.
 *
 * <p>That WorldEdit version scans terrain only from Y=0 to Y=255. Modern
 * Minecraft worlds extend down to Y=-64, so a heightmap brush used on a low
 * superflat world finds air and appears to do nothing.</p>
 */
@Pseudo
@Mixin(targets = "com.sk89q.worldedit.command.tool.brush.ImageHeightmapBrush", remap = false)
public abstract class WorldEditImageHeightmapBrushMixin {
    @Redirect(
            method = "build",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/sk89q/worldedit/EditSession;getHighestTerrainBlock(IIIILcom/sk89q/worldedit/function/mask/Mask;)I"
            ),
            require = 0,
            remap = false
    )
    private int kicraft$scanTheWholeWorldHeight(
            EditSession session,
            int x,
            int z,
            int ignoredMinimumY,
            int ignoredMaximumY,
            Mask mask
    ) {
        return session.getHighestTerrainBlock(
                x,
                z,
                session.getWorld().getMinY(),
                session.getWorld().getMaxY(),
                mask
        );
    }
}
