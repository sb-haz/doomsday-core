package gg.doomsday.core.nations;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;
import java.util.HashSet;

/**
 * Centralized in-memory cache for player nation assignments.
 * This provides fast O(1) lookups for player nations without file I/O.
 * 
 * Key features:
 * - Fast nation lookups for any player (online/offline)
 * - Automatic synchronization with persistent storage
 * - Thread-safe concurrent access
 * - Efficient bulk operations for nation management
 * 
 * Usage:
 * - Use this cache for all nation lookups instead of direct file access
 * - Cache is automatically updated when players join/leave nations
 * - Provides both individual and bulk lookup methods
 */
public class NationPlayerCache {
    
    private final JavaPlugin plugin;
    
    // Core cache: UUID -> Nation ID
    private final Map<UUID, String> playerNationCache = new ConcurrentHashMap<>();
    
    // Reverse lookup: Nation ID -> Set of Player UUIDs
    private final Map<String, Set<UUID>> nationPlayersCache = new ConcurrentHashMap<>();
    
    // Online player tracking: UUID -> Nation ID (for online players only)
    private final Map<UUID, String> onlinePlayerNations = new ConcurrentHashMap<>();
    
    // Online players by nation: Nation ID -> Set of Online Player UUIDs  
    private final Map<String, Set<UUID>> onlinePlayersByNation = new ConcurrentHashMap<>();
    
    private boolean debug = false;
    
    public NationPlayerCache(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Initialize the cache from persistent storage
     */
    public void initialize(Map<UUID, String> initialData, Set<String> nationIds) {
        // Clear existing cache
        clear();
        
        // Initialize nation sets
        for (String nationId : nationIds) {
            nationPlayersCache.put(nationId, ConcurrentHashMap.newKeySet());
            onlinePlayersByNation.put(nationId, ConcurrentHashMap.newKeySet());
        }
        
        // Load all player data into cache
        for (Map.Entry<UUID, String> entry : initialData.entrySet()) {
            UUID playerId = entry.getKey();
            String nationId = entry.getValue();
            
            if (nationId != null) {
                playerNationCache.put(playerId, nationId);
                nationPlayersCache.computeIfAbsent(nationId, k -> ConcurrentHashMap.newKeySet()).add(playerId);
                
                // Track online players
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    onlinePlayerNations.put(playerId, nationId);
                    onlinePlayersByNation.get(nationId).add(playerId);
                }
            }
        }
        
        if (debug) {
            plugin.getLogger().info("NationPlayerCache initialized with " + playerNationCache.size() + " players across " + nationIds.size() + " nations");
        }
    }
    
    /**
     * Get a player's nation ID (works for online and offline players)
     * @param playerId Player UUID
     * @return Nation ID or null if player has no nation
     */
    public String getPlayerNation(UUID playerId) {
        return playerNationCache.get(playerId);
    }
    
    /**
     * Get a player's nation ID (online players only - faster lookup)
     * @param playerId Player UUID  
     * @return Nation ID or null if player is offline or has no nation
     */
    public String getOnlinePlayerNation(UUID playerId) {
        return onlinePlayerNations.get(playerId);
    }
    
    /**
     * Check if a player is in a nation
     * @param playerId Player UUID
     * @return true if player is in a nation
     */
    public boolean hasPlayerJoinedNation(UUID playerId) {
        return playerNationCache.containsKey(playerId);
    }
    
    /**
     * Get all players in a specific nation (online and offline)
     * @param nationId Nation ID
     * @return Set of player UUIDs (never null, may be empty)
     */
    public Set<UUID> getPlayersInNation(String nationId) {
        return new HashSet<>(nationPlayersCache.getOrDefault(nationId, Collections.emptySet()));
    }
    
    /**
     * Get all online players in a specific nation
     * @param nationId Nation ID
     * @return Set of online player UUIDs (never null, may be empty)
     */
    public Set<UUID> getOnlinePlayersInNation(String nationId) {
        return new HashSet<>(onlinePlayersByNation.getOrDefault(nationId, Collections.emptySet()));
    }
    
    /**
     * Get count of all players in a nation (online and offline)
     * @param nationId Nation ID
     * @return Player count
     */
    public int getPlayerCountInNation(String nationId) {
        return nationPlayersCache.getOrDefault(nationId, Collections.emptySet()).size();
    }
    
    /**
     * Get count of online players in a nation
     * @param nationId Nation ID
     * @return Online player count
     */
    public int getOnlinePlayerCountInNation(String nationId) {
        return onlinePlayersByNation.getOrDefault(nationId, Collections.emptySet()).size();
    }
    
    /**
     * Get online player counts for all nations
     * @return Map of Nation ID -> Online Player Count
     */
    public Map<String, Integer> getOnlinePlayerCountsByNation() {
        Map<String, Integer> counts = new ConcurrentHashMap<>();
        for (Map.Entry<String, Set<UUID>> entry : onlinePlayersByNation.entrySet()) {
            counts.put(entry.getKey(), entry.getValue().size());
        }
        return counts;
    }
    
    /**
     * Get total player counts for all nations (online and offline)
     * @return Map of Nation ID -> Total Player Count
     */
    public Map<String, Integer> getPlayerCountsByNation() {
        Map<String, Integer> counts = new ConcurrentHashMap<>();
        for (Map.Entry<String, Set<UUID>> entry : nationPlayersCache.entrySet()) {
            counts.put(entry.getKey(), entry.getValue().size());
        }
        return counts;
    }
    
