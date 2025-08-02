package me.continent.season;

import me.continent.ContinentPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class WinterFreezeTask {
    public static void start(ContinentPlugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (SeasonManager.getCurrentSeason() != Season.WINTER) {
                return;
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                freezeAround(player.getLocation(), 20);
            }
        }, 200L, 200L); // every 10 seconds
    }

    private static void freezeAround(Location center, int radius) {
        World world = center.getWorld();
        if (world == null) return;
        int cx = center.getBlockX();
        int cz = center.getBlockZ();
        int cy = center.getBlockY();
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                int y = world.getHighestBlockYAt(x, z);
                if (y < cy - 5 || y > cy + radius) continue;
                Block block = world.getBlockAt(x, y, z);
                if (block.getType() == Material.WATER && block.getRelative(0, 1, 0).getType() == Material.AIR) {
                    block.setType(Material.ICE);
                    Bukkit.getScheduler().runTaskLater(ContinentPlugin.getInstance(), () -> {
                        if (block.getType() == Material.ICE) {
                            block.setType(Material.WATER);
                        }
                    }, 12000L); // thaw after 10 minutes
                }
            }
        }
    }
}
