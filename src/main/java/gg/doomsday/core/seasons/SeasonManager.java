package gg.doomsday.core.seasons;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Manages the current season and handles season operations
 */
public class SeasonManager {
    
    private final JavaPlugin plugin;
    private Season currentSeason;
    private FileConfiguration seasonsConfig;
    private File seasonsFile;
    private SeasonEventListener eventListener;
    
    public SeasonManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfiguration();
    }
    
    public void loadConfiguration() {
        seasonsFile = new File(plugin.getDataFolder(), "seasons.yml");
        
        // Create default seasons.yml if it doesn't exist
        if (!seasonsFile.exists()) {
            plugin.saveResource("seasons.yml", false);
        }
        
        seasonsConfig = YamlConfiguration.loadConfiguration(seasonsFile);
        
        // Load default configuration from resource if file is empty
        InputStream defConfigStream = plugin.getResource("seasons.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            seasonsConfig.setDefaults(defConfig);
        }
        
        loadCurrentSeason();
    }
    
    private void loadCurrentSeason() {
        List<?> seasonsList = seasonsConfig.getList("seasons");
        
        if (seasonsList == null || seasonsList.isEmpty()) {
            plugin.getLogger().info("No seasons configured");
            currentSeason = null;
            return;
        }
        
        // Since we only store one season, get the first (and only) entry
        Object firstSeason = seasonsList.get(0);
        
        // Handle both ConfigurationSection (when loaded from memory) and Map (when loaded from YAML file)
        ConfigurationSection seasonSection = null;
        if (firstSeason instanceof ConfigurationSection) {
            seasonSection = (ConfigurationSection) firstSeason;
        } else if (firstSeason instanceof java.util.Map) {
            // Convert Map to temporary ConfigurationSection for consistent access
            seasonSection = seasonsConfig.createSection("temp_season", (java.util.Map<?, ?>) firstSeason);
        } else {
            plugin.getLogger().warning("Invalid season configuration format: " + firstSeason.getClass());
            currentSeason = null;
            return;
        }
        
        try {
            int id = seasonSection.getInt("id");
            String displayName = seasonSection.getString("displayName", "Season " + id);
            String statusStr = seasonSection.getString("status", "planned");
            String startAtStr = seasonSection.getString("startAt");
            String endAtStr = seasonSection.getString("endAt");
            
            Season.Status status = Season.Status.valueOf(statusStr.toUpperCase());
            
            Instant startAt = startAtStr != null ? Instant.parse(startAtStr) : null;
            Instant endAt = endAtStr != null ? Instant.parse(endAtStr) : null;
            
            currentSeason = new Season(id, displayName, status, startAt, endAt);
            plugin.getLogger().info("Loaded season: " + currentSeason);
            
        } catch (DateTimeParseException | IllegalArgumentException e) {
            plugin.getLogger().severe("Failed to parse season configuration: " + e.getMessage());
            currentSeason = null;
        }
    }
    
    /**
     * Validates the current active season on plugin startup
     * @throws IllegalStateException if active season is invalid
     */
    public void validateActiveSeason() throws IllegalStateException {
        if (currentSeason == null) {
            plugin.getLogger().info("No season configured - plugin will start normally");
            return;
        }
        
        if (currentSeason.isActive()) {
            if (!currentSeason.isValidActive()) {
                throw new IllegalStateException(
                    "Active season is invalid! Season " + currentSeason.getId() + 
                    " has ended or missing endAt. Current time: " + Instant.now() + 
                    ", Season endAt: " + currentSeason.getEndAtFormatted()
                );
            }
            plugin.getLogger().info("Active season validated: Season " + currentSeason.getId());
        } else if (currentSeason.isArchived()) {
            plugin.getLogger().info("Current season is archived - no active season");
        } else {
            plugin.getLogger().info("Current season is planned - not yet active");
        }
    }
    
    /**
     * Creates a new season (replaces current one)
     */
    public boolean createSeason(int id, String displayName, Instant startAt, Instant endAt) {
        try {
            Season newSeason = new Season(id, displayName, Season.Status.PLANNED, startAt, endAt);
            
            // Replace current season
            currentSeason = newSeason;
            saveSeason();
            
            plugin.getLogger().info("Created new season: " + newSeason);
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create season: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Activates the current planned season
     */
    public boolean activateSeason() {
        if (currentSeason == null) {
            return false;
        }
        
        if (!currentSeason.canActivate()) {
            return false;
        }
        
        currentSeason.setStatus(Season.Status.ACTIVE);
        saveSeason();
        
        // Trigger season activation events
        if (eventListener != null) {
            eventListener.onSeasonActivated(currentSeason);
        }
        
        plugin.getLogger().info("Activated season: " + currentSeason.getId());
        return true;
    }
    
    /**
     * Archives the current season
     */
    public boolean archiveSeason() {
        if (currentSeason == null) {
            return false;
        }
        
        currentSeason.setStatus(Season.Status.ARCHIVED);
        
        // Trigger season archival events
        if (eventListener != null) {
            eventListener.onSeasonArchived(currentSeason);
        }
        
        saveSeason();
        
        plugin.getLogger().info("Archived season: " + currentSeason.getId());
        return true;
    }
    
    private void saveSeason() {
        if (currentSeason == null) {
            seasonsConfig.set("seasons", null);
        } else {
            // Create a single-entry list structure
            java.util.List<java.util.Map<String, Object>> seasonsList = new java.util.ArrayList<>();
            java.util.Map<String, Object> seasonData = new java.util.HashMap<>();
            
            seasonData.put("id", currentSeason.getId());
            seasonData.put("displayName", currentSeason.getDisplayName());
            seasonData.put("status", currentSeason.getStatus().name().toLowerCase());
            
            if (currentSeason.getStartAt() != null) {
                seasonData.put("startAt", currentSeason.getStartAtFormatted());
            }
            
            if (currentSeason.getEndAt() != null) {
                seasonData.put("endAt", currentSeason.getEndAtFormatted());
            }
            
            seasonsList.add(seasonData);
            seasonsConfig.set("seasons", seasonsList);
        }
        
        try {
            seasonsConfig.save(seasonsFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Could not save seasons.yml: " + e.getMessage());
        }
    }
    
    // Getters
    public Season getCurrentSeason() { return currentSeason; }
    
    public boolean hasActiveSeason() {
        return currentSeason != null && currentSeason.isActive();
    }
    
    public boolean hasArchivedSeason() {
        return currentSeason != null && currentSeason.isArchived();
    }
    
    public void reload() {
        loadConfiguration();
        plugin.getLogger().info("Season system reloaded");
    }
    
    public void setEventListener(SeasonEventListener eventListener) {
        this.eventListener = eventListener;
    }
}