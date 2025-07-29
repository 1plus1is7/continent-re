package me.continent;

import me.continent.chat.NationChatListener;
import me.continent.chat.GlobalChatListener;
import me.continent.command.GoldCommand;
import me.continent.command.NationCommand;
import me.continent.command.UnionCommand;
import me.continent.war.WarCommand;
import me.continent.command.GuideCommand;
import me.continent.command.SpecialtyCommand;
import me.continent.command.AdminCommand;
import me.continent.command.MenuCommand;
import me.continent.command.EnterpriseCommand;
import me.continent.economy.CentralBankDataManager;
import me.continent.listener.TerritoryListener;
import me.continent.listener.MaintenanceJoinListener;
import me.continent.listener.KeepLevelListener;
import me.continent.protection.TerritoryProtectionListener;
import me.continent.protection.CoreProtectionListener;
import me.continent.war.CoreAttackListener;
import me.continent.war.WarDeathListener;
import me.continent.war.CoreSlimeDamageListener;
import me.continent.protection.ProtectionStateListener;
import me.continent.nation.service.ChestListener;
import me.continent.nation.service.MaintenanceService;
import me.continent.nation.service.NationMenuListener;
import me.continent.nation.service.NationTreasuryListener;
import me.continent.nation.service.NationMemberListener;
import me.continent.nation.service.NationUpkeepListener;
import me.continent.menu.ServerMenuListener;
import me.continent.job.JobManager;
import me.continent.job.JobMenuListener;
import me.continent.command.JobCommand;
import me.continent.enterprise.EnterpriseTypeConfig;
import me.continent.nation.gui.NationListListener;
import me.continent.enterprise.gui.EnterpriseListListener;
import org.bukkit.plugin.java.JavaPlugin;
import me.continent.player.PlayerDataManager;
import me.continent.command.MarketCommand;
import me.continent.market.MarketManager;
import me.continent.market.MarketListener;
import me.continent.enterprise.EnterpriseService;
import me.continent.enterprise.contract.ContractManager;
import me.continent.storage.NationStorage;
import me.continent.storage.UnionStorage;
import me.continent.scoreboard.ScoreboardService;
import me.continent.crop.CropGrowthManager;
import me.continent.crop.CropListener;
import me.continent.research.ResearchListener;
import me.continent.research.ResearchManager;
import me.continent.specialty.SpecialtyManager;
import me.continent.specialty.SpecialtyListener;
import me.continent.nation.service.NationSpecialtyListener;

public class ContinentPlugin extends JavaPlugin {
    private static ContinentPlugin instance;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        translateColorCodes(getConfig());
        MaintenanceService.init(getConfig());
        MaintenanceService.schedule();

        // 명령어 등록 (plugin.yml 누락 시 NPE 방지)
        registerCommand("crown", new GoldCommand());
        registerCommand("nation", new NationCommand());
        registerCommand("union", new UnionCommand());
        registerCommand("war", new WarCommand());
        registerCommand("guide", new GuideCommand());
        registerCommand("specialty", new SpecialtyCommand());
        registerCommand("market", new MarketCommand());
        registerCommand("admin", new AdminCommand());
        registerCommand("menu", new MenuCommand());
        registerCommand("enterprise", new EnterpriseCommand());
        registerCommand("job", new JobCommand());
        registerCommand("stat", new me.continent.command.StatCommand());

        // 중앙은행 데이터 로딩
        CentralBankDataManager.load();
        NationStorage.loadAll(); // 저장된 모든 국가 불러오기
        UnionStorage.loadAll();
        PlayerDataManager.loadAll();

        ResearchManager.loadNodes(this);
        SpecialtyManager.load(this);
        JobManager.load(this);
        EnterpriseTypeConfig.load(this);
        me.continent.enterprise.production.ProductionManager.load(this);
        me.continent.enterprise.production.ProductionManager.schedule();

        CropGrowthManager.init(this);

        ScoreboardService.schedule();

