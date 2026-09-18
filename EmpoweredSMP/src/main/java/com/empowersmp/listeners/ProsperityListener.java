package com.empowersmp.listeners;

import com.empowersmp.EmpowerSMP;
import com.empowersmp.data.PlayerData;
import com.empowersmp.skills.SkillTree;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.List;
import java.util.Set;

/**
 * Prosperity tree: bonus ore drops, XP multiplier, potion-duration multiplier,
 * and doubled villager trade output.
 */
public class ProsperityListener implements Listener {

    private static final Set<Material> ORES = Set.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE, Material.NETHER_GOLD_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.NETHER_QUARTZ_ORE, Material.ANCIENT_DEBRIS
    );

    private final EmpowerSMP plugin;

    public ProsperityListener(EmpowerSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockDrop(BlockDropItemEvent event) {
        if (!ORES.contains(event.getBlockState().getType())) return;
        Player player = event.getPlayer();
        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        int lvl = data.getLevel(SkillTree.PROSPERITY);
        if (lvl < 2) return;

        double bonus = 0.0;
        if (lvl >= 2) bonus = plugin.getConfig().getDouble("prosperity.ore-drop-bonus-level2", 0.50);
        if (lvl >= 7) bonus = plugin.getConfig().getDouble("prosperity.ore-drop-bonus-level7", 1.00);

        List<Item> items = event.getItems();
        for (Item item : items) {
            ItemStack stack = item.getItemStack();
            double extra = stack.getAmount() * bonus;
            int wholeExtra = (int) Math.floor(extra);
            double fractional = extra - wholeExtra;
            if (Math.random() < fractional) wholeExtra++;
            if (wholeExtra > 0) {
                stack.setAmount(Math.min(stack.getMaxStackSize(), stack.getAmount() + wholeExtra));
                item.setItemStack(stack);
            }
        }
    }

    @EventHandler
    public void onExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        int lvl = data.getLevel(SkillTree.PROSPERITY);

        double multiplier = 1.0;
        if (lvl >= 3) multiplier = plugin.getConfig().getDouble("prosperity.xp-multiplier-level3", 2.0);
        if (lvl >= 8) multiplier = plugin.getConfig().getDouble("prosperity.xp-multiplier-level8", 3.0);
        if (multiplier > 1.0) {
            event.setAmount((int) Math.round(event.getAmount() * multiplier));
        }
    }

    @EventHandler
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (event.getAction() != EntityPotionEffectEvent.Action.ADDED) return;
        if (event.getCause() != EntityPotionEffectEvent.Cause.POTION_DRINK) return;
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player)) return;

        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        if (data.getLevel(SkillTree.PROSPERITY) < 4) return;

        PotionEffect newEffect = event.getNewEffect();
        if (newEffect == null) return;
        double multiplier = plugin.getConfig().getDouble("prosperity.potion-multiplier-level4", 1.5);
        int newDuration = (int) Math.round(newEffect.getDuration() * multiplier);
        event.setNewEffect(new PotionEffect(
                newEffect.getType(), newDuration, newEffect.getAmplifier(),
                newEffect.isAmbient(), newEffect.hasParticles(), newEffect.hasIcon()));
    }

    @EventHandler
    public void onMerchantClick(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof MerchantInventory merchant)) return;
        if (event.getRawSlot() != 2) return; // the trade result slot
        if (!(event.getWhoClicked() instanceof Player player)) return;

        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        if (data.getLevel(SkillTree.PROSPERITY) < 6) return;

        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType().isAir()) return;
        double multiplier = plugin.getConfig().getDouble("prosperity.villager-trade-multiplier-level6", 2.0);
        int doubled = (int) Math.round(result.getAmount() * multiplier);
        result.setAmount(Math.min(result.getMaxStackSize(), doubled));
        event.setCurrentItem(result);
    }
}
