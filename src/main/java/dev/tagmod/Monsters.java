package dev.tagmod;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import java.util.*;
import java.util.function.Consumer;

public final class Monsters {
    public static final String PROCESSED="tagmod:v1", PREFIX="tagmod:effect:";
    private static final String LARGE="tagmod:large", SIZE_PREFIX="tagmod:large_scale:";
    private static final Set<String> NEUTRAL=Set.of("bee","wolf","polar_bear","panda","dolphin","llama","trader_llama","iron_golem","goat","zombified_piglin","piglin","enderman","spider","cave_spider");
    private static final Set<String> ARMED=Set.of("zombie","husk","drowned","zombie_villager","skeleton","stray","bogged","wither_skeleton","piglin","piglin_brute","zombified_piglin","pillager","vindicator");
    private static final Map<Mob,Long> INVISIBLE_AT=new WeakHashMap<>();
    public static boolean boss(Mob m){return m instanceof EnderDragon||m instanceof WitherBoss||m instanceof Warden;}
    public static boolean target(Mob m) {
        String id=BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()).toString();
        return !TagMod.config.excludedMobTypes.contains(id)&&(m instanceof Monster||boss(m)||NEUTRAL.contains(id.substring(id.indexOf(':')+1))||TagMod.config.extraMobTypes.contains(id));
    }
    public static void initialize(Mob m) {
        if(!target(m)||m.entityTags().contains(PROCESSED))return;
        m.addTag(PROCESSED);Random r=Gear.RANDOM;Config c=TagMod.config;
        if(boss(m)) {if(r.nextDouble()<c.bossChance)m.addTag(PREFIX+"resistance");return;}
        if(r.nextDouble()<c.largeMobChance) {
            double scale=c.largeMobMinScale+r.nextDouble()*(c.largeMobMaxScale-c.largeMobMinScale);
            m.addTag(LARGE);m.addTag(SIZE_PREFIX+scale);restoreSize(m);
        }
        if(r.nextDouble()<c.mobChance) {
            List<String> choices=new ArrayList<>(c.mobEffectWeights.keySet());
            String id=BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()).getPath();
            if(Set.of("skeleton","stray","bogged","pillager","ghast","blaze","witch","guardian","elder_guardian","breeze","evoker","creeper").contains(id))choices.remove("strength");
            int count=r.nextDouble()<c.doubleTraitChance?2:1;
            for(int i=0;i<count&&!choices.isEmpty();i++) {
                int n=Rolls.weighted(r,choices.stream().mapToDouble(k->c.mobEffectWeights.get(k)).toArray());if(n<0)break;
                m.addTag(PREFIX+choices.remove(n));
            }
        }
        if(ARMED.contains(BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()).getPath())&&r.nextDouble()<c.equipmentChance)equip(m);
    }
    private static void equip(Mob m) {
        Random r=Gear.RANDOM;
        Item[][] tiers={{Items.LEATHER_BOOTS,Items.LEATHER_LEGGINGS,Items.LEATHER_CHESTPLATE,Items.LEATHER_HELMET},{Items.CHAINMAIL_BOOTS,Items.CHAINMAIL_LEGGINGS,Items.CHAINMAIL_CHESTPLATE,Items.CHAINMAIL_HELMET},{Items.IRON_BOOTS,Items.IRON_LEGGINGS,Items.IRON_CHESTPLATE,Items.IRON_HELMET}};
        EquipmentSlot[] slots={EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD};
        List<Integer> empty=new ArrayList<>();for(int i=0;i<slots.length;i++)if(m.getItemBySlot(slots[i]).isEmpty())empty.add(i);
        Collections.shuffle(empty,r);int count=Math.min(empty.size(),1+r.nextInt(2));
        for(int n=0;n<count;n++){int index=empty.get(n);give(m,slots[index],new ItemStack(tiers[r.nextInt(tiers.length)][index]));}
        if(m.getMainHandItem().isEmpty()&&Set.of("zombie","husk","zombie_villager","zombified_piglin").contains(BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()).getPath()))give(m,EquipmentSlot.MAINHAND,new ItemStack(r.nextBoolean()?Items.STONE_SWORD:Items.IRON_SWORD));
    }
    private static void give(Mob m,EquipmentSlot slot,ItemStack s) {Gear.ensure(s);m.setItemSlot(slot,s);m.setDropChance(slot,(float)TagMod.config.equipmentDropChance);}
    private static void restoreSize(Mob m) {
        if(boss(m)||!m.entityTags().contains(LARGE))return;
        var scale=m.getAttribute(Attributes.SCALE);
        if(scale==null||scale.hasModifier(TagMod.id("large_mob")))return;
        for(String tag:List.copyOf(m.entityTags()))if(tag.startsWith(SIZE_PREFIX)) {
            try {
                double factor=Double.parseDouble(tag.substring(SIZE_PREFIX.length()));
                if(Double.isFinite(factor)&&factor>=1&&factor<=4)scale.addPermanentModifier(new AttributeModifier(TagMod.id("large_mob"),factor-1,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } catch(NumberFormatException ignored) { }
            return;
        }
    }
    private static HolderEffect resolve(String key) {return new HolderEffect(Effects.effect(switch(key){case "jump"->"jump_boost";default->key;}));}
    private record HolderEffect(net.minecraft.core.Holder<MobEffect> value){}
    static void forEachPersistentTrait(Set<String> tags,Consumer<String> action) {
        // Applying an effect invokes Effects.beforeExternal, which may remove a gear-effect
        // ownership tag from this same set. Traverse a snapshot before invoking callbacks.
        for(String tag:List.copyOf(tags))if(tag.startsWith(PREFIX)&&!tag.equals(PREFIX+"invisibility"))action.accept(tag.substring(PREFIX.length()));
    }
    public static void tick(Mob m) {
        initialize(m);if(!m.entityTags().contains(PROCESSED)||!m.isAlive())return;
        restoreSize(m);
        if(m.tickCount%20==0)forEachPersistentTrait(m.entityTags(),key->{
            var type=resolve(key).value();var old=m.getEffect(type);
            if(old==null||(old.getAmplifier()==0&&!old.isInfiniteDuration()&&old.getDuration()<40)) {
                MobEffectInstance effect=new MobEffectInstance(type,100,0,false,true,true);
                // Dragon and wither reject addEffect outright; allow only our restricted resistance trait.
                if(boss(m)&&type.equals(MobEffects.RESISTANCE))m.forceAddEffect(effect,null);
                else m.addEffect(effect);
            }
        });
        if(m.entityTags().contains(PREFIX+"invisibility")) {
            long now=m.level().getGameTime();long next=INVISIBLE_AT.computeIfAbsent(m,k->now+interval());
            if(now>=next){m.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,TagMod.config.invisibleSeconds*20,0,false,true,true));INVISIBLE_AT.put(m,now+interval());}
        }
    }
    private static int interval(){Config c=TagMod.config;return 20*(c.invisibleMinSeconds+Gear.RANDOM.nextInt(c.invisibleMaxSeconds-c.invisibleMinSeconds+1));}
    public static void copyBeforeConversion(Mob original,Mob converted) {
        for(String tag:original.entityTags())if(tag.startsWith("tagmod:"))converted.addTag(tag);
    }
    public static void clear(){INVISIBLE_AT.clear();}
}
