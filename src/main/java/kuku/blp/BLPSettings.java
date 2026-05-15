package kuku.blp;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BLPSettings {
    // false = 禁止剝皮
    private static final Map<UUID, Boolean> PLAYER_STATUS = new ConcurrentHashMap<>();

    public static boolean canPeel(UUID playerId) {
        return PLAYER_STATUS.getOrDefault(playerId, false);
    }

    public static void setCanPeel(UUID playerId, boolean enabled) {
        PLAYER_STATUS.put(playerId, enabled);
    }

    public static void remove(UUID playerId) {
        PLAYER_STATUS.remove(playerId);
    }
}