package me.continent.market.pricing;

import me.continent.economy.CentralBank;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Calculates market prices using base price, demand and exchange rate.
 */
public class MarketPriceCalculator {
    private static final Map<Material, Integer> basePrices = new HashMap<>();
    static {
        basePrices.put(Material.IRON_INGOT, 10);
        basePrices.put(Material.GOLD_INGOT, 20);
        basePrices.put(Material.DIAMOND, 100);
    }

    /** Base price for an item. Defaults to 1 if unspecified. */
    public static int getBasePrice(ItemStack item) {
        return basePrices.getOrDefault(item.getType(), 1);
    }

    /**
     * Calculate unit price for an item applying demand, exchange and quality.
     * @param item item type
     * @param quality quality modifier (1.0 = normal)
     * @return calculated unit price
     */
    public static double calculateUnitPrice(ItemStack item, double quality) {
        String key = item.getType().name();
        double demand = DemandManager.getDemandCoefficient(key);
        double exchange = CentralBank.getExchangeRate() / CentralBank.getBaseRate();
        return getBasePrice(item) * demand * exchange * quality;
    }

    /** Calculate total price for amount of an item. */
    public static int calculateTotalPrice(ItemStack item, int amount, double quality) {
        double unit = calculateUnitPrice(item, quality);
        return (int) Math.max(1, Math.round(unit * amount));
    }

    /** Return all materials that have base prices defined. */
    public static Set<Material> getPricedMaterials() {
        return basePrices.keySet();
    }
}
