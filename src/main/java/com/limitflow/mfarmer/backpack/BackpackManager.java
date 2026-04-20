package com.limitflow.mfarmer.backpack;

import com.limitflow.mfarmer.MFarmer;
import org.bukkit.Material;
import org.bukkit.Bukkit;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BackpackManager {

    private final MFarmer plugin;
    private final Map<UUID, Map<Material, Integer>> backpack = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> extraCapacityCache = new ConcurrentHashMap<>();

    public BackpackManager(MFarmer plugin) {
        this.plugin = plugin;
    }

    public void preloadPlayer(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int dbValue = plugin.getDatabase().loadExtraSlots(uuid);
            extraCapacityCache.merge(uuid, dbValue, Math::max);
        });
    }

    public void removeData(UUID uuid) {
        savePlayerState(uuid);
        backpack.remove(uuid);
        extraCapacityCache.remove(uuid);
    }

    public int getAmount(UUID uuid) {
        Map<Material, Integer> crops = backpack.get(uuid);
        if (crops == null) return 0;
        return crops.values().stream().mapToInt(Integer::intValue).sum();
    }

    public Map<Material, Integer> getCropAmounts(UUID uuid) {
        Map<Material, Integer> crops = backpack.get(uuid);
        return crops == null ? Collections.emptyMap() : Collections.unmodifiableMap(crops);
    }

    public void addItem(UUID uuid, Material material) {
        backpack.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .merge(material, 1, Integer::sum);
    }

    public void addItem(UUID uuid, Material material, int amount) {
        backpack.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .merge(material, amount, Integer::sum);
    }

    public void clear(UUID uuid) {
        backpack.remove(uuid);
    }

    public int getBaseCapacity(UUID uuid) {
        var group = plugin.getGroupManager().getGroupByUUID(uuid);
        return (group != null) ? group.capacity() : 64;
    }

    public int getExtraCapacity(UUID uuid) {
        return extraCapacityCache.getOrDefault(uuid, 0);
    }

    public int getTotalCapacity(UUID uuid) {
        return getBaseCapacity(uuid) + getExtraCapacity(uuid);
    }

    public boolean isFull(UUID uuid) {
        return getAmount(uuid) >= getTotalCapacity(uuid);
    }

    public void addExtraCapacity(UUID uuid, int amount) {
        int newValue = extraCapacityCache.merge(uuid, amount, Integer::sum);
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> plugin.getDatabase().saveExtraSlotsSync(uuid, newValue));
    }

    public void setExtraCapacity(UUID uuid, int amount) {
        extraCapacityCache.put(uuid, amount);
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> plugin.getDatabase().saveExtraSlotsSync(uuid, amount));
    }

    public void savePlayerState(UUID uuid) {
        int extra = getExtraCapacity(uuid);
        plugin.getDatabase().saveExtraSlots(uuid, extra);
    }
}
