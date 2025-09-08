package gg.doomsday.core.data;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Manager class that handles user data service lifecycle and provides
 * convenient access methods for the rest of the plugin
 */
public class UserDataManager implements Listener {
    
    private final JavaPlugin plugin;
    private final UserDataService userDataService;
    
    public UserDataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.userDataService = new FileUserDataService(plugin);
        
        // Register event listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Initialize the user data service
     * @return CompletableFuture indicating success/failure
     */
    public CompletableFuture<Boolean> initialize() {
        return userDataService.initialize()
            .thenApply(success -> {
                if (success) {
                    plugin.getLogger().info("UserDataManager initialized successfully");
                } else {
                    plugin.getLogger().severe("Failed to initialize UserDataManager!");
                }
                return success;
            });
    }
    
    /**
     * Shutdown the user data service
     * @return CompletableFuture indicating success/failure
     */
    public CompletableFuture<Boolean> shutdown() {
        return userDataService.shutdown()
            .thenApply(success -> {
                if (success) {
                    plugin.getLogger().info("UserDataManager shut down successfully");
                } else {
                    plugin.getLogger().warning("UserDataManager shutdown had issues");
                }
                return success;
            });
    }
    
    /**
     * Get the underlying UserDataService
     * @return The user data service
     */
    public UserDataService getService() {
        return userDataService;
    }
    
    /**
     * Handle player join - ensure user data exists
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Load or create user data asynchronously
        userDataService.hasUserData(playerId)
            .thenCompose(exists -> {
                if (!exists) {
                    plugin.getLogger().info("Creating user data for new player: " + player.getName());
                    return userDataService.createUserData(playerId, null);
                } else {
                    // Update last seen time
                    return userDataService.setUserValue(playerId, "lastSeen", System.currentTimeMillis());
                }
            })
            .thenCompose(success -> {
                if (success) {
                    // Increment join count
                    return userDataService.incrementUserValue(playerId, "stats.joins", 1);
                } else {
                    return CompletableFuture.completedFuture(0);
                }
            })
            .thenAccept(joinCount -> {
                if (joinCount > 0) {
                    plugin.getLogger().fine("Updated join count for " + player.getName() + ": " + joinCount);
                }
            })
            .exceptionally(throwable -> {
                plugin.getLogger().log(Level.WARNING, "Failed to update user data for " + player.getName(), throwable);
                return null;
            });
    }
    
    /**
     * Handle player quit - update last seen
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Update last seen time
        userDataService.setUserValue(playerId, "lastSeen", System.currentTimeMillis())
            .exceptionally(throwable -> {
                plugin.getLogger().log(Level.WARNING, "Failed to update last seen for " + player.getName(), throwable);
                return false;
            });
    }
    
    // Convenience methods that delegate to the service
    
    /**
     * Get user value with error handling
     */
    public <T> CompletableFuture<T> getUserValue(Player player, String path, T defaultValue) {
        return userDataService.getUserValue(player.getUniqueId(), path, defaultValue)
            .exceptionally(throwable -> {
                plugin.getLogger().log(Level.WARNING, "Failed to get user value " + path + " for " + player.getName(), throwable);
                return defaultValue;
            });
    }
    
    /**
     * Set user value with error handling
     */
    public CompletableFuture<Boolean> setUserValue(Player player, String path, Object value) {
        return userDataService.setUserValue(player.getUniqueId(), path, value)
            .exceptionally(throwable -> {
                plugin.getLogger().log(Level.WARNING, "Failed to set user value " + path + " for " + player.getName(), throwable);
                return false;
            });
    }
    
    /**
     * Increment user value with error handling
     */
    public CompletableFuture<Integer> incrementUserValue(Player player, String path, int amount) {
        return userDataService.incrementUserValue(player.getUniqueId(), path, amount)
            .exceptionally(throwable -> {
                plugin.getLogger().log(Level.WARNING, "Failed to increment user value " + path + " for " + player.getName(), throwable);
                return 0;
            });
    }
    
    /**
     * Load user data with error handling
     */
    public CompletableFuture<YamlConfiguration> loadUserData(Player player) {
        return userDataService.loadUserData(player.getUniqueId())
            .exceptionally(throwable -> {
                plugin.getLogger().log(Level.WARNING, "Failed to load user data for " + player.getName(), throwable);
                return new YamlConfiguration(); // Return empty config as fallback
            });
    }
    
    /**
     * Save user data with error handling
     */
    public CompletableFuture<Boolean> saveUserData(Player player, YamlConfiguration data) {
        return userDataService.saveUserData(player.getUniqueId(), data)
            .exceptionally(throwable -> {
                plugin.getLogger().log(Level.WARNING, "Failed to save user data for " + player.getName(), throwable);
                return false;
            });
    }
}