package me.continent.stat;

import me.continent.ContinentPlugin;
import me.continent.player.PlayerData;
import me.continent.player.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.FireworkEffect;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;

public class StatsManager {

    private static final NamespacedKey STR_DAMAGE_KEY = new NamespacedKey(ContinentPlugin.getInstance(), "str_damage");
    private static final NamespacedKey AGI_SPEED_KEY = new NamespacedKey(ContinentPlugin.getInstance(), "agi_speed");
    private static final NamespacedKey VIT_HEALTH_KEY = new NamespacedKey(ContinentPlugin.getInstance(), "vit_health");
    private static final NamespacedKey STR_ATTACK_SPEED_KEY = new NamespacedKey(ContinentPlugin.getInstance(), "str_attack_speed");
    private static final NamespacedKey STR_KNOCKBACK_KEY = new NamespacedKey(ContinentPlugin.getInstance(), "str_knockback");

    private static void applyModifier(Player player, Attribute attrType, NamespacedKey key, double amount, AttributeModifier.Operation op) {
        var attr = player.getAttribute(attrType);
        if (attr == null) return;
        attr.getModifiers().stream().filter(m -> key.equals(m.getKey())).forEach(attr::removeModifier);
        if (amount != 0) {
            attr.addTransientModifier(new AttributeModifier(key, amount, op, EquipmentSlotGroup.ANY));
        }
    }

    public static void checkLevelUp(Player player) {
        PlayerData data = PlayerDataManager.get(player.getUniqueId());
        PlayerStats stats = data.getStats();
        int level = player.getLevel();
        int last = stats.getLastLevelGiven();
        if (level < 100) return;
        int next = ((level - 100) / 25) * 25 + 100;
        if (level < next || next <= last) return;
        stats.setLastLevelGiven(next);
        stats.addPoints(1);
        PlayerDataManager.save(player.getUniqueId());
        if (next % 100 == 0) {
            launchFireworks(player, true);
            player.sendTitle("§6★ 마스터 레벨 도달 ★", "§e스탯 포인트 +1!", 10, 40, 10);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        } else {
            launchFireworks(player, false);
            player.sendActionBar("§e[Stat] §e스탯 포인트 +1!");
        }

        applyStats(player);
    }

    private static final Color[] MASTER_COLORS = {
            Color.fromRGB(0xFFD700),
            Color.fromRGB(0xFFFF55),
            Color.fromRGB(0xB366FF)
    };

    private static void launchFireworks(Player player, boolean multiple) {
        if (multiple) {
            int shots = 3 + (int) (Math.random() * 3);
            new BukkitRunnable() {
                int count = 0;
                @Override
                public void run() {
                    Color c = MASTER_COLORS[(int) (Math.random() * MASTER_COLORS.length)];
                    spawnFirework(player, c, true);
                    count++;
                    if (count >= shots) cancel();
                }
            }.runTaskTimer(ContinentPlugin.getInstance(), 0L, 4L);
        } else {
            spawnFirework(player, Color.fromRGB(0xFFDB4D), false);
        }
    }

    private static void spawnFirework(Player player, Color color, boolean around) {
        org.bukkit.Location loc = player.getLocation();
        if (around) {
            double dx = (Math.random() - 0.5) * 4;
            double dz = (Math.random() - 0.5) * 4;
            loc = loc.clone().add(dx, 0, dz);
        }
        Firework fw = (Firework) player.getWorld().spawnEntity(loc, EntityType.FIREWORK_ROCKET);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder().withColor(color).with(FireworkEffect.Type.STAR).build());
        meta.setPower(0);
        fw.setFireworkMeta(meta);
        fw.detonate();
    }

    public static void applyStats(Player player) {
        PlayerStats stats = PlayerDataManager.get(player.getUniqueId()).getStats();

        int str = stats.get(StatType.STRENGTH);
        int agi = stats.get(StatType.AGILITY);
        int vit = stats.get(StatType.VITALITY);

        applyModifier(player, Attribute.ATTACK_DAMAGE, STR_DAMAGE_KEY, str * 0.2, AttributeModifier.Operation.ADD_NUMBER);
        applyModifier(player, Attribute.ATTACK_KNOCKBACK, STR_KNOCKBACK_KEY, str >= 14 ? 1.0 : 0.0, AttributeModifier.Operation.ADD_NUMBER);
        applyModifier(player, Attribute.ATTACK_SPEED, STR_ATTACK_SPEED_KEY, str >= 10 ? 16.0 : 0.0, AttributeModifier.Operation.ADD_NUMBER);
        applyModifier(player, Attribute.MOVEMENT_SPEED, AGI_SPEED_KEY, 0.05 * agi, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        applyModifier(player, Attribute.MAX_HEALTH, VIT_HEALTH_KEY, vit * 2.0, AttributeModifier.Operation.ADD_NUMBER);
        var healthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null && player.getHealth() > healthAttr.getValue()) {
            player.setHealth(healthAttr.getValue());
        }
    }
}
