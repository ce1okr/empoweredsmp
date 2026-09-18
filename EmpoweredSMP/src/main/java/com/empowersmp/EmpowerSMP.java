package com.empowersmp;

import com.empowersmp.commands.EmpowerCommand;
import com.empowersmp.data.DataManager;
import com.empowersmp.listeners.ArmorEnchantListener;
import com.empowersmp.listeners.CombatListener;
import com.empowersmp.listeners.EnchantCapListener;
import com.empowersmp.listeners.PlayerConnectionListener;
import com.empowersmp.listeners.ProsperityListener;
import com.empowersmp.skills.EffectManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class EmpowerSMP extends JavaPlugin {

    private DataManager dataManager;
    private EffectManager effectManager;
    private ArmorEnchantListener armorEnchantListener;
    private EnchantCapListener enchantCapListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.dataManager = new DataManager(this);
        this.effectManager = new EffectManager(this);
        this.armorEnchantListener = new ArmorEnchantListener(this);
        this.enchantCapListener = new EnchantCapListener(this);

        getServer().getPluginManager().registerEvents(armorEnchantListener, this);
        getServer().getPluginManager().registerEvents(enchantCapListener, this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new ProsperityListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);

        EmpowerCommand cmd = new EmpowerCommand(this);
        getCommand("empower").setExecutor(cmd);
        getCommand("empower").setTabCompleter(cmd);

        startTasks();

        // Re-apply effects for anyone already online (e.g. /reload).
        Bukkit.getOnlinePlayers().forEach(p ->
                effectManager.refresh(p, dataManager.get(p.getUniqueId())));

        getLogger().info("EmpowerSMP enabled.");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveAll();
        }
        getLogger().info("EmpowerSMP disabled.");
    }

    private void startTasks() {
        long autosaveTicks = getConfig().getLong("storage.autosave-interval-minutes", 5) * 60L * 20L;
        Bukkit.getScheduler().runTaskTimer(this, () -> dataManager.saveAll(), autosaveTicks, autosaveTicks);

        // Fallback re-scan every 5s to catch boots equipped through means other than
        // the normal armor slot click (dispensers, plugins, etc).
        Bukkit.getScheduler().runTaskTimer(this,
                () -> Bukkit.getOnlinePlayers().forEach(armorEnchantListener::enchantBoots),
                100L, 100L);

        // Fallback re-scan every 15s to catch enchant-cap violations from sources
        // other than anvils/enchanting tables/clicks (commands, other plugins, etc).
        Bukkit.getScheduler().runTaskTimer(this,
                () -> Bukkit.getOnlinePlayers().forEach(enchantCapListener::periodicSweep),
                300L, 300L);
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public EffectManager getEffectManager() {
        return effectManager;
    }
}
