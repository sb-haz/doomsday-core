package gg.doomsday.core.fuel;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages anti-air defense fuel storage and consumption
 * Each anti-air defense has its own fuel tank that can be filled but not emptied
 * Anti-air defenses consume 50% less fuel than missiles for firing
 */
public class AntiAirFuelManager {
    
    private final JavaPlugin plugin;
    private final Map<String, Integer> antiAirFuel; // defense key -> fuel amount
    private File fuelDataFile;
    private FileConfiguration fuelConfig;
    
    public AntiAirFuelManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.antiAirFuel = new ConcurrentHashMap<>();
        setupFuelDataFile();
        loadFuelData();
    }
    
    /**
     * Setup the fuel data file
     */
    private void setupFuelDataFile() {
        fuelDataFile = new File(plugin.getDataFolder(), "antiair_fuel.yml");
        if (!fuelDataFile.exists()) {
            try {
                fuelDataFile.createNewFile();
                plugin.getLogger().info("Created antiair_fuel.yml file");
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create antiair_fuel.yml file: " + e.getMessage());
            }
        }
        fuelConfig = YamlConfiguration.loadConfiguration(fuelDataFile);
    }
    
    /**
     * Load fuel data from file
     */
    public void loadFuelData() {
        antiAirFuel.clear();
        
        for (String key : fuelConfig.getKeys(false)) {
            int fuel = fuelConfig.getInt(key, 0);
            antiAirFuel.put(key, fuel);
        }
        
        plugin.getLogger().info("Loaded fuel data for " + antiAirFuel.size() + " anti-air defenses");
    }
    
    /**
     * Save fuel data to file
     */
    public void saveFuelData() {
        for (Map.Entry<String, Integer> entry : antiAirFuel.entrySet()) {
            fuelConfig.set(entry.getKey(), entry.getValue());
        }
        
        try {
            fuelConfig.save(fuelDataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save anti-air fuel data: " + e.getMessage());
        }
    }
    
    /**
     * Add fuel to an anti-air defense
     * @param defenseKey The defense system key
     * @param fuelAmount Amount of fuel to add
     * @return true if fuel was added successfully
     */
    public boolean addFuel(String defenseKey, int fuelAmount) {
        if (fuelAmount <= 0) {
            return false;
        }
        
        int currentFuel = antiAirFuel.getOrDefault(defenseKey, 0);
        int newFuel = currentFuel + fuelAmount;
        
        // Cap fuel at reasonable maximum (1000 units)
        newFuel = Math.min(newFuel, 1000);
        
        antiAirFuel.put(defenseKey, newFuel);
        saveFuelData();
        
        plugin.getLogger().info("Added " + fuelAmount + " fuel to anti-air " + defenseKey + 
                               " (total: " + newFuel + ")");
        
        return true;
    }
    
    /**
     * Consume fuel from an anti-air defense
     * @param defenseKey The defense system key
     * @param fuelAmount Amount of fuel to consume
     * @return true if there was enough fuel and it was consumed
     */
    public boolean consumeFuel(String defenseKey, int fuelAmount) {
        if (fuelAmount <= 0) {
            return true; // No fuel needed
        }
        
        int currentFuel = antiAirFuel.getOrDefault(defenseKey, 0);
        
        if (currentFuel < fuelAmount) {
            return false; // Not enough fuel
        }
        
        int newFuel = currentFuel - fuelAmount;
        antiAirFuel.put(defenseKey, newFuel);
        saveFuelData();
        
        plugin.getLogger().info("Consumed " + fuelAmount + " fuel from anti-air " + defenseKey + 
                               " (remaining: " + newFuel + ")");
        
        return true;
    }
    
    /**
     * Get current fuel amount for an anti-air defense
     * @param defenseKey The defense system key
     * @return Current fuel amount
     */
    public int getFuel(String defenseKey) {
        return antiAirFuel.getOrDefault(defenseKey, 0);
    }
    
    /**
     * Check if an anti-air defense has enough fuel for firing
     * @param defenseKey The defense system key
     * @param requiredFuel The amount of fuel required
     * @return true if defense has enough fuel
     */
    public boolean hasSufficientFuel(String defenseKey, int requiredFuel) {
        int currentFuel = antiAirFuel.getOrDefault(defenseKey, 0);
        return currentFuel >= requiredFuel;
    }
    
    /**
     * Get fuel requirement for an anti-air defense based on its tier
     * Lower tier defenses require less fuel, higher tier require more
     * @param defenseKey The defense system key
     * @return Required fuel amount for one shot
     */
    public int getFuelRequirement(String defenseKey) {
        String lowerKey = defenseKey.toLowerCase();
        
        // High-tier defenses (require more fuel)
        if (lowerKey.contains("s400") || lowerKey.contains("aegis") || lowerKey.contains("patriot")) {
            return 15; // 50% of typical high-tier missile fuel (30)
        }
        
        // Medium-tier defenses
        if (lowerKey.contains("hq9") || lowerKey.contains("core_defense")) {
            return 10; // 50% of typical medium-tier missile fuel (20)
        }
        
        // Low-tier/manual defenses (require less fuel)
        return 5; // 50% of typical low-tier missile fuel (10)
    }
    
    /**
     * Get all anti-air fuel data (for debugging/admin purposes)
     * @return Map of defense keys to fuel amounts
     */
    public Map<String, Integer> getAllFuelData() {
        return new HashMap<>(antiAirFuel);
    }
    
    /**
     * Reset fuel for an anti-air defense (admin command)
     * @param defenseKey The defense system key
     */
    public void resetFuel(String defenseKey) {
        antiAirFuel.put(defenseKey, 0);
        saveFuelData();
        plugin.getLogger().info("Reset fuel for anti-air " + defenseKey);
    }
    
    /**
     * Reset all fuel data (admin command)
     */
    public void resetAllFuel() {
        antiAirFuel.clear();
        saveFuelData();
        plugin.getLogger().info("Reset all anti-air fuel data");
    }
    
    /**
     * Get the tier name for display purposes
     * @param defenseKey The defense system key
     * @return Tier name (High, Medium, Low)
     */
    public String getDefenseTier(String defenseKey) {
        String lowerKey = defenseKey.toLowerCase();
        
        if (lowerKey.contains("s400") || lowerKey.contains("aegis") || lowerKey.contains("patriot")) {
            return "High";
        }
        
        if (lowerKey.contains("hq9") || lowerKey.contains("core_defense")) {
            return "Medium";
        }
        
        return "Low";
    }
}