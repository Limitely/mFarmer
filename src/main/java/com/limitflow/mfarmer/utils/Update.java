package com.limitflow.mfarmer.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.limitflow.mfarmer.MFarmer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Update {

    private static final String REPO = "Limitely/mFarmer";
    private static final String API_URL = "https://api.github.com/repos/" + REPO + "/releases/latest";
    private static final String PREFIX = ChatColor.GRAY + "[" + ChatColor.WHITE + "mFarmer" + ChatColor.GRAY + "] " + ChatColor.RESET;

    private final MFarmer plugin;

    public Update(MFarmer plugin) {
        this.plugin = plugin;
    }

    public void check() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(API_URL).openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "mFarmer-UpdateChecker");
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                if (connection.getResponseCode() != 200) {
                    logError("Не удалось связаться с GitHub. Код ответа: " + connection.getResponseCode());
                    return;
                }

                try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
                    JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
                    String latestVersion = json.get("tag_name").getAsString();
                    String downloadUrl = json.get("html_url").getAsString();
                    String currentVersion = plugin.getDescription().getVersion();

                    if (compareVersions(currentVersion, latestVersion) < 0) {
                        printUpdateMessage(currentVersion, latestVersion, downloadUrl);
                    } else {
                        logInfo("Плагин актуален! Текущая версия: " + ChatColor.GREEN + currentVersion);
                    }
                }
            } catch (Exception e) {
                logError("Ошибка при проверке обновлений: " + e.getMessage());
            }
        });
    }

    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.replaceAll("[^0-9.]", "").split("\\.");
        String[] parts2 = v2.replaceAll("[^0-9.]", "").split("\\.");
        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int p1 = i < parts1.length && !parts1[i].isEmpty() ? Integer.parseInt(parts1[i]) : 0;
            int p2 = i < parts2.length && !parts2[i].isEmpty() ? Integer.parseInt(parts2[i]) : 0;
            if (p1 != p2) return Integer.compare(p1, p2);
        }
        return 0;
    }

    private void printUpdateMessage(String current, String latest, String url) {
        Bukkit.getConsoleSender().sendMessage(PREFIX);
        logWarning("Доступна новая версия плагина!");
        logWarning("Текущая: " + ChatColor.GRAY + current + ChatColor.YELLOW + "  →  Новая: " + ChatColor.GREEN + latest);
        logWarning("Скачать: " + ChatColor.AQUA + url);
        Bukkit.getConsoleSender().sendMessage(PREFIX);
    }

    private void logInfo(String msg) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + ChatColor.GREEN + msg);
    }

    private void logWarning(String msg) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + ChatColor.YELLOW + msg);
    }

    private void logError(String msg) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + ChatColor.RED + msg);
    }
}
