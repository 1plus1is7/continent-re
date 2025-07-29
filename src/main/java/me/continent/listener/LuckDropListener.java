package me.continent.listener;

import me.continent.player.PlayerDataManager;
import me.continent.stat.PlayerStats;
import me.continent.stat.StatType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class LuckDropListener implements Listener {
    private static final Material[] RARE_ORES = {
            Material.DIAMOND_ORE,
            Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE,
            Material.DEEPSLATE_EMERALD_ORE,
            Material.ANCIENT_DEBRIS,
            Material.NETHERITE_BLOCK
    };

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material type = block.getType();
        boolean rare = false;
        for (Material m : RARE_ORES) {
            if (m == type) {
                rare = true;
                break;
            }
        }
        if (!rare) return;

        PlayerStats stats = PlayerDataManager.get(event.getPlayer().getUniqueId()).getStats();
        int luck = stats.get(StatType.LUCK);
        if (luck <= 0) return;

        double chance = luck * 0.05; // 5% per point
        if (Math.random() < chance) {
            for (ItemStack drop : block.getDrops(event.getPlayer().getInventory().getItemInMainHand())) {
                block.getWorld().dropItemNaturally(block.getLocation(), drop);
            }
        }
    }
}
