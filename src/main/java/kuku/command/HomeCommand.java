package kuku.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import kuku.back.BackManager;
import kuku.config.HomeConfig;
import kuku.data.HomeData;
import kuku.home.HomeManager;
import kuku.lang.LanguageManager;
import kuku.util.DimensionUtil;
import kuku.util.MessageDisplayManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

public class HomeCommand {

    private static final SuggestionProvider<CommandSourceStack> HOME_SUGGESTIONS =
            (context, builder) -> {
                try {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    for (HomeData h : HomeManager.getHomes(player.getUUID())) {
                        builder.suggest(h.getName());
                    }
                } catch (CommandSyntaxException ignored) {
                }
                return builder.buildFuture();
            };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sethome")
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(HomeCommand::setHome))
        );

        String defaultName = HomeConfig.getInstance().getDefaultHomeName();
        dispatcher.register(Commands.literal("home")
                .executes(ctx -> teleportHome(ctx, HomeConfig.getInstance().getDefaultHomeName()))
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .suggests(HOME_SUGGESTIONS)
                        .executes(ctx -> teleportHome(ctx, StringArgumentType.getString(ctx, "name"))))
        );

        dispatcher.register(Commands.literal("homes")
                .executes(HomeCommand::listHomes)
        );

        dispatcher.register(Commands.literal("delhome")
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .suggests(HOME_SUGGESTIONS)
                        .executes(HomeCommand::deleteHome))
        );
    }

    private static boolean checkEnabled(CommandSourceStack source, ServerPlayer player) {
        if (!HomeConfig.getInstance().isEnabled()) {
            MessageDisplayManager.sendSystemMessage(player,
                    LanguageManager.prefixed("Home", "home.error.disabled", player)
                            .withStyle(ChatFormatting.RED));
            return false;
        }
        return true;
    }

    private static int setHome(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (!checkEnabled(ctx.getSource(), player)) return 0;

        String name = StringArgumentType.getString(ctx, "name");
        List<HomeData> list = HomeManager.getHomes(player.getUUID());
        HomeConfig config = HomeConfig.getInstance();

        if (list.size() >= config.getMaxHomes()) {
            MessageDisplayManager.sendSystemMessage(player,
                    LanguageManager.prefixed("Home", "home.error.max_homes", player, config.getMaxHomes())
                            .withStyle(ChatFormatting.RED));
            return 0;
        }

        String dimId = DimensionUtil.dimensionToString(player.level().dimension());
        Identifier dimension = Identifier.tryParse(dimId);
        if (dimension == null) {
            MessageDisplayManager.sendSystemMessage(player,
                    LanguageManager.prefixed("Home", "home.error.dimension_parse", player)
                            .withStyle(ChatFormatting.RED));
            return 0;
        }
        HomeData home = new HomeData(name, dimension, player.blockPosition());

        if (HomeManager.addHome(player.getUUID(), home)) {
            MessageDisplayManager.sendSystemMessage(player,
                    LanguageManager.prefixed("Home", "home.success.set", player, name)
                            .withStyle(ChatFormatting.GREEN));
        } else {
            MessageDisplayManager.sendSystemMessage(player,
                    LanguageManager.prefixed("Home", "home.error.exists", player, name)
                            .withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    private static int teleportHome(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (!checkEnabled(ctx.getSource(), player)) return 0;

        Optional<HomeData> opt = HomeManager.getHome(player.getUUID(), name);
        if (opt.isPresent()) {
            HomeData home = opt.get();
            var dimension = ctx.getSource().getServer().getLevel(net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION, home.getDimensionId()));
            if (dimension == null) {
                MessageDisplayManager.sendSystemMessage(player,
                        LanguageManager.prefixed("Home", "home.error.world_not_found", player, home.getDimensionId().toString())
                                .withStyle(ChatFormatting.RED));
                return 0;
            }
            BackManager.recordTeleport(player);
            player.teleportTo(dimension, home.getPos().getX() + 0.5, home.getPos().getY(), home.getPos().getZ() + 0.5,
                    java.util.Set.of(), player.getYRot(), player.getXRot(), true);
            MessageDisplayManager.sendSystemMessage(player,
                    LanguageManager.prefixed("Home", "home.success.teleport", player, name)
                            .withStyle(ChatFormatting.GREEN));
        } else {
            MessageDisplayManager.sendSystemMessage(player,
                    LanguageManager.prefixed("Home", "home.error.not_found", player, name)
                            .withStyle(ChatFormatting.RED));
        }
        return 1;
    }

    private static int listHomes(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (!checkEnabled(ctx.getSource(), player)) return 0;

        List<HomeData> list = HomeManager.getHomes(player.getUUID());
        if (list.isEmpty()) {
            ctx.getSource().sendSuccess(() ->
                    LanguageManager.prefixed("Home", "home.list.empty", player)
                            .withStyle(ChatFormatting.YELLOW), false);
            return 0;
        }

        ctx.getSource().sendSuccess(() -> {
            MutableComponent msg = LanguageManager.prefixed("Home", "home.list.header", player)
                    .withStyle(ChatFormatting.GOLD);
            for (HomeData h : list) {
                String entry = LanguageManager.format(
                        LanguageManager.translate("home.list.entry", player),
                        h.getName(), h.getDimensionId().toString(), h.getPos().toShortString()
                );
                msg.append("\n").append(Component.literal(entry).withStyle(ChatFormatting.WHITE));
            }
            return msg;
        }, false);
        return 1;
    }

    private static int deleteHome(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (!checkEnabled(ctx.getSource(), player)) return 0;

        String name = StringArgumentType.getString(ctx, "name");
        if (HomeManager.deleteHome(player.getUUID(), name)) {
            MessageDisplayManager.sendSystemMessage(player,
                    LanguageManager.prefixed("Home", "home.success.delete", player, name)
                            .withStyle(ChatFormatting.GREEN));
        } else {
            MessageDisplayManager.sendSystemMessage(player,
                    LanguageManager.prefixed("Home", "home.error.delete_not_found", player, name)
                            .withStyle(ChatFormatting.RED));
        }
        return 1;
    }
}