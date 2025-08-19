package me.continent.biome;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads biome traits from YAML and provides cached lookups.
 */
public class BiomeTraitService {
    private static JavaPlugin plugin;
    private static volatile Map<Biome, BiomeTrait> ACTIVE_CACHE = Map.of();
    private static final int WARN_LIMIT = 20;

    public static void init(JavaPlugin pl) {
        plugin = pl;
        reload();
    }

    public static void reload() {
        if (plugin == null) return;
        LoadResult result = loadFromFile();
        ACTIVE_CACHE = Collections.unmodifiableMap(result.cache());
        plugin.getLogger().info("Loaded biome traits: " + result.loaded() + "/" + result.total());
    }

    public static void reloadAsync(CommandSender sender) {
        if (plugin == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            long start = System.currentTimeMillis();
            LoadResult result;
            try {
                result = loadFromFile();
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage("§c[Continent] Reload failed: " + e.getMessage()));
                plugin.getLogger().log(Level.SEVERE, "Biome trait reload failed", e);
                return;
            }
            long elapsed = System.currentTimeMillis() - start;
            Bukkit.getScheduler().runTask(plugin, () -> {
                ACTIVE_CACHE = Collections.unmodifiableMap(result.cache());
                sender.sendMessage("§a[Continent] Biome traits reloaded: " + result.loaded() + "/" + result.total() + " (skipped: " + result.skipped() + ", warnings: " + result.warnings() + ") in " + elapsed + "ms");
                if (result.warnings() > 0) {
                    sender.sendMessage("§e[Continent] Reload partial: warnings=" + result.warnings() + ", see console");
                }
                plugin.getLogger().info("Biome traits reload completed in " + elapsed + "ms");
            });
        });
    }

    private static LoadResult loadFromFile() {
        Map<Biome, BiomeTrait> newCache = new HashMap<>();
        int skipped = 0;
        int warnings = 0;
        File dir = new File(plugin.getDataFolder(), "config");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Cannot create config directory");
        }
        File file = new File(dir, "biome_traits.yml");
        if (!file.exists()) {
            plugin.saveResource("config/biome_traits.yml", false);
        }
        Logger log = plugin.getLogger();
        Map<String, Object> root;
        try (FileReader reader = new FileReader(file)) {
            root = new Yaml().load(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read biome_traits.yml", e);
        }
        if (root == null) root = Map.of();
        for (Map.Entry<String, Object> entry : root.entrySet()) {
            String key = entry.getKey();
            Biome biome;
            try {
                biome = Biome.valueOf(key.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                if (warnings < WARN_LIMIT) log.warning("Unknown biome: " + key);
                skipped++;
                continue;
            }
            Map<String, Object> section = (Map<String, Object>) entry.getValue();
            double baseTemp = getDouble(section, "base_temp", 20.0, 0.0, 100.0, log);
            double moveMult = getDouble(section, "move_mult", 1.0, 0.1, 5.0, log);
            double cropRate = getDouble(section, "crop_rate", 1.0, 0.1, 5.0, log);
            double cropYield = getDouble(section, "crop_yield_rate", 1.0, 0.1, 5.0, log);

            Map<String, Object> deformSec = (Map<String, Object>) section.getOrDefault("trade_deform", Map.of());
            double food = getDouble(deformSec, "food", 0.0, -1.0, 1.0, log);
            double metal = getDouble(deformSec, "metal", 0.0, -1.0, 1.0, log);
            double leather = getDouble(deformSec, "leather", 0.0, -1.0, 1.0, log);
            double gem = getDouble(deformSec, "gem", 0.0, -1.0, 1.0, log);

            Set<String> tags = new HashSet<>();
            Object tagObj = section.get("tags");
            if (tagObj instanceof Iterable<?> it) {
                for (Object o : it) {
                    if (o != null) tags.add(o.toString());
                }
            }

            List<BiomeTrait.Rule> rules = new ArrayList<>();
            Object rulesObj = section.get("special_rules");
            if (rulesObj instanceof Iterable<?> it) {
                for (Object o : it) {
                    if (o instanceof Map<?, ?> map) {
                        String typeStr = Objects.toString(map.get("type"), "");
                        BiomeTrait.RuleType type;
                        try {
                            type = BiomeTrait.RuleType.valueOf(typeStr.toUpperCase(Locale.ROOT));
                        } catch (IllegalArgumentException ex) {
                            if (warnings < WARN_LIMIT) log.warning("Unknown rule type: " + typeStr + " in " + key);
                            warnings++;
                            continue;
                        }
                        Map<String, Object> params = new HashMap<>();
                        switch (type) {
                            case LOW_LIGHT_SLOW -> {
                                int lightMax = (int) getDouble((Map<String, Object>) map, "light_max", 5, 0, 15, log);
                                double mult = getDouble((Map<String, Object>) map, "move_mult", 0.90, 0.1, 5.0, log);
                                params.put("light_max", lightMax);
                                params.put("move_mult", mult);
                            }
                            case LEATHER_IMMUNITY -> {
                                // no params
                            }
                            case ELEVATION_TRADE_BONUS -> {
                                int yMin = (int) getDouble((Map<String, Object>) map, "y_min", 100, 0, 256, log);
                                int radius = (int) getDouble((Map<String, Object>) map, "radius", 200, 1, 500, log);
                                double valMult = getDouble((Map<String, Object>) map, "value_mult", 1.0, 0.1, 5.0, log);
                                String item = Objects.toString(map.get("item"), "sweet_berries");
                                params.put("y_min", yMin);
                                params.put("radius", radius);
                                params.put("value_mult", valMult);
                                params.put("item", item);
                            }
                        }
                        rules.add(new BiomeTrait.Rule(type, params));
                    }
                }
            }

            BiomeTrait trait = new BiomeTrait(
                    (float) baseTemp,
                    (float) moveMult,
                    (float) cropRate,
                    (float) cropYield,
                    new BiomeTrait.TradeDeform((float) food, (float) metal, (float) leather, (float) gem),
                    Collections.unmodifiableSet(tags),
                    Collections.unmodifiableList(rules)
            );
            newCache.put(biome, trait);
        }
        return new LoadResult(Map.copyOf(newCache), newCache.size(), root.size(), skipped, warnings);
    }

    private static double getDouble(Map<String, Object> sec, String key, double def, double min, double max, Logger log) {
        Object valObj = sec.get(key);
        if (valObj == null) return def;
        double value;
        try {
            value = Double.parseDouble(valObj.toString());
        } catch (NumberFormatException e) {
            log.warning("Invalid number for " + key + ": " + valObj);
            return def;
        }
        if (value < min || value > max) {
            log.warning("Out of range for " + key + ": " + value);
            value = Math.max(min, Math.min(max, value));
        }
        return value;
    }

    public static BiomeTrait get(Player player) {
        if (player == null) return BiomeTrait.DEFAULT;
        return get(player.getLocation());
    }

    public static BiomeTrait get(Location loc) {
        if (loc == null || loc.getWorld() == null) return BiomeTrait.DEFAULT;
        return get(loc.getBlock().getBiome());
    }

    public static BiomeTrait get(Biome biome) {
        if (biome == null) return BiomeTrait.DEFAULT;
        BiomeTrait trait = ACTIVE_CACHE.get(biome);
        return trait != null ? trait : BiomeTrait.DEFAULT;
    }

    private record LoadResult(Map<Biome, BiomeTrait> cache, int loaded, int total, int skipped, int warnings) {
    }
}

