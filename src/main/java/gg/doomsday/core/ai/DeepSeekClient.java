package gg.doomsday.core.ai;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import okhttp3.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class DeepSeekClient {
    
    private final JavaPlugin plugin;
    private final OkHttpClient client;
    private final Gson gson;
    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final int maxTokens;
    private final double temperature;
    
    public DeepSeekClient(JavaPlugin plugin, String apiKey, String apiUrl, String model, int maxTokens, double temperature) {
        this.plugin = plugin;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
        this.gson = new Gson();
        
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }
    
    public CompletableFuture<String> sendMessage(List<ChatMessage> messages) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject requestBody = createRequestBody(messages);
                Request request = createRequest(requestBody);
                
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        plugin.getLogger().warning("DeepSeek API request failed with code: " + response.code());
                        return "Sorry, I'm having trouble thinking right now. Maybe try again?";
                    }
                    
                    String responseBody = response.body().string();
                    return parseResponse(responseBody);
                }
                
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to send request to DeepSeek API", e);
                return "My brain seems to be lagging. Give me a moment and try again!";
            }
        });
    }
    
    private JsonObject createRequestBody(List<ChatMessage> messages) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.addProperty("max_tokens", maxTokens);
        requestBody.addProperty("temperature", temperature);
        requestBody.addProperty("stream", false);
        
        JsonArray messagesArray = new JsonArray();
        for (ChatMessage message : messages) {
            JsonObject messageObj = new JsonObject();
            messageObj.addProperty("role", message.getRole());
            messageObj.addProperty("content", message.getContent());
            messagesArray.add(messageObj);
        }
        
        requestBody.add("messages", messagesArray);
        return requestBody;
    }
    
    private Request createRequest(JsonObject requestBody) {
        RequestBody body = RequestBody.create(
            requestBody.toString(),
            MediaType.get("application/json; charset=utf-8")
        );
        
        return new Request.Builder()
                .url(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(body)
                .build();
    }
    
    private String parseResponse(String responseBody) {
        try {
            JsonObject response = gson.fromJson(responseBody, JsonObject.class);
            
            if (response.has("error")) {
                plugin.getLogger().warning("DeepSeek API error: " + response.get("error").toString());
                return "Oops! Something went wrong on my end. Try again?";
            }
            
            if (response.has("choices") && response.getAsJsonArray("choices").size() > 0) {
                JsonObject choice = response.getAsJsonArray("choices").get(0).getAsJsonObject();
                if (choice.has("message")) {
                    String content = choice.getAsJsonObject("message").get("content").getAsString();
                    return content.trim();
                }
            }
            
            return "I'm drawing a blank here. Maybe rephrase that?";
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to parse DeepSeek API response", e);
            return "My response decoder is acting up. Try asking again!";
        }
    }
    
    public void shutdown() {
        if (client != null) {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
        }
    }
    
    public static class ChatMessage {
        private final String role;
        private final String content;
        
        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
        
        public String getRole() {
            return role;
        }
        
        public String getContent() {
            return content;
        }
    }
}