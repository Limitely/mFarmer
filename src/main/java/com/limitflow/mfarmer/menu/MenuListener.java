package com.limitflow.mfarmer.menu;

import com.limitflow.mfarmer.MFarmer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class MenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {

        if (!(e.getInventory().getHolder() instanceof MenuHolder holder)) return;

        if (!holder.getId().equals("stats")) return;

        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player player)) return;

        String action = MFarmer.getInstance()
                .getMenu()
                .getAction(e.getRawSlot());

        if (action == null) return;

        switch (action) {
            case "salary" -> player.performCommand("mfarmer salary");
            case "upgrade" -> player.performCommand("mfarmer upgrade");
            case "top" -> player.performCommand("mfarmer top");
        }
    }
}