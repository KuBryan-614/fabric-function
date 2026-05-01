package kuku.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import kuku.config.BackConfig;
import kuku.back.BackManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class BackCommand {

    private static MutableComponent prefix() {
        return Component.literal("[Function Back] ").withStyle(ChatFormatting.GOLD);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("back")
                .executes(BackCommand::teleportBack));
    }

    private static boolean checkEnabled(CommandSourceStack source) {
        if (!BackConfig.getInstance().isEnabled()) {
            source.sendFailure(prefix().append(
                    Component.literal("Back 模块已被禁用。").withStyle(ChatFormatting.RED)));
            return false;
        }
        return true;
    }

    private static int teleportBack(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (!checkEnabled(ctx.getSource())) return 0;

        ServerPlayer player = ctx.getSource().getPlayerOrException();
        BackManager.LastLocation loc = BackManager.consume(player.getUUID());

        if (loc == null) {
            ctx.getSource().sendFailure(prefix().append(
                    Component.literal("没有可返回的上一个位置。").withStyle(ChatFormatting.RED)));
            return 0;
        }

        // 获取目标世界
        ServerLevel targetWorld = player.level().getServer().getLevel(
                ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, loc.dimensionId()));
        if (targetWorld == null) {
            ctx.getSource().sendFailure(prefix().append(
                    Component.literal("目标世界不存在：" + loc.dimensionId()).withStyle(ChatFormatting.RED)));
            return 0;
        }

        player.teleportTo(targetWorld, loc.x(), loc.y(), loc.z(),
                java.util.Set.of(), loc.yaw(), loc.pitch(), true);

        ctx.getSource().sendSuccess(() -> prefix().append(
                Component.literal("已返回上一个位置。").withStyle(ChatFormatting.GREEN)), false);
        return 1;
    }
}