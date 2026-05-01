package kuku.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import kuku.back.BackManager;
import kuku.util.DimensionUtil;
import kuku.config.WarpConfig;
import kuku.data.WarpData;
import kuku.warp.WarpManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class WarpCommand {

    private static MutableComponent prefix() {
        return Component.literal("[Warp] ").withStyle(ChatFormatting.GOLD);
    }

    // 为 /warp 和 /delwarp 提供所有 warp 名补全，但 /delwarp 只显示自己的
    private static final SuggestionProvider<CommandSourceStack> ALL_WARPS = (context, builder) -> {
        for (WarpData w : WarpManager.getAllWarps()) {
            builder.suggest(w.getName());
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> OWNED_WARPS = (context, builder) -> {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            for (WarpData w : WarpManager.getWarpsByOwner(player.getUUID())) {
                builder.suggest(w.getName());
            }
        } catch (CommandSyntaxException ignored) {}
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!WarpConfig.getInstance().isEnabled()) return;

        dispatcher.register(Commands.literal("setwarp")
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(WarpCommand::setWarp)));

        dispatcher.register(Commands.literal("delwarp")
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .suggests(OWNED_WARPS)
                        .executes(WarpCommand::deleteWarp)));

        dispatcher.register(Commands.literal("warp")
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .suggests(ALL_WARPS)
                        .executes(WarpCommand::teleportWarp)));

        dispatcher.register(Commands.literal("warps")
                .executes(WarpCommand::listWarps));
    }

    private static boolean checkEnabled(CommandSourceStack source) {
        if (!WarpConfig.getInstance().isEnabled()) {
            source.sendFailure(prefix().append(
                    Component.literal("Warp 模块已被禁用。").withStyle(ChatFormatting.RED)));
            return false;
        }
        return true;
    }

    private static int setWarp(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (!checkEnabled(ctx.getSource())) return 0;
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "name");

        // 维度ID提取（沿用 Home 模块的 dimensionToString）
        String dimId = DimensionUtil.dimensionToString(player.level().dimension());
        Identifier dimension = Identifier.tryParse(dimId);
        if (dimension == null) {
            ctx.getSource().sendFailure(prefix().append(
                    Component.literal("无法解析当前维度ID，设置失败。").withStyle(ChatFormatting.RED)));
            return 0;
        }

        WarpData warp = new WarpData(name, player.getUUID(), dimension, player.blockPosition());
        if (WarpManager.addWarp(warp)) {
            ctx.getSource().sendSuccess(() -> prefix().append(
                    Component.literal("公共传送点 '").withStyle(ChatFormatting.GREEN)
                            .append(Component.literal(name).withStyle(ChatFormatting.WHITE))
                            .append(Component.literal("' 设置成功！").withStyle(ChatFormatting.GREEN))
            ), false);
        } else {
            // 失败原因可能是名称重复或超过上限
            if (WarpManager.getWarp(name) != null) {
                ctx.getSource().sendFailure(prefix().append(
                        Component.literal("公共传送点 '").withStyle(ChatFormatting.RED)
                                .append(Component.literal(name).withStyle(ChatFormatting.WHITE))
                                .append(Component.literal("' 已存在！").withStyle(ChatFormatting.RED))
                ));
            } else {
                ctx.getSource().sendFailure(prefix().append(
                        Component.literal("你已达到最大公共传送点数量 (" + WarpConfig.getInstance().getMaxWarps() + ")。").withStyle(ChatFormatting.RED)));
            }
        }
        return 1;
    }

    private static int deleteWarp(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (!checkEnabled(ctx.getSource())) return 0;
        String name = StringArgumentType.getString(ctx, "name");
        // 判断执行者是否是玩家，若是玩家则限制只能删除自己的
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            WarpData warp = WarpManager.getWarp(name);
            if (warp == null) {
                ctx.getSource().sendFailure(prefix().append(
                        Component.literal("公共传送点 '").withStyle(ChatFormatting.RED)
                                .append(Component.literal(name).withStyle(ChatFormatting.WHITE))
                                .append(Component.literal("' 不存在！").withStyle(ChatFormatting.RED))
                ));
                return 0;
            }
            if (!warp.getOwnerUUID().equals(player.getUUID())) {
                ctx.getSource().sendFailure(prefix().append(
                        Component.literal("你只能删除自己设置的公共传送点。").withStyle(ChatFormatting.RED)));
                return 0;
            }
            WarpManager.deleteWarp(name, player.getUUID());
        } catch (CommandSyntaxException e) {
            // 控制台没有玩家实体，允许强制删除任意 warp
            if (!WarpManager.forceDelete(name)) {
                ctx.getSource().sendFailure(prefix().append(
                        Component.literal("公共传送点 '" + name + "' 不存在！").withStyle(ChatFormatting.RED)));
                return 0;
            }
        }

        ctx.getSource().sendSuccess(() -> prefix().append(
                Component.literal("公共传送点 '").withStyle(ChatFormatting.GREEN)
                        .append(Component.literal(name).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal("' 已删除。").withStyle(ChatFormatting.GREEN))
        ), true);
        return 1;
    }

    private static int teleportWarp(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (!checkEnabled(ctx.getSource())) return 0;
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "name");
        WarpData warp = WarpManager.getWarp(name);
        if (warp == null) {
            ctx.getSource().sendFailure(prefix().append(
                    Component.literal("找不到公共传送点 '").withStyle(ChatFormatting.RED)
                            .append(Component.literal(name).withStyle(ChatFormatting.WHITE))
                            .append(Component.literal("'。").withStyle(ChatFormatting.RED))
            ));
            return 0;
        }

        ServerLevel targetWorld = player.level().getServer().getLevel(
                ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, warp.getDimensionId()));
        if (targetWorld == null) {
            ctx.getSource().sendFailure(prefix().append(
                    Component.literal("目标世界不存在：" + warp.getDimensionId()).withStyle(ChatFormatting.RED)));
            return 0;
        }

        // 记录返回点
        BackManager.record(player);
        player.teleportTo(targetWorld,
                warp.getPos().getX() + 0.5, warp.getPos().getY(), warp.getPos().getZ() + 0.5,
                java.util.Set.of(), player.getYRot(), player.getXRot(), true);

        ctx.getSource().sendSuccess(() -> prefix().append(
                Component.literal("已传送到公共传送点 '").withStyle(ChatFormatting.GREEN)
                        .append(Component.literal(name).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal("'。").withStyle(ChatFormatting.GREEN))
        ), false);
        return 1;
    }

    private static int listWarps(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (!checkEnabled(ctx.getSource())) return 0;
        var all = WarpManager.getAllWarps();
        if (all.isEmpty()) {
            ctx.getSource().sendSuccess(() -> prefix().append(
                    Component.literal("目前没有任何公共传送点。").withStyle(ChatFormatting.YELLOW)), false);
            return 0;
        }
        ctx.getSource().sendSuccess(() -> {
            MutableComponent msg = prefix().append(Component.literal("所有公共传送点：").withStyle(ChatFormatting.GOLD));
            for (WarpData w : all) {
                msg.append("\n")
                        .append(Component.literal(" - " + w.getName()).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(" @" + w.getDimensionId() + " " + w.getPos().toShortString())
                                .withStyle(ChatFormatting.GRAY));
            }
            return msg;
        }, false);
        return 1;
    }

}