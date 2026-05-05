package kuku.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MessageDisplayManager {
    public enum DisplayMode {
        CHAT,
        ACTION_BAR
    }

    private static final Map<UUID, DisplayMode> playerModes = new ConcurrentHashMap<>();
    private static final Gson GSON = new Gson();
    private static Path storagePath;

    public static void initStorage() {
        storagePath = FabricLoader.getInstance().getConfigDir()
                .resolve("function").resolve("player_message_display.json");
    }

    public static void setMode(UUID playerId, DisplayMode mode) {
        playerModes.put(playerId, mode);
        save(); // 即時寫入檔案
    }

    public static DisplayMode getMode(UUID playerId) {
        return playerModes.getOrDefault(playerId, DisplayMode.CHAT);
    }

    public static void sendSystemMessage(ServerPlayer player, Component message) {
        if (getMode(player.getUUID()) == DisplayMode.ACTION_BAR) {
            // 依序嘗試兩種可能的方法名（Mojang vs Yarn mapping）
            String[] methodNames = {"displayClientMessage", "sendMessage"};
            boolean sent = false;
            for (String methodName : methodNames) {
                try {
                    java.lang.reflect.Method method = ServerPlayer.class.getMethod(
                            methodName, Component.class, boolean.class);
                    method.invoke(player, message, true);
                    sent = true;
                    break;
                } catch (NoSuchMethodException ignored) {
                    // 此方法不存在，換下一個
                } catch (Exception e) {
                    // 方法存在但調用失敗（例如 IllegalAccessException），直接結束迴圈
                    break;
                }
            }
            if (!sent) {
                // 以上方法都失敗時，改用封包作為最終回退方案
                player.connection.send(
                        new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(message));
            }
        } else {
            player.sendSystemMessage(message);
        }
    }

    /** 載入玩家設定（全程使用 NIO，避免 toFile() 異常） */
    public static void load() {
        if (storagePath == null) initStorage();
        if (!Files.exists(storagePath)) return;

        try (Reader reader = Files.newBufferedReader(storagePath, StandardCharsets.UTF_8)) {
            Map<UUID, String> rawMap = GSON.fromJson(reader,
                    new TypeToken<Map<UUID, String>>() {}.getType());
            if (rawMap != null) {
                rawMap.forEach((uuid, str) -> {
                    try {
                        playerModes.put(uuid, DisplayMode.valueOf(str));
                    } catch (IllegalArgumentException ignored) {}
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** 儲存玩家設定（全程使用 NIO） */
    public static void save() {
        if (storagePath == null) return;
        try {
            Files.createDirectories(storagePath.getParent());
            try (Writer writer = Files.newBufferedWriter(storagePath, StandardCharsets.UTF_8)) {
                Map<UUID, String> rawMap = new HashMap<>();
                playerModes.forEach((uuid, mode) -> rawMap.put(uuid, mode.name()));
                GSON.toJson(rawMap, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}