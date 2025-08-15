package me.continent.crop;

import me.continent.biome.BiomeTraitService;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class CropListener implements Listener {

    private static final Set<Material> CROP_DROPS = EnumSet.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
            Material.NETHER_WART, Material.SWEET_BERRIES, Material.TORCHFLOWER,
            Material.PITCHER_PLANT, Material.SUGAR_CANE, Material.CACTUS,
            Material.KELP, Material.BAMBOO, Material.MELON_SLICE, Material.PUMPKIN
    );

    private static final Map<String, Double> REMAINDER = new HashMap<>();

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        CropType type = CropType.fromMaterial(event.getBlockPlaced().getType());
        if (type != null) {
            CropGrowthManager.registerCrop(event.getBlockPlaced(), type);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        CropType type = CropType.fromMaterial(event.getBlock().getType());
        if (type != null) {
            CropGrowthManager.unregisterCrop(event.getBlock());
        }
    }

    @EventHandler
    public void onGrow(BlockGrowEvent event) {
        CropType type = CropType.fromMaterial(event.getBlock().getType());
        if (type != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFertilize(BlockFertilizeEvent event) {
        for (var state : event.getBlocks()) {
            if (CropType.fromMaterial(state.getType()) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onDrop(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        if (player.getInventory().getItemInMainHand().containsEnchantment(Enchantment.SILK_TOUCH)) return;
        float rate = BiomeTraitService.get(player).cropYieldRate();
        if (Math.abs(rate - 1.0f) < 0.0001f) return;
        rate = Math.max(0.1f, Math.min(5.0f, rate));
        for (Item item : event.getItems()) {
            ItemStack stack = item.getItemStack();
            if (!CROP_DROPS.contains(stack.getType())) continue;
            int base = stack.getAmount();
            double scaled = base * rate;
            int whole = (int) Math.floor(scaled);
            double rem = scaled - whole;
            String key = player.getUniqueId() + ":" + stack.getType().name();
            double acc = REMAINDER.getOrDefault(key, 0.0) + rem;
            int extra = (int) Math.floor(acc);
            REMAINDER.put(key, acc - extra);
            int finalAmount = whole + extra;
            if (finalAmount <= 0) {
                item.remove();
                continue;
            }
            while (finalAmount > 64) {
                ItemStack split = stack.clone();
                split.setAmount(64);
                item.getWorld().dropItem(item.getLocation(), split);
                finalAmount -= 64;
            }
            stack.setAmount(finalAmount);
            item.setItemStack(stack);
        }
    }

    @EventHandler
    public void onBreakYield(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        if (!event.isDropItems()) return;
        if (player.getInventory().getItemInMainHand().containsEnchantment(Enchantment.SILK_TOUCH)) return;
        float rate = BiomeTraitService.get(player).cropYieldRate();
        if (Math.abs(rate - 1.0f) < 0.0001f) return;
        rate = Math.max(0.1f, Math.min(5.0f, rate));
        Collection<ItemStack> drops = event.getBlock().getDrops(player.getInventory().getItemInMainHand(), player);
        if (drops.isEmpty()) return;
        event.setDropItems(false);
        for (ItemStack drop : drops) {
            if (!CROP_DROPS.contains(drop.getType())) continue;
            int base = drop.getAmount();
            double scaled = base * rate;
            int whole = (int) Math.floor(scaled);
            double rem = scaled - whole;
            String key = player.getUniqueId() + ":" + drop.getType().name();
            double acc = REMAINDER.getOrDefault(key, 0.0) + rem;
            int extra = (int) Math.floor(acc);
            REMAINDER.put(key, acc - extra);
            int finalAmount = whole + extra;
            if (finalAmount <= 0) continue;
            while (finalAmount > 64) {
                ItemStack split = drop.clone();
                split.setAmount(64);
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), split);
                finalAmount -= 64;
            }
            drop.setAmount(finalAmount);
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), drop);
        }
    }
}
