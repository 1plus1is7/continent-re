package me.continent.enterprise;

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
import java.util.List;

/** Production facility GUI. */
public class ProductionGUI implements Listener {
    private static final int[] RECIPE_SLOTS = {0,1,2,9,10,11};
    private static final int[] QUEUE_SLOTS = {27,28,29,30,31,32,33,34,35};

    public static class Holder implements InventoryHolder {
        private final Enterprise enterprise;
        private Inventory inv;
        public Holder(Enterprise ent) { this.enterprise = ent; }
        void setInventory(Inventory inv) { this.inv = inv; }
        @Override public Inventory getInventory() { return inv; }
        public Enterprise getEnterprise() { return enterprise; }
    }

    public static void open(Player player, Enterprise enterprise) {
        Holder h = new Holder(enterprise);
        Inventory inv = Bukkit.createInventory(h, 54, "생산시설");
        h.setInventory(inv);
        render(inv, enterprise);
        player.openInventory(inv);
    }

    private static void render(Inventory inv, Enterprise enterprise) {
        fill(inv);
        List<EnterpriseProductionManager.Recipe> recipes = EnterpriseProductionManager.getRecipes();
        for (int i=0;i<recipes.size() && i<RECIPE_SLOTS.length;i++) {
            var r = recipes.get(i);
            ItemStack item = new ItemStack(r.getOutput(), r.getAmount());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(r.getId());
            List<String> lore = new ArrayList<>();
            for (var e : r.getInputs().entrySet()) {
                lore.add("필요: "+e.getKey().name()+" x"+e.getValue());
            }
            lore.add("시간: "+r.getTime()+"초");
            lore.add("비용: 5G");
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(RECIPE_SLOTS[i], item);
        }
        // queue display
        List<EnterpriseProductionManager.ProductionTask> q = EnterpriseProductionManager.getQueue(enterprise);
        long now = System.currentTimeMillis();
        for (int i=0;i<q.size() && i<QUEUE_SLOTS.length;i++) {
            var t = q.get(i);
            ItemStack item = new ItemStack(t.getRecipe().getOutput(), t.getRecipe().getAmount());
            ItemMeta meta = item.getItemMeta();
            if (t.isComplete()) {
                meta.setDisplayName("생산 완료!");
                meta.setLore(List.of("수령 대기"));
            } else {
                long remain = (t.getFinishTime()-now)/1000;
                if (remain < 0) remain = 0;
                meta.setDisplayName(t.getRecipe().getOutput().name());
                meta.setLore(List.of("남은 시간: "+remain+"초"));
            }
            item.setItemMeta(meta);
            inv.setItem(QUEUE_SLOTS[i], item);
        }
        // gold display
        ItemStack gold = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta meta = gold.getItemMeta();
        meta.setDisplayName("골드: "+EnterpriseStorageManager.getGold(enterprise));
        gold.setItemMeta(meta);
        inv.setItem(49, gold);
        inv.setItem(53, button(Material.BARRIER, "닫기"));
    }

    private static void fill(Inventory inv) {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        for (int i=0;i<inv.getSize();i++) inv.setItem(i,pane);
    }

    private static ItemStack button(Material m, String name) {
        ItemStack item = new ItemStack(m);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder h)) return;
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        if (slot == 53) { player.closeInventory(); return; }
        // recipe selection
        for (int i=0;i<RECIPE_SLOTS.length;i++) {
            if (slot == RECIPE_SLOTS[i]) {
                var recipes = EnterpriseProductionManager.getRecipes();
                if (i < recipes.size()) {
                    EnterpriseProductionManager.start(player, h.getEnterprise(), recipes.get(i));
                    open(player, h.getEnterprise());
                }
                return;
            }
        }
        // queue items - nothing
    }
}
