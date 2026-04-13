package com.limitflow.mfarmer.command;

import com.limitflow.mfarmer.MFarmer;
import com.limitflow.mfarmer.model.GroupData;
import com.limitflow.mfarmer.utils.Message;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class FarmerCommand implements CommandExecutor, TabCompleter {

    private final MFarmer plugin;

    public FarmerCommand(MFarmer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command cmd,
                             @NotNull String label,
                             @NotNull String[] args) {

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Только игроки могут использовать основные команды.");
                return true;
            }
            plugin.getMenu().openMenu(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "menu" -> {
                if (sender instanceof Player player) {
                    String menuId = args.length >= 2 ? args[1] : plugin.getMenu().getDefaultMenuId();
                    plugin.getMenu().openMenu(player, menuId);
                } else {
                    sender.sendMessage("Только игроки могут открывать меню.");
                }
            }
            case "help"      -> { if (sender instanceof Player player) sendHelp(player); }
            case "salary"    -> { if (sender instanceof Player player) handleSalary(player); }
            case "upgrade"   -> { if (sender instanceof Player player) handleUpgrade(player); }
            case "top"       -> { if (sender instanceof Player player) handleTop(player); }
            case "giveboost"  -> handleGiveBoost(sender, args);
            case "adminboost" -> handleAdminBoost(sender, args);
            case "stopboost"  -> handleStopBoost(sender, args);
            case "reload"     -> handleReload(sender);
            default -> sender.sendMessage(
                    plugin.getConfigCache().getMessage("unknown_command", "&cНеизвестная подкоманда."));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("help", "salary", "upgrade", "top", "menu"));
            if (sender.hasPermission("mfarmer.admin")) {
                subs.addAll(List.of("giveboost", "adminboost", "stopboost"));
            }
            if (sender.hasPermission("mfarmer.reload")) {
                subs.add("reload");
            }
            String prefix = args[0].toLowerCase();
            return subs.stream().filter(s -> s.startsWith(prefix)).collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if ((sub.equals("giveboost") || sub.equals("stopboost")) && sender.hasPermission("mfarmer.admin")) {
                String prefix = args[1].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(prefix))
                        .collect(Collectors.toList());
            }
        }

        return List.of();
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
        plugin.getDatabase().addEarned(uuid, total);
        plugin.getBackpackManager().clear(uuid);

        player.sendMessage(plugin.getConfigCache().getMessage("salary_success")
                .replace("{amount}", String.valueOf(amount))
                .replace("{money}", String.format("%.2f", total)));
    }

    private void handleUpgrade(Player player) {
        UUID uuid = player.getUniqueId();
        int currentExtra = plugin.getBackpackManager().getExtraCapacity(uuid);

        int maxExtra = plugin.getConfigCache().getMaxExtraCapacity();
        if (maxExtra > 0 && currentExtra >= maxExtra) {
            player.sendMessage(plugin.getConfigCache().getMessage(
                    "upgrade_max_reached", "&cВы достигли максимального размера рюкзака!"));
            return;
        }

        double baseCost      = plugin.getConfigCache().getUpgradeBaseCost();
        double perSlot       = plugin.getConfigCache().getUpgradeCostPerSlot();
        int slotsPerLevel    = plugin.getConfigCache().getUpgradeSlotsPerLevel();
        double price = baseCost + (currentExtra * perSlot);

        if (!plugin.getEconomyManager().hasEnough(player, price)) {
            player.sendMessage(plugin.getConfigCache().getMessage("upgrade_no_money")
                    .replace("{price}", String.format("%.2f", price)));
            return;
        }

        plugin.getEconomyManager().withdraw(player, price);
        plugin.getBackpackManager().addExtraCapacity(uuid, slotsPerLevel);

        player.sendMessage(plugin.getConfigCache().getMessage("upgrade_success")
                .replace("{bonus}", String.valueOf(currentExtra + slotsPerLevel)));
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
            sender.sendMessage(Message.color("&cИгрок не найден или не в сети."));
            return;
        }
        Double mult = parseDouble(args[2]);
        Integer mins = parseInt(args[3]);
        if (mult == null || mins == null) {
            sender.sendMessage(Message.color("&cВведите корректные числа."));
            return;
        }
        plugin.getBoostManager().setLocalBoost(target.getUniqueId(), mult, mins);
        sender.sendMessage(Message.color("&aВыдан личный буст x" + mult + " игроку " + target.getName() + " на " + mins + " мин."));
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
        Double mult = parseDouble(args[1]);
        Integer mins = parseInt(args[2]);
        if (mult == null || mins == null) {
            sender.sendMessage(Message.color("&cВведите корректные числа."));
            return;
        }
        plugin.getBoostManager().setGlobalBoost(mult, mins);
        sender.sendMessage(Message.color("&aГлобальный буст x" + mult + " активирован на " + mins + " мин."));
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
        sender.sendMessage(Message.color("&aЛичный буст игрока " + target.getName() + " остановлен."));
    }

    private void handleTop(Player player) {
        player.sendMessage(plugin.getConfigCache().getMessage("top_loading"));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Object[]> entries = plugin.getDatabase().getTopEntries(10);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (entries.isEmpty()) {
                    player.sendMessage(plugin.getConfigCache().getMessage("top_empty"));
                    return;
                }
                player.sendMessage(plugin.getConfigCache().getMessage("top_header"));

                for (int i = 0; i < entries.size(); i++) {
                    UUID uuid = (UUID) entries.get(i)[0];
                    double earned = (double) entries.get(i)[1];
                    String name = Bukkit.getOfflinePlayer(uuid).getName();
                    if (name == null) name = plugin.getConfigCache().getMessage("top_unknown");

                    String medal = switch (i) {
                        case 0 -> plugin.getConfigCache().getMessage("top_medal_1");
                        case 1 -> plugin.getConfigCache().getMessage("top_medal_2");
                        case 2 -> plugin.getConfigCache().getMessage("top_medal_3");
                        default -> plugin.getConfigCache().getMessage("top_medal_other")
                                .replace("{pos}", String.valueOf(i + 1));
                    };

                    player.sendMessage(Message.color(
                            medal + " &f" + name + " &8| &f" + String.format("%.2f", earned) + "$"));
                }
            });
        });
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("mfarmer.reload")) {
            sender.sendMessage(plugin.getConfigCache().getMessage("no_permission"));
            return;
        }
        plugin.loadPluginConfig();
        sender.sendMessage(plugin.getConfigCache().getMessage("reloaded", "&aКонфигурация перезагружена!"));
    }

    private void sendHelp(Player player) {
        plugin.getConfigCache().getMessageList("help_player").forEach(player::sendMessage);
        if (player.hasPermission("mfarmer.admin")) {
            plugin.getConfigCache().getMessageList("help_admin").forEach(player::sendMessage);
        }
    }

    private Double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return null; }
    }

    private Integer parseInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return null; }
    }
}
