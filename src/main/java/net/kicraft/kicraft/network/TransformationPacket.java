package net.kicraft.kicraft.network;

import net.kicraft.kicraft.core.player.KiTransformation;
import net.kicraft.kicraft.core.player.PlayerData;
import net.kicraft.kicraft.server.PlayerDataEvents;
import net.kicraft.kicraft.server.ResourceTickEvents;
import net.kicraft.kicraft.server.capability.PlayerDataCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Comparator;
import java.util.function.Supplier;

public record TransformationPacket(Action action, KiTransformation transformation) {
    public TransformationPacket(Action action) {
        this(action, KiTransformation.BASE);
    }

    public TransformationPacket(Action action, KiTransformation transformation) {
        this.action = action;
        this.transformation = transformation == null ? KiTransformation.BASE : transformation;
    }

    public TransformationPacket(FriendlyByteBuf buffer) {
        this(buffer.readEnum(Action.class), buffer.readEnum(KiTransformation.class));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(action);
        buffer.writeEnum(transformation);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
                if (action == Action.OFF) {
                    ResourceTickEvents.clearTransformationInstability(player);
                    data.activateTransformation(KiTransformation.BASE);
                    player.displayClientMessage(Component.translatable("commands.kicraft.transformation_active", Component.translatable(KiTransformation.BASE.translationKey())), true);
                    PlayerDataEvents.sync(player);
                    data.clearDirty();
                    return;
                }

                if (!player.isAlive() || player.isSpectator() || player.isSleeping() || !data.isRaceChosen()) {
                    return;
                }

                if (action == Action.SELECT) {
                    if (!activateManualTransformation(player, data, transformation)) {
                        player.displayClientMessage(Component.translatable("commands.kicraft.transformation_cannot_activate", Component.translatable(transformation.translationKey())), true);
                        return;
                    }
                    return;
                }

                if (data.activeTransformation() == KiTransformation.OOZARU) {
                    if (data.ki() > data.maxKi() * 0.20 && data.stamina() > data.maxStamina() * 0.10) {
                        ResourceTickEvents.clearTransformationInstability(player);
                        activatePreservingFullResources(data, KiTransformation.GOLDEN_OOZARU);
                        player.displayClientMessage(Component.translatable("commands.kicraft.transformation_active", Component.translatable(KiTransformation.GOLDEN_OOZARU.translationKey())), true);
                        PlayerDataEvents.sync(player);
                        data.clearDirty();
                    }
                    return;
                }

                if (data.activeTransformation() == KiTransformation.GOLDEN_OOZARU) {
                    if (data.hasTransformation(KiTransformation.SSJ3) && data.transformationMastery(KiTransformation.SSJ3) >= 80.0) {
                        data.unlockTransformation(KiTransformation.SSJ4);
                        ResourceTickEvents.clearTransformationInstability(player);
                        activatePreservingFullResources(data, KiTransformation.SSJ4);
                        player.displayClientMessage(Component.translatable("message.kicraft.ssj4_unlocked"), true);
                        PlayerDataEvents.sync(player);
                        data.clearDirty();
                    } else {
                        player.displayClientMessage(Component.translatable("message.kicraft.ssj4_locked"), true);
                    }
                    return;
                }

                KiTransformation next = data.transformations().stream()
                        .filter(transformation -> transformation.allowedFor(data))
                        .filter(KiTransformation::menuVisible)
                        .sorted(Comparator.comparingInt(KiTransformation::ordinal))
                        .filter(transformation -> transformation.ordinal() > data.activeTransformation().ordinal())
                        .findFirst()
                        .orElse(KiTransformation.BASE);

                if (next == KiTransformation.BASE || !activatePreservingFullResources(data, next)) {
                    player.displayClientMessage(Component.translatable("message.kicraft.no_higher_transformation"), true);
                    return;
                }
                ResourceTickEvents.clearTransformationInstability(player);
                player.displayClientMessage(Component.translatable("commands.kicraft.transformation_active", Component.translatable(next.translationKey())), true);
                PlayerDataEvents.sync(player);
                data.clearDirty();
            });
        });
        context.setPacketHandled(true);
        return true;
    }

    private static boolean activateManualTransformation(ServerPlayer player, PlayerData data, KiTransformation next) {
        if (next == KiTransformation.BASE || !next.menuVisible() || !data.hasTransformation(next) || !next.allowedFor(data)) {
            return false;
        }
        ResourceTickEvents.clearTransformationInstability(player);
        if (!activatePreservingFullResources(data, next)) {
            return false;
        }
        player.displayClientMessage(Component.translatable("commands.kicraft.transformation_active", Component.translatable(next.translationKey())), true);
        PlayerDataEvents.sync(player);
        data.clearDirty();
        return true;
    }

    private static boolean activatePreservingFullResources(PlayerData data, KiTransformation next) {
        boolean hpWasFull = data.maxHp() <= 0.0 || data.hp() >= data.maxHp() - 0.001;
        boolean kiWasFull = data.maxKi() <= 0.0 || data.ki() >= data.maxKi() - 0.001;
        boolean staminaWasFull = data.maxStamina() <= 0.0 || data.stamina() >= data.maxStamina() - 0.001;
        if (!data.activateTransformation(next)) {
            return false;
        }
        if (hpWasFull) {
            data.setHp(data.maxHp());
        }
        if (kiWasFull) {
            data.setKi(data.maxKi());
        }
        if (staminaWasFull) {
            data.setStamina(data.maxStamina());
        }
        return true;
    }

    public enum Action {
        NEXT,
        OFF,
        SELECT
    }
}
