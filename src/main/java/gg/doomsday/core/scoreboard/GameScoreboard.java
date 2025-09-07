package gg.doomsday.core.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import gg.doomsday.core.nations.Nation;
import gg.doomsday.core.nations.NationManager;
import gg.doomsday.core.nations.NationPlayerManager;
import gg.doomsday.core.seasons.Season;
import gg.doomsday.core.seasons.SeasonManager;
import gg.doomsday.core.utils.NationColors;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Efficient scoreboard system showing season info and nation statistics
 */
public class GameScoreboard {
    
    private final JavaPlugin plugin;
    private final SeasonManager seasonManager;
    private final NationManager nationManager;
    private final NationPlayerManager nationPlayerManager;
    
    private ScoreboardManager scoreboardManager;
    private Scoreboard mainScoreboard;
    private Objective sidebarObjective;
    
    private BukkitRunnable updateTask;
    private final Map<String, Integer> cachedNationCounts = new HashMap<>();
    private long lastNationCountUpdate = 0;
    private static final long NATION_COUNT_CACHE_TIME = 5000; // 5 seconds
    
    public GameScoreboard(JavaPlugin plugin, SeasonManager seasonManager, 
                         NationManager nationManager, NationPlayerManager nationPlayerManager) {
        this.plugin = plugin;
        this.seasonManager = seasonManager;
        this.nationManager = nationManager;
        this.nationPlayerManager = nationPlayerManager;
        
        initializeScoreboard();
        startUpdateTask();
    }
    
    private void initializeScoreboard() {
        scoreboardManager = Bukkit.getScoreboardManager();
        mainScoreboard = scoreboardManager.getNewScoreboard();
        
        // Create main objective for sidebar - title will be updated dynamically
        sidebarObjective = mainScoreboard.registerNewObjective("sidebar", "dummy", 
            ChatColor.GOLD + "" + ChatColor.BOLD + "DOOMSDAY");
        sidebarObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        plugin.getLogger().info("Initialized game scoreboard");
    }
    
