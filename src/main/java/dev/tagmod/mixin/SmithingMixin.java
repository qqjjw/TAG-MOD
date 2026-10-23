package dev.tagmod.mixin;
import dev.tagmod.*;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(SmithingMenu.class)
abstract class SmithingMixin {
    @Inject(method="createResult",at=@At("TAIL"))
    private void tagmod$result(CallbackInfo ci){
        if(!TagMod.onServerThread())return;var self=(SmithingMenu)(Object)this;
        ItemStack template=self.getSlot(0).getItem(),base=self.getSlot(1).getItem(),addition=self.getSlot(2).getItem(),out=self.getSlot(3).getItem();
        if(!out.isEmpty()&&base.getItem()!=out.getItem())Preview.reroll(((CombinerAccessor)this).tagmod$player(),out,template,base,addition);
    }
}
