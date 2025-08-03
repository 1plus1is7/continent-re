package me.continent.enterprise;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

/** Holds runtime enterprise data such as gold and warehouse contents. */
public class EnterpriseData {
    private final Enterprise enterprise;
    private double gold;
    private final Map<Material, Integer> storage = new HashMap<>();

    public EnterpriseData(Enterprise enterprise) {
        this.enterprise = enterprise;
    }

    public Enterprise getEnterprise() {
        return enterprise;
    }

    public double getGold() {
        return gold;
    }

    public void addGold(double amount) {
        this.gold += amount;
    }

    public boolean removeGold(double amount) {
        if (gold < amount) return false;
        gold -= amount;
        return true;
    }

    /** Map of stored items. */
    public Map<Material, Integer> getStorage() {
        return storage;
    }
}
