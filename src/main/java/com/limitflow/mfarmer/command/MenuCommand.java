package com.limitflow.mfarmer.command;

import com.limitflow.mfarmer.MFarmer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MenuCommand extends Command {

    private final MFarmer plugin;
    private final String menuId;

    public MenuCommand(MFarmer plugin, String name, String menuId) {
        super(name);
        this.plugin = plugin;
        this.menuId = menuId;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;
        plugin.getMenu().openMenu(player, menuId);
        return true;
    }
}
