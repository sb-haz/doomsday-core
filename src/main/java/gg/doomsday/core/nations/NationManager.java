package gg.doomsday.core.nations;

import gg.doomsday.core.DoomsdayCore;
import gg.doomsday.core.managers.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class NationManager {
    private final JavaPlugin plugin;
    private final Map<String, Nation> nations;
    private final MessageManager messageManager;
    private FileConfiguration nationsConfig;
    private boolean enableDisasters;
    private int checkInterval;
    private boolean debug;
    private BukkitRunnable disasterTask;
    private DisasterEffectsHandler effectsHandler;
    private NationPlayerManager nationPlayerManager;

    public NationManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.nations = new HashMap<>();
        this.messageManager = new MessageManager(plugin);
        loadConfiguration();
        startDisasterChecker();
    }
    
    public void setNationPlayerManager(NationPlayerManager nationPlayerManager) {
        this.nationPlayerManager = nationPlayerManager;
        this.effectsHandler = new DisasterEffectsHandler(plugin, nationPlayerManager);
    }

    public void loadConfiguration() {
        File nationsFile = new File(plugin.getDataFolder(), "nations.yml");
        
        if (!nationsFile.exists()) {
            plugin.saveResource("nations.yml", false);
        }
        
        nationsConfig = YamlConfiguration.loadConfiguration(nationsFile);
        
        // Load default configuration from resource if file is empty
        InputStream defConfigStream = plugin.getResource("nations.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            nationsConfig.setDefaults(defConfig);
        }
        
        loadGlobalSettings();
        loadNations();
        
        plugin.getLogger().info("Loaded " + nations.size() + " nations with disaster systems");
    }

    private void loadGlobalSettings() {
        enableDisasters = nationsConfig.getBoolean("global.enableDisasters", true);
        checkInterval = nationsConfig.getInt("global.checkInterval", 600);
        debug = nationsConfig.getBoolean("global.debug", false);
        
        // Remove announceToAll and announceRadius - now handled by MessagingManager
        
        if (debug) {
            plugin.getLogger().info("Nation Manager Debug Mode Enabled");
        }
    }

    private void loadNations() {
        nations.clear();
        ConfigurationSection nationsSection = nationsConfig.getConfigurationSection("nations");
        
        if (nationsSection == null) {
            plugin.getLogger().warning("No nations configured in nations.yml");
            return;
        }
        
        for (String nationId : nationsSection.getKeys(false)) {
            ConfigurationSection nationSection = nationsSection.getConfigurationSection(nationId);
            
            if (nationSection == null) continue;
            
            String displayName = nationSection.getString("displayName", nationId);
            
            // Load borders
            ConfigurationSection borderSection = nationSection.getConfigurationSection("borders");
            if (borderSection == null) {
                plugin.getLogger().warning("No borders defined for nation: " + nationId);
                continue;
            }
            
            NationBorders borders = new NationBorders(
                borderSection.getInt("minX"),
                borderSection.getInt("maxX"),
                borderSection.getInt("minZ"),
                borderSection.getInt("maxZ"),
                borderSection.getInt("minY"),
                borderSection.getInt("maxY")
            );
            
            Nation nation = new Nation(nationId, displayName, borders);
            
            // Load additional nation data
            nation.setTotalPlayers(nationSection.getInt("totalPlayers", 0));
            nation.setCenterX(nationSection.getDouble("centerX", borders.getCenterX()));
            nation.setCenterZ(nationSection.getDouble("centerZ", borders.getCenterZ()));
            
            if (nationSection.contains("missileTypes")) {
                nation.setMissileTypes(nationSection.getStringList("missileTypes"));
            }
            
            // Load disasters
            ConfigurationSection disastersSection = nationSection.getConfigurationSection("disasters");
            if (disastersSection != null) {
                for (String disasterId : disastersSection.getKeys(false)) {
                    ConfigurationSection disasterSection = disastersSection.getConfigurationSection(disasterId);
                    
                    if (disasterSection == null) continue;
                    
                    Disaster disaster = new Disaster(
                        disasterId,
                        disasterSection.getBoolean("enabled", true),
                        disasterSection.getInt("minInterval", 1200),
                        disasterSection.getInt("maxInterval", 3600),
                        disasterSection.getInt("duration", 600),
                        disasterSection.getDouble("probability", 0.3),
                        disasterSection.getString("message", "A disaster is occurring!")
                    );
                    
                    // Schedule initial check
                    disaster.scheduleNext();
                    
                    nation.addDisaster(disasterId, disaster);
                }
            }
            
            nations.put(nationId, nation);
            
            if (debug) {
                plugin.getLogger().info("Loaded nation: " + nationId + " (" + nation.getDisplayName() + ")");
            }
        }
    }

    private void startDisasterChecker() {
        if (disasterTask != null) {
            disasterTask.cancel();
        }
        
        if (!enableDisasters) {
            plugin.getLogger().info("Natural disasters are disabled");
            return;
        }
        
        disasterTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkForDisasters();
            }
        };
        
        disasterTask.runTaskTimer(plugin, checkInterval, checkInterval);
        
        if (debug) {
            plugin.getLogger().info("Disaster checker started with interval: " + checkInterval + " ticks");
        }
    }

    private void checkForDisasters() {
        long currentTime = System.currentTimeMillis();
        
        for (Nation nation : nations.values()) {
            for (Disaster disaster : nation.getDisasters().values()) {
                if (!disaster.isEnabled()) continue;
                
                // Check if active disaster should end
                if (disaster.isActive() && currentTime >= disaster.getEndTime()) {
                    disaster.end();
                    announceDisasterEnd(nation, disaster);
                    continue;
                }
                
                // Skip if disaster is currently active
                if (disaster.isActive()) continue;
                
                // Check if it's time to evaluate this disaster
                if (currentTime >= disaster.getNextCheck()) {
                    // Roll for disaster occurrence
                    if (ThreadLocalRandom.current().nextDouble() < disaster.getProbability()) {
                        triggerDisaster(nation, disaster);
                    } else {
                        // Schedule next check
                        disaster.scheduleNext();
                        
                        if (debug) {
                            plugin.getLogger().info("Disaster check failed for " + disaster.getId() + 
                                " in " + nation.getDisplayName() + ", rescheduling");
                        }
                    }
                }
            }
        }
    }

    private void triggerDisaster(Nation nation, Disaster disaster) {
        disaster.start();
        
        // Get message from messages.yml
        String messageKey = "disasters." + disaster.getId() + ".start";
        String message = messageManager.getMessage(messageKey)
            .replace("{nation}", nation.getDisplayName());
        
        // Use configurable messaging system
        if (plugin instanceof DoomsdayCore) {
            DoomsdayCore doomsdayCore = (DoomsdayCore) plugin;
            NationBorders borders = nation.getBorders();
            Location centerLoc = new Location(
                Bukkit.getWorlds().get(0), // Assume main world
                borders.getCenterX(),
                borders.getMinY() + (borders.getMaxY() - borders.getMinY()) / 2,
                borders.getCenterZ()
            );
            
            doomsdayCore.getMessagingManager().sendDisasterMessage(message, centerLoc, nation);
        } else {
            // Fallback to broadcast if not DoomsdayCore instance
            Bukkit.broadcastMessage(message);
        }
        
        plugin.getLogger().info("DISASTER TRIGGERED: " + disaster.getId() + " in " + nation.getDisplayName() + 
            " (Duration: " + disaster.getDuration() + " ticks)");
        
        // Trigger actual disaster effects
        effectsHandler.triggerDisaster(nation, disaster);
    }

    private void announceDisasterEnd(Nation nation, Disaster disaster) {
        // Get end message from messages.yml
        String messageKey = "disasters." + disaster.getId() + ".end";
        String endMessage = messageManager.getMessage(messageKey)
            .replace("{nation}", nation.getDisplayName());
        
        // Use configurable messaging system for end messages too
        if (plugin instanceof DoomsdayCore) {
            DoomsdayCore doomsdayCore = (DoomsdayCore) plugin;
            NationBorders borders = nation.getBorders();
            Location centerLoc = new Location(
                Bukkit.getWorlds().get(0), // Assume main world
                borders.getCenterX(),
                borders.getMinY() + (borders.getMaxY() - borders.getMinY()) / 2,
                borders.getCenterZ()
            );

            doomsdayCore.getMessagingManager().sendDisasterMessage(endMessage, centerLoc, nation);
        } else {
            // Fallback to broadcast if not DoomsdayCore instance
            Bukkit.broadcastMessage(endMessage);
        }
        
        if (debug) {
            plugin.getLogger().info("DISASTER ENDED: " + disaster.getId() + " in " + nation.getDisplayName());
        }
        
        // Stop disaster effects
        effectsHandler.stopDisaster(nation, disaster);
    }

    public Nation getNationAt(Location location) {
        return getNationAt(location.getX(), location.getY(), location.getZ());
    }

    public Nation getNationAt(double x, double y, double z) {
        for (Nation nation : nations.values()) {
            if (nation.containsLocation(x, y, z)) {
                return nation;
            }
        }
        return null;
    }

    public Map<String, Nation> getAllNations() {
        return new HashMap<>(nations);
    }

    public void reload() {
        if (disasterTask != null) {
            disasterTask.cancel();
        }
        
        // Shutdown old effects handler and create new one
        if (effectsHandler != null) {
            effectsHandler.shutdown();
        }
        effectsHandler = new DisasterEffectsHandler(plugin, nationPlayerManager);
        
        loadConfiguration();
        startDisasterChecker();
        plugin.getLogger().info("Nation system reloaded");
    }

    public void shutdown() {
        if (disasterTask != null) {
            disasterTask.cancel();
        }
        
        if (effectsHandler != null) {
            effectsHandler.shutdown();
        }
    }

    public boolean isDebugEnabled() {
        return debug;
    }
    
    public void saveNationPlayerCount(String nationId, int playerCount) {
        nationsConfig.set("nations." + nationId + ".totalPlayers", playerCount);
        
        try {
            File nationsFile = new File(plugin.getDataFolder(), "nations.yml");
            nationsConfig.save(nationsFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Could not save nations.yml: " + e.getMessage());
        }
    }

    /**
     * Manually trigger a disaster (for admin commands)
     */
    public void manuallyTriggerDisaster(Nation nation, Disaster disaster) {
        triggerDisaster(nation, disaster);
    }

    /**
     * Manually stop a disaster (for admin commands)
     */
    public void manuallyStopDisaster(Nation nation, Disaster disaster) {
        announceDisasterEnd(nation, disaster);
    }

    /**
     * Enable automatic disasters
     */
    public void enableAutomaticDisasters() {
        if (!enableDisasters) {
            enableDisasters = true;
            startDisasterChecker();
            plugin.getLogger().info("Automatic disasters enabled");
        }
    }

    /**
     * Disable automatic disasters
     */
    public void disableAutomaticDisasters() {
        if (enableDisasters) {
            enableDisasters = false;
            if (disasterTask != null) {
                disasterTask.cancel();
                disasterTask = null;
            }
            plugin.getLogger().info("Automatic disasters disabled");
        }
    }

    /**
     * Check if automatic disasters are enabled
     */
    public boolean areAutomaticDisastersEnabled() {
        return enableDisasters;
    }
    
    /**
     * Get the plugin instance
     */
    public JavaPlugin getPlugin() {
        return plugin;
    }
}