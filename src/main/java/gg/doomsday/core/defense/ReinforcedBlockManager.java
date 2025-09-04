package gg.doomsday.core.defense;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ReinforcedBlockManager {
    
    private final JavaPlugin plugin;
    private final Map<String, Long> reinforcedBlocks = new ConcurrentHashMap<>();
    private final File dataFile;
    private List<String> validBlocks;
    private Map<String, Double> resistanceValues;
    
    public ReinforcedBlockManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "reinforced_blocks.yml");
        loadConfiguration();
        loadReinforcedBlocks();
    }
    
    private void loadConfiguration() {
        FileConfiguration config = plugin.getConfig();
        validBlocks = config.getStringList("reinforcement.validBlocks");
        
        // Load resistance values
        resistanceValues = new HashMap<>();
        if (config.isConfigurationSection("reinforcement.resistance")) {
            for (String key : config.getConfigurationSection("reinforcement.resistance").getKeys(false)) {
                resistanceValues.put(key, config.getDouble("reinforcement.resistance." + key));
            }
        }
        
        plugin.getLogger().info("Loaded " + validBlocks.size() + " valid reinforcement materials");
        plugin.getLogger().info("Loaded " + resistanceValues.size() + " resistance values");
    }
    
    public boolean isValidBlock(Material material) {
        return validBlocks.contains(material.name());
    }
    
    public boolean reinforceBlock(Location location) {
        if (!isValidBlock(location.getBlock().getType())) {
            return false;
        }
        
        String blockKey = getBlockKey(location);
        long currentTime = System.currentTimeMillis();
        reinforcedBlocks.put(blockKey, currentTime);
        
        plugin.getLogger().info("Reinforced block at " + blockKey);
        return true;
    }
    
    public boolean isReinforced(Location location) {
        return reinforcedBlocks.containsKey(getBlockKey(location));
    }
    
    public double getResistance(Location location) {
        if (!isReinforced(location)) {
            return 0.0;
        }
        
        Material material = location.getBlock().getType();
        return resistanceValues.getOrDefault(material.name(), 0.0);
    }
    
    public void removeReinforcement(Location location) {
        String blockKey = getBlockKey(location);
        if (reinforcedBlocks.remove(blockKey) != null) {
            plugin.getLogger().info("Removed reinforcement from block at " + blockKey);
        }
    }
    
    private String getBlockKey(Location location) {
        return location.getWorld().getName() + ":" + 
               location.getBlockX() + ":" + 
               location.getBlockY() + ":" + 
               location.getBlockZ();
    }
    
    public void saveReinforcedBlocks() {
        try {
            YamlConfiguration config = new YamlConfiguration();
            
            for (Map.Entry<String, Long> entry : reinforcedBlocks.entrySet()) {
                config.set("blocks." + entry.getKey(), entry.getValue());
            }
            
            config.save(dataFile);
            plugin.getLogger().info("Saved " + reinforcedBlocks.size() + " reinforced blocks to file");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save reinforced blocks: " + e.getMessage());
        }
    }
    
    private void loadReinforcedBlocks() {
        if (!dataFile.exists()) {
            plugin.getLogger().info("No reinforced blocks data file found, starting fresh");
            return;
        }
        
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
            
            if (config.isConfigurationSection("blocks")) {
                for (String key : config.getConfigurationSection("blocks").getKeys(false)) {
                    long timestamp = config.getLong("blocks." + key);
                    
                    // Validate that the world and location still exist
                    if (validateBlockLocation(key)) {
                        reinforcedBlocks.put(key, timestamp);
                    } else {
                        plugin.getLogger().info("Removed invalid reinforced block: " + key);
                    }
                }
            }
            
            plugin.getLogger().info("Loaded " + reinforcedBlocks.size() + " reinforced blocks from file");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load reinforced blocks: " + e.getMessage());
        }
    }
    
    private boolean validateBlockLocation(String blockKey) {
        try {
            String[] parts = blockKey.split(":");
            if (parts.length != 4) return false;
            
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) return false;
            
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            
            // Check if coordinates are within world bounds
            return y >= world.getMinHeight() && y <= world.getMaxHeight();
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public void cleanupInvalidReinforcements() {
        // Remove reinforcements for blocks that no longer exist or have been replaced
        int removedCount = 0;
        Iterator<Map.Entry<String, Long>> iterator = reinforcedBlocks.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            String blockKey = entry.getKey();
            Location location = getLocationFromKey(blockKey);
            
            if (location == null || location.getWorld() == null) {
                iterator.remove();
                removedCount++;
                continue;
            }
            
            Block block = location.getBlock();
            if (block.getType() == Material.AIR || !isValidBlock(block.getType())) {
                iterator.remove();
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            plugin.getLogger().info("Cleaned up " + removedCount + " invalid reinforced block entries");
        }
    }
    
    public int getReinforcedBlockCount() {
        return reinforcedBlocks.size();
    }
    
    public Set<String> getAllReinforcedBlocks() {
        return reinforcedBlocks.keySet();
    }
    
    // Get location from block key for debugging/admin purposes
    public Location getLocationFromKey(String blockKey) {
        try {
            String[] parts = blockKey.split(":");
            if (parts.length != 4) return null;
            
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) return null;
            
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            
            return new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}