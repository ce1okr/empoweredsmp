package com.empowersmp.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Custom items granted by the skill trees. The Mobility Level 8 reward is a
 * real Spear (Material.SPEAR, added in the Mounts of Mayhem update) enchanted
 * with Sharpness VI, exceeding vanilla's normal Sharpness V cap the same way
 * Damage L3's anvil override does for other weapons.
 */
public final class CustomItems {

    public static final NamespacedKey EMPOWERED_SPEAR_KEY =
            new NamespacedKey("empowersmp", "empowered_spear");

    private CustomItems() {}

    public static ItemStack empoweredSpear() {
        ItemStack item = new ItemStack(Material.SPEAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Empowered Spear", NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Mobility Level 8 Reward", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.addEnchant(Enchantment.SHARPNESS, 6, true); // exceeds vanilla cap on purpose
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(EMPOWERED_SPEAR_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isEmpoweredSpear(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(EMPOWERED_SPEAR_KEY, PersistentDataType.BYTE);
    }
}
