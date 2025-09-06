package gg.doomsday.core.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages multiple configuration files for the DoomsdayCore plugin
 */
public class ConfigManager {
    
    private final JavaPlugin plugin;
    private final Map<String, FileConfiguration> configs;
    private final Map<String, File> configFiles;
    
    // Configuration file names
    public static final String MAIN_CONFIG = "config";
    public static final String ROCKETS_CONFIG = "rockets";
    public static final String ANTIAIR_CONFIG = "antiair";
    public static final String NUCLEAR_CONFIG = "nuclear";
    public static final String REINFORCEMENT_CONFIG = "reinforcement";
    public static final String MESSAGING_CONFIG = "messaging";
    public static final String NATIONS_CONFIG = "nations";
    public static final String CUSTOM_ITEMS_CONFIG = "custom_items";
    public static final String MESSAGES_CONFIG = "messages";
    
    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configs = new HashMap<>();
        this.configFiles = new HashMap<>();
        
        // Load all configuration files
        loadAllConfigs();
    }
    
    /**
     * Load all configuration files
     */
    public void loadAllConfigs() {
        // Load main config (handled by Bukkit by default)
        plugin.saveDefaultConfig();
        configs.put(MAIN_CONFIG, plugin.getConfig());
        configFiles.put(MAIN_CONFIG, new File(plugin.getDataFolder(), "config.yml"));
        
        // Load feature-specific configs
        loadConfig(ROCKETS_CONFIG);
        loadConfig(ANTIAIR_CONFIG);
        loadConfig(NUCLEAR_CONFIG);
        loadConfig(REINFORCEMENT_CONFIG);
        loadConfig(MESSAGING_CONFIG);
        
        // Load existing configs
        loadConfig(NATIONS_CONFIG);
        loadConfig(CUSTOM_ITEMS_CONFIG);
        loadConfig(MESSAGES_CONFIG);
        
        plugin.getLogger().info("Loaded " + configs.size() + " configuration files");
    }
    
    /**
     * Load a specific configuration file
     */
    private void loadConfig(String configName) {
        String fileName = configName + ".yml";
        File configFile = new File(plugin.getDataFolder(), fileName);
        
        // Save default if file doesn't exist and resource is available
        if (!configFile.exists()) {
            try {
                if (plugin.getResource(fileName) != null) {
                    plugin.saveResource(fileName, false);
                    plugin.getLogger().info("Created default " + fileName);
                } else {
                    // Create empty config if resource doesn't exist
                    plugin.getLogger().warning("No default " + fileName + " found in resources, creating empty config");
                    try {
                        configFile.getParentFile().mkdirs();
                        configFile.createNewFile();
                    } catch (IOException e) {
                        plugin.getLogger().log(Level.SEVERE, "Could not create " + fileName, e);
                        return;
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save default " + fileName, e);
                return;
            }
        }
        
        // Load the configuration
        FileConfiguration config;
        try {
            config = YamlConfiguration.loadConfiguration(configFile);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Could not load " + fileName, e);
            return;
        }
        
        // Load defaults from resource if available
        try {
            InputStream defaultStream = plugin.getResource(fileName);
            if (defaultStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
                config.setDefaults(defaultConfig);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Could not load default configuration for " + fileName, e);
        }
        
        configs.put(configName, config);
        configFiles.put(configName, configFile);
    }
    
    /**
     * Get a configuration by name
     */
    public FileConfiguration getConfig(String configName) {
        FileConfiguration config = configs.get(configName);
        if (config == null) {
            plugin.getLogger().warning("Configuration '" + configName + "' not found, creating empty config");
            // Create empty config as fallback
            config = new YamlConfiguration();
            configs.put(configName, config);
        }
        return config;
    }
    
    /**
     * Get the main config (equivalent to plugin.getConfig())
     */
    public FileConfiguration getMainConfig() {
        return getConfig(MAIN_CONFIG);
    }
    
    /**
     * Get the rockets configuration
     */
    public FileConfiguration getRocketsConfig() {
        FileConfiguration config = getConfig(ROCKETS_CONFIG);
        if (config == null || !config.contains("soundRadius")) {
            plugin.getLogger().warning("Rockets configuration is missing or incomplete, using defaults");
            // Create minimal default config
            config = new YamlConfiguration();
            config.set("soundRadius", 100.0);
            configs.put(ROCKETS_CONFIG, config);
        }
        return config;
    }
    
    /**
     * Get the anti-air configuration
     */
    public FileConfiguration getAntiairConfig() {
        return getConfig(ANTIAIR_CONFIG);
    }
    
    /**
     * Get the nuclear configuration
     */
    public FileConfiguration getNuclearConfig() {
        return getConfig(NUCLEAR_CONFIG);
    }
    
    /**
     * Get the reinforcement configuration
     */
    public FileConfiguration getReinforcementConfig() {
        return getConfig(REINFORCEMENT_CONFIG);
    }
    
    /**
     * Get the messaging configuration
     */
    public FileConfiguration getMessagingConfig() {
        FileConfiguration config = getConfig(MESSAGING_CONFIG);
        if (config == null || !config.contains("messaging")) {
            plugin.getLogger().warning("Messaging configuration is missing or empty, using defaults");
            // Create minimal default config
            config = new YamlConfiguration();
            config.set("messaging.missiles.global", true);
            config.set("messaging.antiair.global", false);
            config.set("messaging.disasters.global", false);
            configs.put(MESSAGING_CONFIG, config);
        }
        return config;
    }
    
    /**
     * Get the nations configuration
     */
    public FileConfiguration getNationsConfig() {
        return getConfig(NATIONS_CONFIG);
    }
    
    /**
     * Get the custom items configuration
     */
    public FileConfiguration getCustomItemsConfig() {
        return getConfig(CUSTOM_ITEMS_CONFIG);
    }
    
    /**
     * Get the messages configuration
     */
    public FileConfiguration getMessagesConfig() {
        return getConfig(MESSAGES_CONFIG);
    }
    
    /**
     * Reload a specific configuration
     */
    public void reloadConfig(String configName) {
        if (configName.equals(MAIN_CONFIG)) {
            plugin.reloadConfig();
            configs.put(MAIN_CONFIG, plugin.getConfig());
        } else {
            loadConfig(configName);
        }
        plugin.getLogger().info("Reloaded " + configName + ".yml");
    }
    
    /**
     * Reload all configurations
     */
    public void reloadAllConfigs() {
        configs.clear();
        configFiles.clear();
        loadAllConfigs();
        plugin.getLogger().info("Reloaded all configuration files");
    }
    
    /**
     * Save a specific configuration
     */
    public void saveConfig(String configName) {
        FileConfiguration config = configs.get(configName);
        File configFile = configFiles.get(configName);
        
        if (config != null && configFile != null) {
            try {
                config.save(configFile);
                plugin.getLogger().info("Saved " + configName + ".yml");
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save " + configName + ".yml", e);
            }
        }
    }
    
    /**
     * Save all configurations
     */
    public void saveAllConfigs() {
        for (String configName : configs.keySet()) {
            if (!configName.equals(MAIN_CONFIG)) { // Main config is handled by Bukkit
                saveConfig(configName);
            }
        }
        plugin.saveConfig(); // Save main config through Bukkit
    }
    
    /**
     * Check if a configuration exists
     */
    public boolean hasConfig(String configName) {
        return configs.containsKey(configName);
    }
    
    /**
     * Get all loaded configuration names
     */
    public String[] getConfigNames() {
        return configs.keySet().toArray(new String[0]);
    }
}