package com.limitflow.mfarmer.utils;

import com.limitflow.mfarmer.MFarmer;
import com.limitflow.mfarmer.model.GroupData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class Expansion extends PlaceholderExpansion {

    private final MFarmer plugin;

    public Expansion(MFarmer plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "mfarmer";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Limitely";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.1";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        UUID uuid = player.getUniqueId();
        int amount = plugin.getBackpackManager().getAmount(uuid);
        int bonusSlots = plugin.getBackpackManager().getExtraCapacity(uuid);
        GroupData group = plugin.getGroupManager().getGroup(player);

        double basePrice = (group != null) ? group.price() : 0.0;
        int capacity = (group != null) ? (group.capacity() + bonusSlots) : bonusSlots;
        double currentMultiplier = plugin.getBoostManager().getMultiplier(uuid);

        return switch (params.toLowerCase()) {
            case "amount"          -> String.valueOf(amount);
            case "capacity"        -> String.valueOf(capacity);
            case "bonus_slots"     -> String.valueOf(bonusSlots);
            case "price"           -> String.format("%.2f", basePrice * currentMultiplier);
            case "multiplier"      -> String.format("%.1fx", currentMultiplier);
            case "boost_time"      -> plugin.getBoostManager().getGlobalRemainingTime();
            case "local_boost_time"-> plugin.getBoostManager().getLocalRemainingTime(uuid);
            case "salary"          -> String.format("%.2f", (double) amount * basePrice * currentMultiplier);
            default                -> null;
        };
    }
}
