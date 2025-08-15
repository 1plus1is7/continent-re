package me.continent.movement;

import me.continent.biome.BiomeTrait;
import me.continent.biome.BiomeTraitService;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Applies movement multipliers based on biome traits and special rules.
 */
public class MovementListener implements Listener {
    private static final Map<UUID, Double> BASE_SPEED = new HashMap<>();

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        AttributeInstance attr = event.getPlayer().getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr != null) {
            BASE_SPEED.put(event.getPlayer().getUniqueId(), attr.getBaseValue());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        BASE_SPEED.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getChunk().equals(event.getTo().getChunk()) &&
                event.getFrom().getBlock().getBiome() == event.getTo().getBlock().getBiome()) {
            return; // same chunk and biome
        }
        Player player = event.getPlayer();
        AttributeInstance attr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr == null) return;
        double base = BASE_SPEED.computeIfAbsent(player.getUniqueId(), id -> attr.getBaseValue());

        BiomeTrait trait = BiomeTraitService.get(player);
        double mult = Math.max(0.1f, Math.min(5.0f, trait.moveMult()));
        double finalMult = mult;

        boolean leatherImmune = hasLeatherImmunity(player, trait);
        for (BiomeTrait.Rule rule : trait.rules()) {
            if (rule.type() == BiomeTrait.RuleType.LOW_LIGHT_SLOW) {
                int lightMax = ((Number) rule.params().getOrDefault("light_max", 5)).intValue();
                double ruleMult = ((Number) rule.params().getOrDefault("move_mult", 1.0)).doubleValue();
                if (!leatherImmune && player.getLocation().getBlock().getLightFromBlocks() <= lightMax) {
                    finalMult *= ruleMult;
                }
            }
        }

        double target = base * finalMult;
        if (Math.abs(attr.getBaseValue() - target) > 1e-6) {
            attr.setBaseValue(target);
        }
    }

    private boolean hasLeatherImmunity(Player player, BiomeTrait trait) {
        boolean rulePresent = trait.rules().stream().anyMatch(r -> r.type() == BiomeTrait.RuleType.LEATHER_IMMUNITY);
        if (!rulePresent) return false;
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (ItemStack piece : armor) {
            if (piece == null || piece.getType() == Material.AIR) return false;
            if (!piece.getType().name().startsWith("LEATHER_")) return false;
        }
        return true;
    }
}

