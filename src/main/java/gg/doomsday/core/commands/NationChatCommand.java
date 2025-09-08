package gg.doomsday.core.commands;

import gg.doomsday.core.nations.Nation;
import gg.doomsday.core.nations.NationManager;
import gg.doomsday.core.nations.NationPlayerManager;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedDataManager;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class NationChatCommand implements CommandExecutor, TabCompleter {
    
    private final JavaPlugin plugin;
    private final NationPlayerManager nationPlayerManager;
    private final NationManager nationManager;
    private LuckPerms luckPerms;
    private FileConfiguration chatConfig;
    
    public NationChatCommand(JavaPlugin plugin, NationPlayerManager nationPlayerManager, NationManager nationManager) {
        this.plugin = plugin;
        this.nationPlayerManager = nationPlayerManager;
        this.nationManager = nationManager;
        
        // Load chat configuration
        loadChatConfig();
        
        // Get LuckPerms API
        try {
            this.luckPerms = LuckPermsProvider.get();
        } catch (IllegalStateException e) {
            this.luckPerms = null;
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("n")) {
            return false;
        }
        
        // Check if nation chat is enabled
        if (!chatConfig.getBoolean("nation_chat.enabled", true)) {
            return false; // Let other plugins handle /n if nation chat is disabled
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use nation chat!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            String usage = chatConfig.getString("nation_chat.messages.usage", "&cUsage: /n <message>");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', usage));
            return true;
        }
        
        // Get player's nation
        String nationId = nationPlayerManager.getPlayerNation(player.getUniqueId());
        if (nationId == null) {
            String noNation = chatConfig.getString("nation_chat.messages.no_nation", "&cYou must be in a nation to use nation chat!");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', noNation));
            return true;
        }
        
        Nation nation = nationManager.getAllNations().get(nationId);
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Your nation could not be found!");
            return true;
        }
        
        // Build the message
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            messageBuilder.append(args[i]);
            if (i < args.length - 1) {
                messageBuilder.append(" ");
            }
        }
        String message = messageBuilder.toString();
        
        // Format the nation chat message
        String formattedMessage = buildNationChatMessage(player, nation, message);
        
        // Send to all online players in the same nation
        Set<UUID> onlinePlayersInNation = nationPlayerManager.getOnlinePlayersInNation(nationId);
        int sentCount = 0;
        
        for (UUID playerId : onlinePlayersInNation) {
            Player nationPlayer = Bukkit.getPlayer(playerId);
            if (nationPlayer != null && nationPlayer.isOnline()) {
                nationPlayer.sendMessage(formattedMessage);
                sentCount++;
            }
        }
        
        // Log to console if enabled
        if (chatConfig.getBoolean("nation_chat.console.enabled", true)) {
            String consoleMessage = formattedMessage;
            if (!chatConfig.getBoolean("nation_chat.console.include_colors", false)) {
                consoleMessage = ChatColor.stripColor(formattedMessage);
            }
            String consolePrefix = chatConfig.getString("nation_chat.console.prefix", "[NATION-CHAT]");
            plugin.getLogger().info(consolePrefix + " " + consoleMessage);
        }
        
        // If no one else received the message, let the sender know
        if (sentCount <= 1) { // Only the sender
            String noRecipients = chatConfig.getString("nation_chat.messages.no_recipients", "&eNo other nation members are online to receive your message.");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', noRecipients));
        }
        
        return true;
    }
    
    private String buildNationChatMessage(Player player, Nation nation, String message) {
        // Get format from config
        String chatFormat = chatConfig.getString("nation_chat.format", "&6[NATION] &f{player}{suffix}&7: &f{message}");
        
        // Replace placeholders
        chatFormat = chatFormat.replace("{player}", player.getName());
        chatFormat = chatFormat.replace("{message}", message);
        chatFormat = chatFormat.replace("{nation}", nation.getDisplayName());
        
        // Handle suffix
        String suffix = getLuckPermsSuffix(player);
        String formattedSuffix = formatSuffix(suffix);
        chatFormat = chatFormat.replace("{suffix}", formattedSuffix);
        
        // Convert color codes and return
        return ChatColor.translateAlternateColorCodes('&', chatFormat);
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
            return "";
        }
    }
    
    private String formatSuffix(String suffix) {
        if (suffix == null || suffix.isEmpty()) {
            return "";
        }
        
        StringBuilder formattedSuffix = new StringBuilder();
        
        // Add space before if configured
        if (chatConfig.getBoolean("nation_chat.suffix.add_space_before", true)) {
            formattedSuffix.append(" ");
        }
        
        // Add prefix
        String suffixPrefix = chatConfig.getString("nation_chat.suffix.prefix", "");
        if (!suffixPrefix.isEmpty()) {
            formattedSuffix.append(suffixPrefix);
        }
        
        // Add the suffix itself
        formattedSuffix.append(suffix);
        
        // Add suffix
        String suffixSuffix = chatConfig.getString("nation_chat.suffix.suffix", "");
        if (!suffixSuffix.isEmpty()) {
            formattedSuffix.append(suffixSuffix);
        }
        
        return formattedSuffix.toString();
    }
    
    private void loadChatConfig() {
        File chatConfigFile = new File(plugin.getDataFolder(), "chat.yml");
        
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
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // No tab completion for chat messages
        return new ArrayList<>();
    }
}