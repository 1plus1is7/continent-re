package me.continent.nation.service;

import me.continent.nation.Nation;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/** Listener for the war statistics menu. */
public class NationWarStatsListener implements Listener {
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        if (inv.getHolder() instanceof NationWarStatsService.StatsHolder holder) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            Nation nation = holder.getNation();
            if (event.getRawSlot() == 26) {
                NationMenuService.openMenu(player, nation);
            }
        }
    }
}
