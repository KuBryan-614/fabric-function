package kuku.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

public class TpaConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir()
            .resolve("function").resolve("tpa.json");

    private boolean enabled = true;
    private int timeout = 60; // 秒

    private static TpaConfig INSTANCE;

    public static TpaConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    public static TpaConfig load() {
        File file = CONFIG_PATH.toFile();
        if (!file.exists()) {
            TpaConfig config = new TpaConfig();
            config.save();
            return config;
        }
        try (Reader reader = new FileReader(file)) {
            TpaConfig config = GSON.fromJson(reader, TpaConfig.class);
            if (config != null) return config;
        } catch (Exception e) {
            File backup = new File(file.getPath() + ".bak");
            try { java.nio.file.Files.copy(file.toPath(), backup.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING); } catch (IOException ignored) {}
            e.printStackTrace();
        }
        return new TpaConfig();
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

    public boolean isEnabled() { return enabled; }
    public int getTimeout() { return timeout; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setTimeout(int timeout) { this.timeout = timeout; }
}