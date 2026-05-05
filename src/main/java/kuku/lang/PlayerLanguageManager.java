package kuku.lang;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerLanguageManager {
    private static final Gson GSON = new Gson();
    private static final Map<UUID, String> playerLangs = new ConcurrentHashMap<>();

    public static void setLanguage(UUID playerId, String langCode) {
        if (langCode == null || langCode.isEmpty()) {
            playerLangs.remove(playerId);
        } else {
            playerLangs.put(playerId, langCode);
        }
    }

    public static String getLanguage(UUID playerId) {
        return playerLangs.get(playerId);
    }

    public static void load(Path configDir) {
        File file = configDir.resolve("player_languages.json").toFile();
        if (!file.exists()) return;
        try (Reader reader = new FileReader(file)) {
            Map<UUID, String> loaded = GSON.fromJson(reader, new TypeToken<Map<UUID, String>>() {}.getType());
            if (loaded != null) {
                playerLangs.clear();
                playerLangs.putAll(loaded);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void save(Path configDir) {
        File file = configDir.resolve("player_languages.json").toFile();
        file.getParentFile().mkdirs();
        try (Writer writer = new FileWriter(file)) {
            GSON.toJson(playerLangs, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}