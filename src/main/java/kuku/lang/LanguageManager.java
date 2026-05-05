package kuku.lang;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class LanguageManager {
    private static final Gson GSON = new Gson();
    private static final Map<String, Map<String, String>> translations = new HashMap<>();
    private static final String FALLBACK_LANG = "en_us";

    public static void load() {
        translations.clear();

        // 取得語言資源目錄路徑（可能位於 JAR 或檔案系統）
        Optional<Path> optionalLangDir = FabricLoader.getInstance()
                .getModContainer("function")
                .map(container -> container.getPath("assets/function/lang"));

        if (optionalLangDir.isEmpty()) {
            System.err.println("[Function] 無法找到語言資源目錄！");
            return;
        }

        Path langDir = optionalLangDir.get();

        // 檢查是否存在
        if (!Files.exists(langDir)) {
            System.err.println("[Function] 語言資源目錄不存在：" + langDir);
            return;
        }

        // 使用 Files.list 遍歷（相容 ZipFileSystem）
        try (var files = Files.list(langDir)) {
            files.filter(path -> path.toString().endsWith(".json"))
                    .forEach(LanguageManager::loadTranslationFile);
        } catch (IOException e) {
            System.err.println("[Function] 讀取語言檔案時發生錯誤：" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void loadTranslationFile(Path filePath) {
        String fileName = filePath.getFileName().toString();
        String langCode = fileName.substring(0, fileName.length() - 5); // 去掉 ".json"

        try (InputStream in = Files.newInputStream(filePath);
             Reader reader = new InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)) {

            Map<String, String> map = GSON.fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
            if (map != null) {
                translations.put(langCode, map);
                System.out.println("[Function] 已載入語言檔案：" + langCode);
            }
        } catch (IOException e) {
            System.err.println("[Function] 無法讀取語言檔案：" + langCode);
            e.printStackTrace();
        }
    }

    public static String getLanguage(ServerPlayer player) {
        if (player == null) return FALLBACK_LANG;
        // 優先使用玩家自訂語言
        String customLang = PlayerLanguageManager.getLanguage(player.getUUID());
        if (customLang != null && translations.containsKey(customLang)) {
            return customLang;
        }
        // 否則使用客戶端語言
        try {
            String lang = player.clientInformation().language();
            return lang != null && !lang.isEmpty() && translations.containsKey(lang) ? lang : FALLBACK_LANG;
        } catch (Exception e) {
            return FALLBACK_LANG;
        }
    }

    public static boolean isLanguageAvailable(String langCode) {
        return translations.containsKey(langCode);
    }

    public static Set<String> getAvailableLanguages() {
        return translations.keySet();
    }

    public static String translate(String key, ServerPlayer player, Object... args) {
        String lang = getLanguage(player);
        Map<String, String> map = translations.getOrDefault(lang, translations.get(FALLBACK_LANG));
        if (map == null) return key;
        String pattern = map.getOrDefault(key, key);
        return format(pattern, args);
    }

    public static MutableComponent component(String key, ServerPlayer player, Object... args) {
        return Component.literal(translate(key, player, args));
    }

    public static MutableComponent prefixed(String moduleName, String key, ServerPlayer player, Object... args) {
        MutableComponent prefix = Component.literal("[Function " + moduleName + "] ")
                .withStyle(ChatFormatting.GOLD);
        if (key == null || key.isEmpty()) {
            return prefix;
        }
        return prefix.append(component(key, player, args));
    }

    public static String format(String pattern, Object... args) {
        if (args.length == 0) return pattern;
        String result = pattern;
        for (int i = 0; i < args.length; i++) {
            result = result.replace("{" + i + "}", args[i] == null ? "null" : args[i].toString());
        }
        return result;
    }
}