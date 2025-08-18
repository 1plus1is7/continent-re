package me.continent.rainfeel;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Handles rain wetness build-up, speed reduction and slippery movement.
 */
public class RainFeelService implements Listener, Runnable {
    private static final UUID SPEED_MOD_UUID = UUID.fromString("b71ad5f7-1a9f-4c26-8c9d-33ec92d16d2e");
    private static RainFeelService instance;

    private final JavaPlugin plugin;
    private final Map<UUID, Double> wetness = new HashMap<>();
    private int taskId = -1;

    private boolean enabled;
    private BiomeMode biomeMode;
    private Set<Biome> biomeSet;
    private OutdoorMethod outdoorMethod;
    private boolean allowUnderLeaves;
    private double baseMultiplier;
    private double sprintMultiplier;
    private double buildUpPerTick;
    private double decayPerTick;
    private boolean slipperyEnabled;
    private double friction;
    private double inputBoost;
    private double sprintMult;
    private double maxSpeed;
    private double minThreshold;
    private double slideBias;
    private boolean exSneak;
    private boolean exSwim;
    private boolean exFly;
    private boolean exRide;
    private boolean exClimb;
    private boolean exElytra;

    private RainFeelService(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public static void init(JavaPlugin plugin) {
        if (instance == null) {
            instance = new RainFeelService(plugin);
            Bukkit.getPluginManager().registerEvents(instance, plugin);
            instance.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, instance, 1L, 1L);
        }
    }

    public static void reload() {
        if (instance != null) {
            instance.loadConfig();
            instance.resetModifiers();
        }
    }

    public static void shutdown() {
        if (instance != null) {
            instance.resetModifiers();
            if (instance.taskId != -1) {
                Bukkit.getScheduler().cancelTask(instance.taskId);
            }
            instance.wetness.clear();
        }
    }

    private void loadConfig() {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("rainfeel");
        if (sec == null) return; // defaults already present in config.yml

        enabled = sec.getBoolean("enabled", true);
        biomeMode = "whitelist".equalsIgnoreCase(sec.getString("biome-mode")) ? BiomeMode.WHITELIST : BiomeMode.BLACKLIST;
        biomeSet = new HashSet<>();
        for (String name : sec.getStringList("biomes")) {
            try {
                biomeSet.add(Biome.valueOf(name.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
            }
        }

        ConfigurationSection out = sec.getConfigurationSection("outdoor-check");
        if (out != null) {
            outdoorMethod = "can_see_sky".equalsIgnoreCase(out.getString("method")) ? OutdoorMethod.CAN_SEE_SKY : OutdoorMethod.HIGHEST_BLOCK;
            allowUnderLeaves = out.getBoolean("allow-under-leaves", false);
        } else {
            outdoorMethod = OutdoorMethod.HIGHEST_BLOCK;
            allowUnderLeaves = false;
        }

        ConfigurationSection sp = sec.getConfigurationSection("speed");
        if (sp != null) {
            baseMultiplier = sp.getDouble("base-multiplier", 0.92);
            sprintMultiplier = sp.getDouble("sprint-multiplier", 0.95);
            buildUpPerTick = sp.getDouble("build_up_per_tick", 0.02);
            decayPerTick = sp.getDouble("decay_per_tick", 0.05);
        }

        ConfigurationSection slip = sec.getConfigurationSection("slippery");
        if (slip != null) {
            slipperyEnabled = slip.getBoolean("enabled", true);
            friction = slip.getDouble("friction", 0.984);
            inputBoost = slip.getDouble("input-boost", 0.018);
            sprintMult = slip.getDouble("sprint-mult", 1.12);
            maxSpeed = slip.getDouble("max-speed", 0.60);
            minThreshold = slip.getDouble("min-threshold", 0.025);
            slideBias = slip.getDouble("slide-bias", 0.08);
        }

        ConfigurationSection ex = sec.getConfigurationSection("exclude");
        exSneak = ex == null || ex.getBoolean("sneaking", true);
        exSwim = ex == null || ex.getBoolean("swimming", true);
        exFly = ex == null || ex.getBoolean("flying", true);
        exRide = ex == null || ex.getBoolean("riding", true);
        exClimb = ex == null || ex.getBoolean("climbing", true);
        exElytra = ex == null || ex.getBoolean("elytra", true);
    }

    private void resetModifiers() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            AttributeInstance attr = p.getAttribute(Attribute.MOVEMENT_SPEED);
            if (attr != null) {
                attr.removeModifier(SPEED_MOD_UUID);
            }
        }
    }

