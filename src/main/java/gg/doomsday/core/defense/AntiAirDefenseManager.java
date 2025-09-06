package gg.doomsday.core.defense;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import gg.doomsday.core.config.ConfigManager;

import java.util.ArrayList;
import java.util.List;

public class AntiAirDefenseManager {
    private final JavaPlugin plugin;
    private final List<AntiAirDefense> defenses;

    public AntiAirDefenseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.defenses = new ArrayList<>();
        try {
            loadDefensesFromConfig();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load anti-air configuration: " + e.getMessage());
        }
    }

    private void loadDefensesFromConfig() {
        List<AntiAirDefense> oldDefenses = new ArrayList<>(defenses);
        defenses.clear();
        
        ConfigurationSection defensesSection;
        try {
            defensesSection = ((gg.doomsday.core.DoomsdayCore) plugin).getConfigManager().getAntiairConfig().getConfigurationSection("antiair.defenses");
            if (defensesSection == null) {
                plugin.getLogger().info("No anti-air defenses configured");
                return;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to access anti-air configuration: " + e.getMessage());
            return;
        }

        for (String defenseName : defensesSection.getKeys(false)) {
            ConfigurationSection defenseConfig = defensesSection.getConfigurationSection(defenseName);
            if (defenseConfig == null) continue;

            try {
                World world = plugin.getServer().getWorld("world");
                if (world == null) {
                    plugin.getLogger().warning("World 'world' not found for defense '" + defenseName + "'");
                    continue;
                }

                double x = defenseConfig.getDouble("location.x");
                double y = defenseConfig.getDouble("location.y");
                double z = defenseConfig.getDouble("location.z");
                Location location = new Location(world, x, y, z);

                double range = defenseConfig.getDouble("range", 100.0);
                double accuracy = defenseConfig.getDouble("accuracy", 0.8);
                double interceptorSpeed = defenseConfig.getDouble("interceptorSpeed", 2.0);
                double reloadTime = defenseConfig.getDouble("reloadTime", 0.1);
                double startupTime = defenseConfig.getDouble("startupTime", 1.0);
                boolean automatic = defenseConfig.getBoolean("automatic", true);
                String displayName = defenseConfig.getString("displayName", defenseName);

                AntiAirDefense defense = new AntiAirDefense(plugin, defenseName, displayName, location, 
                    range, accuracy, interceptorSpeed, reloadTime, startupTime, automatic);
                
                // Preserve operational status from old defense if it exists
                AntiAirDefense oldDefense = findDefenseByName(oldDefenses, defenseName);
                if (oldDefense != null) {
                    defense.setOperational(oldDefense.isOperational());
                }
                
                defenses.add(defense);

                plugin.getLogger().info("Loaded anti-air defense: " + defenseName + " at (" + 
                    (int)x + ", " + (int)y + ", " + (int)z + ") with " + (int)(accuracy*100) + "% accuracy");

            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load anti-air defense '" + defenseName + "': " + e.getMessage());
            }
        }

        plugin.getLogger().info("Loaded " + defenses.size() + " anti-air defense systems");
    }

    private AntiAirDefense findDefenseByName(List<AntiAirDefense> defenseList, String name) {
        for (AntiAirDefense defense : defenseList) {
            if (defense.getName().equals(name)) {
                return defense;
            }
        }
        return null;
    }

    public void reloadDefenses() {
        loadDefensesFromConfig();
    }

    public boolean checkForInterception(Location missileStart, Location missileEnd, double missileSpeed, BlockDisplay missileEntity) {
        if (defenses.isEmpty()) {
            return false;
        }

        List<AntiAirDefense> capableDefenses = new ArrayList<>();
        for (AntiAirDefense defense : defenses) {
            if (defense.canIntercept(missileStart, missileEnd, missileSpeed)) {
                capableDefenses.add(defense);
            }
        }

        if (capableDefenses.isEmpty()) {
            return false;
        }

        AntiAirDefense selectedDefense = capableDefenses.get(0);
        double closestDistance = selectedDefense.getLocation().distance(missileEnd);

        for (AntiAirDefense defense : capableDefenses) {
            double distance = defense.getLocation().distance(missileEnd);
            if (distance < closestDistance) {
                selectedDefense = defense;
                closestDistance = distance;
            }
        }

        return selectedDefense.attemptIntercept(missileStart, missileEnd, missileSpeed, missileEntity);
    }

    public List<AntiAirDefense> getDefenses() {
        return new ArrayList<>(defenses);
    }

    public AntiAirDefense getDefenseByName(String name) {
        for (AntiAirDefense defense : defenses) {
            if (defense.getName().equalsIgnoreCase(name)) {
                return defense;
            }
        }
        return null;
    }

    public void reloadAllDefenses() {
        plugin.getLogger().info("All " + defenses.size() + " anti-air defenses have unlimited ammo");
    }

    public void setDefenseOperational(String name, boolean operational) {
        AntiAirDefense defense = getDefenseByName(name);
        if (defense != null) {
            defense.setOperational(operational);
            plugin.getLogger().info("Anti-air defense '" + name + "' set to " + (operational ? "ONLINE" : "OFFLINE"));
        }
    }

    public List<String> getDefenseStatus() {
        List<String> status = new ArrayList<>();
        status.add("§6§l=== ANTI-AIR DEFENSE STATUS ===");
        
        if (defenses.isEmpty()) {
            status.add("§7No anti-air defenses configured");
        } else {
            for (AntiAirDefense defense : defenses) {
                status.add(defense.getStatusString());
            }
        }
        
        return status;
    }
}