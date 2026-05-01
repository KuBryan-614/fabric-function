// ✅ 新建 kuku.util.DimensionUtil.java
package kuku.util;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class DimensionUtil {
    public static String dimensionToString(ResourceKey<Level> dimensionKey) {
        String raw = dimensionKey.toString();
        int slashIndex = raw.indexOf('/');
        if (slashIndex != -1) {
            String idPart = raw.substring(slashIndex + 1).trim();
            if (idPart.endsWith("]")) idPart = idPart.substring(0, idPart.length() - 1);
            return idPart;
        }
        return raw;
    }
}