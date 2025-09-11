package gg.doomsday.core.commands;

import gg.doomsday.core.ai.AIService;
import gg.doomsday.core.data.PlayerDataManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AICommand implements CommandExecutor, TabCompleter {
    
    private final JavaPlugin plugin;
    private final AIService aiService;
    
    public AICommand(JavaPlugin plugin, AIService aiService) {
        this.plugin = plugin;
        this.aiService = aiService;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("ai")) {
            return false;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /ai <message>");
            player.sendMessage(ChatColor.GRAY + "Ask me anything! I'll give you a fun response.");
            player.sendMessage(ChatColor.GRAY + "Commands: /ai stats, /ai clear" + 
                (player.hasPermission("ai.reload") ? ", /ai reload" : ""));
            return true;
        }
        
        if (args[0].equalsIgnoreCase("stats")) {
            showPlayerStats(player);
            return true;
        }
        
        if (args[0].equalsIgnoreCase("clear")) {
            aiService.clearPlayerMemory(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Your conversation history has been cleared!");
            return true;
        }
        
        if (args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("ai.reload")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to reload the AI config!");
                return true;
            }
            
            boolean success = aiService.reloadConfig();
            if (success) {
                player.sendMessage(ChatColor.GREEN + "AI configuration reloaded successfully!");
            } else {
                player.sendMessage(ChatColor.RED + "Failed to reload AI configuration. Check console for errors.");
            }
            return true;
        }
        
        String message = String.join(" ", args);
        
        if (message.trim().isEmpty()) {
            player.sendMessage(ChatColor.RED + "Please provide a message to send!");
            return true;
        }
        
        if (message.length() > 500) {
            player.sendMessage(ChatColor.RED + "Your message is too long! Keep it under 500 characters.");
            return true;
        }
        
        player.sendMessage(ChatColor.GRAY + "Thinking...");
        
        aiService.processMessage(player, message).thenAccept(response -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage(response);
            });
        }).exceptionally(throwable -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage(ChatColor.RED + "Something went wrong while processing your request. Try again later!");
            });
            plugin.getLogger().warning("AI service error for player " + player.getName() + ": " + throwable.getMessage());
            return null;
        });
        
        return true;
    }
    
    private void showPlayerStats(Player player) {
        PlayerDataManager.PlayerData stats = aiService.getPlayerStats(player.getUniqueId());
        
        player.sendMessage(ChatColor.WHITE + "" + ChatColor.ITALIC + "--------------------");
        player.sendMessage(ChatColor.GOLD + "AI Usage Stats");
        
        // Get current usage for rate limiting
        String[] rateLimitInfo = aiService.getRateLimitInfo(player.getUniqueId());
        int currentHourRequests = Integer.parseInt(rateLimitInfo[1]);
        int maxPerHour = Integer.parseInt(rateLimitInfo[3]);
        
        // Show hour limit  
        ChatColor hourColor = currentHourRequests >= maxPerHour ? ChatColor.RED : ChatColor.GREEN;
        player.sendMessage(ChatColor.GRAY + "- " + ChatColor.YELLOW + "Hourly Usage: " + hourColor + currentHourRequests + ChatColor.GRAY + "/" + ChatColor.WHITE + maxPerHour);
        
        player.sendMessage(ChatColor.GRAY + "- " + ChatColor.YELLOW + "Requests Today: " + ChatColor.WHITE + stats.getRequestsToday());
        
        // Calculate time until next hour reset  
        long currentTime = System.currentTimeMillis();
        long nextHourReset = 3600 - ((currentTime / 1000) % 3600);
        long hoursLeft = nextHourReset / 3600;
        long minutesLeft = (nextHourReset % 3600) / 60;
        String hourResetStr = hoursLeft > 0 ? hoursLeft + "h " + minutesLeft + "m" : minutesLeft + "m";
        player.sendMessage(ChatColor.GRAY + "- " + ChatColor.YELLOW + "Hourly Reset: " + ChatColor.WHITE + hourResetStr);
        
        // Calculate time until daily reset (midnight)
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        long secondsUntilMidnight = java.time.Duration.between(now, midnight).getSeconds();
        long hoursUntilMidnight = secondsUntilMidnight / 3600;
        long minutesUntilMidnight = (secondsUntilMidnight % 3600) / 60;
        String dailyResetStr = hoursUntilMidnight > 0 ? hoursUntilMidnight + "h " + minutesUntilMidnight + "m" : minutesUntilMidnight + "m";
        player.sendMessage(ChatColor.GRAY + "- " + ChatColor.YELLOW + "Daily Reset: " + ChatColor.WHITE + dailyResetStr);
        
        player.sendMessage(ChatColor.WHITE + "" + ChatColor.ITALIC + "--------------------");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("ai")) {
            return null;
        }
        
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String partial = args[0].toLowerCase();
            
            List<String> options = new ArrayList<>(Arrays.asList("stats", "clear"));
            if (sender.hasPermission("ai.reload")) {
                options.add("reload");
            }
            
            for (String option : options) {
                if (option.startsWith(partial)) {
                    completions.add(option);
                }
            }
            
            return completions;
        }
        
        return new ArrayList<>();
    }
}