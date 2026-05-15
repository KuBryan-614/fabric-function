package kuku.ptv;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PTVSettings {
    private static final Map<UUID, Boolean> PLAYER_STATUS = new ConcurrentHashMap<>();

    public static boolean canAttack(UUID playerId) {
        return PLAYER_STATUS.getOrDefault(playerId, false);
    }

    public static void setCanAttack(UUID playerId, boolean enabled) {
        PLAYER_STATUS.put(playerId, enabled);
    }

    // ++ 新增移除方法
    public static void remove(UUID playerId) {
        PLAYER_STATUS.remove(playerId);
    }
}