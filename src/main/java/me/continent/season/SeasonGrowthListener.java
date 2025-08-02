package me.continent.season;

import me.continent.ContinentPlugin;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;

public class SeasonGrowthListener implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onGrow(BlockGrowEvent event) {
        Season season = SeasonManager.getCurrentSeason();
        switch (season) {
            case SPRING -> {
                if (Math.random() < 0.2) {
                    Block block = event.getBlock();
                    if (event.getNewState().getBlockData() instanceof Ageable age) {
                        int next = age.getAge() + 1;
                        if (next <= age.getMaximumAge()) {
                            Bukkit.getScheduler().runTaskLater(ContinentPlugin.getInstance(), () -> {
                                age.setAge(next);
                                block.setBlockData(age);
                            }, 1L);
                        }
                    }
                }
            }
            case AUTUMN -> {
                if (Math.random() < 0.2) {
                    event.setCancelled(true);
                }
            }
            case WINTER -> {
                if (Math.random() < 0.5) {
                    event.setCancelled(true);
                }
            }
            default -> {
            }
        }
    }
}
