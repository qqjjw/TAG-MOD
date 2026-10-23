package dev.tagmod;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.network.chat.Component;
import java.nio.file.*;
import java.util.*;

public final class CoreTests {
    private static int checks;
    static void check(boolean condition,String label){checks++;if(!condition)throw new AssertionError(label);}
    public static void main(String[] args) throws Exception {
        if(args.length==1&&args[0].equals("--export-default")){Files.writeString(Path.of("config/tagmod.json"),TagMod.JSON.toJson(new Config()));return;}
        SharedConstants.tryDetectVersion();Bootstrap.bootStrap();
        var registries=net.minecraft.data.registries.VanillaRegistries.createReloadableLookup(net.minecraft.data.registries.VanillaRegistries.createWorldLookup());
        net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries).forEach(net.minecraft.core.component.DataComponentInitializers.PendingComponents::apply);
        distribution();config();gear();
        if(Boolean.getBoolean("tagmod.mixinTests")) {
            new TagMod().onInitialize();
            for(String type:List.of("net.minecraft.world.entity.LivingEntity","net.minecraft.world.entity.Mob","net.minecraft.world.entity.player.Inventory","net.minecraft.world.inventory.AnvilMenu","net.minecraft.world.inventory.SmithingMenu","net.minecraft.world.inventory.GrindstoneMenu","net.minecraft.world.inventory.AbstractContainerMenu","net.minecraft.world.item.crafting.RepairItemRecipe","net.minecraft.world.item.BowItem","net.minecraft.world.item.CrossbowItem"))Class.forName(type);
            ItemStack s=new ItemStack(Items.IRON_SWORD);Gear.roll(s,42);
            check(s.getHoverName().getString().contains(Gear.data(s).options().getFirst().prefix()),"transformed item name");
        }
        System.out.println("TAGMOD TESTS PASSED: "+checks+" checks");
        if(Boolean.getBoolean("tagmod.writeDefaultConfig"))Files.writeString(Path.of("config/tagmod.json"),TagMod.JSON.toJson(new Config()));
    }
    private static void distribution() {
        Config c=new Config();Random r=new Random(1234567);int[] counts=new int[3];
        for(int i=0;i<90000;i++){int cat=Rolls.category(r,c);counts[cat]++;int n=Rolls.count(r,cat);check(n>=1&&n<=3&&(cat!=2||n>=2),"option count");}
        for(int n:counts)check(n>29000&&n<31000,"balanced categories");
        check(Rolls.weighted(r,0,0)==-1,"empty weight handling");
    }
    private static void config() throws Exception {
        Path directory=Files.createTempDirectory(Path.of(".cache"),"config-test-");Path file=directory.resolve("tagmod.json");
        Config defaults=Config.load(file);check(Files.exists(file)&&defaults.mobChance==.50,"default config created");
        Files.writeString(file,"{\"mobChance\":2,\"bossChance\":0.2,\"invisibleMinSeconds\":-1,\"ranges\":{\"damage\":[0.1,0.2],\"durability\":[0.4,0.1]},\"excludedItems\":[\"minecraft:bow\"]}");
        Config c=Config.load(file);check(c.mobChance==.50&&c.bossChance==.2&&c.invisibleMinSeconds==20,"per-field fallback");
        check(c.ranges.get("durability")[0]==.1&&c.ranges.get("damage")[0]==.1,"range fallback");check(c.excludedItems.contains("minecraft:bow"),"item exclusion config");
        Files.writeString(file,"broken");check(Config.load(file).mobChance==.50,"malformed config fallback");
    }
    private static void gear() throws Exception {
        TagMod.config=new Config();
        List<Item> items=List.of(Items.IRON_SWORD,Items.DIAMOND_PICKAXE,Items.IRON_HELMET,Items.DIAMOND_CHESTPLATE,Items.NETHERITE_BOOTS,Items.BOW,Items.CROSSBOW,Items.TRIDENT,Items.SHIELD,Items.ELYTRA,Items.FISHING_ROD,Items.SHEARS,Items.FLINT_AND_STEEL,Items.MACE,Items.BRUSH);
        for(Item item:items)for(int i=0;i<200;i++) {
            ItemStack s=new ItemStack(item);int base=s.getMaxDamage();s.setDamageValue(base/2);
            Component name=Component.literal("내 장비");s.set(DataComponents.CUSTOM_NAME,name);
            CustomData.update(DataComponents.CUSTOM_DATA,s,t->t.putString("other_mod","preserve"));
            Gear.roll(s,i);GearData d=Gear.data(s);
            check(d!=null&&d.options().size()>=1&&d.options().size()<=3,"eligible item gets options: "+item);
            check(d.options().stream().map(Option::key).distinct().count()==d.options().size(),"unique option keys");
            check(s.get(DataComponents.CUSTOM_NAME).equals(name),"custom name retained");
            check(s.get(DataComponents.CUSTOM_DATA).copyTag().getStringOr("other_mod","").equals("preserve"),"other custom data retained");
            check(Math.abs((double)s.getDamageValue()/s.getMaxDamage()-.5)<.03,"wear fraction preserved");
            ItemStack copy=s.copy();check(Gear.data(copy).equals(d)&&!Gear.ensure(copy),"copy and reacquisition retain roll");
            ItemStack encoded=ItemStack.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,ItemStack.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,s).getOrThrow()).getOrThrow();
            check(Gear.data(encoded).equals(d),"item serialization retains roll");
            long stable=Preview.seed(77,s,copy);check(stable==Preview.seed(77,s,copy)&&stable==Preview.seed(77,copy,s),"preview reopen and slot order stable");
            Gear.clean(s);check(s.getMaxDamage()==base&&Gear.data(s)==null,"clean restores base durability");
            check(s.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY).modifiers().stream().noneMatch(e->e.modifier().id().getNamespace().equals("tagmod")),"clean removes only owned modifiers");
        }
        ItemStack s=new ItemStack(Items.IRON_SWORD);Gear.roll(s,21);Gear.roll(s,21);GearData first=Gear.data(s);
        for(int i=0;i<50;i++)Gear.roll(s,21);
        check(first.equals(Gear.data(s)),"rerolls never accumulate metadata");
        double attack=s.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY).compute(Attributes.ATTACK_DAMAGE,1,EquipmentSlot.MAINHAND);
        check(attack>0&&attack<10,"rerolls never accumulate damage");
        check(!Gear.eligible(new ItemStack(Items.STONE)),"blocks excluded");
        for(Item item:items) {
            ItemStack source=new ItemStack(item);Gear.roll(source,83217);
            var access=net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY);
            var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),access);
            try {
                ItemStack.STREAM_CODEC.encode(buffer,source);ItemStack received=ItemStack.STREAM_CODEC.decode(buffer);
                check(Gear.data(source).equals(Gear.data(received)),"vanilla network stream preserves roll");
                check(source.getComponents().equals(received.getComponents()),"vanilla network stream preserves effective stats");
            } finally {buffer.release();}
        }
        ItemStack tough=new ItemStack(Items.DIAMOND_CHESTPLATE);
        var originalAttributes=tough.get(DataComponents.ATTRIBUTE_MODIFIERS);
        var customId=TagMod.id("foreign_test_modifier");
        var external=new net.minecraft.world.entity.ai.attributes.AttributeModifier(net.minecraft.resources.Identifier.parse("another_mod:armor"),2,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE);
        tough.set(DataComponents.ATTRIBUTE_MODIFIERS,originalAttributes.withModifierAdded(Attributes.ARMOR,external,net.minecraft.world.entity.EquipmentSlotGroup.CHEST));
        for(int i=0;i<100;i++)Gear.roll(tough,i);
        check(tough.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().stream().anyMatch(e->e.modifier().equals(external)),"other mod attributes retained across rerolls");
        var attackRange=new AttackRange(0,4,0,6,.2f,1);
        ItemStack longSword=new ItemStack(Items.IRON_SWORD);longSword.set(DataComponents.ATTACK_RANGE,attackRange);
        for(int i=0;i<100;i++)Gear.roll(longSword,i);
        Gear.clean(longSword);check(longSword.get(DataComponents.ATTACK_RANGE).equals(attackRange),"custom range restored after repeated rerolls");
        ItemStack upgraded=new ItemStack(Items.DIAMOND_CHESTPLATE);Gear.roll(upgraded,33);
        ItemStack netherite=upgraded.transmuteCopy(Items.NETHERITE_CHESTPLATE);Gear.roll(netherite,44);Gear.clean(netherite);
        check(netherite.getMaxDamage()==new ItemStack(Items.NETHERITE_CHESTPLATE).getMaxDamage(),"upgrade gets target durability");
        check(netherite.get(DataComponents.ATTRIBUTE_MODIFIERS).equals(new ItemStack(Items.NETHERITE_CHESTPLATE).get(DataComponents.ATTRIBUTE_MODIFIERS)),"upgrade gets target armor attributes");
        Config zero=new Config();zero.optionWeights.replaceAll((key,weight)->0.0);zero.potionOptionWeight=0;zero.lifestealOptionWeight=0;TagMod.config=zero;
        for(int i=0;i<30;i++){ItemStack fallback=new ItemStack(Items.SHIELD);Gear.roll(fallback,i);var options=Gear.data(fallback).options();check(options.stream().map(Option::key).distinct().count()==options.size(),"zero pool fallback retains unique options");}
        TagMod.config=new Config();
        TagMod.config.excludedItems.add("minecraft:iron_sword");check(!Gear.eligible(s),"configured exclusions honored");TagMod.config=new Config();
    }
}
