package me.continent.stat;

import java.util.EnumMap;
import java.util.Map;

public class PlayerStats {
    private final Map<StatType, Integer> stats = new EnumMap<>(StatType.class);
    private StatType mastery = null;
    private int unusedPoints = 0;
    private int lastLevelGiven = 0;

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

    public void usePoint() {
        if (unusedPoints > 0) unusedPoints--;
    }

    /**
     * Removes one point from the given stat if possible.
     * Returns true if a point was removed.
     */
    public boolean refundPoint(StatType type) {
        int current = get(type);
        if (current <= 0) return false;
        stats.put(type, current - 1);
        unusedPoints++;
        if (current - 1 < 11 && mastery == type) {
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
