package dev.tagmod.mixin;
import dev.tagmod.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(RepairItemRecipe.class)
abstract class RepairRecipeMixin {
    @Inject(method="canCombine",at=@At("HEAD"),cancellable=true)
    private static void tagmod$compatible(ItemStack first,ItemStack second,CallbackInfoReturnable<Boolean> cir){if(first.getItem()==second.getItem()&&Gear.eligible(first)&&first.getCount()==1&&second.getCount()==1)cir.setReturnValue(true);}
    @Inject(method="assemble(Lnet/minecraft/world/item/crafting/CraftingInput;)Lnet/minecraft/world/item/ItemStack;",at=@At("RETURN"),cancellable=true)
    private void tagmod$result(CraftingInput input,CallbackInfoReturnable<ItemStack> cir){
        if(!TagMod.onServerThread())return;var items=input.items().stream().filter(s->!s.isEmpty()).toList();
        if(items.size()==2)cir.setReturnValue(Preview.combine(null,cir.getReturnValue(),items.get(0),items.get(1)));
    }
}
