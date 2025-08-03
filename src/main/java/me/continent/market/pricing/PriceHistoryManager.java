package me.continent.market.pricing;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Tracks recent unit prices for items to display fluctuation history.
 */
public class PriceHistoryManager {
    private static final Map<Material, Deque<Integer>> history = new HashMap<>();
    private static final int MAX = 9;

    /** Record price for a material. */
    public static void record(Material mat, int price) {
        Deque<Integer> deque = history.computeIfAbsent(mat, k -> new ArrayDeque<>());
        deque.addFirst(price);
        while (deque.size() > MAX) deque.removeLast();
    }

    /** Get price history for a material, most recent first. */
    public static List<Integer> getHistory(Material mat) {
        Deque<Integer> deque = history.get(mat);
        return deque == null ? Collections.emptyList() : new ArrayList<>(deque);
    }

    /** Save history to file. */
    public static void save(File file) {
        FileConfiguration config = new YamlConfiguration();
        for (var e : history.entrySet()) {
            config.set(e.getKey().name(), new ArrayList<>(e.getValue()));
        }
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Load history from file. */
    public static void load(File file) {
        history.clear();
        if (!file.exists()) return;
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            List<Integer> list = config.getIntegerList(key);
            history.put(Material.valueOf(key), new ArrayDeque<>(list));
        }
    }
}
