package com.empowersmp.data;

import com.empowersmp.skills.SkillTree;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Loads/saves PlayerData to playerdata/<uuid>.yml and keeps an in-memory cache
 * for online players.
 */
public class DataManager {

    private final JavaPlugin plugin;
    private final File folder;
    private final Map<UUID, PlayerData> cache = new HashMap<>();

    public DataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "playerdata");
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    public PlayerData get(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::load);
    }

    public void unload(UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data != null) {
            save(data);
        }
    }

    public void saveAll() {
        for (PlayerData data : cache.values()) {
            save(data);
        }
    }

    private PlayerData load(UUID uuid) {
        PlayerData data = new PlayerData(uuid);
        File file = new File(folder, uuid.toString() + ".yml");
        if (!file.exists()) {
            return data;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (SkillTree tree : SkillTree.values()) {
            data.setLevel(tree, yml.getInt("levels." + tree.name(), 0));
        }
        String choice = yml.getString("tenacityChoice", "NONE");
        try {
            data.setTenacityChoice(PlayerData.TenacityChoice.valueOf(choice));
        } catch (IllegalArgumentException ignored) {
            data.setTenacityChoice(PlayerData.TenacityChoice.NONE);
        }
        data.setReceivedSpear(yml.getBoolean("receivedSpear", false));
        return data;
    }

    public void save(PlayerData data) {
        File file = new File(folder, data.getUuid().toString() + ".yml");
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<SkillTree, Integer> e : data.allLevels().entrySet()) {
            yml.set("levels." + e.getKey().name(), e.getValue());
        }
        yml.set("tenacityChoice", data.getTenacityChoice().name());
        yml.set("receivedSpear", data.hasReceivedSpear());
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save data for " + data.getUuid(), e);
        }
    }
}
