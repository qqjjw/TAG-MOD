package dev.tagmod;

import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import java.util.*;

public final class Gear {
    private static final Map<CustomData, Optional<GearData>> CACHE=Collections.synchronizedMap(new WeakHashMap<>());
    public static final Random RANDOM=new Random();
    private static final Set<String> SPECIAL=Set.of("bow","crossbow","trident","shield","elytra","fishing_rod","shears","flint_and_steel","mace","brush");
    private static final Set<String> MELEE_SUFFIX=Set.of("_sword","_axe","_spear");
    private static final Map<String,String[]> LABELS=Map.ofEntries(
        Map.entry("damage",new String[]{"맹렬한","무딘","피해량"}), Map.entry("speed",new String[]{"날렵한","둔중한","공격속도"}),
        Map.entry("mining",new String[]{"잘 벼린","마모된","채굴속도"}), Map.entry("draw",new String[]{"민첩한","뻑뻑한","장전속도"}),
        Map.entry("durability",new String[]{"튼튼한","부실한","최대 내구도"}), Map.entry("reach",new String[]{"길게 벼린","짧게 벼린","공격 사거리"}),
        Map.entry("block_reach",new String[]{"긴 손잡이의","짧은 손잡이의","블록 상호작용 거리"}), Map.entry("armor",new String[]{"견고한","허술한","방어력"}),
        Map.entry("toughness",new String[]{"강인한","연약한","방어 강도"}), Map.entry("movement",new String[]{"경쾌한","육중한","이동속도"}),
        Map.entry("health",new String[]{"활력의","쇠약의","최대 체력"}), Map.entry("knockback",new String[]{"굳건한","불안정한","넉백 저항"}),
        Map.entry("safe_fall",new String[]{"사뿐한","위태로운","안전 낙하 거리"}), Map.entry("fall_damage",new String[]{"완충의","충격에 약한","낙하 피해"}),
        Map.entry("water_mining",new String[]{"해저의","물에 둔한","수중 채굴속도"}), Map.entry("water_move",new String[]{"물살의","물에 잠긴","수중 이동 효율"}));
    public static String itemId(ItemStack s) {return BuiltInRegistries.ITEM.getKey(s.getItem()).toString();}
    public static String path(ItemStack s) {return BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();}
    public static boolean melee(ItemStack s) {String p=path(s);return MELEE_SUFFIX.stream().anyMatch(p::endsWith)||p.equals("mace")||p.equals("trident");}
    public static boolean ranged(ItemStack s) {return s.getItem() instanceof ProjectileWeaponItem || path(s).equals("trident");}
    public static boolean mining(ItemStack s) {String p=path(s);return p.endsWith("_pickaxe")||p.endsWith("_axe")||p.endsWith("_shovel")||p.endsWith("_hoe")||p.equals("shears");}
    public static boolean armor(ItemStack s) {var e=s.get(DataComponents.EQUIPPABLE); return e!=null&&List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET).contains(e.slot());}
    public static boolean eligible(ItemStack s) {
        if(s.isEmpty()||!s.isDamageableItem())return false;
        String id=itemId(s); Config c=TagMod.config;
        return !c.excludedItems.contains(id)&&(c.extraItems.contains(id)||melee(s)||mining(s)||armor(s)||SPECIAL.contains(path(s)));
    }
    public static GearData data(ItemStack s) {
        CustomData c=s.get(DataComponents.CUSTOM_DATA); if(c==null)return null;
        return CACHE.computeIfAbsent(c,key->{
            try {String json=key.copyTag().getStringOr("tagmod",""); if(json.isEmpty())return Optional.empty();
                GearData d=TagMod.JSON.fromJson(json,GearData.class);
                if(d==null||d.version()!=GearData.VERSION||d.options()==null||d.options().isEmpty()||d.options().size()>3||d.options().stream().anyMatch(o->o==null||o.key()==null||o.prefix()==null||o.description()==null||!Double.isFinite(o.value())))return Optional.empty();
                return Optional.of(d);
            } catch(Exception e){return Optional.empty();}
        }).orElse(null);
    }
    public static boolean ensure(ItemStack s) { if(!eligible(s)||data(s)!=null)return false; roll(s,RANDOM.nextLong());return true; }
    public static void save(ItemStack s,GearData d) {CustomData.update(DataComponents.CUSTOM_DATA,s,t->t.putString("tagmod",TagMod.JSON.toJson(d)));}
    public static double value(ItemStack s,String key) {GearData d=data(s);return d==null?0:d.options().stream().filter(o->o.key().equals(key)).mapToDouble(Option::value).sum();}
    public static void roll(ItemStack s,long seed) {
        if(!eligible(s))return;
        clean(s);
        Random r=new Random(seed);Config c=TagMod.config;int category=Rolls.category(r,c), count=Rolls.count(r,category);
        List<Option> options=new ArrayList<>();
        for(int i=0;i<count;i++) {
            boolean positive=category==0||(category==2&&(i==0||(i>1&&r.nextBoolean())));
            Option option=draw(s,positive,options,r);
            if(option!=null)options.add(option);
        }
        int base=s.getMaxDamage();
        AttackRange range=s.get(DataComponents.ATTACK_RANGE);
        String rangeJson=range==null?null:AttackRange.CODEC.encodeStart(JsonOps.INSTANCE,range).getOrThrow().toString();
        GearData d=new GearData(GearData.VERSION,seed,itemId(s),new String[]{"유리","불리","혼합"}[category],List.copyOf(options),base,rangeJson,c.lifestealCap,c.lifestealCooldownTicks);
        save(s,d); apply(s,d);
    }
    private static Option draw(ItemStack s,boolean positive,List<Option> previous,Random r) {
        Config c=TagMod.config; List<String> keys=new ArrayList<>();List<Double> weights=new ArrayList<>();
        for(String key:c.ranges.keySet()) {
            if(!applicable(s,key,positive)||conflict(key,previous))continue;
            keys.add(key);weights.add(c.optionWeights.getOrDefault(key,1.0));
        }
        if(!previous.stream().anyMatch(o->o.key().startsWith("effect:"))) { keys.add("potion");weights.add(c.potionOptionWeight); }
        if(positive&&(melee(s)||ranged(s))&&!conflict("lifesteal",previous)) {keys.add("lifesteal");weights.add(c.lifestealOptionWeight);}
        int n=Rolls.weighted(r,weights.stream().mapToDouble(Double::doubleValue).toArray());
        if(n<0)n=r.nextInt(keys.size()); // Invalid all-zero compatible pool: preserve uniqueness.
        String key=keys.get(n);
        if(key.equals("potion"))return potion(positive,previous,r);
        if(key.equals("lifesteal"))return new Option(key,c.lifestealRatio,"핏빛의",String.format(Locale.ROOT,"직접 공격 흡혈 %.1f%% · 적중당 최대 %.2f 체력 · %.2f초 간격",c.lifestealRatio*100,c.lifestealCap,c.lifestealCooldownTicks/20.0));
        double amount=c.amount(key,r)*(positive?1:-1);
        if(key.equals("fall_damage"))amount=-amount;
        String[] label=LABELS.get(key);
        boolean flat=Set.of("reach","block_reach","health","safe_fall").contains(key);
        String unit=Set.of("reach","block_reach","safe_fall").contains(key)?"블록":key.equals("health")?" 체력":"%";
        String description=label[2]+" "+String.format(Locale.ROOT,"%+.2f",flat?amount:amount*100)+unit;
        return new Option(key,amount,label[positive?0:1],description);
    }
    private static Option potion(boolean positive,List<Option> previous,Random r) {
        String[][] good={{"speed","바람의","신속"},{"resistance","수호의","저항"},{"jump_boost","도약의","점프 강화"},{"haste","분주한","성급함"}};
        String[][] bad={{"weakness","무력한","나약함"},{"slowness","속박의","구속"},{"mining_fatigue","피로한","채굴 피로"},{"hunger","굶주린","허기"}};
        String[][] severe={{"poison","독기의","독"},{"wither","시듦의","위더"},{"blindness","눈먼","실명"},{"darkness","암흑의","어둠"},{"levitation","떠도는","공중 부양"}};
        String[][] pool=positive?good:r.nextDouble()<TagMod.config.severeEffectChance?severe:bad;
        List<String[]> choices=Arrays.stream(pool).filter(a->!conflict("effect:"+a[0],previous)).toList();
        if(choices.isEmpty())return new Option("durability",positive?.1:-.1,positive?"튼튼한":"부실한",positive?"최대 내구도 +10%":"최대 내구도 -10%");
        String[] a=choices.get(r.nextInt(choices.size())); return new Option("effect:"+a[0],1,a[1],"장착 중 "+a[2]+" I · 같은 효과 중첩 없음");
    }
    private static String family(String key) {return switch(key){case "speed","draw"->"speed";case "movement","effect:speed","effect:slowness"->"movement";case "damage","effect:weakness"->"damage";case "mining","effect:haste","effect:mining_fatigue"->"mining";case "armor","effect:resistance"->"armor";case "safe_fall","fall_damage"->"fall";default->key;};}
    private static boolean conflict(String key,List<Option> previous) {return previous.stream().anyMatch(o->family(o.key()).equals(family(key)));}
    private static boolean applicable(ItemStack s,String key,boolean positive) {
        return switch(key) {
            case "damage"->melee(s)||ranged(s);
            case "speed","reach"->melee(s);
            case "mining","block_reach"->mining(s);
            case "draw"->s.getItem() instanceof BowItem||s.getItem() instanceof CrossbowItem;
            case "durability"->true;
            case "armor"->armor(s)&&baseAttribute(s,Attributes.ARMOR,0)>0;
            case "toughness"->armor(s)&&baseAttribute(s,Attributes.ARMOR_TOUGHNESS,0)>0;
            case "knockback"->armor(s)&&(positive||baseAttribute(s,Attributes.KNOCKBACK_RESISTANCE,0)>0);
            case "water_move"->armor(s)&&positive;
            case "movement","health"->true;
            case "safe_fall","fall_damage","water_mining"->armor(s);
            default->false;
        };
    }
    private static EquipmentSlot slot(ItemStack s) {var e=s.get(DataComponents.EQUIPPABLE);return armor(s)&&e!=null?e.slot():EquipmentSlot.MAINHAND;}
    private static double baseAttribute(ItemStack s,Holder<Attribute> a,double base) {return s.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY).compute(a,base,slot(s));}
    public static void clean(ItemStack s) {
        GearData old=data(s); if(old==null)return;
        boolean transform=!old.sourceItem().equals(itemId(s));
        ItemAttributeModifiers modifiers=s.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY);
        List<ItemAttributeModifiers.Entry> entries=new ArrayList<>(modifiers.modifiers().stream().filter(e->!e.modifier().id().getNamespace().equals(TagMod.ID)).toList());
        if(transform) { // Vanilla transforms copy component patches; update original vanilla equipment stats.
            var defaults=s.getItem().components().getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY);
            Item source=BuiltInRegistries.ITEM.getValue(Identifier.parse(old.sourceItem()));
            var sourceDefaults=source==null?ItemAttributeModifiers.EMPTY:source.components().getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY);
            entries.removeIf(e->sourceDefaults.modifiers().stream().anyMatch(original->original.attribute().equals(e.attribute())&&original.modifier().id().equals(e.modifier().id())&&original.slot()==e.slot()));
            entries.addAll(defaults.modifiers());
        }
        s.set(DataComponents.ATTRIBUTE_MODIFIERS,new ItemAttributeModifiers(entries));
        double damageFraction=s.getMaxDamage()>0?(double)s.getDamageValue()/s.getMaxDamage():0;
        int base=transform?s.getItem().components().getOrDefault(DataComponents.MAX_DAMAGE,old.baseDurability()):old.baseDurability();
        s.set(DataComponents.MAX_DAMAGE,Math.max(1,base));s.setDamageValue(Math.min(base-1,(int)Math.round(base*damageFraction)));
        if(old.options().stream().anyMatch(o->o.key().equals("reach"))) {
            if(old.originalRange()==null)s.remove(DataComponents.ATTACK_RANGE);
            else try{s.set(DataComponents.ATTACK_RANGE,AttackRange.CODEC.parse(JsonOps.INSTANCE,com.google.gson.JsonParser.parseString(old.originalRange())).getOrThrow());}catch(Exception e){s.remove(DataComponents.ATTACK_RANGE);}
        }
        CustomData.update(DataComponents.CUSTOM_DATA,s,t->t.remove("tagmod"));
    }
    private static void apply(ItemStack s,GearData d) {
        var modifiers=s.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY);
        EquipmentSlotGroup group=path(s).equals("shield")?EquipmentSlotGroup.HAND:EquipmentSlotGroup.bySlot(slot(s));
        for(Option o:d.options()) {
            String key=o.key();double v=o.value();Holder<Attribute> a=null;
            AttributeModifier.Operation operation=AttributeModifier.Operation.ADD_VALUE;
            switch(key) {
                case "damage"->{if(melee(s)){a=Attributes.ATTACK_DAMAGE;v=baseAttribute(s,a,1)*v;}}
                case "speed"->{a=Attributes.ATTACK_SPEED;v=baseAttribute(s,a,4)*v;}
                case "mining"->{a=Attributes.BLOCK_BREAK_SPEED;operation=AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;}
                case "block_reach"->a=Attributes.BLOCK_INTERACTION_RANGE;
                case "armor"->{a=Attributes.ARMOR;v=baseAttribute(s,a,0)*v;}
                case "toughness"->{a=Attributes.ARMOR_TOUGHNESS;v=baseAttribute(s,a,0)*v;}
                case "movement"->{a=Attributes.MOVEMENT_SPEED;operation=AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;}
                case "health"->a=Attributes.MAX_HEALTH;
                case "knockback"->a=Attributes.KNOCKBACK_RESISTANCE;
                case "safe_fall"->a=Attributes.SAFE_FALL_DISTANCE;
                case "fall_damage"->{a=Attributes.FALL_DAMAGE_MULTIPLIER;operation=AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;}
                case "water_mining"->{a=Attributes.SUBMERGED_MINING_SPEED;operation=AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;}
                case "water_move"->a=Attributes.WATER_MOVEMENT_EFFICIENCY;
                case "durability"->{
                    double ratio=(double)s.getDamageValue()/s.getMaxDamage();int max=Math.max(1,(int)Math.round(d.baseDurability()*(1+v)));
                    s.set(DataComponents.MAX_DAMAGE,max);s.setDamageValue(Math.min(max-1,(int)Math.round(max*ratio)));
                }
                case "reach"->{
                    AttackRange b=s.getOrDefault(DataComponents.ATTACK_RANGE,new AttackRange(0,3,0,5,.3f,1));
                    s.set(DataComponents.ATTACK_RANGE,new AttackRange(b.minReach(),Math.max(b.minReach(),Math.min(64,b.maxReach()+(float)v)),b.minCreativeReach(),Math.max(b.minCreativeReach(),Math.min(64,b.maxCreativeReach()+(float)v)),b.hitboxMargin(),b.mobFactor()));
                }
            }
            if(a!=null)modifiers=modifiers.withModifierAdded(a,new AttributeModifier(TagMod.id("gear_"+group.getSerializedName()+"_"+key),v,operation),group);
        }
        s.set(DataComponents.ATTRIBUTE_MODIFIERS,modifiers);
    }
    public static Component name(ItemStack s,Component vanilla) {
        GearData d=data(s);if(d==null)return vanilla;
        ChatFormatting color=switch(d.category()){case "유리"->ChatFormatting.GREEN;case "불리"->ChatFormatting.RED;default->ChatFormatting.GOLD;};
        return Component.literal(d.options().getFirst().prefix()+" ").withStyle(color).append(vanilla.copy());
    }
    public static void tooltip(ItemStack s,List<Component> lines) {
        GearData d=data(s);if(d==null)return;
        lines.add(Component.literal("변덕의 특성 · "+d.category()+" · "+d.options().size()+"개").withStyle(ChatFormatting.GOLD));
        for(Option o:d.options())lines.add(Component.literal(" • "+o.description()).withStyle(ChatFormatting.GRAY));
    }
}
