package com.limitflow.mfarmer.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Region {

    private static RegionQuery query;
    private static boolean enabled = false;

    public static void init() {
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {
            enabled = false;
            return;
        }

        try {
            RegionContainer container = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer();

            query = container.createQuery();
            enabled = true;

        } catch (Throwable t) {
            enabled = false;
        }
    }

    public static boolean isInRegion(Player player, Collection<String> regionNames) {

        if (!enabled || query == null) {
            return true;
        }

        if (regionNames == null || regionNames.isEmpty()) {
            return false;
        }

        Set<String> names = new HashSet<>();
        for (String name : regionNames) {
            names.add(name.toLowerCase());
        }

        var regions = query.getApplicableRegions(
                BukkitAdapter.adapt(player.getLocation())
        );

        for (var region : regions) {
            if (names.contains(region.getId().toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    public static boolean isEnabled() {
        return enabled;
    }
}