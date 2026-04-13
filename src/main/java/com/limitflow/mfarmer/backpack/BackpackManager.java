package com.limitflow.mfarmer.backpack;

import com.limitflow.mfarmer.MFarmer;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BackpackManager {

    private final MFarmer plugin;
    private final Map<UUID, Integer> backpack = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> extraCapacityCache = new ConcurrentHashMap<>();

    public BackpackManager(MFarmer plugin) {
        this.plugin = plugin;
    }

    public void preloadPlayer(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int slots = plugin.getDatabase().loadExtraSlots(uuid);
            extraCapacityCache.put(uuid, slots);
        });
    }

    public int getAmount(UUID uuid) {
        return backpack.getOrDefault(uuid, 0);
    }

    public int getExtraCapacity(UUID uuid) {
        return extraCapacityCache.getOrDefault(uuid, 0);
    }

    public void addExtraCapacity(UUID uuid, int amount) {
        int newTotal = getExtraCapacity(uuid) + amount;
        extraCapacityCache.put(uuid, newTotal);
        plugin.getDatabase().saveExtraSlots(uuid, newTotal);
    }

    public void setExtraCapacity(UUID uuid, int amount) {
        extraCapacityCache.put(uuid, amount);
        plugin.getDatabase().saveExtraSlots(uuid, amount);
    }

    public void addItem(UUID uuid) {
        backpack.merge(uuid, 1, Integer::sum);
    }

    public void clear(UUID uuid) {
        backpack.remove(uuid);
    }

    public void removeData(UUID uuid) {
        backpack.remove(uuid);
        extraCapacityCache.remove(uuid);
    }

    public boolean isFull(UUID uuid, int maxSize) {
        return getAmount(uuid) >= maxSize;
    }
}
