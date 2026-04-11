package com.limitflow.mfarmer.menu;

import com.limitflow.mfarmer.utils.Message;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
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

    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
}