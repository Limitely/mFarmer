package com.limitflow.mfarmer.utils;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Message {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([a-fA-F0-9]{6})");

    public static String color(@Nullable String text) {
        if (text == null) return "";

        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(sb, replacement.toString());
        }
        matcher.appendTail(sb);

        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    public static void sendActionBar(@NotNull Player player, @NotNull String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, player.getUniqueId(),
                TextComponent.fromLegacyText(color(message)));
    }
}
