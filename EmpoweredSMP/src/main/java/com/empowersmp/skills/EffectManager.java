package com.empowersmp.skills;

import com.empowersmp.EmpowerSMP;
import com.empowersmp.data.PlayerData;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Central place that (re)applies all permanent passive effects for a player
 * based on their current skill levels. Call refresh(player) whenever a
 * player's data changes, and on join.
 *
 * Potion-based "permanent" effects are applied as infinite-duration, ambient,
 * particle-free effects so they survive relogs (re-applied on join) without
 * spamming the player's screen with icons/particles.
 */
public class EffectManager {

    private static final int INFINITE = PotionEffect.INFINITE_DURATION;

    private final EmpowerSMP plugin;

    public EffectManager(EmpowerSMP plugin) {
        this.plugin = plugin;
    }

    public void refresh(Player player, PlayerData data) {
        applyMobility(player, data);
        applyDamage(player, data);
        applyVitality(player, data);
        applyProsperity(player, data);
        applyTenacity(player, data);
    }

    // ---------------------------------------------------------------
    // Mobility
    // ---------------------------------------------------------------
    private void applyMobility(Player player, PlayerData data) {
        int lvl = data.getLevel(SkillTree.MOBILITY);

        // Speed amplifier: L1 -> Speed I (0), L5 -> Speed II (1), L6 -> Speed III (2, optional)
        int speedAmp = -1;
        if (lvl >= 1) speedAmp = 0;
        if (lvl >= 5) speedAmp = 1;
        boolean speed3Enabled = plugin.getConfig().getBoolean("mobility.level6-speed3-enabled", true);
        if (lvl >= 6 && speed3Enabled) speedAmp = 2;
        setPermanent(player, PotionEffectType.SPEED, speedAmp);

        // Weaving, permanent from level 3 (real 1.21 Tricky Trials status effect)
        setPermanent(player, PotionEffectType.WEAVING, lvl >= 3 ? 0 : -1);

        // Dolphin's Grace I permanent from level 4
        setPermanent(player, PotionEffectType.DOLPHINS_GRACE, lvl >= 4 ? 0 : -1);

        // Swift Sneak III (L2) and Soul Speed III (L7) are applied to worn boots -
        // see ArmorEnchantListener, which reacts to this level via getLevel().

        // Level 8: one-time grant of the "Empowered Spear" (Sharpness VI spear).
        if (lvl >= 8 && !data.hasReceivedSpear()) {
            player.getInventory().addItem(com.empowersmp.items.CustomItems.empoweredSpear());
            data.setReceivedSpear(true);
        }
    }

    // ---------------------------------------------------------------
    // Damage
    // ---------------------------------------------------------------
    private void applyDamage(Player player, PlayerData data) {
        int lvl = data.getLevel(SkillTree.DAMAGE);

        // Strength amplifier: L1 -> Strength I (0), L5 -> Strength II (1)
        int strAmp = -1;
        if (lvl >= 1) strAmp = 0;
        if (lvl >= 5) strAmp = 1;
        setPermanent(player, PotionEffectType.STRENGTH, strAmp);

        // Crit chance is handled by CombatListener. Sharpness/Sweeping Edge cap
        // overrides for levels 3 and 6 are handled by EnchantCapListener, which
        // reads the level directly at the moment of the relevant event.
    }

    // ---------------------------------------------------------------
    // Vitality
    // ---------------------------------------------------------------
    private void applyVitality(Player player, PlayerData data) {
        int vLvl = data.getLevel(SkillTree.VITALITY);
        int tLvl = data.getLevel(SkillTree.TENACITY);

        // Max health in half-hearts (2 per heart). Base vanilla is 20 (10 hearts).
        double maxHealth = 20.0;
        if (vLvl >= 1) maxHealth = Math.max(maxHealth, 24.0); // 12 hearts
        if (vLvl >= 3) maxHealth = Math.max(maxHealth, 26.0); // 13 hearts
        if (vLvl >= 4) maxHealth = Math.max(maxHealth, 28.0); // 14 hearts
        if (vLvl >= 5) maxHealth = Math.max(maxHealth, 30.0); // 15 hearts
        if (vLvl >= 6) maxHealth = Math.max(maxHealth, 34.0); // 17 hearts
        if (vLvl >= 7) maxHealth = Math.max(maxHealth, 38.0); // 19 hearts
        if (vLvl >= 8) maxHealth = Math.max(maxHealth, 40.0); // 20 hearts
        // Tenacity L6 also grants a "12 hearts" floor per the design doc.
        if (tLvl >= 6) maxHealth = Math.max(maxHealth, 24.0);

        AttributeInstance healthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null) {
            double old = healthAttr.getBaseValue();
            healthAttr.setBaseValue(maxHealth);
            // If max health increased, top the player up by the difference so they
            // don't spawn/relog at a sliver of a much bigger bar; never lowers current HP.
            if (maxHealth > old) {
                player.setHealth(Math.min(maxHealth, player.getHealth() + (maxHealth - old)));
            } else if (player.getHealth() > maxHealth) {
                player.setHealth(maxHealth);
            }
        }

