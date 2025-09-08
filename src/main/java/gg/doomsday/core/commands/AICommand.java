package gg.doomsday.core.commands;

import gg.doomsday.core.ai.AIService;
import gg.doomsday.core.ai.PlayerStatsManager;
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
        PlayerStatsManager.PlayerAIStats stats = aiService.getPlayerStats(player.getUniqueId());
        
        player.sendMessage(ChatColor.GOLD + "=== Your AI Usage Stats ===");
        player.sendMessage(ChatColor.YELLOW + "Total Requests: " + ChatColor.WHITE + stats.getTotalRequests());
        player.sendMessage(ChatColor.YELLOW + "Requests Today: " + ChatColor.WHITE + stats.getRequestsToday());
        
        if (stats.getLastRequestTime() > 0) {
            long timeSince = (System.currentTimeMillis() - stats.getLastRequestTime()) / 1000;
            String timeStr;
            
            if (timeSince < 60) {
                timeStr = timeSince + " seconds ago";
            } else if (timeSince < 3600) {
                timeStr = (timeSince / 60) + " minutes ago";
            } else if (timeSince < 86400) {
                timeStr = (timeSince / 3600) + " hours ago";
            } else {
                timeStr = (timeSince / 86400) + " days ago";
            }
            
            player.sendMessage(ChatColor.YELLOW + "Last Request: " + ChatColor.WHITE + timeStr);
        }
        
        player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/ai clear" + ChatColor.GRAY + " to reset conversation history");
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