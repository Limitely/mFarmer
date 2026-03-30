package com.limitflow.mfarmer.command;

import com.limitflow.mfarmer.MFarmer;
import com.limitflow.mfarmer.model.GroupData;
import com.limitflow.mfarmer.utils.Message;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class FarmerCommand implements CommandExecutor {

    private final MFarmer plugin;

    public FarmerCommand(MFarmer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "adminboost" -> { handleAdminBoost(sender, args); return true; }
                case "giveboost"  -> { handleGiveBoost(sender, args);  return true; }
                case "stopboost"  -> { handleStopBoost(sender, args);  return true; }
            }
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Только игроки могут использовать основные команды.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "salary"  -> handleSalary(player);
            case "upgrade" -> handleUpgrade(player);
            case "reload"  -> handleReload(player);
            default -> player.sendMessage(plugin.getConfigCache().getMessage("unknown_command", "&cНеизвестная подкоманда."));
        }

        return true;
    }

    private void handleSalary(Player player) {
        UUID uuid = player.getUniqueId();
        int amount = plugin.getBackpackManager().getAmount(uuid);

        if (amount <= 0) {
            player.sendMessage(plugin.getConfigCache().getMessage("salary_fail"));
            return;
        }

        GroupData group = plugin.getGroupManager().getGroup(player);
        if (group == null) {
            player.sendMessage(Message.color("&cОшибка: группа не найдена."));
            return;
        }

        double multiplier = plugin.getBoostManager().getMultiplier(uuid);
        double total = group.price() * amount * multiplier;

        plugin.getEconomyManager().deposit(player, total);
        plugin.getBackpackManager().clear(uuid);

        player.sendMessage(plugin.getConfigCache().getMessage("salary_success")
                .replace("{amount}", String.valueOf(amount))
                .replace("{money}", String.format("%.2f", total)));
    }

    private void handleUpgrade(Player player) {
        UUID uuid = player.getUniqueId();
        int currentExtra = plugin.getBackpackManager().getExtraCapacity(uuid);
        double price = 1000.0 + (currentExtra * 10.0);

        if (!plugin.getEconomyManager().hasEnough(player, price)) {
            player.sendMessage(plugin.getConfigCache().getMessage("upgrade_no_money")
                    .replace("{price}", String.format("%.2f", price)));
            return;
        }

        plugin.getEconomyManager().withdraw(player, price);
        plugin.getBackpackManager().addExtraCapacity(uuid, 10);

        player.sendMessage(plugin.getConfigCache().getMessage("upgrade_success")
                .replace("{bonus}", String.valueOf(currentExtra + 10)));
    }

    private void handleGiveBoost(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mfarmer.admin")) {
            sender.sendMessage(plugin.getConfigCache().getMessage("no_permission"));
            return;
        }

        if (args.length < 4) {
            sender.sendMessage(Message.color("&cИспользование: /mfarmer giveboost <игрок> <множитель> <минуты>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Message.color("&cИгрок не найден."));
            return;
        }

        try {
            double mult = Double.parseDouble(args[2]);
            int mins = Integer.parseInt(args[3]);
            plugin.getBoostManager().setLocalBoost(target.getUniqueId(), mult, mins);
            sender.sendMessage(Message.color("&aВы успешно выдали личный буст игроку " + target.getName()));
        } catch (NumberFormatException e) {
            sender.sendMessage(Message.color("&cВведите корректные числа."));
        }
    }

    private void handleAdminBoost(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mfarmer.admin")) {
            sender.sendMessage(plugin.getConfigCache().getMessage("no_permission"));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(Message.color("&cИспользование: /mfarmer adminboost <множитель> <минуты>"));
            return;
        }

        try {
            double mult = Double.parseDouble(args[1]);
            int mins = Integer.parseInt(args[2]);
            plugin.getBoostManager().setGlobalBoost(mult, mins);
            sender.sendMessage(Message.color("&aГлобальный буст активирован!"));
        } catch (NumberFormatException e) {
            sender.sendMessage(Message.color("&cВведите корректные числа."));
        }
    }

    private void handleStopBoost(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mfarmer.admin")) {
            sender.sendMessage(plugin.getConfigCache().getMessage("no_permission"));
            return;
        }

        if (args.length == 1) {
            plugin.getBoostManager().stopGlobalBoost();
            sender.sendMessage(Message.color("&aГлобальный буст остановлен."));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Message.color("&cИгрок не найден."));
            return;
        }

        plugin.getBoostManager().stopLocalBoost(target.getUniqueId());
        sender.sendMessage(Message.color("&aЛичный буст игрока " + target.getName() + " аннулирован."));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("mfarmer.reload")) {
            sender.sendMessage(plugin.getConfigCache().getMessage("no_permission"));
            return;
        }
        plugin.reloadConfig();
        plugin.loadPluginConfig();
        sender.sendMessage(plugin.getConfigCache().getMessage("reloaded"));
    }

    private void sendHelp(Player player) {
        plugin.getConfigCache().getMessageList("help_player").forEach(player::sendMessage);
        if (player.hasPermission("mfarmer.admin")) {
            plugin.getConfigCache().getMessageList("help_admin").forEach(player::sendMessage);
        }
    }
}
