package me.continent.stat;

import me.continent.ContinentPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;

import java.util.List;
import java.util.function.BiConsumer;

public enum StatType {
    STRENGTH((player, points) -> {
        apply(player, Attribute.ATTACK_DAMAGE, "str_damage", points * 0.2, AttributeModifier.Operation.ADD_NUMBER);
        apply(player, Attribute.ATTACK_KNOCKBACK, "str_knockback", points >= 14 ? 1.0 : 0.0, AttributeModifier.Operation.ADD_NUMBER);
        apply(player, Attribute.ATTACK_SPEED, "str_attack_speed", points >= 10 ? 16.0 : 0.0, AttributeModifier.Operation.ADD_NUMBER);
    }),
    AGILITY((player, points) -> {
        apply(player, Attribute.MOVEMENT_SPEED, "agi_speed", 0.05 * points, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
    }),
    INTELLIGENCE((player, points) -> {
        // no direct effect yet
    }),
    VITALITY((player, points) -> {
        apply(player, Attribute.MAX_HEALTH, "vit_health", points * 2.0, AttributeModifier.Operation.ADD_NUMBER);
        var healthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null && player.getHealth() > healthAttr.getValue()) {
            player.setHealth(healthAttr.getValue());
        }
    }),
    LUCK((player, points) -> {
        // no direct effect yet
    });

    private final BiConsumer<Player, Integer> applier;

    StatType(BiConsumer<Player, Integer> applier) {
        this.applier = applier;
    }

    public void apply(Player player, int points) {
        applier.accept(player, points);
    }

    private static void apply(Player player, Attribute attrType, String key, double amount, AttributeModifier.Operation op) {
        var attr = player.getAttribute(attrType);
        if (attr == null) return;
        NamespacedKey nsKey = new NamespacedKey(ContinentPlugin.getInstance(), key);
        for (var mod : List.copyOf(attr.getModifiers())) {
            if (nsKey.equals(mod.getKey())) {
                attr.removeModifier(mod);
            }
        }
        if (amount != 0) {
            attr.addTransientModifier(new AttributeModifier(nsKey, amount, op, EquipmentSlotGroup.ANY));
        }
    }
}
