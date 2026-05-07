package kuku.warp.gui;

import kuku.data.WarpData;
import kuku.lang.LanguageManager;
import kuku.warp.WarpManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

public class WarpDeleteConfirmMenu extends ChestMenu {
    private final ServerPlayer player;
    private final WarpData warp;

    public WarpDeleteConfirmMenu(int id, Inventory playerInv, WarpData warp) {
        super(MenuType.GENERIC_9x1, id, playerInv, new SimpleContainer(9), 1);
        this.player = (ServerPlayer) playerInv.player;
        this.warp = warp;
        init();
    }

    private void init() {
        SimpleContainer inv = (SimpleContainer) getContainer();

        // 语言本地化
        String infoName = LanguageManager.translate("warp.delete.confirm.item.info", player, warp.getName());
        String infoLore = LanguageManager.translate("warp.delete.confirm.item.owner", player, warp.getOwnerName());

        inv.setItem(3, makeItem(Items.PAPER, infoName, infoLore));
        inv.setItem(4, makeItem(Items.GREEN_CONCRETE,
                LanguageManager.translate("warp.delete.confirm.button.confirm", player), ""));
        inv.setItem(5, makeItem(Items.RED_CONCRETE,
                LanguageManager.translate("warp.delete.confirm.button.cancel", player), ""));
    }

    private ItemStack makeItem(Item item, String name, String lore) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        if (lore != null && !lore.isEmpty()) {
            stack.set(DataComponents.LORE, new ItemLore(List.of(Component.literal(lore))));
        }
        return stack;
    }

    @Override
    public void clicked(int slot, int button, ContainerInput input, Player p) {
        if (!(p instanceof ServerPlayer sp) || sp != player) return;
        if (input != ContainerInput.PICKUP) return;

        if (slot == 4) {
            sp.closeContainer();
            if (WarpManager.deleteWarp(warp.getName(), warp.getOwnerUUID())) {
                sp.sendSystemMessage(Component.literal(
                        LanguageManager.translate("warp.delete.success", sp, warp.getName())));
            } else {
                sp.sendSystemMessage(Component.literal(
                        LanguageManager.translate("warp.delete.fail", sp)));
            }
        } else if (slot == 5) {
            sp.closeContainer();
            WarpGuiHelper.openMainMenu(sp);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}