package kuku.mixin.axolotl;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobBucketItem.class)
public class AxolotlItemMixin {

    @Inject(method = "mobInteract", at = @At("RETURN"))
    private void onCaptured(
            ItemStack stack,
            Player player,
            LivingEntity entity,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        // 只在成功捕獲時處理
        if (cir.getReturnValue() != InteractionResult.SUCCESS) return;
        if (!(entity instanceof Axolotl axolotl)) return;

        ItemStack itemStack = player.getItemInHand(hand);
        if (!itemStack.is(Items.AXOLOTL_BUCKET)) return;

        // 建立多語言名稱
        Component variantName = Component.translatable(
                axolotl.getVariant().getSerializedName()
        );
        Component ageComponent = axolotl.isBaby()
                ? Component.translatable("axolotl.age.baby")
                : Component.translatable("axolotl.age.adult");

        MutableComponent customName = Component.literal("")
                .append(variantName)
                .append(" ")
                .append(ageComponent);

        itemStack.set(DataComponents.CUSTOM_NAME, customName);
    }
}