package net.kicraft.kicraft.network;

import net.kicraft.kicraft.server.MeleeCombat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record MeleeInputPacket(AttackSide side) {
    public enum AttackSide {
        LEFT,
        RIGHT;

        public InteractionHand interactionHand(HumanoidArm mainArm) {
            boolean mainMatches = this == RIGHT && mainArm == HumanoidArm.RIGHT
                    || this == LEFT && mainArm == HumanoidArm.LEFT;
            return mainMatches ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        }
    }

    public MeleeInputPacket(FriendlyByteBuf buffer) {
        this(buffer.readEnum(AttackSide.class));
    }
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(side);
    }
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) apply(context.getSender());
        });
        context.setPacketHandled(true);
        return true;
    }

    public void apply(net.minecraft.server.level.ServerPlayer player) {
        if (net.kicraft.kicraft.core.combat.CombatRules.CUSTOM_MELEE_ENABLED) MeleeCombat.input(player, side);
    }
}
