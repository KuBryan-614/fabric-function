package kuku.modslash;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import kuku.lang.LanguageManager;
import kuku.mixin.modslash.ServerCommonPacketListenerImplAccessor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

public class ModslashCommands {

    public static void register(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher) {
        // /mods – 顯示 Function 模組功能清單
        dispatcher.register(Commands.literal("mods")
                .executes(ModslashCommands::showModInfo));

        // /tps
        dispatcher.register(Commands.literal("tps")
                .executes(ModslashCommands::showTps));

        // /mspt
        dispatcher.register(Commands.literal("mspt")
                .executes(ModslashCommands::showMspt));

        // /ping
        dispatcher.register(Commands.literal("ping")
                .executes(ModslashCommands::showPing));

        // /nc [x] [z]
        dispatcher.register(Commands.literal("nc")
                .executes(ctx -> netherCoord(ctx, null, null))
                .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                .executes(ctx -> {
                                    double x = DoubleArgumentType.getDouble(ctx, "x");
                                    double z = DoubleArgumentType.getDouble(ctx, "z");
                                    return netherCoord(ctx, x, z);
                                })
                        )
                )
        );

        // /rct
        dispatcher.register(Commands.literal("rct")
                .executes(ModslashCommands::showRepairCost));
    }

    // /mods 實作 – 顯示模組功能清單
    private static int showModInfo(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = null;
        try { player = ctx.getSource().getPlayerOrException(); } catch (CommandSyntaxException ignored) {}

        String helpText = LanguageManager.translate("modslash.mods.help", player);
        MutableComponent prefix = LanguageManager.component("prefix.function.generic", player);
        ctx.getSource().sendSuccess(() -> prefix.append(Component.literal(helpText)), false);
        return Command.SINGLE_SUCCESS;
    }

    // /tps 實作
    private static int showTps(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = null;
        try { player = ctx.getSource().getPlayerOrException(); }
        catch (CommandSyntaxException ignored) {}

        MinecraftServer server = ctx.getSource().getServer();
        long[] times = TickTracker.TICK_TIMES;
        double mspt = Arrays.stream(times).average().orElse(0) / 1_000_000.0;
        double tps  = Math.min(1000.0 / mspt, 20.0);

        final String msg = LanguageManager.translate("modslash.tps", player,
                String.format("%.1f", tps), String.format("%.2f", mspt));
        MutableComponent prefix = LanguageManager.component("prefix.function.generic", player);
        ctx.getSource().sendSuccess(() -> prefix.append(Component.literal(msg)), false);
        return Command.SINGLE_SUCCESS;
    }

    // /mspt 實作
    private static int showMspt(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = null;
        try { player = ctx.getSource().getPlayerOrException(); }
        catch (CommandSyntaxException ignored) {}

        MinecraftServer server = ctx.getSource().getServer();
        long[] times = TickTracker.TICK_TIMES;
        double mspt  = Arrays.stream(times).average().orElse(0) / 1_000_000.0;

        final String msg = LanguageManager.translate("modslash.mspt", player,
                String.format("%.2f", mspt));
        MutableComponent prefix = LanguageManager.component("prefix.function.generic", player);
        ctx.getSource().sendSuccess(() -> prefix.append(Component.literal(msg)), false);
        return Command.SINGLE_SUCCESS;
    }

    // /ping 實作
    private static int showPing(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int latency = ((ServerCommonPacketListenerImplAccessor) player.connection).getLatency();

        final String msg = LanguageManager.translate("modslash.ping", player,
                String.valueOf(latency));
        MutableComponent prefix = LanguageManager.component("prefix.function.generic", player);
        ctx.getSource().sendSuccess(() -> prefix.append(Component.literal(msg)), false);
        return Command.SINGLE_SUCCESS;
    }

    // /nc 實作
    private static int netherCoord(CommandContext<CommandSourceStack> ctx, Double xIn, Double zIn) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        double x, z;
        if (xIn == null || zIn == null) {
            x = player.getX();
            z = player.getZ();
        } else {
            x = xIn;
            z = zIn;
        }
        double netherX = x / 8.0;
        double netherZ = z / 8.0;
        String pattern = LanguageManager.translate("modslash.nc", player);
        String msg = pattern.replace("{0}", String.format("%.2f", x))
                .replace("{1}", String.format("%.2f", z))
                .replace("{2}", String.format("%.2f", netherX))
                .replace("{3}", String.format("%.2f", netherZ));
        MutableComponent prefix = LanguageManager.component("prefix.function.generic", player);
        MutableComponent result = prefix.append(msg);   // 先組合完成
        ctx.getSource().sendSuccess(() -> result, false);
        return Command.SINGLE_SUCCESS;
    }

    // /rct 實作
    private static int showRepairCost(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        int cost = 0;
        if (!stack.isEmpty()) {
            Integer repairCost = stack.get(DataComponents.REPAIR_COST);
            if (repairCost != null) cost = repairCost;
        }
        String pattern = LanguageManager.translate("modslash.rct", player);
        String msg = pattern.replace("{0}", String.valueOf(cost));
        MutableComponent prefix = LanguageManager.component("prefix.function.generic", player);
        MutableComponent result = prefix.append(msg);   // 先組合完成
        ctx.getSource().sendSuccess(() -> result, false);
        return Command.SINGLE_SUCCESS;
    }
}