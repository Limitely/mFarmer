package com.limitflow.mfarmer.config;

import org.bukkit.Material;

public record CropConfig(Material material, double priceMultiplier, String displayName) {
}
