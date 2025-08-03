package me.continent.market.pricing;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Tracks recent supply for items and provides demand coefficient.
 * Simple implementation where coefficient ranges 0.5-1.5 around a baseline
 * threshold. Real implementation may persist data and decay over time.
 */
public class DemandManager {
    private static final Map<String, Integer> supplyMap = new HashMap<>();
    private static final int THRESHOLD = 100; // baseline supply

    /** Record supplied amount for an item key. */
    public static void recordSupply(String key, int amount) {
        supplyMap.merge(key, amount, Integer::sum);
    }

    /**
     * Returns demand coefficient based on recent supply.
     * Less supply => coefficient >1, more supply => coefficient <1
     */
    public static double getDemandCoefficient(String key) {
        int supply = supplyMap.getOrDefault(key, 0);
        double coeff;
        if (supply > THRESHOLD) {
            coeff = 1.0 - (supply - THRESHOLD) / (double) THRESHOLD * 0.5; // down to 0.5
        } else {
            coeff = 1.0 + (THRESHOLD - supply) / (double) THRESHOLD * 0.5; // up to 1.5
        }
        if (coeff < 0.5) coeff = 0.5;
        if (coeff > 1.5) coeff = 1.5;
        return coeff;
    }

    /** Reduce stored supply slightly to model daily decay. */
    public static void decay() {
        supplyMap.replaceAll((k, v) -> (int) Math.max(0, v * 0.9));
    }

    /** Persist demand data to the given file. */
    public static void save(File file) {
        FileConfiguration config = new YamlConfiguration();
        for (var e : supplyMap.entrySet()) {
            config.set(e.getKey(), e.getValue());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Load demand data from file. */
    public static void load(File file) {
        supplyMap.clear();
        if (!file.exists()) return;
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            supplyMap.put(key, config.getInt(key));
        }
    }
}
