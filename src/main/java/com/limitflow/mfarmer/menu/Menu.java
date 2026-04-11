package com.limitflow.mfarmer.menu;

import com.limitflow.mfarmer.MFarmer;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Menu {

    private final MFarmer plugin;
    private YamlConfiguration config;

    private final Map<Integer, String> actions = new HashMap<>();

    private String title;
    private int size;

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

        title = com.limitflow.mfarmer.utils.Message.color(
                config.getString("menu.title", "Статистика")
        );

        size = config.getInt("menu.size", 27);
    }

    public void openMenu(Player player) {

        MenuHolder holder = new MenuHolder("stats");

        Inventory inv = Bukkit.createInventory(holder, size, title);

        holder.setInventory(inv);

        actions.clear();

        var items = config.getConfigurationSection("menu.items");

        if (items == null) {
            player.openInventory(inv);
            return;
        }

        for (String key : items.getKeys(false)) {

            var sec = items.getConfigurationSection(key);
            if (sec == null) continue;

            int slot = sec.getInt("slot");

            Material material;
            try {
                material = Material.valueOf(sec.getString("material", "STONE"));
            } catch (Exception e) {
                material = Material.STONE;
            }

            String name = sec.getString("name", "");
            var lore = sec.getStringList("lore");
            String action = sec.getString("action");

            ItemStack item = new ItemBuilder(material)
                    .name(apply(player, name))
                    .lore(
                            lore.stream()
                                    .map(line -> apply(player, line))
                                    .toList()
                    )
                    .build();

            inv.setItem(slot, item);

            if (action != null) {
                actions.put(slot, action);
            }
        }

        player.openInventory(inv);
    }

    public String getAction(int slot) {
        return actions.get(slot);
    }

    private String apply(Player player, String text) {
        return com.limitflow.mfarmer.utils.Message.color(
                PlaceholderAPI.setPlaceholders(player, text)
        );
    }
}