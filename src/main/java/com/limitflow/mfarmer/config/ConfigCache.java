package com.limitflow.mfarmer.config;

import com.limitflow.mfarmer.model.GroupData;
import com.limitflow.mfarmer.utils.Message;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigCache {

    private final Map<String, GroupData> groups = new HashMap<>();
    private final Map<String, String> messages = new HashMap<>();
    private final Map<String, List<String>> messageLists = new HashMap<>();

    private long restoreDelay = 20L;
    private String region = "farmer_zone";
    private String soundType = "ENTITY_ITEM_PICKUP";
    private float soundVolume = 1f;
    private float soundPitch = 1f;

    public void load(@NotNull FileConfiguration config) {
        groups.clear();
        messages.clear();
        messageLists.clear();

        ConfigurationSection groupsSection = config.getConfigurationSection("groups");
        if (groupsSection != null) {
            groupsSection.getKeys(false).forEach(key -> {
                String perm = config.getString("groups." + key + ".permission", "");
                double price = config.getDouble("groups." + key + ".price", 1.0);
                int capacity = config.getInt("groups." + key + ".capacity", 64);
                groups.put(key, new GroupData(perm, price, capacity));
            });
        }

        ConfigurationSection msgSection = config.getConfigurationSection("messages");
        if (msgSection != null) {
            for (String key : msgSection.getKeys(false)) {
                if (msgSection.isList(key)) {
                    List<String> list = msgSection.getStringList(key);
                    list.replaceAll(Message::color);
                    messageLists.put(key, list);
                } else {
                    messages.put(key, Message.color(msgSection.getString(key, "")));
                }
            }
        }

        restoreDelay = config.getLong("settings.restoreDelay", 20L);
        region = config.getString("settings.region", "farmer_zone");
        soundType = config.getString("settings.sound.type", "ENTITY_ITEM_PICKUP");
        soundVolume = (float) config.getDouble("settings.sound.volume", 1.0);
        soundPitch = (float) config.getDouble("settings.sound.pitch", 1.0);
    }

    public List<String> getMessageList(String key) {
        return messageLists.getOrDefault(key, new ArrayList<>());
    }

    public Map<String, GroupData> getGroups() {
        return groups;
    }

    public long getRestoreDelay() {
        return restoreDelay;
    }

    public String getRegion() {
        return region;
    }

    public String getSoundType() {
        return soundType;
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public float getSoundPitch() {
        return soundPitch;
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, "§cMessage not found: " + key);
    }

    public String getMessage(String key, String def) {
        return messages.getOrDefault(key, Message.color(def));
    }
}
