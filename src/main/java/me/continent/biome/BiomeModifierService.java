package me.continent.biome;

import org.bukkit.entity.Player;

public class BiomeModifierService {
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
}
