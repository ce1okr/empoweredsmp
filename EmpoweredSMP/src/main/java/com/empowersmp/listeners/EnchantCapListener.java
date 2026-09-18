package com.empowersmp.listeners;

import com.empowersmp.EmpowerSMP;
import com.empowersmp.items.CustomItems;
import com.empowersmp.skills.EnchantCapRules;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

/**
 * Replaces a third-party enchant-limiter plugin. Caps Sharpness/Smite/Bane of
 * Arthropods and the four Protection enchants at server-wide bases, letting
 * players with enough Damage/Vitality skill exceed them (see
 * EnchantCapRules). Enforced at every point an over-cap enchant could enter
 * a player's possession: anvils, enchanting tables, item pickups, inventory
 * clicks (covers villager trades and moving items out of chests), and a
 * periodic full-inventory sweep to catch anything else (commands, other
 * plugins, etc).
 */
public class EnchantCapListener implements Listener {

    private final EmpowerSMP plugin;
    private final EnchantCapRules rules;

    public EnchantCapListener(EmpowerSMP plugin) {
        this.plugin = plugin;
        this.rules = new EnchantCapRules(plugin);
    }

    // ---------------------------------------------------------------
    // Anvil combining
    // ---------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inv = event.getInventory();
        ItemStack left = inv.getItem(0);
        ItemStack right = inv.getItem(1);
        if (left == null || right == null) return;

        HumanEntity viewer = inv.getViewer();
        if (!(viewer instanceof Player player)) return;

        ItemStack result = event.getResult();
        boolean resultVoided = (result == null || result.getType().isAir());
        ItemStack base = resultVoided ? left.clone() : result;
        ItemMeta meta = base.getItemMeta();
        if (meta == null) return;

        boolean changed = false;
        for (Enchantment ench : allRelevantEnchants()) {
            int combined = combinedLevel(left, right, ench);
            if (combined <= 0) continue;
            int allowed = rules.clamp(player, ench, combined);
            int vanillaMax = ench.getMaxLevel();
            int target = resultVoided
                    ? allowed                              // vanilla voided it (too expensive) - rebuild at our clamp
                    : Math.min(allowed, meta.getEnchantLevel(ench) == 0 ? allowed : meta.getEnchantLevel(ench));

            if (allowed > vanillaMax || resultVoided || meta.getEnchantLevel(ench) != allowed) {
                if (target <= 0) continue;
                meta.addEnchant(ench, target, true);
                changed = true;
            }
        }

        if (changed) {
            base.setItemMeta(meta);
            event.setResult(base);
        }
    }

    // ---------------------------------------------------------------
    // Enchanting table
    // ---------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH)
    public void onEnchantItem(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        Map<Enchantment, Integer> toAdd = event.getEnchantsToAdd();
        Map<Enchantment, Integer> clamped = new HashMap<>();
        for (Map.Entry<Enchantment, Integer> e : toAdd.entrySet()) {
            clamped.put(e.getKey(), rules.clamp(player, e.getKey(), e.getValue()));
        }
        toAdd.clear();
        toAdd.putAll(clamped);
    }

    // ---------------------------------------------------------------
    // Anything entering a player's inventory: pickups, trades, chest moves
    // ---------------------------------------------------------------
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPickup(EntityPickupItemEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) return;
        Bukkit.getScheduler().runTask(plugin, () -> clampHeldAndInventory(player));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Bukkit.getScheduler().runTask(plugin, () -> clampHeldAndInventory(player));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> clampHeldAndInventory(event.getPlayer()), 5L);
    }

    /** Fallback sweep; see EmpowerSMP#startTasks. */
    public void periodicSweep(Player player) {
        clampHeldAndInventory(player);
    }

    private void clampHeldAndInventory(Player player) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            clampItem(player, inv.getItem(i));
        }
        clampItem(player, inv.getItemInOffHand());
    }

    private void clampItem(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        if (CustomItems.isEmpoweredSpear(item)) return; // exempt the Mobility L8 reward
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        boolean changed = false;

        if (meta instanceof EnchantmentStorageMeta bookMeta) {
            for (Enchantment ench : bookMeta.getStoredEnchants().keySet().toArray(new Enchantment[0])) {
                int current = bookMeta.getStoredEnchantLevel(ench);
                int allowed = rules.clamp(player, ench, current);
                if (allowed < current) {
                    bookMeta.removeStoredEnchant(ench);
                    if (allowed > 0) bookMeta.addStoredEnchant(ench, allowed, true);
                    changed = true;
                }
            }
        } else {
            for (Enchantment ench : meta.getEnchants().keySet().toArray(new Enchantment[0])) {
                int current = meta.getEnchantLevel(ench);
                int allowed = rules.clamp(player, ench, current);
                if (allowed < current) {
                    meta.removeEnchant(ench);
                    if (allowed > 0) meta.addEnchant(ench, allowed, true);
                    changed = true;
                }
            }
        }

        if (changed) {
            item.setItemMeta(meta);
        }
    }

    private int combinedLevel(ItemStack left, ItemStack right, Enchantment ench) {
        int a = left.getEnchantmentLevel(ench);
        int b = enchantLevelOf(right, ench);
        if (a == 0 && b == 0) return 0;
        if (a == b) return a + 1;
        return Math.max(a, b);
    }

    private int enchantLevelOf(ItemStack item, Enchantment ench) {
        if (item.getItemMeta() instanceof EnchantmentStorageMeta esm) {
            return esm.getStoredEnchantLevel(ench);
        }
        return item.getEnchantmentLevel(ench);
    }

    private Iterable<Enchantment> allRelevantEnchants() {
        return java.util.List.of(
                Enchantment.SHARPNESS, Enchantment.SMITE, Enchantment.BANE_OF_ARTHROPODS,
                Enchantment.SWEEPING_EDGE,
                Enchantment.PROTECTION, Enchantment.BLAST_PROTECTION,
                Enchantment.PROJECTILE_PROTECTION, Enchantment.FIRE_PROTECTION);
    }
}
