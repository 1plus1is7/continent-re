package me.continent.command;

import me.continent.ContinentPlugin;
import me.continent.rainfeel.RainFeelService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Command to reload the RainFeel configuration at runtime.
 */
public class RainFeelCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("continent.admin")) {
                sender.sendMessage("§c관리자 권한이 필요합니다.");
                return true;
            }
            ContinentPlugin.getInstance().reloadConfig();
            RainFeelService.reload();
            sender.sendMessage("§6[RainFeel] §f설정을 다시 불러왔습니다.");
            return true;
        }

        sender.sendMessage("§e/rainfeel reload");
        return true;
    }
}

