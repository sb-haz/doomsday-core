package gg.doomsday.core.managers;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages loading and retrieving localized messages from messages.yml
 */
public class MessageManager {
    
    private final JavaPlugin plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;
    
    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadMessages();
    }
    
    /**
     * Load or reload messages from messages.yml
     */
    public void reloadMessages() {
        loadMessages();
        plugin.getLogger().info("Messages configuration reloaded");
    }
    
    private void loadMessages() {
        // Create messages.yml if it doesn't exist
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            messagesFile.getParentFile().mkdirs();
            plugin.saveResource("messages.yml", false);
        }
        
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Load defaults from resource
        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(defaultStream));
            messagesConfig.setDefaults(defaultConfig);
        }
    }
    
    /**
     * Get a message by key with color code translation
     * 
     * @param key The message key
     * @return The translated message with color codes
     */
    public String getMessage(String key) {
        String message = messagesConfig.getString(key);
        if (message == null) {
            plugin.getLogger().warning("Missing message key: " + key);
            return "§c[Missing message: " + key + "]";
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * Get a message by key with placeholder replacement
     * 
     * @param key The message key
     * @param placeholder The placeholder name (without % or {})
     * @param value The value to replace the placeholder with
     * @return The translated message with color codes and placeholder replaced
     */
    public String getMessage(String key, String placeholder, String value) {
        String message = getMessage(key);
        // Support both %placeholder% and {placeholder} formats
        message = message.replace("%" + placeholder + "%", value);
        message = message.replace("{" + placeholder + "}", value);
        return message;
    }
    
    /**
     * Get a message by key with multiple placeholder replacements
     * 
     * @param key The message key
     * @param placeholders Array of placeholder names (without % or {})
     * @param values Array of values to replace the placeholders with
     * @return The translated message with color codes and placeholders replaced
     */
    public String getMessage(String key, String[] placeholders, String[] values) {
        if (placeholders.length != values.length) {
            plugin.getLogger().warning("Placeholder and value arrays must be the same length for message key: " + key);
            return getMessage(key);
        }
        
        String message = getMessage(key);
        for (int i = 0; i < placeholders.length; i++) {
            // Support both %placeholder% and {placeholder} formats
            message = message.replace("%" + placeholders[i] + "%", values[i]);
            message = message.replace("{" + placeholders[i] + "}", values[i]);
        }
        return message;
    }
    
    /**
     * Get a list of messages (for multi-line messages)
     * 
     * @param key The message key
     * @return List of translated messages with color codes
     */
    public List<String> getMessageList(String key) {
        List<String> messages = messagesConfig.getStringList(key);
        List<String> translatedMessages = new ArrayList<>();
        
        for (String message : messages) {
            translatedMessages.add(ChatColor.translateAlternateColorCodes('&', message));
        }
        
        return translatedMessages;
    }
    
    /**
     * Check if a message key exists
     * 
     * @param key The message key to check
     * @return true if the key exists
     */
    public boolean hasMessage(String key) {
        return messagesConfig.contains(key);
    }
    
    /**
     * Save the current messages configuration
     */
    public void saveMessages() {
        try {
            messagesConfig.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save messages.yml: " + e.getMessage());
        }
    }
}