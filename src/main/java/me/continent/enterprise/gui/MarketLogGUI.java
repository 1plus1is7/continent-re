package me.continent.enterprise.gui;

import me.continent.enterprise.Enterprise;
import me.continent.market.pricing.DemandManager;
import me.continent.market.pricing.MarketLogManager;
import me.continent.market.pricing.MarketLogManager.SaleLog;
import me.continent.market.pricing.MarketPriceCalculator;

import me.continent.enterprise.gui.PriceFluctuationGUI;
import me.continent.enterprise.gui.MarketAnalysisGUI;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * GUI showing current market prices and recent sales for an enterprise.
 */
public class MarketLogGUI implements Listener {
    public static void open(Player player, Enterprise enterprise) {
        Holder holder = new Holder(enterprise);
        Inventory inv = Bukkit.createInventory(holder, 54, "시장 현황");
        holder.setInventory(inv);
        render(inv, enterprise);
        player.openInventory(inv);
    }

    private static void render(Inventory inv, Enterprise enterprise) {
        fill(inv);
        int idx = 0;
        // priced items in rows 1-4
        for (Material mat : MarketPriceCalculator.getPricedMaterials()) {
            if (idx >= 36) break;
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(mat.name());
            List<String> lore = new ArrayList<>();
            ItemStack unit = new ItemStack(mat);
            int price = MarketPriceCalculator.calculateTotalPrice(unit, 1, 1.0);
            double demand = DemandManager.getDemandCoefficient(mat.name());
            lore.add("§7시세: " + price + "C");
            lore.add("§7수요 계수: " + String.format("%.2f", demand));
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(idx++, item);
        }

        // sale logs row
        List<SaleLog> logs = MarketLogManager.getLogs(enterprise.getId());
        int logIdx = 36;
        for (SaleLog log : logs) {
            if (logIdx >= 44) break;
            ItemStack item = new ItemStack(log.getMaterial());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(log.getMaterial().name());
            List<String> lore = new ArrayList<>();
            lore.add("§7판매량: " + log.getAmount());
            lore.add("§7총액: " + log.getPrice() + "C");
            String time = new SimpleDateFormat("HH:mm").format(new Date(log.getTime()));
            lore.add("§7시간: " + time);
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(logIdx++, item);
        }
        int total = MarketLogManager.getTotalRevenue(enterprise.getId());
        ItemStack summary = new ItemStack(Material.GOLD_INGOT);
        ItemMeta sumMeta = summary.getItemMeta();
        sumMeta.setDisplayName("§e총 수익: " + total + "C");
        summary.setItemMeta(sumMeta);
        inv.setItem(44, summary);

        // control buttons row
        inv.setItem(45, button(Material.PAPER, "가격 변동 보기"));
        inv.setItem(49, button(Material.BARRIER, "닫기"));
        inv.setItem(53, button(Material.BOOK, "국가별 시장 분석"));
    }

    private static void fill(Inventory inv) {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, pane);
        }
    }

    private static ItemStack button(Material m, String name) {
        ItemStack item = new ItemStack(m);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    public static class Holder implements InventoryHolder {
        private final Enterprise enterprise;
        private Inventory inv;
        public Holder(Enterprise ent) { this.enterprise = ent; }
        void setInventory(Inventory inv) { this.inv = inv; }
        @Override public Inventory getInventory() { return inv; }
        public Enterprise getEnterprise() { return enterprise; }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder h)) return;
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        if (slot == 49) { player.closeInventory(); return; }
        if (slot == 45) {
            PriceFluctuationGUI.open(player);
            return;
        }
        if (slot == 53) {
            MarketAnalysisGUI.open(player);
        }
    }
}

