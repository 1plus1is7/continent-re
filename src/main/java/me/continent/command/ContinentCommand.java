package me.continent.command;

import me.continent.biome.BiomeTraitService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class ContinentCommand implements TabExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length >= 2 && args[0].equalsIgnoreCase("biome") && args[1].equalsIgnoreCase("reload")) {
            BiomeTraitService.reloadAsync(sender);
            return true;
        }
        sender.sendMessage("§c[Continent] Usage: /continent biome reload");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("biome");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("biome")) {
            return List.of("reload");
        }
        return Collections.emptyList();
    }
}
