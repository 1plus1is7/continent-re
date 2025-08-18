package me.continent.war;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Slime;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Prevents core slimes from dying to environmental damage.
 */
public class CoreSlimeDamageListener implements Listener {

    private boolean isCoreSlime(Slime slime) {
        return slime.getScoreboardTags().stream()
                .anyMatch(t -> t.startsWith("core_slime:"));
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Slime slime)) return;
        if (!isCoreSlime(slime)) return;

        // Allow damage from players, their projectiles, or TNT they ignited
        if (event instanceof EntityDamageByEntityEvent byEntity) {
            Entity damager = byEntity.getDamager();
            if (damager instanceof Player) return;
            if (damager instanceof Projectile proj && proj.getShooter() instanceof Player) return;
            if (damager instanceof TNTPrimed tnt && tnt.getSource() instanceof Player) return;
        }
        event.setCancelled(true);
    }
}
