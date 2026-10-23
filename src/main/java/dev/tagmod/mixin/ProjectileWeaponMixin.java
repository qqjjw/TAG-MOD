package dev.tagmod.mixin;
import dev.tagmod.Combat;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ProjectileWeaponItem.class)
abstract class ProjectileWeaponMixin {
    @Inject(method="createProjectile",at=@At("RETURN"))
    private void tagmod$projectile(Level level,LivingEntity shooter,ItemStack weapon,ItemStack ammo,boolean critical,CallbackInfoReturnable<Projectile> cir){Combat.projectile(cir.getReturnValue(),weapon);}
}
