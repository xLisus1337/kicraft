package net.kicraft.kicraft.network;

import net.kicraft.kicraft.client.ClientStatXpNotifier;
import net.kicraft.kicraft.core.player.KiStat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/** Sends compact per-stat XP gains to the local HUD. */
public record StatXpGainPacket(Map<KiStat, Double> gains) {
    public StatXpGainPacket(FriendlyByteBuf buffer) {
        this(readGains(buffer));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(gains.size());
        for (Map.Entry<KiStat, Double> entry : gains.entrySet()) {
            buffer.writeVarInt(entry.getKey().ordinal());
            buffer.writeDouble(entry.getValue());
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientStatXpNotifier.show(gains)));
        context.setPacketHandled(true);
        return true;
    }

    private static Map<KiStat, Double> readGains(FriendlyByteBuf buffer) {
        EnumMap<KiStat, Double> gains = new EnumMap<>(KiStat.class);
        int count = Math.min(KiStat.values().length, Math.max(0, buffer.readVarInt()));
        for (int index = 0; index < count; index++) {
            int ordinal = buffer.readVarInt();
            double amount = buffer.readDouble();
            if (ordinal >= 0 && ordinal < KiStat.values().length && amount > 0.0 && Double.isFinite(amount)) {
                gains.put(KiStat.values()[ordinal], amount);
            }
        }
        return gains;
    }
}
