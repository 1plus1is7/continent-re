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

import java.util.List;
import java.util.Map;

/** Warehouse GUI for enterprises. */
public class StorageGUI implements Listener {
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
        Inventory inv = Bukkit.createInventory(h, 54, "창고");
        h.setInventory(inv);
        render(inv, enterprise);
        player.openInventory(inv);
    }

    private static void render(Inventory inv, Enterprise enterprise) {
        fill(inv);
        Map<Material,Integer> storage = EnterpriseStorageManager.getStorage(enterprise);
        int idx = 0;
        for (var e : storage.entrySet()) {
            ItemStack item = new ItemStack(e.getKey());
            item.setAmount(Math.min(e.getValue(), 64));
            ItemMeta meta = item.getItemMeta();
            meta.setLore(List.of("수량: "+e.getValue()));
            item.setItemMeta(meta);
            if (idx < 45) inv.setItem(idx++, item);
        }
        inv.setItem(49, button(Material.BARRIER, "닫기"));
        inv.setItem(53, button(Material.LAVA_BUCKET, "전체 비우기"));
    }

    private static void fill(Inventory inv) {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        for (int i=45;i<54;i++) inv.setItem(i, pane);
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
        if (slot == 49) { player.closeInventory(); return; }
        if (slot == 53 && player.isOp()) {
            EnterpriseStorageManager.clear(h.getEnterprise());
            open(player, h.getEnterprise());
        }
    }
}
