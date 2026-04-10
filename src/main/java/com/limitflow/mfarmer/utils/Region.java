package com.limitflow.mfarmer.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.entity.Player;

import java.util.List;

public class Region {

    private static RegionQuery query;

    public static void init() {
        query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
    }

    public static boolean isInRegion(Player player, List<String> regionNames) {
        if (query == null) return false;
        for (var region : query.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()))) {
            for (String name : regionNames) {
                if (region.getId().equalsIgnoreCase(name)) return true;
            }
        }
        return false;
    }
}
