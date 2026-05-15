package kuku.blp;

import kuku.lang.LanguageManager;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class BLPHandler {

    public static void init() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            // 只在伺服器端處理
            if (world.isClientSide()) return InteractionResult.PASS;

            // 只處理主手，避免副手重複觸發
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

            // 必須是 ServerPlayer
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

            ItemStack held = player.getItemInHand(hand);
            // 只檢查斧頭
            if (!(held.getItem() instanceof AxeItem)) return InteractionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);

            // 只有當方塊在可剝皮映射表中才攔截（未剝皮原木）
            if (!AxeItem.STRIPPABLES.containsKey(state.getBlock())) return InteractionResult.PASS;

            // 若玩家未啟用剝皮，拒絕並提示
            if (!BLPSettings.canPeel(serverPlayer.getUUID())) {
                MutableComponent prefix = LanguageManager.component("prefix.function.generic", serverPlayer);
                String msg = LanguageManager.translate("blp.protected", serverPlayer);
                serverPlayer.sendSystemMessage(prefix.append(msg));
                return InteractionResult.FAIL;  // 取消剝皮
            }

            return InteractionResult.PASS;      // 允許剝皮
        });
    }
}