    private void startUpdateTask() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateScoreboard();
            }
        };
        
        // Update every 1 second for countdown, every 5 seconds for nation counts
        updateTask.runTaskTimer(plugin, 20L, 20L);
    }
    
    private void updateScoreboard() {
        // Clear existing entries and teams
        for (String entry : sidebarObjective.getScoreboard().getEntries()) {
            sidebarObjective.getScoreboard().resetScores(entry);
        }
        
        // Clear all teams to prevent conflicts
        for (Team team : mainScoreboard.getTeams()) {
            team.unregister();
        }
        
        // Update title based on season
        Season currentSeason = seasonManager.getCurrentSeason();
        if (currentSeason != null && currentSeason.isActive()) {
            String title = ChatColor.WHITE + "" + ChatColor.BOLD + "DOOMSDAY " + ChatColor.RED + "" + ChatColor.BOLD + "S" + currentSeason.getId();
            sidebarObjective.setDisplayName(title);
        } else {
            sidebarObjective.setDisplayName(ChatColor.WHITE + "" + ChatColor.BOLD + "DOOMSDAY");
        }
        
        int line = 15; // Start from bottom

        // Season date range - only show if ACTIVE
        if (currentSeason != null && currentSeason.isActive()) {
            String startDate = formatShortDate(currentSeason.getStartAtFormatted());
            String endDate = formatShortDate(currentSeason.getEndAtFormatted());
            addLine(ChatColor.GRAY + "(" + startDate + " - " + endDate + ")", line--);
        }

        // Season countdown - only show if ACTIVE and has end date
        if (currentSeason != null && currentSeason.isActive() && currentSeason.getEndAt() != null) {
            addLine("", line--); // Spacer above countdown
            
            long timeUntilEnd = currentSeason.getTimeUntilEnd();
            if (timeUntilEnd > 0) {
                String countdown = formatDuration(timeUntilEnd);
                addLine(ChatColor.WHITE + "Countdown: " + ChatColor.GRAY + countdown, line--);
            } else {
                addLine(ChatColor.RED + "Season Ended", line--);
            }
            
            addLine("", line--); // Spacer below countdown
        } else {
            addLine("", line--); // Spacer
        }

        // Nation player counts (ordered by highest to lowest)
        updateNationCounts();
        final int[] currentLine = {line}; // Use array to allow modification in lambda
        nationManager.getAllNations().entrySet().stream()
            .sorted((entry1, entry2) -> {
                int count1 = cachedNationCounts.getOrDefault(entry1.getKey(), 0);
                int count2 = cachedNationCounts.getOrDefault(entry2.getKey(), 0);
                return Integer.compare(count2, count1); // Descending order (highest first)
            })
            .forEach(entry -> {
                String nationId = entry.getKey();
                Nation nation = entry.getValue();
                
                int playerCount = cachedNationCounts.getOrDefault(nationId, 0);
                String displayName = nation.getDisplayName();
                
                // Truncate long names
                if (displayName.length() > 10) {
                    displayName = displayName.substring(0, 9) + "…";
                }
                
                ChatColor color = getNationColor(nationId);
                addLine(ChatColor.DARK_GRAY + "- " + color + displayName + ": " + ChatColor.WHITE + playerCount, currentLine[0]--);
            });
        line = currentLine[0];
        
        addLine("", line--); // Spacer

        // Bottom: play.doomsday.gg
        addLine(ChatColor.RED + "play.doomsday.gg", line--);
    }
    
    private void addLine(String text, int score) {
        // Handle duplicate text by adding invisible characters
        while (sidebarObjective.getScore(text).isScoreSet()) {
            text += ChatColor.RESET;
        }
        
        // Alternative method: Use teams to hide scores on older versions
        String teamName = "line" + score;
        Team team = mainScoreboard.getTeam(teamName);
        if (team == null) {
            team = mainScoreboard.registerNewTeam(teamName);
        }
        
        // Use a unique player name for this line
        String playerName = ChatColor.values()[Math.abs(score) % ChatColor.values().length].toString();
        if (playerName.length() > 16) {
            playerName = playerName.substring(0, 16);
        }
        
        // Set the text as team prefix (supports up to 64 chars in newer versions)
        team.setPrefix(text);
        team.setSuffix("");
        team.addEntry(playerName);
        
        Score scoreEntry = sidebarObjective.getScore(playerName);
        scoreEntry.setScore(score);
    }
    
    private void updateNationCounts() {
        long currentTime = System.currentTimeMillis();
        
        // Only update nation counts every 5 seconds for performance
        if (currentTime - lastNationCountUpdate < NATION_COUNT_CACHE_TIME) {
            return;
        }
        
        // Use the centralized tracking instead of scanning all players
        cachedNationCounts.clear();
        cachedNationCounts.putAll(nationPlayerManager.getOnlinePlayerCountsByNation());
        
        lastNationCountUpdate = currentTime;
    }
    
    private ChatColor getNationColor(String nationId) {
        switch (nationId.toLowerCase()) {
            case "america":
                return ChatColor.AQUA; // &b
            case "africa":
                return ChatColor.GOLD; // &6
            case "antarctica":
                return ChatColor.WHITE; // &f
            case "europe":
                return ChatColor.GREEN; // &a
            case "asia":
                return ChatColor.YELLOW; // &e
            default:
                return ChatColor.GRAY;
        }
    }
    
    private String formatDuration(long millis) {
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        
        if (days > 0) {
            return String.format("%dd %02dh", days, hours);
        } else if (hours > 0) {
            return String.format("%dh %02dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%dm %02ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    private String formatShortDate(String isoDate) {
        try {
            // Parse "2025-09-09T18:00:00Z" format
            String[] parts = isoDate.split("T")[0].split("-");
            if (parts.length >= 3) {
                int month = Integer.parseInt(parts[1]);
                int day = Integer.parseInt(parts[2]);
                
                String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                                     "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                
                if (month >= 1 && month <= 12) {
                    return monthNames[month - 1] + " " + String.format("%02d", day);
                }
            }
        } catch (Exception e) {
            // Fallback to showing raw date if parsing fails
        }
        return isoDate.split("T")[0]; // Return YYYY-MM-DD as fallback
    }
    
    /**
     * Show scoreboard to a player
     */
    public void showToPlayer(Player player) {
        player.setScoreboard(mainScoreboard);
    }
    
    /**
     * Show scoreboard to all online players
     */
    public void showToAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            showToPlayer(player);
        }
    }
    
    /**
     * Remove scoreboard from a player
     */
    public void hideFromPlayer(Player player) {
        player.setScoreboard(scoreboardManager.getMainScoreboard());
    }
    
    /**
     * Shutdown the scoreboard system
     */
    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        
        // Reset all players to main scoreboard
        for (Player player : Bukkit.getOnlinePlayers()) {
            hideFromPlayer(player);
        }
        
        plugin.getLogger().info("Shutdown game scoreboard");
    }
    
    /**
     * Refresh the scoreboard immediately
     */
    public void forceUpdate() {
        lastNationCountUpdate = 0; // Force nation count update
        updateScoreboard();
    }
}