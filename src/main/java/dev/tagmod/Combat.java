package dev.tagmod;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class Combat {
    private static final Map<LivingEntity,Long> LAST_HEAL=new WeakHashMap<>();
    public static ItemStack weapon(DamageSource source) {
        if(source.getDirectEntity() instanceof AbstractArrow arrow){ItemStack s=arrow.getWeaponItem();return s==null?ItemStack.EMPTY:s;}
        if(source.getEntity() instanceof LivingEntity living&&source.getDirectEntity()==living)return living.getMainHandItem();
        return ItemStack.EMPTY;
    }
    public static void projectile(Projectile projectile,ItemStack weapon) {
        for(String key:List.of("damage","lifesteal")) {
            double value=Gear.value(weapon,key);if(value!=0)projectile.addTag("tagmod:"+key+":"+value);
        }
        GearData data=Gear.data(weapon);
        if(data!=null){projectile.addTag("tagmod:heal_cap:"+data.lifestealCap());projectile.addTag("tagmod:heal_cooldown:"+data.lifestealCooldownTicks());}
    }
    private static double projectileValue(Entity entity,String key) {
        if(entity==null)return 0;
        String prefix="tagmod:"+key+":";
        for(String tag:entity.entityTags())if(tag.startsWith(prefix))try{return Double.parseDouble(tag.substring(prefix.length()));}catch(NumberFormatException ignored){}
        return 0;
    }
    public static float projectileDamage(DamageSource source,float amount) {
        if(!(source.getDirectEntity() instanceof Projectile)||!source.is(DamageTypeTags.IS_PROJECTILE))return amount;
        ItemStack weapon=weapon(source);double v=weapon.isEmpty()?projectileValue(source.getDirectEntity(),"damage"):Gear.value(weapon,"damage");
        return (float)Math.max(0,amount*(1+v));
    }
    public static void lifesteal(LivingEntity victim,DamageSource source,float healthBefore) {
        if(!(source.getEntity() instanceof LivingEntity attacker)||attacker==victim||!attacker.isAlive()||source.is(DamageTypeTags.IS_EXPLOSION))return;
        if(source.getDirectEntity()!=attacker&&!source.is(DamageTypeTags.IS_PROJECTILE))return;
        ItemStack weapon=weapon(source);double ratio=weapon.isEmpty()?projectileValue(source.getDirectEntity(),"lifesteal"):Gear.value(weapon,"lifesteal");
        GearData data=Gear.data(weapon);
        double cap=data==null?projectileValue(source.getDirectEntity(),"heal_cap"):data.lifestealCap();
        int cooldown=data==null?(int)projectileValue(source.getDirectEntity(),"heal_cooldown"):data.lifestealCooldownTicks();
        float dealt=Math.max(0,healthBefore-victim.getHealth());long now=attacker.level().getGameTime();
        if(ratio<=0||dealt<=0||now-LAST_HEAL.getOrDefault(attacker,Long.MIN_VALUE/2)<cooldown)return;
        attacker.heal((float)Math.min(cap,dealt*ratio));LAST_HEAL.put(attacker,now);
    }
    public static void clear(){LAST_HEAL.clear();}
}
