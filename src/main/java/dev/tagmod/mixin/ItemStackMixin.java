package dev.tagmod.mixin;

import dev.tagmod.Gear;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import java.util.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
abstract class ItemStackMixin {
    @Inject(method="getHoverName",at=@At("RETURN"),cancellable=true)
    private void tagmod$name(CallbackInfoReturnable<Component> cir){cir.setReturnValue(Gear.name((ItemStack)(Object)this,cir.getReturnValue()));}
    @Inject(method="getTooltipLines",at=@At("RETURN"),cancellable=true)
    private void tagmod$tooltip(Item.TooltipContext context,Player player,TooltipFlag flag,CallbackInfoReturnable<List<Component>> cir){
        List<Component> lines=new ArrayList<>(cir.getReturnValue());Gear.tooltip((ItemStack)(Object)this,lines);cir.setReturnValue(lines);
    }
}
