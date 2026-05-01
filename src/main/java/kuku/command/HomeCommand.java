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
import kuku.util.DimensionUtil;
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

    // 消息前缀，金色 [Home]
    private static MutableComponent prefix() {
        return Component.literal("[Function Home] ").withStyle(ChatFormatting.GOLD);
    }

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

    private static boolean checkEnabled(CommandSourceStack source) {
        if (!HomeConfig.getInstance().isEnabled()) {
            source.sendFailure(prefix().append(Component.literal("Home 模块已被禁用。").withStyle(ChatFormatting.RED)));
            return false;
        }
        return true;
    }

    private static int setHome(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (!checkEnabled(ctx.getSource())) return 0;
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "name");
        List<HomeData> list = HomeManager.getHomes(player.getUUID());
        HomeConfig config = HomeConfig.getInstance();

        if (list.size() >= config.getMaxHomes()) {
            ctx.getSource().sendFailure(prefix().append(
                    Component.literal("你已达到最大家园数量 (" + config.getMaxHomes() + ")。").withStyle(ChatFormatting.RED)));
            return 0;
        }

        String dimId = DimensionUtil.dimensionToString(player.level().dimension());
        Identifier dimension = Identifier.tryParse(dimId);
        if (dimension == null) {
            ctx.getSource().sendFailure(prefix().append(
                    Component.literal("无法解析当前维度ID，设置失败。").withStyle(ChatFormatting.RED)));
            return 0;
        }
        HomeData home = new HomeData(name, dimension, player.blockPosition());

        if (HomeManager.addHome(player.getUUID(), home)) {
            ctx.getSource().sendSuccess(() -> prefix().append(
                    Component.literal("家点 '").withStyle(ChatFormatting.GREEN)
                            .append(Component.literal(name).withStyle(ChatFormatting.WHITE))
                            .append(Component.literal("' 设置成功！").withStyle(ChatFormatting.GREEN))
            ), false);
        } else {
            ctx.getSource().sendFailure(prefix().append(
                    Component.literal("家点 '").withStyle(ChatFormatting.RED)
                            .append(Component.literal(name).withStyle(ChatFormatting.WHITE))
                            .append(Component.literal("' 已存在，请换个名称。").withStyle(ChatFormatting.RED))
            ));
        }
        return 1;
    }

    private static int teleportHome(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
        if (!checkEnabled(ctx.getSource())) return 0;
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Optional<HomeData> opt = HomeManager.getHome(player.getUUID(), name);
        if (opt.isPresent()) {
            HomeData home = opt.get();
            var dimension = ctx.getSource().getServer().getLevel(net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION, home.getDimensionId()));
            if (dimension == null) {
                ctx.getSource().sendFailure(prefix().append(
                        Component.literal("无法找到维度：" + home.getDimensionId()).withStyle(ChatFormatting.RED)));
                return 0;
            }
            // 记录返回点
            BackManager.record(player);
            player.teleportTo(dimension, home.getPos().getX() + 0.5, home.getPos().getY(), home.getPos().getZ() + 0.5,
                    java.util.Set.of(), player.getYRot(), player.getXRot(), true);
            ctx.getSource().sendSuccess(() -> prefix().append(
                    Component.literal("已传送到家点 '").withStyle(ChatFormatting.GREEN)
                            .append(Component.literal(name).withStyle(ChatFormatting.WHITE))
                            .append(Component.literal("'。").withStyle(ChatFormatting.GREEN))
            ), false);
        } else {
            ctx.getSource().sendFailure(prefix().append(
                    Component.literal("找不到家点 '").withStyle(ChatFormatting.RED)
                            .append(Component.literal(name).withStyle(ChatFormatting.WHITE))
                            .append(Component.literal("'。").withStyle(ChatFormatting.RED))
            ));
        }
        return 1;
    }

    private static int listHomes(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (!checkEnabled(ctx.getSource())) return 0;
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        List<HomeData> list = HomeManager.getHomes(player.getUUID());
        if (list.isEmpty()) {
            ctx.getSource().sendSuccess(() -> prefix().append(
                    Component.literal("你还没有设置任何家点。").withStyle(ChatFormatting.YELLOW)), false);
            return 0;
        }
        // 构建带颜色的家点列表
        ctx.getSource().sendSuccess(() -> {
            MutableComponent msg = prefix().append(Component.literal("你的家点：").withStyle(ChatFormatting.GOLD));
            for (HomeData h : list) {
                msg.append("\n")
                        .append(Component.literal(" - " + h.getName()).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(" @" + h.getDimensionId() + " " + h.getPos().toShortString())
                                .withStyle(ChatFormatting.GRAY));
            }
            return msg;
        }, false);
        return 1;
    }

    private static int deleteHome(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (!checkEnabled(ctx.getSource())) return 0;
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "name");
        if (HomeManager.deleteHome(player.getUUID(), name)) {
            ctx.getSource().sendSuccess(() -> prefix().append(
                    Component.literal("家点 '").withStyle(ChatFormatting.GREEN)
                            .append(Component.literal(name).withStyle(ChatFormatting.WHITE))
                            .append(Component.literal("' 已删除。").withStyle(ChatFormatting.GREEN))
            ), false);
        } else {
            ctx.getSource().sendFailure(prefix().append(
                    Component.literal("家点 '").withStyle(ChatFormatting.RED)
                            .append(Component.literal(name).withStyle(ChatFormatting.WHITE))
                            .append(Component.literal("' 不存在！").withStyle(ChatFormatting.RED))
            ));
        }
        return 1;
    }
}