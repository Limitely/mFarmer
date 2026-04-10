package com.limitflow.mfarmer.utils;

import com.limitflow.mfarmer.MFarmer;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Update {

    private static final String REPO = "Limitely/mFarmer";
    private static final String API_URL = "https://api.github.com/repos/" + REPO + "/releases/latest";
    private static final String PREFIX = "\u001b[37m[\u001b[90mmFarmer\u001b[37m]\u001b[0m ";

    private static final ConsoleCommandSender console = Bukkit.getConsoleSender();
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
                    logError("апи успешно вернул код " + connection.getResponseCode() + ". Проверку пропустим.");
                    return;
                }

                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                }

                String body = response.toString();
                String latestVersion = extractJsonField(body, "tag_name");
                String downloadUrl   = extractJsonField(body, "html_url");

                if (latestVersion == null) {
                    logError("Неудачно прочитана версия.");
                    return;
                }

                String currentVersion = plugin.getDescription().getVersion();

                if (compareVersions(currentVersion, latestVersion) < 0) {
                    console.sendMessage(PREFIX);
                    logWarning("Доступна новая версия плагина!");
                    logWarning("Текущая: \u001b[90m" + currentVersion
                            + "\u001b[33m  →  Новая: \u001b[32m" + latestVersion + "\u001b[0m");
                    logWarning("Скачать:");
                    console.sendMessage(PREFIX + "\u001b[36m" + downloadUrl + "\u001b[0m");
                    console.sendMessage(PREFIX);
                } else {
                    logInfo("Плагин актуален! Молодец что следишь. Версия: \u001b[32m" + currentVersion + "\u001b[0m");
                }

            } catch (IOException e) {
                logError("Ошибка с апи: " + e.getMessage());
            } catch (Exception e) {
                logError("Ошибка при проверки обновления: " + e.getMessage());
            }
        });
    }

    private static String extractJsonField(String json, String field) {
        String key = "\"" + field + "\"";
        int keyIndex = json.indexOf(key);
        if (keyIndex == -1) return null;

        int colon = json.indexOf(":", keyIndex + key.length());
        if (colon == -1) return null;

        int valueStart = json.indexOf("\"", colon + 1);
        if (valueStart == -1) return null;

        int valueEnd = json.indexOf("\"", valueStart + 1);
        if (valueEnd == -1) return null;

        return json.substring(valueStart + 1, valueEnd);
    }

    private static int compareVersions(String v1, String v2) {
        v1 = v1.replaceAll("^[vV]", "");
        v2 = v2.replaceAll("^[vV]", "");

        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int length = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < length; i++) {
            int p1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int p2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (p1 != p2) return Integer.compare(p1, p2);
        }
        return 0;
    }

    private static void logInfo(String message) {
        console.sendMessage(PREFIX + "\u001b[32m" + message + "\u001b[0m");
    }

    private static void logWarning(String message) {
        console.sendMessage(PREFIX + "\u001b[33m" + message + "\u001b[0m");
    }

    private static void logError(String message) {
        console.sendMessage(PREFIX + "\u001b[31m" + message + "\u001b[0m");
    }
}