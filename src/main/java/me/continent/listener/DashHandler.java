package me.continent.listener;

import me.continent.ContinentPlugin;
import me.continent.player.PlayerDataManager;
import me.continent.stat.PlayerStats;
import me.continent.stat.StatType;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DashHandler implements Listener {
    private final Map<UUID, Long> lastSprintStart = new ConcurrentHashMap<>();
    private final Set<UUID> dashCooldown = ConcurrentHashMap.newKeySet();
    private static final long DOUBLE_WINDOW_MS = 250;
    private static final long COOLDOWN_MS = 1500;

    @EventHandler
    public void onSprint(PlayerToggleSprintEvent event) {
        if (!event.isSprinting()) return;
        Player player = event.getPlayer();
        PlayerStats stats = PlayerDataManager.get(player.getUniqueId()).getStats();
        if (stats.get(StatType.AGILITY) < 10 || player.getGameMode() != GameMode.SURVIVAL) return;

        long now = System.currentTimeMillis();
        UUID id = player.getUniqueId();
        Long prev = lastSprintStart.put(id, now);
        if (dashCooldown.contains(id)) return;
        if (prev != null && (now - prev) <= DOUBLE_WINDOW_MS) {
            Vector dir = player.getLocation().getDirection();
            dir.setY(0).normalize().multiply(1.6);
            Vector current = player.getVelocity();
            player.setVelocity(new Vector(dir.getX(), Math.max(current.getY(), 0.25), dir.getZ()));
            player.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, player.getLocation(), 20, 0.3, 0.1, 0.3, 0);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1f, 1f);
            dashCooldown.add(id);
            Bukkit.getScheduler().runTaskLater(ContinentPlugin.getInstance(), () -> dashCooldown.remove(id), COOLDOWN_MS / 50);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        lastSprintStart.remove(id);
        dashCooldown.remove(id);
    }
}
