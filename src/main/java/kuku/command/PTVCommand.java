package kuku.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import kuku.lang.LanguageManager;
import kuku.ptv.PTVSettings;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public class PTVCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ptv")
                .executes(PTVCommand::toggle));
    }

    private static int toggle(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean current = PTVSettings.canAttack(player.getUUID());
        PTVSettings.setCanAttack(player.getUUID(), !current);

        String key = !current ? "ptv.enabled" : "ptv.disabled";
        String msg = LanguageManager.translate(key, player);
        MutableComponent prefix = LanguageManager.component("prefix.function.generic", player);
        ctx.getSource().sendSuccess(() -> prefix.append(Component.literal(msg)), false);
        return 1;
    }
}