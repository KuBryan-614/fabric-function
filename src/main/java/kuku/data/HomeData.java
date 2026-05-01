package kuku.data;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public class HomeData {
    private String name;
    private String dimension;   // 存储为字符串，如 "minecraft:overworld"
    private int x, y, z;

    // 无参构造器供 Gson 反序列化
    public HomeData() {}

    public HomeData(String name, Identifier dimension, BlockPos pos) {
        this.name = name;
        this.dimension = dimension.toString();
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
    }

    public String getName() { return name; }
    public String getDimension() { return dimension; }
    public int getX() { return x; }
    public int getY() { return y; }
    public Identifier getDimensionId() {
        Identifier id = Identifier.tryParse(dimension);
        if (id == null) {
            // 作为后备，返回一个已知维度的默认值，或者抛出异常
            return Identifier.tryParse("minecraft:overworld");
        }
        return id;
    }// 解析字符串为Identifier

    public BlockPos getPos() { return new BlockPos(x, y, z); }
}