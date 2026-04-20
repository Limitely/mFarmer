package com.limitflow.mfarmer.utils;

import com.limitflow.mfarmer.MFarmer;
import com.limitflow.mfarmer.database.Database;
import com.limitflow.mfarmer.model.GroupData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Expansion extends PlaceholderExpansion {

    private final MFarmer plugin;

    private final List<Database.TopEntry> topCache = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public Expansion(MFarmer plugin) {
        this.plugin = plugin;
        scheduler.scheduleAtFixedRate(this::refreshTop, 0, 60, TimeUnit.SECONDS);
    }

    private void refreshTop() {
        List<Database.TopEntry> fresh = plugin.getDatabase().getTopEntries(10);
        topCache.clear();
        topCache.addAll(fresh);
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

    @Override public @NotNull String getIdentifier() { return "mfarmer"; }
    @Override public @NotNull String getAuthor()     { return "Limitely"; }
    @Override public @NotNull String getVersion()    { return "1.0.3"; }
    @Override public boolean persist()               { return true; }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        String p = params.toLowerCase();

        if (p.startsWith("top_name_")) {
            int pos = parsePos(p.substring(9));
            if (pos < 0) return null;
            Database.TopEntry e = getTopEntry(pos);
            if (e == null) return plugin.getConfigCache().getMessage("top_unknown", "—");
            String name = Bukkit.getOfflinePlayer(e.uuid()).getName();
            return name != null ? name : plugin.getConfigCache().getMessage("top_unknown", "—");
        }

        if (p.startsWith("top_money_")) {
            int pos = parsePos(p.substring(10));
            if (pos < 0) return null;
            Database.TopEntry e = getTopEntry(pos);
            if (e == null) return "—";
            return plugin.getConfigCache().formatMoney(e.earned())
                    + " " + plugin.getConfigCache().getCurrencySymbol();
        }

        if (p.startsWith("top_raw_")) {
            int pos = parsePos(p.substring(8));
            if (pos < 0) return null;
            Database.TopEntry e = getTopEntry(pos);
            return e == null ? "0" : plugin.getConfigCache().formatMoney(e.earned());
        }

        if (player == null) return "";

        UUID uuid = player.getUniqueId();
        int amount     = plugin.getBackpackManager().getAmount(uuid);
        int bonusSlots = plugin.getBackpackManager().getExtraCapacity(uuid);
        GroupData group = plugin.getGroupManager().getGroup(player);
        double basePrice = group != null ? group.price() : 0.0;
        int capacity     = group != null ? group.capacity() + bonusSlots : bonusSlots;
        double multiplier = plugin.getBoostManager().getMultiplier(uuid);
        double eventMult  = plugin.getFarmEvent().getMultiplier();
        double totalMult  = multiplier * eventMult;
        String currency   = plugin.getConfigCache().getCurrencySymbol();

        double salaryValue = 0;
        for (var entry : plugin.getBackpackManager().getCropAmounts(uuid).entrySet()) {
            var cropConfig = plugin.getConfigCache().getCropConfig(entry.getKey());
            double cropMult = cropConfig != null ? cropConfig.priceMultiplier() : 1.0;
            salaryValue += basePrice * entry.getValue() * cropMult * totalMult;
        }

        return switch (p) {
            case "amount"           -> String.valueOf(amount);
            case "capacity"         -> String.valueOf(capacity);
            case "bonus_slots"      -> String.valueOf(bonusSlots);
            case "price"            -> plugin.getConfigCache().formatMoney(basePrice * totalMult) + " " + currency;
            case "multiplier"       -> String.format("%.1fx", multiplier);
            case "boost_time"       -> plugin.getBoostManager().getGlobalRemainingTime();
            case "local_boost_time" -> plugin.getBoostManager().getLocalRemainingTime(uuid);
            case "salary"           -> plugin.getConfigCache().formatMoney(salaryValue) + " " + currency;
            case "balance"          -> plugin.getConfigCache().formatMoney(plugin.getEconomyManager().getBalance(player)) + " " + currency;
            case "event_active"     -> plugin.getFarmEvent().isActive() ? "Да" : "Нет";
            case "event_multiplier" -> String.format("%.1fx", eventMult);
            case "event_time"       -> plugin.getFarmEvent().getRemainingTime();
            case "event_harvest"    -> String.valueOf(plugin.getFarmEvent().getPlayerHarvest(uuid));
            case "name"             -> player.getName();
            case "level"            -> String.valueOf(player.getLevel());
            default                 -> null;
        };
    }

    private int parsePos(String s) {
        try {
            int n = Integer.parseInt(s.trim());
            return (n >= 1 && n <= 10) ? n : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private Database.TopEntry getTopEntry(int pos) {
        int idx = pos - 1;
        return idx < topCache.size() ? topCache.get(idx) : null;
    }
}
