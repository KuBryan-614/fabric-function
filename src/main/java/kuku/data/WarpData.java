package kuku.data;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import java.util.UUID;

public class WarpData {
    private String name;
    private String ownerUUID;
    private String ownerName;
    private String dimension;   // 例如 "minecraft:overworld"
    private int x, y, z;
    private long creationTime;
    private String iconItemId;

    public WarpData() {}

    public WarpData(String name, UUID owner, String ownerName, Identifier dimension, BlockPos pos) {
        this.name = name;
        this.ownerUUID = owner.toString();
        this.ownerName = ownerName;
        this.dimension = dimension.toString();
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.creationTime = System.currentTimeMillis();
    }

    public String getName() { return name; }
    public UUID getOwnerUUID() {
        try { return UUID.fromString(ownerUUID); } catch (Exception e) { return new UUID(0,0); }
    }
    public String getOwnerName() {
        return ownerName != null ? ownerName : "Unknown";
    }
    public Identifier getDimensionId() {
        Identifier id = Identifier.tryParse(dimension);
        return id != null ? id : Identifier.tryParse("overworld");
    }
    public BlockPos getPos() { return new BlockPos(x, y, z); }
    public long getCreationTime() { return creationTime; }
    public String getIconItemId() { return iconItemId; }

    public void setName(String name) { this.name = name; }
    public void setIconItemId(String id) { this.iconItemId = id; }
}