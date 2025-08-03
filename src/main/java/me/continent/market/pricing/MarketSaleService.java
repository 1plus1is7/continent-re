package me.continent.market.pricing;

import me.continent.enterprise.Enterprise;
import me.continent.enterprise.EnterpriseStorageManager;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import me.continent.market.pricing.PriceHistoryManager;

/**
 * Handles selling items to the market for enterprises.
 */
public class MarketSaleService {

    /** Estimate reward for selling given item and amount. */
    public static int calculateReward(ItemStack item, int amount) {
        return MarketPriceCalculator.calculateTotalPrice(item, amount, 1.0);
    }

    /** Sell item and deposit gold to enterprise. */
    public static int sell(Enterprise enterprise, ItemStack item, int amount) {
        int reward = calculateReward(item, amount);
        EnterpriseStorageManager.addGold(enterprise, reward);
        DemandManager.recordSupply(item.getType().name(), amount);
        MarketLogManager.recordSale(enterprise.getId(), item, amount, reward);
        int unit = (int) Math.max(1, Math.round(MarketPriceCalculator.calculateUnitPrice(item, 1.0)));
        PriceHistoryManager.record(item.getType(), unit);
        Bukkit.getLogger().info("Market sale: " + enterprise.getName() + " sold " + amount + "x " + item.getType() + " for " + reward + "C");
        // notify enterprise owner if online
        var owner = Bukkit.getPlayer(enterprise.getOwner());
        if (owner != null) {
            owner.sendMessage("§a판매됨! §f" + item.getType().name() + " x" + amount + " → " + reward + "C");
        }
        return reward;
    }
}
