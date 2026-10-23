package dev.tagmod.mixin;
import dev.tagmod.Monsters;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(Mob.class)
abstract class ConversionMixin {
    @Redirect(method="convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/ConversionParams$AfterConversion;finalizeConversion(Lnet/minecraft/world/entity/Mob;)V"))
    private void tagmod$inherit(ConversionParams.AfterConversion<Mob> callback,Mob target){Monsters.copyBeforeConversion((Mob)(Object)this,target);callback.finalizeConversion(target);}
}
