package kuku.dli;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import kuku.lang.LanguageManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public class DliCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dli")
                .executes(DliCommand::toggle));
    }

    private static int toggle(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean current = DliSettings.isEnabled(player.getUUID());
        DliSettings.setEnabled(player.getUUID(), !current);
        String key = !current ? "dli.enabled" : "dli.disabled";
        String msg = LanguageManager.translate(key, player);
        MutableComponent prefix = LanguageManager.component("prefix.function.generic", player);
        ctx.getSource().sendSuccess(() -> prefix.append(Component.literal(msg)), false);
        return 1;
    }
}