package gg.doomsday.core.listeners;

import gg.doomsday.core.ai.AIService;
import gg.doomsday.core.data.PlayerDataManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class AIPlayerListener implements Listener {
    
    private final JavaPlugin plugin;
    private final PlayerDataManager dataManager;
    private final AIService aiService;
    
    public AIPlayerListener(JavaPlugin plugin, PlayerDataManager dataManager, AIService aiService) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.aiService = aiService;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Create player data file if it doesn't exist
        dataManager.createPlayerDataFile(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        
        // Update login data every time they join
        dataManager.updatePlayerLogin(event.getPlayer().getUniqueId(), event.getPlayer().getName());
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        aiService.clearPlayerMemory(event.getPlayer().getUniqueId());
    }
}