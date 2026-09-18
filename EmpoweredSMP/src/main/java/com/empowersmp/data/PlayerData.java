package com.empowersmp.data;

import com.empowersmp.skills.SkillTree;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Holds one player's progress. Levels are 0-8 per tree.
 */
public class PlayerData {

    public enum TenacityChoice { WATER_BREATHING, NIGHT_VISION, NONE }

    private final UUID uuid;
    private final Map<SkillTree, Integer> levels = new EnumMap<>(SkillTree.class);
    private TenacityChoice tenacityChoice = TenacityChoice.NONE;
    private boolean receivedSpear = false; // Mobility L8 one-time grant

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        for (SkillTree t : SkillTree.values()) {
            levels.put(t, 0);
        }
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getLevel(SkillTree tree) {
        return levels.getOrDefault(tree, 0);
    }

    public void setLevel(SkillTree tree, int level) {
        levels.put(tree, SkillTree.clamp(level));
    }

    public void addLevel(SkillTree tree, int amount) {
        setLevel(tree, getLevel(tree) + amount);
    }

    public Map<SkillTree, Integer> allLevels() {
        return levels;
    }

    public TenacityChoice getTenacityChoice() {
        return tenacityChoice;
    }

    public void setTenacityChoice(TenacityChoice choice) {
        this.tenacityChoice = choice;
    }

    public boolean hasReceivedSpear() {
        return receivedSpear;
    }

    public void setReceivedSpear(boolean receivedSpear) {
        this.receivedSpear = receivedSpear;
    }
}
