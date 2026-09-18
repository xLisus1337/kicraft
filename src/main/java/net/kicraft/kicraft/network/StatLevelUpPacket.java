package net.kicraft.kicraft.network;

import net.kicraft.kicraft.client.ClientStatLevelUpNotifier;
import net.kicraft.kicraft.core.player.KiStat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/** Server-authoritative notification for stat levels earned through training. */
public record StatLevelUpPacket(Map<KiStat, Double> levels) {
    public StatLevelUpPacket(FriendlyByteBuf buffer) {
        this(readLevels(buffer));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(levels.size());
        for (Map.Entry<KiStat, Double> entry : levels.entrySet()) {
            buffer.writeVarInt(entry.getKey().ordinal());
            buffer.writeDouble(entry.getValue());
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientStatLevelUpNotifier.enqueue(levels)));
        context.setPacketHandled(true);
        return true;
    }

    private static Map<KiStat, Double> readLevels(FriendlyByteBuf buffer) {
        EnumMap<KiStat, Double> levels = new EnumMap<>(KiStat.class);
        int amount = Math.min(KiStat.values().length, Math.max(0, buffer.readVarInt()));
        for (int index = 0; index < amount; index++) {
            int ordinal = buffer.readVarInt();
            double level = buffer.readDouble();
            if (ordinal >= 0 && ordinal < KiStat.values().length && Double.isFinite(level)) {
                levels.put(KiStat.values()[ordinal], level);
            }
        }
        return levels;
    }
}
