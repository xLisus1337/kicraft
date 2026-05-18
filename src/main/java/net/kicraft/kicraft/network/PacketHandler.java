package net.kicraft.kicraft.network;

import net.kicraft.kicraft.Kicraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;
    private static int id() { return packetId++; }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(Kicraft.MODID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        net.messageBuilder(RaceSyncC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(RaceSyncC2SPacket::new)
                .encoder(RaceSyncC2SPacket::toBytes)
                .consumerMainThread(RaceSyncC2SPacket::handle)
                .add();

        net.messageBuilder(PlayerDataSyncS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PlayerDataSyncS2CPacket::new)
                .encoder(PlayerDataSyncS2CPacket::toBytes)
                .consumerMainThread(PlayerDataSyncS2CPacket::handle)
                .add();

        net.messageBuilder(OpenRaceMenuS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(OpenRaceMenuS2CPacket::new)
                .encoder(OpenRaceMenuS2CPacket::toBytes)
                .consumerMainThread(OpenRaceMenuS2CPacket::handle)
                .add();

        net.messageBuilder(ChargeKiC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ChargeKiC2SPacket::new)
                .encoder(ChargeKiC2SPacket::toBytes)
                .consumerMainThread(ChargeKiC2SPacket::handle)
                .add();

        net.messageBuilder(TurboModeC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(TurboModeC2SPacket::new)
                .encoder(TurboModeC2SPacket::toBytes)
                .consumerMainThread(TurboModeC2SPacket::handle)
                .add();

        net.messageBuilder(InstantTransmissionC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(InstantTransmissionC2SPacket::new)
                .encoder(InstantTransmissionC2SPacket::toBytes)
                .consumerMainThread(InstantTransmissionC2SPacket::handle)
                .add();

        // --- REJESTRACJA PAKIETU DO RADARU (KI SENSE) ---
        net.messageBuilder(KiSenseUpdateC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(KiSenseUpdateC2SPacket::new)
                .encoder(KiSenseUpdateC2SPacket::toBytes)
                .consumerMainThread(KiSenseUpdateC2SPacket::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}