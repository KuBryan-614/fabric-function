package kuku.debug;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class DebugStickActions {
    private static final String DEBUG_TAG = "debug_stick_marker";

    public static boolean isDebugStick(ItemStack stack) {
        if (stack.getItem() != Items.STICK) return false;
        var data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().contains(DEBUG_TAG);
    }

    public static void giveDebugStick(ServerPlayer player) {
        ItemStack stick = new ItemStack(Items.STICK);
        // 防止合成标记
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(DEBUG_TAG, true);
        stick.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        stick.set(DataComponents.CUSTOM_NAME, Component.literal("Function Debug Stick"));
        stick.set(DataComponents.RARITY, net.minecraft.world.item.Rarity.EPIC);
        stick.set(DataComponents.MAX_STACK_SIZE, 1);
        if (!player.addItem(stick)) {
            player.drop(stick, false);
        }
    }

    // ---------- 映射表 ----------
    private record Action(@Nullable Property<?> left, @Nullable Property<?> right) {}

    private static final Map<Block, Action> ACTIONS = new HashMap<>();

    static {
        // 講台
        ACTIONS.put(Blocks.LECTERN, new Action(LecternBlock.FACING, LecternBlock.HAS_BOOK));
        // 地板門 (所有木質 + 鐵地板門)
        for (Block trapdoor : new Block[]{Blocks.ACACIA_TRAPDOOR, Blocks.BIRCH_TRAPDOOR, Blocks.DARK_OAK_TRAPDOOR,
                Blocks.JUNGLE_TRAPDOOR, Blocks.OAK_TRAPDOOR, Blocks.SPRUCE_TRAPDOOR,
                Blocks.CRIMSON_TRAPDOOR, Blocks.WARPED_TRAPDOOR, Blocks.MANGROVE_TRAPDOOR,
                Blocks.CHERRY_TRAPDOOR, Blocks.BAMBOO_TRAPDOOR, Blocks.IRON_TRAPDOOR}) {
            ACTIONS.put(trapdoor, new Action(TrapDoorBlock.FACING, TrapDoorBlock.OPEN));
        }
        // 日光感應器 (無左鍵，右鍵狀態)
        ACTIONS.put(Blocks.DAYLIGHT_DETECTOR, new Action(null, DaylightDetectorBlock.POWER));
        // 竹子
        ACTIONS.put(Blocks.BAMBOO, new Action(null, BambooStalkBlock.AGE));
        // 歌莱枝
        ACTIONS.put(Blocks.CHORUS_PLANT, new Action(null, ChorusPlantBlock.UP)); // 六個方向均可，这里选一個示例
        // 蘑菇方塊 (連接)
        for (Block mushroom : new Block[]{Blocks.BROWN_MUSHROOM_BLOCK, Blocks.RED_MUSHROOM_BLOCK,
                Blocks.MUSHROOM_STEM}) {
            ACTIONS.put(mushroom, new Action(null, BlockStateProperties.NORTH));
        }
        // 鐘
        ACTIONS.put(Blocks.BELL, new Action(BellBlock.FACING, BellBlock.ATTACHMENT));
        // 半磚
        for (Block slab : new Block[]{Blocks.ACACIA_SLAB, Blocks.BIRCH_SLAB, Blocks.DARK_OAK_SLAB,
                Blocks.JUNGLE_SLAB, Blocks.OAK_SLAB, Blocks.SPRUCE_SLAB,
                Blocks.CRIMSON_SLAB, Blocks.WARPED_SLAB, Blocks.MANGROVE_SLAB,
                Blocks.CHERRY_SLAB, Blocks.BAMBOO_SLAB, Blocks.STONE_SLAB,
                Blocks.COBBLESTONE_SLAB, Blocks.SMOOTH_STONE_SLAB, Blocks.SANDSTONE_SLAB,
                Blocks.CUT_SANDSTONE_SLAB, Blocks.PETRIFIED_OAK_SLAB, Blocks.COBBLED_DEEPSLATE_SLAB,
                Blocks.POLISHED_DEEPSLATE_SLAB, Blocks.DEEPSLATE_BRICK_SLAB, Blocks.DEEPSLATE_TILE_SLAB,
                Blocks.BRICK_SLAB, Blocks.MUD_BRICK_SLAB, Blocks.PRISMARINE_SLAB,
                Blocks.PRISMARINE_BRICK_SLAB, Blocks.DARK_PRISMARINE_SLAB, Blocks.NETHER_BRICK_SLAB,
                Blocks.RED_NETHER_BRICK_SLAB, Blocks.QUARTZ_SLAB, Blocks.SMOOTH_QUARTZ_SLAB,
                Blocks.PURPUR_SLAB, Blocks.POLISHED_GRANITE_SLAB, Blocks.POLISHED_DIORITE_SLAB,
                Blocks.POLISHED_ANDESITE_SLAB, Blocks.END_STONE_BRICK_SLAB, Blocks.BLACKSTONE_SLAB,
                Blocks.POLISHED_BLACKSTONE_SLAB, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB}) {
            ACTIONS.put(slab, new Action(null, SlabBlock.TYPE));
        }
        // 燈籠
        ACTIONS.put(Blocks.LANTERN, new Action(null, LanternBlock.HANGING));
        ACTIONS.put(Blocks.SOUL_LANTERN, new Action(null, LanternBlock.HANGING));
        // 紅石燈 / 紅石礦
        ACTIONS.put(Blocks.REDSTONE_LAMP, new Action(null, RedstoneLampBlock.LIT));
        ACTIONS.put(Blocks.REDSTONE_ORE, new Action(null, RedStoneOreBlock.LIT));
        ACTIONS.put(Blocks.DEEPSLATE_REDSTONE_ORE, new Action(null, RedStoneOreBlock.LIT));
        // 各種蠟燭
        for (Block candle : new Block[]{Blocks.CANDLE, Blocks.WHITE_CANDLE, Blocks.ORANGE_CANDLE, Blocks.MAGENTA_CANDLE,
                Blocks.LIGHT_BLUE_CANDLE, Blocks.YELLOW_CANDLE, Blocks.LIME_CANDLE,
                Blocks.PINK_CANDLE, Blocks.GRAY_CANDLE, Blocks.LIGHT_GRAY_CANDLE,
                Blocks.CYAN_CANDLE, Blocks.PURPLE_CANDLE, Blocks.BLUE_CANDLE,
                Blocks.BROWN_CANDLE, Blocks.GREEN_CANDLE, Blocks.RED_CANDLE,
                Blocks.BLACK_CANDLE}) {
            ACTIONS.put(candle, new Action(null, CandleBlock.LIT));
        }
        // 伏聆触媒
        ACTIONS.put(Blocks.SCULK_CATALYST, new Action(null, BlockStateProperties.BLOOM));
        // 釀造台
        ACTIONS.put(Blocks.BREWING_STAND, new Action(null, BrewingStandBlock.HAS_BOTTLE[0]));
        // 唱片机
        ACTIONS.put(Blocks.JUKEBOX, new Action(null, JukeboxBlock.HAS_RECORD));
        // 壓力板
        for (Block plate : new Block[]{Blocks.ACACIA_PRESSURE_PLATE, Blocks.BIRCH_PRESSURE_PLATE, Blocks.DARK_OAK_PRESSURE_PLATE,
                Blocks.JUNGLE_PRESSURE_PLATE, Blocks.OAK_PRESSURE_PLATE, Blocks.SPRUCE_PRESSURE_PLATE,
                Blocks.CRIMSON_PRESSURE_PLATE, Blocks.WARPED_PRESSURE_PLATE, Blocks.MANGROVE_PRESSURE_PLATE,
                Blocks.CHERRY_PRESSURE_PLATE, Blocks.BAMBOO_PRESSURE_PLATE, Blocks.STONE_PRESSURE_PLATE,
                Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE,
                Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE}) {
            ACTIONS.put(plate, new Action(null, BlockStateProperties.POWERED));
        }
        // 階梯
        for (Block stairs : new Block[]{Blocks.ACACIA_STAIRS, Blocks.BIRCH_STAIRS, Blocks.DARK_OAK_STAIRS,
                Blocks.JUNGLE_STAIRS, Blocks.OAK_STAIRS, Blocks.SPRUCE_STAIRS,
                Blocks.CRIMSON_STAIRS, Blocks.WARPED_STAIRS, Blocks.MANGROVE_STAIRS,
                Blocks.CHERRY_STAIRS, Blocks.BAMBOO_STAIRS, Blocks.STONE_STAIRS,
                Blocks.COBBLESTONE_STAIRS, Blocks.SANDSTONE_STAIRS,
                Blocks.PRISMARINE_STAIRS, Blocks.PRISMARINE_BRICK_STAIRS, Blocks.DARK_PRISMARINE_STAIRS,
                Blocks.NETHER_BRICK_STAIRS, Blocks.RED_NETHER_BRICK_STAIRS, Blocks.QUARTZ_STAIRS,
                Blocks.SMOOTH_QUARTZ_STAIRS, Blocks.PURPUR_STAIRS, Blocks.POLISHED_GRANITE_STAIRS,
                Blocks.POLISHED_DIORITE_STAIRS, Blocks.POLISHED_ANDESITE_STAIRS, Blocks.END_STONE_BRICK_STAIRS,
                Blocks.BLACKSTONE_STAIRS, Blocks.POLISHED_BLACKSTONE_STAIRS, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS}) {
            ACTIONS.put(stairs, new Action(StairBlock.FACING, StairBlock.SHAPE));
        }
        // 旗幟 / 告示牌 / 頭顱 -> 朝向 + 旋轉角度
        for (Block block : new Block[]{
                Blocks.WHITE_BANNER, Blocks.ORANGE_BANNER, Blocks.MAGENTA_BANNER,
                Blocks.LIGHT_BLUE_BANNER, Blocks.YELLOW_BANNER, Blocks.LIME_BANNER,
                Blocks.PINK_BANNER, Blocks.GRAY_BANNER, Blocks.LIGHT_GRAY_BANNER,
                Blocks.CYAN_BANNER, Blocks.PURPLE_BANNER, Blocks.BLUE_BANNER,
                Blocks.BROWN_BANNER, Blocks.GREEN_BANNER, Blocks.RED_BANNER,
                Blocks.BLACK_BANNER,
                Blocks.OAK_SIGN, Blocks.SPRUCE_SIGN, Blocks.BIRCH_SIGN,
                Blocks.JUNGLE_SIGN, Blocks.ACACIA_SIGN, Blocks.DARK_OAK_SIGN,
                Blocks.CRIMSON_SIGN, Blocks.WARPED_SIGN, Blocks.MANGROVE_SIGN,
                Blocks.CHERRY_SIGN, Blocks.BAMBOO_SIGN,
                Blocks.SKELETON_SKULL, Blocks.WITHER_SKELETON_SKULL, Blocks.PLAYER_HEAD,
                Blocks.ZOMBIE_HEAD, Blocks.CREEPER_HEAD, Blocks.PIGLIN_HEAD,
                Blocks.DRAGON_HEAD
        }) {
            // 統一使用 BlockStateProperties.ROTATION_16，技術上等價於 BannerBlock.ROTATION / SkullBlock.ROTATION
            ACTIONS.put(block, new Action(BlockStateProperties.ROTATION_16, BlockStateProperties.ROTATION_16));
        }
        // 營火 / 靈魂營火
        ACTIONS.put(Blocks.CAMPFIRE, new Action(CampfireBlock.FACING, CampfireBlock.LIT));
        ACTIONS.put(Blocks.SOUL_CAMPFIRE, new Action(CampfireBlock.FACING, CampfireBlock.LIT));
        // 熔爐系列
        ACTIONS.put(Blocks.FURNACE, new Action(FurnaceBlock.FACING, FurnaceBlock.LIT));
        ACTIONS.put(Blocks.SMOKER, new Action(SmokerBlock.FACING, SmokerBlock.LIT));
        ACTIONS.put(Blocks.BLAST_FURNACE, new Action(BlastFurnaceBlock.FACING, BlastFurnaceBlock.LIT));
        // 中繼器
        ACTIONS.put(Blocks.REPEATER, new Action(RepeaterBlock.FACING, RepeaterBlock.POWERED));
        // 比較器
        ACTIONS.put(Blocks.COMPARATOR, new Action(ComparatorBlock.FACING, ComparatorBlock.MODE));
        // 浮雕書櫃
        ACTIONS.put(Blocks.CHISELED_BOOKSHELF, new Action(ChiseledBookShelfBlock.FACING,
                ChiseledBookShelfBlock.SLOT_0_OCCUPIED));
        // 木桶
        ACTIONS.put(Blocks.BARREL, new Action(BarrelBlock.FACING, BarrelBlock.OPEN));
        // 按鈕
        for (Block btn : new Block[]{Blocks.ACACIA_BUTTON, Blocks.BIRCH_BUTTON, Blocks.DARK_OAK_BUTTON,
                Blocks.JUNGLE_BUTTON, Blocks.OAK_BUTTON, Blocks.SPRUCE_BUTTON,
                Blocks.CRIMSON_BUTTON, Blocks.WARPED_BUTTON, Blocks.MANGROVE_BUTTON,
                Blocks.CHERRY_BUTTON, Blocks.BAMBOO_BUTTON, Blocks.STONE_BUTTON,
                Blocks.POLISHED_BLACKSTONE_BUTTON}) {
            ACTIONS.put(btn, new Action(ButtonBlock.FACING, ButtonBlock.POWERED));
        }
        // 控制桿
        ACTIONS.put(Blocks.LEVER, new Action(LeverBlock.FACING, LeverBlock.POWERED));
        // 偵測器
        ACTIONS.put(Blocks.OBSERVER, new Action(ObserverBlock.FACING, ObserverBlock.POWERED));
        // 避雷針
        ACTIONS.put(Blocks.LIGHTNING_ROD, new Action(LightningRodBlock.FACING, LightningRodBlock.POWERED));
        // 鐵軌 (全部)
        for (Block rail : new Block[]{Blocks.RAIL, Blocks.POWERED_RAIL, Blocks.DETECTOR_RAIL,
                Blocks.ACTIVATOR_RAIL}) {
            ACTIONS.put(Blocks.RAIL,           new Action(BlockStateProperties.RAIL_SHAPE, null));          // 普通鐵軌
            ACTIONS.put(Blocks.POWERED_RAIL,   new Action(BlockStateProperties.RAIL_SHAPE_STRAIGHT, null)); // 動力鐵軌
            ACTIONS.put(Blocks.DETECTOR_RAIL,  new Action(BlockStateProperties.RAIL_SHAPE_STRAIGHT, null)); // 探測鐵軌
            ACTIONS.put(Blocks.ACTIVATOR_RAIL, new Action(BlockStateProperties.RAIL_SHAPE_STRAIGHT, null)); // 啟動鐵軌
        }

        // 試煉生怪磚：循環狀態 (閒置、啟動中、冷卻…)
        ACTIONS.put(Blocks.TRIAL_SPAWNER, new Action(null, TrialSpawnerBlock.STATE));
        // 險地保管箱：循環狀態
        ACTIONS.put(Blocks.VAULT, new Action(null, VaultBlock.STATE));
    // ────────── 1.21.4 蒼白橡木系列 ──────────
        // 原木、木塊、剝皮原木 (軸向)
        for (Block log : new Block[]{Blocks.PALE_OAK_LOG, Blocks.PALE_OAK_WOOD,
                Blocks.STRIPPED_PALE_OAK_LOG, Blocks.STRIPPED_PALE_OAK_WOOD}) {
            ACTIONS.put(log, new Action(RotatedPillarBlock.AXIS, null));
        }
        // 半磚
        ACTIONS.put(Blocks.PALE_OAK_SLAB, new Action(null, SlabBlock.TYPE));
        // 地板門 (朝向 + 開關)
        ACTIONS.put(Blocks.PALE_OAK_TRAPDOOR, new Action(TrapDoorBlock.FACING, TrapDoorBlock.OPEN));
        // 門 (朝向 + 開關)
        ACTIONS.put(Blocks.PALE_OAK_DOOR, new Action(DoorBlock.FACING, DoorBlock.OPEN));
        // 柵欄門 (朝向)
        ACTIONS.put(Blocks.PALE_OAK_FENCE_GATE, new Action(FenceGateBlock.FACING, null));
        // 按鈕 (朝向 + 觸發)
        ACTIONS.put(Blocks.PALE_OAK_BUTTON, new Action(ButtonBlock.FACING, ButtonBlock.POWERED));
        // 壓力板 (觸發)
        ACTIONS.put(Blocks.PALE_OAK_PRESSURE_PLATE, new Action(null, BlockStateProperties.POWERED));
        // 告示牌 (站立式，角度循環；壁掛式略過)
        ACTIONS.put(Blocks.PALE_OAK_SIGN, new Action(BlockStateProperties.ROTATION_16, BlockStateProperties.ROTATION_16));

        // ────────── 1.21 凝灰岩 / 銅系列 ──────────
        // 凝灰岩階梯
        ACTIONS.put(Blocks.TUFF_STAIRS, new Action(StairBlock.FACING, StairBlock.SHAPE));
        // 銅燈泡 (發光 + 觸發)
        ACTIONS.put(Blocks.COPPER_BULB, new Action(CopperBulbBlock.LIT, CopperBulbBlock.POWERED));
        // 銅格柵 (含水)
        ACTIONS.put(Blocks.COPPER_GRATE, new Action(null, BlockStateProperties.WATERLOGGED));
        // 蒼白苔蘚方塊（無屬性，略過）
        // 發射器 / 投擲器
        ACTIONS.put(Blocks.DISPENSER, new Action(DispenserBlock.FACING, null));
        ACTIONS.put(Blocks.DROPPER, new Action(DropperBlock.FACING, null));
        // 界伏盒
        ACTIONS.put(Blocks.SHULKER_BOX, new Action(ShulkerBoxBlock.FACING, null));
        // 柵欄門
        for (Block gate : new Block[]{Blocks.ACACIA_FENCE_GATE, Blocks.BIRCH_FENCE_GATE, Blocks.DARK_OAK_FENCE_GATE,
                Blocks.JUNGLE_FENCE_GATE, Blocks.OAK_FENCE_GATE, Blocks.SPRUCE_FENCE_GATE,
                Blocks.CRIMSON_FENCE_GATE, Blocks.WARPED_FENCE_GATE, Blocks.MANGROVE_FENCE_GATE,
                Blocks.CHERRY_FENCE_GATE, Blocks.BAMBOO_FENCE_GATE}) {
            ACTIONS.put(gate, new Action(FenceGateBlock.FACING, null));
        }
        // 切石機
        ACTIONS.put(Blocks.STONECUTTER, new Action(StonecutterBlock.FACING, null));
        // 蜂窩 / 蜂箱
        ACTIONS.put(Blocks.BEEHIVE, new Action(BeehiveBlock.FACING, null));
        ACTIONS.put(Blocks.BEE_NEST, new Action(BeehiveBlock.FACING, null));
        // 鐵砧
        ACTIONS.put(Blocks.ANVIL, new Action(AnvilBlock.FACING, null));
        ACTIONS.put(Blocks.CHIPPED_ANVIL, new Action(AnvilBlock.FACING, null));
        ACTIONS.put(Blocks.DAMAGED_ANVIL, new Action(AnvilBlock.FACING, null));
        // 終界箱
        ACTIONS.put(Blocks.ENDER_CHEST, new Action(EnderChestBlock.FACING, null));
        // 活塞 / 粘性活塞
        ACTIONS.put(Blocks.PISTON, new Action(PistonBaseBlock.FACING, null));
        ACTIONS.put(Blocks.STICKY_PISTON, new Action(PistonBaseBlock.FACING, null));
        // 終界燭
        ACTIONS.put(Blocks.END_ROD, new Action(EndRodBlock.FACING, null));
        // 校準伏聆振測器
        ACTIONS.put(Blocks.CALIBRATED_SCULK_SENSOR, new Action(CalibratedSculkSensorBlock.FACING, null));
        // 各種木頭 / 竹方塊 / 蕈柄
        for (Block log : new Block[]{Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG, Blocks.JUNGLE_LOG,
                Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG, Blocks.CRIMSON_STEM, Blocks.WARPED_STEM,
                Blocks.MANGROVE_LOG, Blocks.CHERRY_LOG, Blocks.BAMBOO_BLOCK,
                Blocks.STRIPPED_OAK_LOG, Blocks.STRIPPED_SPRUCE_LOG, Blocks.STRIPPED_BIRCH_LOG,
                Blocks.STRIPPED_JUNGLE_LOG, Blocks.STRIPPED_ACACIA_LOG, Blocks.STRIPPED_DARK_OAK_LOG,
                Blocks.STRIPPED_CRIMSON_STEM, Blocks.STRIPPED_WARPED_STEM, Blocks.STRIPPED_MANGROVE_LOG,
                Blocks.STRIPPED_CHERRY_LOG, Blocks.STRIPPED_BAMBOO_BLOCK}) {
            ACTIONS.put(log, new Action(RotatedPillarBlock.AXIS, null));
        }
        // 砂輪
        ACTIONS.put(Blocks.GRINDSTONE, new Action(GrindstoneBlock.FACING, null));
        // 漏斗
        ACTIONS.put(Blocks.HOPPER, new Action(HopperBlock.FACING, null));
        // 小儲物箱 / 小陷阱箱
        ACTIONS.put(Blocks.CHEST, new Action(ChestBlock.FACING, null));
        ACTIONS.put(Blocks.TRAPPED_CHEST, new Action(ChestBlock.FACING, null));
        // 釉陶
        for (Block glazed : new Block[]{Blocks.WHITE_GLAZED_TERRACOTTA, Blocks.ORANGE_GLAZED_TERRACOTTA,
                Blocks.MAGENTA_GLAZED_TERRACOTTA, Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA,
                Blocks.YELLOW_GLAZED_TERRACOTTA, Blocks.LIME_GLAZED_TERRACOTTA,
                Blocks.PINK_GLAZED_TERRACOTTA, Blocks.GRAY_GLAZED_TERRACOTTA,
                Blocks.LIGHT_GRAY_GLAZED_TERRACOTTA, Blocks.CYAN_GLAZED_TERRACOTTA,
                Blocks.PURPLE_GLAZED_TERRACOTTA, Blocks.BLUE_GLAZED_TERRACOTTA,
                Blocks.BROWN_GLAZED_TERRACOTTA, Blocks.GREEN_GLAZED_TERRACOTTA,
                Blocks.RED_GLAZED_TERRACOTTA, Blocks.BLACK_GLAZED_TERRACOTTA}) {
            ACTIONS.put(glazed, new Action(GlazedTerracottaBlock.FACING, null));
        }
        // 乾草捆 / 南瓜燈 / 雕刻南瓜 / 蛙光體 / 深板岩 / 玄武岩 / 平滑玄武岩 / 骨塊
        for (Block block : new Block[]{Blocks.HAY_BLOCK, Blocks.JACK_O_LANTERN, Blocks.CARVED_PUMPKIN,
                Blocks.OCHRE_FROGLIGHT, Blocks.PEARLESCENT_FROGLIGHT, Blocks.VERDANT_FROGLIGHT,
                Blocks.DEEPSLATE, Blocks.BASALT, Blocks.SMOOTH_BASALT, Blocks.BONE_BLOCK}) {
            ACTIONS.put(block, new Action(RotatedPillarBlock.AXIS, null));
        }
        // 紡織機
        ACTIONS.put(Blocks.LOOM, new Action(LoomBlock.FACING, null));
        // 梯子
        ACTIONS.put(Blocks.LADDER, new Action(LadderBlock.FACING, null));
        // 火把 (無朝向，跳過)
        // 粉瓣花 / 大懸葉草 / 小懸葉草
        ACTIONS.put(Blocks.BIG_DRIPLEAF, new Action(BigDripleafBlock.FACING, null));
        ACTIONS.put(Blocks.SMALL_DRIPLEAF, new Action(SmallDripleafBlock.FACING, null));
        // 紫水晶晶簇 / 紫水晶芽
        for (Block amethyst : new Block[]{Blocks.AMETHYST_CLUSTER, Blocks.SMALL_AMETHYST_BUD,
                Blocks.MEDIUM_AMETHYST_BUD, Blocks.LARGE_AMETHYST_BUD}) {
            ACTIONS.put(amethyst, new Action(AmethystClusterBlock.FACING, null));
        }
        // 絆線勾
        ACTIONS.put(Blocks.TRIPWIRE_HOOK, new Action(TripWireHookBlock.FACING, null));
        // 淤泥紅樹林木根
        ACTIONS.put(Blocks.MUDDY_MANGROVE_ROOTS, new Action(RotatedPillarBlock.AXIS, null));
        // 紫珀柱 / 石英柱
        ACTIONS.put(Blocks.PURPUR_PILLAR, new Action(RotatedPillarBlock.AXIS, null));
        ACTIONS.put(Blocks.QUARTZ_PILLAR, new Action(RotatedPillarBlock.AXIS, null));
        // 可可豆
        ACTIONS.put(Blocks.COCOA, new Action(CocoaBlock.FACING, null));
    }

    public static void handleLeftClick(ServerPlayer player, BlockPos pos) {
        BlockState state = player.level().getBlockState(pos);
        Action action = ACTIONS.get(state.getBlock());
        if (action != null && action.left != null) {
            cycleProperty(player, pos, state, action.left);
        }
    }

    public static void handleRightClick(ServerPlayer player, BlockPos pos) {
        BlockState state = player.level().getBlockState(pos);
        Action action = ACTIONS.get(state.getBlock());
        if (action != null && action.right != null) {
            cycleProperty(player, pos, state, action.right);
        }
    }

    private static void cycleProperty(ServerPlayer player, BlockPos pos, BlockState state, Property<?> property) {
        BlockState newState = state.cycle(property);
        player.level().setBlock(pos, newState, Block.UPDATE_ALL);
    }

    // 註冊右鍵回調
    public static void init() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            // 只在伺服器端執行右鍵功能（SUCCESS 在客戶端也已阻止 GUI，但此處明確劃分責任）
            if (world.isClientSide()) return InteractionResult.PASS;

            ItemStack held = player.getItemInHand(hand);
            if (isDebugStick(held)) {
                if (player instanceof ServerPlayer sp) {
                    handleRightClick(sp, hitResult.getBlockPos());
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
    }
}