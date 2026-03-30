package com.limitflow.mfarmer.boost;

import com.limitflow.mfarmer.MFarmer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BoostManager {

    private final MFarmer plugin;

    private double globalMultiplier = 1.0;
    private long globalBoostEndTime = 0;

    private record PersonalBoost(double multiplier, long endTime) {}
    private final Map<UUID, PersonalBoost> localBoosts = new HashMap<>();

    public BoostManager(MFarmer plugin) {
        this.plugin = plugin;
        loadBoostsFromDb();
    }

    public void setGlobalBoost(double multiplier, int minutes) {
        this.globalMultiplier = multiplier;
        this.globalBoostEndTime = System.currentTimeMillis() + (minutes * 60_000L);

        String msg = plugin.getConfigCache().getMessage("boost_start_broadcast")
                .replace("{multiplier}", String.valueOf(multiplier))
                .replace("{minutes}", String.valueOf(minutes));

        Bukkit.broadcastMessage(msg);
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

    private void loadBoostsFromDb() {
        plugin.getDatabase().loadAllActiveBoosts().forEach((uuid, data) ->
                localBoosts.put(uuid, new PersonalBoost(data[0], data[1].longValue())));
    }

    public double getMultiplier(UUID uuid) {
        double total = 1.0;
        long now = System.currentTimeMillis();

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

    public void stopGlobalBoost() {
        this.globalMultiplier = 1.0;
        this.globalBoostEndTime = 0;
    }

    public void stopLocalBoost(UUID uuid) {
        localBoosts.remove(uuid);
        plugin.getDatabase().saveLocalBoost(uuid, 1.0, 0);
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
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }
}
