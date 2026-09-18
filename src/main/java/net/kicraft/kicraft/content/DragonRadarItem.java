package net.kicraft.kicraft.content;

import net.kicraft.kicraft.server.KicraftAdvancements;
import net.kicraft.kicraft.server.PlayerDataEvents;
import net.kicraft.kicraft.server.capability.PlayerDataCapability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class DragonRadarItem extends Item {
    public DragonRadarItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) {
            return;
        }

        player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
            if (data.hasDragonRadar()) {
                return;
            }
            data.unlockDragonRadar();
            KicraftAdvancements.awardDragonRadar(player);
            PlayerDataEvents.sync(player);
            data.clearDirty();
        });
    }
}
