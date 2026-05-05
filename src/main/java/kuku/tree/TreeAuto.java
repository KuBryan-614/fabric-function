package kuku.tree;

import kuku.lang.LanguageManager;
import kuku.util.MessageDisplayManager;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TreeAuto {

    // -------------------------------------------------------
    // 樹苗對應表
    // -------------------------------------------------------
    private static final Map<Block, Block> LOG_TO_SAPLING = new HashMap<>();

    static {
        putAll(Blocks.OAK_SAPLING,
                Blocks.OAK_LOG, Blocks.OAK_WOOD,
                Blocks.STRIPPED_OAK_LOG, Blocks.STRIPPED_OAK_WOOD);
        putAll(Blocks.SPRUCE_SAPLING,
                Blocks.SPRUCE_LOG, Blocks.SPRUCE_WOOD,
                Blocks.STRIPPED_SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_WOOD);
        putAll(Blocks.BIRCH_SAPLING,
                Blocks.BIRCH_LOG, Blocks.BIRCH_WOOD,
                Blocks.STRIPPED_BIRCH_LOG, Blocks.STRIPPED_BIRCH_WOOD);
        putAll(Blocks.JUNGLE_SAPLING,
                Blocks.JUNGLE_LOG, Blocks.JUNGLE_WOOD,
                Blocks.STRIPPED_JUNGLE_LOG, Blocks.STRIPPED_JUNGLE_WOOD);
        putAll(Blocks.ACACIA_SAPLING,
                Blocks.ACACIA_LOG, Blocks.ACACIA_WOOD,
                Blocks.STRIPPED_ACACIA_LOG, Blocks.STRIPPED_ACACIA_WOOD);
        putAll(Blocks.DARK_OAK_SAPLING,
                Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_WOOD,
                Blocks.STRIPPED_DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_WOOD);
        putAll(Blocks.MANGROVE_PROPAGULE,
                Blocks.MANGROVE_LOG, Blocks.MANGROVE_WOOD,
                Blocks.STRIPPED_MANGROVE_LOG, Blocks.STRIPPED_MANGROVE_WOOD);
        putAll(Blocks.CHERRY_SAPLING,
                Blocks.CHERRY_LOG, Blocks.CHERRY_WOOD,
                Blocks.STRIPPED_CHERRY_LOG, Blocks.STRIPPED_CHERRY_WOOD);
        putAll(Blocks.CRIMSON_FUNGUS,
                Blocks.CRIMSON_STEM, Blocks.CRIMSON_HYPHAE,
                Blocks.STRIPPED_CRIMSON_STEM, Blocks.STRIPPED_CRIMSON_HYPHAE);
        putAll(Blocks.WARPED_FUNGUS,
                Blocks.WARPED_STEM, Blocks.WARPED_HYPHAE,
                Blocks.STRIPPED_WARPED_STEM, Blocks.STRIPPED_WARPED_HYPHAE);
    }

    private static void putAll(Block sapling, Block... logs) {
        for (Block log : logs) LOG_TO_SAPLING.put(log, sapling);
    }

    // -------------------------------------------------------
    // BEFORE 暫存：砍伐位置 → (連通原木集合, 樹苗種類)
    // -------------------------------------------------------
    private record PendingTree(Set<BlockPos> logs, Block sapling) {}
    private static final Map<BlockPos, PendingTree> PENDING = new ConcurrentHashMap<>();

    // -------------------------------------------------------
    // 疊加訊息用計數器
    // -------------------------------------------------------
    private static final Map<UUID, Integer> replantCount    = new ConcurrentHashMap<>();
    private static final Map<UUID, Long>    lastReplantTime = new ConcurrentHashMap<>();
    /** 超過此時間視為新一輪種植，計數歸一 */
    private static final long MESSAGE_EXPIRE_MS = 5_000L;

    // -------------------------------------------------------
    // 搜尋半徑常數
    // -------------------------------------------------------
    /** XZ 搜尋半徑（格）—— 涵蓋巨型橡木 / 紅樹等寬大樹形 */
    private static final int SEARCH_RADIUS = 10;
    /** 向上搜尋高度 */
    private static final int SEARCH_UP     = 30;
    /** 向下搜尋深度 */
    private static final int SEARCH_DOWN   = 30;
    /** 向下掃描種植點的最大距離 */
    private static final int PLANT_SCAN    = 32;

    // -------------------------------------------------------
    // 公開 API
    // -------------------------------------------------------

    public static void register() {

        // BEFORE：記錄此位置的連通原木集合與樹苗種類
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (world.isClientSide()) return true;
            if (!state.is(BlockTags.LOGS)) return true;

            Block sapling = LOG_TO_SAPLING.get(state.getBlock());
            if (sapling == null) return true;

            Set<BlockPos> connected = findConnectedLogs(world, pos);
            PENDING.put(pos.immutable(), new PendingTree(connected, sapling));
            return true;
        });

        // AFTER：判斷整棵樹是否已砍完，若是則重新種植
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world.isClientSide()) return;

            PendingTree pending = PENDING.remove(pos.immutable());
            if (pending == null) return;

            // ── Bug1 修正 ──────────────────────────────────────────
            // 改用半徑範圍掃描取代「只看 BFS 連通集合」。
            // 原先僅檢查 BFS 抓到的集合，紅樹的 MANGROVE_ROOTS 會切斷連通，
            // 使多個「子段」各自在砍完後認為整棵樹消失，重複觸發種植。
            // 現在只要方圓範圍內仍有同樹種原木，就停止。
            if (hasRemainingLogsNearby(world, pos, pending.sapling())) return;

            if (!(player instanceof ServerPlayer serverPlayer)) return;
            if (!TreeAutoManager.isAutoReplantEnabled(serverPlayer.getUUID())) return;

            // ── Bug2 修正 ──────────────────────────────────────────
            // 原先從 PENDING 集合取最低 Y 作為種植點：
            //   若玩家由下往上砍，最後砍的是高處方塊，導致種植點在空中，
            //   canSurvive() 失敗（腳下是空氣），必須「先挖上方再挖根部」才能成功。
            // 現在改為從砍伐位置向下掃描第一個合法種植格，不受砍伐順序影響。
            BlockState saplingState = pending.sapling().defaultBlockState();
            BlockPos plantPos = findPlantingPosition(world, pos, saplingState);
            if (plantPos == null) return;

            // 消耗背包內一個樹苗
            if (!consumeSaplingFromInventory(serverPlayer, pending.sapling().asItem())) {
                MessageDisplayManager.sendSystemMessage(serverPlayer,
                        LanguageManager.prefixed("Tree", "tree.error.no_sapling", serverPlayer)
                                .withStyle(ChatFormatting.YELLOW));
                return;
            }

            world.setBlock(plantPos, saplingState, Block.UPDATE_ALL);
            sendReplantMessage(serverPlayer);
        });
    }

    /** 玩家下線時清理計數器，防止記憶體洩漏 */
    public static void removePlayer(UUID playerId) {
        replantCount.remove(playerId);
        lastReplantTime.remove(playerId);
    }

    // -------------------------------------------------------
    // 私有工具方法
    // -------------------------------------------------------

    /**
     * 在以 center 為中心的矩形範圍內，搜尋是否還有屬於同一樹種的原木。
     *
     * <p>修正 Bug1：紅樹木的樹幹被 MANGROVE_ROOTS 隔開，BFS 連通集合
     * 無法一次涵蓋所有樹幹，導致各段分別觸發「全砍完」判斷。
     * 改用範圍掃描後，只要附近還有同種原木就不種植，避免重複觸發。
     */
    private static boolean hasRemainingLogsNearby(Level world, BlockPos center, Block sapling) {
        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dy = -SEARCH_DOWN; dy <= SEARCH_UP; dy++) {
                for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                    Block b = world.getBlockState(center.offset(dx, dy, dz)).getBlock();
                    if (LOG_TO_SAPLING.getOrDefault(b, null) == sapling) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 從砍伐位置向下掃描，找到第一個「空氣且 canSurvive」的合法種植格。
     *
     * <p>修正 Bug2：原先取 PENDING 集合最低 Y，若最後砍的不是根部（例如
     * 由下往上砍），種植點會跑到半空中。向下掃描後無論砍伐順序為何，
     * 都能找到貼近地面的正確位置。
     */
    private static BlockPos findPlantingPosition(Level world, BlockPos from, BlockState saplingState) {
        for (int y = from.getY(); y >= from.getY() - PLANT_SCAN; y--) {
            BlockPos candidate = new BlockPos(from.getX(), y, from.getZ());
            if (world.getBlockState(candidate).isAir()
                    && saplingState.canSurvive(world, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * 顯示種植成功訊息於 action bar，5 秒內連續種植自動疊加計數。
     *
     * <p>使用 action bar（overlay = true）讓新訊息自動覆蓋舊訊息，
     * 從而達成「已重新種植 → 已重新種植 x2 → 已重新種植 x3」的效果，
     * 不會在聊天欄中堆出大量相同訊息。
     */
    private static void sendReplantMessage(ServerPlayer player) {
        long now  = System.currentTimeMillis();
        UUID  id  = player.getUUID();
        long last = lastReplantTime.getOrDefault(id, 0L);

        int count = (now - last > MESSAGE_EXPIRE_MS)
                ? 1
                : replantCount.getOrDefault(id, 0) + 1;

        replantCount.put(id, count);
        lastReplantTime.put(id, now);

        String base = LanguageManager.translate("tree.success.replanted", player);
        String text = count > 1 ? base + " x" + count : base;

        // 直接發 action bar 封包，相容所有 mapping 版本
        // ClientboundSetActionBarTextPacket 會覆蓋前一則 action bar 訊息，
        // 達成「已重新種植 → 已重新種植 x2 → 已重新種植 x3」的疊加效果
        net.minecraft.network.chat.Component msg =
                LanguageManager.prefixed("Tree", "", player)
                        .append(Component.literal(text).withStyle(ChatFormatting.GREEN));
        player.connection.send(
                new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(msg));
    }

    /** 從背包消耗一個指定樹苗，成功回傳 true */
    private static boolean consumeSaplingFromInventory(ServerPlayer player, Item saplingItem) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.is(saplingItem)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    /** BFS 找出從 start 出發、6 向相連的所有原木位置 */
    private static Set<BlockPos> findConnectedLogs(Level world, BlockPos start) {
        Set<BlockPos>   visited = new HashSet<>();
        Queue<BlockPos> queue   = new LinkedList<>();
        visited.add(start.immutable());
        queue.add(start.immutable());

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir).immutable();
                if (!visited.contains(neighbor)
                        && world.getBlockState(neighbor).is(BlockTags.LOGS)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return visited;
    }
}