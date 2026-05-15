package kuku.mixin.name;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(Entity.class)
public interface EntityTagsAccessor {
    @Accessor("tags")
    Set<String> getTags();
}