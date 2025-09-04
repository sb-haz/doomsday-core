package gg.doomsday.core.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import gg.doomsday.core.seasons.Season;
import gg.doomsday.core.seasons.SeasonManager;
import gg.doomsday.core.scoreboard.GameScoreboard;

import java.util.Objects;

/**
 * Handles player join events for season notifications
 */
public class PlayerJoinListener implements Listener {
    
    private final SeasonManager seasonManager;
    private final GameScoreboard gameScoreboard;
    
    public PlayerJoinListener(SeasonManager seasonManager, GameScoreboard gameScoreboard) {
        this.seasonManager = seasonManager;
        this.gameScoreboard = gameScoreboard;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Delay the message slightly so it doesn't get lost in join spam
        Season currentSeason = seasonManager.getCurrentSeason();
        
        if (currentSeason == null) {
            // No season configured at all
            event.getPlayer().sendMessage("");
            event.getPlayer().sendMessage(ChatColor.RED + "⚠ No season is currently configured");
            event.getPlayer().sendMessage(ChatColor.GRAY + "Contact an administrator");
            event.getPlayer().sendMessage("");
        } else if (currentSeason.isArchived()) {
            // Season is archived (inactive)
            event.getPlayer().sendMessage("");
            event.getPlayer().sendMessage(ChatColor.YELLOW + "⚠ Season " + currentSeason.getId() + " is not active");
            event.getPlayer().sendMessage(ChatColor.GRAY + "The current season has ended. Wait for the next season to start!");
            event.getPlayer().sendMessage("");
        } else if (currentSeason.isPlanned()) {
            // Season is planned but not yet active
            event.getPlayer().sendMessage("");
            event.getPlayer().sendMessage(ChatColor.YELLOW + "⚠ Season " + currentSeason.getId() + " is not yet active");
            event.getPlayer().sendMessage(ChatColor.GRAY + "The season will start on: " + ChatColor.WHITE + currentSeason.getStartAtFormatted());
            event.getPlayer().sendMessage("");
        }
        // If season is active, no special message needed
        
        // Show scoreboard to player (delayed to avoid join conflicts)
        Bukkit.getScheduler().runTaskLater(
            (JavaPlugin) Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Doomsday")),
            () -> gameScoreboard.showToPlayer(event.getPlayer()), 
            10L // 0.5 second delay
        );
    }
}