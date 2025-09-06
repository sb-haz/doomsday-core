package gg.doomsday.core.commands;

import gg.doomsday.core.managers.MessageManager;
import gg.doomsday.core.nations.Disaster;
import gg.doomsday.core.nations.Nation;
import gg.doomsday.core.nations.NationManager;
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
import java.util.stream.Collectors;

public class DisasterCommand implements CommandExecutor, TabCompleter {
    
    private final NationManager nationManager;
    private final MessageManager messageManager;
    private final JavaPlugin plugin;
    
    public DisasterCommand(NationManager nationManager) {
        this.nationManager = nationManager;
        // We need to get the plugin instance properly - let's pass it through constructor
        this.plugin = null; // Will fix this
        this.messageManager = new MessageManager(plugin);
    }
    
    // Add constructor that takes the plugin
    public DisasterCommand(NationManager nationManager, JavaPlugin plugin) {
        this.nationManager = nationManager;
        this.plugin = plugin;
        this.messageManager = new MessageManager(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("disaster")) {
            return false;
        }
        
        if (!sender.hasPermission("rocket.disaster")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to manage disasters!");
            return true;
        }
        
        if (args.length == 0) {
            showUsage(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "trigger":
                return handleTriggerCommand(sender, args);
                
            case "stop":
                return handleStopCommand(sender, args);
                
            case "status":
                return handleStatusCommand(sender, args);
                
            case "list":
                return handleListCommand(sender);
                
            case "reload":
                return handleReloadCommand(sender);
                
            case "auto":
                return handleAutoCommand(sender, args);
                
            default:
                showUsage(sender);
                return true;
        }
    }
    
    private boolean handleTriggerCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /disaster trigger <nation> <disaster_type>");
            return true;
        }
        
        String nationId = args[1].toLowerCase();
        String disasterId = args[2].toLowerCase();
        
        Nation nation = nationManager.getAllNations().get(nationId);
        if (nation == null) {
            sender.sendMessage(ChatColor.RED + "Nation '" + nationId + "' not found!");
            sender.sendMessage(ChatColor.GRAY + "Available nations: " + 
                String.join(", ", nationManager.getAllNations().keySet()));
            return true;
        }
        
        Disaster disaster = nation.getDisasters().get(disasterId);
        if (disaster == null) {
            sender.sendMessage(ChatColor.RED + "Disaster '" + disasterId + "' not found in " + nation.getDisplayName() + "!");
            sender.sendMessage(ChatColor.GRAY + "Available disasters: " + 
                String.join(", ", nation.getDisasters().keySet()));
            return true;
        }
        
        if (disaster.isActive()) {
            sender.sendMessage(ChatColor.YELLOW + "Warning: " + disasterId + " is already active in " + nation.getDisplayName() + "!");
            sender.sendMessage(ChatColor.GRAY + "Time remaining: " + 
                ((disaster.getEndTime() - System.currentTimeMillis()) / 1000) + " seconds");
            return true;
        }
        
        // Manually trigger the disaster
        try {
            nationManager.manuallyTriggerDisaster(nation, disaster);
            
            sender.sendMessage(ChatColor.GREEN + "✅ Successfully triggered " + 
                ChatColor.BOLD + disasterId.replace("_", " ").toUpperCase() + 
                ChatColor.GREEN + " in " + ChatColor.BOLD + nation.getDisplayName());
            sender.sendMessage(ChatColor.GRAY + "Duration: " + disaster.getDuration() + " ticks (" + 
                (disaster.getDuration() / 20) + " seconds)");
                
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Failed to trigger disaster: " + e.getMessage());
            return true;
        }
        