    /**
     * Add a player to a nation in the cache
     * @param playerId Player UUID
     * @param nationId Nation ID
     */
    public void addPlayerToNation(UUID playerId, String nationId) {
        // Remove from previous nation if exists
        removePlayerFromNation(playerId);
        
        // Add to new nation
        if (nationId != null) {
            playerNationCache.put(playerId, nationId);
            nationPlayersCache.computeIfAbsent(nationId, k -> ConcurrentHashMap.newKeySet()).add(playerId);
            
            // Update online tracking if player is online
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                onlinePlayerNations.put(playerId, nationId);
                onlinePlayersByNation.computeIfAbsent(nationId, k -> ConcurrentHashMap.newKeySet()).add(playerId);
            }
            
            if (debug) {
                plugin.getLogger().info("Cache: Added player " + playerId + " to nation " + nationId);
            }
        }
    }
    
    /**
     * Remove a player from their nation in the cache
     * @param playerId Player UUID
     */
    public void removePlayerFromNation(UUID playerId) {
        String previousNation = playerNationCache.remove(playerId);
        if (previousNation != null) {
            Set<UUID> nationPlayers = nationPlayersCache.get(previousNation);
            if (nationPlayers != null) {
                nationPlayers.remove(playerId);
            }
            
            // Remove from online tracking
            onlinePlayerNations.remove(playerId);
            Set<UUID> onlineNationPlayers = onlinePlayersByNation.get(previousNation);
            if (onlineNationPlayers != null) {
                onlineNationPlayers.remove(playerId);
            }
            
            if (debug) {
                plugin.getLogger().info("Cache: Removed player " + playerId + " from nation " + previousNation);
            }
        }
    }
    
    /**
     * Update online status when player joins server
     * @param playerId Player UUID
     */
    public void onPlayerJoin(UUID playerId) {
        String nationId = playerNationCache.get(playerId);
        if (nationId != null) {
            onlinePlayerNations.put(playerId, nationId);
            onlinePlayersByNation.computeIfAbsent(nationId, k -> ConcurrentHashMap.newKeySet()).add(playerId);
            
            if (debug) {
                plugin.getLogger().info("Cache: Player " + playerId + " joined - added to online tracking for nation " + nationId);
            }
        }
    }
    
    /**
     * Update online status when player leaves server
     * @param playerId Player UUID
     */
    public void onPlayerQuit(UUID playerId) {
        String nationId = onlinePlayerNations.remove(playerId);
        if (nationId != null) {
            Set<UUID> onlineNationPlayers = onlinePlayersByNation.get(nationId);
            if (onlineNationPlayers != null) {
                onlineNationPlayers.remove(playerId);
            }
            
            if (debug) {
                plugin.getLogger().info("Cache: Player " + playerId + " quit - removed from online tracking");
            }
        }
    }
    
    /**
     * Get all players currently online and in nations
     * @return Set of online player UUIDs in nations
     */
    public Set<UUID> getAllOnlinePlayersInNations() {
        return new HashSet<>(onlinePlayerNations.keySet());
    }
    
    /**
     * Check if a player is online and in a nation
     * @param playerId Player UUID
     * @return true if player is online and in a nation
     */
    public boolean isPlayerOnlineInNation(UUID playerId) {
        return onlinePlayerNations.containsKey(playerId);
    }
    
    /**
     * Add a nation to the cache (when new nations are created)
     * @param nationId Nation ID
     */
    public void addNation(String nationId) {
        nationPlayersCache.putIfAbsent(nationId, ConcurrentHashMap.newKeySet());
        onlinePlayersByNation.putIfAbsent(nationId, ConcurrentHashMap.newKeySet());
        
        if (debug) {
            plugin.getLogger().info("Cache: Added new nation " + nationId);
        }
    }
    
    /**
     * Remove a nation from the cache (when nations are deleted)
     * @param nationId Nation ID
     */
    public void removeNation(String nationId) {
        // Remove all players from this nation
        Set<UUID> playersInNation = nationPlayersCache.remove(nationId);
        if (playersInNation != null) {
            for (UUID playerId : playersInNation) {
                playerNationCache.remove(playerId);
                onlinePlayerNations.remove(playerId);
            }
        }
        onlinePlayersByNation.remove(nationId);
        
        if (debug) {
            plugin.getLogger().info("Cache: Removed nation " + nationId);
        }
    }
    
    /**
     * Clear all cache data
     */
    public void clear() {
        playerNationCache.clear();
        nationPlayersCache.clear();
        onlinePlayerNations.clear();
        onlinePlayersByNation.clear();
    }
    
    /**
     * Get cache statistics for debugging
     * @return String with cache statistics
     */
    public String getCacheStats() {
        return String.format("NationPlayerCache Stats: %d total players, %d online players, %d nations", 
            playerNationCache.size(), onlinePlayerNations.size(), nationPlayersCache.size());
    }
    
    /**
     * Enable/disable debug logging
     * @param debug true to enable debug logging
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    
    /**
     * Check if debug logging is enabled
     * @return true if debug logging is enabled
     */
    public boolean isDebugEnabled() {
        return debug;
    }
}