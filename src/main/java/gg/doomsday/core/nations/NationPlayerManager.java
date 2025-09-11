package gg.doomsday.core.nations;

import gg.doomsday.core.data.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NationPlayerManager implements Listener {
    private final JavaPlugin plugin;
    private final NationManager nationManager;
    private final PlayerDataManager playerDataManager;
    private FileConfiguration playersConfig;
    private File playersFile;
    
    // Centralized in-memory cache for fast nation lookups
    private final NationPlayerCache nationPlayerCache;
    
    // Legacy: Keep old tracking for backward compatibility (deprecated)
    private final Map<String, Set<UUID>> onlinePlayersByNation = new ConcurrentHashMap<>();
    private final Map<UUID, String> onlinePlayerNations = new ConcurrentHashMap<>();
    
    public NationPlayerManager(JavaPlugin plugin, NationManager nationManager, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.playerDataManager = playerDataManager;
        this.nationPlayerCache = new NationPlayerCache(plugin);
        
        loadConfiguration();
        initializeCache();
        initializeOnlinePlayerTracking(); // Legacy support
        
        // Register as event listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    private void loadConfiguration() {
        playersFile = new File(plugin.getDataFolder(), "nation_players.yml");
        
        if (!playersFile.exists()) {
            plugin.saveResource("nation_players.yml", false);
        }
        
        playersConfig = YamlConfiguration.loadConfiguration(playersFile);
        
        InputStream defConfigStream = plugin.getResource("nation_players.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            playersConfig.setDefaults(defConfig);
        }
    }
    
    private void initializeCache() {
        // Load all player data from YAML into cache
        Map<UUID, String> playerData = new HashMap<>();
        Set<String> nationIds = nationManager.getAllNations().keySet();
        
        if (playersConfig.contains("players")) {
            for (String playerIdStr : playersConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(playerIdStr);
                    String nationId = playersConfig.getString("players." + playerIdStr + ".nation");
                    if (nationId != null) {
                        playerData.put(playerId, nationId);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in nation_players.yml: " + playerIdStr);
                }
            }
        }
        
        nationPlayerCache.initialize(playerData, nationIds);
        plugin.getLogger().info("Initialized nation player cache with " + playerData.size() + " players");
    }
    
    public void saveConfiguration() {
        try {
            playersConfig.save(playersFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save nation_players.yml: " + e.getMessage());
        }
    }
    
    public boolean canPlayerSwitch(UUID playerId) {
        if (!playersConfig.getBoolean("settings.allowSwitching", true)) {
            return false;
        }
        
        if (!hasPlayerJoinedNation(playerId)) {
            return true;
        }
        
        long cooldown = playersConfig.getLong("settings.switchCooldown", 86400000);
        if (cooldown <= 0) {
            return true;
        }
        
        long lastJoinTime = playersConfig.getLong("players." + playerId + ".joinDate", 0);
        return System.currentTimeMillis() - lastJoinTime >= cooldown;
    }
    
    public boolean canPlayerJoin(Player player) {
        if (isAdmin(player)) {
            return true;
        }
        return playersConfig.getBoolean("settings.allowPlayerJoining", true);
    }
    
    public boolean canPlayerLeave(Player player) {
        if (isAdmin(player)) {
            return true;
        }
        return playersConfig.getBoolean("settings.allowPlayerLeaving", true);
    }
    
    private boolean isAdmin(Player player) {
        return player.hasPermission("rocket.reload") || player.isOp();
    }
    
    public boolean hasPlayerJoinedNation(UUID playerId) {
        // Use cache for faster lookup
        return nationPlayerCache.hasPlayerJoinedNation(playerId);
    }
    
    public String getPlayerNation(UUID playerId) {
        // Use fast cache lookup instead of file I/O
        return nationPlayerCache.getPlayerNation(playerId);
    }
    
    public long getPlayerJoinDate(UUID playerId) {
        return playersConfig.getLong("players." + playerId + ".joinDate", 0);
    }
    
    public boolean joinNation(Player player, String nationId) {
        return joinNation(player, nationId, false);
    }
    
    public boolean joinNation(Player player, String nationId, boolean adminForce) {
        UUID playerId = player.getUniqueId();
        
        Nation nation = nationManager.getAllNations().get(nationId);
        if (nation == null) {
            return false;
        }
        
        // Check if player can join (unless admin force)
        if (!adminForce && !canPlayerJoin(player)) {
            return false;
        }
        
        String currentNation = getPlayerNation(playerId);
        
        // Check switching permissions (unless admin force)
        if (!adminForce && currentNation != null && !canPlayerSwitch(playerId)) {
            return false;
        }
        
        if (currentNation != null && !currentNation.equals(nationId)) {
            leaveNation(player, false, adminForce);
        }
        
        long currentTime = System.currentTimeMillis();
        playersConfig.set("players." + playerId + ".nation", nationId);
        playersConfig.set("players." + playerId + ".joinDate", currentTime);
        playersConfig.set("players." + playerId + ".canSwitch", canPlayerSwitch(playerId));
        
        updateNationPlayerCount(nationId, 1);
        
        // Update cache
        nationPlayerCache.addPlayerToNation(playerId, nationId);
        
        // Update PlayerDataManager immediately
        playerDataManager.setPlayerNation(playerId, nationId);
        
        // Update legacy online tracking if player is online
        if (player.isOnline()) {
            addPlayerToOnlineTracking(playerId, nationId);
        }
        
        saveConfiguration();
        
        plugin.getLogger().info("Player " + player.getName() + " joined nation: " + nationId);
        return true;
    }
    
    public boolean leaveNation(Player player, boolean saveConfig) {
        return leaveNation(player, saveConfig, false);
    }
    
    public boolean leaveNation(Player player, boolean saveConfig, boolean adminForce) {
        UUID playerId = player.getUniqueId();
        String currentNation = getPlayerNation(playerId);
        
        if (currentNation == null) {
            return false;
        }
        
        // Check if player can leave (unless admin force)
        if (!adminForce && !canPlayerLeave(player)) {
            return false;
        }
        
        // Check switching permissions (unless admin force) 
        if (!adminForce && !canPlayerSwitch(playerId)) {
            return false;
        }
        
        playersConfig.set("players." + playerId, null);
        
        updateNationPlayerCount(currentNation, -1);
        
        // Update cache
        nationPlayerCache.removePlayerFromNation(playerId);
        
        // Update PlayerDataManager immediately (clear nation)
        playerDataManager.setPlayerNation(playerId, "");
        
        // Update legacy online tracking if player is online
        if (player.isOnline()) {
            removePlayerFromOnlineTracking(playerId);
        }
        
        if (saveConfig) {
            saveConfiguration();
        }
        
        plugin.getLogger().info("Player " + player.getName() + " left nation: " + currentNation);
        return true;
    }
    
    private void updateNationPlayerCount(String nationId, int change) {
        Nation nation = nationManager.getAllNations().get(nationId);
        if (nation != null) {
            int currentCount = nation.getTotalPlayers();
            nation.setTotalPlayers(Math.max(0, currentCount + change));
            nationManager.saveNationPlayerCount(nationId, nation.getTotalPlayers());
        }
    }
    
    public Map<String, Integer> getNationPlayerCounts() {
        Map<String, Integer> counts = new HashMap<>();
        
        for (String nationId : nationManager.getAllNations().keySet()) {
            counts.put(nationId, 0);
        }
        
        if (playersConfig.contains("players")) {
            for (String playerId : playersConfig.getConfigurationSection("players").getKeys(false)) {
                String nation = playersConfig.getString("players." + playerId + ".nation");
                if (nation != null && counts.containsKey(nation)) {
                    counts.put(nation, counts.get(nation) + 1);
                }
            }
        }
        
        return counts;
    }
    
    public void recalculateNationPlayerCounts() {
        Map<String, Integer> counts = getNationPlayerCounts();
        
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            Nation nation = nationManager.getAllNations().get(entry.getKey());
            if (nation != null) {
                nation.setTotalPlayers(entry.getValue());
                nationManager.saveNationPlayerCount(entry.getKey(), entry.getValue());
            }
        }
        
        plugin.getLogger().info("Recalculated nation player counts: " + counts);
    }
    
    public boolean isAllowSwitching() {
        return playersConfig.getBoolean("settings.allowSwitching", true);
    }
    
    public long getSwitchCooldown() {
        return playersConfig.getLong("settings.switchCooldown", 86400000);
    }
    
    public void reload() {
        loadConfiguration();
        initializeCache(); // Reload cache from updated config
        recalculateNationPlayerCounts();
        initializeOnlinePlayerTracking();
    }
    
    /**
     * Initialize the in-memory tracking by scanning all currently online players
     */
    private void initializeOnlinePlayerTracking() {
        // Clear existing tracking
        onlinePlayersByNation.clear();
        onlinePlayerNations.clear();
        
        // Initialize nation sets
        for (String nationId : nationManager.getAllNations().keySet()) {
            onlinePlayersByNation.put(nationId, ConcurrentHashMap.newKeySet());
        }
        
        // Scan all online players and add to tracking
        for (Player player : Bukkit.getOnlinePlayers()) {
            String nationId = getPlayerNation(player.getUniqueId());
            if (nationId != null) {
                addPlayerToOnlineTracking(player.getUniqueId(), nationId);
            }
        }
        
        plugin.getLogger().info("Initialized online player tracking. Nations: " + getOnlinePlayerCountsByNation());
    }
    
    /**
     * Add a player to online tracking
     */
    private void addPlayerToOnlineTracking(UUID playerId, String nationId) {
        // Remove from previous nation if exists
        String previousNation = onlinePlayerNations.remove(playerId);
        if (previousNation != null) {
            Set<UUID> previousSet = onlinePlayersByNation.get(previousNation);
            if (previousSet != null) {
                previousSet.remove(playerId);
            }
        }
        
        // Add to new nation
        if (nationId != null && onlinePlayersByNation.containsKey(nationId)) {
            onlinePlayersByNation.get(nationId).add(playerId);
            onlinePlayerNations.put(playerId, nationId);
        }
    }
    
    /**
     * Remove a player from online tracking
     */
    private void removePlayerFromOnlineTracking(UUID playerId) {
        String nationId = onlinePlayerNations.remove(playerId);
        if (nationId != null) {
            Set<UUID> nationSet = onlinePlayersByNation.get(nationId);
            if (nationSet != null) {
                nationSet.remove(playerId);
            }
        }
    }
    
    /**
     * Get all online players in a specific nation
     */
    public Set<UUID> getOnlinePlayersInNation(String nationId) {
        return nationPlayerCache.getOnlinePlayersInNation(nationId);
    }
    
    /**
     * Get count of online players in a specific nation
     */
    public int getOnlinePlayerCountInNation(String nationId) {
        return nationPlayerCache.getOnlinePlayerCountInNation(nationId);
    }
    
    /**
     * Get online player counts for all nations
     */
    public Map<String, Integer> getOnlinePlayerCountsByNation() {
        return nationPlayerCache.getOnlinePlayerCountsByNation();
    }
    
    /**
     * Get all online players in all nations (for broadcasting, etc.)
     */
    public Set<UUID> getAllOnlinePlayersInNations() {
        return nationPlayerCache.getAllOnlinePlayersInNations();
    }
    
    /**
     * Check if a player is online and in a nation
     */
    public boolean isPlayerOnlineInNation(UUID playerId) {
        return nationPlayerCache.isPlayerOnlineInNation(playerId);
    }
    
    /**
     * Get the nation of an online player (faster than file lookup)
     */
    public String getOnlinePlayerNation(UUID playerId) {
        return nationPlayerCache.getOnlinePlayerNation(playerId);
    }
    
    /**
     * Admin method to force a player to join a nation (bypasses restrictions)
     */
    public boolean adminSetPlayerNation(Player target, String nationId) {
        if (nationId == null || nationId.equalsIgnoreCase("none")) {
            return leaveNation(target, true, true);
        } else {
            return joinNation(target, nationId, true);
        }
    }
    
    /**
     * Admin method to force a player to leave their nation (bypasses restrictions)
     */
    public boolean adminRemovePlayerFromNation(Player target) {
        return leaveNation(target, true, true);
    }
    
    /**
     * Set global join/leave permissions
     */
    public void setAllowPlayerJoining(boolean allow) {
        playersConfig.set("settings.allowPlayerJoining", allow);
        saveConfiguration();
    }
    
    public void setAllowPlayerLeaving(boolean allow) {
        playersConfig.set("settings.allowPlayerLeaving", allow);
        saveConfiguration();
    }
    
    public boolean getAllowPlayerJoining() {
        return playersConfig.getBoolean("settings.allowPlayerJoining", true);
    }
    
    public boolean getAllowPlayerLeaving() {
        return playersConfig.getBoolean("settings.allowPlayerLeaving", true);
    }
    
    // Event handlers to maintain online tracking
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Update cache with online status
        nationPlayerCache.onPlayerJoin(playerId);
        
        // Legacy tracking
        String nationId = getPlayerNation(playerId);
        if (nationId != null) {
            addPlayerToOnlineTracking(playerId, nationId);
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        
        // Update cache with offline status
        nationPlayerCache.onPlayerQuit(playerId);
        
        // Legacy tracking
        removePlayerFromOnlineTracking(playerId);
    }
    
    /**
     * Get the centralized nation player cache for advanced operations
     * @return NationPlayerCache instance
     */
    public NationPlayerCache getNationPlayerCache() {
        return nationPlayerCache;
    }
    
    /**
     * Get all players in a nation (online and offline) - uses cache
     * @param nationId Nation ID
     * @return Set of player UUIDs
     */
    public Set<UUID> getAllPlayersInNation(String nationId) {
        return nationPlayerCache.getPlayersInNation(nationId);
    }
    
    /**
     * Get total player count in a nation (online and offline) - uses cache
     * @param nationId Nation ID
     * @return Player count
     */
    public int getTotalPlayerCountInNation(String nationId) {
        return nationPlayerCache.getPlayerCountInNation(nationId);
    }
    
    /**
     * Get cache statistics for debugging
     * @return Cache statistics string
     */
    public String getCacheStats() {
        return nationPlayerCache.getCacheStats();
    }
}