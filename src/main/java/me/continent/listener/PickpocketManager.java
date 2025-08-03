package me.continent.listener;

import me.continent.ContinentPlugin;
import me.continent.nation.Nation;
import me.continent.player.PlayerData;
import me.continent.player.PlayerDataManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PickpocketManager implements Listener {

    private static class Attempt {
        final Player thief;
        final Player target;
        int ticks = 0;

        Attempt(Player thief, Player target) {
            this.thief = thief;
            this.target = target;
        }
    }

    private static final NamespacedKey UNSTEALABLE_KEY = new NamespacedKey(ContinentPlugin.getInstance(), "unstealable");
    private final Map<UUID, Attempt> attempts = new HashMap<>();

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player target)) return;
        Player thief = event.getPlayer();
        if (!thief.isSneaking()) return;
        if (attempts.containsKey(thief.getUniqueId())) return;

        PlayerData data = PlayerDataManager.get(thief.getUniqueId());
        if (data == null) return;
        Nation nation = data.getNation();
        if (nation == null || nation.getTier() < 2 || !nation.hasPrison()) return;

        Attempt attempt = new Attempt(thief, target);
        attempts.put(thief.getUniqueId(), attempt);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!attempts.containsKey(thief.getUniqueId())) {
                    cancel();
                    return;
                }
                attempt.ticks++;

                if (!thief.isOnline() || !target.isOnline()) {
                    failAttempt(thief, target);
                    cancel();
                    return;
                }
                if (!thief.isSneaking() || thief.getLocation().distance(target.getLocation()) > 3) {
                    failAttempt(thief, target);
                    cancel();
                    return;
                }
                if (isTargetLooking(thief, target)) {
                    failAttempt(thief, target);
                    cancel();
                    return;
                }

                if (attempt.ticks % 20 == 0) {
                    int sec = attempt.ticks / 20;
                    thief.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("소매치기 중" + ".".repeat(Math.min(3, sec))));
                }

                if (attempt.ticks >= 60) {
                    if (ThreadLocalRandom.current().nextDouble() < 0.4) {
                        successAttempt(thief, target);
                    } else {
                        failAttempt(thief, target);
                    }
                    cancel();
                }
            }
        }.runTaskTimer(ContinentPlugin.getInstance(), 1L, 1L);
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) return;
        Attempt attempt = attempts.get(event.getPlayer().getUniqueId());
        if (attempt != null) {
            failAttempt(attempt.thief, attempt.target);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        // Check if moving player is target of an attempt
        for (Attempt attempt : attempts.values()) {
            if (attempt.target.getUniqueId().equals(event.getPlayer().getUniqueId())) {
                if (isTargetLooking(attempt.thief, attempt.target)) {
                    failAttempt(attempt.thief, attempt.target);
                }
                break;
            }
        }
    }

    private void successAttempt(Player thief, Player target) {
        if (thief.getInventory().firstEmpty() == -1) {
            failAttempt(thief, target);
            return;
        }

        PlayerInventory targetInv = target.getInventory();
        ItemStack[] contents = targetInv.getStorageContents();
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < contents.length; i++) {
            if (isStealable(contents[i])) {
                slots.add(i);
            }
        }
        if (slots.isEmpty()) {
            failAttempt(thief, target);
            return;
        }

        attempts.remove(thief.getUniqueId());

        int slot = slots.get(ThreadLocalRandom.current().nextInt(slots.size()));
        ItemStack item = contents[slot];
        int stealAmount = 1 + ThreadLocalRandom.current().nextInt(item.getAmount());
        ItemStack stolen = item.clone();
        stolen.setAmount(stealAmount);
        thief.getInventory().addItem(stolen);
        item.setAmount(item.getAmount() - stealAmount);
        if (item.getAmount() <= 0) {
            targetInv.setItem(slot, null);
        }

        thief.playSound(thief.getLocation(), Sound.ITEM_BUNDLE_REMOVE_ONE, 0.3f, 1f);
        target.playSound(target.getLocation(), Sound.ITEM_BUNDLE_REMOVE_ONE, 0.3f, 1f);
        thief.sendMessage("소매치기에 성공했습니다!");
        target.sendMessage("무언가가 사라진 것 같습니다...");
    }

    private void failAttempt(Player thief, Player target) {
        attempts.remove(thief.getUniqueId());

        thief.playSound(thief.getLocation(), Sound.ITEM_BUNDLE_REMOVE_ONE, 0.3f, 1f);
        target.playSound(target.getLocation(), Sound.ITEM_BUNDLE_REMOVE_ONE, 0.3f, 1f);

        thief.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20, 0));
        dropRandomItem(thief);

        thief.sendMessage("⚠ 소매치기에 실패했습니다! 들켰습니다!");
        target.sendMessage("누군가 당신을 노리고 있었습니다...");
        String notice = thief.getName() + "이 도둑질을 시도하다 들켰습니다!";
        for (Player p : thief.getWorld().getPlayers()) {
            if (p.equals(thief) || p.equals(target)) continue;
            if (p.getLocation().distance(thief.getLocation()) <= 5) {
                p.sendMessage(notice);
            }
        }
    }

    private void dropRandomItem(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getStorageContents();
        List<Integer> available = new ArrayList<>();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && contents[i].getType() != Material.AIR) {
                available.add(i);
            }
        }
        if (available.isEmpty()) return;
        int slot = available.get(ThreadLocalRandom.current().nextInt(available.size()));
        ItemStack drop = contents[slot];
        inv.setItem(slot, null);
        player.getWorld().dropItem(player.getLocation(), drop);
    }

    private boolean isStealable(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return true;
        if (meta.hasEnchant(Enchantment.BINDING_CURSE)) return false;
        if (meta.getPersistentDataContainer().has(UNSTEALABLE_KEY, PersistentDataType.BYTE)) return false;
        return true;
    }

    private boolean isTargetLooking(Player thief, Player target) {
        Location tLoc = target.getLocation();
        Location thiefLoc = thief.getLocation();
        org.bukkit.util.Vector toThief = thiefLoc.toVector().subtract(tLoc.toVector());
        toThief.setY(0);
        org.bukkit.util.Vector dir = tLoc.getDirection();
        dir.setY(0);
        double angle = dir.angle(toThief);
        return angle < Math.toRadians(90);
    }
}
