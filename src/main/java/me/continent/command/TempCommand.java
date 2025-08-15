package me.continent.command;

import me.continent.temperature.PlayerTemperatureService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TempCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only");
            return true;
        }
        float temp = PlayerTemperatureService.getTemperature(player);
        player.sendMessage("§6[체온] §f현재 체온: §e" + String.format("%.1f", temp));
        return true;
    }
}
