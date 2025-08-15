package me.continent.biome;

import org.bukkit.entity.Player;

public class BiomeModifierService {
    public static float getTradeModifier(Player player) {
        return BiomeTraitService.getTradeDeform(player.getLocation());
    }
}
