package gg.doomsday.core.services;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import gg.doomsday.core.config.ConfigManager;
import gg.doomsday.core.explosions.RocketLauncher;
import gg.doomsday.core.managers.MessageManager;
import gg.doomsday.core.messaging.MessagingManager;
import gg.doomsday.core.nations.Nation;
import gg.doomsday.core.nations.NationManager;
import gg.doomsday.core.utils.NationColors;
import gg.doomsday.core.DoomsdayCore;

/**
 * Centralized service for all missile-related operations
 * Provides a single entry point for launching missiles from any part of the application
 */
public class MissileService {
    
    private final JavaPlugin plugin;
    private final RocketLauncher rocketLauncher;
    private final MessageManager messageManager;
    
    public MissileService(JavaPlugin plugin, RocketLauncher rocketLauncher, MessageManager messageManager) {
        this.plugin = plugin;
        this.rocketLauncher = rocketLauncher;
        this.messageManager = messageManager;
    }
    
    /**
     * Launch a missile by its configuration key
     * This is the main entry point for missile launching from anywhere in the application
     * 
     * @param player The player who initiated the launch
     * @param rocketKey The missile configuration key
     * @return true if launch was successful, false otherwise
     */
    public boolean launchMissile(Player player, String rocketKey) {
        // Check permissions
        if (!player.hasPermission("rocket.use")) {
            player.sendMessage("§c❌ You don't have permission to launch rockets!");
            return false;
        }
        
        // Get rocket configuration
        ConfigurationSection rocket = ((DoomsdayCore) plugin).getConfigManager().getRocketsConfig().getConfigurationSection("rockets." + rocketKey);
        if (rocket == null) {
            player.sendMessage("❌ Rocket '" + rocketKey + "' not found in config!");
            return false;
        }
        
        try {
            return executeMissileLaunch(player, rocketKey, rocket);
        } catch (Exception e) {
            player.sendMessage("❌ Error launching rocket: " + e.getMessage());
            plugin.getLogger().warning("Error launching rocket " + rocketKey + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Launch a missile using a command (maintains compatibility with existing /r command)
     * 
     * @param player The player who executed the command
     * @param rocketKey The missile key
     * @return true if successful
     */
    public boolean launchMissileViaCommand(Player player, String rocketKey) {
        plugin.getLogger().info("Missile launch requested via command by " + player.getName() + " for rocket: " + rocketKey);
        return launchMissile(player, rocketKey);
    }
    
    /**
     * Launch a missile from GUI interaction
     * 
     * @param player The player who clicked the GUI
     * @param rocketKey The missile key
     * @return true if successful
     */
    public boolean launchMissileViaGUI(Player player, String rocketKey) {
        plugin.getLogger().info("Missile launch requested via GUI by " + player.getName() + " for rocket: " + rocketKey);
        return launchMissile(player, rocketKey);
    }
    
    /**
     * Execute the actual missile launch with all effects and notifications
     */
    private boolean executeMissileLaunch(Player player, String rocketKey, ConfigurationSection rocket) {
        World world = player.getWorld();
        
        // Extract configuration
        double x1 = rocket.getDouble("start.x");
        double y1 = rocket.getDouble("start.y");
        double z1 = rocket.getDouble("start.z");
        
        double x2 = rocket.getDouble("end.x");
        double y2 = rocket.getDouble("end.y");
        double z2 = rocket.getDouble("end.z");
        
        double smokeOffset = rocket.getDouble("smokeOffset", 0.0);
        double speed = rocket.getDouble("speed", 1.0);
        double arcScale = rocket.getDouble("arcScale", 1.0);
        String soundStr = rocket.getString("sound", "ENTITY_FIREWORK_ROCKET_LAUNCH");
        String explosionTypeStr = rocket.getString("explosionType", "DEFAULT");
        String displayName = rocket.getString("displayName", rocketKey.toUpperCase());
        
        Location start = new Location(world, x1, y1, z1);
        Location end = new Location(world, x2, y2, z2);
        
        // Log launch details
        plugin.getLogger().info("=== MISSILE LAUNCH ===");
        plugin.getLogger().info("Player: " + player.getName());
        plugin.getLogger().info("Rocket: " + rocketKey + " (" + displayName + ")");
        plugin.getLogger().info("Start: " + (int)x1 + "," + (int)y1 + "," + (int)z1);
        plugin.getLogger().info("Target: " + (int)x2 + "," + (int)y2 + "," + (int)z2);
        plugin.getLogger().info("Type: " + explosionTypeStr);
        
        // Launch the rocket
        rocketLauncher.spawnRocket(start, end, smokeOffset, speed, arcScale, soundStr, explosionTypeStr);
        
        // Send notifications to all players
        String nationId = getNationIdFromRocketKey(rocketKey);
        broadcastMissileLaunch(world, nationId, start);
        
        // Send confirmation to launching player
        player.sendMessage("§a§l🚀 " + displayName + " LAUNCHED!");
        player.sendMessage("§7Target: " + (int)x2 + ", " + (int)y2 + ", " + (int)z2);
        
        return true;
    }
    
    /**
     * Broadcast missile launch warning using the configurable messaging system
     */
    private void broadcastMissileLaunch(World world, String nationId, Location launchLocation) {
        String coloredNationName = NationColors.getColoredNationName(nationId);
        String launchMsg = messageManager.getMessage("missile.launch", "nation", coloredNationName);
        
        // Get the launching nation object
        Nation launchingNation = getLaunchingNationFromName(nationId);
        
        // Use the new MessagingManager for configurable messaging
        DoomsdayCore doomsdayCore = (DoomsdayCore) plugin;
        MessagingManager messagingManager = doomsdayCore.getMessagingManager();
        messagingManager.sendMissileMessage(launchMsg, launchLocation, launchingNation);
        
        // Play air raid siren sound to all players within range
        for (Player p : world.getPlayers()) {
            // Only play sound if they would have received the message
            if (shouldPlayerReceiveMessage(p, launchLocation, launchingNation, messagingManager)) {
                playAirRaidSiren(p);
            }
        }
    }
    
    /**
     * Check if a player should receive the missile message based on current configuration
     */
    private boolean shouldPlayerReceiveMessage(Player player, Location launchLocation, Nation launchingNation, MessagingManager messagingManager) {
        if (messagingManager.isMissileGlobal()) {
            return true;
        }
        
        if (launchingNation != null) {
            // Check if player is in the launching nation
            DoomsdayCore doomsdayCore = (DoomsdayCore) plugin;
            String playerNation = doomsdayCore.getNationPlayerManager().getPlayerNation(player.getUniqueId());
            return launchingNation.getId().equals(playerNation);
        }
        
        // If nation is unknown and global=false, no sound is played
        return false;
    }
    
    /**
     * Get Nation object from nation name
     */
    private Nation getLaunchingNationFromName(String nationName) {
        DoomsdayCore doomsdayCore = (DoomsdayCore) plugin;
        NationManager nationManager = doomsdayCore.getNationManager();
        
        for (Nation nation : nationManager.getAllNations().values()) {
            if (nation.getDisplayName().equalsIgnoreCase(nationName) || nation.getId().equalsIgnoreCase(nationName)) {
                return nation;
            }
        }
        
        return null;
    }
    
    /**
     * Determine which nation a missile belongs to based on its key
     */
    private String getNationIdFromRocketKey(String rocketKey) {
        if (rocketKey.contains("america")) return "america";
        if (rocketKey.contains("europe")) return "europe";
        if (rocketKey.contains("africa")) return "africa";
        if (rocketKey.contains("asia")) return "asia";
        if (rocketKey.contains("antarctica")) return "antarctica";
        return "unknown";
    }
    
    /**
     * Play air raid siren sound sequence for a player
     */
    private void playAirRaidSiren(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.5f, 0.5f);
        
        new BukkitRunnable() {
            int beepCount = 0;
            
            @Override
            public void run() {
                if (beepCount >= 2) {
                    cancel();
                    return;
                }
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.5f, 0.5f);
                beepCount++;
            }
        }.runTaskTimer(plugin, 10L, 10L);
        
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
    }
    
    /**
     * Get missile information for GUI display
     */
    public MissileInfo getMissileInfo(String rocketKey) {
        ConfigurationSection rocket = ((DoomsdayCore) plugin).getConfigManager().getRocketsConfig().getConfigurationSection("rockets." + rocketKey);
        if (rocket == null) return null;
        
        return new MissileInfo(
            rocketKey,
            rocket.getString("displayName", rocketKey.toUpperCase()),
            rocket.getString("explosionType", "DEFAULT"),
            rocket.getDouble("speed", 1.0),
            rocket.getDouble("arcScale", 1.0),
            new Location(null, rocket.getDouble("start.x"), rocket.getDouble("start.y"), rocket.getDouble("start.z")),
            new Location(null, rocket.getDouble("end.x"), rocket.getDouble("end.y"), rocket.getDouble("end.z"))
        );
    }
    
    /**
     * Data class to hold missile information
     */
    public static class MissileInfo {
        private final String key;
        private final String displayName;
        private final String explosionType;
        private final double speed;
        private final double arcScale;
        private final Location startLocation;
        private final Location endLocation;
        
        public MissileInfo(String key, String displayName, String explosionType, double speed, 
                          double arcScale, Location startLocation, Location endLocation) {
            this.key = key;
            this.displayName = displayName;
            this.explosionType = explosionType;
            this.speed = speed;
            this.arcScale = arcScale;
            this.startLocation = startLocation;
            this.endLocation = endLocation;
        }
        
        // Getters
        public String getKey() { return key; }
        public String getDisplayName() { return displayName; }
        public String getExplosionType() { return explosionType; }
        public double getSpeed() { return speed; }
        public double getArcScale() { return arcScale; }
        public Location getStartLocation() { return startLocation; }
        public Location getEndLocation() { return endLocation; }
    }
}