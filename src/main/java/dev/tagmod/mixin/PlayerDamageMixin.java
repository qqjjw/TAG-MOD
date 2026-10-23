package dev.tagmod.mixin;
import dev.tagmod.Combat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Player implements its own armor/absorption damage path without calling LivingEntity. */
@Mixin(Player.class)
abstract class PlayerDamageMixin {
    @Unique private float tagmod$playerHealth;
    @Inject(method="actuallyHurt",at=@At("HEAD"))
    private void tagmod$before(ServerLevel level,DamageSource source,float amount,CallbackInfo ci){tagmod$playerHealth=((Player)(Object)this).getHealth();}
    @Inject(method="actuallyHurt",at=@At("RETURN"))
    private void tagmod$after(ServerLevel level,DamageSource source,float amount,CallbackInfo ci){Combat.lifesteal((Player)(Object)this,source,tagmod$playerHealth);}
}
