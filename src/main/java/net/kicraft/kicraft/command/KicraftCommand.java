package net.kicraft.kicraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kicraft.kicraft.capability.PlayerDataProvider;
import net.kicraft.kicraft.event.GameplayEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.network.chat.Component;

public class KicraftCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("kicraft")
                .requires(source -> source.hasPermission(2))

                .then(Commands.literal("setrace")
                        .then(Commands.argument("race", StringArgumentType.word())
                                .executes(KicraftCommand::setRace)))

                .then(Commands.literal("setstat")
                        .then(Commands.argument("target", StringArgumentType.word())
                                .then(Commands.argument("stat", StringArgumentType.word())
                                        .then(Commands.argument("value", LongArgumentType.longArg(0))
                                                .executes(KicraftCommand::execute)))))

                // --- NOWA KOMENDA NA TECHNIKI ---
                .then(Commands.literal("technique")
                        .then(Commands.argument("tech", StringArgumentType.word())
                                .executes(KicraftCommand::setTechnique))));
    }

    private static int setTechnique(CommandContext<CommandSourceStack> context) {
        String tech = StringArgumentType.getString(context, "tech");
        ServerPlayer player = context.getSource().getPlayer();

        if (player != null) {
            player.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(d -> {
                switch (tech.toLowerCase()) {
                    case "fly" -> d.setFlight(true);
                    case "kisense" -> d.setKiSense(true);
                    case "it" -> d.setInstantTransmission(true);
                    case "all" -> {
                        d.setFlight(true);
                        d.setKiSense(true);
                        d.setInstantTransmission(true);
                    }
                    default -> {
                        player.sendSystemMessage(Component.literal("§cUnknown technique! Use: fly, kiSense, it, all"));
                        return;
                    }
                }
                GameplayEvents.sync(player);
                player.sendSystemMessage(Component.literal("§aTechnique unlocked: §e" + tech));
            });
        }
        return 1;
    }

    private static int setRace(CommandContext<CommandSourceStack> context) {
        String race = StringArgumentType.getString(context, "race");
        ServerPlayer p = context.getSource().getPlayer();
        if (p != null) {
            p.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(d -> {
                d.setRace(race);
                GameplayEvents.sync(p);
            });
            p.sendSystemMessage(Component.literal("§aRace changed to: " + race));
        }
        return 1;
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        String stat = StringArgumentType.getString(context, "stat");
        long v = LongArgumentType.getLong(context, "value");
        ServerPlayer p = context.getSource().getPlayer();

        if (p != null) {
            p.getCapability(PlayerDataProvider.PLAYER_DATA).ifPresent(d -> {
                switch (stat.toLowerCase()) {
                    case "level" -> d.setLevel(v);
                    case "ki" -> d.setKi(v);
                    case "maxki" -> d.setMaxKi(v);
                    case "stamina" -> d.setStamina(v);
                    case "maxstamina" -> d.setMaxStamina(v);
                    case "strength" -> d.setStrength(v);
                    case "durability" -> d.setDurability(v);
                    case "maxhp" -> {
                        d.setMaxHP(v);
                        if (p.getAttribute(Attributes.MAX_HEALTH) != null) {
                            p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(v);
                            p.setHealth((float)v);
                        }
                    }
                    case "kicontrol" -> d.setKiControl(v);
                    case "all" -> {
                        d.setLevel(v); d.setMaxKi(v * 100); d.setKi(v * 100);
                        d.setMaxStamina(v * 100); d.setStamina(v * 100);
                        d.setStrength(v); d.setDurability(v); d.setKiControl(v);
                        d.setMaxHP(20 + (v * 2));
                        if (p.getAttribute(Attributes.MAX_HEALTH) != null) {
                            p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20 + (v * 2));
                            p.setHealth(p.getMaxHealth());
                        }
                    }
                }
                GameplayEvents.sync(p);
                p.sendSystemMessage(Component.literal("§eStat §b" + stat + " §eset to §a" + v));
            });
        }
        return 1;
    }
}