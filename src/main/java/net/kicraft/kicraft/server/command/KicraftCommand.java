package net.kicraft.kicraft.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.kicraft.kicraft.Kicraft;
import net.kicraft.kicraft.core.player.KiRace;
import net.kicraft.kicraft.core.player.KiStat;
import net.kicraft.kicraft.core.player.KiTechnique;
import net.kicraft.kicraft.core.player.KiTransformation;
import net.kicraft.kicraft.core.player.PlayerData;
import net.kicraft.kicraft.server.KicraftAdvancements;
import net.kicraft.kicraft.server.PlayerDataEvents;
import net.kicraft.kicraft.server.TechniqueSystem;
import net.kicraft.kicraft.server.capability.PlayerDataCapability;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.Locale;

@Mod.EventBusSubscriber(modid = Kicraft.MODID)
public final class KicraftCommand {
    private static final int DEFAULT_KI_COLOR = 0x45DFFF;

    private KicraftCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("kicraft")
                .then(Commands.literal("kicolor")
                        .then(Commands.argument("color", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(colorSuggestions(), builder))
                                .executes(context -> setKiColor(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "color")
                                ))))
                .then(Commands.literal("setstat")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("max")
                                .executes(context -> setStat(
                                        context.getSource(),
                                        "all",
                                        PlayerData.MAX_LEVEL
                                )))
                        .then(Commands.argument("stat", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(statSuggestions(), builder))
                                .then(Commands.literal("max")
                                        .executes(context -> setStat(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "stat"),
                                                PlayerData.MAX_LEVEL
                                        )))
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0))
                                        .executes(context -> setStat(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "stat"),
                                                DoubleArgumentType.getDouble(context, "value")
                                        )))))
                .then(Commands.literal("setrace")
                        .then(Commands.literal("info")
                                .executes(context -> raceInfoAll(context.getSource()))
                                .then(Commands.argument("race", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(raceSuggestions(), builder))
                                        .executes(context -> raceInfo(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "race")
                                        ))))
                        .executes(context -> reopenRaceSelection(context.getSource()))
                        .then(Commands.argument("race", StringArgumentType.greedyString())
                                .requires(source -> source.hasPermission(2))
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(raceSuggestions(), builder))
                                .executes(context -> setRace(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "race")
                                ))))
                .then(Commands.literal("technique")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> listTechniques(context.getSource()))
                        .then(Commands.literal("list")
                                .executes(context -> listTechniques(context.getSource())))
                        .then(Commands.literal("unlock")
                                .then(Commands.argument("technique", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(techniqueSuggestions(), builder))
                                        .executes(context -> unlockTechnique(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "technique")
                                        ))))
                        .then(Commands.literal("lock")
                                .then(Commands.argument("technique", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(techniqueOnlySuggestions(), builder))
                                        .executes(context -> lockTechnique(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "technique")
                                        ))))
                        .then(Commands.literal("clear")
                                .executes(context -> clearTechniques(context.getSource())))
                        .then(Commands.argument("technique", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(techniqueSuggestions(), builder))
                                .executes(context -> unlockTechnique(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "technique")
                                ))))
                .then(Commands.literal("transformation")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("list")
                                .executes(context -> listTransformations(context.getSource())))
                        .then(Commands.literal("unlock")
                                .then(Commands.argument("transformation", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(transformationSuggestions(context.getSource(), true, false), builder))
                                        .executes(context -> unlockTransformation(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "transformation")
                                        ))))
                        .then(Commands.literal("lock")
                                .then(Commands.argument("transformation", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(transformationSuggestions(context.getSource(), false, false), builder))
                                        .executes(context -> lockTransformation(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "transformation")
                                        ))))
                        .then(Commands.literal("mastery")
                                .then(Commands.argument("transformation", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(transformationSuggestions(context.getSource(), true, false), builder))
                                        .then(Commands.literal("max")
                                                .executes(context -> setTransformationMastery(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "transformation"),
                                                        100.0
                                                )))
                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 100.0))
                                                .executes(context -> setTransformationMastery(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "transformation"),
                                                        DoubleArgumentType.getDouble(context, "value")
                                                )))))));
    }

    private static int raceInfoAll(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("commands.kicraft.race_info.header"), false);
        for (KiRace race : KiRace.values()) {
            if (race != KiRace.NONE) {
                sendRaceInfo(source, race);
            }
        }
        return 1;
    }

    private static int raceInfo(CommandSourceStack source, String raceName) {
        KiRace race = isLegendarySaiyanName(raceName) ? KiRace.SAIYAN : KiRace.byName(raceName);
        if (race == KiRace.NONE) {
            source.sendFailure(Component.translatable("commands.kicraft.unknown_race", raceName));
            return 0;
        }

        sendRaceInfo(source, race);
        return 1;
    }

    private static void sendRaceInfo(CommandSourceStack source, KiRace race) {
        String key = raceInfoKey(race);
        source.sendSuccess(() -> Component.translatable("commands.kicraft.race_info.title", Component.translatable(race.translationKey())), false);
        source.sendSuccess(() -> Component.translatable("commands.kicraft.race_info.style", Component.translatable(key + ".style")), false);
        source.sendSuccess(() -> Component.translatable("commands.kicraft.race_info.strengths", Component.translatable(key + ".strengths")), false);
        source.sendSuccess(() -> Component.translatable("commands.kicraft.race_info.weaknesses", Component.translatable(key + ".weaknesses")), false);
        source.sendSuccess(() -> Component.translatable("commands.kicraft.race_info.progression", Component.translatable(key + ".progression")), false);
        source.sendSuccess(() -> Component.translatable("commands.kicraft.race_info.unique", Component.translatable(key + ".unique")), false);
    }

    private static int setStat(CommandSourceStack source, String statName, double value) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
            String key = normalize(statName);
            if ("all".equals(key)) {
                data.setAllStats(value);
                source.sendSuccess(() -> Component.translatable("commands.kicraft.set_stat_all", format(PlayerData.capStatLevel(value))), true);
            } else if ("godki".equals(key)) {
                if (!data.setGodKi(value)) {
                    source.sendFailure(Component.translatable("commands.kicraft.god_ki_locked_lssj"));
                    return;
                }
                KicraftAdvancements.awardGodKi(player);
                source.sendSuccess(() -> Component.translatable("commands.kicraft.set_god_ki", format(data.godKi())), true);
            } else {
                KiStat stat = statByName(key);
                if (stat == null) {
                    source.sendFailure(Component.translatable("commands.kicraft.unknown_stat", statName));
                    return;
                }
                data.setStat(stat, value);
                source.sendSuccess(() -> Component.translatable("commands.kicraft.set_stat", Component.translatable(stat.translationKey()), format(data.stat(stat))), true);
            }
            refillResources(player);
            PlayerDataEvents.sync(player);
            data.clearDirty();
        });
        return 1;
    }

    private static int setKiColor(CommandSourceStack source, String colorName) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Integer color = parseColor(colorName);
        if (color == null) {
            source.sendFailure(Component.translatable("commands.kicraft.unknown_ki_color", colorName));
            return 0;
        }

        player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
            data.setKiColor(color);
            PlayerDataEvents.sync(player);
            data.clearDirty();
            source.sendSuccess(() -> Component.translatable("commands.kicraft.set_ki_color", formatColor(color)), false);
        });
        return 1;
    }

    private static int setRace(CommandSourceStack source, String raceName) throws CommandSyntaxException {
        if (!source.hasPermission(2)) {
            source.sendFailure(Component.translatable("commands.generic.permission"));
            return 0;
        }

        ServerPlayer player = source.getPlayerOrException();
        boolean legendary = isLegendarySaiyanName(raceName);
        KiRace race = legendary ? KiRace.SAIYAN : KiRace.byName(raceName);
        if (race == KiRace.NONE) {
            source.sendFailure(Component.translatable("commands.kicraft.unknown_race", raceName));
            return 0;
        }

        player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
            data.setRace(race);
            data.setLegendarySaiyan(legendary);
            data.initializeRaceStats(player.getRandom());
            if (legendary) {
                KicraftAdvancements.awardBerserk(player);
            }
            refillResources(player);
            PlayerDataEvents.sync(player);
            data.clearDirty();
            Component raceDisplay = legendary ? Component.literal("Legendary Super Saiyan") : Component.translatable(race.translationKey());
            source.sendSuccess(() -> Component.translatable("commands.kicraft.set_race", raceDisplay), true);
        });
        return 1;
    }

    private static int reopenRaceSelection(CommandSourceStack source) throws CommandSyntaxException {
        if (!source.hasPermission(2)) {
            source.sendFailure(Component.translatable("commands.generic.permission"));
            return 0;
        }

        ServerPlayer player = source.getPlayerOrException();
        player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
            data.clearRaceChoice();
            PlayerDataEvents.sync(player);
            data.clearDirty();
            source.sendSuccess(() -> Component.translatable("commands.kicraft.race_selection_reopened"), true);
        });
        return 1;
    }

    private static int unlockTechnique(CommandSourceStack source, String techniqueName) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if ("all".equals(normalize(techniqueName))) {
            player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
                TechniqueSystem.unlockAll(player, data);
                source.sendSuccess(() -> Component.translatable("commands.kicraft.techniques_unlocked_all"), true);
            });
            return 1;
        }

        KiTechnique technique = KiTechnique.byName(techniqueName);
        if (technique == null) {
            source.sendFailure(Component.translatable("commands.kicraft.unknown_technique", techniqueName));
            return 0;
        }

        player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
            TechniqueSystem.unlock(player, data, technique);
            source.sendSuccess(() -> Component.translatable("commands.kicraft.technique_unlocked", TechniqueSystem.name(technique)), true);
        });
        return 1;
    }

    private static int lockTechnique(CommandSourceStack source, String techniqueName) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        KiTechnique technique = KiTechnique.byName(techniqueName);
        if (technique == null) {
            source.sendFailure(Component.translatable("commands.kicraft.unknown_technique", techniqueName));
            return 0;
        }

        player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
            TechniqueSystem.lock(player, data, technique);
            source.sendSuccess(() -> Component.translatable("commands.kicraft.technique_locked_command", TechniqueSystem.name(technique)), true);
        });
        return 1;
    }

    private static int clearTechniques(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
            TechniqueSystem.clear(player, data);
            source.sendSuccess(() -> Component.translatable("commands.kicraft.techniques_cleared"), true);
        });
        return 1;
    }

    private static int listTechniques(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
            source.sendSuccess(() -> Component.translatable("commands.kicraft.technique_list", TechniqueSystem.list(data.techniques())), false);
        });
        return 1;
    }

    private static int unlockTransformation(CommandSourceStack source, String transformationName) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
            if ("all".equals(normalize(transformationName))) {
                data.unlockAllRaceTransformations();
                source.sendSuccess(() -> Component.translatable("commands.kicraft.transformations_unlocked_all"), true);
                PlayerDataEvents.sync(player);
                data.clearDirty();
                return;
            }

            KiTransformation transformation = KiTransformation.byName(transformationName);
            if (transformation == null || transformation == KiTransformation.BASE || !transformation.menuVisible()) {
                source.sendFailure(Component.translatable("commands.kicraft.unknown_transformation", transformationName));
                return;
            }
            if (!transformation.belongsToRace(data.race(), data.isLegendarySaiyan())) {
                source.sendFailure(Component.translatable("commands.kicraft.transformation_wrong_race", name(transformation)));
                return;
            }
            if (!data.unlockTransformation(transformation)) {
                source.sendFailure(Component.translatable("commands.kicraft.transformation_cannot_activate", name(transformation)));
                return;
            }
            source.sendSuccess(() -> Component.translatable("commands.kicraft.transformation_unlocked", name(transformation)), true);
            PlayerDataEvents.sync(player);
            data.clearDirty();
        });
        return 1;
    }

    private static int lockTransformation(CommandSourceStack source, String transformationName) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        KiTransformation transformation = KiTransformation.byName(transformationName);
        if (transformation == null || transformation == KiTransformation.BASE || !transformation.menuVisible()) {
            source.sendFailure(Component.translatable("commands.kicraft.unknown_transformation", transformationName));
            return 0;
        }

        player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
            data.lockTransformation(transformation);
            source.sendSuccess(() -> Component.translatable("commands.kicraft.transformation_locked", name(transformation)), true);
            PlayerDataEvents.sync(player);
            data.clearDirty();
        });
        return 1;
    }

    private static int setTransformationMastery(CommandSourceStack source, String transformationName, double value) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if ("all".equals(normalize(transformationName))) {
            player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
                for (KiTransformation transformation : KiTransformation.values()) {
                    if (transformation != KiTransformation.BASE
                            && transformation.menuVisible()
                            && transformation.belongsToRace(data.race(), data.isLegendarySaiyan())) {
                        data.unlockTransformation(transformation);
                        data.setTransformationMastery(transformation, value);
                    }
                }
                source.sendSuccess(() -> Component.translatable(
                        "commands.kicraft.transformation_mastery_set",
                        Component.literal("all race transformations"),
                        Math.round(Math.max(0.0, Math.min(100.0, value)))
                ), true);
                PlayerDataEvents.sync(player);
                data.clearDirty();
            });
            return 1;
        }

        KiTransformation transformation = KiTransformation.byName(transformationName);
        if (transformation == null || transformation == KiTransformation.BASE || !transformation.menuVisible()) {
            source.sendFailure(Component.translatable("commands.kicraft.unknown_transformation", transformationName));
            return 0;
        }

        player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
            if (!transformation.belongsToRace(data.race(), data.isLegendarySaiyan())) {
                source.sendFailure(Component.translatable("commands.kicraft.transformation_wrong_race", name(transformation)));
                return;
            }
            data.unlockTransformation(transformation);
            data.setTransformationMastery(transformation, value);
            source.sendSuccess(() -> Component.translatable("commands.kicraft.transformation_mastery_set", name(transformation), Math.round(data.transformationMastery(transformation))), true);
            PlayerDataEvents.sync(player);
            data.clearDirty();
        });
        return 1;
    }

    private static int listTransformations(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
            Component list = data.transformations().isEmpty()
                    ? Component.translatable("common.kicraft.none")
                    : transformationList(data);
            source.sendSuccess(() -> Component.translatable("commands.kicraft.transformation_list", list, name(data.activeTransformation())), false);
        });
        return 1;
    }

    private static void refillResources(ServerPlayer player) {
        player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
            data.setHp(data.maxHp());
            data.setKi(data.maxKi());
            data.setStamina(data.maxStamina());
            AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
            double clampedMaxHealth = Math.min(1024.0, Math.max(1.0, data.maxHp()));
            if (maxHealth != null) {
                maxHealth.setBaseValue(clampedMaxHealth);
            }
            player.setHealth((float) clampedMaxHealth);
        });
    }

    private static KiStat statByName(String key) {
        for (KiStat stat : KiStat.values()) {
            if (normalize(stat.name()).equals(key) || normalize(stat.displayName()).equals(key)) {
                return stat;
            }
        }
        return null;
    }

    private static String[] statSuggestions() {
        String[] stats = Arrays.stream(KiStat.values())
                .map(stat -> stat.name().toLowerCase())
                .toArray(String[]::new);
        String[] suggestions = Arrays.copyOf(stats, stats.length + 2);
        suggestions[stats.length] = "all";
        suggestions[stats.length + 1] = "god_ki";
        return suggestions;
    }

    private static String[] raceSuggestions() {
        String[] races = Arrays.stream(KiRace.values())
                .filter(race -> race != KiRace.NONE)
                .map(race -> race.name().toLowerCase())
                .toArray(String[]::new);
        String[] suggestions = Arrays.copyOf(races, races.length + 1);
        suggestions[races.length] = "Legendary Super Saiyan";
        return suggestions;
    }

    private static String[] techniqueSuggestions() {
        String[] techniques = KiTechnique.suggestions();
        String[] suggestions = Arrays.copyOf(techniques, techniques.length + 1);
        suggestions[techniques.length] = "all";
        return suggestions;
    }

    private static String[] techniqueOnlySuggestions() {
        return KiTechnique.suggestions();
    }

    private static String[] transformationSuggestions(CommandSourceStack source, boolean includeAll, boolean includeBase) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            String[] transformations = KiTransformation.suggestions();
            int extra = (includeAll ? 1 : 0) + (includeBase ? 1 : 0);
            String[] suggestions = Arrays.copyOf(transformations, transformations.length + extra);
            int index = transformations.length;
            if (includeAll) {
                suggestions[index++] = "all";
            }
            if (includeBase) {
                suggestions[index] = "base";
            }
            return suggestions;
        }

        final java.util.List<String> suggestions = new java.util.ArrayList<>();
        player.getCapability(PlayerDataCapability.PLAYER_DATA).ifPresent(data -> {
            for (KiTransformation transformation : KiTransformation.values()) {
                if (transformation != KiTransformation.BASE
                        && transformation.menuVisible()
                        && transformation.belongsToRace(data.race(), data.isLegendarySaiyan())) {
                    suggestions.add(transformation.name().toLowerCase(Locale.ROOT));
                }
            }
        });
        if (includeAll) {
            suggestions.add("all");
        }
        if (includeBase) {
            suggestions.add("base");
        }
        return suggestions.toArray(String[]::new);
    }

    private static String[] colorSuggestions() {
        return new String[] {
                "cyan",
                "blue",
                "purple",
                "pink",
                "red",
                "orange",
                "gold",
                "green",
                "white",
                "reset",
                "45dfff"
        };
    }

    private static Integer parseColor(String value) {
        String key = normalize(value);
        return switch (key) {
            case "cyan" -> 0x45DFFF;
            case "blue" -> 0x3A8CFF;
            case "purple" -> 0xB56CFF;
            case "pink" -> 0xFF5BBA;
            case "red" -> 0xFF4056;
            case "orange" -> 0xFF8A30;
            case "gold", "yellow" -> 0xFFDD4A;
            case "green" -> 0x74FFB0;
            case "white" -> 0xF2F2F2;
            case "reset", "default" -> DEFAULT_KI_COLOR;
            default -> parseHexColor(value);
        };
    }

    private static Integer parseHexColor(String value) {
        if (value == null) {
            return null;
        }
        String hex = value.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        } else if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex = hex.substring(2);
        }
        if (hex.length() != 6) {
            return null;
        }
        try {
            return Integer.parseUnsignedInt(hex, 16) & 0xFFFFFF;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String formatColor(int color) {
        return String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF);
    }

    private static String raceInfoKey(KiRace race) {
        return "commands.kicraft.race_info." + race.name().toLowerCase(Locale.ROOT);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase().replace("_", "").replace("-", "").replace(" ", "");
    }

    private static Component name(KiTransformation transformation) {
        return Component.translatable(transformation.translationKey());
    }

    private static Component transformationList(PlayerData data) {
        MutableComponent result = Component.empty();
        boolean first = true;
        for (KiTransformation transformation : data.transformations()) {
            if (!first) {
                result.append(Component.literal(", "));
            }
            first = false;
            result.append(name(transformation)).append(Component.literal(" " + Math.round(data.transformationMastery(transformation)) + "%"));
        }
        return result;
    }

    private static boolean isLegendarySaiyanName(String value) {
        String key = normalize(value);
        return "lssj".equals(key)
                || "legendarysaiyan".equals(key)
                || "legendarysupersaiyan".equals(key);
    }

    private static String format(double value) {
        if (value >= PlayerData.MAX_LEVEL) {
            return "MAXLVL";
        }
        if (Math.abs(value) <= Long.MAX_VALUE && value == Math.rint(value)) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }
}
