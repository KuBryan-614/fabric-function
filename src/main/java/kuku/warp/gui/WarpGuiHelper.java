package kuku.warp.gui;

import kuku.back.BackManager;
import kuku.data.WarpData;
import kuku.lang.LanguageManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

public class WarpGuiHelper {
    public static void openMainMenu(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new WarpListMenu(id, inv, (ServerPlayer) p),
                Component.literal(LanguageManager.translate("warp.gui.title.main_menu", player))
        ));
    }

    public static void openModifyMenu(ServerPlayer player, WarpData warp) {
        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new WarpModifyMenu(id, inv, warp),
                Component.literal(LanguageManager.translate("warp.gui.title.modify_menu", player))
        ));
    }

    public static void openDeleteConfirm(ServerPlayer player, WarpData warp) {
        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new WarpDeleteConfirmMenu(id, inv, warp),
                Component.literal(LanguageManager.translate("warp.gui.title.delete_confirm", player))
        ));
    }

    public static void teleportToWarp(ServerPlayer player, WarpData warp) {
        ResourceKey<net.minecraft.world.level.Level> dimKey = ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION, warp.getDimensionId());
        net.minecraft.server.level.ServerLevel world = player.level().getServer().getLevel(dimKey);
        if (world == null) {
            player.sendSystemMessage(Component.literal(
                    LanguageManager.translate("warp.teleport.world_not_found", player)));
            return;
        }
        BackManager.recordTeleport(player);
        player.teleportTo(world, warp.getPos().getX() + 0.5, warp.getPos().getY(), warp.getPos().getZ() + 0.5,
                java.util.Set.of(), player.getYRot(), player.getXRot(), true);
        player.sendSystemMessage(Component.literal(
                LanguageManager.translate("warp.teleport.success", player, warp.getName())));
    }
}