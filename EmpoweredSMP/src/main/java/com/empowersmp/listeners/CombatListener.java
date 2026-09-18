package com.empowersmp.listeners;

import com.empowersmp.EmpowerSMP;
import com.empowersmp.data.PlayerData;
import com.empowersmp.skills.SkillTree;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Handles Damage tree crit-chance rolls on player melee hits.
 */
public class CombatListener implements Listener {

    private final EmpowerSMP plugin;

    public CombatListener(EmpowerSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (!(damager instanceof Player player)) return;

        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        int lvl = data.getLevel(SkillTree.DAMAGE);

        double chance = 0.0;
        if (lvl >= 8) chance = 1.00;
        else if (lvl >= 7) chance = 0.75;
        else if (lvl >= 4) chance = 0.50;
        else if (lvl >= 2) chance = 0.25;
        if (chance <= 0.0) return;

        // Don't stack with a "natural" vanilla crit (falling attack); vanilla already
        // applies its own 1.5x in that case.
        boolean naturalCrit = player.getFallDistance() > 0.0f
                && !player.isOnGround()
                && !player.isSprinting();
        if (naturalCrit) return;

        if (Math.random() < chance) {
            double multiplier = plugin.getConfig().getDouble("damage.crit-damage-multiplier", 1.5);
            event.setDamage(event.getDamage() * multiplier);
            player.getWorld().spawnParticle(org.bukkit.Particle.CRIT,
                    event.getEntity().getLocation().add(0, 1, 0), 12, 0.3, 0.3, 0.3, 0.1);
        }
    }
}
