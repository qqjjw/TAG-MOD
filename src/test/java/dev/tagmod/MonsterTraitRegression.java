package dev.tagmod;

import java.util.*;

/** One targeted regression; no game launch, world, or broad randomized test suite. */
public final class MonsterTraitRegression {
    public static void main(String[] args) {
        String ownership="tagmod:gear_effect:resistance";
        Set<String> tags=new LinkedHashSet<>(List.of(Monsters.PROCESSED,Monsters.PREFIX+"resistance",ownership,Monsters.PREFIX+"speed",Monsters.PREFIX+"invisibility"));
        List<String> visited=new ArrayList<>();
        Monsters.forEachPersistentTrait(tags,key->{
            visited.add(key);
            // Mirrors the effect hook removing its ownership marker and adding new state.
            tags.remove(ownership);
            tags.add(Monsters.PREFIX+"jump");
        });
        if(!visited.equals(List.of("resistance","speed"))||tags.contains(ownership)||!tags.contains(Monsters.PREFIX+"jump"))throw new AssertionError("Trait snapshot changed during callbacks: "+visited);
        System.out.println("PASS: monster trait callbacks may mutate entity tags without invalidating iteration (1 scenario)");
    }
}
