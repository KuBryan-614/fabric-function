package kuku.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import kuku.back.BackManager;
import kuku.lang.LanguageManager;
import kuku.util.DimensionUtil;
import kuku.config.WarpConfig;
import kuku.data.WarpData;
import kuku.warp.WarpManager;
import kuku.util.MessageDisplayManager;
import kuku.warp.gui.WarpGuiHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collection;

public class WarpCommand {

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

        dispatcher.register(Commands.literal("renamewarp")
                .then(Commands.argument("old", StringArgumentType.string())       // 只取一个单词
                        .then(Commands.argument("new", StringArgumentType.greedyString()) // 剩余全部
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    if (!checkEnabled(ctx.getSource(), player)) return 0;
                                    String old = StringArgumentType.getString(ctx, "old");
                                    String newName = StringArgumentType.getString(ctx, "new");
                                    WarpData warp = WarpManager.getWarp(old);
                                    if (warp == null) {
                                        player.sendSystemMessage(Component.literal("§c传送点不存在"));
                                        return 0;
                                    }
                                    if (!warp.getOwnerUUID().equals(player.getUUID())) {
                                        player.sendSystemMessage(Component.literal("§c你不是该传送点的所有者"));
                                        return 0;
                                    }
                                    if (WarpManager.getWarp(newName) != null) {
                                        player.sendSystemMessage(Component.literal("§c该名称已被占用"));
                                        return 0;
                                    }
                                    WarpManager.renameWarp(old, newName);
                                    player.sendSystemMessage(Component.literal("§a重命名成功：§e" + newName));
                                    return 1;
                                })
                        )
                )
        );
    }

    private static boolean checkEnabled(CommandSourceStack source, ServerPlayer player) {
        if (!WarpConfig.getInstance().isEnabled()) {
            MessageDisplayManager.sendSystemMessage(player,
                    LanguageManager.prefixed("Warp", "warp.error.disabled", player)
                            .withStyle(ChatFormatting.RED));
            return false;
        }
        return true;
    }

    private static int setWarp(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (!checkEnabled(ctx.getSource(), player)) return 0;

        String name = StringArgumentType.getString(ctx, "name");
        String dimId = DimensionUtil.dimensionToString(player.level().dimension());
        Identifier dimension = Identifier.tryParse(dimId);
        if (dimension == null) {
            MessageDisplayManager.sendSystemMessage(player,
                    LanguageManager.prefixed("Warp", "warp.error.dimension_parse", player)
                            .withStyle(ChatFormatting.RED));
            return 0;
        }

        WarpData warp = new WarpData(name, player.getUUID(), player.getName().getString(), dimension, player.blockPosition());

        // 自动将脚下非空气方块设为图标
        BlockPos below = player.blockPosition().below();
        BlockState state = player.level().getBlockState(below);
        if (!state.isAir()) {
            Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            warp.setIconItemId(blockId.toString());
        }

        if (WarpManager.addWarp(warp)) {
            MessageDisplayManager.sendSystemMessage(player,
                    LanguageManager.prefixed("Warp", "warp.success.set", player, name)
                            .withStyle(ChatFormatting.GREEN));
        } else {
            if (WarpManager.getWarp(name) != null) {
                MessageDisplayManager.sendSystemMessage(player,
                        LanguageManager.prefixed("Warp", "warp.error.exists", player, name)
                                .withStyle(ChatFormatting.RED));
            } else {
                MessageDisplayManager.sendSystemMessage(player,
                        LanguageManager.prefixed("Warp", "warp.error.max_warps", player, WarpConfig.getInstance().getMaxWarps())
                                .withStyle(ChatFormatting.RED));
            }
        }
        return 1;
    }

    private static int deleteWarp(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String name = StringArgumentType.getString(ctx, "name");
        ServerPlayer tempPlayer = null;
        try {
            tempPlayer = ctx.getSource().getPlayerOrException();
        } catch (CommandSyntaxException ignored) {}

        final ServerPlayer player = tempPlayer;

        // 如果是玩家執行，則檢查模組是否啟用
        if (player != null && !checkEnabled(ctx.getSource(), player)) return 0;

        if (player != null) {
            // 玩家執行：檢查所有權與存在性，並發送系統訊息
            WarpData warp = WarpManager.getWarp(name);
            if (warp == null) {
                MessageDisplayManager.sendSystemMessage(player,
                        LanguageManager.prefixed("Warp", "warp.error.not_found", player, name)
                                .withStyle(ChatFormatting.RED));
                return 0;
            }
            if (!warp.getOwnerUUID().equals(player.getUUID())) {
                MessageDisplayManager.sendSystemMessage(player,
                        LanguageManager.prefixed("Warp", "warp.error.not_owner", player)
                                .withStyle(ChatFormatting.RED));
                return 0;
            }
            WarpManager.deleteWarp(name, player.getUUID());
            MessageDisplayManager.sendSystemMessage(player,
                    LanguageManager.prefixed("Warp", "warp.success.delete", player, name)
                            .withStyle(ChatFormatting.GREEN));
        } else {
            // 控制台執行：無視所有權，直接強制刪除
            if (!WarpManager.forceDelete(name)) {
                System.out.println("[Function Warp] 傳送點不存在：" + name);
                return 0;
            }
            System.out.println("[Function Warp] 已刪除傳送點：" + name);
        }
        return 1;
    }

    private static int teleportWarp(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (!checkEnabled(ctx.getSource(), player)) return 0;

        String name = StringArgumentType.getString(ctx, "name");
        WarpData warp = WarpManager.getWarp(name);
        if (warp == null) {
            MessageDisplayManager.sendSystemMessage(player,
                    LanguageManager.prefixed("Warp", "warp.error.teleport_not_found", player, name)
                            .withStyle(ChatFormatting.RED));
            return 0;
        }

        ServerLevel targetWorld = player.level().getServer().getLevel(
                ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, warp.getDimensionId()));
        if (targetWorld == null) {
            MessageDisplayManager.sendSystemMessage(player,
                    LanguageManager.prefixed("Warp", "warp.error.world_not_found", player, warp.getDimensionId().toString())
                            .withStyle(ChatFormatting.RED));
            return 0;
        }

        BackManager.recordTeleport(player);
        player.teleportTo(targetWorld,
                warp.getPos().getX() + 0.5, warp.getPos().getY(), warp.getPos().getZ() + 0.5,
                java.util.Set.of(), player.getYRot(), player.getXRot(), true);

        MessageDisplayManager.sendSystemMessage(player,
                LanguageManager.prefixed("Warp", "warp.success.teleport", player, name)
                        .withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int listWarps(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = null;
        try {
            player = ctx.getSource().getPlayerOrException();
        } catch (CommandSyntaxException ignored) {}

        if (player != null) {
            if (!checkEnabled(ctx.getSource(), player)) return 0;
            WarpGuiHelper.openMainMenu(player);
            return 1;
        } else {
            var all = WarpManager.getAllWarps();
            if (all.isEmpty()) {
                System.out.println("[Function Warp] 暂无传送点");
                return 0;
            }
            System.out.println("--- 传送点列表 ---");
            for (WarpData w : all) {
                System.out.println(w.getName() + " | 主人: " + w.getOwnerName() + " | " + w.getDimensionId() + " " + w.getPos().toShortString());
            }
            return 1;
        }
    }
}