    @Override
    public void run() {
        if (!enabled) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID id = p.getUniqueId();
            double w = wetness.getOrDefault(id, 0.0);
            boolean cond = shouldBuildUp(p);
            if (cond) {
                w = Math.min(1.0, w + buildUpPerTick);
            } else {
                w = Math.max(0.0, w - decayPerTick);
            }
            wetness.put(id, w);
            applySpeed(p, w, cond && !isExcluded(p));
        }
    }

    private boolean shouldBuildUp(Player p) {
        World world = p.getWorld();
        if (!world.hasStorm()) return false;
        if (!isOutdoors(p)) return false;
        if (!isBiomeAllowed(p)) return false;
        return true;
    }

    private boolean isBiomeAllowed(Player p) {
        Biome biome = p.getLocation().getBlock().getBiome();
        boolean contains = biomeSet.contains(biome);
        return biomeMode == BiomeMode.BLACKLIST ? !contains : contains;
    }

    private boolean isOutdoors(Player p) {
        if (outdoorMethod == OutdoorMethod.CAN_SEE_SKY) {
            if (!allowUnderLeaves) {
                Block highest = p.getWorld().getHighestBlockAt(p.getLocation());
                if (highest.getY() > p.getLocation().getY() && highest.getType().name().endsWith("LEAVES")) {
                    return false;
                }
            }
            return p.getLocation().getBlock().getLightFromSky() == 15;
        }

        Block highest = p.getWorld().getHighestBlockAt(p.getLocation());
        if (highest.getY() > p.getLocation().getY()) {
            if (highest.getType().name().endsWith("LEAVES")) {
                return allowUnderLeaves;
            }
            return false;
        }
        return true;
    }

    private boolean isExcluded(Player p) {
        if (exSneak && p.isSneaking()) return true;
        if (exSwim && p.isSwimming()) return true;
        if (exFly && p.isFlying()) return true;
        if (exRide && p.isInsideVehicle()) return true;
        if (exClimb && p.isClimbing()) return true;
        if (exElytra && p.isGliding()) return true;
        return false;
    }

    private void applySpeed(Player p, double wet, boolean active) {
        AttributeInstance attr = p.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr == null) return;
        attr.removeModifier(SPEED_MOD_UUID);
        if (active && wet > 0) {
            double target = p.isSprinting() ? sprintMultiplier : baseMultiplier;
            double finalMult = 1.0 + (target - 1.0) * wet;
            AttributeModifier mod = new AttributeModifier(SPEED_MOD_UUID, "rainfeel-speed", finalMult - 1.0, AttributeModifier.Operation.ADD_SCALAR);
            attr.addTransientModifier(mod);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        wetness.put(event.getPlayer().getUniqueId(), 0.0);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        wetness.remove(event.getPlayer().getUniqueId());
        AttributeInstance attr = event.getPlayer().getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr != null) {
            attr.removeModifier(SPEED_MOD_UUID);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!enabled || !slipperyEnabled) return;
        Player p = event.getPlayer();
        if (!shouldBuildUp(p)) return;
        if (isExcluded(p)) return;

        Vector vel = p.getVelocity();
        Vector horiz = vel.clone();
        horiz.setY(0);
        horiz.multiply(friction);

        Vector inputDir = event.getTo().toVector().subtract(event.getFrom().toVector());
        inputDir.setY(0);
        if (inputDir.lengthSquared() > 0) {
            inputDir.normalize().multiply(inputBoost * (p.isSprinting() ? sprintMult : 1.0));
            Vector side = new Vector(-inputDir.getZ(), 0, inputDir.getX());
            if (side.lengthSquared() > 0) {
                side.normalize().multiply(slideBias);
                horiz.add(side);
            }
        }
        horiz.add(inputDir);
        double len = horiz.length();
        if (len > maxSpeed) {
            horiz.normalize().multiply(maxSpeed);
        } else if (len < minThreshold) {
            horiz.setX(0);
            horiz.setZ(0);
        }

        vel.setX(horiz.getX());
        vel.setZ(horiz.getZ());
        p.setVelocity(vel);
    }

    private enum BiomeMode {BLACKLIST, WHITELIST}

    private enum OutdoorMethod {HIGHEST_BLOCK, CAN_SEE_SKY}
}

