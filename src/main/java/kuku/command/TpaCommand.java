package kuku.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import kuku.back.BackManager;
import kuku.config.TpaConfig;
import kuku.tpa.TpaManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class TpaCommand {

    private static MutableComponent prefix() {
        return Component.literal("[Function Tpa] ").withStyle(ChatFormatting.GOLD);
    }

    private static final SuggestionProvider<CommandSourceStack> PLAYER_SUGGESTIONS =
            (context, builder) -> {
                try {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    for (ServerPlayer p : player.level().getServer().getPlayerList().getPlayers()) {
                        if (!p.getUUID().equals(player.getUUID())) {
                            builder.suggest(p.getName().getString());
                        }
                    }
                } catch (CommandSyntaxException ignored) {
                }
                return builder.buildFuture();
            };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tpa")
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(PLAYER_SUGGESTIONS)
                        .executes(TpaCommand::sendRequest))
        );

        dispatcher.register(Commands.literal("tpahere")
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(PLAYER_SUGGESTIONS)
                        .executes(TpaCommand::sendHereRequest))
        );

        dispatcher.register(Commands.literal("tpaccept")
                .executes(TpaCommand::acceptRequest)
        );

        dispatcher.register(Commands.literal("tpadeny")
                .executes(TpaCommand::denyRequest)
        );
    }

    private static boolean checkEnabled(CommandSourceStack source) {
        if (!TpaConfig.getInstance().isEnabled()) {
            source.sendFailure(prefix().append(
                    Component.literal("TPA 模块已被禁用。").withStyle(ChatFormatting.RED)));
            return false;
        }
        return true;
    }

    private static int sendHereRequest(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (!checkEnabled(ctx.getSource())) return 0;
        ServerPlayer sender = ctx.getSource().getPlayerOrException();
        String targetName = StringArgumentType.getString(ctx, "player");
        ServerPlayer target = sender.level().getServer().getPlayerList().getPlayerByName(targetName);

        if (target == null) {
            ctx.getSource().sendFailure(prefix().append(
                    Component.literal("玩家 " + targetName + " 不在线。").withStyle(ChatFormatting.RED)));
            return 0;
        }
        if (target.getUUID().equals(sender.getUUID())) {
            ctx.getSource().sendFailure(prefix().append(
                    Component.literal("你不能向自己发送传送请求。").withStyle(ChatFormatting.RED)));
            return 0;
        }

        TpaConfig config = TpaConfig.getInstance();
        TpaManager.clearExpired(config.getTimeout() * 1000L);

        UUID oldSenderUUID = TpaManager.getExistingSender(target.getUUID());
        if (oldSenderUUID != null) {
            ServerPlayer oldSender = sender.level().getServer().getPlayerList().getPlayer(oldSenderUUID);
            if (oldSender != null) {
                oldSender.sendSystemMessage(prefix().append(
                        Component.literal("你发往 ").withStyle(ChatFormatting.GRAY)
                                .append(Component.literal(target.getName().getString()).withStyle(ChatFormatting.WHITE))
                                .append(Component.literal(" 的传送请求已被新请求覆盖。").withStyle(ChatFormatting.GRAY))
                ));
            }
        }

        TpaManager.addRequest(target.getUUID(), sender.getUUID(), TpaManager.RequestType.TPA_HERE);

        sender.sendSystemMessage(prefix().append(
                Component.literal("你向 ").withStyle(ChatFormatting.GREEN)
                        .append(Component.literal(targetName).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(" 发送了传送请求（使对方传送到你身边）。").withStyle(ChatFormatting.GREEN))
        ));

        // 可点击按钮 - 对方点击同意后会执行 /tpaccept
        MutableComponent agreeBtn = Component.literal("[同意]")
                .withStyle(style -> style.withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent.RunCommand("/tpaccept")));
        MutableComponent denyBtn = Component.literal("[拒絕]")
                .withStyle(style -> style.withColor(ChatFormatting.RED)
                        .withClickEvent(new ClickEvent.RunCommand("/tpadeny")));

        target.sendSystemMessage(prefix()
                .append(Component.literal(sender.getName().getString()).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" 请求你传送到他身边，是否同意？ ").withStyle(ChatFormatting.GREEN))
                .append(agreeBtn).append(" ").append(denyBtn)
                .append(Component.literal("  （" + config.getTimeout() + "秒后过期）").withStyle(ChatFormatting.GRAY))
        );

        target.sendSystemMessage(prefix()
                .append(Component.literal("或输入 ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("/tpaccept").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" 或 ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("/tpadeny").withStyle(ChatFormatting.RED))
        );

        return 1;
    }

    private static int sendRequest(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (!checkEnabled(ctx.getSource())) return 0;
        ServerPlayer sender = ctx.getSource().getPlayerOrException();
        String targetName = StringArgumentType.getString(ctx, "player");
        ServerPlayer target = sender.level().getServer().getPlayerList().getPlayerByName(targetName);

        if (target == null) {
            ctx.getSource().sendFailure(prefix().append(
                    Component.literal("玩家 " + targetName + " 不在线。").withStyle(ChatFormatting.RED)));
            return 0;
        }
        if (target.getUUID().equals(sender.getUUID())) {
            ctx.getSource().sendFailure(prefix().append(
                    Component.literal("你不能向自己发送传送请求。").withStyle(ChatFormatting.RED)));
            return 0;
        }

        TpaConfig config = TpaConfig.getInstance();
        TpaManager.clearExpired(config.getTimeout() * 1000L);

        UUID oldSenderUUID = TpaManager.getExistingSender(target.getUUID());
        if (oldSenderUUID != null) {
            ServerPlayer oldSender = sender.level().getServer().getPlayerList().getPlayer(oldSenderUUID);
            if (oldSender != null) {
                oldSender.sendSystemMessage(prefix().append(
                        Component.literal("你发往 ").withStyle(ChatFormatting.GRAY)
                                .append(Component.literal(target.getName().getString()).withStyle(ChatFormatting.WHITE))
                                .append(Component.literal(" 的传送请求已被新请求覆盖。").withStyle(ChatFormatting.GRAY))
                ));
            }
        }

        TpaManager.addRequest(target.getUUID(), sender.getUUID(), TpaManager.RequestType.TPA_TO_TARGET);

        sender.sendSystemMessage(prefix().append(
                Component.literal("你向 ").withStyle(ChatFormatting.GREEN)
                        .append(Component.literal(targetName).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(" 发送了传送请求。").withStyle(ChatFormatting.GREEN))
        ));

        // 修正：使用 new ClickEvent.RunCommand(...) 构造可点击按钮
        MutableComponent agreeBtn = Component.literal("[同意]")
                .withStyle(style -> style.withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent.RunCommand("/tpaccept")));
        MutableComponent denyBtn = Component.literal("[拒絕]")
                .withStyle(style -> style.withColor(ChatFormatting.RED)
                        .withClickEvent(new ClickEvent.RunCommand("/tpadeny")));

        target.sendSystemMessage(prefix()
                .append(Component.literal(sender.getName().getString()).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" 请求传送到你身旁，是否同意？ ").withStyle(ChatFormatting.GREEN))
                .append(agreeBtn).append(" ").append(denyBtn)
                .append(Component.literal("  （" + config.getTimeout() + "秒后过期）").withStyle(ChatFormatting.GRAY))
        );

        target.sendSystemMessage(prefix()
                .append(Component.literal("或输入 ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("/tpaccept").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" 或 ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("/tpadeny").withStyle(ChatFormatting.RED))
        );

        return 1;
    }

    private static int acceptRequest(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (!checkEnabled(ctx.getSource())) return 0;
        ServerPlayer target = ctx.getSource().getPlayerOrException();
        TpaManager.TpaRequest request = TpaManager.getRequest(target.getUUID());

        if (request == null) {
            ctx.getSource().sendFailure(prefix().append(
                    Component.literal("你没有待处理的传送请求。").withStyle(ChatFormatting.RED)));
            return 0;
        }

        TpaConfig config = TpaConfig.getInstance();
        if (System.currentTimeMillis() - request.timestamp > config.getTimeout() * 1000L) {
            TpaManager.removeRequest(target.getUUID());
            ctx.getSource().sendFailure(prefix().append(
                    Component.literal("请求已超时。").withStyle(ChatFormatting.RED)));
            return 0;
        }

        ServerPlayer sender = target.level().getServer().getPlayerList().getPlayer(request.sender);
        if (sender == null) {
            TpaManager.removeRequest(target.getUUID());
            ctx.getSource().sendFailure(prefix().append(
                    Component.literal("请求者已离线。").withStyle(ChatFormatting.RED)));
            return 0;
        }

        // 根据请求类型决定传送方向
        if (request.type == TpaManager.RequestType.TPA_HERE) {
            // /tpahere：接受者（target）传送到发送者（sender）
            BackManager.record(sender);
            target.teleportTo((ServerLevel) sender.level(),
                    sender.getX(), sender.getY(), sender.getZ(),
                    java.util.Set.of(), target.getYRot(), target.getXRot(), true);

            target.sendSystemMessage(prefix().append(
                    Component.literal("你接受了 ").withStyle(ChatFormatting.GREEN)
                            .append(Component.literal(sender.getName().getString()).withStyle(ChatFormatting.WHITE))
                            .append(Component.literal(" 的传送请求，正在传送过去。").withStyle(ChatFormatting.GREEN))
            ));
            sender.sendSystemMessage(prefix().append(
                    Component.literal(target.getName().getString()).withStyle(ChatFormatting.WHITE)
                            .append(Component.literal(" 接受了你的传送请求，正在传送到你身边。").withStyle(ChatFormatting.GREEN))
            ));
        } else {
            // 默认 /tpa：发送者（sender）传送到接受者（target）
            BackManager.record(sender);
            sender.teleportTo((ServerLevel) target.level(),
                    target.getX(), target.getY(), target.getZ(),
                    java.util.Set.of(), sender.getYRot(), sender.getXRot(), true);

            target.sendSystemMessage(prefix().append(
                    Component.literal("你接受了 ").withStyle(ChatFormatting.GREEN)
                            .append(Component.literal(sender.getName().getString()).withStyle(ChatFormatting.WHITE))
                            .append(Component.literal(" 的传送请求。").withStyle(ChatFormatting.GREEN))
            ));
            sender.sendSystemMessage(prefix().append(
                    Component.literal(target.getName().getString()).withStyle(ChatFormatting.WHITE)
                            .append(Component.literal(" 接受了你的传送请求，正在传送...").withStyle(ChatFormatting.GREEN))
            ));
        }

        TpaManager.removeRequest(target.getUUID());
        return 1;
    }

    private static int denyRequest(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (!checkEnabled(ctx.getSource())) return 0;
        ServerPlayer target = ctx.getSource().getPlayerOrException();
        TpaManager.TpaRequest request = TpaManager.getRequest(target.getUUID());

        if (request == null) {
            ctx.getSource().sendFailure(prefix().append(
                    Component.literal("你没有待处理的传送请求。").withStyle(ChatFormatting.RED)));
            return 0;
        }

        ServerPlayer sender = target.level().getServer().getPlayerList().getPlayer(request.sender);
        TpaManager.removeRequest(target.getUUID());

        target.sendSystemMessage(prefix().append(
                Component.literal("你拒绝了传送请求。").withStyle(ChatFormatting.RED)));
        if (sender != null) {
            sender.sendSystemMessage(prefix().append(
                    Component.literal(target.getName().getString()).withStyle(ChatFormatting.WHITE)
                            .append(Component.literal(" 拒绝了你的传送请求。").withStyle(ChatFormatting.RED))
            ));
        }
        return 1;
    }
}