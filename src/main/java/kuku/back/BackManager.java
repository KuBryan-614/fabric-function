package kuku.back;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BackManager {
    public record LastLocation(Identifier dimensionId, double x, double y, double z,
                               float yaw, float pitch) {}

    private static final Map<UUID, LastLocation> lastLocs = new ConcurrentHashMap<>();

    // 记录当前位置（用于传送前）
    public static void record(ServerPlayer player) {
        lastLocs.put(player.getUUID(), fromPlayer(player));
    }

    // 获取并清除记录（/back 后移除，防止重复返回）
    public static LastLocation consume(UUID playerId) {
        return lastLocs.remove(playerId);
    }

    public static LastLocation peek(UUID playerId) {
        return lastLocs.get(playerId);
    }

    private static LastLocation fromPlayer(ServerPlayer player) {
        Identifier dimId = Identifier.tryParse(dimensionToString(player.level().dimension()));
        return new LastLocation(
                dimId != null ? dimId : Identifier.tryParse("minecraft:overworld"),
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot()
        );
    }

    // 从 HomeCommand 复制的维度解析方法
    private static String dimensionToString(ResourceKey<Level> dimensionKey) {
        String raw = dimensionKey.toString();
        int slashIndex = raw.indexOf('/');
        if (slashIndex != -1) {
            String idPart = raw.substring(slashIndex + 1).trim();
            if (idPart.endsWith("]")) {
                idPart = idPart.substring(0, idPart.length() - 1);
            }
            return idPart;
        }
        return raw;
    }
}