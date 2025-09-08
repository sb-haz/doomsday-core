package gg.doomsday.core.data;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.Map;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for managing user data storage and retrieval
 * Provides abstraction for different storage backends (file, database, etc.)
 */
public interface UserDataService {
    
    /**
     * Load user data for a specific player
     * @param playerId The player's UUID
     * @return CompletableFuture containing the user's data configuration
     */
    CompletableFuture<YamlConfiguration> loadUserData(UUID playerId);
    
    /**
     * Save user data for a specific player
     * @param playerId The player's UUID
     * @param data The data to save
     * @return CompletableFuture indicating success/failure
     */
    CompletableFuture<Boolean> saveUserData(UUID playerId, YamlConfiguration data);
    
    /**
     * Check if user data exists for a player
     * @param playerId The player's UUID
     * @return CompletableFuture containing true if data exists
     */
    CompletableFuture<Boolean> hasUserData(UUID playerId);
    
    /**
     * Create new user data file/entry for a player
     * @param playerId The player's UUID
     * @param defaultData Default data to initialize with
     * @return CompletableFuture indicating success/failure
     */
    CompletableFuture<Boolean> createUserData(UUID playerId, YamlConfiguration defaultData);
    
    /**
     * Delete user data for a player
     * @param playerId The player's UUID
     * @return CompletableFuture indicating success/failure
     */
    CompletableFuture<Boolean> deleteUserData(UUID playerId);
    
    /**
     * Get a specific value from user data
     * @param playerId The player's UUID
     * @param path The configuration path (e.g., "stats.kills")
     * @param defaultValue Default value if path doesn't exist
     * @return CompletableFuture containing the value
     */
    <T> CompletableFuture<T> getUserValue(UUID playerId, String path, T defaultValue);
    
    /**
     * Set a specific value in user data
     * @param playerId The player's UUID
     * @param path The configuration path (e.g., "stats.kills")
     * @param value The value to set
     * @return CompletableFuture indicating success/failure
     */
    CompletableFuture<Boolean> setUserValue(UUID playerId, String path, Object value);
    
    /**
     * Increment a numeric value in user data
     * @param playerId The player's UUID
     * @param path The configuration path
     * @param amount The amount to increment by
     * @return CompletableFuture containing the new value
     */
    CompletableFuture<Integer> incrementUserValue(UUID playerId, String path, int amount);
    
    /**
     * Get all users with data
     * @return CompletableFuture containing list of player UUIDs
     */
    CompletableFuture<List<UUID>> getAllUsers();
    
    /**
     * Bulk save operation for multiple users
     * @param userData Map of player UUIDs to their data
     * @return CompletableFuture indicating success/failure
     */
    CompletableFuture<Boolean> bulkSaveUserData(Map<UUID, YamlConfiguration> userData);
    
    /**
     * Initialize the user data service
     * @return CompletableFuture indicating initialization success/failure
     */
    CompletableFuture<Boolean> initialize();
    
    /**
     * Shutdown the user data service and clean up resources
     * @return CompletableFuture indicating shutdown success/failure
     */
    CompletableFuture<Boolean> shutdown();
    
    // Convenience methods for Player objects
    
    /**
     * Load user data for a player
     * @param player The player
     * @return CompletableFuture containing the user's data configuration
     */
    default CompletableFuture<YamlConfiguration> loadUserData(Player player) {
        return loadUserData(player.getUniqueId());
    }
    
    /**
     * Save user data for a player
     * @param player The player
     * @param data The data to save
     * @return CompletableFuture indicating success/failure
     */
    default CompletableFuture<Boolean> saveUserData(Player player, YamlConfiguration data) {
        return saveUserData(player.getUniqueId(), data);
    }
    
    /**
     * Get a specific value from user data
     * @param player The player
     * @param path The configuration path
     * @param defaultValue Default value if path doesn't exist
     * @return CompletableFuture containing the value
     */
    default <T> CompletableFuture<T> getUserValue(Player player, String path, T defaultValue) {
        return getUserValue(player.getUniqueId(), path, defaultValue);
    }
    
    /**
     * Set a specific value in user data
     * @param player The player
     * @param path The configuration path
     * @param value The value to set
     * @return CompletableFuture indicating success/failure
     */
    default CompletableFuture<Boolean> setUserValue(Player player, String path, Object value) {
        return setUserValue(player.getUniqueId(), path, value);
    }
}