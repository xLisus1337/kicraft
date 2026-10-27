package net.kicraft.kicraft.network;

import net.kicraft.kicraft.client.ClientOozaruFocus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record OozaruFocusPacket(int playerId, int remainingTicks, float yaw, float pitch) {
    public OozaruFocusPacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readVarInt(), buffer.readFloat(), buffer.readFloat());
    }
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(playerId);
        buffer.writeVarInt(remainingTicks);
        buffer.writeFloat(yaw);
        buffer.writeFloat(pitch);
    }
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientOozaruFocus.receive(this)));
        context.setPacketHandled(true);
        return true;
    }
}
