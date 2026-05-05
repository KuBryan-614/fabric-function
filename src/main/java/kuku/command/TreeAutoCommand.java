package kuku.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import kuku.lang.LanguageManager;
import kuku.tree.TreeAutoManager;
import kuku.util.MessageDisplayManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class TreeAutoCommand {

    private static final SuggestionProvider<CommandSourceStack> ON_OFF_SUGGESTIONS =
            (context, builder) -> {
                builder.suggest("on");
                builder.suggest("off");
                return builder.buildFuture();
            };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("treeauto")
                .executes(TreeAutoCommand::queryStatus)
                .then(Commands.argument("onoff", StringArgumentType.word())
                        .suggests(ON_OFF_SUGGESTIONS)
                        .executes(TreeAutoCommand::setStatus))
        );
    }

    private static int queryStatus(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean enabled = TreeAutoManager.isAutoReplantEnabled(player.getUUID());
        String key = enabled ? "treeauto.status.enabled" : "treeauto.status.disabled";
        String msg = LanguageManager.translate(key, player);
        MessageDisplayManager.sendSystemMessage(player,
                Component.literal("[Function] ").withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(msg).withStyle(ChatFormatting.GREEN)));
        return 1;
    }

    private static int setStatus(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String arg = StringArgumentType.getString(ctx, "onoff");
        boolean enable;
        if (arg.equalsIgnoreCase("on")) {
            enable = true;
        } else if (arg.equalsIgnoreCase("off")) {
            enable = false;
        } else {
            String usage = LanguageManager.translate("treeauto.error.usage", player);
            MessageDisplayManager.sendSystemMessage(player,
                    Component.literal("[Function] ").withStyle(ChatFormatting.GOLD)
                            .append(Component.literal(usage).withStyle(ChatFormatting.RED)));
            return 0;
        }

        TreeAutoManager.setAutoReplant(player.getUUID(), enable);
        String key = enable ? "treeauto.set.on" : "treeauto.set.off";
        String msg = LanguageManager.translate(key, player);
        MessageDisplayManager.sendSystemMessage(player,
                Component.literal("[Function] ").withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(msg).withStyle(ChatFormatting.GREEN)));
        return 1;
    }
}