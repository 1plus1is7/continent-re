package me.continent.biome;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

/**
 * Provides helpers for applying biome based economic modifiers.
 */
public class BiomeModifierService {
    /**
     * Returns the trade category deformation for the player's current biome.
     */
    public static float getTradeModifier(Player player, String category) {
        BiomeTrait trait = BiomeTraitService.get(player);
        BiomeTrait.TradeDeform td = trait.tradeDeform();
        return switch (category.toLowerCase()) {
            case "food" -> td.food();
            case "metal" -> td.metal();
            case "leather" -> td.leather();
            case "gem" -> td.gem();
            default -> 0f;
        };
    }

    /**
     * Calculates item specific value multipliers based on special rules.
     */
    public static float getItemValueMultiplier(Material item, Location loc) {
        BiomeTrait trait = BiomeTraitService.get(loc);
        float mult = 1.0f;
        for (BiomeTrait.Rule rule : trait.rules()) {
            if (rule.type() == BiomeTrait.RuleType.ELEVATION_TRADE_BONUS) {
                String target = String.valueOf(rule.params().get("item")).toLowerCase();
                if (!item.name().toLowerCase().equals(target)) continue;
                int yMin = ((Number) rule.params().getOrDefault("y_min", 0)).intValue();
                int radius = ((Number) rule.params().getOrDefault("radius", 0)).intValue();
                double valueMult = ((Number) rule.params().getOrDefault("value_mult", 1.0)).doubleValue();
                if (loc.getBlockY() < yMin) continue;
                if (!isUniformBiome(loc, radius)) continue;
                mult *= (float) valueMult;
            }
        }
        return mult;
    }

    private static boolean isUniformBiome(Location loc, int radius) {
        if (radius <= 0) return true;
        Biome base = loc.getBlock().getBiome();
        int step = Math.max(1, radius / 5);
        for (int x = -radius; x <= radius; x += step) {
            for (int z = -radius; z <= radius; z += step) {
                if (loc.getWorld().getBiome(loc.getBlockX() + x, loc.getBlockY(), loc.getBlockZ() + z) != base) {
                    return false;
                }
            }
        }
        return true;
    }
}
