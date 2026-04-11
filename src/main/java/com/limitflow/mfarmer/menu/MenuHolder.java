package com.limitflow.mfarmer.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class MenuHolder implements InventoryHolder {

    private final String id;
    private Inventory inventory;

    public MenuHolder(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}