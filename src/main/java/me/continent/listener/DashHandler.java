package me.continent.listener;

import me.continent.ContinentPlugin;
import me.continent.player.PlayerDataManager;
import me.continent.stat.PlayerStats;
import me.continent.stat.StatType;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles player dash ability. Players with AGILITY stat 10 or higher can
 * perform a forward dash by double tapping their jump key. The dash adds a
 * burst of horizontal velocity and triggers particles and sound effects. A
 * short cooldown prevents repeated usage.
 */
public class DashHandler implements Listener {
    private static final long COOLDOWN_MS = 1500; // Dash cooldown

    private final Set<UUID> dashCooldown = ConcurrentHashMap.newKeySet();

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL) return;

        PlayerStats stats = PlayerDataManager.get(player.getUniqueId()).getStats();
        if (stats.get(StatType.AGILITY) < 10) return;

        event.setCancelled(true);

        UUID id = player.getUniqueId();
        if (dashCooldown.contains(id)) return;

        Vector dir = player.getLocation().getDirection();
        dir.setY(0).normalize().multiply(1.6);
        Vector current = player.getVelocity();

        player.setVelocity(new Vector(dir.getX(), Math.max(current.getY(), 0.25), dir.getZ()));
        player.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, player.getLocation(), 20, 0.3, 0.1, 0.3, 0);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WIND_CHARGE_WIND_BURST, 0.5f, 1f);

        startCooldown(player, id);
    }

    private void startCooldown(Player player, UUID id) {
        dashCooldown.add(id);
        player.setAllowFlight(false);

        final long totalTicks = COOLDOWN_MS / 50;
        new BukkitRunnable() {
            long elapsed = 0;

            @Override
            public void run() {
                double progress = (double) elapsed / totalTicks;
                int bars = (int) Math.round(progress * 10);
                String bar = "[" + "■".repeat(bars) + "□".repeat(10 - bars) + "]";
                player.sendActionBar(Component.text(bar));

                if (elapsed++ >= totalTicks) {
                    dashCooldown.remove(id);
                    player.sendActionBar(Component.empty());
                    if (player.isOnGround()) {
                        player.setAllowFlight(true);
                    }
                    cancel();
                }
            }
        }.runTaskTimer(ContinentPlugin.getInstance(), 0L, 1L);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL) return;

        PlayerStats stats = PlayerDataManager.get(player.getUniqueId()).getStats();
        if (stats.get(StatType.AGILITY) < 10) return;

        if (player.isOnGround() && !dashCooldown.contains(player.getUniqueId())) {
            player.setAllowFlight(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        dashCooldown.remove(id);
    }
}

