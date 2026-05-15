package kuku.mixin.debug;

import kuku.debug.DebugStickActions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {

    @Shadow protected ServerPlayer player;
    @Shadow private GameType gameModeForPlayer;   // 直接對映私有欄位

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void onDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        // 創造模式不攔截
        if (this.gameModeForPlayer == GameType.CREATIVE) return;

        ItemStack held = this.player.getItemInHand(InteractionHand.MAIN_HAND);
        if (DebugStickActions.isDebugStick(held)) {
            cir.setReturnValue(false);
            DebugStickActions.handleLeftClick(this.player, pos);
        }
    }
}