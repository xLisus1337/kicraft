package net.kicraft.kicraft.client;

/** Selected Ki-attack slot, independent of the player's item inventory. */
final class TechniqueHotbarState {
    static final int SLOT_COUNT = 4;

    private int selectedSlot;

    int selectedSlot() {
        return selectedSlot;
    }

    void select(int slot) {
        if (slot >= 0 && slot < SLOT_COUNT) selectedSlot = slot;
    }

    void scroll(double delta) {
        if (delta != 0.0) {
            select(Math.floorMod(selectedSlot - (delta > 0.0 ? 1 : -1), SLOT_COUNT));
        }
    }

    void reset() {
        selectedSlot = 0;
    }
}
