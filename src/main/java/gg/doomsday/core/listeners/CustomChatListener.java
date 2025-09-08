package gg.doomsday.core.listeners;

import gg.doomsday.core.nations.Nation;
import gg.doomsday.core.nations.NationManager;
import gg.doomsday.core.nations.NationPlayerManager;
import gg.doomsday.core.nations.NationRoleManager;
import gg.doomsday.core.nations.NationRole;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedDataManager;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class CustomChatListener implements Listener {
    
    private final JavaPlugin plugin;
    private final NationPlayerManager nationPlayerManager;
    private final NationManager nationManager;
    private NationRoleManager roleManager; // Optional, may be null
    private LuckPerms luckPerms;
    private FileConfiguration chatConfig;
    private File chatConfigFile;
    
    public CustomChatListener(JavaPlugin plugin, NationPlayerManager nationPlayerManager, NationManager nationManager) {
        this.plugin = plugin;
        this.nationPlayerManager = nationPlayerManager;
        this.nationManager = nationManager;
        this.roleManager = null; // Will be set via setter if available
        
        // Load chat configuration
        loadChatConfig();
        
        // Get LuckPerms API
        try {
            this.luckPerms = LuckPermsProvider.get();
            if (isDebugEnabled()) {
                plugin.getLogger().info("LuckPerms integration enabled for custom chat");
            }
        } catch (IllegalStateException e) {
            if (isDebugEnabled()) {
                plugin.getLogger().warning("LuckPerms not found - suffix integration disabled");
            }
            this.luckPerms = null;
        }
    }
    
    public void setRoleManager(NationRoleManager roleManager) {
        this.roleManager = roleManager;
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // Check if custom chat is enabled
        if (!chatConfig.getBoolean("chat.enabled", true)) {
            return;
        }
        
        // Cancel the event to prevent default chat formatting
        event.setCancelled(true);
        
        Player player = event.getPlayer();
        String message = event.getMessage();
        
        // Build custom chat format
        String formattedMessage = buildChatMessage(player, message);
        
        // Build personalized messages for each player (role visibility)
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                String personalizedMessage = buildPersonalizedChatMessage(player, message, onlinePlayer);
                onlinePlayer.sendMessage(personalizedMessage);
            }
        });
        
        // Log to console if enabled
        if (chatConfig.getBoolean("chat.console.enabled", true)) {
            String consoleMessage = formattedMessage;
            if (!chatConfig.getBoolean("chat.console.include_colors", false)) {
                consoleMessage = ChatColor.stripColor(formattedMessage);
            }
            plugin.getLogger().info(consoleMessage);
        }
    }
    
    private String buildChatMessage(Player player, String message) {
        // Get player's nation
        String nationId = nationPlayerManager.getPlayerNation(player.getUniqueId());
        String chatFormat;
        
        // Choose format based on whether player has nation
        if (nationId != null) {
            Nation nation = nationManager.getAllNations().get(nationId);
            if (nation != null) {
                chatFormat = chatConfig.getString("chat.format_with_nation", "&7({nation}) &f{player}{suffix}&7: &f{message}");
                chatFormat = chatFormat.replace("{nation}", nation.getDisplayName());
            } else {
                chatFormat = chatConfig.getString("chat.format_without_nation", "&f{player}{suffix}&7: &f{message}");
            }
        } else {
            chatFormat = chatConfig.getString("chat.format_without_nation", "&f{player}{suffix}&7: &f{message}");
        }
        
        // Replace placeholders
        chatFormat = chatFormat.replace("{player}", player.getName());
        chatFormat = chatFormat.replace("{message}", message);
        
        // Handle suffix
        String suffix = getLuckPermsSuffix(player);
        String formattedSuffix = formatSuffix(suffix);
        chatFormat = chatFormat.replace("{suffix}", formattedSuffix);
        
        // Convert color codes and return
        return ChatColor.translateAlternateColorCodes('&', chatFormat);
    }
    
    private String buildPersonalizedChatMessage(Player sender, String message, Player receiver) {
        // Get sender's nation
        String senderNationId = nationPlayerManager.getPlayerNation(sender.getUniqueId());
        String receiverNationId = nationPlayerManager.getPlayerNation(receiver.getUniqueId());
        String chatFormat;
        
        // Choose format based on whether sender has nation
        if (senderNationId != null) {
            Nation nation = nationManager.getAllNations().get(senderNationId);
            if (nation != null) {
                chatFormat = chatConfig.getString("chat.format_with_nation", "&7({nation}{role}) &f{player}{suffix}&7: &f{message}");
                chatFormat = chatFormat.replace("{nation}", nation.getDisplayName());
                
                // Handle role display
                String roleDisplay = buildRoleDisplay(sender, receiver, senderNationId, receiverNationId);
                chatFormat = chatFormat.replace("{role}", roleDisplay);
            } else {
                chatFormat = chatConfig.getString("chat.format_without_nation", "&f{player}{suffix}&7: &f{message}");
                chatFormat = chatFormat.replace("{role}", "");
            }
        } else {
            chatFormat = chatConfig.getString("chat.format_without_nation", "&f{player}{suffix}&7: &f{message}");
            chatFormat = chatFormat.replace("{role}", "");
        }
        
        // Replace placeholders
        chatFormat = chatFormat.replace("{player}", sender.getName());
        chatFormat = chatFormat.replace("{message}", message);
        
        // Handle suffix
        String suffix = getLuckPermsSuffix(sender);
        String formattedSuffix = formatSuffix(suffix);
        chatFormat = chatFormat.replace("{suffix}", formattedSuffix);
        
        // Convert color codes and return
        return ChatColor.translateAlternateColorCodes('&', chatFormat);
    }
    
    private String buildRoleDisplay(Player sender, Player receiver, String senderNationId, String receiverNationId) {
        // Check if role display is enabled
        if (!chatConfig.getBoolean("chat.roles.enabled", true) || roleManager == null) {
            return "";
        }
        
        // Check if roles should only be shown to same nation members
        boolean sameNationOnly = chatConfig.getBoolean("chat.roles.same_nation_only", true);
        if (sameNationOnly && !senderNationId.equals(receiverNationId)) {
            return "";
        }
        
        // Get sender's role
        NationRole senderRole = roleManager.getPlayerRole(sender.getUniqueId());
        
        // Don't show citizen role if configured
        boolean showCitizen = chatConfig.getBoolean("chat.roles.show_citizen_role", false);
        if (senderRole == NationRole.CITIZEN && !showCitizen) {
            return "";
        }
        
        // Get role configuration
        String separator = chatConfig.getString("chat.roles.separator", " ");
        String roleFormat = chatConfig.getString("chat.roles.format", "&8{role}");
        
        // Get role color from configuration (fallback to default)
        String roleColor = getRoleColorFromConfig(senderRole);
        
        // Build the role display
        String roleDisplay = roleFormat.replace("{role}", senderRole.getDisplayName());
        roleDisplay = roleDisplay.replace("&8", roleColor); // Replace default color with configured color
        
        return separator + roleDisplay;
    }
    
    private String getRoleColorFromConfig(NationRole role) {
        // Try to get color from roles.yml configuration via role manager
        // For now, just return the default from the role enum
        // This could be enhanced to read from roles.yml in the future
        return role.getColorCode();
    }
    
    private String getLuckPermsSuffix(Player player) {
        if (luckPerms == null) {
            return "";
        }
        
        try {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user == null) {
                return "";
            }
            
            CachedDataManager cachedData = user.getCachedData();
            String suffix = cachedData.getMetaData().getSuffix();
            
            return suffix != null ? suffix : "";
        } catch (Exception e) {
            if (isDebugEnabled()) {
                plugin.getLogger().warning("Error getting LuckPerms suffix for " + player.getName() + ": " + e.getMessage());
            }
            return "";
        }
    }
    
    private String formatSuffix(String suffix) {
        if (suffix == null || suffix.isEmpty()) {
            return "";
        }
        
        StringBuilder formattedSuffix = new StringBuilder();
        
        // Add space before if configured
        if (chatConfig.getBoolean("chat.suffix.add_space_before", true)) {
            formattedSuffix.append(" ");
        }
        
        // Add prefix
        String suffixPrefix = chatConfig.getString("chat.suffix.prefix", "");
        if (!suffixPrefix.isEmpty()) {
            formattedSuffix.append(suffixPrefix);
        }
        
        // Add the suffix itself
        formattedSuffix.append(suffix);
        
        // Add suffix
        String suffixSuffix = chatConfig.getString("chat.suffix.suffix", "");
        if (!suffixSuffix.isEmpty()) {
            formattedSuffix.append(suffixSuffix);
        }
        
        return formattedSuffix.toString();
    }
    
    private void loadChatConfig() {
        chatConfigFile = new File(plugin.getDataFolder(), "chat.yml");
        
        if (!chatConfigFile.exists()) {
            plugin.saveResource("chat.yml", false);
        }
        
        chatConfig = YamlConfiguration.loadConfiguration(chatConfigFile);
        
        // Load defaults from resource
        InputStream defConfigStream = plugin.getResource("chat.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            chatConfig.setDefaults(defConfig);
        }
        
        if (isDebugEnabled()) {
            plugin.getLogger().info("Chat configuration loaded from chat.yml");
        }
    }
    
    public void reloadChatConfig() {
        loadChatConfig();
        plugin.getLogger().info("Chat configuration reloaded");
    }
    
    public FileConfiguration getChatConfig() {
        return chatConfig;
    }
    
    private boolean isDebugEnabled() {
        return chatConfig != null && chatConfig.getBoolean("chat.debug", false);
    }
}