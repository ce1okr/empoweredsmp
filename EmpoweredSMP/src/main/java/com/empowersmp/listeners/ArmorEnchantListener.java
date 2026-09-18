package com.empowersmp.listeners;

import com.empowersmp.EmpowerSMP;
import com.empowersmp.data.PlayerData;
import com.empowersmp.skills.SkillTree;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerArmorChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Keeps Swift Sneak III (Mobility L2) and Soul Speed III (Mobility L7) applied
 * to whatever boots a qualifying player is currently wearing. Reacts to armor
 * changes immediately, and a periodic task in EmpowerSMP catches edge cases
 * (e.g. armor equipped via dispenser) by re-scanning online players.
 */
public class ArmorEnchantListener implements Listener {

    private final EmpowerSMP plugin;

    public ArmorEnchantListener(EmpowerSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> enchantBoots(event.getPlayer()), 5L);
    }

    @EventHandler
    public void onArmorChange(PlayerArmorChangeEvent event) {
        if (event.getSlotType() == PlayerArmorChangeEvent.SlotType.FEET) {
            Bukkit.getScheduler().runTask(plugin, () -> enchantBoots(event.getPlayer()));
        }
    }

    /** Re-applied every few seconds for all online players; see EmpowerSMP#startTasks. */
    public void enchantBoots(Player player) {
        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        int mobilityLvl = data.getLevel(SkillTree.MOBILITY);

        ItemStack boots = player.getInventory().getBoots();
        if (boots == null || boots.getType().isAir()) return;

        boolean changed = false;
        ItemMeta meta = boots.getItemMeta();
        if (meta == null) return;

        if (mobilityLvl >= 2 && meta.getEnchantLevel(Enchantment.SWIFT_SNEAK) < 3) {
            meta.addEnchant(Enchantment.SWIFT_SNEAK, 3, true);
            changed = true;
        }
        if (mobilityLvl >= 7 && meta.getEnchantLevel(Enchantment.SOUL_SPEED) < 3) {
            meta.addEnchant(Enchantment.SOUL_SPEED, 3, true);
            changed = true;
        }

        if (changed) {
            boots.setItemMeta(meta);
            player.getInventory().setBoots(boots);
        }
    }
}