        MarketManager.load(this);
        EnterpriseService.init(this);
        EnterpriseService.loadAll();
        ContractManager.loadAll();
        ContractManager.scheduleGeneration();
        getServer().getPluginManager().registerEvents(new TerritoryListener(), this);
        getServer().getPluginManager().registerEvents(new NationChatListener(), this);
        getServer().getPluginManager().registerEvents(new GlobalChatListener(), this);
        getServer().getPluginManager().registerEvents(new MaintenanceJoinListener(), this);
        getServer().getPluginManager().registerEvents(new TerritoryProtectionListener(), this);
        getServer().getPluginManager().registerEvents(new CoreProtectionListener(), this);
        getServer().getPluginManager().registerEvents(new CoreAttackListener(), this);
        getServer().getPluginManager().registerEvents(new CoreSlimeDamageListener(), this);
        getServer().getPluginManager().registerEvents(new ChestListener(), this);
        getServer().getPluginManager().registerEvents(new ProtectionStateListener(), this);
        getServer().getPluginManager().registerEvents(new WarDeathListener(), this);
        getServer().getPluginManager().registerEvents(new KeepLevelListener(), this);
        getServer().getPluginManager().registerEvents(new CropListener(), this);
        getServer().getPluginManager().registerEvents(new ResearchListener(), this);
        getServer().getPluginManager().registerEvents(new me.continent.specialty.SpecialtyListener(), this);
        getServer().getPluginManager().registerEvents(new NationSpecialtyListener(), this);
        getServer().getPluginManager().registerEvents(new NationMenuListener(), this);
        getServer().getPluginManager().registerEvents(new NationTreasuryListener(), this);
        getServer().getPluginManager().registerEvents(new NationMemberListener(), this);
        getServer().getPluginManager().registerEvents(new NationUpkeepListener(), this);
        getServer().getPluginManager().registerEvents(new ServerMenuListener(), this);
        getServer().getPluginManager().registerEvents(new me.continent.economy.gui.GoldMenuListener(), this);
        getServer().getPluginManager().registerEvents(new me.continent.enterprise.gui.EnterpriseMenuListener(), this);
        getServer().getPluginManager().registerEvents(new me.continent.enterprise.gui.DeliveryMenuListener(), this);
        getServer().getPluginManager().registerEvents(new me.continent.nation.gui.NationListListener(), this);
        getServer().getPluginManager().registerEvents(new me.continent.enterprise.gui.EnterpriseListListener(), this);
        getServer().getPluginManager().registerEvents(new me.continent.enterprise.gui.ProductionMenuListener(), this);
        getServer().getPluginManager().registerEvents(new JobMenuListener(), this);
        getServer().getPluginManager().registerEvents(new me.continent.listener.StatsEffectListener(), this);
        getServer().getPluginManager().registerEvents(new me.continent.listener.StatLevelListener(), this);


        getServer().getPluginManager().registerEvents(new MarketListener(), this);

        getLogger().info("Continent 플러그인 활성화됨");
    }

    @Override
    public void onDisable() {
        // 중앙은행 데이터 저장
        CentralBankDataManager.save();
        MarketManager.save();
        EnterpriseService.saveAll();
        ContractManager.saveAll();
        PlayerDataManager.saveAll();
        UnionStorage.saveAll();

        CropGrowthManager.shutdown();

        getLogger().info("Continent 플러그인 비활성화됨");
    }

    public static ContinentPlugin getInstance() {
        return instance;
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor exe) {
        var cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(exe);
        } else {
            getLogger().severe("Command '" + name + "' not defined in plugin.yml");
        }
    }

    private static void translateColorCodes(org.bukkit.configuration.ConfigurationSection section) {
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof String str) {
                section.set(key, org.bukkit.ChatColor.translateAlternateColorCodes('&', str));
            } else if (value instanceof org.bukkit.configuration.ConfigurationSection cs) {
                translateColorCodes(cs);
            }
        }
    }
}
