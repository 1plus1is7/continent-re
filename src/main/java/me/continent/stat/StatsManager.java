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

public class StatsManager {

    private static final NamespacedKey STR_DAMAGE_KEY = new NamespacedKey(ContinentPlugin.getInstance(), "str_damage");
    private static final NamespacedKey AGI_SPEED_KEY = new NamespacedKey(ContinentPlugin.getInstance(), "agi_speed");
    private static final NamespacedKey VIT_HEALTH_KEY = new NamespacedKey(ContinentPlugin.getInstance(), "vit_health");
    private static final NamespacedKey STR_ATTACK_SPEED_KEY = new NamespacedKey(ContinentPlugin.getInstance(), "str_attack_speed");
    private static final NamespacedKey STR_KNOCKBACK_KEY = new NamespacedKey(ContinentPlugin.getInstance(), "str_knockback");

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

        var damageAttr = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.getModifiers().stream().filter(m -> STR_DAMAGE_KEY.equals(m.getKey())).forEach(damageAttr::removeModifier);
            double bonus = 0.05 * str + Math.min(3, Math.max(0, str - 10));
            if (bonus != 0) {
                damageAttr.addTransientModifier(new org.bukkit.attribute.AttributeModifier(STR_DAMAGE_KEY, bonus, org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
            }
        }

        var knockAttr = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_KNOCKBACK);
        if (knockAttr != null) {
            knockAttr.getModifiers().stream().filter(m -> STR_KNOCKBACK_KEY.equals(m.getKey())).forEach(knockAttr::removeModifier);
            if (str >= 14) {
                knockAttr.addTransientModifier(new org.bukkit.attribute.AttributeModifier(STR_KNOCKBACK_KEY, 1.0, org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
            }
        }

        var attackSpeedAttr = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_SPEED);
        if (attackSpeedAttr != null) {
            attackSpeedAttr.getModifiers().stream().filter(m -> STR_ATTACK_SPEED_KEY.equals(m.getKey())).forEach(attackSpeedAttr::removeModifier);
            if (str >= 10) {
                // Large bonus effectively removes attack cooldown
                attackSpeedAttr.addTransientModifier(new org.bukkit.attribute.AttributeModifier(STR_ATTACK_SPEED_KEY, 16.0, org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
            }
        }

        var moveAttr = player.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED);
        if (moveAttr != null) {
            moveAttr.getModifiers().stream().filter(m -> AGI_SPEED_KEY.equals(m.getKey())).forEach(moveAttr::removeModifier);
            double bonus = 0.05 * agi;
            if (bonus != 0) {
                moveAttr.addTransientModifier(new org.bukkit.attribute.AttributeModifier(AGI_SPEED_KEY, bonus, org.bukkit.attribute.AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.ANY));
            }
        }

        var healthAttr = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.getModifiers().stream().filter(m -> VIT_HEALTH_KEY.equals(m.getKey())).forEach(healthAttr::removeModifier);
            double bonus = vit + Math.min(3, Math.max(0, vit - 10));
            if (bonus != 0) {
                healthAttr.addTransientModifier(new org.bukkit.attribute.AttributeModifier(VIT_HEALTH_KEY, bonus, org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
            }
            if (player.getHealth() > healthAttr.getValue()) {
                player.setHealth(healthAttr.getValue());
            }
        }
    }
}
