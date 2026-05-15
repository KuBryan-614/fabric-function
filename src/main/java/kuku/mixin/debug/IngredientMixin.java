package kuku.mixin.debug;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Ingredient.class)
public class IngredientMixin {
    private static final String DEBUG_TAG = "debug_stick_marker";

    @Inject(method = "test", at = @At("HEAD"), cancellable = true)
    private void preventDebugStickCrafting(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null && data.copyTag().contains(DEBUG_TAG)) {
            cir.setReturnValue(false);
        }
    }
}