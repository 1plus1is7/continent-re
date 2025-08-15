package me.continent.temperature;

import me.continent.biome.BiomeTraitService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerTemperatureService implements Runnable {
    private static final Map<UUID, Float> temps = new HashMap<>();
    private static int taskId = -1;

    public static void start(JavaPlugin plugin) {
        if (taskId != -1) return;
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new PlayerTemperatureService(), 20L, 20L);
    }

    public static void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        temps.clear();
    }

    public static float getTemperature(Player player) {
        return temps.getOrDefault(player.getUniqueId(), BiomeTraitService.get(player).baseTemp());
    }

    @Override
    public void run() {
        var online = Bukkit.getOnlinePlayers();
        if (online.isEmpty()) {
            temps.clear();
            return;
        }
        Set<UUID> valid = new HashSet<>();
        for (Player p : online) {
            if (!p.isOnline()) continue;
            valid.add(p.getUniqueId());
            float newTemp = calculateTemp(p);
            float oldTemp = temps.getOrDefault(p.getUniqueId(), newTemp);
            if (Math.abs(newTemp - oldTemp) < 0.01f) continue;
            temps.put(p.getUniqueId(), newTemp);
            p.sendActionBar(Component.text("Temp: " + Math.round(newTemp)));
        }
        temps.keySet().retainAll(valid);
    }

    private float calculateTemp(Player player) {
        float base = BiomeTraitService.get(player).baseTemp();
        // TODO weather, indoor/outdoor, armor, potion effects
        return base;
    }
}
