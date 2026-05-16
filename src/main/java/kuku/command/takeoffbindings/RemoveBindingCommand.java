package kuku.command.takeoffbindings;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import kuku.lang.LanguageManager;

import java.util.*;

public class RemoveBindingCommand {

    private enum Target {
        ALL(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET),
        HEAD(EquipmentSlot.HEAD),
        CHEST(EquipmentSlot.CHEST),
        LEGS(EquipmentSlot.LEGS),
        FEET(EquipmentSlot.FEET);

        final List<EquipmentSlot> slots;

        Target(EquipmentSlot... slots) {
            this.slots = List.of(slots);
        }
    }

    private static final Map<String, Target> TARGET_MAP = new HashMap<>();

    static {
        TARGET_MAP.put("all", Target.ALL);
        TARGET_MAP.put("head",   Target.HEAD);
        TARGET_MAP.put("chest",  Target.CHEST);
        TARGET_MAP.put("legs",   Target.LEGS);
        TARGET_MAP.put("feet",   Target.FEET);

        TARGET_MAP.put("頭盔", Target.HEAD);
        TARGET_MAP.put("胸甲", Target.CHEST);
        TARGET_MAP.put("護腿", Target.LEGS);
        TARGET_MAP.put("靴子", Target.FEET);
    }

    public static void register(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                net.minecraft.commands.Commands.literal("removebinding")
                        .then(net.minecraft.commands.Commands.argument("target", StringArgumentType.word())
                                .executes(RemoveBindingCommand::execute))
        );
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String input = StringArgumentType.getString(ctx, "target").toLowerCase(Locale.ROOT);
        Target target = TARGET_MAP.get(input);
        if (target == null) {
            ctx.getSource().sendFailure(
                    LanguageManager.component("prefix.function.generic", player)
                            .append(LanguageManager.component("removebinding.error.invalid_target", player))
            );
            return 0;
        }

        List<ItemStack> removed = new ArrayList<>();

        for (EquipmentSlot slot : target.slots) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;

            ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
            if (enchantments == null) continue;

            boolean hasBinding = false;
            for (Holder<Enchantment> holder : enchantments.keySet()) {
                if (holder.is(Enchantments.BINDING_CURSE)) {
                    hasBinding = true;
                    break;
                }
            }
            if (!hasBinding) continue;

            // 保存副本以備成功訊息使用
            ItemStack copyForMessage = stack.copy();

            // 從裝備欄取下
            player.setItemSlot(slot, ItemStack.EMPTY);
            // 嘗試放入背包（若背包滿則自動掉落）
            player.addItem(stack);

            removed.add(copyForMessage);
        }

        if (removed.isEmpty()) {
            ctx.getSource().sendFailure(
                    LanguageManager.component("prefix.function.generic", player)
                            .append(LanguageManager.component("removebinding.error.no_item", player))
            );
            return 0;
        }

        // 組合成功訊息
        MutableComponent prefix = LanguageManager.component("prefix.function.generic", player);
        String itemNames = removed.stream()
                .map(ItemStack::getHoverName)
                .map(Component::getString)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        String pattern = LanguageManager.translate("removebinding.success", player);
        String finalMsg = pattern.replace("{0}", itemNames);
        ctx.getSource().sendSuccess(() -> prefix.append(finalMsg), false);
        return Command.SINGLE_SUCCESS;
    }
}