package kuku.warp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import kuku.config.WarpConfig;
import kuku.data.WarpData;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class WarpManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, WarpData> warps = new ConcurrentHashMap<>();
    private static final Type TYPE = new TypeToken<Map<String, WarpData>>() {}.getType();
    private static Path configDir;   // 保存路径，加載時設定

    public static synchronized boolean addWarp(WarpData warp) {
        if (warps.containsKey(warp.getName())) return false;
        long count = warps.values().stream()
                .filter(w -> w.getOwnerUUID().equals(warp.getOwnerUUID()))
                .count();
        if (count >= WarpConfig.getInstance().getMaxWarps()) {
            return false;
        }
        warps.put(warp.getName(), warp);
        scheduleSave();
        return true;
    }

    public static synchronized boolean deleteWarp(String name, UUID requesterUUID) {
        WarpData warp = warps.get(name);
        if (warp == null) return false;
        if (!warp.getOwnerUUID().equals(requesterUUID)) {
            return false;
        }
        warps.remove(name);
        scheduleSave();
        return true;
    }

    public static synchronized boolean renameWarp(String oldName, String newName) {
        // 此方法已有 synchronized，无需更改，这里仅展示保持一致
        if (warps.containsKey(newName)) return false;
        WarpData warp = warps.remove(oldName);
        if (warp != null) {
            warp.setName(newName);
            warps.put(newName, warp);
            scheduleSave();
            return true;
        }
        return false;
    }

    public static synchronized boolean forceDelete(String name) {
        boolean removed = warps.remove(name) != null;
        if (removed) scheduleSave();
        return removed;
    }

    public static WarpData getWarp(String name) {
        return warps.get(name);
    }

    public static Collection<WarpData> getAllWarps() {
        return List.copyOf(warps.values());
    }

    public static List<WarpData> getWarpsByOwner(UUID owner) {
        return warps.values().stream().filter(w -> w.getOwnerUUID().equals(owner)).toList();
    }

    public static void load(Path configDir) {
        WarpManager.configDir = configDir;   // 記住路徑
        File file = configDir.resolve("warps.json").toFile();
        if (!file.exists()) return;

        Map<String, WarpData> loaded = null;
        try (Reader reader = new FileReader(file)) {
            loaded = GSON.fromJson(reader, TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (loaded != null) {
            warps.clear();
            warps.putAll(loaded);
        } else {
            try {
                File backup = new File(file.getPath() + ".bak");
                java.nio.file.Files.copy(file.toPath(), backup.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                System.err.println("[WarpManager] 数据文件损坏，已备份为 " + backup.getName());
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    public static void save(Path configDir) {
        File file = configDir.resolve("warps.json").toFile();
        file.getParentFile().mkdirs();
        try (Writer writer = new FileWriter(file)) {
            GSON.toJson(warps, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 異步保存，避免阻塞主執行緒
    private static void scheduleSave() {
        if (configDir == null) return;   // 尚未初始化
        final Path path = configDir;
        CompletableFuture.runAsync(() -> save(path));
    }
}