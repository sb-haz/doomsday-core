package gg.doomsday.core.ai;

import gg.doomsday.core.data.PlayerDataManager;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class AIService {
    
    private final JavaPlugin plugin;
    private DeepSeekClient deepSeekClient;
    private final PlayerDataManager dataManager;
    private YamlConfiguration aiConfig;
    
    private String systemPrompt;
    private String messagePrefix;
    private String publicMessageFormat;
    private int conversationMemory;
    private int maxRequestsPerMinute;
    private int maxRequestsPerHour;
    
    private final Map<UUID, List<DeepSeekClient.ChatMessage>> conversationHistory;
    private final Map<UUID, List<Long>> requestTimestamps;
    
    public AIService(JavaPlugin plugin, YamlConfiguration aiConfig, PlayerDataManager dataManager) {
        this.plugin = plugin;
        this.aiConfig = aiConfig;
        this.dataManager = dataManager;
        this.conversationHistory = new ConcurrentHashMap<>();
        this.requestTimestamps = new ConcurrentHashMap<>();
        
        loadConfiguration();
    }
    
    private void loadConfiguration() {
        this.systemPrompt = aiConfig.getString("ai.system_prompt", "You are a helpful AI assistant.");
        this.messagePrefix = ChatColor.translateAlternateColorCodes('&', aiConfig.getString("ai.message_prefix", "&e[AI] &f"));
        this.publicMessageFormat = aiConfig.getString("ai.public_message_format", "&d[AI &7-> &d{player}] &f");
        this.conversationMemory = aiConfig.getInt("ai.conversation_memory", 10);
        this.maxRequestsPerMinute = aiConfig.getInt("ai.rate_limit.max_requests_per_minute", 10);
        this.maxRequestsPerHour = aiConfig.getInt("ai.rate_limit.max_requests_per_hour", 30);
        
        // Load API key from separate api_keys.yml file
        String apiKey = loadApiKey();
        String apiUrl = aiConfig.getString("ai.api_url", "https://api.deepseek.com/v1/chat/completions");
        String model = aiConfig.getString("ai.model", "deepseek-chat");
        int maxTokens = aiConfig.getInt("ai.max_tokens", 150);
        double temperature = aiConfig.getDouble("ai.temperature", 0.8);
        
        if (this.deepSeekClient != null) {
            this.deepSeekClient.shutdown();
        }
        
        this.deepSeekClient = new DeepSeekClient(plugin, apiKey, apiUrl, model, maxTokens, temperature);
    }
    
    private String loadApiKey() {
        try {
            File apiKeysFile = new File(plugin.getDataFolder(), "api_keys.yml");
            if (!apiKeysFile.exists()) {
                plugin.saveResource("api_keys.yml", false);
                plugin.getLogger().warning("Created new api_keys.yml file. Please add your DeepSeek API key!");
            }
            
            YamlConfiguration apiKeysConfig = YamlConfiguration.loadConfiguration(apiKeysFile);
            String apiKey = apiKeysConfig.getString("AI_API_KEY", "");
            
            if (apiKey.isEmpty() || apiKey.equals("CHANGE_ME_TO_YOUR_DEEPSEEK_API_KEY")) {
                plugin.getLogger().warning("AI API key not configured in api_keys.yml! AI features will not work.");
                return "";
            }
            
            return apiKey;
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load API key from api_keys.yml: " + e.getMessage());
            return "";
        }
    }
    
    public CompletableFuture<String> processMessage(Player player, String message) {
        UUID playerUUID = player.getUniqueId();
        
        if (!isValidApiKey()) {
            return CompletableFuture.completedFuture("§cAI service is not configured properly. Please contact an admin.");
        }
        
        if (isRateLimited(playerUUID)) {
            return CompletableFuture.completedFuture("§cWhoa there! Slow down a bit. You've hit your request limit.");
        }
        
        recordRequest(playerUUID);
        updatePlayerStats(playerUUID);
        
        List<DeepSeekClient.ChatMessage> messages = buildConversation(playerUUID, message);
        
        return deepSeekClient.sendMessage(messages).thenApply(response -> {
            addToConversationHistory(playerUUID, "user", message);
            addToConversationHistory(playerUUID, "assistant", response);
            // Process color codes in the AI response
            String coloredResponse = ChatColor.translateAlternateColorCodes('&', response);
            return messagePrefix + coloredResponse;
        });
    }
    
    public CompletableFuture<String> processPublicMessage(Player player, String message) {
        UUID playerUUID = player.getUniqueId();
        
        if (!isValidApiKey()) {
            return CompletableFuture.completedFuture("§cAI service is not configured properly. Please contact an admin.");
        }
        
        if (isRateLimited(playerUUID)) {
            return CompletableFuture.completedFuture("§cWhoa there! Slow down a bit. You've hit your request limit.");
        }
        
        recordRequest(playerUUID);
        updatePlayerStats(playerUUID);
        
        List<DeepSeekClient.ChatMessage> messages = buildConversation(playerUUID, message);
        
        return deepSeekClient.sendMessage(messages).thenApply(response -> {
            addToConversationHistory(playerUUID, "user", message);
            addToConversationHistory(playerUUID, "assistant", response);
            // Process color codes in the AI response
            String coloredResponse = ChatColor.translateAlternateColorCodes('&', response);
            // Use configurable format with {player} placeholder
            String formattedPrefix = publicMessageFormat.replace("{player}", player.getName());
            return ChatColor.translateAlternateColorCodes('&', formattedPrefix) + coloredResponse;
        });
    }
    
    private boolean isValidApiKey() {
        String apiKey = loadApiKey();
        return !apiKey.isEmpty() && !apiKey.equals("CHANGE_ME_TO_YOUR_DEEPSEEK_API_KEY");
    }
    
    private boolean isRateLimited(UUID playerUUID) {
        List<Long> timestamps = requestTimestamps.computeIfAbsent(playerUUID, k -> new ArrayList<>());
        long currentTime = System.currentTimeMillis();
        
        timestamps.removeIf(timestamp -> currentTime - timestamp > 3600000); // Remove timestamps older than 1 hour
        
        long minuteAgo = currentTime - 60000;
        long recentMinuteRequests = timestamps.stream()
                .mapToLong(timestamp -> timestamp)
                .filter(timestamp -> timestamp > minuteAgo)
                .count();
        
        if (recentMinuteRequests >= maxRequestsPerMinute) {
            return true;
        }
        
        return timestamps.size() >= maxRequestsPerHour;
    }
    
    private void recordRequest(UUID playerUUID) {
        List<Long> timestamps = requestTimestamps.computeIfAbsent(playerUUID, k -> new ArrayList<>());
        timestamps.add(System.currentTimeMillis());
    }
    
    private void updatePlayerStats(UUID playerUUID) {
        PlayerDataManager.PlayerData stats = dataManager.getPlayerData(playerUUID);
        
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        if (!today.equals(stats.getLastResetDate())) {
            stats.setRequestsToday(0);
            stats.setLastResetDate(today);
        }
        
        stats.setTotalRequests(stats.getTotalRequests() + 1);
        stats.setRequestsToday(stats.getRequestsToday() + 1);
        stats.setLastRequestTime(System.currentTimeMillis());
        
        dataManager.savePlayerData(playerUUID, stats);
    }
    
    private List<DeepSeekClient.ChatMessage> buildConversation(UUID playerUUID, String userMessage) {
        List<DeepSeekClient.ChatMessage> messages = new ArrayList<>();
        
        messages.add(new DeepSeekClient.ChatMessage("system", systemPrompt));
        
        List<DeepSeekClient.ChatMessage> history = conversationHistory.get(playerUUID);
        if (history != null && !history.isEmpty()) {
            int startIndex = Math.max(0, history.size() - (conversationMemory * 2));
            messages.addAll(history.subList(startIndex, history.size()));
        }
        
        messages.add(new DeepSeekClient.ChatMessage("user", userMessage));
        
        return messages;
    }
    
    private void addToConversationHistory(UUID playerUUID, String role, String content) {
        List<DeepSeekClient.ChatMessage> history = conversationHistory.computeIfAbsent(playerUUID, k -> new ArrayList<>());
        history.add(new DeepSeekClient.ChatMessage(role, content));
        
        if (history.size() > conversationMemory * 2) {
            history.subList(0, history.size() - conversationMemory * 2).clear();
        }
    }
    
    public void clearPlayerMemory(UUID playerUUID) {
        conversationHistory.remove(playerUUID);
        requestTimestamps.remove(playerUUID);
        dataManager.clearPlayerMemory(playerUUID);
    }
    
    public PlayerDataManager.PlayerData getPlayerStats(UUID playerUUID) {
        return dataManager.getPlayerData(playerUUID);
    }
    
    public String[] getRateLimitInfo(UUID playerUUID) {
        List<Long> timestamps = requestTimestamps.computeIfAbsent(playerUUID, k -> new ArrayList<>());
        long currentTime = System.currentTimeMillis();
        
        // Clean old timestamps (older than 1 hour)
        timestamps.removeIf(timestamp -> currentTime - timestamp > 3600000);
        
        // Count requests in the last minute
        long minuteAgo = currentTime - 60000;
        long recentMinuteRequests = timestamps.stream()
                .mapToLong(timestamp -> timestamp)
                .filter(timestamp -> timestamp > minuteAgo)
                .count();
        
        // Total requests in the last hour
        int recentHourRequests = timestamps.size();
        
        // Return array: [currentMinuteRequests, currentHourRequests, maxPerMinute, maxPerHour]
        return new String[]{
            String.valueOf(recentMinuteRequests),
            String.valueOf(recentHourRequests), 
            String.valueOf(maxRequestsPerMinute),
            String.valueOf(maxRequestsPerHour)
        };
    }
    
    public boolean reloadConfig() {
        try {
            File aiConfigFile = new File(plugin.getDataFolder(), "ai_config.yml");
            if (!aiConfigFile.exists()) {
                plugin.getLogger().warning("AI config file not found, cannot reload!");
                return false;
            }
            
            this.aiConfig = YamlConfiguration.loadConfiguration(aiConfigFile);
            loadConfiguration();
            
            plugin.getLogger().info("AI configuration reloaded successfully!");
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to reload AI configuration", e);
            return false;
        }
    }
    
    public void shutdown() {
        if (deepSeekClient != null) {
            deepSeekClient.shutdown();
        }
    }
}