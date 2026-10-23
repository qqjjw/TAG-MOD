package dev.tagmod;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Only the logical server reads gameplay configuration. Clients display stored results. */
public final class Config {
    public double mobChance = .50, doubleTraitChance = .20, bossChance = .10;
    public double equipmentChance = .20, equipmentDropChance = .005;
    public double largeMobChance = .08, largeMobMinScale = 1.6, largeMobMaxScale = 2.0;
    public int invisibleMinSeconds = 20, invisibleMaxSeconds = 40, invisibleSeconds = 2;
    public double beneficialWeight = 1, harmfulWeight = 1, mixedWeight = 1;
    public double severeEffectChance = .10, potionOptionWeight = 1, lifestealOptionWeight = 1;
    public double lifestealRatio = .03, lifestealCap = 1;
    public int lifestealCooldownTicks = 20;
    public Map<String, double[]> ranges = new LinkedHashMap<>();
    public Map<String, Double> optionWeights = new LinkedHashMap<>();
    public Map<String, Double> mobEffectWeights = new LinkedHashMap<>();
    public List<String> extraMobTypes = new ArrayList<>(), excludedMobTypes = new ArrayList<>();
    public List<String> extraItems = new ArrayList<>(), excludedItems = new ArrayList<>();
    public Config() {
        for (String key : List.of("damage", "speed", "mining", "draw", "armor", "toughness", "water_mining", "water_move")) ranges.put(key, new double[]{.05,.20});
        ranges.put("durability", new double[]{.10,.30});
        ranges.put("reach", new double[]{.25,.5}); ranges.put("block_reach", new double[]{.25,.5});
        ranges.put("movement", new double[]{.02,.05}); ranges.put("health", new double[]{1,2});
        ranges.put("knockback", new double[]{.02,.08}); ranges.put("safe_fall", new double[]{1,2});
        ranges.put("fall_damage", new double[]{.05,.20});
        for (String key : ranges.keySet()) optionWeights.put(key, 1.0);
        for (String key : List.of("speed","resistance","jump","strength","fire_resistance","invisibility")) mobEffectWeights.put(key,1.0);
    }
    public static Config load(Path path) {
        Config defaults = new Config();
        JsonObject baseline = TagMod.JSON.toJsonTree(defaults).getAsJsonObject();
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) Files.writeString(path,TagMod.JSON.toJson(defaults),StandardCharsets.UTF_8);
            JsonObject input = JsonParser.parseString(Files.readString(path,StandardCharsets.UTF_8)).getAsJsonObject();
            for (String key : baseline.keySet()) {
                if (!input.has(key)) continue;
                JsonElement candidate = input.get(key), fallback = baseline.get(key);
                try {
                    if (fallback.isJsonPrimitive()) {
                        double n = candidate.getAsDouble();
                        boolean probability = key.endsWith("Chance");
                        boolean ticks = key.endsWith("Seconds") || key.endsWith("Ticks");
                        if (key.equals("largeMobMinScale") || key.equals("largeMobMaxScale")) {
                            if (!Double.isFinite(n) || n < 1 || n > 4) throw new IllegalArgumentException("크기는 1~4배 범위여야 함");
                        }
                        if (!Double.isFinite(n) || n < 0 || (probability && n > 1) || (ticks && (n < 1 || n > 72000 || n != Math.floor(n))) || (!probability && !ticks && n > 100)) throw new IllegalArgumentException("허용 범위 밖");
                        baseline.add(key,candidate);
                    } else if (fallback.isJsonArray()) {
                        List<String> ids = new ArrayList<>();
                        for (JsonElement e : candidate.getAsJsonArray()) {
                            String id=e.getAsString();
                            if (!id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) throw new IllegalArgumentException("잘못된 ID: " + id);
                            ids.add(id);
                        }
                        baseline.add(key,TagMod.JSON.toJsonTree(ids));
                    } else {
                        JsonObject merged = fallback.deepCopy().getAsJsonObject();
                        for (String entry : merged.keySet()) if (candidate.getAsJsonObject().has(entry)) {
                            try {
                                JsonElement v=candidate.getAsJsonObject().get(entry);
                                if (key.equals("ranges")) {
                                    JsonArray a=v.getAsJsonArray(); double lo=a.get(0).getAsDouble(), hi=a.get(1).getAsDouble();
                                    boolean fractional=!Set.of("health","safe_fall","reach","block_reach").contains(entry);
                                    if (a.size()!=2 || !Double.isFinite(lo) || !Double.isFinite(hi) || lo<0 || hi<lo || hi>(fractional?.9:64)) throw new IllegalArgumentException("[최솟값, 최댓값] 범위 오류");
                                } else if (!Double.isFinite(v.getAsDouble()) || v.getAsDouble()<0 || v.getAsDouble()>10000) throw new IllegalArgumentException("가중치 범위 오류");
                                merged.add(entry,v);
                            } catch (Exception e) { TagMod.LOG.warn("설정 {}.{}: {} — 기본값 사용",key,entry,e.getMessage()); }
                        }
                        baseline.add(key,merged);
                    }
                } catch (Exception e) { TagMod.LOG.warn("설정 {}: {} — 기본값 사용",key,e.getMessage()); }
            }
            Config c=TagMod.JSON.fromJson(baseline,Config.class);
            if (c.invisibleMaxSeconds<c.invisibleMinSeconds) { TagMod.LOG.warn("invisibleMaxSeconds가 최솟값보다 작음: 기본 간격 사용"); c.invisibleMinSeconds=20;c.invisibleMaxSeconds=40; }
            if (c.largeMobMaxScale<c.largeMobMinScale) { TagMod.LOG.warn("largeMobMaxScale이 최솟값보다 작음: 기본 크기 사용"); c.largeMobMinScale=1.6;c.largeMobMaxScale=2.0; }
            if (c.beneficialWeight+c.harmfulWeight+c.mixedWeight==0) { TagMod.LOG.warn("유형 가중치 합계가 0: 기본값 사용");c.beneficialWeight=c.harmfulWeight=c.mixedWeight=1; }
            return c;
        } catch (Exception e) { TagMod.LOG.warn("설정 {} 읽기 실패: {} — 기본값 사용 (원본 파일 보존)",path,e.toString()); return defaults; }
    }
    public double amount(String key, Random r) { double[] a=ranges.get(key); return a[0]+r.nextDouble()*(a[1]-a[0]); }
}
