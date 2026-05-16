package kuku.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import kuku.blp.BLPSettings;
import kuku.lang.LanguageManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public class BLPCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("blp")
                .then(Commands.literal("peeler")
                        .executes(BLPCommand::toggle)));
    }

    private static int toggle(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean current = BLPSettings.canPeel(player.getUUID());
        BLPSettings.setCanPeel(player.getUUID(), !current);

        String key = !current ? "blp.peeler.enabled" : "blp.peeler.disabled";
        String msg = LanguageManager.translate(key, player);
        MutableComponent prefix = LanguageManager.component("prefix.function.generic", player);
        ctx.getSource().sendSuccess(() -> prefix.append(Component.literal(msg)), false);
        return 1;
    }
}