        return true;
    }
    
    private boolean handleStopCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /disaster stop <nation> <disaster_type>");
            return true;
        }
        
        String nationId = args[1].toLowerCase();
        String disasterId = args[2].toLowerCase();
        
        Nation nation = nationManager.getAllNations().get(nationId);
        if (nation == null) {
            sender.sendMessage(ChatColor.RED + "Nation '" + nationId + "' not found!");
            return true;
        }
        
        Disaster disaster = nation.getDisasters().get(disasterId);
        if (disaster == null) {
            sender.sendMessage(ChatColor.RED + "Disaster '" + disasterId + "' not found in " + nation.getDisplayName() + "!");
            return true;
        }
        
        if (!disaster.isActive()) {
            sender.sendMessage(ChatColor.YELLOW + "Disaster '" + disasterId + "' is not currently active in " + nation.getDisplayName() + "!");
            return true;
        }
        
        // Stop the disaster
        try {
            nationManager.manuallyStopDisaster(nation, disaster);
            
            sender.sendMessage(ChatColor.GREEN + "✅ Successfully stopped " + 
                ChatColor.BOLD + disasterId.replace("_", " ").toUpperCase() + 
                ChatColor.GREEN + " in " + ChatColor.BOLD + nation.getDisplayName());
                
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Failed to stop disaster: " + e.getMessage());
            return true;
        }
        
        return true;
    }
    
    private boolean handleStatusCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            // Show status for all nations
            sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "=== DISASTER STATUS (ALL NATIONS) ===");
            
            boolean hasActiveDisasters = false;
            for (Nation nation : nationManager.getAllNations().values()) {
                List<String> activeDisasters = new ArrayList<>();
                
                for (Disaster disaster : nation.getDisasters().values()) {
                    if (disaster.isActive()) {
                        long timeLeft = (disaster.getEndTime() - System.currentTimeMillis()) / 1000;
                        activeDisasters.add(disaster.getId().replace("_", " ") + " (" + timeLeft + "s)");
                        hasActiveDisasters = true;
                    }
                }
                
                if (!activeDisasters.isEmpty()) {
                    sender.sendMessage(ChatColor.YELLOW + nation.getDisplayName() + ChatColor.GRAY + ": " + 
                        ChatColor.RED + String.join(", ", activeDisasters));
                }
            }
            
            if (!hasActiveDisasters) {
                sender.sendMessage(ChatColor.GREEN + "No active disasters in any nation");
            }
            
        } else {
            // Show status for specific nation
            String nationId = args[1].toLowerCase();
            Nation nation = nationManager.getAllNations().get(nationId);
            
            if (nation == null) {
                sender.sendMessage(ChatColor.RED + "Nation '" + nationId + "' not found!");
                return true;
            }
            
            sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "=== DISASTER STATUS: " + 
                nation.getDisplayName().toUpperCase() + " ===");
            
            boolean hasActiveDisasters = false;
            for (Disaster disaster : nation.getDisasters().values()) {
                if (disaster.isActive()) {
                    long timeLeft = (disaster.getEndTime() - System.currentTimeMillis()) / 1000;
                    sender.sendMessage(ChatColor.RED + "🔥 " + disaster.getId().replace("_", " ").toUpperCase() + 
                        ChatColor.GRAY + " - " + timeLeft + " seconds remaining");
                    hasActiveDisasters = true;
                } else {
                    long nextCheck = (disaster.getNextCheck() - System.currentTimeMillis()) / 1000;
                    if (nextCheck > 0) {
                        sender.sendMessage(ChatColor.GREEN + "⭕ " + disaster.getId().replace("_", " ").toUpperCase() + 
                            ChatColor.GRAY + " - next check in " + nextCheck + " seconds");
                    } else {
                        sender.sendMessage(ChatColor.GREEN + "⭕ " + disaster.getId().replace("_", " ").toUpperCase() + 
                            ChatColor.GRAY + " - ready for check");
                    }
                }
            }
            
            if (!hasActiveDisasters) {
                sender.sendMessage(ChatColor.GREEN + "No active disasters");
            }
        }
        
        return true;
    }
    
    private boolean handleListCommand(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "=== AVAILABLE DISASTERS ===");
        
        for (Nation nation : nationManager.getAllNations().values()) {
            sender.sendMessage("");
            sender.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + nation.getDisplayName() + ChatColor.GRAY + ":");
            
            for (Disaster disaster : nation.getDisasters().values()) {
                String status = disaster.isEnabled() ? ChatColor.GREEN + "✅" : ChatColor.RED + "❌";
                sender.sendMessage(ChatColor.GRAY + "  " + status + ChatColor.WHITE + " " + 
                    disaster.getId().replace("_", " ") + 
                    ChatColor.GRAY + " (probability: " + (int)(disaster.getProbability() * 100) + "%)");
            }
        }
        
        return true;
    }
    
    private boolean handleReloadCommand(CommandSender sender) {
        try {
            nationManager.reload();
            sender.sendMessage(ChatColor.GREEN + "✅ Successfully reloaded disaster configuration!");
            sender.sendMessage(ChatColor.GRAY + "All active disasters have been stopped and configurations reloaded.");
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "❌ Failed to reload disaster configuration: " + e.getMessage());
        }
        
        return true;
    }
    
    private boolean handleAutoCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            // Show current status
            boolean enabled = nationManager.areAutomaticDisastersEnabled();
            String statusMsg = enabled ? messageManager.getMessage("disasters.command.status_enabled") : 
                                       messageManager.getMessage("disasters.command.status_disabled");
            sender.sendMessage(statusMsg);
            return true;
        }
        
        String action = args[1].toLowerCase();
        
        switch (action) {
            case "enable":
            case "on":
                nationManager.enableAutomaticDisasters();
                sender.sendMessage(messageManager.getMessage("disasters.command.auto_enabled"));
                break;
                
            case "disable":
            case "off":
                nationManager.disableAutomaticDisasters();
                sender.sendMessage(messageManager.getMessage("disasters.command.auto_disabled"));
                break;
                
            case "status":
                boolean enabled = nationManager.areAutomaticDisastersEnabled();
                String statusMsg = enabled ? messageManager.getMessage("disasters.command.status_enabled") : 
                                           messageManager.getMessage("disasters.command.status_disabled");
                sender.sendMessage(statusMsg);
                break;
                
            default:
                sender.sendMessage(ChatColor.RED + "Usage: /disaster auto <enable|disable|status>");
                break;
        }
        
        return true;
    }
    
    private void showUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "=== DISASTER COMMANDS ===");
        sender.sendMessage(ChatColor.YELLOW + "/disaster trigger <nation> <disaster_type>" + ChatColor.GRAY + " - Manually trigger a disaster");
        sender.sendMessage(ChatColor.YELLOW + "/disaster stop <nation> <disaster_type>" + ChatColor.GRAY + " - Stop an active disaster");
        sender.sendMessage(ChatColor.YELLOW + "/disaster status [nation]" + ChatColor.GRAY + " - Show disaster status");
        sender.sendMessage(ChatColor.YELLOW + "/disaster list" + ChatColor.GRAY + " - List all available disasters");
        sender.sendMessage(ChatColor.YELLOW + "/disaster auto <enable|disable|status>" + ChatColor.GRAY + " - Control automatic disasters");
        sender.sendMessage(ChatColor.YELLOW + "/disaster reload" + ChatColor.GRAY + " - Reload disaster configuration");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "Available nations: " + 
            String.join(", ", nationManager.getAllNations().keySet()));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("rocket.disaster")) {
            return new ArrayList<>();
        }
        
        if (args.length == 1) {
            return Arrays.asList("trigger", "stop", "status", "list", "auto", "reload")
                .stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("trigger") || subCommand.equals("stop") || subCommand.equals("status")) {
                return nationManager.getAllNations().keySet()
                    .stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
            
            if (subCommand.equals("auto")) {
                return Arrays.asList("enable", "disable", "status", "on", "off")
                    .stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }
        
        if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            String nationId = args[1].toLowerCase();
            
            if (subCommand.equals("trigger") || subCommand.equals("stop")) {
                Nation nation = nationManager.getAllNations().get(nationId);
                if (nation != null) {
                    return nation.getDisasters().keySet()
                        .stream()
                        .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
                }
            }
        }
        
        return new ArrayList<>();
    }
}