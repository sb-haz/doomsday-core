package gg.doomsday.core.listeners;

import gg.doomsday.core.nations.Nation;
import gg.doomsday.core.nations.NationManager;
import gg.doomsday.core.nations.NationPlayerManager;
import gg.doomsday.core.nations.NationRoleManager;
import gg.doomsday.core.nations.NationRole;
import gg.doomsday.core.data.PlayerDataManager;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedDataManager;
import net.luckperms.api.model.user.User;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import com.earth2me.essentials.Essentials;
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
import java.util.Random;

public class CustomChatListener implements Listener {
    
    private final JavaPlugin plugin;
    private final NationPlayerManager nationPlayerManager;
    private final NationManager nationManager;
    private final PlayerDataManager playerDataManager;
    private NationRoleManager roleManager; // Optional, may be null
    private LuckPerms luckPerms;
    private Essentials essentials;
    private FileConfiguration chatConfig;
    private File chatConfigFile;
    private final Random random = new Random();
    private final String[] greetings = {"hi", "wassup", "yo", "hey", "sup"};
    
    public CustomChatListener(JavaPlugin plugin, NationPlayerManager nationPlayerManager, NationManager nationManager, PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.nationPlayerManager = nationPlayerManager;
        this.nationManager = nationManager;
        this.playerDataManager = playerDataManager;
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
        
        // Get Essentials API
        try {
            this.essentials = (Essentials) plugin.getServer().getPluginManager().getPlugin("Essentials");
            if (this.essentials != null && isDebugEnabled()) {
                plugin.getLogger().info("Essentials integration enabled for AFK detection");
            }
        } catch (Exception e) {
            if (isDebugEnabled()) {
                plugin.getLogger().warning("Essentials not found - AFK detection disabled");
            }
            this.essentials = null;
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
                TextComponent personalizedMessage = buildPersonalizedChatComponent(player, message, onlinePlayer);
                onlinePlayer.spigot().sendMessage(personalizedMessage);
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
        
        // Format @ai mentions
        String formattedMessage = formatAIMentions(message);
        chatFormat = chatFormat.replace("{message}", formattedMessage);
        
        // Handle suffix
        String suffix = getLuckPermsSuffix(player);
        String formattedSuffix = formatSuffix(suffix);
        chatFormat = chatFormat.replace("{suffix}", formattedSuffix);
        
        // Convert color codes and return
        return ChatColor.translateAlternateColorCodes('&', chatFormat);
    }
    
    private TextComponent buildPersonalizedChatComponent(Player sender, String message, Player receiver) {
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
        
        // Format @ai mentions
        String formattedMessage = formatAIMentions(message);
        chatFormat = chatFormat.replace("{message}", formattedMessage);
        
        // Handle suffix
        String suffix = getLuckPermsSuffix(sender);
        String formattedSuffix = formatSuffix(suffix);
        chatFormat = chatFormat.replace("{suffix}", formattedSuffix);
        
        // Split the format to isolate the player name for hover functionality
        String[] parts = chatFormat.split("\\{player\\}");
        
        TextComponent finalComponent = new TextComponent();
        
        // Add the part before player name
        if (parts.length > 0) {
            TextComponent beforeName = new TextComponent(ChatColor.translateAlternateColorCodes('&', parts[0]));
            finalComponent.addExtra(beforeName);
        }
        
        // Create hoverable player name component
        TextComponent playerNameComponent = new TextComponent(sender.getName());
        playerNameComponent.setColor(net.md_5.bungee.api.ChatColor.WHITE);
        
        // Create hover text with player info
        String hoverText = buildPlayerHoverText(sender, receiver);
        playerNameComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
            new ComponentBuilder(hoverText).create()));
        
