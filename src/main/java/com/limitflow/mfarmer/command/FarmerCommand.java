package com.limitflow.mfarmer.command;

import com.limitflow.mfarmer.MFarmer;
import com.limitflow.mfarmer.database.Database;
import com.limitflow.mfarmer.model.GroupData;
import com.limitflow.mfarmer.utils.Message;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FarmerCommand implements CommandExecutor, TabCompleter {

    private final MFarmer plugin;

    private static final double BOOST_MULT_MIN = 0.1;
    private static final double BOOST_MULT_MAX = 100.0;
    private static final int BOOST_MINS_MIN = 1;
    private static final int BOOST_MINS_MAX = 1440;

    private final Map<UUID, Long> upgradeCooldown = new ConcurrentHashMap<>();

    public FarmerCommand(MFarmer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command cmd,
                             @NotNull String label,
                             @NotNull String[] args) {

        if (args.length == 0) {
            if (sender instanceof Player player) plugin.getMenu().openMenu(player);
            else sender.sendMessage("Only players.");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "menu" -> {
                if (sender instanceof Player p) {
                    String id = args.length >= 2 ? args[1] : plugin.getMenu().getDefaultMenuId();
                    plugin.getMenu().openMenu(p, id);
                }
            }
            case "help" -> {
                if (sender instanceof Player p) sendHelp(p);
                else plugin.getConfigCache().getMessageList("help_admin").forEach(sender::sendMessage);
            }
            case "salary"     -> { if (sender instanceof Player p) handleSalary(p); }
            case "upgrade"    -> { if (sender instanceof Player p) handleUpgrade(p); }
            case "top"        -> { if (sender instanceof Player p) handleTop(p); }
            case "giveboost"  -> handleGiveBoost(sender, args);
            case "adminboost" -> handleAdminBoost(sender, args);
            case "stopboost"  -> handleStopBoost(sender, args);
            case "event"      -> handleEvent(sender, args);
            case "reload"     -> handleReload(sender);
            default -> sender.sendMessage(plugin.getConfigCache().getMessage("unknown_command"));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      @NotNull String[] args) {
        if (args.length == 1) {
            List<String> base = new ArrayList<>(List.of(
                    "help", "salary", "upgrade", "top", "menu", "reload"
            ));
            return filter(base, args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sender.hasPermission("mfarmer.admin") &&
                    (sub.equals("giveboost") || sub.equals("stopboost"))) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (sub.equals("event") && sender.hasPermission("mfarmer.admin")) {
                return filter(List.of("start", "stop"), args[1]);
            }
        }
        if (args.length == 3 && args[0].toLowerCase().contains("boost")) {
            return List.of("1.5", "2.0", "5.0", "10.0");
        }
        if (args.length == 4 && args[0].toLowerCase().contains("boost")) {
            return List.of("5", "10", "30", "60");
        }
        return List.of();
    }

    private List<String> filter(List<String> list, String prefix) {
        return list.stream().filter(s -> s.startsWith(prefix.toLowerCase())).toList();
    }

    private void handleSalary(Player player) {
        UUID uuid = player.getUniqueId();
        int amount = plugin.getBackpackManager().getAmount(uuid);

        if (amount <= 0) {
            player.sendMessage(plugin.getConfigCache().getMessage("salary_fail"));
            return;
        }

        GroupData group = plugin.getGroupManager().getGroup(player);
        if (group == null) return;

        double boostMultiplier = plugin.getBoostManager().getMultiplier(uuid)
                * plugin.getFarmEvent().getMultiplier();

        double total = 0;
        for (var entry : plugin.getBackpackManager().getCropAmounts(uuid).entrySet()) {
            var cropConfig = plugin.getConfigCache().getCropConfig(entry.getKey());
            double cropMult = cropConfig != null ? cropConfig.priceMultiplier() : 1.0;
            total += group.price() * entry.getValue() * cropMult * boostMultiplier;
        }

        if (!Double.isFinite(total) || total < 0) total = 0;

        plugin.getEconomyManager().deposit(player, total);
        plugin.getDatabase().addEarned(uuid, total);
        plugin.getBackpackManager().clear(uuid);

        String formatted = plugin.getConfigCache().formatMoney(total)
                + " " + plugin.getConfigCache().getCurrencySymbol();

        player.sendMessage(plugin.getConfigCache().getMessage("salary_success")
                .replace("{amount}", String.valueOf(amount))
                .replace("{money}", formatted));
    }

    private void handleUpgrade(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        Long last = upgradeCooldown.get(uuid);
        if (last != null && now - last < 500) return;
        upgradeCooldown.put(uuid, now);

        int current = plugin.getBackpackManager().getExtraCapacity(uuid);
        int max = plugin.getConfigCache().getMaxExtraCapacity();

        if (max > 0 && current >= max) {
            player.sendMessage(plugin.getConfigCache().getMessage("upgrade_max_reached"));
            return;
        }

        double price = plugin.getConfigCache().getUpgradeBaseCost()
                + (current * plugin.getConfigCache().getUpgradeCostPerSlot());

        if (!plugin.getEconomyManager().hasEnough(player, price)) {
            String formatted = plugin.getConfigCache().formatMoney(price)
                    + " " + plugin.getConfigCache().getCurrencySymbol();
            player.sendMessage(plugin.getConfigCache().getMessage("upgrade_no_money")
                    .replace("{price}", formatted));
            return;
        }

        plugin.getEconomyManager().withdraw(player, price);
        plugin.getBackpackManager().addExtraCapacity(uuid, plugin.getConfigCache().getUpgradeSlotsPerLevel());

        int newExtra = plugin.getBackpackManager().getExtraCapacity(uuid);
        player.sendMessage(plugin.getConfigCache().getMessage("upgrade_success")
                .replace("{bonus}", String.valueOf(newExtra)));
    }

    private void handleTop(Player player) {
        player.sendMessage(plugin.getConfigCache().getMessage("top_loading"));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Database.TopEntry> entries = plugin.getDatabase().getTopEntries(10);
            List<String> lines = new ArrayList<>();
            String currency = plugin.getConfigCache().getCurrencySymbol();

            for (int i = 0; i < entries.size(); i++) {
                Database.TopEntry entry = entries.get(i);
                String name = Bukkit.getOfflinePlayer(entry.uuid()).getName();
                if (name == null) name = plugin.getConfigCache().getMessage("top_unknown", "Unknown");

                String medal = switch (i) {
                    case 0 -> plugin.getConfigCache().getMessage("top_medal_1", "&6#1");
                    case 1 -> plugin.getConfigCache().getMessage("top_medal_2", "&7#2");
                    case 2 -> plugin.getConfigCache().getMessage("top_medal_3", "&e#3");
                    default -> plugin.getConfigCache()
                            .getMessage("top_medal_other", "&7#{pos}")
                            .replace("{pos}", String.valueOf(i + 1));
                };

                String earned = plugin.getConfigCache().formatMoney(entry.earned()) + " " + currency;
                lines.add(Message.color(medal + " &f" + name + " &8| &a" + earned));
            }

            final List<String> finalLines = lines;
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(plugin.getConfigCache().getMessage("top_header"));
                if (finalLines.isEmpty()) {
                    player.sendMessage(plugin.getConfigCache().getMessage("top_empty", "&cПока нет данных."));
                } else {
                    finalLines.forEach(player::sendMessage);
                }
            });
        });
    }

    private void handleEvent(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mfarmer.admin")) {
            sender.sendMessage(plugin.getConfigCache().getMessage("no_permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§cИспользование: /mfarmer event <start|stop>");
            return;
        }
        String starterName = sender instanceof Player p ? p.getName() : "Консоль";
        switch (args[1].toLowerCase()) {
            case "start" -> plugin.getFarmEvent().start(starterName);
            case "stop"  -> plugin.getFarmEvent().stop();
            default      -> sender.sendMessage("§cИспользование: /mfarmer event <start|stop>");
        }
    }


    private void handleGiveBoost(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mfarmer.admin")) {
            sender.sendMessage(plugin.getConfigCache().getMessage("no_permission"));
            return;
        }
        if (args.length < 4) {
            sender.sendMessage("§cИспользование: /mfarmer giveboost <игрок> <множитель> <минуты>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { sender.sendMessage("§cИгрок не найден."); return; }
        double mult; int mins;
        try {
            mult = Double.parseDouble(args[2]);
            mins = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) { sender.sendMessage("§cНекорректные числа."); return; }
        mult = Math.max(BOOST_MULT_MIN, Math.min(BOOST_MULT_MAX, mult));
        mins = Math.max(BOOST_MINS_MIN, Math.min(BOOST_MINS_MAX, mins));
        plugin.getBoostManager().setLocalBoost(target.getUniqueId(), mult, mins);
    }

    private void handleAdminBoost(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mfarmer.admin")) {
            sender.sendMessage(plugin.getConfigCache().getMessage("no_permission"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("§cИспользование: /mfarmer adminboost <множитель> <минуты>");
            return;
        }
        double mult; int mins;
        try {
            mult = Double.parseDouble(args[1]);
            mins = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) { sender.sendMessage("§cНекорректные числа."); return; }
        mult = Math.max(BOOST_MULT_MIN, Math.min(BOOST_MULT_MAX, mult));
        mins = Math.max(BOOST_MINS_MIN, Math.min(BOOST_MINS_MAX, mins));
        plugin.getBoostManager().setGlobalBoost(mult, mins);
    }

    private void handleStopBoost(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mfarmer.admin")) {
            sender.sendMessage(plugin.getConfigCache().getMessage("no_permission"));
            return;
        }
        if (args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) { sender.sendMessage("§cИгрок не найден."); return; }
            plugin.getBoostManager().stopLocalBoost(target.getUniqueId());
            sender.sendMessage("§aЛичный буст для " + target.getName() + " остановлен.");
        } else {
            plugin.getBoostManager().stopGlobalBoost();
            sender.sendMessage("§aГлобальный буст остановлен.");
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("mfarmer.reload")) {
            sender.sendMessage(plugin.getConfigCache().getMessage("no_permission"));
            return;
        }
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
