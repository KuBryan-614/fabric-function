package kuku.slab;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.List;

public class SlabMiningHandler {

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            // 只在伺服器端、蹲下時處理
            if (world.isClientSide() || !player.isCrouching()) return true;

            // 創意模式不消耗耐久、不掉落
            if (player.getAbilities().instabuild) return true;

            // 必須是半磚，且為雙層（使用 pattern matching 簡化）
            if (!(state.getBlock() instanceof SlabBlock slabBlock)) return true;
            SlabType type = state.getValue(SlabBlock.TYPE);
            if (type != SlabType.DOUBLE) return true;

            // 根據視線決定保留哪一半
            boolean lookingUp = player.getXRot() < 0;      // pitch < 0 表示向上看
            SlabType keepType = lookingUp ? SlabType.TOP : SlabType.BOTTOM;
            SlabType dropType = lookingUp ? SlabType.BOTTOM : SlabType.TOP;

            BlockState keepState = state.setValue(SlabBlock.TYPE, keepType);
            BlockState dropState = state.setValue(SlabBlock.TYPE, dropType);

            ItemStack tool = player.getMainHandItem();

            // 1. 先設定世界方塊為保留的狀態
            world.setBlock(pos, keepState, Block.UPDATE_ALL);

            // 2. 消耗工具耐久
            tool.mineBlock((ServerLevel) world, dropState, pos, player);

            // 3. 更新玩家統計 (挖掘次數)
            player.awardStat(Stats.BLOCK_MINED.get(dropState.getBlock()));

            // 4. 飢餓度消耗 (與原版破壞方塊一致)
            player.causeFoodExhaustion(0.005F);

            // 5. 掉落另一半
            List<ItemStack> drops = Block.getDrops(dropState, (ServerLevel) world, pos, blockEntity, player, tool);
            for (ItemStack drop : drops) {
                Block.popResource(world, pos, drop);
            }

            // 6. 播放方塊破壞的音效與粒子效果
            ((ServerLevel) world).levelEvent(null, 2001, pos, Block.getId(dropState));

            // 取消原版破壞
            return false;
        });
    }
}