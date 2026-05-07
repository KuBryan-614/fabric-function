package kuku.warp.gui;

import kuku.config.WarpConfig;
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

import java.util.ArrayList;
import java.util.List;

public class WarpListMenu extends ChestMenu {

    private final ServerPlayer player;
    private int page = 0;
    private static final int WARPS_PER_PAGE = 45;
    private List<WarpData> warps;
    private final SimpleContainer inventory;

    public WarpListMenu(int containerId, Inventory playerInv, ServerPlayer player) {
        super(MenuType.GENERIC_9x6, containerId, playerInv, new SimpleContainer(54), 6);
        this.player = player;
        this.inventory = (SimpleContainer) getContainer();
        this.warps = new ArrayList<>(WarpManager.getAllWarps());
        refresh();
    }

    private void refresh() {
        for (int i = 0; i < 54; i++) inventory.setItem(i, ItemStack.EMPTY);

        int start = page * WARPS_PER_PAGE;
        int end = Math.min(start + WARPS_PER_PAGE, warps.size());

        for (int i = 0; i < 45; i++) {
            int idx = start + i;
            if (idx < end) {
                inventory.setItem(i, createWarpIcon(warps.get(idx)));
            }
        }

        // 上一页按钮 (格子 47)
        if (page > 0) {
            inventory.setItem(47, makeButton(Items.SPECTRAL_ARROW,
                    LanguageManager.translate("warp.list.button.prev_page", player),
                    LanguageManager.translate("warp.list.button.prev_page.lore", player)));
        } else {
            inventory.setItem(47, placeholder());
        }

        // 玩家传送点信息 (格子 48)
        long owned = WarpManager.getWarpsByOwner(player.getUUID()).size();
        int max = WarpConfig.getInstance().getMaxWarps();
        ItemStack info = new ItemStack(Items.KNOWLEDGE_BOOK);
        info.set(DataComponents.CUSTOM_NAME, Component.literal(
                LanguageManager.translate("warp.list.info.player_warps", player)));
        addLore(info, LanguageManager.translate("warp.list.info.player_warps.lore", player, owned, max));
        inventory.setItem(48, info);

        // 刷新按钮 (格子 49)
        inventory.setItem(49, makeButton(Items.CLOCK,
                LanguageManager.translate("warp.list.button.refresh", player),
                LanguageManager.translate("warp.list.button.refresh.lore", player)));

        // 下一页按钮 (格子 50)
        if ((page + 1) * WARPS_PER_PAGE < warps.size()) {
            inventory.setItem(50, makeButton(Items.SPECTRAL_ARROW,
                    LanguageManager.translate("warp.list.button.next_page", player),
                    LanguageManager.translate("warp.list.button.next_page.lore", player)));
        } else {
            inventory.setItem(50, placeholder());
        }

        // 关闭按钮 (格子 53)
        inventory.setItem(53, makeButton(Items.BARRIER,
                LanguageManager.translate("warp.list.button.close", player),
                LanguageManager.translate("warp.list.button.close.lore", player)));
    }

    private ItemStack createWarpIcon(WarpData warp) {
        ItemStack stack;
        String iconId = warp.getIconItemId();
        if (iconId != null) {
            Identifier rl = Identifier.tryParse(iconId);
            if (rl != null) {
                stack = BuiltInRegistries.ITEM.getOptional(rl)
                        .map(ItemStack::new)
                        .orElse(null);
                if (stack == null) {
                    stack = BuiltInRegistries.BLOCK.getOptional(rl)
                            .map(block -> new ItemStack(block.asItem()))
                            .orElse(new ItemStack(Items.COMPASS));
                }
            } else {
                stack = new ItemStack(Items.COMPASS);
            }
        } else {
            stack = new ItemStack(Items.COMPASS);
        }

        // 设置物品名为传送点名称
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("§b" + warp.getName()));

        // 多语言化的 lore，根据是否所有者决定显示右键编辑提示
        boolean isOwner = warp.getOwnerUUID().equals(player.getUUID());

        List<String> loreLines = new ArrayList<>();
        loreLines.add(LanguageManager.translate("warp.list.lore.left_click", player, warp.getName()));
        if (isOwner) {
            loreLines.add(LanguageManager.translate("warp.list.lore.right_click", player));
        }
        loreLines.add(LanguageManager.translate("warp.list.lore.separator", player));
        loreLines.add(LanguageManager.translate("warp.list.lore.owner", player, warp.getOwnerName()));
        loreLines.add(LanguageManager.translate("warp.list.lore.created", player,
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(warp.getCreationTime()))));
        loreLines.add(LanguageManager.translate("warp.list.lore.dimension", player, warp.getDimensionId().toString()));
        loreLines.add(LanguageManager.translate("warp.list.lore.position", player, warp.getPos().toShortString()));

        addLore(stack, loreLines.toArray(new String[0]));
        return stack;
    }

    private ItemStack makeButton(Item item, String name, String lore) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        if (lore != null) addLore(stack, lore);
        return stack;
    }

    private ItemStack placeholder() {
        ItemStack stack = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(
                LanguageManager.translate("warp.list.placeholder", player)));
        return stack;
    }

    private void addLore(ItemStack stack, String... lines) {
        List<Component> loreList = new ArrayList<>();
        for (String line : lines) {
            loreList.add(Component.literal(line));
        }
        stack.set(DataComponents.LORE, new ItemLore(loreList));
    }

    @Override
    public void clicked(int slot, int button, ContainerInput input, Player player) {
        if (!(player instanceof ServerPlayer sp) || sp != this.player) return;
        if (input != ContainerInput.PICKUP && input != ContainerInput.CLONE) return;
        if (slot < 0 || slot > 53) return;

        // 功能按钮：只响应左键
        if (slot == 47 || slot == 49 || slot == 50 || slot == 53) {
            if (button != 0) return;
            if (slot == 47 && page > 0) { page--; refresh(); return; }
            if (slot == 49) {
                warps = new ArrayList<>(WarpManager.getAllWarps());
                int maxPage = warps.isEmpty() ? 0 : (warps.size() - 1) / WARPS_PER_PAGE;
                page = Math.min(page, maxPage);
                refresh();
                return;
            }
            if (slot == 50 && (page + 1) * WARPS_PER_PAGE < warps.size()) { page++; refresh(); return; }
            if (slot == 53) { sp.closeContainer(); return; }
            return;
        }

        // 传送点格 (0-44)
        if (slot < 45) {
            int idx = page * WARPS_PER_PAGE + slot;
            if (idx >= warps.size()) return;
            WarpData warp = warps.get(idx);
            boolean isOwner = warp.getOwnerUUID().equals(sp.getUUID());

            if (button == 0) {                  // 左键传送
                WarpGuiHelper.teleportToWarp(sp, warp);
                sp.closeContainer();
            } else if (button == 1 && isOwner) { // 右键打开修改菜单
                WarpGuiHelper.openModifyMenu(sp, warp);
            }
        }
    }

    @Override
    public boolean stillValid(Player player) { return true; }
}