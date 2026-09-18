package net.kicraft.kicraft.network;

import net.kicraft.kicraft.client.ClientKiSenseHighlights;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** A scan belongs to its viewer; it must never change server teams or glowing flags. */
public record KiSenseHighlightPacket(ResourceLocation dimension, int durationTicks, List<Entry> entries) {
    public static final int MAX_ENTRIES = 512;

    public KiSenseHighlightPacket {
        entries = List.copyOf(entries);
        if (entries.size() > MAX_ENTRIES || durationTicks < 0 || durationTicks > 200)
            throw new IllegalArgumentException("Invalid Ki Sense scan");
    }

    public KiSenseHighlightPacket(FriendlyByteBuf buffer) {
        this(buffer.readResourceLocation(), buffer.readVarInt(), readEntries(buffer));
    }

    private static List<Entry> readEntries(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_ENTRIES) throw new IllegalArgumentException("Invalid Ki Sense scan size");
        List<Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) entries.add(new Entry(buffer.readUUID(), buffer.readInt() & 0xFFFFFF));
        return entries;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(dimension);
        buffer.writeVarInt(durationTicks);
        buffer.writeVarInt(entries.size());
        for (Entry entry : entries) {
            buffer.writeUUID(entry.entityId());
            buffer.writeInt(entry.color());
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientKiSenseHighlights.receive(this)));
        context.setPacketHandled(true);
        return true;
    }

    public record Entry(UUID entityId, int color) { }
}
