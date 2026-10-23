package dev.tagmod;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Own only the exact effect instance we installed. External applications replace our layer. */
public final class Effects {
    private static final Map<LivingEntity,Map<Holder<MobEffect>,MobEffectInstance>> OWNED=new WeakHashMap<>();
    private static final ThreadLocal<Boolean> INTERNAL=ThreadLocal.withInitial(()->false);
    private static final String MARKER="tagmod:gear_effect:";
    public static Holder<MobEffect> effect(String id) {return BuiltInRegistries.MOB_EFFECT.get(Identifier.withDefaultNamespace(id)).orElseThrow();}
    private static Map<Holder<MobEffect>,MobEffectInstance> owned(LivingEntity entity) {
        return OWNED.computeIfAbsent(entity,e->{
            Map<Holder<MobEffect>,MobEffectInstance> recovered=new HashMap<>();
            for(String tag:new ArrayList<>(e.entityTags()))if(tag.startsWith(MARKER)) {
                try {
                    Holder<MobEffect> type=effect(tag.substring(MARKER.length()));MobEffectInstance instance=e.getEffect(type);
                    if(instance!=null&&instance.getAmplifier()==0&&!instance.isInfiniteDuration()&&instance.getDuration()<=40)recovered.put(type,instance);
                    else e.removeTag(tag);
                }catch(Exception ignored){e.removeTag(tag);}
            }
            return recovered;
        });
    }
    private static String marker(Holder<MobEffect> type){return MARKER+BuiltInRegistries.MOB_EFFECT.getKey(type.value()).getPath();}
    public static void tick(LivingEntity entity) {
        Map<Holder<MobEffect>,MobEffectInstance> owned=owned(entity);
        Set<Holder<MobEffect>> wanted=new HashSet<>();
        if(entity.isAlive())for(EquipmentSlot slot:EquipmentSlot.values()) {
            ItemStack s=entity.getItemBySlot(slot);if(s.isEmpty())continue;
            boolean valid=Gear.armor(s)?s.get(net.minecraft.core.component.DataComponents.EQUIPPABLE).slot()==slot:
                Gear.path(s).equals("shield")?slot==EquipmentSlot.MAINHAND||slot==EquipmentSlot.OFFHAND:slot==EquipmentSlot.MAINHAND;
            if(!valid)continue;
            GearData d=Gear.data(s);if(d==null)continue;
            for(Option o:d.options())if(o.key().startsWith("effect:"))wanted.add(effect(o.key().substring(7)));
        }
        Iterator<Map.Entry<Holder<MobEffect>,MobEffectInstance>> iterator=owned.entrySet().iterator();
        while(iterator.hasNext()) {
            var e=iterator.next();
            if(!wanted.contains(e.getKey())||entity.getEffect(e.getKey())!=e.getValue()) {
                if(entity.getEffect(e.getKey())==e.getValue())entity.removeEffect(e.getKey());
                entity.removeTag(marker(e.getKey()));
                iterator.remove();
            }
        }
        for(Holder<MobEffect> type:wanted) {
            MobEffectInstance current=entity.getEffect(type);
            if(current==null) {
                MobEffectInstance added=new MobEffectInstance(type,40,0,false,true,true);
                INTERNAL.set(true);
                try {entity.addEffect(added);if(entity.getEffect(type)==added){owned.put(type,added);entity.addTag(marker(type));}}finally{INTERNAL.set(false);}
            } else if(owned.get(type)==current&&current.getDuration()<20) {
                INTERNAL.set(true);
                try {entity.addEffect(new MobEffectInstance(type,40,0,false,true,true));}finally{INTERNAL.set(false);}
            }
        }
        if(owned.isEmpty())OWNED.remove(entity);
    }
    public static void beforeExternal(LivingEntity e,MobEffectInstance incoming) {
        if(INTERNAL.get()||!TagMod.onServerThread())return;
        var map=owned(e);
        var old=map.remove(incoming.getEffect());
        e.removeTag(marker(incoming.getEffect()));
        if(old!=null&&e.getEffect(incoming.getEffect())==old)e.removeEffect(incoming.getEffect());
    }
    public static void clear(){OWNED.clear();}
}
