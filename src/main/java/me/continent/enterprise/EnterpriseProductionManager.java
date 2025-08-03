package me.continent.enterprise;

import me.continent.ContinentPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/** Handles production queue and recipes for enterprises. */
public class EnterpriseProductionManager {
    /** Production recipe definition. */
    public static class Recipe {
        private final String id;
        private final Map<Material, Integer> inputs;
        private final Material output;
        private final int amount;
        private final int time; // seconds

        public Recipe(String id, Map<Material, Integer> inputs, Material output, int amount, int time) {
            this.id = id;
            this.inputs = inputs;
            this.output = output;
            this.amount = amount;
            this.time = time;
        }

        public String getId() { return id; }
        public Map<Material, Integer> getInputs() { return inputs; }
        public Material getOutput() { return output; }
        public int getAmount() { return amount; }
        public int getTime() { return time; }
    }

    /** Production task waiting or running. */
    public static class ProductionTask {
        private final Recipe recipe;
        private final long finishTime;

        public ProductionTask(Recipe recipe, long finishTime) {
            this.recipe = recipe;
            this.finishTime = finishTime;
        }

        public Recipe getRecipe() { return recipe; }
        public long getFinishTime() { return finishTime; }
        public boolean isComplete() { return System.currentTimeMillis() >= finishTime; }
    }

    private static final Map<String, List<ProductionTask>> queues = new HashMap<>();
    private static final List<Recipe> recipes = new ArrayList<>();

    static {
        // default recipes
        recipes.add(new Recipe("bread", Map.of(Material.WHEAT, 3), Material.BREAD, 1, 10));
        recipes.add(new Recipe("plank", Map.of(Material.OAK_LOG, 2), Material.OAK_PLANKS, 4, 5));
        recipes.add(new Recipe("iron", Map.of(Material.IRON_ORE, 1), Material.IRON_INGOT, 1, 8));
    }

    public static List<Recipe> getRecipes() {
        return recipes;
    }

    public static List<ProductionTask> getQueue(Enterprise enterprise) {
        return queues.computeIfAbsent(enterprise.getId(), k -> new ArrayList<>());
    }

    /** Attempt to start production. */
    public static boolean start(Player player, Enterprise enterprise, Recipe recipe) {
        List<ProductionTask> q = getQueue(enterprise);
        if (q.size() >= 9) {
            player.sendMessage("§c대기열이 가득 찼습니다.");
            return false;
        }
        if (!EnterpriseStorageManager.removeGold(enterprise, 5)) {
            player.sendMessage("§c기업 골드가 부족합니다.");
            return false;
        }
        // check resources
        for (var e : recipe.getInputs().entrySet()) {
            if (!player.getInventory().containsAtLeast(new ItemStack(e.getKey()), e.getValue())) {
                player.sendMessage("§c자원이 부족합니다.");
                EnterpriseStorageManager.addGold(enterprise, 5); // refund
                return false;
            }
        }
        // consume resources
        for (var e : recipe.getInputs().entrySet()) {
            player.getInventory().removeItem(new ItemStack(e.getKey(), e.getValue()));
        }
        long finish = System.currentTimeMillis() + recipe.getTime() * 1000L;
        q.add(new ProductionTask(recipe, finish));
        return true;
    }

    /** Called every second to move completed tasks to warehouse. */
    private static void tick() {
        for (var entry : queues.entrySet()) {
            Enterprise ent = EnterpriseManager.get(entry.getKey());
            if (ent == null) continue;
            Iterator<ProductionTask> it = entry.getValue().iterator();
            while (it.hasNext()) {
                ProductionTask task = it.next();
                if (task.isComplete()) {
                    ItemStack out = new ItemStack(task.getRecipe().getOutput(), task.getRecipe().getAmount());
                    if (EnterpriseStorageManager.addItem(ent, out)) {
                        Player owner = Bukkit.getPlayer(ent.getOwner());
                        if (owner != null) {
                            owner.sendMessage("\uD83D\uDCE6 생산이 완료되어 창고에 보관되었습니다!");
                        }
                    } else {
                        Player owner = Bukkit.getPlayer(ent.getOwner());
                        if (owner != null) owner.sendMessage("§c창고가 가득 차 생산물이 폐기되었습니다.");
                    }
                    it.remove();
                }
            }
        }
    }

    /** Schedule periodic tick. */
    public static void schedule() {
        Bukkit.getScheduler().runTaskTimer(ContinentPlugin.getInstance(), EnterpriseProductionManager::tick, 20L, 20L);
    }
}
