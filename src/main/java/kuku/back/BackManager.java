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

    // 分開儲存死亡與傳送前的位置
    private static final Map<UUID, LastLocation> deathLocations = new ConcurrentHashMap<>();
    private static final Map<UUID, LastLocation> teleportLocations = new ConcurrentHashMap<>();

    /** 傳送前（/home /warp /tpa）記錄當前位置 */
    public static void recordTeleport(ServerPlayer player) {
        teleportLocations.put(player.getUUID(), fromPlayer(player));
    }

    /** 死亡時記錄死亡位置 */
    public static void recordDeath(ServerPlayer player) {
        deathLocations.put(player.getUUID(), fromPlayer(player));
    }

    // 死亡記錄操作
    public static LastLocation getDeath(UUID playerId) {
        return deathLocations.get(playerId);
    }
    public static LastLocation consumeDeath(UUID playerId) {
        return deathLocations.remove(playerId);
    }
    public static boolean hasDeath(UUID playerId) {
        return deathLocations.containsKey(playerId);
    }

    // 傳送記錄操作
    public static LastLocation getTeleport(UUID playerId) {
        return teleportLocations.get(playerId);
    }
    public static LastLocation consumeTeleport(UUID playerId) {
        return teleportLocations.remove(playerId);
    }
    public static boolean hasTeleport(UUID playerId) {
        return teleportLocations.containsKey(playerId);
    }

    /** 玩家離線時清除所有記錄 */
    public static void removeAll(UUID playerId) {
        deathLocations.remove(playerId);
        teleportLocations.remove(playerId);
    }

    // ── 向下相容（保留舊 API 呼叫）──────────────────────
    public static void recordTeleport(ServerPlayer player, boolean ignored) { recordTeleport(player); }
    public static LastLocation get(UUID id)      { return getTeleport(id); }
    public static LastLocation consume(UUID id)  { return consumeTeleport(id); }
    public static boolean has(UUID id)           { return hasTeleport(id); }
    // ─────────────────────────────────────────────────────

    private static LastLocation fromPlayer(ServerPlayer player) {
        String dimStr = DimensionUtil.dimensionToString(player.level().dimension());
        Identifier dimId = Identifier.tryParse(dimStr);
        return new LastLocation(
                dimId != null ? dimId : Identifier.withDefaultNamespace("overworld"),
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot()
        );
    }
}