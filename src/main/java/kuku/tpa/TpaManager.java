package kuku.tpa;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TpaManager {

    // 请求类型
    public enum RequestType {
        TPA_TO_TARGET,  // /tpa：发送者传送到目标
        TPA_HERE        // /tpahere：目标传送到发送者
    }

    public static class TpaRequest {
        public final UUID sender;
        public final long timestamp;
        public final RequestType type;

        public TpaRequest(UUID sender, long timestamp, RequestType type) {
            this.sender = sender;
            this.timestamp = timestamp;
            this.type = type;
        }
    }

    public static UUID getExistingSender(UUID target) {
        TpaRequest existing = requests.get(target);
        return existing != null ? existing.sender : null;
    }

    private static final Map<UUID, TpaRequest> requests = new ConcurrentHashMap<>();

    // 新增请求方法，带类型参数
    public static void addRequest(UUID target, UUID sender, RequestType type) {
        requests.put(target, new TpaRequest(sender, System.currentTimeMillis(), type));
    }

    // 旧方法保留（兼容，内部默认使用 TPA_TO_TARGET）
    public static void addRequest(UUID target, UUID sender) {
        addRequest(target, sender, RequestType.TPA_TO_TARGET);
    }

    public static TpaRequest getRequest(UUID target) {
        return requests.get(target);
    }

    public static boolean removeRequest(UUID target) {
        return requests.remove(target) != null;
    }

    public static void clearExpired(long timeoutMs) {
        long now = System.currentTimeMillis();
        requests.entrySet().removeIf(entry -> now - entry.getValue().timestamp > timeoutMs);
    }
}