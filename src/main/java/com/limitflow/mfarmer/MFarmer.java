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
import org.bukkit.plugin.java.JavaPlugin;

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

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        saveDefaultConfig();

        this.database = new Database(this);
        this.database.cleanupOldBoosts();

        Region.init();

        this.economyManager = new Economic(this);

        this.backpackManager = new BackpackManager(this);
        this.boostManager = new BoostManager(this);

        loadPluginConfig();

        this.menu = new Menu(this);

        new Update(this).check();

        this.breakBlockListener = new BreakBlock(this);

        Bukkit.getPluginManager().registerEvents(breakBlockListener, this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(), this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new Expansion(this).register();
            getLogger().info("PlaceholderAPI успех!");
        }

        PluginCommand cmd = getCommand("mfarmer");

        if (cmd != null) {
            cmd.setExecutor(new FarmerCommand(this));
        }

        getLogger().info("===================================");
        getLogger().info("MFarmer v" + getDescription().getVersion() + " запущен!");
        getLogger().info("База данных загрузилась.");
        getLogger().info("===================================");

        int pluginId = 27687;
        new Metrics(this, pluginId);
    }

    @Override
    public void onDisable() {

        if (breakBlockListener != null) {
            breakBlockListener.cleanup();
        }

        if (database != null) {
            database.close();
        }

        getLogger().info("MFarmer выключен. Данные на месте.");
    }

    public void loadPluginConfig() {

        reloadConfig();

        this.configCache = new ConfigCache();
        this.configCache.load(getConfig());

        this.groupManager = new GroupManager(this);

        if (menu != null) {
            menu.loadConfig();
        }
    }

    public static MFarmer getInstance() {
        return instance;
    }

    public Database getDatabase() {
        return database;
    }

    public Economic getEconomyManager() {
        return economyManager;
    }

    public BackpackManager getBackpackManager() {
        return backpackManager;
    }

    public ConfigCache getConfigCache() {
        return configCache;
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public BoostManager getBoostManager() {
        return boostManager;
    }

    public Menu getMenu() {
        return menu;
    }
}