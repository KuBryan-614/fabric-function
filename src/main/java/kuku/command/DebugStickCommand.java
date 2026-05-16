package kuku.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import kuku.debug.DebugStickActions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DebugStickCommand {

    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();
    private static final long COOLDOWN_MS = 10 * 60 * 1000; // 10 分鐘

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("debug")
                .then(Commands.literal("function")
                        .executes(DebugStickCommand::giveDebugStick))
        );
    }

    private static int giveDebugStick(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            UUID uuid = player.getUUID();
            long now = System.currentTimeMillis();

            // 清理所有已過期的冷卻記錄，防止記憶體洩漏
            COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() <= now);

            // 檢查玩家冷卻
            if (COOLDOWNS.containsKey(uuid)) {
                long remaining = COOLDOWNS.get(uuid) - now;
                if (remaining > 0) {
                    long minutes = remaining / 60000;
                    long seconds = (remaining % 60000) / 1000;
                    ctx.getSource().sendFailure(Component.literal(
                            String.format("§c冷卻中，剩餘 %d 分 %d 秒", minutes, seconds)));
                    return 0;
                }
            }

            // 給予除錯棒並設定冷卻時間
            DebugStickActions.giveDebugStick(player);
            COOLDOWNS.put(uuid, now + COOLDOWN_MS);

            ctx.getSource().sendSuccess(() -> Component.literal("§a已給予 Function 除錯棒！"), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§c只有玩家才能使用此指令"));
            return 0;
        }
    }
}