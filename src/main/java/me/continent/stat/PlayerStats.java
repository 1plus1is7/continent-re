package me.continent.stat;

import java.util.EnumMap;
import java.util.Map;

public class PlayerStats {
    private final Map<StatType, Integer> stats = new EnumMap<>(StatType.class);
    private StatType mastery = null;
    private int unusedPoints = 0;
    private int lastLevelGiven = 0;

    private static final int BASE_LIMIT = 10;
    private static final int MASTERY_LIMIT = 15;

    public PlayerStats() {
        for (StatType type : StatType.values()) {
            stats.put(type, 0);
        }
    }

    public int get(StatType type) {
        return stats.getOrDefault(type, 0);
    }

    public void set(StatType type, int value) {
        stats.put(type, value);
    }

    public Map<StatType, Integer> getAll() {
        return stats;
    }

    public StatType getMastery() {
        return mastery;
    }

    public void setMastery(StatType mastery) {
        this.mastery = mastery;
    }

    public int getUnusedPoints() {
        return unusedPoints;
    }

    /**
     * Sets the number of unused stat points the player currently has.
     * Negative values are clamped to zero.
     */
    public void setPoints(int amount) {
        this.unusedPoints = Math.max(0, amount);
    }

    public void addPoints(int amount) {
        this.unusedPoints += amount;
    }

    /**
     * Attempts to invest one point into the given stat.
     * Returns true if successful.
     */
    public boolean investPoint(StatType type) {
        int current = get(type);
        int limit = mastery == type ? MASTERY_LIMIT : BASE_LIMIT;
        if (unusedPoints <= 0 || current >= limit) return false;
        if (current + 1 > BASE_LIMIT) {
            if (mastery == null || mastery == type) {
                mastery = type;
            } else {
                return false;
            }
        }
        stats.put(type, current + 1);
        unusedPoints--;
        return true;
    }

    /**
     * Removes one point from the given stat if possible.
     * Returns true if a point was removed.
     */
    public boolean removePoint(StatType type) {
        int current = get(type);
        if (current <= 0) return false;
        stats.put(type, current - 1);
        unusedPoints++;
        if (current - 1 < BASE_LIMIT + 1 && mastery == type) {
            mastery = null;
        }
        return true;
    }

    /**
     * Resets all stats, converting invested points back to unused points.
     */
    public void resetAll() {
        int total = 0;
        for (StatType type : StatType.values()) {
            total += get(type);
            stats.put(type, 0);
        }
        unusedPoints += total;
        mastery = null;
    }

    public int getLastLevelGiven() {
        return lastLevelGiven;
    }

    public void setLastLevelGiven(int lvl) {
        this.lastLevelGiven = lvl;
    }
}
