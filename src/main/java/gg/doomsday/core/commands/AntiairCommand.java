package gg.doomsday.core.commands;

import gg.doomsday.core.defense.AntiAirDefense;
import gg.doomsday.core.defense.AntiAirDefenseManager;
import gg.doomsday.core.managers.MessageManager;
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

public class AntiairCommand implements CommandExecutor, TabCompleter {
    
    private final JavaPlugin plugin;
    private final AntiAirDefenseManager antiAirManager;
    private final MessageManager messageManager;
    
    public AntiairCommand(JavaPlugin plugin, AntiAirDefenseManager antiAirManager, MessageManager messageManager) {
        this.plugin = plugin;
        this.antiAirManager = antiAirManager;
        this.messageManager = messageManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("antiair")) {
            return false;
        }
        
        if (!sender.hasPermission("rocket.antiair")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to manage anti-air defenses!");
            return true;
        }
        
        if (args.length == 0) {
            showUsage(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "status":
                return handleStatus(sender);
            case "reload":
                return handleReload(sender);
            case "online":
                return handleOnline(sender, args);
            case "offline":
                return handleOffline(sender, args);
            case "list":
                return handleList(sender);
            default:
                showUsage(sender);
                return true;
        }
    }
    
    private boolean handleStatus(CommandSender sender) {
        List<String> statusLines = antiAirManager.getDefenseStatus();
        
        sender.sendMessage(messageManager.getMessage("commands.antiair.status_header"));
        
        if (statusLines.isEmpty()) {
            sender.sendMessage(messageManager.getMessage("commands.antiair.no_defenses"));
        } else {
            for (String line : statusLines) {
                sender.sendMessage(line);
            }
        }
        
        return true;
    }
    
    private boolean handleReload(CommandSender sender) {
        try {
            // Reload config files first
            ((gg.doomsday.core.DoomsdayCore) plugin).getConfigManager().reloadConfig("antiair");
            
            // Reload defense configurations from the updated config
            antiAirManager.reloadDefenses();
            
            sender.sendMessage(ChatColor.GREEN + "Anti-air configurations reloaded successfully!");
            sender.sendMessage(ChatColor.GRAY + "Loaded " + antiAirManager.getDefenses().size() + " defense systems");
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Failed to reload anti-air configurations: " + e.getMessage());
            plugin.getLogger().warning("Anti-air reload failed: " + e.getMessage());
        }
        return true;
    }
    
    private boolean handleOnline(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /antiair online <defense_name>");
            return true;
        }
        
        String defenseName = args[1];
        antiAirManager.setDefenseOperational(defenseName, true);
        sender.sendMessage(messageManager.getMessage("commands.antiair.online").replace("{name}", defenseName));
        return true;
    }
    
    private boolean handleOffline(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /antiair offline <defense_name>");
            return true;
        }
        
        String defenseName = args[1];
        antiAirManager.setDefenseOperational(defenseName, false);
        sender.sendMessage(messageManager.getMessage("commands.antiair.offline").replace("{name}", defenseName));
        return true;
    }
    
    private boolean handleList(CommandSender sender) {
        List<AntiAirDefense> defenses = antiAirManager.getDefenses();
        
        sender.sendMessage(messageManager.getMessage("commands.antiair.list_header"));
        
        if (defenses.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No anti-air defenses configured");
            return true;
        }
        
        for (AntiAirDefense defense : defenses) {
            String status = defense.isOperational() ? ChatColor.GREEN + "ONLINE" : ChatColor.RED + "OFFLINE";
            String automatic = defense.isAutomatic() ? ChatColor.YELLOW + "AUTO" : ChatColor.GRAY + "MANUAL";
            
            sender.sendMessage("");
            sender.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + defense .getDisplayName());
            sender.sendMessage(ChatColor.GRAY + "  Status: " + status);
            sender.sendMessage(ChatColor.GRAY + "  Mode: " + automatic);
            sender.sendMessage(ChatColor.GRAY + "  Range: " + ChatColor.WHITE + defense.getRange() + " blocks");
            sender.sendMessage(ChatColor.GRAY + "  Accuracy: " + ChatColor.WHITE + (int)(defense.getAccuracy() * 100) + "%");
            sender.sendMessage(ChatColor.GRAY + "  Location: " + ChatColor.WHITE + 
                defense.getLocation().getBlockX() + ", " + 
                defense.getLocation().getBlockY() + ", " + 
                defense.getLocation().getBlockZ());
        }
        
        return true;
    }
    
    private void showUsage(CommandSender sender) {
        sender.sendMessage(messageManager.getMessage("commands.antiair.usage_header"));
        sender.sendMessage(ChatColor.YELLOW + "/antiair status" + ChatColor.GRAY + " - View defense system status");
        sender.sendMessage(ChatColor.YELLOW + "/antiair list" + ChatColor.GRAY + " - List all defense systems");
        sender.sendMessage(ChatColor.YELLOW + "/antiair online <name>" + ChatColor.GRAY + " - Bring defense system online");
        sender.sendMessage(ChatColor.YELLOW + "/antiair offline <name>" + ChatColor.GRAY + " - Take defense system offline");
        sender.sendMessage(ChatColor.YELLOW + "/antiair reload" + ChatColor.GRAY + " - Reload defense configurations");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "Available defenses:");
        for (AntiAirDefense defense : antiAirManager.getDefenses()) {
            String status = defense.isOperational() ? ChatColor.GREEN + "●" : ChatColor.RED + "●";
            sender.sendMessage(ChatColor.GRAY + "  " + status + " " + ChatColor.WHITE + defense.getName());
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("rocket.antiair")) {
            return new ArrayList<>();
        }
        
        if (args.length == 1) {
            return Arrays.asList("status", "list", "online", "offline", "reload")
                .stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("online") || subCommand.equals("offline")) {
                return antiAirManager.getDefenses()
                    .stream()
                    .map(AntiAirDefense::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }
        
        return new ArrayList<>();
    }
}