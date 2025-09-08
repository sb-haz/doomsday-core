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
 * Manages missile fuel storage and consumption
 * Each missile has its own fuel tank that can be filled but not emptied
 */
public class MissileFuelManager {
    
    private final JavaPlugin plugin;
    private final Map<String, Integer> missileFuel; // missile key -> fuel amount
    private File fuelDataFile;
    private FileConfiguration fuelConfig;
    
    public MissileFuelManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.missileFuel = new ConcurrentHashMap<>();
        setupFuelDataFile();
        loadFuelData();
    }
    
    /**
     * Setup the fuel data file
     */
    private void setupFuelDataFile() {
        fuelDataFile = new File(plugin.getDataFolder(), "missile_fuel.yml");
        if (!fuelDataFile.exists()) {
            try {
                fuelDataFile.createNewFile();
                plugin.getLogger().info("Created missile_fuel.yml file");
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create missile_fuel.yml file: " + e.getMessage());
            }
        }
        fuelConfig = YamlConfiguration.loadConfiguration(fuelDataFile);
    }
    
    /**
     * Load fuel data from file
     */
    public void loadFuelData() {
        missileFuel.clear();
        
        for (String key : fuelConfig.getKeys(false)) {
            int fuel = fuelConfig.getInt(key, 0);
            missileFuel.put(key, fuel);
        }
        
        plugin.getLogger().info("Loaded fuel data for " + missileFuel.size() + " missiles");
    }
    
    /**
     * Save fuel data to file
     */
    public void saveFuelData() {
        for (Map.Entry<String, Integer> entry : missileFuel.entrySet()) {
            fuelConfig.set(entry.getKey(), entry.getValue());
        }
        
        try {
            fuelConfig.save(fuelDataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save missile fuel data: " + e.getMessage());
        }
    }
    
    /**
     * Add fuel to a missile
     * @param missileKey The missile configuration key
     * @param fuelAmount Amount of fuel to add
     * @return true if fuel was added successfully
     */
    public boolean addFuel(String missileKey, int fuelAmount) {
        if (fuelAmount <= 0) {
            return false;
        }
        
        int currentFuel = missileFuel.getOrDefault(missileKey, 0);
        int newFuel = currentFuel + fuelAmount;
        
        // Cap fuel at reasonable maximum (1000 units)
        newFuel = Math.min(newFuel, 1000);
        
        missileFuel.put(missileKey, newFuel);
        saveFuelData();
        
        plugin.getLogger().info("Added " + fuelAmount + " fuel to missile " + missileKey + 
                               " (total: " + newFuel + ")");
        
        return true;
    }
    
    /**
     * Consume fuel from a missile
     * @param missileKey The missile configuration key
     * @param fuelAmount Amount of fuel to consume
     * @return true if there was enough fuel and it was consumed
     */
    public boolean consumeFuel(String missileKey, int fuelAmount) {
        if (fuelAmount <= 0) {
            return true; // No fuel needed
        }
        
        int currentFuel = missileFuel.getOrDefault(missileKey, 0);
        
        if (currentFuel < fuelAmount) {
            return false; // Not enough fuel
        }
        
        int newFuel = currentFuel - fuelAmount;
        missileFuel.put(missileKey, newFuel);
        saveFuelData();
        
        plugin.getLogger().info("Consumed " + fuelAmount + " fuel from missile " + missileKey + 
                               " (remaining: " + newFuel + ")");
        
        return true;
    }
    
    /**
     * Get current fuel amount for a missile
     * @param missileKey The missile configuration key
     * @return Current fuel amount
     */
    public int getFuel(String missileKey) {
        return missileFuel.getOrDefault(missileKey, 0);
    }
    
    /**
     * Check if a missile has enough fuel for launch
     * @param missileKey The missile configuration key
     * @param requiredFuel The amount of fuel required
     * @return true if missile has enough fuel
     */
    public boolean hasSufficientFuel(String missileKey, int requiredFuel) {
        int currentFuel = missileFuel.getOrDefault(missileKey, 0);
        return currentFuel >= requiredFuel;
    }
    
    /**
     * Get all missile fuel data (for debugging/admin purposes)
     * @return Map of missile keys to fuel amounts
     */
    public Map<String, Integer> getAllFuelData() {
        return new HashMap<>(missileFuel);
    }
    
    /**
     * Reset fuel for a missile (admin command)
     * @param missileKey The missile configuration key
     */
    public void resetFuel(String missileKey) {
        missileFuel.put(missileKey, 0);
        saveFuelData();
        plugin.getLogger().info("Reset fuel for missile " + missileKey);
    }
    
    /**
     * Reset all fuel data (admin command)
     */
    public void resetAllFuel() {
        missileFuel.clear();
        saveFuelData();
        plugin.getLogger().info("Reset all missile fuel data");
    }
}