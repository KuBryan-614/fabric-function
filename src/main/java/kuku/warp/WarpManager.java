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
import java.util.concurrent.ConcurrentHashMap;

public class WarpManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    // 以名称为键，校验全局唯一
    private static final Map<String, WarpData> warps = new ConcurrentHashMap<>();
    private static final Type TYPE = new TypeToken<Map<String, WarpData>>() {}.getType();

    public static boolean addWarp(WarpData warp) {
        // 名称已存在则失败
        if (warps.containsKey(warp.getName())) return false;
        // 检查所有者 warp 数量
        long count = warps.values().stream()
                .filter(w -> w.getOwnerUUID().equals(warp.getOwnerUUID()))
                .count();
        if (count >= WarpConfig.getInstance().getMaxWarps()) {
            return false;   // 超过上限
        }
        warps.put(warp.getName(), warp);
        return true;
    }

    public static boolean deleteWarp(String name, UUID requesterUUID) {
        WarpData warp = warps.get(name);
        if (warp == null) return false;
        // 只能由所有者删除（控制台可绕过，但命令中额外处理）
        if (!warp.getOwnerUUID().equals(requesterUUID)) {
            return false;
        }
        warps.remove(name);
        return true;
    }

    // 供控制台强制删除
    public static boolean forceDelete(String name) {
        return warps.remove(name) != null;
    }

    public static WarpData getWarp(String name) {
        return warps.get(name);
    }

    public static Collection<WarpData> getAllWarps() {
        return List.copyOf(warps.values());
    }

    // 获取某玩家拥有的 warp 列表
    public static List<WarpData> getWarpsByOwner(UUID owner) {
        return warps.values().stream().filter(w -> w.getOwnerUUID().equals(owner)).toList();
    }

    public static void load(Path configDir) {
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
}