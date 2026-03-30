package com.limitflow.mfarmer.eco;

import com.limitflow.mfarmer.MFarmer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

public class Economic {

    private final MFarmer plugin;
    private Economy economy;

    public Economic(MFarmer plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().severe("Vault не найден!");
            return;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            economy = rsp.getProvider();
        }

        if (economy == null) {
            plugin.getLogger().severe("Не удалось подключиться к экономике через Vault");
        } else {
            plugin.getLogger().info("Успешно подключено к Vault.");
        }
    }

    public boolean isEnabled() {
        return economy != null;
    }

    public void deposit(@NotNull Player player, double amount) {
        if (economy != null && amount > 0) {
            economy.depositPlayer(player, amount);
        }
    }

    public boolean hasEnough(@NotNull Player player, double amount) {
        return economy != null && economy.has(player, amount);
    }

    public void withdraw(@NotNull Player player, double amount) {
        if (economy != null && amount > 0) {
            economy.withdrawPlayer(player, amount);
        }
    }

    public double getBalance(@NotNull Player player) {
        return economy != null ? economy.getBalance(player) : 0.0;
    }
}
