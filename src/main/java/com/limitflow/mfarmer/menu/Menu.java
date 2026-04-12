package com.limitflow.mfarmer.menu;

import com.limitflow.mfarmer.MFarmer;
import com.limitflow.mfarmer.command.MenuCommand;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Menu {

    private final MFarmer plugin;
    private YamlConfiguration config;

    private final Map<String, Map<Integer, String>> menuActions = new HashMap<>();
    private final Map<String, String> menuTitles = new HashMap<>();
    private final Map<String, Integer> menuSizes = new HashMap<>();

    private final List<String> registeredCommands = new ArrayList<>();

    private String defaultMenuId = "stats";

    public Menu(MFarmer plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        File file = new File(plugin.getDataFolder(), "menu.yml");
        if (!file.exists()) {
            plugin.saveResource("menu.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);

        menuActions.clear();
        menuTitles.clear();
        menuSizes.clear();

        var menusSection = config.getConfigurationSection("menus");
        if (menusSection == null) {
            loadLegacy();
            return;
        }

        boolean first = true;
        for (String menuId : menusSection.getKeys(false)) {
            var sec = menusSection.getConfigurationSection(menuId);
            if (sec == null) continue;

            String title = com.limitflow.mfarmer.utils.Message.color(sec.getString("title", "Меню"));
            int size = sec.getInt("size", 27);

            menuTitles.put(menuId, title);
            menuSizes.put(menuId, size);
            menuActions.put(menuId, new HashMap<>());

            if (first) {
                defaultMenuId = menuId;
                first = false;
            }

            String command = sec.getString("command");
            if (command != null && !command.isBlank()) {
                registerCommand(command.trim(), menuId);
            }
        }
    }

    private void registerCommand(String commandName, String menuId) {
        try {
            Field f = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            f.setAccessible(true);
            SimpleCommandMap commandMap = (SimpleCommandMap) f.get(Bukkit.getServer());

            if (registeredCommands.contains(commandName)) {
                var known = commandMap.getCommand(commandName);
                if (known != null) known.unregister(commandMap);
            }

            commandMap.register(plugin.getName().toLowerCase(), new MenuCommand(plugin, commandName, menuId));
            registeredCommands.add(commandName);

        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось зарегистрировать команду /" + commandName + ": " + e.getMessage());
        }
    }

    public void openMenu(Player player) {
        openMenu(player, defaultMenuId);
    }

    public void openMenu(Player player, String menuId) {
        if (!menuTitles.containsKey(menuId)) {
            player.sendMessage(com.limitflow.mfarmer.utils.Message.color("&cМеню &e" + menuId + " &cне найдено."));
            return;
        }

        MenuHolder holder = new MenuHolder(menuId);
        Inventory inv = Bukkit.createInventory(holder, menuSizes.get(menuId), menuTitles.get(menuId));
        holder.setInventory(inv);

        Map<Integer, String> actions = menuActions.computeIfAbsent(menuId, k -> new HashMap<>());
        actions.clear();

        var menusSection = config.getConfigurationSection("menus");
        if (menusSection == null) {
            player.openInventory(inv);
            return;
        }

        var menuSection = menusSection.getConfigurationSection(menuId);
        if (menuSection == null) {
            player.openInventory(inv);
            return;
        }

        var items = menuSection.getConfigurationSection("items");
        if (items == null) {
            player.openInventory(inv);
            return;
        }

        for (String key : items.getKeys(false)) {
            var itemSec = items.getConfigurationSection(key);
            if (itemSec == null) continue;

            int slot = itemSec.getInt("slot");
            String materialStr = itemSec.getString("material", "STONE");
            String name = itemSec.getString("name", "");
            var lore = itemSec.getStringList("lore");
            String action = itemSec.getString("action");

            ItemStack item = buildItem(player, materialStr, name, lore);
            inv.setItem(slot, item);

            if (action != null) actions.put(slot, action);
        }

        player.openInventory(inv);
    }

    private ItemStack buildItem(Player player, String materialStr, String name, java.util.List<String> lore) {
        if (materialStr.startsWith("HEAD:")) {
            String texture = materialStr.substring(5).trim();
            return new ItemBuilder(Material.PLAYER_HEAD)
                    .name(apply(player, name))
                    .lore(lore.stream().map(l -> apply(player, l)).toList())
                    .customHead(texture)
                    .build();
        }
        if (materialStr.equalsIgnoreCase("PLAYER_HEAD")) {
            return new ItemBuilder(Material.PLAYER_HEAD)
                    .name(apply(player, name))
                    .lore(lore.stream().map(l -> apply(player, l)).toList())
                    .playerHead(player)
                    .build();
        }
        Material material;
        try {
            material = Material.valueOf(materialStr);
        } catch (Exception e) {
            material = Material.STONE;
        }
        return new ItemBuilder(material)
                .name(apply(player, name))
                .lore(lore.stream().map(l -> apply(player, l)).toList())
                .build();
    }

    public String getAction(String menuId, int slot) {
        Map<Integer, String> actions = menuActions.get(menuId);
        return actions == null ? null : actions.get(slot);
    }

    public String getDefaultMenuId() { return defaultMenuId; }

    private void loadLegacy() {
        String title = com.limitflow.mfarmer.utils.Message.color(config.getString("menu.title", "Статистика"));
        int size = config.getInt("menu.size", 27);
        menuTitles.put("stats", title);
        menuSizes.put("stats", size);
        menuActions.put("stats", new HashMap<>());
        defaultMenuId = "stats";
    }

    private String apply(Player player, String text) {
        return com.limitflow.mfarmer.utils.Message.color(PlaceholderAPI.setPlaceholders(player, text));
    }
}
