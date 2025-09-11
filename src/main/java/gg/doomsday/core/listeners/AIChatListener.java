package gg.doomsday.core.listeners;

import gg.doomsday.core.ai.AIService;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class AIChatListener implements Listener {
    
    private final JavaPlugin plugin;
    private final AIService aiService;
    
    public AIChatListener(JavaPlugin plugin, AIService aiService) {
        this.plugin = plugin;
        this.aiService = aiService;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        Player player = event.getPlayer();
        
        // Check if message contains @ai anywhere
        if (!message.toLowerCase().contains("@ai")) {
            return;
        }
        
        // Check permission
        if (!player.hasPermission("ai.public")) {
            // Send private message to player about missing permission
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage(ChatColor.RED + "You don't have permission to use @ai in public chat!");
                player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/ai <message>" + ChatColor.GRAY + " for private AI chat instead.");
            });
            return;
        }
        
        // Extract the actual message (remove "@ai" and get the rest)
        String aiMessage = extractAIMessage(message);
        
        if (aiMessage.isEmpty()) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage(ChatColor.RED + "Please provide a message with @ai!");
            });
            return;
        }
        
        if (aiMessage.length() > 500) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage(ChatColor.RED + "Your message is too long! Keep it under 500 characters.");
            });
            return;
        }
        
        // Process the AI message publicly
        aiService.processPublicMessage(player, aiMessage).thenAccept(response -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                // Broadcast to all players
                plugin.getServer().broadcastMessage(response);
            });
        }).exceptionally(throwable -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage(ChatColor.RED + "Something went wrong while processing your AI request. Try again later!");
            });
            plugin.getLogger().warning("AI service error for public message from player " + player.getName() + ": " + throwable.getMessage());
            return null;
        });
    }
    
    private String extractAIMessage(String message) {
        // Remove @ai from the message and return the rest
        String lowerMessage = message.toLowerCase();
        int aiIndex = lowerMessage.indexOf("@ai");
        
        if (aiIndex == -1) {
            return "";
        }
        
        // Get the part before @ai and the part after @ai
        String beforeAI = message.substring(0, aiIndex).trim();
        String afterAI = message.substring(aiIndex + 3).trim(); // +3 for "@ai"
        
        // Combine both parts, removing @ai
        String result = (beforeAI + " " + afterAI).trim();
        
        return result.isEmpty() ? "" : result;
    }
}