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
        loadGlobalBoostFromDb();
    }

    public void setGlobalBoost(double multiplier, int minutes) {
        globalMultiplier = multiplier;
        globalBoostEndTime = System.currentTimeMillis() + minutes * 60_000L;
        plugin.getDatabase().saveGlobalBoost(multiplier, globalBoostEndTime);
        Bukkit.broadcastMessage(
                plugin.getConfigCache().getMessage("boost_start_broadcast")
                        .replace("{multiplier}", String.valueOf(multiplier))
                        .replace("{minutes}", String.valueOf(minutes))
        );
    }

    public void stopGlobalBoost() {
        globalMultiplier = 1.0;
        globalBoostEndTime = 0;
        plugin.getDatabase().saveGlobalBoost(1.0, 0);
    }

    private void loadGlobalBoostFromDb() {
        Thread thread = new Thread(() -> {
            double[] data = plugin.getDatabase().loadGlobalBoost();
            if (data == null) return;
            double mult = data[0];
            long end = (long) data[1];
            if (end > System.currentTimeMillis()) {
                globalMultiplier = mult;
                globalBoostEndTime = end;
            } else {
                globalMultiplier = 1.0;
                globalBoostEndTime = 0;
                plugin.getDatabase().saveGlobalBoost(1.0, 0);
            }
        }, "mFarmer-boost-load");
        thread.setDaemon(true);
        thread.start();
    }

    public void loadPlayerBoost(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            double[] data = plugin.getDatabase().loadLocalBoost(uuid);
            if (data == null) return;
            double mult = data[0];
            long end = (long) data[1];
            if (end > System.currentTimeMillis()) {
                localBoosts.put(uuid, new PersonalBoost(mult, end));
            } else {
                plugin.getDatabase().saveLocalBoost(uuid, 1.0, 0);
            }
        });
    }

    public void unloadPlayerBoost(UUID uuid) {
        localBoosts.remove(uuid);
    }

    public void setLocalBoost(UUID uuid, double multiplier, int minutes) {
        long end = System.currentTimeMillis() + minutes * 60_000L;
        localBoosts.put(uuid, new PersonalBoost(multiplier, end));
        plugin.getDatabase().saveLocalBoost(uuid, multiplier, end);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.sendMessage(
                    plugin.getConfigCache().getMessage("personal_boost_received")
                            .replace("{multiplier}", String.valueOf(multiplier))
                            .replace("{minutes}", String.valueOf(minutes))
            );
        }
    }

    public void stopLocalBoost(UUID uuid) {
        localBoosts.remove(uuid);
        plugin.getDatabase().saveLocalBoost(uuid, 1.0, 0);
    }

    public double getMultiplier(UUID uuid) {
        long now = System.currentTimeMillis();
        double result = 1.0;

        if (now < globalBoostEndTime) {
            result *= globalMultiplier;
        }

        PersonalBoost boost = localBoosts.get(uuid);
        if (boost != null) {
            if (now < boost.endTime()) {
                result *= boost.multiplier();
            } else {
                localBoosts.remove(uuid);
                plugin.getDatabase().saveLocalBoost(uuid, 1.0, 0);
            }
        }

        return result;
    }

    public String getGlobalRemainingTime() {
        return format(globalBoostEndTime);
    }

    public String getLocalRemainingTime(UUID uuid) {
        PersonalBoost b = localBoosts.get(uuid);
        if (b == null) return "00:00";
        return format(b.endTime());
    }

    private String format(long endTime) {
        long sec = Math.max(0, (endTime - System.currentTimeMillis()) / 1000);
        return String.format("%02d:%02d", sec / 60, sec % 60);
    }
}
