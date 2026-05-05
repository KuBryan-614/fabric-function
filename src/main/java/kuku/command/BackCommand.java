package kuku.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import kuku.config.BackConfig;
import kuku.back.BackManager;
import kuku.lang.LanguageManager;
import kuku.util.MessageDisplayManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class BackCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("back")
                .executes(BackCommand::teleportBack));
    }

    private static boolean checkEnabled(CommandSourceStack source, ServerPlayer player) {
        if (!BackConfig.getInstance().isEnabled()) {
            MessageDisplayManager.sendSystemMessage(player,
                    LanguageManager.prefixed("Back", "back.error.disabled", player)
                            .withStyle(ChatFormatting.RED));
            return false;
        }
        return true;
    }

    private static int teleportBack(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (!checkEnabled(ctx.getSource(), player)) return 0;

        UUID playerId = player.getUUID();

        if (BackManager.hasDeath(playerId)) {
            BackManager.LastLocation loc = BackManager.consumeDeath(playerId);
            return performBack(ctx, player, loc, "back.success.death");
        }

        if (BackManager.hasTeleport(playerId)) {
            BackManager.LastLocation loc = BackManager.consumeTeleport(playerId);
            return performBack(ctx, player, loc, "back.success.teleport");
        }

        MessageDisplayManager.sendSystemMessage(player,
                LanguageManager.prefixed("Back", "back.error.no_location", player)
                        .withStyle(ChatFormatting.RED));
        return 0;
    }

    private static int performBack(CommandContext<CommandSourceStack> ctx, ServerPlayer player,
                                   BackManager.LastLocation loc, String successKey) throws CommandSyntaxException {
        ServerLevel targetWorld = player.level().getServer().getLevel(
                ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, loc.dimensionId()));
        if (targetWorld == null) {
            MessageDisplayManager.sendSystemMessage(player,
                    LanguageManager.prefixed("Back", "back.error.world_not_found", player, loc.dimensionId().toString())
                            .withStyle(ChatFormatting.RED));
            return 0;
        }

        // 傳送前記住當前位置，實現 A↔B 無限切換
        BackManager.recordTeleport(player);
        player.teleportTo(targetWorld, loc.x(), loc.y(), loc.z(),
                java.util.Set.of(), loc.yaw(), loc.pitch(), true);

        MessageDisplayManager.sendSystemMessage(player,
                LanguageManager.prefixed("Back", successKey, player)
                        .withStyle(ChatFormatting.GREEN));
        return 1;
    }
}