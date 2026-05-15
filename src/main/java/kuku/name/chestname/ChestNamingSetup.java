package kuku.name.chestname;

import kuku.mixin.name.chestname.BaseContainerBlockEntityAccessor;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.BlockHitResult;
import kuku.lang.LanguageManager;

public class ChestNamingSetup {

    public static void init() {
        UseBlockCallback.EVENT.register(ChestNamingSetup::onUseBlock);
    }

    private static InteractionResult onUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {
        // 只在伺服端處理
        if (world.isClientSide()) return InteractionResult.PASS;

        // 必須潛行 + 主手
        if (!player.isCrouching() || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

        ItemStack heldItem = player.getItemInHand(hand);
        if (!heldItem.is(Items.NAME_TAG)) return InteractionResult.PASS;

        BlockPos pos = hitResult.getBlockPos();
        BlockEntity blockEntity = world.getBlockEntity(pos);

        // 僅允許箱子、木桶、界伏盒
        if (!isAllowedContainer(blockEntity)) return InteractionResult.PASS;

        ServerPlayer serverPlayer = (ServerPlayer) player;
        Component customName = heldItem.get(DataComponents.CUSTOM_NAME);

        if (customName != null) {
            // 有命名 → 設定名稱
            String nameStr = customName.getString();
            setContainerName(blockEntity, Component.literal(nameStr));
            String msg = LanguageManager.translate("chestname.set", serverPlayer);
            msg = msg.replace("{0}", nameStr);
            serverPlayer.sendSystemMessage(
                    LanguageManager.component("prefix.function.generic", serverPlayer)
                            .append(Component.literal(msg))
            );
        } else {
            // 無命名 → 清除名稱
            setContainerName(blockEntity, null);
            String msg = LanguageManager.translate("chestname.clear", serverPlayer);
            serverPlayer.sendSystemMessage(
                    LanguageManager.component("prefix.function.generic", serverPlayer)
                            .append(Component.literal(msg))
            );
        }

        return InteractionResult.SUCCESS;
    }

    private static boolean isAllowedContainer(BlockEntity blockEntity) {
        return blockEntity instanceof ChestBlockEntity
                || blockEntity instanceof BarrelBlockEntity
                || blockEntity instanceof ShulkerBoxBlockEntity;
    }

    private static void setContainerName(BlockEntity blockEntity, Component name) {
        if (blockEntity instanceof BaseContainerBlockEntity base) {
            ((BaseContainerBlockEntityAccessor) base).setContainerName(name);
            base.setChanged(); // 標記區塊，確保存檔與客戶端同步
        }
    }
}