package com.limitflow.mfarmer.backpack;

import com.limitflow.mfarmer.MFarmer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BackpackManager {

    private final MFarmer plugin;
    private final Map<UUID, Integer> backpack = new HashMap<>();
    private final Map<UUID, Integer> extraCapacityCache = new HashMap<>();

    public BackpackManager(MFarmer plugin) {
        this.plugin = plugin;
    }

    public int getAmount(UUID uuid) {
        return backpack.getOrDefault(uuid, 0);
    }

    public int getExtraCapacity(UUID uuid) {
        return extraCapacityCache.computeIfAbsent(uuid, id -> plugin.getDatabase().loadExtraSlots(id));
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
