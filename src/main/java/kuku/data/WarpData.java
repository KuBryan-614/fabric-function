package kuku.data;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import java.util.UUID;

public class WarpData {
    private String name;
    private String ownerUUID;   // 所有者 UUID 字符串
    private String dimension;
    private int x, y, z;

    // Gson 无参构造
    public WarpData() {}

    public WarpData(String name, UUID owner, Identifier dimension, BlockPos pos) {
        this.name = name;
        this.ownerUUID = owner.toString();
        this.dimension = dimension.toString();
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
    }

    public String getName() { return name; }
    public UUID getOwnerUUID() {
        if (ownerUUID == null) {
            kuku.Function.LOGGER.warn("[Warp] warp '{}' 的 ownerUUID 為 null，已使用預設值", name);
            return new UUID(0, 0);
        }
        try {
            return UUID.fromString(ownerUUID);
        } catch (IllegalArgumentException e) {
            kuku.Function.LOGGER.warn("[Warp] warp '{}' 的 ownerUUID 格式錯誤：{}，已使用預設值", name, ownerUUID);
            return new UUID(0, 0);
        }
    }
    public Identifier getDimensionId() {
        Identifier id = Identifier.tryParse(dimension);
        return id != null ? id : Identifier.tryParse("minecraft:overworld");
    }
    public BlockPos getPos() { return new BlockPos(x, y, z); }
}