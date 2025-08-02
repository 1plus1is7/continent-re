package me.continent.season;

import me.continent.ContinentPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class SeasonManager {
    private static LocalDate startDate;
    private static final Map<Season, Map<String, Object>> variables = new EnumMap<>(Season.class);
    private static Season lastSeason = null;
    private static ContinentPlugin plugin;

    public static void init(ContinentPlugin pl) {
        plugin = pl;
        FileConfiguration config = plugin.getConfig();
        String start = config.getString("season.start-date", "2025-01-01");
        startDate = LocalDate.parse(start, DateTimeFormatter.ISO_DATE);

        ConfigurationSection varSec = config.getConfigurationSection("season.variables");
        if (varSec != null) {
            for (String key : varSec.getKeys(false)) {
                try {
                    Season season = Season.valueOf(key.toUpperCase());
                    ConfigurationSection sec = varSec.getConfigurationSection(key);
                    if (sec != null) {
                        Map<String, Object> map = new HashMap<>();
                        for (String k : sec.getKeys(false)) {
                            map.put(k, sec.get(k));
                        }
                        variables.put(season, map);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        scheduleSeasonCheck();
    }

    private static void scheduleSeasonCheck() {
        Bukkit.getScheduler().runTaskTimer(plugin, SeasonManager::checkSeasonChange, 0L, 1200L * 30); // every 30 minutes
    }

    private static void checkSeasonChange() {
        Season current = getCurrentSeason();
        if (current != lastSeason) {
            lastSeason = current;
            broadcastSeasonMessage(current);
        }
    }

    public static Season getCurrentSeason() {
        long days = ChronoUnit.DAYS.between(startDate, LocalDate.now());
        if (days < 0) days = 0;
        int index = (int) ((days / 14) % 4);
        return Season.values()[index];
    }

    public static Object getVariable(String name) {
        Map<String, Object> map = variables.get(getCurrentSeason());
        return map != null ? map.get(name) : null;
    }

    private static void broadcastSeasonMessage(Season season) {
        String msg;
        switch (season) {
            case SPRING -> msg = "§a봄이 시작되었습니다!";
            case SUMMER -> msg = "§e여름이 시작되었습니다!";
            case AUTUMN -> msg = "§6가을이 시작되었습니다!";
            case WINTER -> msg = "§b❄ 겨울이 시작되었습니다! 물이 얼기 시작합니다.";
            default -> msg = season.name();
        }
        Bukkit.broadcastMessage(msg);
    }
}
