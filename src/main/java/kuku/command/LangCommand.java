package kuku.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import kuku.lang.LanguageManager;
import kuku.lang.PlayerLanguageManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class LangCommand {

    private static final SuggestionProvider<CommandSourceStack> LANG_SUGGESTIONS = (context, builder) -> {
        for (String lang : LanguageManager.getAvailableLanguages()) {
            builder.suggest(lang);
        }
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("lang")
                .executes(LangCommand::showCurrentLang)
                .then(Commands.argument("language", StringArgumentType.word())
                        .suggests(LANG_SUGGESTIONS)
                        .executes(LangCommand::setLang))
        );
    }

    private static int showCurrentLang(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String currentLang = LanguageManager.getLanguage(player);
        String customLang = PlayerLanguageManager.getLanguage(player.getUUID());
        boolean isCustom = customLang != null;

        String key = isCustom ? "lang.status.custom" : "lang.status.client";
        String message = LanguageManager.translate(key, player, currentLang);

        ctx.getSource().sendSuccess(() ->
                Component.literal("[Function] ").withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(message).withStyle(ChatFormatting.GREEN)), false);
        return 1;
    }

    private static int setLang(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String langCode = StringArgumentType.getString(ctx, "language");

        if (!LanguageManager.isLanguageAvailable(langCode)) {
            String msg = LanguageManager.translate("lang.error.unavailable", player, langCode);
            ctx.getSource().sendFailure(
                    Component.literal("[Function] ").withStyle(ChatFormatting.GOLD)
                            .append(Component.literal(msg).withStyle(ChatFormatting.RED)));
            return 0;
        }

        PlayerLanguageManager.setLanguage(player.getUUID(), langCode);
        String msg = LanguageManager.translate("lang.success.set", player, langCode);
        ctx.getSource().sendSuccess(() ->
                Component.literal("[Function] ").withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(msg).withStyle(ChatFormatting.GREEN)), false);
        return 1;
    }
}