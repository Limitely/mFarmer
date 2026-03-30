package com.limitflow.mfarmer.group;

import com.limitflow.mfarmer.MFarmer;
import com.limitflow.mfarmer.model.GroupData;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GroupManager {

    private final List<GroupData> sortedGroups;

    public GroupManager(MFarmer plugin) {
        this.sortedGroups = new ArrayList<>(plugin.getConfigCache().getGroups().values());
        this.sortedGroups.sort(Comparator.comparingDouble(GroupData::price).reversed());
    }

    public GroupData getGroup(Player player) {
        return sortedGroups.stream()
                .filter(g -> g.permission().isEmpty() || player.hasPermission(g.permission()))
                .findFirst()
                .orElse(null);
    }
}
