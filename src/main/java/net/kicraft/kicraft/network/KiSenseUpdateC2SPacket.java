package net.kicraft.kicraft.network;

import net.kicraft.kicraft.KiSenseHandlerServer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static net.kicraft.kicraft.KiSenseHandlerServer.*;

public class KiSenseUpdateC2SPacket {
    private final Map<Integer, SenseCategory> entitiesToUpdate;

    // Kategoria siły
    public enum SenseCategory {
        WEAK(0), EQUAL(1), STRONG(2);
        public final int id;
        SenseCategory(int id) { this.id = id; }
        public static SenseCategory fromId(int id) {
            for (SenseCategory c : values()) { if (c.id == id) return c; }
            return EQUAL;
        }
    }

    public KiSenseUpdateC2SPacket(Map<Integer, SenseCategory> entitiesToUpdate) {
        this.entitiesToUpdate = entitiesToUpdate;
    }

    // Decoder
    public KiSenseUpdateC2SPacket(FriendlyByteBuf buf) {
        this.entitiesToUpdate = new HashMap<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            this.entitiesToUpdate.put(buf.readInt(), SenseCategory.fromId(buf.readByte()));
        }
    }

    // Encoder
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entitiesToUpdate.size());
        for (Map.Entry<Integer, SenseCategory> entry : entitiesToUpdate.entrySet()) {
            buf.writeInt(entry.getKey());
            buf.writeByte(entry.getValue().id);
        }
    }

    // Handler (Serwer)
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            // Logika obsługi na serwerze (napuściemy zaraz handler)
            handleUpdate(player, entitiesToUpdate);
        });
        return true;
    }
}