package me.continent.season;

import me.continent.ContinentPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;

public class SeasonWeatherTask {
    private static final Map<World, Long> lastDay = new HashMap<>();

    public static void start(ContinentPlugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Season season = SeasonManager.getCurrentSeason();
            for (World world : Bukkit.getWorlds()) {
                long day = world.getFullTime() / 24000;
                if (world.getTime() < 5 && (!lastDay.containsKey(world) || lastDay.get(world) != day)) {
                    lastDay.put(world, day);
                    applyWeather(world, season);
                }
            }
        }, 200L, 200L); // check every 10 seconds
    }

    private static void applyWeather(World world, Season season) {
        switch (season) {
            case SPRING -> {
                boolean rain = Math.random() < 0.4;
                world.setStorm(rain);
                world.setThundering(false);
            }
            case SUMMER -> {
                boolean storm = Math.random() < 0.3;
                world.setStorm(storm);
                world.setThundering(storm);
            }
            case AUTUMN -> {
                boolean rain = Math.random() < 0.2;
                world.setStorm(rain);
                world.setThundering(false);
            }
            case WINTER -> {
                boolean snow = Math.random() < 0.3;
                world.setStorm(snow);
                world.setThundering(false);
            }
        }
    }
}
