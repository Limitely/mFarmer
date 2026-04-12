package com.limitflow.mfarmer.menu;

import com.limitflow.mfarmer.utils.Message;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder name(String name) {
        meta.setDisplayName(Message.color(name));
        return this;
    }

    public ItemBuilder lore(List<String> lore) {
        meta.setLore(
                lore.stream()
                        .map(Message::color)
                        .collect(Collectors.toList())
        );
        return this;
    }

    public ItemBuilder lore(String... lore) {
        return lore(Arrays.asList(lore));
    }

    public ItemBuilder playerHead(Player player) {
        if (meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(player);
        }
        return this;
    }

    public ItemBuilder customHead(String textureValue) {
        if (!(meta instanceof SkullMeta skullMeta)) return this;

        try {
            String base64;

            if (textureValue.startsWith("http://") || textureValue.startsWith("https://")) {
                String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + textureValue + "\"}}}";
                base64 = Base64.getEncoder().encodeToString(json.getBytes());
            } else {
                base64 = textureValue;
            }

            GameProfile profile = new GameProfile(UUID.randomUUID(), null);
            profile.getProperties().put("textures", new Property("textures", base64));

            Field profileField = skullMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(skullMeta, profile);

        } catch (Exception e) {
            java.util.logging.Logger.getLogger("mFarmer").warning("Неудачно применилась текстура " + e.getMessage());
        }

        return this;
    }

    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
}
