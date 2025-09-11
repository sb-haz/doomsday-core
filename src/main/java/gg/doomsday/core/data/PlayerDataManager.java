package gg.doomsday.core.data;

import gg.doomsday.core.nations.NationRole;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PlayerDataManager {
    
    private final JavaPlugin plugin;
    private final File dataDirectory;
    private final ConcurrentHashMap<UUID, PlayerData> dataCache;
    
    public PlayerDataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataDirectory = new File(plugin.getDataFolder(), "player_data");
        this.dataCache = new ConcurrentHashMap<>();
        
        if (!dataDirectory.exists()) {
            boolean created = dataDirectory.mkdirs();
            if (created) {
                plugin.getLogger().info("Created player data directory");
            }
        }
        
        // Migrate existing player_stats files if they exist
        migratePlayerStats();
    }
    
    private void migratePlayerStats() {
        File oldStatsDirectory = new File(plugin.getDataFolder(), "player_stats");
        if (oldStatsDirectory.exists() && oldStatsDirectory.isDirectory()) {
            plugin.getLogger().info("Migrating player_stats to player_data...");
            
            File[] oldFiles = oldStatsDirectory.listFiles((dir, name) -> name.endsWith(".yml"));
            if (oldFiles != null) {
                for (File oldFile : oldFiles) {
                    try {
                        String fileName = oldFile.getName();
                        String uuidString = fileName.substring(0, fileName.length() - 4);
                        UUID playerUUID = UUID.fromString(uuidString);
                        
                        // Load old data
                        YamlConfiguration oldConfig = YamlConfiguration.loadConfiguration(oldFile);
                        
                        // Create new data file
                        File newDataFile = new File(dataDirectory, fileName);
                        YamlConfiguration newConfig = new YamlConfiguration();
                        
                        // Migrate AI stats to new format
                        newConfig.set("ai.total_requests", oldConfig.getInt("total_requests", 0));
                        newConfig.set("ai.requests_today", oldConfig.getInt("requests_today", 0));
                        newConfig.set("ai.last_request_time", oldConfig.getLong("last_request_time", 0));
                        newConfig.set("ai.last_reset_date", oldConfig.getString("last_reset_date", ""));
                        
                        // Add new sections
                        newConfig.set("profile.last_login", System.currentTimeMillis());
                        newConfig.set("profile.last_known_username", "Unknown");
                        newConfig.set("nation.current_nation", "");
                        newConfig.set("roles.current_role", "CITIZEN");
                        newConfig.set("roles.role_assignment_time", 0L);
                        newConfig.set("roles.assigned_by", "");
                        
                        newConfig.save(newDataFile);
                        plugin.getLogger().info("Migrated data for player: " + playerUUID);
                        
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to migrate data file: " + oldFile.getName() + " - " + e.getMessage());
                    }
                }
            }
            
            plugin.getLogger().info("Player stats migration completed");
        }
    }
    
    public PlayerData getPlayerData(UUID playerUUID) {
        return dataCache.computeIfAbsent(playerUUID, uuid -> {
            File dataFile = new File(dataDirectory, uuid.toString() + ".yml");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
            
            PlayerData data = new PlayerData();
            
            // Load AI stats
            data.setTotalRequests(config.getInt("ai.total_requests", 0));
            data.setRequestsToday(config.getInt("ai.requests_today", 0));
            data.setLastRequestTime(config.getLong("ai.last_request_time", 0));
            data.setLastResetDate(config.getString("ai.last_reset_date", ""));
            
            // Load profile data
            data.setLastLogin(config.getLong("profile.last_login", System.currentTimeMillis()));
            data.setLastKnownUsername(config.getString("profile.last_known_username", "Unknown"));
            
            // Load nation data
            data.setCurrentNation(config.getString("nation.current_nation", ""));
            
            // Load role data
            String roleString = config.getString("roles.current_role", "CITIZEN");
            data.setCurrentRole(NationRole.fromString(roleString));
            data.setRoleAssignmentTime(config.getLong("roles.role_assignment_time", 0L));
            data.setAssignedBy(config.getString("roles.assigned_by", ""));
            
            return data;
        });
    }
    
    public void savePlayerData(UUID playerUUID, PlayerData data) {
        try {
            File dataFile = new File(dataDirectory, playerUUID.toString() + ".yml");
            YamlConfiguration config = new YamlConfiguration();
            
            // Save AI stats
            config.set("ai.total_requests", data.getTotalRequests());
            config.set("ai.requests_today", data.getRequestsToday());
            config.set("ai.last_request_time", data.getLastRequestTime());
            config.set("ai.last_reset_date", data.getLastResetDate());
            
            // Save profile data
            config.set("profile.last_login", data.getLastLogin());
            config.set("profile.last_known_username", data.getLastKnownUsername());
            
            // Save nation data
            config.set("nation.current_nation", data.getCurrentNation());
            
            // Save role data
            config.set("roles.current_role", data.getCurrentRole().name());
            config.set("roles.role_assignment_time", data.getRoleAssignmentTime());
            config.set("roles.assigned_by", data.getAssignedBy());
            
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player data for " + playerUUID, e);
        }
    }
    
    public void createPlayerDataFile(UUID playerUUID, String username) {
        File dataFile = new File(dataDirectory, playerUUID.toString() + ".yml");
        if (!dataFile.exists()) {
            try {
                YamlConfiguration config = new YamlConfiguration();
                
                // Initialize AI stats
                config.set("ai.total_requests", 0);
                config.set("ai.requests_today", 0);
                config.set("ai.last_request_time", 0L);
                config.set("ai.last_reset_date", "");
                
                // Initialize profile data
                config.set("profile.last_login", System.currentTimeMillis());
                config.set("profile.last_known_username", username);
                
                // Initialize nation data
                config.set("nation.current_nation", "");
                
                // Initialize role data
                config.set("roles.current_role", "CITIZEN");
                config.set("roles.role_assignment_time", 0L);
                config.set("roles.assigned_by", "");
                
                config.save(dataFile);
                plugin.getLogger().info("Created data file for player: " + username + " (" + playerUUID + ")");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create data file for " + playerUUID, e);
            }
        }
    }
    
    public void updatePlayerLogin(UUID playerUUID, String username) {
        PlayerData data = getPlayerData(playerUUID);
        data.setLastLogin(System.currentTimeMillis());
        data.setLastKnownUsername(username);
        savePlayerData(playerUUID, data);
    }
    
    public void assignPlayerRole(UUID playerUUID, NationRole role, String assignedBy) {
        PlayerData data = getPlayerData(playerUUID);
        data.setCurrentRole(role);
        data.setRoleAssignmentTime(System.currentTimeMillis());
        data.setAssignedBy(assignedBy);
        savePlayerData(playerUUID, data);
        
        plugin.getLogger().info("Assigned role " + role.name() + " to player " + playerUUID + " by " + assignedBy);
    }
    
    public void removePlayerRole(UUID playerUUID, String removedBy) {
        PlayerData data = getPlayerData(playerUUID);
        data.setCurrentRole(NationRole.CITIZEN);
        data.setRoleAssignmentTime(System.currentTimeMillis());
        data.setAssignedBy(removedBy + " (removed)");
        savePlayerData(playerUUID, data);
        
        plugin.getLogger().info("Removed role from player " + playerUUID + " by " + removedBy);
    }
    
    public void setPlayerNation(UUID playerUUID, String nationId) {
        PlayerData data = getPlayerData(playerUUID);
        data.setCurrentNation(nationId != null ? nationId : "");
        savePlayerData(playerUUID, data);
    }
    
    public void clearPlayerMemory(UUID playerUUID) {
        dataCache.remove(playerUUID);
    }
    
    public static class PlayerData {
        // AI stats (migrated from PlayerAIStats)
        private int totalRequests;
        private int requestsToday;
        private long lastRequestTime;
        private String lastResetDate;
        
        // Profile data
        private long lastLogin;
        private String lastKnownUsername;
        
        // Nation data
        private String currentNation;
        
        // Role data
        private NationRole currentRole;
        private long roleAssignmentTime;
        private String assignedBy;
        
        public PlayerData() {
            this.totalRequests = 0;
            this.requestsToday = 0;
            this.lastRequestTime = 0;
            this.lastResetDate = "";
            this.lastLogin = System.currentTimeMillis();
            this.lastKnownUsername = "Unknown";
            this.currentNation = "";
            this.currentRole = NationRole.CITIZEN;
            this.roleAssignmentTime = 0;
            this.assignedBy = "";
        }
        
        // AI stats getters/setters
        public int getTotalRequests() { return totalRequests; }
        public void setTotalRequests(int totalRequests) { this.totalRequests = totalRequests; }
        
        public int getRequestsToday() { return requestsToday; }
        public void setRequestsToday(int requestsToday) { this.requestsToday = requestsToday; }
        
        public long getLastRequestTime() { return lastRequestTime; }
        public void setLastRequestTime(long lastRequestTime) { this.lastRequestTime = lastRequestTime; }
        
        public String getLastResetDate() { return lastResetDate; }
        public void setLastResetDate(String lastResetDate) { this.lastResetDate = lastResetDate; }
        
        // Profile getters/setters
        public long getLastLogin() { return lastLogin; }
        public void setLastLogin(long lastLogin) { this.lastLogin = lastLogin; }
        
        public String getLastKnownUsername() { return lastKnownUsername; }
        public void setLastKnownUsername(String lastKnownUsername) { this.lastKnownUsername = lastKnownUsername; }
        
        // Nation getters/setters
        public String getCurrentNation() { return currentNation; }
        public void setCurrentNation(String currentNation) { this.currentNation = currentNation; }
        
        // Role getters/setters
        public NationRole getCurrentRole() { return currentRole; }
        public void setCurrentRole(NationRole currentRole) { this.currentRole = currentRole; }
        
        public long getRoleAssignmentTime() { return roleAssignmentTime; }
        public void setRoleAssignmentTime(long roleAssignmentTime) { this.roleAssignmentTime = roleAssignmentTime; }
        
        public String getAssignedBy() { return assignedBy; }
        public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }
        
        public String getFormattedLastLogin() {
            return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(lastLogin), 
                    java.time.ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        
        public String getFormattedRoleAssignmentTime() {
            if (roleAssignmentTime == 0) return "Never";
            return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(roleAssignmentTime), 
                    java.time.ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
    }
}