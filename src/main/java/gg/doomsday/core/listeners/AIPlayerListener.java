package gg.doomsday.core.listeners;

import gg.doomsday.core.ai.AIService;
import gg.doomsday.core.ai.PlayerStatsManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class AIPlayerListener implements Listener {
    
    private final JavaPlugin plugin;
    private final PlayerStatsManager statsManager;
    private final AIService aiService;
    
    public AIPlayerListener(JavaPlugin plugin, PlayerStatsManager statsManager, AIService aiService) {
        this.plugin = plugin;
        this.statsManager = statsManager;
        this.aiService = aiService;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        statsManager.createPlayerStatsFile(event.getPlayer().getUniqueId());
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        aiService.clearPlayerMemory(event.getPlayer().getUniqueId());
    }
}