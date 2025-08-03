package me.continent.listener;

import me.continent.ContinentPlugin;
import me.continent.nation.Nation;
import me.continent.player.PlayerData;
import me.continent.player.PlayerDataManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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

    private final Map<UUID, Attempt> attempts = new HashMap<>();
    private final Set<UUID> viewing = new HashSet<>();

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
                    successAttempt(thief, target);
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

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (viewing.contains(event.getWhoClicked().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        viewing.remove(event.getPlayer().getUniqueId());
    }

    private void successAttempt(Player thief, Player target) {
        attempts.remove(thief.getUniqueId());
        thief.playSound(thief.getLocation(), Sound.ITEM_BUNDLE_REMOVE_ONE, 0.3f, 1f);
        target.playSound(target.getLocation(), Sound.ITEM_BUNDLE_REMOVE_ONE, 0.3f, 1f);
        thief.openInventory(target.getInventory());
        viewing.add(thief.getUniqueId());
        thief.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent("🎒 당신은 인벤토리를 훔쳐보고 있습니다"));
    }

    private void failAttempt(Player thief, Player target) {
        Attempt attempt = attempts.remove(thief.getUniqueId());
        if (attempt == null) return;

        thief.playSound(thief.getLocation(), Sound.ITEM_BUNDLE_REMOVE_ONE, 0.3f, 1f);
        target.playSound(target.getLocation(), Sound.ITEM_BUNDLE_REMOVE_ONE, 0.3f, 1f);

        thief.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 5 * 20, 0));
        thief.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20, 0));

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
