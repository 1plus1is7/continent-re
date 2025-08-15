package me.continent.command;

import me.continent.war.War;
import me.continent.war.WarManager;
import me.continent.nation.NationManager;
import me.continent.nation.Nation;
import me.continent.economy.CentralBank;
import me.continent.nation.service.MaintenanceService;
import me.continent.player.PlayerData;
import me.continent.player.PlayerDataManager;
import me.continent.stat.PlayerStats;
import me.continent.scoreboard.ScoreboardService;
import me.continent.ContinentPlugin;
import me.continent.biome.BiomeTraitService;
import org.bukkit.OfflinePlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.*;

public class AdminCommand implements TabExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("continent.admin")) {
            sender.sendMessage("§c관리자 권한이 필요합니다.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§6[Admin 명령어]");
            sender.sendMessage("§e/admin war list §7- 진행 중인 전쟁 목록");
            sender.sendMessage("§e/admin war start <A> <B> §7- 전쟁 시작");
            sender.sendMessage("§e/admin war end <국가명> §7- 전쟁 강제 종료");
            sender.sendMessage("§e/admin war info <국가명> §7- 전쟁 정보");
            sender.sendMessage("§e/admin rate get §7- 현재 환율 확인");
            sender.sendMessage("§e/admin rate set <값> §7- 환율 설정");
            sender.sendMessage("§e/admin rate reset §7- 환율 초기화");
            sender.sendMessage("§e/admin rate min|max <값> §7- 환율 범위 설정");
            sender.sendMessage("§e/admin maintenance get §7- 유지비 정보");
            sender.sendMessage("§e/admin maintenance setcost <값> §7- 기본 유지비 설정");
            sender.sendMessage("§e/admin maintenance setperchunk <값> §7- 청크당 비용 설정");
            sender.sendMessage("§e/admin maintenance setlimit <값> §7- 미납 한도 설정");
            sender.sendMessage("§e/admin stat add <플레이어> <수량> §7- 스탯 포인트 지급");
            sender.sendMessage("§e/admin stat remove <플레이어> <수량> §7- 스탯 포인트 차감");
            sender.sendMessage("§e/admin stat set <플레이어> <수량> §7- 스탯 포인트 설정");
            sender.sendMessage("§e/admin scoreboard get §7- 스코어보드 설정 확인");
            sender.sendMessage("§e/admin scoreboard title <제목> §7- 스코어보드 제목 설정");
            sender.sendMessage("§e/admin scoreboard showcoords <true|false> §7- 좌표 표시 토글");
            sender.sendMessage("§e/admin scoreboard minimap <크기> §7- 미니맵 크기 설정");
            sender.sendMessage("§e/admin scoreboard refresh <초> §7- 갱신 주기 설정");
            sender.sendMessage("§e/admin config get <경로> §7- 설정값 확인");
            sender.sendMessage("§e/admin config set <경로> <값> §7- 설정값 변경");
            sender.sendMessage("§e/admin reload §7- 플러그인 설정 다시 불러오기");
            return true;
        }

        if (args[0].equalsIgnoreCase("biome")) {
            if (args.length >= 2 && args[1].equalsIgnoreCase("reload")) {
                BiomeTraitService.reloadAsync(sender);
                return true;
            }
            if (args.length >= 2 && args[1].equalsIgnoreCase("get")) {
                if (!(sender instanceof org.bukkit.entity.Player p)) {
                    sender.sendMessage("§c[Continent] Player only");
                    return true;
                }
                var trait = BiomeTraitService.get(p);
                String biomeName = p.getLocation().getBlock().getBiome().name();
                String tags = trait.tags().isEmpty() ? "-" : String.join(",", trait.tags());
                sender.sendMessage("§6[Continent] Biome: §e" + biomeName + " §7tags=" + tags);
                sender.sendMessage(String.format("§6[Continent] base_temp=§e%.1f §6move_mult=§e%.2f §6crop_rate=§e%.2f §6crop_yield_rate=§e%.2f",
                        trait.baseTemp(), trait.moveMult(), trait.cropRate(), trait.cropYieldRate()));
                if (!trait.rules().isEmpty()) {
                    String ruleList = String.join(",", trait.rules().stream()
                            .map(r -> r.type().name().toLowerCase()).toList());
                    sender.sendMessage("§6[Continent] rules: §f" + ruleList);
                }
                return true;
            }
            sender.sendMessage("§c[Continent] 사용법: /admin biome <reload|get>");
            return true;
        }

        if (args[0].equalsIgnoreCase("war")) {
            if (args.length >= 2 && args[1].equalsIgnoreCase("list")) {
                Collection<War> wars = WarManager.getWars();
                if (wars.isEmpty()) {
                    sender.sendMessage("§6[전쟁] §f현재 진행 중인 전쟁이 없습니다.");
                } else {
                    sender.sendMessage("§6[전쟁 목록]");
                    for (War war : wars) {
                        sender.sendMessage("§f" + war.getAttacker() + " vs " + war.getDefender());
                    }
                }
                return true;
            }
            if (args.length >= 4 && args[1].equalsIgnoreCase("start")) {
                Nation atk = NationManager.getByName(args[2]);
                Nation def = NationManager.getByName(args[3]);
                if (atk == null || def == null) {
                    sender.sendMessage("§c국가을 찾을 수 없습니다.");
                    return true;
                }
                if (WarManager.isAtWar(atk.getName(), def.getName())) {
                    sender.sendMessage("§c이미 전쟁 중입니다.");
                    return true;
                }
                WarManager.declareWar(atk, def);
                sender.sendMessage("§e[전쟁] §f전쟁을 시작했습니다: " + atk.getName() + " vs " + def.getName());
                return true;
            }
            if (args.length >= 3 && args[1].equalsIgnoreCase("end")) {
                War war = WarManager.getWar(args[2]);
                if (war == null) {
                    sender.sendMessage("§c해당 국가의 전쟁이 존재하지 않습니다.");
                    return true;
                }
                WarManager.endWar(war);
                sender.sendMessage("§e[전쟁] §f전쟁을 강제 종료했습니다.");
                return true;
            }
            if (args.length >= 3 && args[1].equalsIgnoreCase("info")) {
                War war = WarManager.getWar(args[2]);
                if (war == null) {
                    sender.sendMessage("§c전쟁 정보가 없습니다.");
                    return true;
                }
                sender.sendMessage("§6[전쟁 정보]");
                sender.sendMessage("§f공격국: §e" + war.getAttacker());
                sender.sendMessage("§f방어국: §e" + war.getDefender());
                sender.sendMessage("§f파괴된 국가: §e" + war.getDestroyedNations().size());
                return true;
            }
            sender.sendMessage("§c사용법: /admin war <list|start|end|info>");
            return true;
        }

        if (args[0].equalsIgnoreCase("rate")) {
            if (args.length >= 2 && args[1].equalsIgnoreCase("get")) {
                sender.sendMessage("§6[환율] §f현재 환율: §e" + CentralBank.getExchangeRate() + "C");
                return true;
            }
            if (args.length >= 3 && args[1].equalsIgnoreCase("set")) {
                try {
                    double value = Double.parseDouble(args[2]);
                    CentralBank.setExchangeRate(value);
                    sender.sendMessage("§6[환율] §f환율을 " + CentralBank.getExchangeRate() + "G 로 설정했습니다.");
                } catch (NumberFormatException e) {
                    sender.sendMessage("§c숫자를 입력해야 합니다.");
                }
                return true;
            }
            if (args.length >= 2 && args[1].equalsIgnoreCase("reset")) {
                CentralBank.resetExchangeRate();
                sender.sendMessage("§6[환율] §f환율을 초기화했습니다. 현재 환율: §e" + CentralBank.getExchangeRate() + "C");
                return true;
            }
            if (args.length >= 3 && args[1].equalsIgnoreCase("min")) {
                try {
                    double v = Double.parseDouble(args[2]);
                    CentralBank.setMinRate(v);
                    sender.sendMessage("§6[환율] §f최소 환율을 " + CentralBank.getMinRate() + "G 로 설정했습니다.");
                } catch (NumberFormatException e) {
                    sender.sendMessage("§c숫자를 입력해야 합니다.");
                }
                return true;
            }
            if (args.length >= 3 && args[1].equalsIgnoreCase("max")) {
                try {
                    double v = Double.parseDouble(args[2]);
                    CentralBank.setMaxRate(v);
                    sender.sendMessage("§6[환율] §f최대 환율을 " + CentralBank.getMaxRate() + "G 로 설정했습니다.");
                } catch (NumberFormatException e) {
                    sender.sendMessage("§c숫자를 입력해야 합니다.");
                }
                return true;
            }
            sender.sendMessage("§c사용법: /admin rate <get|set|reset|min|max>");
            return true;
        }

        if (args[0].equalsIgnoreCase("maintenance")) {
            if (args.length >= 2 && args[1].equalsIgnoreCase("get")) {
                sender.sendMessage("§6[유지비] §f기본: §e" + MaintenanceService.getCost() + "C");
                sender.sendMessage("§6[유지비] §f청크당: §e" + MaintenanceService.getPerChunkCost() + "C");
                sender.sendMessage("§6[유지비] §f미납 허용 주수: §e" + MaintenanceService.getUnpaidLimit());
                return true;
            }
            if (args.length >= 3 && args[1].equalsIgnoreCase("setcost")) {
                try {
                    double v = Double.parseDouble(args[2]);
                    MaintenanceService.setCost(v);
                    sender.sendMessage("§6[유지비] §f기본 유지비를 " + MaintenanceService.getCost() + "G 로 설정했습니다.");
                } catch (NumberFormatException e) {
                    sender.sendMessage("§c숫자를 입력해야 합니다.");
                }
                return true;
            }
            if (args.length >= 3 && args[1].equalsIgnoreCase("setperchunk")) {
                try {
                    double v = Double.parseDouble(args[2]);
                    MaintenanceService.setPerChunkCost(v);
                    sender.sendMessage("§6[유지비] §f청크당 비용을 " + MaintenanceService.getPerChunkCost() + "G 로 설정했습니다.");
                } catch (NumberFormatException e) {
                    sender.sendMessage("§c숫자를 입력해야 합니다.");
                }
                return true;
            }
            if (args.length >= 3 && args[1].equalsIgnoreCase("setlimit")) {
                try {
                    int v = Integer.parseInt(args[2]);
                    MaintenanceService.setUnpaidLimit(v);
                    sender.sendMessage("§6[유지비] §f미납 허용 주수를 " + MaintenanceService.getUnpaidLimit() + "주 로 설정했습니다.");
                } catch (NumberFormatException e) {
                    sender.sendMessage("§c숫자를 입력해야 합니다.");
                }
                return true;
            }
            sender.sendMessage("§c사용법: /admin maintenance <get|setcost|setperchunk|setlimit>");
            return true;
        }


        if (args[0].equalsIgnoreCase("config")) {
            ContinentPlugin plugin = ContinentPlugin.getInstance();
            var config = plugin.getConfig();

            if (args.length >= 3 && args[1].equalsIgnoreCase("get")) {
                String path = args[2];
                Object value = config.get(path);
                if (value == null) {
                    sender.sendMessage("§c경로를 찾을 수 없습니다.");
                } else {
                    sender.sendMessage("§6[설정] §f" + path + " = §e" + value);
                }
                return true;
            }

            if (args.length >= 4 && args[1].equalsIgnoreCase("set")) {
                String path = args[2];
                String raw = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                Object val;
                if (raw.equalsIgnoreCase("true") || raw.equalsIgnoreCase("false")) {
                    val = Boolean.parseBoolean(raw);
                } else {
                    try {
                        val = Integer.parseInt(raw);
                    } catch (NumberFormatException e1) {
                        try {
                            val = Double.parseDouble(raw);
                        } catch (NumberFormatException e2) {
                            val = raw;
                        }
                    }
                }
                config.set(path, val);
                plugin.saveConfig();
                sender.sendMessage("§6[설정] §f" + path + " 을(를) " + val + " 로 설정했습니다.");
                return true;
            }

            sender.sendMessage("§c사용법: /admin config <get|set> <경로> [값]");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            ContinentPlugin.reloadConfigValues();
            sender.sendMessage("§6[관리자] §f설정을 다시 불러왔습니다.");
            return true;
        }

        if (args[0].equalsIgnoreCase("scoreboard")) {
            ContinentPlugin plugin = ContinentPlugin.getInstance();
            var config = plugin.getConfig();

            if (args.length >= 2 && args[1].equalsIgnoreCase("get")) {
                sender.sendMessage("§6[스코어보드] §f제목: §e" + config.getString("scoreboard.title"));
                sender.sendMessage("§6[스코어보드] §f미니맵 크기: §e" + config.getInt("scoreboard.minimap-size"));
                sender.sendMessage("§6[스코어보드] §f좌표 표시: §e" + config.getBoolean("scoreboard.show-coordinates"));
                sender.sendMessage("§6[스코어보드] §f갱신 주기: §e" + config.getDouble("scoreboard.refresh-interval"));
                return true;
            }

            if (args.length >= 3 && args[1].equalsIgnoreCase("title")) {
                String title = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                config.set("scoreboard.title", title);
                plugin.saveConfig();
                Bukkit.getOnlinePlayers().forEach(ScoreboardService::update);
                sender.sendMessage("§6[스코어보드] §f제목을 설정했습니다.");
                return true;
            }

            if (args.length >= 3 && args[1].equalsIgnoreCase("showcoords")) {
                boolean flag = Boolean.parseBoolean(args[2]);
                config.set("scoreboard.show-coordinates", flag);
                plugin.saveConfig();
                Bukkit.getOnlinePlayers().forEach(ScoreboardService::update);
                sender.sendMessage("§6[스코어보드] §f좌표 표시를 " + flag + " 로 설정했습니다.");
                return true;
            }

            if (args.length >= 3 && args[1].equalsIgnoreCase("minimap")) {
                try {
                    int size = Integer.parseInt(args[2]);
                    config.set("scoreboard.minimap-size", size);
                    plugin.saveConfig();
                    Bukkit.getOnlinePlayers().forEach(ScoreboardService::update);
                    sender.sendMessage("§6[스코어보드] §f미니맵 크기를 " + size + "로 설정했습니다.");
                } catch (NumberFormatException e) {
                    sender.sendMessage("§c숫자를 입력해야 합니다.");
                }
                return true;
            }

            if (args.length >= 3 && args[1].equalsIgnoreCase("refresh")) {
                try {
                    double seconds = Double.parseDouble(args[2]);
                    config.set("scoreboard.refresh-interval", seconds);
                    plugin.saveConfig();
                    Bukkit.getScheduler().cancelTasks(plugin);
                    ScoreboardService.schedule();
                    sender.sendMessage("§6[스코어보드] §f갱신 주기를 " + seconds + "초로 설정했습니다.");
                } catch (NumberFormatException e) {
                    sender.sendMessage("§c숫자를 입력해야 합니다.");
                }
                return true;
            }

            sender.sendMessage("§c사용법: /admin scoreboard <get|title|showcoords|minimap|refresh>");
            return true;
        }

        if (args[0].equalsIgnoreCase("stat")) {
            if (args.length < 4) {
                sender.sendMessage("§c사용법: /admin stat <add|remove|set> <플레이어> <수량>");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
            PlayerData data = PlayerDataManager.get(target.getUniqueId());
            PlayerStats stats = data.getStats();
            try {
                int amount = Integer.parseInt(args[3]);
                if (args[1].equalsIgnoreCase("add")) {
                    stats.addPoints(amount);
                    PlayerDataManager.save(target.getUniqueId());
                    sender.sendMessage("§e" + target.getName() + "§f의 포인트를 +" + amount + " 만큼 지급했습니다. (현재 " + stats.getUnusedPoints() + ")");
                } else if (args[1].equalsIgnoreCase("remove")) {
                    int newVal = stats.getUnusedPoints() - amount;
                    stats.setPoints(newVal);
                    PlayerDataManager.save(target.getUniqueId());
                    sender.sendMessage("§e" + target.getName() + "§f의 포인트를 -" + amount + " 만큼 차감했습니다. (현재 " + stats.getUnusedPoints() + ")");
                } else if (args[1].equalsIgnoreCase("set")) {
                    stats.setPoints(amount);
                    PlayerDataManager.save(target.getUniqueId());
                    sender.sendMessage("§e" + target.getName() + "§f의 포인트를 " + stats.getUnusedPoints() + " 로 설정했습니다.");
                } else {
                    sender.sendMessage("§c사용법: /admin stat <add|remove|set> <플레이어> <수량>");
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§c수량은 숫자여야 합니다.");
            }
            return true;
        }

        sender.sendMessage("§c알 수 없는 하위 명령어입니다.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("war", "rate", "maintenance", "config", "scoreboard", "stat", "reload", "biome").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("war")) {
            return Arrays.asList("list", "start", "end", "info").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .toList();
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("war") && args[1].equalsIgnoreCase("start")) {
            return NationManager.getAll().stream()
                    .map(Nation::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("biome")) {
            return Arrays.asList("reload", "get").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .toList();
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("war") && args[1].equalsIgnoreCase("start")) {
            return NationManager.getAll().stream()
                    .map(Nation::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[3].toLowerCase()))
                    .toList();
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("war") && (args[1].equalsIgnoreCase("end") || args[1].equalsIgnoreCase("info"))) {
            return NationManager.getAll().stream()
                    .map(Nation::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("rate")) {
            return Arrays.asList("get", "set", "reset", "min", "max").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .toList();
        }


        if (args.length == 2 && args[0].equalsIgnoreCase("config")) {
            return Arrays.asList("get", "set").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("scoreboard")) {
            return Arrays.asList("get", "title", "showcoords", "minimap", "refresh").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("maintenance")) {
            return Arrays.asList("get", "setcost", "setperchunk", "setlimit").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("stat")) {
            return Arrays.asList("add", "remove", "set").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .toList();
        }


        if (args.length == 3 && args[0].equalsIgnoreCase("stat")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(org.bukkit.entity.Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                    .toList();
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("maintenance")) {
            return List.of();
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("scoreboard") && args[1].equalsIgnoreCase("showcoords")) {
            return Arrays.asList("true", "false").stream()
                    .filter(s -> s.startsWith(args[2].toLowerCase()))
                    .toList();
        }

        return Collections.emptyList();
    }
}
