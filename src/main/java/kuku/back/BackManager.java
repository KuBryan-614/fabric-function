package kuku.back;

import kuku.util.DimensionUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BackManager {
    public record LastLocation(Identifier dimensionId, double x, double y, double z,
                               float yaw, float pitch) {}

    /**
     * 單一 slot：死亡和傳送都寫這裡。
     * /back 前先把當前位置寫入，再讀取舊值傳送，實現 A↔B 無限切換。
     */
    private static final Map<UUID, LastLocation> lastLocations = new ConcurrentHashMap<>();

    /** 傳送前（/home /warp /tpa）記錄當前位置 */
    public static void recordTeleport(ServerPlayer player) {
        lastLocations.put(player.getUUID(), fromPlayer(player));
    }

    /** 死亡時記錄死亡位置（與 recordTeleport 寫同一個 slot，優先蓋過傳送記錄） */
    public static void recordDeath(ServerPlayer player) {
        lastLocations.put(player.getUUID(), fromPlayer(player));
    }

    /** 取得記錄（不清除），供 /back 先讀目標再寫當前位置用 */
    public static LastLocation get(UUID playerId) {
        return lastLocations.get(playerId);
    }

    /** 取得並清除記錄 */
    public static LastLocation consume(UUID playerId) {
        return lastLocations.remove(playerId);
    }

    /** 是否有記錄 */
    public static boolean has(UUID playerId) {
        return lastLocations.containsKey(playerId);
    }

    /** 玩家離線時清除 */
    public static void removeAll(UUID playerId) {
        lastLocations.remove(playerId);
    }

    // ── 舊 API 兼容（避免其他地方編譯失敗）──────────────────────
    public static void recordTeleport(ServerPlayer player, boolean ignored) { recordTeleport(player); }
    public static LastLocation getDeath(UUID id)      { return get(id); }
    public static LastLocation getTeleport(UUID id)   { return get(id); }
    public static LastLocation consumeDeath(UUID id)  { return consume(id); }
    public static LastLocation consumeTeleport(UUID id){ return consume(id); }
    public static boolean hasDeath(UUID id)           { return has(id); }
    public static boolean hasTeleport(UUID id)        { return has(id); }
    // ─────────────────────────────────────────────────────────────

    private static LastLocation fromPlayer(ServerPlayer player) {
        String dimStr = DimensionUtil.dimensionToString(player.level().dimension());
        Identifier dimId = Identifier.tryParse(dimStr);
        return new LastLocation(
                dimId != null ? dimId : Identifier.tryParse("minecraft:overworld"),
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot()
        );
    }
}