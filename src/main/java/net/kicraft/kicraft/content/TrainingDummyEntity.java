package net.kicraft.kicraft.content;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** A stationary humanoid target used only by Ki Craft's training system. */
public final class TrainingDummyEntity extends Mob {
    public TrainingDummyEntity(EntityType<? extends TrainingDummyEntity> type, Level level) {
        super(type, level);
        setNoAi(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
        // A training target must remain exactly where its owner placed it.
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && getHealth() < getMaxHealth()) {
            // Hits remain visible, but this target can never be worn down during training.
            setHealth(getMaxHealth());
        }
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEFINED;
    }
}
