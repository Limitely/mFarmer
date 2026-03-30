package com.limitflow.mfarmer.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.entity.Player;

public class Region {

    private static RegionQuery query;

    public static void init() {
        query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
    }

    public static boolean isInRegion(Player player, String regionName) {
        if (query == null) return false;
        for (var region : query.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()))) {
            if (region.getId().equalsIgnoreCase(regionName)) return true;
        }
        return false;
    }
}