        // Add click event to suggest /msg command with random greeting
        String randomGreeting = greetings[random.nextInt(greetings.length)];
        playerNameComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, 
            "/msg " + sender.getName() + " " + randomGreeting));
        
        finalComponent.addExtra(playerNameComponent);
        
        // Add the part after player name
        if (parts.length > 1) {
            TextComponent afterName = new TextComponent(ChatColor.translateAlternateColorCodes('&', parts[1]));
            finalComponent.addExtra(afterName);
        }
        
        return finalComponent;
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
        
        // Format @ai mentions
        String formattedMessage = formatAIMentions(message);
        chatFormat = chatFormat.replace("{message}", formattedMessage);
        
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
            if (isDebugEnabled()) {
                plugin.getLogger().info("Role display disabled or role manager null");
            }
            return "";
        }
        
        // Check if roles should only be shown to same nation members
        boolean sameNationOnly = chatConfig.getBoolean("chat.roles.same_nation_only", true);
        if (sameNationOnly) {
            // Handle null nation IDs safely
            if (senderNationId == null || receiverNationId == null || !senderNationId.equals(receiverNationId)) {
                if (isDebugEnabled()) {
                    plugin.getLogger().info("Same nation only enabled - sender nation: " + senderNationId + ", receiver nation: " + receiverNationId);
                }
                return "";
            }
        }
        
        // Get sender's role
        NationRole senderRole = roleManager.getPlayerRole(sender.getUniqueId());
        if (isDebugEnabled()) {
            plugin.getLogger().info("Player " + sender.getName() + " has role: " + senderRole);
        }
        
        // Don't show citizen role if configured
        boolean showCitizen = chatConfig.getBoolean("chat.roles.show_citizen_role", false);
        if (senderRole == NationRole.CITIZEN && !showCitizen) {
            if (isDebugEnabled()) {
                plugin.getLogger().info("Citizen role hidden for " + sender.getName());
            }
            return "";
        }
        
        // Get role configuration
        String separator = chatConfig.getString("chat.roles.separator", " ");
        String roleFormat = chatConfig.getString("chat.roles.format", "&8{role}");
        
        // Get role color from configuration (fallback to default)
        String roleColor = getRoleColorFromConfig(senderRole);
        
        // Build the role display with colored role name
        String coloredRoleName = roleColor + senderRole.getDisplayName();
        String roleDisplay = roleFormat.replace("{role}", coloredRoleName);
        
        if (isDebugEnabled()) {
            plugin.getLogger().info("=== ROLE DEBUG INFO ===");
            plugin.getLogger().info("Player: " + sender.getName());
            plugin.getLogger().info("Sender Nation: " + senderNationId);
            plugin.getLogger().info("Receiver Nation: " + receiverNationId);
            plugin.getLogger().info("Same Nation Only: " + sameNationOnly);
            plugin.getLogger().info("Role Manager Available: " + (roleManager != null));
            plugin.getLogger().info("Player Role: " + senderRole);
            plugin.getLogger().info("Show Citizen Role: " + showCitizen);
            plugin.getLogger().info("Final Role Display: '" + roleDisplay + "'");
            plugin.getLogger().info("=======================");
        }
        
        return separator + roleDisplay;
    }
    
    private String getRoleColorFromConfig(NationRole role) {
        // Get color from role manager which loads from roles.yml configuration
        if (roleManager != null) {
            return roleManager.getRoleColor(role);
        }
        
        // Fallback to default color from enum if role manager is not available
        return role.getDefaultColorCode();
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
    
    private String formatAIMentions(String message) {
        // Replace all instances of @ai (case insensitive) with colored @AI
        String coloredAI = ChatColor.LIGHT_PURPLE + "@AI" + ChatColor.RESET;
        
        // Use regex to replace @ai anywhere in the message (case insensitive)
        return message.replaceAll("(?i)@ai\\b", coloredAI);
    }
    
    private String buildPlayerHoverText(Player player, Player viewer) {
        PlayerDataManager.PlayerData playerData = playerDataManager.getPlayerData(player.getUniqueId());
        String playerNationId = nationPlayerManager.getPlayerNation(player.getUniqueId());
        String viewerNationId = nationPlayerManager.getPlayerNation(viewer.getUniqueId());
        long messageSentTime = System.currentTimeMillis();
        
        StringBuilder hoverText = new StringBuilder();
        
        // Player name header - show as it appears in chat
        String chatDisplayName = buildChatDisplayName(player, viewer, playerNationId, viewerNationId);
        String coloredDisplayName = ChatColor.translateAlternateColorCodes('&', chatDisplayName);
        hoverText.append(coloredDisplayName).append("\n");
        
        // Nation information
        if (playerNationId != null && !playerNationId.isEmpty()) {
            Nation nation = nationManager.getAllNations().get(playerNationId);
            if (nation != null) {
                hoverText.append("§6Nation: §f").append(nation.getDisplayName()).append("\n");
            }
        } else {
            hoverText.append("§6Nation: §cNone\n");
        }
        
        // Role information - only show if not citizen
        if (roleManager != null) {
            NationRole role = roleManager.getPlayerRole(player.getUniqueId());
            if (role != NationRole.CITIZEN) {
                String roleColor = getRoleColorFromConfig(role);
                hoverText.append("§6Role: ").append(roleColor).append(role.getDisplayName()).append("\n");
                
                // Role assignment info
                if (playerData.getRoleAssignmentTime() > 0) {
                    hoverText.append("§7Assigned: §f").append(playerData.getFormattedRoleAssignmentTime()).append("\n");
                    if (!playerData.getAssignedBy().isEmpty()) {
                        hoverText.append("§7By: §f").append(playerData.getAssignedBy()).append("\n");
                    }
                }
            }
        }
        
        // Online time or last login
        if (player.isOnline()) {
            long sessionTime = System.currentTimeMillis() - playerData.getLastLogin();
            String onlineTime = formatDuration(sessionTime);
            hoverText.append("§6Online Since: §f").append(onlineTime).append("\n");
        } else {
            hoverText.append("§6Last Login: §f").append(playerData.getFormattedLastLogin()).append("\n");
        }
        
        // AI Usage stats
        if (playerData.getTotalRequests() > 0) {
            hoverText.append("§6AI Requests: §f").append(playerData.getTotalRequests())
                    .append(" §7(Today: §f").append(playerData.getRequestsToday()).append("§7)\n");
        }
        
        // Message timestamp - calculate time since message was sent
        long currentTime = System.currentTimeMillis();
        long timeDifference = currentTime - messageSentTime;
        String timeAgo = formatTimeAgo(timeDifference);
        hoverText.append("§7Sent ").append(timeAgo);
        
        return hoverText.toString();
    }
    
    private String buildChatDisplayName(Player player, Player viewer, String playerNationId, String viewerNationId) {
        StringBuilder displayName = new StringBuilder();
        
        // Add nation and role prefix if player has a nation
        if (playerNationId != null && !playerNationId.isEmpty()) {
            Nation nation = nationManager.getAllNations().get(playerNationId);
            if (nation != null) {
                // Add nation name
                displayName.append("§7(").append(nation.getDisplayName());
                
                // Add role if applicable
                String roleDisplay = buildRoleDisplay(player, viewer, playerNationId, viewerNationId);
                if (!roleDisplay.isEmpty()) {
                    displayName.append(roleDisplay);
                }
                
                displayName.append("§7) ");
            }
        }
        
        // Add player name
        displayName.append("§f").append(player.getName());
        
        // Add suffix if available
        String suffix = getLuckPermsSuffix(player);
        String formattedSuffix = formatSuffix(suffix);
        if (!formattedSuffix.isEmpty()) {
            displayName.append(formattedSuffix);
        }
        
        return displayName.toString();
    }
    
    private boolean isPlayerAFK(Player player) {
        if (essentials == null) {
            return false;
        }
        
        try {
            com.earth2me.essentials.User user = essentials.getUser(player);
            return user != null && user.isAfk();
        } catch (Exception e) {
            if (isDebugEnabled()) {
                plugin.getLogger().warning("Error checking AFK status for " + player.getName() + ": " + e.getMessage());
            }
            return false;
        }
    }
    
    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + "d " + (hours % 24) + "h " + (minutes % 60) + "m";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }
    
    private String formatTimeAgo(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (seconds < 60) {
            return "just now";
        } else if (minutes < 60) {
            return minutes + " min" + (minutes == 1 ? "" : "s") + " ago";
        } else if (hours < 24) {
            return hours + " hour" + (hours == 1 ? "" : "s") + " ago";
        } else {
            return days + " day" + (days == 1 ? "" : "s") + " ago";
        }
    }
}