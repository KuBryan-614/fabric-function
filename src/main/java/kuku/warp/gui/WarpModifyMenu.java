package kuku.warp.gui;

import kuku.data.WarpData;
import kuku.lang.LanguageManager;
import kuku.warp.WarpManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

public class WarpModifyMenu extends ChestMenu {
    private final ServerPlayer player;
    private final WarpData warp;

    public WarpModifyMenu(int id, Inventory playerInv, WarpData warp) {
        super(MenuType.GENERIC_9x1, id, playerInv, new SimpleContainer(9), 1);
        this.player = (ServerPlayer) playerInv.player;
        this.warp = warp;
        init();
    }

    private void init() {
        setButton(0, Items.NAME_TAG,
                LanguageManager.translate("warp.modify.button.rename", player),
                LanguageManager.translate("warp.modify.button.rename.lore", player));
        setButton(1, Items.CHEST,
                LanguageManager.translate("warp.modify.button.icon_hand", player),
                LanguageManager.translate("warp.modify.button.icon_hand.lore", player));
        setButton(2, Items.GRASS_BLOCK,
                LanguageManager.translate("warp.modify.button.icon_ground", player),
                LanguageManager.translate("warp.modify.button.icon_ground.lore", player));
        setButton(7, Items.BARRIER,
                LanguageManager.translate("warp.modify.button.delete", player),
                LanguageManager.translate("warp.modify.button.delete.lore", player));
        setButton(8, Items.SPECTRAL_ARROW,
                LanguageManager.translate("warp.modify.button.back", player),
                LanguageManager.translate("warp.modify.button.back.lore", player));
    }

    private void setButton(int slot, Item item, String name, String lore) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        if (lore != null) {
            stack.set(DataComponents.LORE, new ItemLore(List.of(Component.literal(lore))));
        }
        getContainer().setItem(slot, stack);
    }

    @Override
    public void clicked(int slot, int button, ContainerInput input, Player p) {
        if (!(p instanceof ServerPlayer sp) || sp != player) return;
        if (input != ContainerInput.PICKUP) return;

        if (slot == 0) {
            sp.closeContainer();
            RenameSessionManager.startRename(sp, warp);   // 方法名与你的 RenameSessionManager 保持一致
        } else if (slot == 1) {
            ItemStack handItem = sp.getMainHandItem();
            if (handItem.isEmpty()) {
                sp.sendSystemMessage(Component.literal(
                        LanguageManager.translate("warp.modify.icon_hand.empty", sp)));
                return;
            }
            Identifier itemId = BuiltInRegistries.ITEM.getKey(handItem.getItem());
            warp.setIconItemId(itemId.toString());
            WarpManager.scheduleSave();   // ← 新增：觸發存檔
            sp.sendSystemMessage(Component.literal(
                    LanguageManager.translate("warp.modify.icon_hand.success", sp,
                            handItem.getHoverName().getString())));
            sp.closeContainer();
            WarpGuiHelper.openMainMenu(sp);
        } else if (slot == 2) {
            net.minecraft.core.BlockPos pos = sp.blockPosition().below();
            net.minecraft.world.level.block.state.BlockState state = sp.level().getBlockState(pos);
            if (state.isAir()) {
                sp.sendSystemMessage(Component.literal(
                        LanguageManager.translate("warp.modify.icon_ground.air", sp)));
                return;
            }
            Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            warp.setIconItemId(blockId.toString());
            WarpManager.scheduleSave();   // ← 新增：觸發存檔
            sp.sendSystemMessage(Component.literal(
                    LanguageManager.translate("warp.modify.icon_ground.success", sp)));
            sp.closeContainer();
            WarpGuiHelper.openMainMenu(sp);
        } else if (slot == 7) {
            // 删除确认
            sp.closeContainer();
            WarpGuiHelper.openDeleteConfirm(sp, warp);
        } else if (slot == 8) {
            // 返回主菜单
            sp.closeContainer();
            WarpGuiHelper.openMainMenu(sp);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}