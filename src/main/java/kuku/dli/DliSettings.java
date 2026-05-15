package kuku.dli;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DliSettings {
    private static final Map<UUID, Boolean> PLAYER_SETTINGS = new ConcurrentHashMap<>();

    public static boolean isEnabled(UUID playerId) {
        return PLAYER_SETTINGS.getOrDefault(playerId, true);
    }

    public static void setEnabled(UUID playerId, boolean enabled) {
        PLAYER_SETTINGS.put(playerId, enabled);
    }

    // ++ 新增移除方法
    public static void remove(UUID playerId) {
        PLAYER_SETTINGS.remove(playerId);
    }
}