        // Regeneration amplifier: L2 -> Regen I (0), L5 -> Regen II (1)
        int regenAmp = -1;
        if (vLvl >= 2) regenAmp = 0;
        if (vLvl >= 5) regenAmp = 1;
        setPermanent(player, PotionEffectType.REGENERATION, regenAmp);

        // Absorption: combine Vitality L7 (Absorption I) with Tenacity L5/L7 (Absorption I/II).
        int absorptionAmp = -1;
        if (vLvl >= 7) absorptionAmp = Math.max(absorptionAmp, 0);
        if (tLvl >= 5) absorptionAmp = Math.max(absorptionAmp, 0);
        if (tLvl >= 7) absorptionAmp = Math.max(absorptionAmp, 1);
        setPermanent(player, PotionEffectType.ABSORPTION, absorptionAmp);

        // Vitality L4's "Allow Protection IV" is enforced by EnchantCapListener.
        // Tenacity L4 "Override Armor Limit to Netherite" remains a no-op (see README).
    }

    // ---------------------------------------------------------------
    // Prosperity
    // ---------------------------------------------------------------
    private void applyProsperity(Player player, PlayerData data) {
        int lvl = data.getLevel(SkillTree.PROSPERITY);

        // Hero of the Village amplifier: L1->II(1), L2->IV(3), L3->VI(5), L4->VIII(7), L5->X(9)
        int hotvAmp = -1;
        if (lvl >= 1) hotvAmp = 1;
        if (lvl >= 2) hotvAmp = 3;
        if (lvl >= 3) hotvAmp = 5;
        if (lvl >= 4) hotvAmp = 7;
        if (lvl >= 5) hotvAmp = 9;
        setPermanent(player, PotionEffectType.HERO_OF_THE_VILLAGE, hotvAmp);

        // Ore drop bonus, XP multiplier, potion-duration multiplier, and villager trade
        // multiplier are all handled at the moment of the relevant event by
        // ProsperityListener, which reads the level directly.
    }

    // ---------------------------------------------------------------
    // Tenacity
    // ---------------------------------------------------------------
    private void applyTenacity(Player player, PlayerData data) {
        int lvl = data.getLevel(SkillTree.TENACITY);

        // Level 1: player's chosen Water Breathing or Night Vision, permanent.
        PotionEffectType chosen = null;
        if (lvl >= 1) {
            switch (data.getTenacityChoice()) {
                case WATER_BREATHING -> chosen = PotionEffectType.WATER_BREATHING;
                case NIGHT_VISION -> chosen = PotionEffectType.NIGHT_VISION;
                case NONE -> chosen = null; // player hasn't picked yet; prompted by command handler
            }
        }
        // Clear whichever of the two isn't chosen, then apply the chosen one.
        if (chosen != PotionEffectType.WATER_BREATHING) player.removePotionEffect(PotionEffectType.WATER_BREATHING);
        if (chosen != PotionEffectType.NIGHT_VISION) player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        if (chosen != null) {
            setPermanent(player, chosen, 0);
        }

        setPermanent(player, PotionEffectType.FIRE_RESISTANCE, lvl >= 2 ? 0 : -1);
        setPermanent(player, PotionEffectType.RESISTANCE, lvl >= 3 ? 0 : -1);

        // Knockback resistance, Level 8
        AttributeInstance kb = player.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (kb != null) {
            double value = lvl >= 8
                    ? plugin.getConfig().getDouble("tenacity.knockback-resistance-level8", 1.0)
                    : 0.0;
            kb.setBaseValue(value);
        }

        // Absorption is combined in applyVitality(); armor-tier override is enforced
        // by ArmorEnchantListener/inventory checks.
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /**
     * Applies an infinite, ambient, icon-free potion effect at the given amplifier,
     * or removes the effect entirely if amplifier is negative.
     */
    private void setPermanent(Player player, PotionEffectType type, int amplifier) {
        if (amplifier < 0) {
            player.removePotionEffect(type);
            return;
        }
        // Avoid redundant re-application churn if already correct.
        PotionEffect current = player.getPotionEffect(type);
        if (current != null && current.getAmplifier() == amplifier && current.getDuration() > 20 * 60) {
            return;
        }
        player.addPotionEffect(new PotionEffect(type, INFINITE, amplifier, true, false, false));
    }
}
