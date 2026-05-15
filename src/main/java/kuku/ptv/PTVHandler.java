package kuku.ptv;

import kuku.lang.LanguageManager;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class PTVHandler {

    public static void init() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            // 只在伺服器端檢查
            if (world.isClientSide()) return InteractionResult.PASS;

            // 提前排除非玩家實體（如機器人、假玩家等）
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }

            // 只檢查村民、貓、狼
            EntityType<?> type = entity.getType();
            if (type != EntityType.VILLAGER && type != EntityType.CAT && type != EntityType.WOLF) {
                return InteractionResult.PASS;
            }

            // 如果玩家尚未啟用 /ptv，則取消攻擊
            if (!PTVSettings.canAttack(serverPlayer.getUUID())) {
                MutableComponent prefix = LanguageManager.component("prefix.function.generic", serverPlayer);
                String msg = LanguageManager.translate("ptv.protected", serverPlayer);
                serverPlayer.sendSystemMessage(prefix.append(msg));
                return InteractionResult.FAIL;
            }

            return InteractionResult.PASS; // 允許攻擊
        });
    }
}