package net.kicraft.kicraft.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** One current dummy per owner, shared by every dimension and persisted with the world. */
public final class TrainingDummyRegistry extends SavedData {
    private static final String DATA_NAME = "kicraft_training_dummies";
    // A null value is a tombstone: old unloaded dummies must not return after manual removal.
    private final Map<UUID, UUID> currentDummies = new HashMap<>();

    public static TrainingDummyRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                TrainingDummyRegistry::load, TrainingDummyRegistry::new, DATA_NAME);
    }

    public UUID current(UUID owner) {
        return currentDummies.get(owner);
    }

    public void replace(UUID owner, UUID dummy) {
        currentDummies.put(owner, dummy);
        setDirty();
    }

    public void remove(UUID owner, UUID dummy) {
        if (dummy.equals(current(owner))) replace(owner, null);
    }

    public boolean acceptOrAdopt(UUID owner, UUID dummy) {
        if (!currentDummies.containsKey(owner)) {
            // Adopt the first legacy dummy; any duplicates loaded later are rejected.
            replace(owner, dummy);
        }
        return dummy.equals(current(owner));
    }

    public static TrainingDummyRegistry load(CompoundTag tag) {
        TrainingDummyRegistry registry = new TrainingDummyRegistry();
        ListTag entries = tag.getList("Owners", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            if (entry.hasUUID("Owner")) {
                registry.currentDummies.put(entry.getUUID("Owner"),
                        entry.hasUUID("Dummy") ? entry.getUUID("Dummy") : null);
            }
        }
        return registry;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag entries = new ListTag();
        currentDummies.forEach((owner, dummy) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Owner", owner);
            if (dummy != null) entry.putUUID("Dummy", dummy);
            entries.add(entry);
        });
        tag.put("Owners", entries);
        return tag;
    }
}
