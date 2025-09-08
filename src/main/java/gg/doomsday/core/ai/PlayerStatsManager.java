package gg.doomsday.core.ai;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PlayerStatsManager {
    
    private final JavaPlugin plugin;
    private final File statsDirectory;
    private final ConcurrentHashMap<UUID, PlayerAIStats> statsCache;
    
    public PlayerStatsManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.statsDirectory = new File(plugin.getDataFolder(), "player_stats");
        this.statsCache = new ConcurrentHashMap<>();
        
        if (!statsDirectory.exists()) {
            boolean created = statsDirectory.mkdirs();
            if (created) {
                plugin.getLogger().info("Created player stats directory");
            }
        }
    }
    
    public PlayerAIStats getPlayerStats(UUID playerUUID) {
        return statsCache.computeIfAbsent(playerUUID, uuid -> {
            File statsFile = new File(statsDirectory, uuid.toString() + ".yml");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(statsFile);
            
            PlayerAIStats stats = new PlayerAIStats();
            stats.setTotalRequests(config.getInt("total_requests", 0));
            stats.setRequestsToday(config.getInt("requests_today", 0));
            stats.setLastRequestTime(config.getLong("last_request_time", 0));
            stats.setLastResetDate(config.getString("last_reset_date", ""));
            
            return stats;
        });
    }
    
    public void savePlayerStats(UUID playerUUID, PlayerAIStats stats) {
        try {
            File statsFile = new File(statsDirectory, playerUUID.toString() + ".yml");
            YamlConfiguration config = new YamlConfiguration();
            
            config.set("total_requests", stats.getTotalRequests());
            config.set("requests_today", stats.getRequestsToday());
            config.set("last_request_time", stats.getLastRequestTime());
            config.set("last_reset_date", stats.getLastResetDate());
            
            config.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player stats for " + playerUUID, e);
        }
    }
    
    public void createPlayerStatsFile(UUID playerUUID) {
        File statsFile = new File(statsDirectory, playerUUID.toString() + ".yml");
        if (!statsFile.exists()) {
            try {
                YamlConfiguration config = new YamlConfiguration();
                config.set("total_requests", 0);
                config.set("requests_today", 0);
                config.set("last_request_time", 0L);
                config.set("last_reset_date", "");
                config.save(statsFile);
                
                plugin.getLogger().info("Created stats file for player: " + playerUUID);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create stats file for " + playerUUID, e);
            }
        }
    }
    
    public void clearPlayerMemory(UUID playerUUID) {
        statsCache.remove(playerUUID);
    }
    
    public static class PlayerAIStats {
        private int totalRequests;
        private int requestsToday;
        private long lastRequestTime;
        private String lastResetDate;
        
        public int getTotalRequests() {
            return totalRequests;
        }
        
        public void setTotalRequests(int totalRequests) {
            this.totalRequests = totalRequests;
        }
        
        public int getRequestsToday() {
            return requestsToday;
        }
        
        public void setRequestsToday(int requestsToday) {
            this.requestsToday = requestsToday;
        }
        
        public long getLastRequestTime() {
            return lastRequestTime;
        }
        
        public void setLastRequestTime(long lastRequestTime) {
            this.lastRequestTime = lastRequestTime;
        }
        
        public String getLastResetDate() {
            return lastResetDate;
        }
        
        public void setLastResetDate(String lastResetDate) {
            this.lastResetDate = lastResetDate;
        }
    }
}