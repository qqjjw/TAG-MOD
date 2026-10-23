package dev.tagmod.mixin;
import dev.tagmod.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AbstractContainerMenu.class)
abstract class MenuMixin {
    @Inject(method="setCarried",at=@At("HEAD"))
    private void tagmod$carried(ItemStack stack,CallbackInfo ci){if(TagMod.onServerThread())Gear.ensure(stack);}
}
