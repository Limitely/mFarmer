package com.limitflow.mfarmer.menu;

import com.limitflow.mfarmer.MFarmer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class MenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {

        if (!(e.getInventory().getHolder() instanceof MenuHolder holder)) return;

        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player player)) return;

        String menuId = holder.getId();

        String action = MFarmer.getInstance()
                .getMenu()
                .getAction(menuId, e.getRawSlot());

        if (action == null) return;

        handleAction(player, action);
    }

    private void handleAction(Player player, String action) {

        switch (action) {
            case "salary" -> {
                player.performCommand("mfarmer salary");
                return;
            }
            case "upgrade" -> {
                player.performCommand("mfarmer upgrade");
                return;
            }
            case "top" -> {
                player.performCommand("mfarmer top");
                return;
            }
        }

        if (action.startsWith("cmd:")) {
            String cmd = action.substring(4).trim();
            cmd = cmd.replace("{player}", player.getName());
            player.performCommand(cmd);
            return;
        }

        if (action.startsWith("console:")) {
            String cmd = action.substring(8).trim();
            cmd = cmd.replace("{player}", player.getName());
            final String finalCmd = cmd;
            Bukkit.getScheduler().runTask(MFarmer.getInstance(), () ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd)
            );
            return;
        }

        if (action.startsWith("menu:")) {
            String targetMenu = action.substring(5).trim();
            MFarmer.getInstance().getMenu().openMenu(player, targetMenu);
            return;
        }

        player.performCommand(action);
    }
}
