package kuku.command.demagic;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import kuku.lang.LanguageManager;

public class DemagicCommand {

    public static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();

        // 建立通用前綴
        MutableComponent prefix = LanguageManager.component("prefix.function.generic", player);

        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (stack.isEmpty()) {
            source.sendFailure(prefix.copy().append(
                    LanguageManager.component("demagic.error.no_item", player)));
            return 0;
        }

        DataComponentType<ItemEnchantments> componentType = DataComponents.ENCHANTMENTS;
        ItemEnchantments enchantments = stack.get(componentType);
        if (enchantments == null || enchantments.isEmpty()) {
            componentType = DataComponents.STORED_ENCHANTMENTS;
            enchantments = stack.get(componentType);
        }

        if (enchantments == null || enchantments.isEmpty()) {
            source.sendFailure(prefix.copy().append(
                    LanguageManager.component("demagic.error.no_enchants", player)));
            return 0;
        }

        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(enchantments);
        int before = mutable.keySet().size();
        mutable.removeIf(holder -> holder.is(EnchantmentTags.CURSE));
        int curseCount = before - mutable.keySet().size();

        if (curseCount == 0) {
            source.sendFailure(prefix.copy().append(
                    LanguageManager.component("demagic.error.no_curse", player)));
            return 0;
        }

        stack.set(componentType, mutable.toImmutable());

        String msg = LanguageManager.format(
                LanguageManager.translate("demagic.success", player), curseCount);
        source.sendSuccess(() -> prefix.copy().append(msg), false);
        return Command.SINGLE_SUCCESS;
    }
}