package kuku.tree;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TreeAutoManager {
    private static final Map<UUID, Boolean> playerStatus = new ConcurrentHashMap<>();

    public static boolean isAutoReplantEnabled(UUID playerId) {
        return playerStatus.getOrDefault(playerId, true); // 預設啟用
    }

    public static void setAutoReplant(UUID playerId, boolean enabled) {
        playerStatus.put(playerId, enabled);
    }

    public static boolean toggleAutoReplant(UUID playerId) {
        boolean current = isAutoReplantEnabled(playerId);
        playerStatus.put(playerId, !current);
        return !current;
    }
}