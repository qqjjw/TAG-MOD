package dev.tagmod;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.nio.ByteBuffer;
import java.util.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;

public final class Preview {
    /** Stable across menu reopen, input replacement, config reload, and server restart. */
    public static long seed(long worldSeed,ItemStack... inputs) {
        try {
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            List<String> descriptors=new ArrayList<>();
            for(ItemStack s:inputs)if(!s.isEmpty()) {
                GearData d=Gear.data(s);
                descriptors.add(Gear.itemId(s)+"/"+s.getDamageValue()+"/"+s.getMaxDamage()+"/"+(d==null?"unrolled":d.roll()));
            }
            Collections.sort(descriptors);digest.update(Long.toString(worldSeed).getBytes(StandardCharsets.UTF_8));
            for(String text:descriptors){digest.update((byte)0);digest.update(text.getBytes(StandardCharsets.UTF_8));}
            return ByteBuffer.wrap(digest.digest()).getLong();
        }catch(java.security.NoSuchAlgorithmException e){throw new AssertionError(e);}
    }
    public static void reroll(Player player,ItemStack result,ItemStack... inputs) {
        if(!TagMod.onServerThread()||result.isEmpty()||!Gear.eligible(result))return;
        Gear.roll(result,seed(TagMod.server.overworld().getSeed(),inputs));
    }
    public static ItemStack combine(Player player,ItemStack vanilla,ItemStack first,ItemStack second) {
        if(!TagMod.onServerThread()||vanilla.isEmpty()||!Gear.eligible(first)||second.isEmpty()||first.getItem()!=second.getItem())return vanilla;
        ItemStack result=first.copyWithCount(1);
        result.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS,vanilla.getEnchantments());
        double remaining=(1-(double)first.getDamageValue()/first.getMaxDamage())+(1-(double)second.getDamageValue()/second.getMaxDamage())+.05;
        result.setDamageValue(Math.max(0,(int)Math.round(first.getMaxDamage()*(1-Math.min(1,remaining)))));
        reroll(player,result,first,second);return result;
    }
}
