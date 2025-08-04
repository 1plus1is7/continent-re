package me.continent.stat;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class PlayerStats {
    private final int[] stats = new int[StatType.values().length];
    private StatType mastery = null;
    private int unusedPoints = 0;
    private int lastLevelGiven = 0;

    private static final int BASE_LIMIT = 10;
    private static final int MASTERY_LIMIT = 15;

    public int get(StatType type) {
        return stats[type.ordinal()];
    }

    public void set(StatType type, int value) {
        stats[type.ordinal()] = value;
    }

    public Map<StatType, Integer> getAll() {
        Map<StatType, Integer> map = new EnumMap<>(StatType.class);
        for (StatType t : StatType.values()) {
            map.put(t, stats[t.ordinal()]);
        }
        return Collections.unmodifiableMap(map);
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
        this.unusedPoints = Math.max(0, this.unusedPoints + amount);
    }

    /**
     * Attempts to invest one point into the given stat.
     * Returns true if successful.
     */
    public boolean investPoint(StatType type) {
        if (unusedPoints <= 0) return false;
        int idx = type.ordinal();
        int current = stats[idx];
        if (current >= limitFor(type)) return false;
        if (current >= BASE_LIMIT) {
            if (mastery == null || mastery == type) {
                mastery = type;
            } else {
                return false;
            }
        }
        stats[idx] = current + 1;
        unusedPoints--;
        return true;
    }

    /**
     * Removes one point from the given stat if possible.
     * Returns true if a point was removed.
     */
    public boolean removePoint(StatType type) {
        int idx = type.ordinal();
        int current = stats[idx];
        if (current <= 0) return false;
        stats[idx] = current - 1;
        unusedPoints++;
        if (mastery == type && current - 1 <= BASE_LIMIT) {
            mastery = null;
        }
        return true;
    }

    /**
     * Resets all stats, converting invested points back to unused points.
     */
    public void resetAll() {
        int total = 0;
        for (int i = 0; i < stats.length; i++) {
            total += stats[i];
            stats[i] = 0;
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

    private int limitFor(StatType type) {
        return mastery == type ? MASTERY_LIMIT : BASE_LIMIT;
    }
}
