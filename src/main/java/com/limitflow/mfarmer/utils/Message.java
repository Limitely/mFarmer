package com.limitflow.mfarmer.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Message {

    public static final char COLOR_CHAR = '§';
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([a-fA-F\\d]{6})");

    public static String color(@Nullable String message) {
        if (message == null || message.isEmpty()) return message == null ? "" : message;

        final Matcher matcher = HEX_PATTERN.matcher(message);
        final StringBuilder builder = new StringBuilder(message.length() + 32);

        while (matcher.find()) {
            final String group = matcher.group(1);
            matcher.appendReplacement(builder,
                    COLOR_CHAR + "x" +
                            COLOR_CHAR + group.charAt(0) +
                            COLOR_CHAR + group.charAt(1) +
                            COLOR_CHAR + group.charAt(2) +
                            COLOR_CHAR + group.charAt(3) +
                            COLOR_CHAR + group.charAt(4) +
                            COLOR_CHAR + group.charAt(5));
        }

        message = matcher.appendTail(builder).toString();
        return translateAlternateColorCodes('&', message);
    }

    public static void sendActionBar(@NotNull Player player, @NotNull String message) {
        Component component = LegacyComponentSerializer.legacySection().deserialize(color(message));
        player.sendActionBar(component);
    }

    private static String translateAlternateColorCodes(char altColorChar, String text) {
        final char[] chars = text.toCharArray();
        for (int i = 0, len = chars.length - 1; i < len; i++) {
            if (chars[i] == altColorChar && isValidColorChar(chars[i + 1])) {
                chars[i++] = COLOR_CHAR;
                chars[i] |= 0x20;
            }
        }
        return new String(chars);
    }

    private static boolean isValidColorChar(char c) {
        return switch (c) {
            case '0','1','2','3','4','5','6','7','8','9',
                 'a','b','c','d','e','f','A','B','C','D','E','F',
                 'r','R','k','K','l','L','m','M','n','N','o','O','x','X' -> true;
            default -> false;
        };
    }
}
