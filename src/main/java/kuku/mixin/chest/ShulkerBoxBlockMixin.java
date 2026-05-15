package kuku.mixin.chest;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShulkerBoxBlock.class)
public class ShulkerBoxBlockMixin {

    @Inject(method = "canOpen", at = @At("HEAD"), cancellable = true)
    private static void cancelOpenCheck(BlockState state, Level level, BlockPos pos,
                                        ShulkerBoxBlockEntity blockEntity, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);   // 永遠允許打開
    }
}