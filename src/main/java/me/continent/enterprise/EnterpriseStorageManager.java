package me.continent.enterprise;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/** Manages enterprise storage (warehouse) and gold. */
public class EnterpriseStorageManager {
    private static final Map<String, EnterpriseData> data = new HashMap<>();
    private static final int MAX_SLOTS = 64;

    private static EnterpriseData getData(Enterprise enterprise) {
        return data.computeIfAbsent(enterprise.getId(), k -> new EnterpriseData(enterprise));
    }

    public static double getGold(Enterprise enterprise) {
        return getData(enterprise).getGold();
    }

    public static void addGold(Enterprise enterprise, double amount) {
        getData(enterprise).addGold(amount);
    }

    public static boolean removeGold(Enterprise enterprise, double amount) {
        return getData(enterprise).removeGold(amount);
    }

    /** Attempt to store item into enterprise warehouse. */
    public static boolean addItem(Enterprise enterprise, ItemStack stack) {
        EnterpriseData d = getData(enterprise);
        Map<Material, Integer> storage = d.getStorage();
        if (!storage.containsKey(stack.getType()) && storage.size() >= MAX_SLOTS) {
            return false; // full
        }
        storage.merge(stack.getType(), stack.getAmount(), Integer::sum);
        return true;
    }

    public static Map<Material, Integer> getStorage(Enterprise enterprise) {
        return getData(enterprise).getStorage();
    }

    /** Clear storage contents. */
    public static void clear(Enterprise enterprise) {
        getData(enterprise).getStorage().clear();
    }
}
