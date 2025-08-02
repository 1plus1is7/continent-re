package me.continent.command;

import me.continent.player.PlayerData;
import me.continent.player.PlayerDataManager;
import me.continent.stat.PlayerStats;
import me.continent.stat.StatType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StatCommand implements TabExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있습니다.");
            return true;
        }
        PlayerData data = PlayerDataManager.get(player.getUniqueId());
        PlayerStats stats = data.getStats();

        if (args.length == 0) {
            player.sendMessage("§6[Stat] 현재 스탯");
            for (StatType type : StatType.values()) {
                player.sendMessage("§e" + type.name() + ": §f" + stats.get(type));
            }
            if (stats.getMastery() != null) {
                player.sendMessage("§e마스터리: §f" + stats.getMastery().name());
            }
            player.sendMessage("남은 포인트: " + stats.getUnusedPoints());
            return true;
        }

        if (args[0].equalsIgnoreCase("add") && args.length >= 2) {
            try {
                StatType type = StatType.valueOf(args[1].toUpperCase(Locale.ROOT));
                if (stats.investPoint(type)) {
                    PlayerDataManager.save(player.getUniqueId());
                    player.sendMessage("§a" + type.name() + " +1 (" + stats.get(type) + ")");
                    me.continent.stat.StatsManager.applyStats(player);
                } else {
                    player.sendMessage("§c스탯을 추가할 수 없습니다.");
                }
            } catch (IllegalArgumentException e) {
                player.sendMessage("§c잘못된 스탯입니다.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("remove") && args.length >= 2) {
            try {
                StatType type = StatType.valueOf(args[1].toUpperCase(Locale.ROOT));
                if (stats.removePoint(type)) {
                    PlayerDataManager.save(player.getUniqueId());
                    player.sendMessage("§a" + type.name() + " -1 (" + stats.get(type) + ")");
                    me.continent.stat.StatsManager.applyStats(player);
                } else {
                    player.sendMessage("§c해당 스탯에 투자된 포인트가 없습니다.");
                }
            } catch (IllegalArgumentException e) {
                player.sendMessage("§c잘못된 스탯입니다.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("reset")) {
            stats.resetAll();
            PlayerDataManager.save(player.getUniqueId());
            me.continent.stat.StatsManager.applyStats(player);
            player.sendMessage("§a스탯을 초기화했습니다. 사용 가능한 포인트: " + stats.getUnusedPoints());
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return List.of("add", "remove", "reset");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
            List<String> list = new ArrayList<>();
            for (StatType type : StatType.values()) list.add(type.name().toLowerCase(Locale.ROOT));
            return list;
        }
        return List.of();
    }
}
