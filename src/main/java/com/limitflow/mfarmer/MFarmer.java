package com.limitflow.mfarmer;

import com.limitflow.mfarmer.backpack.BackpackManager;
import com.limitflow.mfarmer.boost.BoostManager;
import com.limitflow.mfarmer.command.FarmerCommand;
import com.limitflow.mfarmer.config.ConfigCache;
import com.limitflow.mfarmer.database.Database;
import com.limitflow.mfarmer.eco.Economic;
import com.limitflow.mfarmer.group.GroupManager;
import com.limitflow.mfarmer.listener.BreakBlock;
import com.limitflow.mfarmer.menu.Menu;
import com.limitflow.mfarmer.menu.MenuListener;
import com.limitflow.mfarmer.utils.Expansion;
import com.limitflow.mfarmer.utils.Region;
import com.limitflow.mfarmer.utils.Update;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class MFarmer extends JavaPlugin {

    private static MFarmer instance;

    private Database database;
    private Economic economyManager;
    private BackpackManager backpackManager;
    private ConfigCache configCache;
    private GroupManager groupManager;
    private BoostManager boostManager;
    private Menu menu;
    private BreakBlock breakBlockListener;

    @Override
    public void onEnable() {
        instance = this;

        if (!getDataFolder().exists()) getDataFolder().mkdirs();

        saveDefaultConfig();
        saveResourceIfAbsent("messages.yml");

        this.database = new Database(this);
        this.database.cleanupOldBoosts();

        Region.init();

        this.economyManager = new Economic(this);
        this.backpackManager = new BackpackManager(this);

        loadPluginConfig();

        this.boostManager = new BoostManager(this);

        this.menu = new Menu(this);

        new Update(this).check();

        this.breakBlockListener = new BreakBlock(this);
        Bukkit.getPluginManager().registerEvents(breakBlockListener, this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(), this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new Expansion(this).register();
            getLogger().info("PlaceholderAPI подключён.");
        }

        PluginCommand cmd = getCommand("mfarmer");
        if (cmd != null) {
            FarmerCommand farmerCmd = new FarmerCommand(this);
            cmd.setExecutor(farmerCmd);
            cmd.setTabCompleter(farmerCmd);
        }

        getLogger().info("===================================");
        getLogger().info("MFarmer v" + getDescription().getVersion() + " запущен!");
        getLogger().info("Author: Limitely");
        getLogger().info("===================================");

        new Metrics(this, 27687);
    }

    @Override
    public void onDisable() {
        if (breakBlockListener != null) breakBlockListener.cleanup();
        if (database != null) database.close();
        getLogger().info("MFarmer выключен. Данные сохранены.");
    }

    public void loadPluginConfig() {
        reloadConfig();

        this.configCache = new ConfigCache();
        this.configCache.load(getConfig());
        this.configCache.loadMessages(loadMessagesConfig());

        this.groupManager = new GroupManager(this);

        if (menu != null) menu.loadConfig();
    }

    private FileConfiguration loadMessagesConfig() {
        File file = new File(getDataFolder(), "messages.yml");
        if (!file.exists()) saveResourceIfAbsent("messages.yml");
        return YamlConfiguration.loadConfiguration(file);
    }

    private void saveResourceIfAbsent(String name) {
        File file = new File(getDataFolder(), name);
        if (!file.exists()) saveResource(name, false);
    }

    public static MFarmer getInstance() { return instance; }
    public Database getDatabase() { return database; }
    public Economic getEconomyManager() { return economyManager; }
    public BackpackManager getBackpackManager() { return backpackManager; }
    public ConfigCache getConfigCache() { return configCache; }
    public GroupManager getGroupManager() { return groupManager; }
    public BoostManager getBoostManager() { return boostManager; }
    public Menu getMenu() { return menu; }
}
