package com.limitflow.mfarmer.config;

import com.limitflow.mfarmer.model.GroupData;
import com.limitflow.mfarmer.utils.Message;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ConfigCache {

    private final Map<String, GroupData> groups = new HashMap<>();
    private final Map<String, String> messages = new HashMap<>();
    private final Map<String, List<String>> messageLists = new HashMap<>();
    private final Map<Material, CropConfig> crops = new HashMap<>();

    private long restoreDelay = 20L;
    private List<String> regions = new ArrayList<>();
    private String soundType = "ENTITY_ITEM_PICKUP";
    private float soundVolume = 1f;
    private float soundPitch = 1f;

    private double upgradeBaseCost = 1000.0;
    private double upgradeCostPerSlot = 10.0;
    private int upgradeSlotsPerLevel = 10;
    private int maxExtraCapacity = 0;

    public void load(@NotNull FileConfiguration config) {
        groups.clear();
        crops.clear();

        ConfigurationSection groupsSection = config.getConfigurationSection("groups");
        if (groupsSection != null) {
            groupsSection.getKeys(false).forEach(key -> {
                String perm = config.getString("groups." + key + ".permission", "");
                double price = config.getDouble("groups." + key + ".price", 1.0);
                int capacity = config.getInt("groups." + key + ".capacity", 64);
                groups.put(key, new GroupData(perm, price, capacity));
            });
        }

        restoreDelay = config.getLong("settings.restoreDelay", 20L);

        if (config.isList("settings.regions")) {
            regions = config.getStringList("settings.regions");
        } else {
            String single = config.getString("settings.region", "farmer_zone");
            regions = new ArrayList<>(List.of(single));
        }

        soundType   = config.getString("settings.sound.type", "ENTITY_ITEM_PICKUP");
        soundVolume = (float) config.getDouble("settings.sound.volume", 1.0);
        soundPitch  = (float) config.getDouble("settings.sound.pitch", 1.0);

        upgradeBaseCost      = config.getDouble("upgrade.baseCost", 1000.0);
        upgradeCostPerSlot   = config.getDouble("upgrade.costPerSlot", 10.0);
        upgradeSlotsPerLevel = config.getInt("upgrade.slotsPerLevel", 10);
        maxExtraCapacity     = config.getInt("upgrade.maxExtraCapacity", 0);

        ConfigurationSection cropsSection = config.getConfigurationSection("crops");
        if (cropsSection != null) {
            for (String key : cropsSection.getKeys(false)) {
                String matName = config.getString("crops." + key + ".material", key.toUpperCase());
                double mult    = config.getDouble("crops." + key + ".priceMultiplier", 1.0);
                String display = config.getString("crops." + key + ".displayName", key);
                try {
                    Material mat = Material.valueOf(matName.toUpperCase());
                    crops.put(mat, new CropConfig(mat, mult, display));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        if (crops.isEmpty()) {
            crops.put(Material.WHEAT, new CropConfig(Material.WHEAT, 1.0, "Пшеница"));
        }
    }

    public void loadMessages(@NotNull FileConfiguration msgConfig) {
        messages.clear();
        messageLists.clear();

        ConfigurationSection msgSection = msgConfig.getConfigurationSection("messages");
        if (msgSection == null) return;

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

    @Nullable
    public CropConfig getCropConfig(Material material) {
        return crops.get(material);
    }

    public Map<Material, CropConfig> getCrops() {
        return Collections.unmodifiableMap(crops);
    }

    public Map<String, GroupData> getGroups() { return groups; }
    public long getRestoreDelay() { return restoreDelay; }
    public List<String> getRegions() { return regions; }
    public String getSoundType() { return soundType; }
    public float getSoundVolume() { return soundVolume; }
    public float getSoundPitch() { return soundPitch; }
    public double getUpgradeBaseCost() { return upgradeBaseCost; }
    public double getUpgradeCostPerSlot() { return upgradeCostPerSlot; }
    public int getUpgradeSlotsPerLevel() { return upgradeSlotsPerLevel; }
    public int getMaxExtraCapacity() { return maxExtraCapacity; }

    public List<String> getMessageList(String key) {
        return messageLists.getOrDefault(key, new ArrayList<>());
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, "§cMessage not found: " + key);
    }

    public String getMessage(String key, String def) {
        return messages.getOrDefault(key, Message.color(def));
    }
}
