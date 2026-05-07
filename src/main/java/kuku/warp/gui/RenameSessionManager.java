package kuku.warp.gui;

import kuku.data.WarpData;
import kuku.lang.LanguageManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RenameSessionManager {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final Map<UUID, RenameSession> sessions = new ConcurrentHashMap<>();
    private static final long TIMEOUT_MS = 30_000;

    public static void startRename(ServerPlayer player, WarpData warp) {
        UUID id = player.getUUID();
        cancelIfExists(id);
        RenameSession session = new RenameSession(warp);
        sessions.put(id, session);

        scheduler.schedule(() -> {
            RenameSession s = sessions.remove(id);
            if (s != null && s.isActive()) {
                player.sendSystemMessage(Component.literal(
                        LanguageManager.translate("warp.rename.timeout", player)));
            }
        }, TIMEOUT_MS / 1000, TimeUnit.SECONDS);  // 改這行

        player.sendSystemMessage(Component.literal(
                LanguageManager.translate("warp.rename.start", player)));
    }

    public static boolean handleChat(ServerPlayer player, String message) {
        UUID id = player.getUUID();
        RenameSession session = sessions.get(id);
        if (session == null || !session.isActive()) return false;

        if (message.equalsIgnoreCase("/cancel")) {
            sessions.remove(id);
            player.sendSystemMessage(Component.literal(
                    LanguageManager.translate("warp.rename.cancel", player)));
            return true;
        }

        // 尝试重命名
        String newName = message.trim();
        WarpData warp = session.getWarp();
        if (kuku.warp.WarpManager.getWarp(newName) != null) {
            player.sendSystemMessage(Component.literal(
                    LanguageManager.translate("warp.rename.duplicate", player)));
            return true;
        }
        if (kuku.warp.WarpManager.renameWarp(warp.getName(), newName)) {
            player.sendSystemMessage(Component.literal(
                    LanguageManager.translate("warp.rename.success", player, newName)));
        } else {
            player.sendSystemMessage(Component.literal(
                    LanguageManager.translate("warp.rename.fail", player)));
        }
        sessions.remove(id);
        return true;
    }

    public static void cancelIfExists(UUID playerId) {
        sessions.remove(playerId);
    }

    private static class RenameSession {
        private final WarpData warp;
        private final long startTime;
        RenameSession(WarpData warp) {
            this.warp = warp;
            this.startTime = System.currentTimeMillis();
        }
        boolean isActive() { return true; }
        WarpData getWarp() { return warp; }
    }

    public static void shutdown() {
        scheduler.shutdownNow();
    }
}