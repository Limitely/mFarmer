package com.limitflow.mfarmer.listener;

import com.limitflow.mfarmer.MFarmer;
import com.limitflow.mfarmer.config.CropConfig;
import com.limitflow.mfarmer.model.GroupData;
import com.limitflow.mfarmer.utils.Message;
import com.limitflow.mfarmer.utils.Region;
import org.bukkit.Bukkit;
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
    private final Map<Block, BukkitTask> restoreTasks = new ConcurrentHashMap<>();

    public BreakBlock(MFarmer plugin) {
        this.plugin = plugin;
    }

    public void cleanup() {
        restoreTasks.values().forEach(BukkitTask::cancel);
        restoreTasks.clear();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getBackpackManager().preloadPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getBackpackManager().removeData(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        CropConfig cropConfig = plugin.getConfigCache().getCropConfig(block.getType());
        if (cropConfig == null) return;

        if (!(block.getBlockData() instanceof Ageable ageable)
                || ageable.getAge() < ageable.getMaximumAge()) return;

        Player player = event.getPlayer();
        if (!Region.isInRegion(player, plugin.getConfigCache().getRegions())) return;

        event.setCancelled(true);

        GroupData group = plugin.getGroupManager().getGroup(player);
        int baseCapacity = (group != null) ? group.capacity() : 64;
        UUID uuid = player.getUniqueId();
        int totalCapacity = baseCapacity + plugin.getBackpackManager().getExtraCapacity(uuid);

        if (plugin.getBackpackManager().isFull(uuid, totalCapacity)) {
            player.sendMessage(plugin.getConfigCache().getMessage("backpack_full"));
            return;
        }

        plugin.getBackpackManager().addItem(uuid);

        int amount = plugin.getBackpackManager().getAmount(uuid);
        Message.sendActionBar(player, plugin.getConfigCache().getMessage("collect")
                .replace("{amount}", String.valueOf(amount))
                .replace("{capacity}", String.valueOf(totalCapacity))
                .replace("{crop}", cropConfig.displayName()));

        playCollectSound(player);
        handleBlockRestore(block, plugin.getConfigCache().getRestoreDelay());
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

    private void handleBlockRestore(Block block, long delay) {
        BukkitTask existing = restoreTasks.remove(block);
        if (existing != null) existing.cancel();

        Ageable ageable = (Ageable) block.getBlockData();
        ageable.setAge(0);
        block.setBlockData(ageable);

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (block.getBlockData() instanceof Ageable grown) {
                grown.setAge(grown.getMaximumAge());
                block.setBlockData(grown);
            }
            restoreTasks.remove(block);
        }, Math.max(1, delay) * 20L);

        restoreTasks.put(block, task);
    }
}
