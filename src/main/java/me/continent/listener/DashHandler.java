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
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles player dash ability. Players with AGILITY stat 10 or higher can
 * perform a forward dash by double tapping their sneak key. The dash adds a
 * burst of horizontal velocity and triggers particles and sound effects. A
 * short cooldown prevents repeated usage.
 */
public class DashHandler implements Listener {
    private static final long DOUBLE_WINDOW_MS = 250; // Time between sneak presses
    private static final long COOLDOWN_MS = 1500; // Dash cooldown

    private final Map<UUID, Long> lastSneak = new ConcurrentHashMap<>();
    private final Set<UUID> dashCooldown = ConcurrentHashMap.newKeySet();

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return; // Only react when player begins sneaking

        Player player = event.getPlayer();
        PlayerStats stats = PlayerDataManager.get(player.getUniqueId()).getStats();

        if (stats.get(StatType.AGILITY) < 10 || player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }

        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();

        Long last = lastSneak.put(id, now);
        if (last == null || now - last > DOUBLE_WINDOW_MS || dashCooldown.contains(id)) {
            return;
        }

        Vector dir = player.getLocation().getDirection();
        dir.setY(0).normalize().multiply(1.6);
        Vector current = player.getVelocity();

        player.setVelocity(new Vector(dir.getX(), Math.max(current.getY(), 0.25), dir.getZ()));
        player.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, player.getLocation(), 20, 0.3, 0.1, 0.3, 0);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1f, 1f);

        dashCooldown.add(id);
        Bukkit.getScheduler().runTaskLater(
                ContinentPlugin.getInstance(),
                () -> dashCooldown.remove(id),
                COOLDOWN_MS / 50
        );
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        lastSneak.remove(id);
        dashCooldown.remove(id);
    }
}

