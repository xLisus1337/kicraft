package net.kicraft.kicraft.network;

import net.kicraft.kicraft.server.DiscordBugReportService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Sends a player-written report to the server without exposing its webhook. */
public record BugReportPacket(String report) {
    public static final int MAX_REPORT_LENGTH = 900;

    public BugReportPacket(FriendlyByteBuf buffer) {
        this(buffer.readUtf(MAX_REPORT_LENGTH));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(report == null ? "" : report, MAX_REPORT_LENGTH);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.isAlive() && !player.isSpectator()) {
                DiscordBugReportService.submit(player, report);
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
