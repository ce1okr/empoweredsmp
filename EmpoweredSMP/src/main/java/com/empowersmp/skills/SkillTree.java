package com.empowersmp.skills;

/**
 * The five skill trees. Each has levels 0 (unranked) through 8 (maxed).
 */
public enum SkillTree {
    MOBILITY("Mobility"),
    DAMAGE("Damage"),
    VITALITY("Vitality"),
    PROSPERITY("Prosperity"),
    TENACITY("Tenacity");

    public static final int MAX_LEVEL = 8;
    public static final int MIN_LEVEL = 0;

    private final String displayName;

    SkillTree(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static SkillTree fromString(String s) {
        for (SkillTree t : values()) {
            if (t.name().equalsIgnoreCase(s) || t.displayName.equalsIgnoreCase(s)) {
                return t;
            }
        }
        return null;
    }

    public static int clamp(int level) {
        return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
    }
}
