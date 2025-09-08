package gg.doomsday.core.data;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * File-based implementation of UserDataService
 * Stores user data in YAML files in the users/ directory
 */
public class FileUserDataService implements UserDataService {
    
    private final JavaPlugin plugin;
    private final File usersDirectory;
    private final Map<UUID, YamlConfiguration> cache = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;
    
    public FileUserDataService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.usersDirectory = new File(plugin.getDataFolder(), "users");
    }
    
    @Override
    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!usersDirectory.exists()) {
                    boolean created = usersDirectory.mkdirs();
                    if (!created) {
                        plugin.getLogger().severe("Failed to create users directory!");
                        return false;
                    }
                    plugin.getLogger().info("Created users directory at: " + usersDirectory.getAbsolutePath());
                }
                
                initialized = true;
                plugin.getLogger().info("FileUserDataService initialized successfully");
                return true;
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to initialize FileUserDataService", e);
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Save all cached data before shutdown
                for (Map.Entry<UUID, YamlConfiguration> entry : cache.entrySet()) {
                    saveUserDataSync(entry.getKey(), entry.getValue());
                }
                cache.clear();
                initialized = false;
                plugin.getLogger().info("FileUserDataService shutdown successfully");
                return true;
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error during FileUserDataService shutdown", e);
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<YamlConfiguration> loadUserData(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            if (!initialized) {
                throw new IllegalStateException("UserDataService not initialized");
            }
            
            // Check cache first
            YamlConfiguration cached = cache.get(playerId);
            if (cached != null) {
                return cached;
            }
            
            File userFile = getUserFile(playerId);
            YamlConfiguration config;
            
            if (userFile.exists()) {
                config = YamlConfiguration.loadConfiguration(userFile);
            } else {
                // Create default user data
                config = createDefaultUserData(playerId);
                saveUserDataSync(playerId, config);
            }
            
            // Cache the loaded data
            cache.put(playerId, config);
            return config;
        });
    }
    
    @Override
    public CompletableFuture<Boolean> saveUserData(UUID playerId, YamlConfiguration data) {
        return CompletableFuture.supplyAsync(() -> {
            if (!initialized) {
                plugin.getLogger().warning("Attempted to save user data before initialization");
                return false;
            }
            
            boolean success = saveUserDataSync(playerId, data);
            if (success) {
                // Update cache
                cache.put(playerId, data);
            }
            return success;
        });
    }
    
    @Override
    public CompletableFuture<Boolean> hasUserData(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            if (!initialized) return false;
            
            // Check cache first
            if (cache.containsKey(playerId)) {
                return true;
            }
            
            // Check file system
            return getUserFile(playerId).exists();
        });
    }
    
    @Override
    public CompletableFuture<Boolean> createUserData(UUID playerId, YamlConfiguration defaultData) {
        return CompletableFuture.supplyAsync(() -> {
            if (!initialized) return false;
            
            File userFile = getUserFile(playerId);
            if (userFile.exists()) {
                return true; // Already exists
            }
            
            try {
                boolean created = userFile.createNewFile();
                if (!created) return false;
                
                YamlConfiguration config = defaultData != null ? defaultData : createDefaultUserData(playerId);
                boolean saved = saveUserDataSync(playerId, config);
                
                if (saved) {
                    cache.put(playerId, config);
                }
                
                return saved;
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to create user data file for " + playerId, e);
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> deleteUserData(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            if (!initialized) return false;
            
            // Remove from cache
            cache.remove(playerId);
            
            File userFile = getUserFile(playerId);
            if (!userFile.exists()) {
                return true; // Already doesn't exist
            }
            
            boolean deleted = userFile.delete();
            if (!deleted) {
                plugin.getLogger().warning("Failed to delete user data file for " + playerId);
            }
            return deleted;
        });
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> getUserValue(UUID playerId, String path, T defaultValue) {
        return loadUserData(playerId).thenApply(config -> {
            if (config.contains(path)) {
                Object value = config.get(path);
                try {
                    return (T) value;
                } catch (ClassCastException e) {
                    plugin.getLogger().warning("Type mismatch for path " + path + " in user " + playerId + ", returning default");
                    return defaultValue;
                }
            }
            return defaultValue;
        });
    }
    
    @Override
    public CompletableFuture<Boolean> setUserValue(UUID playerId, String path, Object value) {
        return loadUserData(playerId).thenCompose(config -> {
            config.set(path, value);
            return saveUserData(playerId, config);
        });
    }
    
    @Override
    public CompletableFuture<Integer> incrementUserValue(UUID playerId, String path, int amount) {
        return loadUserData(playerId).thenCompose(config -> {
            int currentValue = config.getInt(path, 0);
            int newValue = currentValue + amount;
            config.set(path, newValue);
            
            return saveUserData(playerId, config).thenApply(success -> {
                if (success) {
                    return newValue;
                } else {
                    return currentValue; // Return old value if save failed
                }
            });
        });
    }
    
    @Override
    public CompletableFuture<List<UUID>> getAllUsers() {
        return CompletableFuture.supplyAsync(() -> {
            if (!initialized) return new ArrayList<>();
            
            List<UUID> users = new ArrayList<>();
            File[] files = usersDirectory.listFiles((dir, name) -> name.endsWith(".yml"));
            
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    String uuidString = fileName.substring(0, fileName.length() - 4); // Remove .yml
                    
                    try {
                        UUID uuid = UUID.fromString(uuidString);
                        users.add(uuid);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in filename: " + fileName);
                    }
                }
            }
            
            return users;
        });
    }
    
    @Override
    public CompletableFuture<Boolean> bulkSaveUserData(Map<UUID, YamlConfiguration> userData) {
        return CompletableFuture.supplyAsync(() -> {
            if (!initialized) return false;
            
            boolean allSuccessful = true;
            for (Map.Entry<UUID, YamlConfiguration> entry : userData.entrySet()) {
                boolean saved = saveUserDataSync(entry.getKey(), entry.getValue());
                if (saved) {
                    cache.put(entry.getKey(), entry.getValue());
                } else {
                    allSuccessful = false;
                }
            }
            
            return allSuccessful;
        });
    }
    
    // Helper methods
    
    /**
     * Get the file for a user's data
     * @param playerId The player's UUID
     * @return The user's data file
     */
    private File getUserFile(UUID playerId) {
        return new File(usersDirectory, playerId.toString() + ".yml");
    }
    
    /**
     * Synchronously save user data to file
     * @param playerId The player's UUID
     * @param data The data to save
     * @return true if successful
     */
    private boolean saveUserDataSync(UUID playerId, YamlConfiguration data) {
        try {
            File userFile = getUserFile(playerId);
            
            // Ensure parent directory exists
            if (!userFile.getParentFile().exists()) {
                userFile.getParentFile().mkdirs();
            }
            
            data.save(userFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save user data for " + playerId, e);
            return false;
        }
    }
    
    /**
     * Create default user data configuration
     * @param playerId The player's UUID
     * @return Default user data
     */
    private YamlConfiguration createDefaultUserData(UUID playerId) {
        YamlConfiguration config = new YamlConfiguration();
        
        // Set basic information
        config.set("uuid", playerId.toString());
        config.set("created", System.currentTimeMillis());
        config.set("lastSeen", System.currentTimeMillis());
        
        // Set default stats
        config.set("stats.playtime", 0L);
        config.set("stats.joins", 0);
        config.set("stats.kills", 0);
        config.set("stats.deaths", 0);
        config.set("stats.missilesLaunched", 0);
        config.set("stats.blocksDestroyed", 0);
        
        // Set default preferences
        config.set("preferences.receiveNotifications", true);
        config.set("preferences.showCoordinates", true);
        
        // Initialize empty sections that plugins might use
        config.createSection("nations");
        config.createSection("inventory");
        config.createSection("achievements");
        config.createSection("customData");
        
        return config;
    }
    
    /**
     * Get the users directory
     * @return The users directory
     */
    public File getUsersDirectory() {
        return usersDirectory;
    }
    
    /**
     * Clear the cache (useful for testing)
     */
    public void clearCache() {
        cache.clear();
    }
    
    /**
     * Get cache size (useful for monitoring)
     * @return Number of cached user data entries
     */
    public int getCacheSize() {
        return cache.size();
    }
}