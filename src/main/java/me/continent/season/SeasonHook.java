package me.continent.season;

import org.bukkit.plugin.java.JavaPlugin;

public interface SeasonHook {
    void init(JavaPlugin plugin);
    void shutdown();

    SeasonHook NOOP = new SeasonHook() {
        @Override
        public void init(JavaPlugin plugin) {}

        @Override
        public void shutdown() {}
    };
}
