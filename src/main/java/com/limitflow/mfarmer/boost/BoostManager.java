package com.limitflow.mfarmer.boost;

import com.limitflow.mfarmer.MFarmer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BoostManager {

    private final MFarmer plugin;

    private volatile double globalMultiplier = 1.0;
    private volatile long globalBoostEndTime = 0;

    private record PersonalBoost(double multiplier, long endTime) {}

    private final Map<UUID, PersonalBoost> localBoosts = new ConcurrentHashMap<>();

    public BoostManager(MFarmer plugin) {
        this.plugin = plugin;
        loadBoostsFromDb();
        loadGlobalBoostFromDb();
    }

    public void setGlobalBoost(double multiplier, int minutes) {
        globalMultiplier = multiplier;
        globalBoostEndTime = System.currentTimeMillis() + (minutes * 60_000L);
        plugin.getDatabase().saveGlobalBoost(globalMultiplier, globalBoostEndTime);

        String msg = plugin.getConfigCache().getMessage("boost_start_broadcast")
                .replace("{multiplier}", String.valueOf(multiplier))
                .replace("{minutes}", String.valueOf(minutes));
        Bukkit.broadcastMessage(msg);
    }

    public void stopGlobalBoost() {
        globalMultiplier = 1.0;
        globalBoostEndTime = 0;
        plugin.getDatabase().saveGlobalBoost(1.0, 0);
    }

    private void loadGlobalBoostFromDb() {
        double[] data = plugin.getDatabase().loadGlobalBoost();
        if (data != null) {
            globalMultiplier = data[0];
            globalBoostEndTime = (long) data[1];
            long remaining = (globalBoostEndTime - System.currentTimeMillis()) / 1000;
            plugin.getLogger().info("Восстановлен глобальный буст x" + globalMultiplier +
                                    ", осталось " + formatTime(remaining));
        }
    }

    public void setLocalBoost(UUID uuid, double multiplier, int minutes) {
        long endTime = System.currentTimeMillis() + (minutes * 60_000L);
        localBoosts.put(uuid, new PersonalBoost(multiplier, endTime));
        plugin.getDatabase().saveLocalBoost(uuid, multiplier, endTime);

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            String msg = plugin.getConfigCache().getMessage("personal_boost_received")
                    .replace("{multiplier}", String.valueOf(multiplier))
                    .replace("{minutes}", String.valueOf(minutes));
            player.sendMessage(msg);
        }
    }

    public void stopLocalBoost(UUID uuid) {
        localBoosts.remove(uuid);
        plugin.getDatabase().saveLocalBoost(uuid, 1.0, 0);
    }

    private void loadBoostsFromDb() {
        plugin.getDatabase().loadAllActiveBoosts().forEach((uuid, data) ->
                localBoosts.put(uuid, new PersonalBoost(data[0], data[1].longValue())));
    }

    public double getMultiplier(UUID uuid) {
        long now = System.currentTimeMillis();
        double total = 1.0;

        if (now < globalBoostEndTime) {
            total *= globalMultiplier;
        }

        PersonalBoost personal = localBoosts.get(uuid);
        if (personal != null) {
            if (now < personal.endTime()) {
                total *= personal.multiplier();
            } else {
                localBoosts.remove(uuid);
            }
        }

        return total;
    }

    public String getGlobalRemainingTime() {
        long seconds = (globalBoostEndTime - System.currentTimeMillis()) / 1000;
        return seconds > 0 ? formatTime(seconds) : "00:00";
    }

    public String getLocalRemainingTime(UUID uuid) {
        PersonalBoost personal = localBoosts.get(uuid);
        if (personal == null) return "00:00";
        long seconds = (personal.endTime() - System.currentTimeMillis()) / 1000;
        return seconds > 0 ? formatTime(seconds) : "00:00";
    }

    private String formatTime(long seconds) {
        long m = seconds / 60;
        long s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }
}
