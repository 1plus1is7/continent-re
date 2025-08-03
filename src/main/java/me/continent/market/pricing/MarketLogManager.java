package me.continent.market.pricing;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Stores sale logs per enterprise for market transactions.
 */
public class MarketLogManager {
    public static class SaleLog {
        private final Material material;
        private final int amount;
        private final int price;
        private final long time;
        public SaleLog(Material material, int amount, int price, long time) {
            this.material = material;
            this.amount = amount;
            this.price = price;
            this.time = time;
        }
        public Material getMaterial() { return material; }
        public int getAmount() { return amount; }
        public int getPrice() { return price; }
        public long getTime() { return time; }
    }

    private static final Map<String, List<SaleLog>> logs = new HashMap<>();
    private static final int MAX_LOGS = 9;

    /** Record a sale for the given enterprise. */
    public static void recordSale(String enterpriseId, ItemStack item, int amount, int price) {
        List<SaleLog> list = logs.computeIfAbsent(enterpriseId, k -> new ArrayList<>());
        list.add(0, new SaleLog(item.getType(), amount, price, System.currentTimeMillis()));
        if (list.size() > MAX_LOGS) list.remove(list.size() - 1);
    }

    /** Get sale logs for an enterprise. */
    public static List<SaleLog> getLogs(String enterpriseId) {
        return logs.getOrDefault(enterpriseId, Collections.emptyList());
    }

    /** Total revenue from recorded logs. */
    public static int getTotalRevenue(String enterpriseId) {
        return logs.getOrDefault(enterpriseId, Collections.emptyList())
                .stream().mapToInt(SaleLog::getPrice).sum();
    }

    /** Save logs to file. */
    public static void save(File file) {
        FileConfiguration config = new YamlConfiguration();
        for (var e : logs.entrySet()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (SaleLog log : e.getValue()) {
                Map<String, Object> map = new HashMap<>();
                map.put("material", log.getMaterial().name());
                map.put("amount", log.getAmount());
                map.put("price", log.getPrice());
                map.put("time", log.getTime());
                list.add(map);
            }
            config.set(e.getKey(), list);
        }
        try {
            config.save(file);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /** Load logs from file. */
    public static void load(File file) {
        logs.clear();
        if (!file.exists()) return;
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            List<Map<?, ?>> list = config.getMapList(key);
            List<SaleLog> sl = new ArrayList<>();
            for (Map<?, ?> map : list) {
                try {
                    Material mat = Material.valueOf((String) map.get("material"));
                    int amount = ((Number) map.get("amount")).intValue();
                    int price = ((Number) map.get("price")).intValue();
                    long time = ((Number) map.get("time")).longValue();
                    sl.add(new SaleLog(mat, amount, price, time));
                } catch (Exception ignored) {}
            }
            logs.put(key, sl);
        }
    }
}

