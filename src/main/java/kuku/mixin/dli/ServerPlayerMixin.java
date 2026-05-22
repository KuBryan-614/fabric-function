package kuku.mixin.dli;

import kuku.dli.DliSetup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void onDrop(ItemStack stack, boolean throwRandomly, boolean retainOwnership,
                        CallbackInfoReturnable<ItemEntity> cir) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (!DliSetup.onDropItem(self, stack)) {
            cir.setReturnValue(null); // 取消丟棄
        }
    }
}
