package com.limitflow.mfarmer.listener;

import com.limitflow.mfarmer.MFarmer;
import com.limitflow.mfarmer.config.CropConfig;
import com.limitflow.mfarmer.utils.Message;
import com.limitflow.mfarmer.utils.Region;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BreakBlock implements Listener {

    private final MFarmer plugin;
    private record BlockKey(String world, int x, int y, int z) {}
    private final Map<BlockKey, BukkitTask> restoreTasks = new ConcurrentHashMap<>();

    public BreakBlock(MFarmer plugin) {
        this.plugin = plugin;
    }

    public void cleanup() {
        restoreTasks.values().forEach(t -> { if (!t.isCancelled()) t.cancel(); });
        restoreTasks.clear();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        plugin.getBackpackManager().preloadPlayer(uuid);
        plugin.getBoostManager().loadPlayerBoost(uuid);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        plugin.getBackpackManager().removeData(uuid);
        plugin.getBoostManager().unloadPlayerBoost(uuid);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        CropConfig cropConfig = plugin.getConfigCache().getCropConfig(block.getType());
        if (cropConfig == null) return;
        if (!(block.getBlockData() instanceof Ageable ageable)) return;
        if (ageable.getAge() < ageable.getMaximumAge()) return;

        Player player = event.getPlayer();
        if (!Region.isInRegion(player, plugin.getConfigCache().getRegions())) return;

        event.setCancelled(true);

        UUID uuid = player.getUniqueId();

        if (plugin.getBackpackManager().isFull(uuid)) {
            player.sendMessage(plugin.getConfigCache().getMessage("backpack_full"));
            return;
        }

        plugin.getBackpackManager().addItem(uuid, block.getType());
        plugin.getFarmEvent().recordHarvest(uuid, 1);

        int amount = plugin.getBackpackManager().getAmount(uuid);
        int totalCapacity = plugin.getBackpackManager().getTotalCapacity(uuid);

        String actionBar = plugin.getConfigCache().getMessage("collect")
                .replace("{amount}", String.valueOf(amount))
                .replace("{capacity}", String.valueOf(totalCapacity))
                .replace("{crop}", cropConfig.displayName());

        if (plugin.getFarmEvent().isActive()) {
            actionBar += plugin.getConfigCache().getMessage("event_collect_suffix",
                    " &d[x" + plugin.getFarmEvent().getMultiplier() + " ИВЕНТ]")
                    .replace("{multiplier}", String.valueOf(plugin.getFarmEvent().getMultiplier()));
        }

        Message.sendActionBar(player, actionBar);
        playCollectSound(player);
        handleBlockRestore(block);
    }

    private void playCollectSound(Player player) {
        try {
            Sound sound = Sound.valueOf(plugin.getConfigCache().getSoundType().toUpperCase());
            player.playSound(player.getLocation(), sound,
                    plugin.getConfigCache().getSoundVolume(),
                    plugin.getConfigCache().getSoundPitch());
        } catch (IllegalArgumentException e) {
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
        }
    }

    private BlockKey toKey(Block block) {
        Location l = block.getLocation();
        return new BlockKey(l.getWorld().getName(), l.getBlockX(), l.getBlockY(), l.getBlockZ());
    }

    private void handleBlockRestore(Block block) {
        BlockKey key = toKey(block);
        BukkitTask existing = restoreTasks.remove(key);
        if (existing != null && !existing.isCancelled()) existing.cancel();

        if (!(block.getBlockData() instanceof Ageable ageable)) return;
        var material = block.getType();
        long delay = plugin.getConfigCache().getRestoreDelay();

        ageable.setAge(0);
        block.setBlockData(ageable);

        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            restoreTasks.remove(key);
            if (block.getType() != material) return;
            if (!(block.getBlockData() instanceof Ageable current)) return;
            current.setAge(current.getMaximumAge());
            block.setBlockData(current);
        }, Math.max(1, delay) * 20L);

        restoreTasks.put(key, task);
    }
}
