package dev.tagmod;

import java.util.*;

/** Registry-independent weighted draws, also exercised by the distribution tests. */
public final class Rolls {
    private Rolls() {}
    public static int weighted(Random r, double... weights) {
        double total=Arrays.stream(weights).sum();
        if (!(total>0)) return -1;
        double n=r.nextDouble()*total;
        for (int i=0;i<weights.length;i++) { n-=weights[i]; if(n<0) return i; }
        return weights.length-1;
    }
    public static int category(Random r, Config c) { return weighted(r,c.beneficialWeight,c.harmfulWeight,c.mixedWeight); }
    public static int count(Random r,int category) { return category==2 ? 2+r.nextInt(2) : 1+r.nextInt(3); }
    public static long seed(long salt, int fingerprint) {
        long x=salt^Integer.toUnsignedLong(fingerprint); x=(x^(x>>>30))*0xbf58476d1ce4e5b9L;
        x=(x^(x>>>27))*0x94d049bb133111ebL; return x^(x>>>31);
    }
}
