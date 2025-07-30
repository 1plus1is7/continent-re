package me.continent.nation.service;

import me.continent.nation.Nation;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/** Simple GUI showing war statistics for a nation. */
public class NationWarStatsService {
    public static void openMenu(Player player, Nation nation) {
        StatsHolder holder = new StatsHolder(nation);
        Inventory inv = Bukkit.createInventory(holder, 27, "War Statistics");
        holder.setInventory(inv);
        render(inv, nation);
        player.openInventory(inv);
    }

    static void render(Inventory inv, Nation nation) {
        inv.clear();
        ItemStack info = item(Material.IRON_SWORD, "전쟁 통계");
        ItemMeta meta = info.getItemMeta();
        meta.setLore(List.of(
                "§7승리: " + nation.getWarWins() + "회",
                "§7패배: " + nation.getWarLosses() + "회"
        ));
        info.setItemMeta(meta);
        inv.setItem(13, info);
        inv.setItem(26, item(Material.ARROW, "메인 메뉴"));
    }

    private static ItemStack item(Material mat, String name) {
        ItemStack is = new ItemStack(mat);
        ItemMeta meta = is.getItemMeta();
        meta.setDisplayName(name);
        is.setItemMeta(meta);
        return is;
    }

    static class StatsHolder implements InventoryHolder {
        private final Nation nation;
        private Inventory inv;
        StatsHolder(Nation n) { this.nation = n; }
        void setInventory(Inventory inv) { this.inv = inv; }
        @Override public Inventory getInventory() { return inv; }
        public Nation getNation() { return nation; }
    }
}
