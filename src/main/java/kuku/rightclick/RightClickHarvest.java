package kuku.rightclick;

import kuku.config.RightClickConfig;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RightClickHarvest {

    // 作物 → 種子對照（無反射，跨映射安全）
    private static final Map<Block, Item> CROP_SEED_MAP = Map.of(
            Blocks.WHEAT,       Items.WHEAT_SEEDS,
            Blocks.CARROTS,     Items.CARROT,
            Blocks.POTATOES,    Items.POTATO,
            Blocks.BEETROOTS,   Items.BEETROOT_SEEDS
    );

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!RightClickConfig.getInstance().isEnabled()) return InteractionResult.PASS;
            if (world.isClientSide()) return InteractionResult.PASS;
            if (player.isCrouching()) return InteractionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);
            Block block = state.getBlock();

            IntegerProperty ageProperty = null;
            int maxAge;
            int currentAge;
            Item seedItem = null;

            // 判斷作物類型
            if (block instanceof CropBlock crop) {
                ageProperty = getAgeProperty(crop);      // 保留反射取得正確年齡屬性
                maxAge = crop.getMaxAge();
                currentAge = state.getValue(ageProperty);
                seedItem = CROP_SEED_MAP.get(block);      // 無反射種子對應
            } else if (block instanceof CocoaBlock) {
                ageProperty = CocoaBlock.AGE;
                maxAge = 2;
                currentAge = state.getValue(ageProperty);
                seedItem = Items.COCOA_BEANS;
            } else if (block instanceof NetherWartBlock) {
                ageProperty = NetherWartBlock.AGE;
                maxAge = 3;
                currentAge = state.getValue(ageProperty);
                seedItem = Items.NETHER_WART;
            } else if (block instanceof SweetBerryBushBlock) {
                ageProperty = SweetBerryBushBlock.AGE;
                maxAge = 2;                              // 年齡 2 即可採收
                currentAge = state.getValue(ageProperty);
                seedItem = Items.SWEET_BERRIES;
            } else if (block instanceof CaveVinesBlock || block instanceof CaveVinesPlantBlock) {
                return handleBerries(world, pos, state);
            } else {
                return InteractionResult.PASS;
            }

            if (currentAge < maxAge) return InteractionResult.PASS;

            // 收穫成熟作物
            List<ItemStack> drops = Block.getDrops(state, (ServerLevel) world, pos, world.getBlockEntity(pos), player, ItemStack.EMPTY);
            List<ItemStack> finalDrops = new ArrayList<>(drops);

            // 扣除一顆種植材料（模擬重種）
            if (seedItem != null) {
                for (int i = 0; i < finalDrops.size(); i++) {
                    ItemStack stack = finalDrops.get(i);
                    if (stack.is(seedItem)) {
                        stack.shrink(1);
                        if (stack.isEmpty()) finalDrops.remove(i);
                        break;
                    }
                }
            }

            for (ItemStack drop : finalDrops) {
                Block.popResource(world, pos, drop);
            }

            world.levelEvent(2001, pos, Block.getId(state));
            world.setBlock(pos, state.setValue(ageProperty, 0), 3);

            return InteractionResult.CONSUME;
        });
    }

    // 保留反射取得年齡屬性（甜菜根等需要）
    private static IntegerProperty getAgeProperty(CropBlock crop) {
        try {
            java.lang.reflect.Method method = CropBlock.class.getDeclaredMethod("getAgeProperty");
            method.setAccessible(true);
            return (IntegerProperty) method.invoke(crop);
        } catch (Exception e) {
            return CropBlock.AGE;  // 安全降級
        }
    }

    // ✅ 簡化螢光莓處理（兩個 Blocks 的 BERRIES 是同一個屬性）
    private static InteractionResult handleBerries(net.minecraft.world.level.Level world, BlockPos pos, BlockState state) {
        // 檢查是否有漿果（使用 CaveVinesPlantBlock.BERRIES 即可，兩者共用）
        if (state.hasProperty(CaveVinesPlantBlock.BERRIES)) {
            boolean hasBerries = state.getValue(CaveVinesPlantBlock.BERRIES);
            if (!hasBerries) return InteractionResult.PASS;

            Block.popResource(world, pos, new ItemStack(Items.GLOW_BERRIES));
            world.levelEvent(2001, pos, Block.getId(state));
            // 設定漿果為 false
            world.setBlock(pos, state.setValue(CaveVinesPlantBlock.BERRIES, false), 3);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;   // 理論上不會進入，因為前面已判斷是 CaveVines
    }
}