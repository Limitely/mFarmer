package com.limitflow.mfarmer.event;

import com.limitflow.mfarmer.MFarmer;
import com.limitflow.mfarmer.config.ConfigCache;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class FarmEvent {

    public enum State { IDLE, ACTIVE }

    private final MFarmer plugin;

    private State state = State.IDLE;
    private double eventMultiplier = 2.0;
    private int eventDurationMinutes = 30;
    private long endTime = 0;

    private BukkitTask schedulerTask;
    private BukkitTask countdownTask;

    private final Map<UUID, Integer> eventHarvest = new ConcurrentHashMap<>();

    public FarmEvent(MFarmer plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        ConfigCache cfg = plugin.getConfigCache();
        eventMultiplier = cfg.getEventMultiplier();
        eventDurationMinutes = cfg.getEventDurationMinutes();

        if (schedulerTask != null && !schedulerTask.isCancelled()) {
            schedulerTask.cancel();
        }

        List<String> times = cfg.getEventScheduleTimes();
        if (times.isEmpty()) return;

        schedulerTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            String now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            if (times.contains(now) && state == State.IDLE) {
                Bukkit.getScheduler().runTask(plugin, () -> start(null));
            }
        }, 20L, 20L * 30);
    }

    public void start(String starterName) {
        if (state == State.ACTIVE) {
            Bukkit.broadcastMessage(plugin.getConfigCache().getMessage("event_already_active"));
            return;
        }

        state = State.ACTIVE;
        endTime = System.currentTimeMillis() + (long) eventDurationMinutes * 60_000L;
        eventHarvest.clear();

        String broadcast = plugin.getConfigCache().getMessage("event_start_broadcast")
                .replace("{multiplier}", String.valueOf(eventMultiplier))
                .replace("{minutes}", String.valueOf(eventDurationMinutes))
                .replace("{starter}", starterName != null ? starterName : "Сервер");

        Bukkit.broadcastMessage(broadcast);

        countdownTask = Bukkit.getScheduler().runTaskLater(plugin,
                this::stop, (long) eventDurationMinutes * 60L * 20L);
    }

    public void stop() {
        if (state == State.IDLE) return;
        state = State.IDLE;
        endTime = 0;

        if (countdownTask != null && !countdownTask.isCancelled()) {
            countdownTask.cancel();
        }

        announceWinner();
        eventHarvest.clear();

        Bukkit.broadcastMessage(plugin.getConfigCache().getMessage("event_end_broadcast"));
    }

    private void announceWinner() {
        if (eventHarvest.isEmpty()) return;

        UUID winnerUuid = eventHarvest.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if (winnerUuid == null) return;

        String winnerName = Bukkit.getOfflinePlayer(winnerUuid).getName();
        if (winnerName == null) winnerName = "Unknown";

        int harvested = eventHarvest.get(winnerUuid);
        String rewardCommands = plugin.getConfigCache().getEventWinnerCommands();

        String msg = plugin.getConfigCache().getMessage("event_winner_broadcast")
                .replace("{player}", winnerName)
                .replace("{amount}", String.valueOf(harvested));

        Bukkit.broadcastMessage(msg);

        if (!rewardCommands.isBlank()) {
            final String finalName = winnerName;
            for (String cmd : rewardCommands.split(";")) {
                String finalCmd = cmd.trim().replace("{player}", finalName).replace("{amount}", String.valueOf(harvested));
                Bukkit.getScheduler().runTask(plugin, () ->
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd));
            }
        }
    }

    public void recordHarvest(UUID uuid, int amount) {
        if (state != State.ACTIVE) return;
        eventHarvest.merge(uuid, amount, Integer::sum);
    }

    public boolean isActive() {
        return state == State.ACTIVE;
    }

    public double getMultiplier() {
        return state == State.ACTIVE ? eventMultiplier : 1.0;
    }

    public String getRemainingTime() {
        if (state == State.IDLE) return "00:00";
        long sec = Math.max(0, (endTime - System.currentTimeMillis()) / 1000);
        return String.format("%02d:%02d", sec / 60, sec % 60);
    }

    public int getPlayerHarvest(UUID uuid) {
        return eventHarvest.getOrDefault(uuid, 0);
    }

    public void shutdown() {
        if (schedulerTask != null && !schedulerTask.isCancelled()) schedulerTask.cancel();
        if (countdownTask != null && !countdownTask.isCancelled()) countdownTask.cancel();
    }
}
