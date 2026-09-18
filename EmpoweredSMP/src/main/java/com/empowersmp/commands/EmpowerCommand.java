package com.empowersmp.commands;

import com.empowersmp.EmpowerSMP;
import com.empowersmp.data.PlayerData;
import com.empowersmp.skills.SkillTree;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /empower give <player> <tree> <amount>   - grant levels (admin, after events)
 * /empower set  <player> <tree> <level>    - set an exact level (admin)
 * /empower levels [player]                 - view current levels
 * /empower choice <waterbreathing|nightvision> - pick Tenacity L1 effect
 * /empower info                            - list the trees
 */
public class EmpowerCommand implements CommandExecutor, TabCompleter {

    private final EmpowerSMP plugin;

    public EmpowerCommand(EmpowerSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /empower <give|set|levels|choice|info>", NamedTextColor.RED));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give" -> handleGiveOrSet(sender, args, true);
            case "set" -> handleGiveOrSet(sender, args, false);
            case "levels" -> handleLevels(sender, args);
            case "choice" -> handleChoice(sender, args);
            case "info" -> handleInfo(sender);
            default -> sender.sendMessage(Component.text("Unknown subcommand.", NamedTextColor.RED));
        }
        return true;
    }

    private void handleGiveOrSet(CommandSender sender, String[] args, boolean relative) {
        if (!sender.hasPermission("empowersmp.admin")) {
            sender.sendMessage(Component.text("You don't have permission to do that.", NamedTextColor.RED));
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(Component.text(
                    "Usage: /empower " + args[0] + " <player> <tree> <amount>", NamedTextColor.RED));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        SkillTree tree = SkillTree.fromString(args[2]);
        if (tree == null) {
            sender.sendMessage(Component.text("Unknown tree: " + args[2], NamedTextColor.RED));
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Amount must be a number.", NamedTextColor.RED));
            return;
        }

        PlayerData data = plugin.getDataManager().get(target.getUniqueId());
        int before = data.getLevel(tree);
        if (relative) {
            data.addLevel(tree, amount);
        } else {
            data.setLevel(tree, amount);
        }
        int after = data.getLevel(tree);
        plugin.getDataManager().save(data);

        sender.sendMessage(Component.text(
                (target.getName() == null ? args[1] : target.getName()) + "'s " + tree.displayName()
                        + " level: " + before + " -> " + after, NamedTextColor.GREEN));

        Player online = target.getPlayer();
        if (online != null) {
            plugin.getEffectManager().refresh(online, data);
            online.sendMessage(Component.text(
                    "Your " + tree.displayName() + " level is now " + after + "!", NamedTextColor.LIGHT_PURPLE));
        }
    }

    private void handleLevels(CommandSender sender, String[] args) {
        OfflinePlayer target;
        if (args.length >= 2) {
            target = Bukkit.getOfflinePlayer(args[1]);
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(Component.text("Specify a player.", NamedTextColor.RED));
            return;
        }
        PlayerData data = plugin.getDataManager().get(target.getUniqueId());
        sender.sendMessage(Component.text("=== " + (target.getName() == null ? "Player" : target.getName())
                + "'s EmpowerSMP levels ===", NamedTextColor.GOLD));
        for (SkillTree tree : SkillTree.values()) {
            sender.sendMessage(Component.text(
                    tree.displayName() + ": " + data.getLevel(tree) + "/" + SkillTree.MAX_LEVEL,
                    NamedTextColor.AQUA));
        }
    }

    private void handleChoice(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /empower choice <waterbreathing|nightvision>", NamedTextColor.RED));
            return;
        }
        PlayerData data = plugin.getDataManager().get(player.getUniqueId());
        PlayerData.TenacityChoice choice = switch (args[1].toLowerCase()) {
            case "waterbreathing", "water_breathing", "water" -> PlayerData.TenacityChoice.WATER_BREATHING;
            case "nightvision", "night_vision", "night" -> PlayerData.TenacityChoice.NIGHT_VISION;
            default -> null;
        };
        if (choice == null) {
            player.sendMessage(Component.text("Choose either 'waterbreathing' or 'nightvision'.", NamedTextColor.RED));
            return;
        }
        data.setTenacityChoice(choice);
        plugin.getDataManager().save(data);
        plugin.getEffectManager().refresh(player, data);
        player.sendMessage(Component.text("Tenacity Level 1 effect set to " + choice.name() + ".", NamedTextColor.GREEN));
    }

    private void handleInfo(CommandSender sender) {
        sender.sendMessage(Component.text("=== EmpowerSMP Skill Trees ===", NamedTextColor.GOLD));
        for (SkillTree tree : SkillTree.values()) {
            sender.sendMessage(Component.text("- " + tree.displayName() + " (levels 0-" + SkillTree.MAX_LEVEL + ")",
                    NamedTextColor.AQUA));
        }
        sender.sendMessage(Component.text("Levels are awarded by admins after events. Ask a staff member!",
                NamedTextColor.GRAY));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("give", "set", "levels", "choice", "info"), args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("levels"))) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[1]);
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("set"))) {
            return filter(List.of("Mobility", "Damage", "Vitality", "Prosperity", "Tenacity"), args[2]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("choice")) {
            return filter(List.of("waterbreathing", "nightvision"), args[1]);
        }
        return new ArrayList<>();
    }

    private List<String> filter(List<String> options, String prefix) {
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}
