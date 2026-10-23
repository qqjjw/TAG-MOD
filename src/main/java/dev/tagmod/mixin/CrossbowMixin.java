package dev.tagmod.mixin;
import dev.tagmod.Gear;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(CrossbowItem.class)
abstract class CrossbowMixin {
    @Inject(method="getChargeDuration",at=@At("RETURN"),cancellable=true)
    private static void tagmod$charge(ItemStack stack,LivingEntity entity,CallbackInfoReturnable<Integer> cir){cir.setReturnValue(Math.max(1,(int)Math.round(cir.getReturnValue()/(1+Gear.value(stack,"draw")))));}
}
