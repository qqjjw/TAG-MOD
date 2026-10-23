package dev.tagmod.mixin;
import dev.tagmod.*;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AnvilMenu.class)
abstract class AnvilMixin {
    @Shadow protected abstract void onTake(Player p,ItemStack s);
    @Inject(method="createResult",at=@At("TAIL"))
    private void tagmod$result(CallbackInfo ci){
        if(!TagMod.onServerThread())return;
        var self=(AnvilMenu)(Object)this;ItemStack first=self.getSlot(0).getItem(),second=self.getSlot(1).getItem(),out=self.getSlot(2).getItem();
        if(out.isEmpty()||second.isEmpty()||!Gear.eligible(first))return;
        boolean repair=first.getItem()==second.getItem()||(out.getDamageValue()<first.getDamageValue());
        if(repair)Preview.reroll(((CombinerAccessor)this).tagmod$player(),out,first,second);
    }
}
