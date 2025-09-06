package gg.doomsday.core.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import gg.doomsday.core.seasons.Season;
import gg.doomsday.core.seasons.SeasonManager;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Handles all /season commands
 */
public class SeasonCommand implements CommandExecutor, TabCompleter {
    
    private final SeasonManager seasonManager;
    
    public SeasonCommand(SeasonManager seasonManager) {
        this.seasonManager = seasonManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("seasons")) {
            return false;
        }
        
        if (args.length == 0) {
            showUsage(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "create":
                return handleCreate(sender, args);
            case "activate":
                return handleActivate(sender);
            case "archive":
                return handleArchive(sender);
            case "current":
                return handleCurrent(sender);
            default:
                showUsage(sender);
                return true;
        }
    }
    
    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("season.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to manage seasons!");
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /seasons create <id>");
            sender.sendMessage(ChatColor.GRAY + "Example: /seasons create 2");
            return true;
        }
        
        try {
            int id = Integer.parseInt(args[1]);
            String displayName = "Season " + id;
            Instant startAt = Instant.now(); // Current time as start time
            Instant endAt = Instant.now().plusSeconds(7 * 24 * 60 * 60); // 1 week from now
            
            boolean success = seasonManager.createSeason(id, displayName, startAt, endAt);
            
            if (success) {
                sender.sendMessage(ChatColor.GREEN + "✓ Created Season " + id + ": " + displayName);
                sender.sendMessage(ChatColor.GRAY + "Start: " + startAt);
                sender.sendMessage(ChatColor.GRAY + "End: " + endAt);
                sender.sendMessage(ChatColor.GRAY + "Status: PLANNED");
                sender.sendMessage(ChatColor.YELLOW + "Use '/seasons activate' to activate when ready");
            } else {
                sender.sendMessage(ChatColor.RED + "Failed to create season!");
            }
            
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid season ID - must be a number!");
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Error creating season: " + e.getMessage());
        }
        
        return true;
    }
    
    private boolean handleActivate(CommandSender sender) {
        if (!sender.hasPermission("season.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to manage seasons!");
            return true;
        }
        
        Season currentSeason = seasonManager.getCurrentSeason();
        if (currentSeason == null) {
            sender.sendMessage(ChatColor.RED + "No season configured! Use '/seasons create' first.");
            return true;
        }
        
        if (currentSeason.isActive()) {
            sender.sendMessage(ChatColor.YELLOW + "Season " + currentSeason.getId() + " is already active!");
            return true;
        }
        
        if (!currentSeason.canActivate()) {
            sender.sendMessage(ChatColor.RED + "Cannot activate season:");
            if (!currentSeason.hasStarted()) {
                sender.sendMessage(ChatColor.RED + "- Season hasn't started yet");
            }
            if (currentSeason.isArchived()) {
                sender.sendMessage(ChatColor.RED + "- Season is already archived");
            }
            return true;
        }
        
        boolean success = seasonManager.activateSeason();
        
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "✓ Activated Season " + currentSeason.getId());
            sender.sendMessage(ChatColor.GREEN + "The season is now LIVE!");
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to activate season!");
        }
        
        return true;
    }
    
    private boolean handleArchive(CommandSender sender) {
        if (!sender.hasPermission("season.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to manage seasons!");
            return true;
        }
        
        Season currentSeason = seasonManager.getCurrentSeason();
        if (currentSeason == null) {
            sender.sendMessage(ChatColor.RED + "No season to archive!");
            return true;
        }
        
        boolean success = seasonManager.archiveSeason();
        
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "✓ Archived Season " + currentSeason.getId());
            sender.sendMessage(ChatColor.YELLOW + "Players will now see 'no active season' message on join");
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to archive season!");
        }
        
        return true;
    }
    
    private boolean handleCurrent(CommandSender sender) {
        Season currentSeason = seasonManager.getCurrentSeason();
        
        if (currentSeason == null) {
            sender.sendMessage(ChatColor.RED + "No season configured");
            return true;
        }
        
        sender.sendMessage(ChatColor.GOLD + "=== Season " + currentSeason.getId() + " ===");
        sender.sendMessage(ChatColor.WHITE + currentSeason.getDisplayName());
        sender.sendMessage(ChatColor.GRAY + "Status: " + getStatusColor(currentSeason.getStatus()) + currentSeason.getStatus());
        sender.sendMessage(ChatColor.GRAY + "Start: " + ChatColor.WHITE + currentSeason.getStartAtFormatted());
        sender.sendMessage(ChatColor.GRAY + "End: " + ChatColor.WHITE + currentSeason.getEndAtFormatted());
        
        if (currentSeason.isActive()) {
            long timeLeft = currentSeason.getTimeUntilEnd();
            if (timeLeft > 0) {
                String timeLeftFormatted = formatDuration(timeLeft);
                sender.sendMessage(ChatColor.GREEN + "Time remaining: " + ChatColor.WHITE + timeLeftFormatted);
            } else {
                sender.sendMessage(ChatColor.RED + "Season has ended!");
            }
        } else if (currentSeason.isPlanned()) {
            long timeUntilStart = currentSeason.getTimeUntilStart();
            if (timeUntilStart > 0) {
                String timeUntilStartFormatted = formatDuration(timeUntilStart);
                sender.sendMessage(ChatColor.YELLOW + "Starts in: " + ChatColor.WHITE + timeUntilStartFormatted);
            } else {
                sender.sendMessage(ChatColor.GREEN + "Ready to activate!");
            }
        }
        
        return true;
    }
    
    private void showUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Season Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/seasons current" + ChatColor.GRAY + " - Show current season info");
        
        if (sender.hasPermission("season.admin")) {
            sender.sendMessage(ChatColor.YELLOW + "/seasons create <id>" + ChatColor.GRAY + " - Create new season");
            sender.sendMessage(ChatColor.YELLOW + "/seasons activate" + ChatColor.GRAY + " - Activate planned season");
            sender.sendMessage(ChatColor.YELLOW + "/seasons archive" + ChatColor.GRAY + " - Archive current season");
        }
    }
    
    private ChatColor getStatusColor(Season.Status status) {
        switch (status) {
            case ACTIVE:
                return ChatColor.GREEN;
            case PLANNED:
                return ChatColor.YELLOW;
            case ARCHIVED:
                return ChatColor.GRAY;
            default:
                return ChatColor.WHITE;
        }
    }
    
    private String formatDuration(long millis) {
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        
        if (days > 0) {
            return String.format("%dd %02dh %02dm", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%dh %02dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "activate", "archive", "current");
        }
        return null;
    }
}