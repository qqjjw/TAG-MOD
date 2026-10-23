package dev.tagmod.mixin;
import dev.tagmod.Gear;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(BowItem.class)
abstract class BowMixin {
    @Redirect(method="releaseUsing",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/BowItem;getPowerForTime(I)F"))
    private float tagmod$draw(int ticks,ItemStack stack,Level level,LivingEntity entity,int left){return BowItem.getPowerForTime(Math.max(0,(int)Math.round(ticks*(1+Gear.value(stack,"draw")))));}
}
