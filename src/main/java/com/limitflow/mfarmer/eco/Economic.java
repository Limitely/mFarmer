package com.limitflow.mfarmer.eco;

import com.limitflow.mfarmer.MFarmer;
import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

public class Economic {

    public enum Mode { VAULT, PLAYERPOINTS }

    private final MFarmer plugin;
    private Mode mode = Mode.VAULT;

    private Economy vaultEconomy;
    private PlayerPointsAPI pointsAPI;

    public Economic(MFarmer plugin) {
        this.plugin = plugin;
        String cfgMode = plugin.getConfig().getString("currency.provider", "vault").toLowerCase();
        mode = cfgMode.equals("playerpoints") ? Mode.PLAYERPOINTS : Mode.VAULT;

        if (mode == Mode.PLAYERPOINTS) {
            setupPlayerPoints();
        } else {
            setupVault();
        }
    }

    private void setupVault() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().severe("Vault не найден! Установите Vault или переключите currency.provider на playerpoints.");
            return;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp != null) vaultEconomy = rsp.getProvider();

        if (vaultEconomy == null) {
            plugin.getLogger().severe("Vault найден, но отсутствует библиотека.");
        } else {
            plugin.getLogger().info("Экономика: Vault (" + vaultEconomy.getName() + ").");
        }
    }

    private void setupPlayerPoints() {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlayerPoints")) {
            plugin.getLogger().severe("PlayerPoints не найден! Установите PlayerPoints или переключите currency.provider на vault.");
            return;
        }
        pointsAPI = PlayerPoints.getInstance().getAPI();
        plugin.getLogger().info("Экономика: PlayerPoints.");
    }

    public boolean isEnabled() {
        return mode == Mode.VAULT ? vaultEconomy != null : pointsAPI != null;
    }

    public Mode getMode() { return mode; }

    public void deposit(@NotNull Player player, double amount) {
        if (amount <= 0) return;
        if (mode == Mode.PLAYERPOINTS && pointsAPI != null) {
            pointsAPI.give(player.getUniqueId(), (int) Math.round(amount));
        } else if (vaultEconomy != null) {
            vaultEconomy.depositPlayer(player, amount);
        }
    }

    public boolean hasEnough(@NotNull Player player, double amount) {
        if (mode == Mode.PLAYERPOINTS && pointsAPI != null) {
            return pointsAPI.look(player.getUniqueId()) >= (int) Math.ceil(amount);
        }
        return vaultEconomy != null && vaultEconomy.has(player, amount);
    }

    public void withdraw(@NotNull Player player, double amount) {
        if (amount <= 0) return;
        if (mode == Mode.PLAYERPOINTS && pointsAPI != null) {
            pointsAPI.take(player.getUniqueId(), (int) Math.ceil(amount));
        } else if (vaultEconomy != null) {
            vaultEconomy.withdrawPlayer(player, amount);
        }
    }

    public double getBalance(@NotNull Player player) {
        if (mode == Mode.PLAYERPOINTS && pointsAPI != null) {
            return pointsAPI.look(player.getUniqueId());
        }
        return vaultEconomy != null ? vaultEconomy.getBalance(player) : 0.0;
    }
}
