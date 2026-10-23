package dev.tagmod.mixin;

import dev.tagmod.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    @Unique private float tagmod$healthBefore;
    @Inject(method="tick",at=@At("TAIL"))
    private void tagmod$tick(CallbackInfo ci){if(!TagMod.onServerThread())return;LivingEntity e=(LivingEntity)(Object)this;if(e instanceof Mob m)Monsters.tick(m);if(e instanceof net.minecraft.world.entity.player.Player||e.entityTags().contains(Monsters.PROCESSED))Effects.tick(e);}
    @Inject(method="addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",at=@At("HEAD"))
    private void tagmod$external(MobEffectInstance effect,Entity source,CallbackInfoReturnable<Boolean> cir){Effects.beforeExternal((LivingEntity)(Object)this,effect);}
    @Inject(method="forceAddEffect",at=@At("HEAD"))
    private void tagmod$force(MobEffectInstance effect,Entity source,CallbackInfo ci){Effects.beforeExternal((LivingEntity)(Object)this,effect);}
    @ModifyVariable(method="hurtServer",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private float tagmod$projectileDamage(float amount,ServerLevel level,DamageSource source,float original){return Combat.projectileDamage(source,amount);}
    @Inject(method="actuallyHurt",at=@At("HEAD"))
    private void tagmod$beforeDamage(ServerLevel level,DamageSource source,float amount,CallbackInfo ci){tagmod$healthBefore=((LivingEntity)(Object)this).getHealth();}
    @Inject(method="actuallyHurt",at=@At("TAIL"))
    private void tagmod$afterDamage(ServerLevel level,DamageSource source,float amount,CallbackInfo ci){Combat.lifesteal((LivingEntity)(Object)this,source,tagmod$healthBefore);}
}
