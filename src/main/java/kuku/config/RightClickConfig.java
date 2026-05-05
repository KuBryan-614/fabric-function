package kuku.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

public class RightClickConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir()
            .resolve("function").resolve("rightclick.json");

    private boolean enabled = true;

    private static RightClickConfig INSTANCE;

    public static RightClickConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    public static RightClickConfig load() {
        File file = CONFIG_PATH.toFile();
        if (!file.exists()) {
            RightClickConfig config = new RightClickConfig();
            config.save();
            return config;
        }
        try (Reader reader = new FileReader(file)) {
            RightClickConfig config = GSON.fromJson(reader, RightClickConfig.class);
            if (config != null) return config;
        } catch (Exception e) {
            // 備份損壞的配置
            File backup = new File(file.getPath() + ".bak");
            try {
                java.nio.file.Files.copy(file.toPath(), backup.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {}
            e.printStackTrace();
        }
        return new RightClickConfig();
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
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}