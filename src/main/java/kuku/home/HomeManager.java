package kuku.home;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import kuku.config.HomeConfig;
import kuku.data.HomeData;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HomeManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, List<HomeData>> homes = new ConcurrentHashMap<>();
    private static final Type TYPE = new TypeToken<Map<UUID, List<HomeData>>>() {}.getType();

    public static List<HomeData> getHomes(UUID playerId) {
        return homes.computeIfAbsent(playerId, k -> new ArrayList<>());
    }

    public static boolean addHome(UUID playerId, HomeData home) {
        List<HomeData> list = getHomes(playerId);
        HomeConfig config = HomeConfig.getInstance();
        if (list.size() >= config.getMaxHomes()) {
            return false;
        }
        if (list.stream().anyMatch(h -> h.getName().equalsIgnoreCase(home.getName()))) {
            return false;
        }
        list.add(home);
        return true;
    }

    public static boolean deleteHome(UUID playerId, String name) {
        List<HomeData> list = getHomes(playerId);
        return list.removeIf(h -> h.getName().equalsIgnoreCase(name));
    }

    public static Optional<HomeData> getHome(UUID playerId, String name) {
        return getHomes(playerId).stream()
                .filter(h -> h.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    // 安全加载：解析成功才替换，失败时备份并保留旧数据
    public static void load(Path configDir) {
        File file = configDir.resolve("homes.json").toFile();
        if (!file.exists()) return;

        Map<UUID, List<HomeData>> loaded = null;
        try (Reader reader = new FileReader(file)) {
            loaded = GSON.fromJson(reader, TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (loaded != null) {
            homes.clear();
            homes.putAll(loaded);
        } else {
            // 数据损坏，备份原文件，不清空现有内存
            try {
                File backup = new File(file.getPath() + ".bak");
                java.nio.file.Files.copy(file.toPath(), backup.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                System.err.println("[Home] 資料檔損壞，已備份為 " + backup.getName());
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    public static void save(Path configDir) {
        File file = configDir.resolve("homes.json").toFile();
        file.getParentFile().mkdirs();
        try (Writer writer = new FileWriter(file)) {
            GSON.toJson(homes, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}