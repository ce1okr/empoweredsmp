package com.empowersmp.skills;

import com.empowersmp.EmpowerSMP;
import com.empowersmp.data.PlayerData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

import java.util.Set;

/**
 * Resolves the max level a given enchantment may reach for a given player,
 * based on server-wide base caps and the player's Damage/Vitality tree
 * levels. This replaces a third-party enchant-limiter plugin entirely so
 * there's a single source of truth and no event-priority fighting.
 */
public class EnchantCapRules {

    private static final Set<Enchantment> SHARPNESS_FAMILY = Set.of(
            Enchantment.SHARPNESS, Enchantment.SMITE, Enchantment.BANE_OF_ARTHROPODS);

    private static final Set<Enchantment> PROTECTION_FAMILY = Set.of(
            Enchantment.PROTECTION, Enchantment.BLAST_PROTECTION,
            Enchantment.PROJECTILE_PROTECTION, Enchantment.FIRE_PROTECTION);

    private final EmpowerSMP plugin;

    public EnchantCapRules(EmpowerSMP plugin) {
        this.plugin = plugin;
    }

    /** Max level `enchantment` may reach for `player`, or -1 for "not capped here" (use vanilla max). */
    public int capFor(Player player, Enchantment enchantment) {
        PlayerData data = plugin.getDataManager().get(player.getUniqueId());

        if (enchantment.equals(Enchantment.SWEEPING_EDGE)) {
            int damageLvl = data.getLevel(SkillTree.DAMAGE);
            int unlockLevel = plugin.getConfig().getInt("enchant-caps.sweeping-edge.unlock-level", 6);
            if (damageLvl >= unlockLevel) {
                return plugin.getConfig().getInt("enchant-caps.sweeping-edge.elevated-max", 5);
            }
            return -1; // vanilla default cap (3) applies
        }

        if (SHARPNESS_FAMILY.contains(enchantment)) {
            int damageLvl = data.getLevel(SkillTree.DAMAGE);
            int unlockLevel = plugin.getConfig().getInt("enchant-caps.sharpness-family.unlock-level", 3);
            if (damageLvl >= unlockLevel) {
                return plugin.getConfig().getInt("enchant-caps.sharpness-family.elevated-max", 6);
            }
            return plugin.getConfig().getInt("enchant-caps.sharpness-family.base-max", 4);
        }

        if (PROTECTION_FAMILY.contains(enchantment)) {
            int vitalityLvl = data.getLevel(SkillTree.VITALITY);
            int unlockLevel = plugin.getConfig().getInt("enchant-caps.protection-family.unlock-level", 4);
            if (vitalityLvl >= unlockLevel) {
                return plugin.getConfig().getInt("enchant-caps.protection-family.elevated-max", 4);
            }
            return plugin.getConfig().getInt("enchant-caps.protection-family.base-max", 3);
        }

        return -1; // this enchant isn't governed by EmpowerSMP's caps
    }

    /** Clamps `level` down to whatever `player` is allowed for `enchantment`. Never raises it. */
    public int clamp(Player player, Enchantment enchantment, int level) {
        int cap = capFor(player, enchantment);
        if (cap < 0) return level;
        return Math.min(level, cap);
    }

    public boolean isGoverned(Enchantment enchantment) {
        return enchantment.equals(Enchantment.SWEEPING_EDGE)
                || SHARPNESS_FAMILY.contains(enchantment)
                || PROTECTION_FAMILY.contains(enchantment);
    }
}
