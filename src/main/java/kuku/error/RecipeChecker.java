package kuku.error;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.Recipe;

import java.util.Set;

public class RecipeChecker {

    // 預期必須加載的自定義配方 ID
    private static final Set<Identifier> EXPECTED_IDS = Set.of(
            Identifier.fromNamespaceAndPath("function", "wool_to_string"),
            Identifier.fromNamespaceAndPath("function", "quartz_from_quartz_block"),
            Identifier.fromNamespaceAndPath("function", "quartz_block_from_quartz_bricks"),
            Identifier.fromNamespaceAndPath("function", "packed_ice_from_blue_ice"),
            Identifier.fromNamespaceAndPath("function", "ice_from_packed_ice")
    );

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            RecipeManager recipeManager = server.getRecipeManager();
            for (Identifier id : EXPECTED_IDS) {
                ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, id);
                if (recipeManager.byKey(key).isPresent()) {
                    System.out.println("[Function] ✓ 合成表已加載：" + id);
                } else {
                    System.err.println("[Function] ✗ 錯誤：合成表未加載 ── " + id);
                }
            }
        });
    }
}