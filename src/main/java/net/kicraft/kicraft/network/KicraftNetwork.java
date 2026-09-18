package net.kicraft.kicraft.network;

import net.kicraft.kicraft.Kicraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class KicraftNetwork {
    private static final String PROTOCOL = "16";
    private static int packetId;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Kicraft.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private KicraftNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(PlayerDataSyncPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PlayerDataSyncPacket::new)
                .encoder(PlayerDataSyncPacket::encode)
                .consumerMainThread(PlayerDataSyncPacket::handle)
                .add();

        CHANNEL.messageBuilder(StatLevelUpPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(StatLevelUpPacket::new)
                .encoder(StatLevelUpPacket::encode)
                .consumerMainThread(StatLevelUpPacket::handle)
                .add();

        CHANNEL.messageBuilder(StatXpGainPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(StatXpGainPacket::new)
                .encoder(StatXpGainPacket::encode)
                .consumerMainThread(StatXpGainPacket::handle)
                .add();

        CHANNEL.messageBuilder(FlightStateSyncPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(FlightStateSyncPacket::new)
                .encoder(FlightStateSyncPacket::encode)
                .consumerMainThread(FlightStateSyncPacket::handle)
                .add();

        CHANNEL.messageBuilder(FlightOrientationSyncPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(FlightOrientationSyncPacket::new)
                .encoder(FlightOrientationSyncPacket::encode)
                .consumerMainThread(FlightOrientationSyncPacket::handle)
                .add();

        // Kept registered for safe cleanup of legacy in-progress maneuver state.
        CHANNEL.messageBuilder(FlightManeuverSyncPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(FlightManeuverSyncPacket::new)
                .encoder(FlightManeuverSyncPacket::encode)
                .consumerMainThread(FlightManeuverSyncPacket::handle)
                .add();

        CHANNEL.messageBuilder(KiChargePacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(KiChargePacket::new)
                .encoder(KiChargePacket::encode)
                .consumerMainThread(KiChargePacket::handle)
                .add();

        CHANNEL.messageBuilder(SelectRacePacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SelectRacePacket::new)
                .encoder(SelectRacePacket::encode)
                .consumerMainThread(SelectRacePacket::handle)
                .add();

        CHANNEL.messageBuilder(DashPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(DashPacket::new)
                .encoder(DashPacket::encode)
                .consumerMainThread(DashPacket::handle)
                .add();

        CHANNEL.messageBuilder(BlockPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(BlockPacket::new)
                .encoder(BlockPacket::encode)
                .consumerMainThread(BlockPacket::handle)
                .add();

        CHANNEL.messageBuilder(KiBlastPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(KiBlastPacket::new)
                .encoder(KiBlastPacket::encode)
                .consumerMainThread(KiBlastPacket::handle)
                .add();

        CHANNEL.messageBuilder(ToggleFlightPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ToggleFlightPacket::new)
                .encoder(ToggleFlightPacket::encode)
                .consumerMainThread(ToggleFlightPacket::handle)
                .add();

        CHANNEL.messageBuilder(FlightControlPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(FlightControlPacket::new)
                .encoder(FlightControlPacket::encode)
                .consumerMainThread(FlightControlPacket::handle)
                .add();

        CHANNEL.messageBuilder(KiSensePacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(KiSensePacket::new)
                .encoder(KiSensePacket::encode)
                .consumerMainThread(KiSensePacket::handle)
                .add();

        CHANNEL.messageBuilder(KiTransferPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(KiTransferPacket::new)
                .encoder(KiTransferPacket::encode)
                .consumerMainThread(KiTransferPacket::handle)
                .add();

        CHANNEL.messageBuilder(LockOnPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(LockOnPacket::new)
                .encoder(LockOnPacket::encode)
                .consumerMainThread(LockOnPacket::handle)
                .add();

        CHANNEL.messageBuilder(LockOnSyncPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(LockOnSyncPacket::new)
                .encoder(LockOnSyncPacket::encode)
                .consumerMainThread(LockOnSyncPacket::handle)
                .add();

        CHANNEL.messageBuilder(TransformationPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(TransformationPacket::new)
                .encoder(TransformationPacket::encode)
                .consumerMainThread(TransformationPacket::handle)
                .add();

        CHANNEL.messageBuilder(TrainingActionPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(TrainingActionPacket::new)
                .encoder(TrainingActionPacket::encode)
                .consumerMainThread(TrainingActionPacket::handle)
                .add();

        CHANNEL.messageBuilder(BugReportPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(BugReportPacket::new)
                .encoder(BugReportPacket::encode)
                .consumerMainThread(BugReportPacket::handle)
                .add();

        CHANNEL.messageBuilder(MeleeInputPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(MeleeInputPacket::new).encoder(MeleeInputPacket::encode)
                .consumerMainThread(MeleeInputPacket::handle).add();
        CHANNEL.messageBuilder(MeleeStatePacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(MeleeStatePacket::new).encoder(MeleeStatePacket::encode)
                .consumerMainThread(MeleeStatePacket::handle).add();
        CHANNEL.messageBuilder(KiSenseHighlightPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(KiSenseHighlightPacket::new).encoder(KiSenseHighlightPacket::encode)
                .consumerMainThread(KiSenseHighlightPacket::handle).add();
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToTrackingAndSelf(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    private static int nextId() {
        return packetId++;
    }
}
