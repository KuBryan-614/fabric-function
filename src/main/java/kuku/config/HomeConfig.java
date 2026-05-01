package kuku.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

public class HomeConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir()
            .resolve("function").resolve("home.json");

    private boolean enabled = true;
    private int maxHomes = 5;
    private String defaultHomeName = "home";

    // 单例
    private static HomeConfig INSTANCE;

    public static HomeConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    public static HomeConfig load() {
        File file = CONFIG_PATH.toFile();
        if (!file.exists()) {
            HomeConfig config = new HomeConfig();
            config.save();
            return config;
        }
        try (Reader reader = new FileReader(file)) {
            HomeConfig config = GSON.fromJson(reader, HomeConfig.class);
            if (config != null) return config;
        } catch (Exception e) {
            File backup = new File(file.getPath() + ".bak");
            try { java.nio.file.Files.copy(file.toPath(), backup.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING); } catch (IOException ignored) {}
            e.printStackTrace();
        }
        return new HomeConfig();
    }

    public void save() {
        File file = CONFIG_PATH.toFile();
        file.getParentFile().mkdirs();
        try (Writer writer = new FileWriter(file)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Getter
    public boolean isEnabled() { return enabled; }
    public int getMaxHomes() { return maxHomes; }
    public String getDefaultHomeName() { return defaultHomeName; }

    // Setter（用于 /function reload 时替换）
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setMaxHomes(int maxHomes) { this.maxHomes = maxHomes; }
    public void setDefaultHomeName(String defaultHomeName) { this.defaultHomeName = defaultHomeName; }
}