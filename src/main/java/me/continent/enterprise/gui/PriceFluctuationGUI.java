package me.continent.enterprise.gui;

import me.continent.market.pricing.MarketPriceCalculator;
import me.continent.market.pricing.PriceHistoryManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * GUI displaying recent price history for items.
 */
public class PriceFluctuationGUI implements Listener {
    public static void open(Player player) {
        Holder h = new Holder();
        Inventory inv = Bukkit.createInventory(h, 54, "가격 변동");
        h.setInventory(inv);
        render(inv);
        player.openInventory(inv);
    }

    private static void render(Inventory inv) {
        fill(inv);
        List<Material> mats = new ArrayList<>(MarketPriceCalculator.getPricedMaterials());
        mats.sort(Comparator.comparing(Enum::name));
        int idx = 0;
        for (Material mat : mats) {
            if (idx >= 45) break;
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(mat.name());
            List<String> lore = new ArrayList<>();
            List<Integer> hist = PriceHistoryManager.getHistory(mat);
            for (int i = 0; i < hist.size(); i++) {
                lore.add("§7" + (i + 1) + "회전전: " + hist.get(i) + "C");
            }
            if (hist.isEmpty()) {
                int current = MarketPriceCalculator.calculateTotalPrice(new ItemStack(mat), 1, 1.0);
                lore.add("§7현재가: " + current + "C");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(idx++, item);
        }
        inv.setItem(49, button(Material.BARRIER, "닫기"));
    }

    private static ItemStack button(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private static void fill(Inventory inv) {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, pane);
    }

    static class Holder implements InventoryHolder {
        private Inventory inv;
        void setInventory(Inventory inv) { this.inv = inv; }
        @Override public Inventory getInventory() { return inv; }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder)) return;
        event.setCancelled(true);
        if (event.getRawSlot() == 49) {
            ((Player) event.getWhoClicked()).closeInventory();
        }
    }
}
