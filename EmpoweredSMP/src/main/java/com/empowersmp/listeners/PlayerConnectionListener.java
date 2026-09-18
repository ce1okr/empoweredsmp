package com.empowersmp.listeners;

import com.empowersmp.EmpowerSMP;
import com.empowersmp.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Loads data and (re)applies effects on join/respawn, and persists + frees the
 * cache on quit.
 */
public class PlayerConnectionListener implements Listener {

    private final EmpowerSMP plugin;

    public PlayerConnectionListener(EmpowerSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlayerData data = plugin.getDataManager().get(event.getPlayer().getUniqueId());
        // Delay one tick so attributes/inventory are fully initialized.
        Bukkit.getScheduler().runTask(plugin, () ->
                plugin.getEffectManager().refresh(event.getPlayer(), data));
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        PlayerData data = plugin.getDataManager().get(event.getPlayer().getUniqueId());
        Bukkit.getScheduler().runTask(plugin, () ->
                plugin.getEffectManager().refresh(event.getPlayer(), data));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getDataManager().unload(event.getPlayer().getUniqueId());
    }
}
