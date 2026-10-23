package dev.tagmod.mixin;

import dev.tagmod.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Inventory.class)
abstract class InventoryMixin {
    @Inject(method="add(ILnet/minecraft/world/item/ItemStack;)Z",at=@At("HEAD"))
    private void tagmod$add(int slot,ItemStack stack,CallbackInfoReturnable<Boolean> cir){if(TagMod.onServerThread())Gear.ensure(stack);}
    @Inject(method="setItem",at=@At("HEAD"))
    private void tagmod$set(int slot,ItemStack stack,CallbackInfo ci){if(TagMod.onServerThread())Gear.ensure(stack);}
}
