package kuku.mixin.name;

import kuku.name.NameFeature;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void onHurtServerHead(ServerLevel level, DamageSource source, float amount,
                                  CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        // nd：完全免疫傷害
        if (NameFeature.hasTag(self, "nd")) {
            cir.setReturnValue(false);
            return;
        }

        // ned：若這次傷害會致死，攔截並鎖定血量為 1
        if (NameFeature.hasTag(self, "ned") && self.getHealth() - amount <= 0.0F) {
            self.setHealth(1.0F);
            cir.setReturnValue(false);
        }
    }
}