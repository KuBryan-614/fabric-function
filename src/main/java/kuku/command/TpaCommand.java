package kuku.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import kuku.back.BackManager;
import kuku.config.TpaConfig;
import kuku.lang.LanguageManager;
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

    private static boolean checkEnabled(CommandSourceStack source, ServerPlayer player) {
        if (!TpaConfig.getInstance().isEnabled()) {
            source.sendFailure(
                    LanguageManager.prefixed("Tpa", "tpa.error.disabled", player)
                            .withStyle(ChatFormatting.RED)
            );
            return false;
        }
        return true;
    }

    private static int sendHereRequest(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer sender = ctx.getSource().getPlayerOrException();
        if (!checkEnabled(ctx.getSource(), sender)) return 0;

        String targetName = StringArgumentType.getString(ctx, "player");
        ServerPlayer target = sender.level().getServer().getPlayerList().getPlayerByName(targetName);

        if (target == null) {
            ctx.getSource().sendFailure(
                    LanguageManager.prefixed("Tpa", "tpa.error.player_offline", sender, targetName)
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }
        if (target.getUUID().equals(sender.getUUID())) {
            ctx.getSource().sendFailure(
                    LanguageManager.prefixed("Tpa", "tpa.error.self_request", sender)
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        TpaConfig config = TpaConfig.getInstance();
        TpaManager.clearExpired(config.getTimeout() * 1000L);

        UUID oldSenderUUID = TpaManager.getExistingSender(target.getUUID());
        if (oldSenderUUID != null) {
            ServerPlayer oldSender = sender.level().getServer().getPlayerList().getPlayer(oldSenderUUID);
            if (oldSender != null) {
                oldSender.sendSystemMessage(
                        LanguageManager.prefixed("Tpa", "tpa.info.old_request_overwritten", oldSender, target.getName().getString())
                                .withStyle(ChatFormatting.GRAY)
                );
            }
        }

        TpaManager.addRequest(target.getUUID(), sender.getUUID(), TpaManager.RequestType.TPA_HERE);

        sender.sendSystemMessage(
                LanguageManager.prefixed("Tpa", "tpa.success.request_sent_here", sender, targetName)
                        .withStyle(ChatFormatting.GREEN)
        );

        // 可點擊按鈕
        MutableComponent agreeBtn = Component.literal(
                        LanguageManager.translate("tpa.button.accept", target))
                .withStyle(style -> style.withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent.RunCommand("/tpaccept")));
        MutableComponent denyBtn = Component.literal(
                        LanguageManager.translate("tpa.button.deny", target))
                .withStyle(style -> style.withColor(ChatFormatting.RED)
                        .withClickEvent(new ClickEvent.RunCommand("/tpadeny")));

        target.sendSystemMessage(
                LanguageManager.prefixed("Tpa", "", target)
                        .append(Component.literal(sender.getName().getString()).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(
                                        LanguageManager.translate("tpa.incoming.request_tpahere", target))
                                .withStyle(ChatFormatting.GREEN))
                        .append(agreeBtn)
                        .append(" ")
                        .append(denyBtn)
                        .append(Component.literal(
                                        LanguageManager.translate("tpa.incoming.timeout_note", target, config.getTimeout()))
                                .withStyle(ChatFormatting.GRAY))
        );

        target.sendSystemMessage(
                LanguageManager.prefixed("Tpa", "", target)
                        .append(Component.literal(LanguageManager.translate("tpa.incoming.or_use", target))
                                .withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("/tpaccept").withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(LanguageManager.translate("tpa.incoming.or", target))
                                .withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("/tpadeny").withStyle(ChatFormatting.RED))
        );

        return 1;
    }

    private static int sendRequest(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer sender = ctx.getSource().getPlayerOrException();
        if (!checkEnabled(ctx.getSource(), sender)) return 0;

        String targetName = StringArgumentType.getString(ctx, "player");
        ServerPlayer target = sender.level().getServer().getPlayerList().getPlayerByName(targetName);

        if (target == null) {
            ctx.getSource().sendFailure(
                    LanguageManager.prefixed("Tpa", "tpa.error.player_offline", sender, targetName)
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }
        if (target.getUUID().equals(sender.getUUID())) {
            ctx.getSource().sendFailure(
                    LanguageManager.prefixed("Tpa", "tpa.error.self_request", sender)
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        TpaConfig config = TpaConfig.getInstance();
        TpaManager.clearExpired(config.getTimeout() * 1000L);

        UUID oldSenderUUID = TpaManager.getExistingSender(target.getUUID());
        if (oldSenderUUID != null) {
            ServerPlayer oldSender = sender.level().getServer().getPlayerList().getPlayer(oldSenderUUID);
            if (oldSender != null) {
                oldSender.sendSystemMessage(
                        LanguageManager.prefixed("Tpa", "tpa.info.old_request_overwritten", oldSender, target.getName().getString())
                                .withStyle(ChatFormatting.GRAY)
                );
            }
        }

        TpaManager.addRequest(target.getUUID(), sender.getUUID(), TpaManager.RequestType.TPA_TO_TARGET);

        sender.sendSystemMessage(
                LanguageManager.prefixed("Tpa", "tpa.success.request_sent", sender, targetName)
                        .withStyle(ChatFormatting.GREEN)
        );

        MutableComponent agreeBtn = Component.literal(
                        LanguageManager.translate("tpa.button.accept", target))
                .withStyle(style -> style.withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent.RunCommand("/tpaccept")));
        MutableComponent denyBtn = Component.literal(
                        LanguageManager.translate("tpa.button.deny", target))
                .withStyle(style -> style.withColor(ChatFormatting.RED)
                        .withClickEvent(new ClickEvent.RunCommand("/tpadeny")));

        target.sendSystemMessage(
                LanguageManager.prefixed("Tpa", "", target)
                        .append(Component.literal(sender.getName().getString()).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(
                                        LanguageManager.translate("tpa.incoming.request_tpa", target))
                                .withStyle(ChatFormatting.GREEN))
                        .append(agreeBtn)
                        .append(" ")
                        .append(denyBtn)
                        .append(Component.literal(
                                        LanguageManager.translate("tpa.incoming.timeout_note", target, config.getTimeout()))
                                .withStyle(ChatFormatting.GRAY))
        );

        target.sendSystemMessage(
                LanguageManager.prefixed("Tpa", "", target)
                        .append(Component.literal(LanguageManager.translate("tpa.incoming.or_use", target))
                                .withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("/tpaccept").withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(LanguageManager.translate("tpa.incoming.or", target))
                                .withStyle(ChatFormatting.GRAY))
                        .append(Component.literal("/tpadeny").withStyle(ChatFormatting.RED))
        );

        return 1;
    }

    private static int acceptRequest(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = ctx.getSource().getPlayerOrException();
        if (!checkEnabled(ctx.getSource(), target)) return 0;

        TpaManager.TpaRequest request = TpaManager.getRequest(target.getUUID());

        if (request == null) {
            ctx.getSource().sendFailure(
                    LanguageManager.prefixed("Tpa", "tpa.error.no_pending", target)
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        TpaConfig config = TpaConfig.getInstance();
        if (System.currentTimeMillis() - request.timestamp > config.getTimeout() * 1000L) {
            TpaManager.removeRequest(target.getUUID());
            ctx.getSource().sendFailure(
                    LanguageManager.prefixed("Tpa", "tpa.error.expired", target)
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        ServerPlayer sender = target.level().getServer().getPlayerList().getPlayer(request.sender);
        if (sender == null) {
            TpaManager.removeRequest(target.getUUID());
            ctx.getSource().sendFailure(
                    LanguageManager.prefixed("Tpa", "tpa.error.requester_offline", target)
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        if (request.type == TpaManager.RequestType.TPA_HERE) {
            BackManager.recordTeleport(target);
            target.teleportTo((ServerLevel) sender.level(),
                    sender.getX(), sender.getY(), sender.getZ(),
                    java.util.Set.of(), target.getYRot(), target.getXRot(), true);

            target.sendSystemMessage(
                    LanguageManager.prefixed("Tpa", "tpa.success.accept_tpahere_target", target, sender.getName().getString())
                            .withStyle(ChatFormatting.GREEN)
            );
            sender.sendSystemMessage(
                    LanguageManager.prefixed("Tpa", "tpa.success.accept_tpahere_sender", sender, target.getName().getString())
                            .withStyle(ChatFormatting.GREEN)
            );
        } else {
            BackManager.recordTeleport(sender);
            sender.teleportTo((ServerLevel) target.level(),
                    target.getX(), target.getY(), target.getZ(),
                    java.util.Set.of(), sender.getYRot(), sender.getXRot(), true);

            target.sendSystemMessage(
                    LanguageManager.prefixed("Tpa", "tpa.success.accept_tpa_target", target, sender.getName().getString())
                            .withStyle(ChatFormatting.GREEN)
            );
            sender.sendSystemMessage(
                    LanguageManager.prefixed("Tpa", "tpa.success.accept_tpa_sender", sender, target.getName().getString())
                            .withStyle(ChatFormatting.GREEN)
            );
        }

        TpaManager.removeRequest(target.getUUID());
        return 1;
    }

    private static int denyRequest(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = ctx.getSource().getPlayerOrException();
        if (!checkEnabled(ctx.getSource(), target)) return 0;

        TpaManager.TpaRequest request = TpaManager.getRequest(target.getUUID());

        if (request == null) {
            ctx.getSource().sendFailure(
                    LanguageManager.prefixed("Tpa", "tpa.error.no_pending", target)
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        ServerPlayer sender = target.level().getServer().getPlayerList().getPlayer(request.sender);
        TpaManager.removeRequest(target.getUUID());

        target.sendSystemMessage(
                LanguageManager.prefixed("Tpa", "tpa.success.deny_target", target)
                        .withStyle(ChatFormatting.RED)
        );
        if (sender != null) {
            sender.sendSystemMessage(
                    LanguageManager.prefixed("Tpa", "tpa.info.deny_sender", sender, target.getName().getString())
                            .withStyle(ChatFormatting.RED)
            );
        }
        return 1;
    }
}