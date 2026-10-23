package dev.tagmod.mixin;
import dev.tagmod.*;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(GrindstoneMenu.class)
abstract class GrindstoneMixin {
    @Inject(method="computeResult",at=@At("RETURN"),cancellable=true)
    private void tagmod$combine(ItemStack first,ItemStack second,CallbackInfoReturnable<ItemStack> cir){
        if(!TagMod.onServerThread()||first.isEmpty()||second.isEmpty())return;
        // No player-dependent entropy: both LAN clients receive the server's result.
        cir.setReturnValue(Preview.combine(null,cir.getReturnValue(),first,second));
    }
}
