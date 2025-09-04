package gg.doomsday.core.messaging;

import gg.doomsday.core.DoomsdayCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import gg.doomsday.core.nations.Nation;
import gg.doomsday.core.nations.NationPlayerManager;

import java.util.UUID;

/**
 * Centralized messaging system for configurable notifications
 * Handles global vs nation-specific messaging for missiles, anti-air, and disasters
 * 
 * Configuration:
 * - global: true = Send to all online players
 * - global: false = Send only to players in the relevant nation (launching/owning/affected)
 * 
 * Uses the centralized nation player tracking system for optimal performance.
 */
public class MessagingManager {
    
    private final JavaPlugin plugin;
    private final NationPlayerManager nationPlayerManager;
    
    // Configuration values
    private boolean missileGlobal;
    private boolean antiairGlobal;
    private boolean disasterGlobal;
    
    public MessagingManager(JavaPlugin plugin, NationPlayerManager nationPlayerManager) {
        this.plugin = plugin;
        this.nationPlayerManager = nationPlayerManager;
        loadConfiguration();
    }
    
    public void loadConfiguration() {
        // Load messaging configuration from config.yml
        missileGlobal = plugin.getConfig().getBoolean("messaging.missiles.global", true);
        antiairGlobal = plugin.getConfig().getBoolean("messaging.antiair.global", false);
        disasterGlobal = plugin.getConfig().getBoolean("messaging.disasters.global", false);
        
        plugin.getLogger().info("Messaging Manager loaded - Missiles: " + (missileGlobal ? "global" : "nation-specific") + 
                               ", Anti-air: " + (antiairGlobal ? "global" : "nation-specific") +
                               ", Disasters: " + (disasterGlobal ? "global" : "nation-specific"));
    }
    
    /**
     * Send missile launch message based on configuration
     * @param message The message to send
     * @param launchLocation The location where missile was launched (for logging/future use)
     * @param launchingNation The nation that launched the missile (required for nation-specific messaging)
     */
    public void sendMissileMessage(String message, Location launchLocation, Nation launchingNation) {
        if (missileGlobal) {
            // Send to all online players
            Bukkit.broadcastMessage(message);
        } else if (launchingNation != null) {
            // Send only to players in the launching nation using cached data
            for (UUID playerId : nationPlayerManager.getOnlinePlayersInNation(launchingNation.getId())) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    player.sendMessage(message);
                }
            }
        }
        // Note: If launchingNation is null and global=false, no message is sent
    }
    
    /**
     * Send anti-air defense message based on configuration
     * @param message The message to send
     * @param defenseLocation The location of the anti-air defense (for logging/future use)
     * @param defenseName The name of the anti-air defense (used to determine owning nation)
     */
    public void sendAntiAirMessage(String message, Location defenseLocation, String defenseName) {
        if (antiairGlobal) {
            // Send to all online players
            Bukkit.broadcastMessage(message);
        } else {
            // Send only to players in the nation that owns this anti-air defense using cached data
            Nation owningNation = getAntiAirOwningNation(defenseName);
            if (owningNation != null) {
                for (UUID playerId : nationPlayerManager.getOnlinePlayersInNation(owningNation.getId())) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null && player.isOnline()) {
                        player.sendMessage(message);
                    }
                }
            }
            // Note: If owningNation is null and global=false, no message is sent
        }
    }
    
    /**
     * Determine which nation owns an anti-air defense based on its name/location
     * @param defenseName The name of the anti-air defense
     * @return The nation that owns this defense, or null if unknown
     */
    private Nation getAntiAirOwningNation(String defenseName) {
        // Parse nation from defense name (e.g., "patriot_america_border" -> "america")
        String lowerName = defenseName.toLowerCase();
        
        if (lowerName.contains("america")) return getNationById("america");
        if (lowerName.contains("europe")) return getNationById("europe");
        if (lowerName.contains("africa")) return getNationById("africa");
        if (lowerName.contains("asia")) return getNationById("asia");
        if (lowerName.contains("antarctica")) return getNationById("antarctica");
        
        return null; // Unknown nation
    }
    
    /**
     * Get Nation object by ID
     */
    private Nation getNationById(String nationId) {
        // We need access to NationManager - let's get it through the plugin
        if (plugin instanceof DoomsdayCore) {
            DoomsdayCore doomsdayCore = (DoomsdayCore) plugin;
            return doomsdayCore.getNationManager().getAllNations().get(nationId);
        }
        return null;
    }
    
    /**
     * Send disaster message based on configuration
     * @param message The message to send
     * @param disasterLocation The center location of the disaster (for logging/future use)
     * @param affectedNation The nation affected by the disaster (required for nation-specific messaging)
     */
    public void sendDisasterMessage(String message, Location disasterLocation, Nation affectedNation) {
        if (disasterGlobal) {
            // Send to all online players
            Bukkit.broadcastMessage(message);
        } else if (affectedNation != null) {
            // Send only to players in the affected nation using cached data
            for (UUID playerId : nationPlayerManager.getOnlinePlayersInNation(affectedNation.getId())) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    player.sendMessage(message);
                }
            }
        }
        // Note: If affectedNation is null and global=false, no message is sent
    }
    
    // Getter methods for configuration values
    public boolean isMissileGlobal() { return missileGlobal; }
    public boolean isAntiairGlobal() { return antiairGlobal; }
    public boolean isDisasterGlobal() { return disasterGlobal; }
    
    /**
     * Reload configuration from config files
     */
    public void reload() {
        loadConfiguration();
    }
}