package me.continent.biome;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BiomeTraitService {
    private static final Map<Biome, BiomeTrait> traits = new HashMap<>();
    private static JavaPlugin plugin;

    public static void init(JavaPlugin pl) {
        plugin = pl;
        load();
    }

    public static void reload() {
        load();
    }

    private static void load() {
        traits.clear();
        if (plugin == null) return;
        File file = new File(plugin.getDataFolder(), "biome_traits.yml");
        if (!file.exists()) {
            plugin.saveResource("biome_traits.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            try {
                Biome biome = Biome.valueOf(key.toUpperCase());
                float base = (float) config.getDouble(key + ".base_temp", 20.0);
                float deform = (float) config.getDouble(key + ".trade_deform", 1.0);
                traits.put(biome, new BiomeTrait(base, deform));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unknown biome in biome_traits.yml: " + key);
            }
        }
    }

    public static BiomeTrait getTrait(Biome biome) {
        return traits.getOrDefault(biome, new BiomeTrait(20.0f, 1.0f));
    }

    public static float getBaseTemp(Location loc) {
        Biome biome = loc.getBlock().getBiome();
        return getTrait(biome).baseTemp();
    }

    public static float getTradeDeform(Location loc) {
        Biome biome = loc.getBlock().getBiome();
        return getTrait(biome).tradeDeform();
    }
}
