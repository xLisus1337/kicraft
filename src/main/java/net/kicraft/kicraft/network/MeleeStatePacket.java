package net.kicraft.kicraft.network;

import net.kicraft.kicraft.client.MeleeInput;
import net.kicraft.kicraft.core.combat.CombatFeedback;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record MeleeStatePacket(int recovery, int exposed, int counter, CombatFeedback feedback) {
    public MeleeStatePacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readEnum(CombatFeedback.class));
    }
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(recovery);
        buffer.writeVarInt(exposed);
        buffer.writeVarInt(counter);
        buffer.writeEnum(feedback);
    }
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MeleeInput.receive(this)));
        context.setPacketHandled(true);
        return true;
    }
}
