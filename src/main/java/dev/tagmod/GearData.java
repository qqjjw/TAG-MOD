package dev.tagmod;

import java.util.List;

public record GearData(int version, long roll, String sourceItem, String category, List<Option> options,
                       int baseDurability, String originalRange, double lifestealCap, int lifestealCooldownTicks) {
    public static final int VERSION=1;
}
