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

        WarpData warp = new WarpData(name, player.getUUID(), dimension, player.blockPosition());
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

        if (player != null && !checkEnabled(ctx.getSource(), player)) return 0;

        if (player != null) {
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
            if (!WarpManager.forceDelete(name)) {
                MessageDisplayManager.sendSystemMessage(null,
                        LanguageManager.prefixed("Warp", "warp.error.not_found", null, name)
                                .withStyle(ChatFormatting.RED));
                return 0;
            }
            MessageDisplayManager.sendSystemMessage(null,
                    LanguageManager.prefixed("Warp", "warp.success.delete", null, name)
                            .withStyle(ChatFormatting.GREEN));
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
        ServerPlayer tempPlayer = null;
        try {
            tempPlayer = ctx.getSource().getPlayerOrException();
        } catch (CommandSyntaxException ignored) {}
        final ServerPlayer player = tempPlayer;

        if (player != null && !checkEnabled(ctx.getSource(), player)) return 0;

        var all = WarpManager.getAllWarps();
        if (all.isEmpty()) {
            MutableComponent msg = LanguageManager.prefixed("Warp", "warp.list.empty", player)
                    .withStyle(ChatFormatting.YELLOW);
            if (player != null) {
                player.sendSystemMessage(msg);               // ✅ 强制聊天栏
            } else {
                System.out.println(msg.getString());
            }
            return 0;
        }

        MutableComponent msg = LanguageManager.prefixed("Warp", "warp.list.header", player)
                .withStyle(ChatFormatting.GOLD);
        for (WarpData w : all) {
            String entry = LanguageManager.format(
                    LanguageManager.translate("warp.list.entry", player),
                    w.getName(), w.getDimensionId().toString(), w.getPos().toShortString()
            );
            msg.append("\n").append(Component.literal(entry).withStyle(ChatFormatting.WHITE));
        }
        if (player != null) {
            player.sendSystemMessage(msg);                   // ✅ 强制聊天栏
        } else {
            System.out.println(msg.getString());
        }
        return 1;
    }
}