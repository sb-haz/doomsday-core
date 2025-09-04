package gg.doomsday.core.nations;

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
    private FileConfiguration playersConfig;
    private File playersFile;
    
    // In-memory tracking of online players by nation
    private final Map<String, Set<UUID>> onlinePlayersByNation = new ConcurrentHashMap<>();
    private final Map<UUID, String> onlinePlayerNations = new ConcurrentHashMap<>();
    
    public NationPlayerManager(JavaPlugin plugin, NationManager nationManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        loadConfiguration();
        initializeOnlinePlayerTracking();
        
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
    
    public boolean hasPlayerJoinedNation(UUID playerId) {
        return playersConfig.contains("players." + playerId);
    }
    
    public String getPlayerNation(UUID playerId) {
        return playersConfig.getString("players." + playerId + ".nation");
    }
    
    public long getPlayerJoinDate(UUID playerId) {
        return playersConfig.getLong("players." + playerId + ".joinDate", 0);
    }
    
    public boolean joinNation(Player player, String nationId) {
        UUID playerId = player.getUniqueId();
        
        Nation nation = nationManager.getAllNations().get(nationId);
        if (nation == null) {
            return false;
        }
        
        String currentNation = getPlayerNation(playerId);
        
        if (currentNation != null && !canPlayerSwitch(playerId)) {
            return false;
        }
        
        if (currentNation != null && !currentNation.equals(nationId)) {
            leaveNation(player, false);
        }
        
        long currentTime = System.currentTimeMillis();
        playersConfig.set("players." + playerId + ".nation", nationId);
        playersConfig.set("players." + playerId + ".joinDate", currentTime);
        playersConfig.set("players." + playerId + ".canSwitch", canPlayerSwitch(playerId));
        
        updateNationPlayerCount(nationId, 1);
        
        // Update online tracking if player is online
        if (player.isOnline()) {
            addPlayerToOnlineTracking(playerId, nationId);
        }
        
        saveConfiguration();
        
        plugin.getLogger().info("Player " + player.getName() + " joined nation: " + nationId);
        return true;
    }
    
    public boolean leaveNation(Player player, boolean saveConfig) {
        UUID playerId = player.getUniqueId();
        String currentNation = getPlayerNation(playerId);
        
        if (currentNation == null) {
            return false;
        }
        
        if (!canPlayerSwitch(playerId)) {
            return false;
        }
        
        playersConfig.set("players." + playerId, null);
        
        updateNationPlayerCount(currentNation, -1);
        
        // Update online tracking if player is online
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
        return new HashSet<>(onlinePlayersByNation.getOrDefault(nationId, Collections.emptySet()));
    }
    
    /**
     * Get count of online players in a specific nation
     */
    public int getOnlinePlayerCountInNation(String nationId) {
        return onlinePlayersByNation.getOrDefault(nationId, Collections.emptySet()).size();
    }
    
    /**
     * Get online player counts for all nations
     */
    public Map<String, Integer> getOnlinePlayerCountsByNation() {
        Map<String, Integer> counts = new HashMap<>();
        for (Map.Entry<String, Set<UUID>> entry : onlinePlayersByNation.entrySet()) {
            counts.put(entry.getKey(), entry.getValue().size());
        }
        return counts;
    }
    
    /**
     * Get all online players in all nations (for broadcasting, etc.)
     */
    public Set<UUID> getAllOnlinePlayersInNations() {
        return new HashSet<>(onlinePlayerNations.keySet());
    }
    
    /**
     * Check if a player is online and in a nation
     */
    public boolean isPlayerOnlineInNation(UUID playerId) {
        return onlinePlayerNations.containsKey(playerId);
    }
    
    /**
     * Get the nation of an online player (faster than file lookup)
     */
    public String getOnlinePlayerNation(UUID playerId) {
        return onlinePlayerNations.get(playerId);
    }
    
    // Event handlers to maintain online tracking
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String nationId = getPlayerNation(player.getUniqueId());
        if (nationId != null) {
            addPlayerToOnlineTracking(player.getUniqueId(), nationId);
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removePlayerFromOnlineTracking(event.getPlayer().getUniqueId());
    }
}