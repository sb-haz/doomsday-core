package gg.doomsday.core.items;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CustomItemManager {
    
    private final JavaPlugin plugin;
    private File customItemsFile;
    private FileConfiguration customItemsConfig;
    
    public CustomItemManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadCustomItemsConfig();
    }
    
    private void loadCustomItemsConfig() {
        customItemsFile = new File(plugin.getDataFolder(), "custom_items.yml");
        
        // Create default config if it doesn't exist
        if (!customItemsFile.exists()) {
            plugin.saveResource("custom_items.yml", false);
            plugin.getLogger().info("Created default custom_items.yml configuration");
        }
        
        customItemsConfig = YamlConfiguration.loadConfiguration(customItemsFile);
        
        // Load defaults from plugin jar
        InputStream defaultStream = plugin.getResource("custom_items.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            customItemsConfig.setDefaults(defaultConfig);
        }
        
        plugin.getLogger().info("Loaded custom items configuration");
    }
    
    public void reloadCustomItemsConfig() {
        customItemsConfig = YamlConfiguration.loadConfiguration(customItemsFile);
        
        // Load defaults from plugin jar
        InputStream defaultStream = plugin.getResource("custom_items.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            customItemsConfig.setDefaults(defaultConfig);
        }
        
        plugin.getLogger().info("Reloaded custom items configuration");
    }
    
    public ItemStack createReinforcementPowder() {
        // Load configuration values
        String materialName = customItemsConfig.getString("reinforcement_powder.material", "CLAY_BALL");
        int customModelData = customItemsConfig.getInt("reinforcement_powder.custom_model_data", 2001);
        String displayName = customItemsConfig.getString("reinforcement_powder.display_name", "&7Concrete Reinforcement Powder");
        List<String> loreLines = customItemsConfig.getStringList("reinforcement_powder.lore");
        
        // Parse material
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material '" + materialName + "' in custom_items.yml, using CLAY_BALL");
            material = Material.CLAY_BALL;
        }
        
        // Create item
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        // Set display name with color codes
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
        
        // Set lore with color codes
        List<String> processedLore = new ArrayList<>();
        for (String line : loreLines) {
            processedLore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(processedLore);
        
        // Set custom model data
        meta.setCustomModelData(customModelData);
        
        item.setItemMeta(meta);
        return item;
    }
    
    public ItemStack createReinforcementDetectorHelmet() {
        // Load configuration values
        String materialName = customItemsConfig.getString("reinforcement_detector_helmet.material", "DIAMOND_HELMET");
        int customModelData = customItemsConfig.getInt("reinforcement_detector_helmet.custom_model_data", 2002);
        String displayName = customItemsConfig.getString("reinforcement_detector_helmet.display_name", "&bReinforcement Detection Helmet");
        List<String> loreLines = customItemsConfig.getStringList("reinforcement_detector_helmet.lore");
        
        // Parse material
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material '" + materialName + "' in custom_items.yml, using DIAMOND_HELMET");
            material = Material.DIAMOND_HELMET;
        }
        
        // Create item
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        // Set display name with color codes
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
        
        // Set lore with color codes
        List<String> processedLore = new ArrayList<>();
        for (String line : loreLines) {
            processedLore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(processedLore);
        
        // Set custom model data
        meta.setCustomModelData(customModelData);
        
        item.setItemMeta(meta);
        return item;
    }
    
    public ItemStack createRocketFuel() {
        // Load configuration values
        String materialName = customItemsConfig.getString("rocket_fuel.material", "BLAZE_POWDER");
        int customModelData = customItemsConfig.getInt("rocket_fuel.custom_model_data", 2003);
        String displayName = customItemsConfig.getString("rocket_fuel.display_name", "&6Rocket Fuel");
        List<String> loreLines = customItemsConfig.getStringList("rocket_fuel.lore");
        
        // Parse material
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material '" + materialName + "' in custom_items.yml, using BLAZE_POWDER");
            material = Material.BLAZE_POWDER;
        }
        
        // Create item
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        // Set display name with color codes
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
        
        // Set lore with color codes
        List<String> processedLore = new ArrayList<>();
        for (String line : loreLines) {
            processedLore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(processedLore);
        
        // Set custom model data
        meta.setCustomModelData(customModelData);
        
        item.setItemMeta(meta);
        return item;
    }
    
    public ItemStack createRocketFuel(int amount) {
        ItemStack fuel = createRocketFuel();
        fuel.setAmount(Math.max(1, Math.min(64, amount)));
        return fuel;
    }
    
    public boolean isReinforcementPowder(ItemStack item) {
        if (item == null) return false;
        
        // Load configuration values for comparison
        String materialName = customItemsConfig.getString("reinforcement_powder.material", "CLAY_BALL");
        int customModelData = customItemsConfig.getInt("reinforcement_powder.custom_model_data", 2001);
        String displayName = customItemsConfig.getString("reinforcement_powder.display_name", "&7Concrete Reinforcement Powder");
        
        // Parse material
        Material expectedMaterial;
        try {
            expectedMaterial = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            expectedMaterial = Material.CLAY_BALL;
        }
        
        // Check material
        if (item.getType() != expectedMaterial) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        // Check display name
        String expectedDisplayName = ChatColor.translateAlternateColorCodes('&', displayName);
        if (!expectedDisplayName.equals(meta.getDisplayName())) {
            return false;
        }
        
        // Check custom model data
        return meta.hasCustomModelData() && meta.getCustomModelData() == customModelData;
    }
    
    public boolean isReinforcementDetectorHelmet(ItemStack item) {
        if (item == null) return false;
        
        // Load configuration values for comparison
        String materialName = customItemsConfig.getString("reinforcement_detector_helmet.material", "DIAMOND_HELMET");
        int customModelData = customItemsConfig.getInt("reinforcement_detector_helmet.custom_model_data", 2002);
        String displayName = customItemsConfig.getString("reinforcement_detector_helmet.display_name", "&bReinforcement Detection Helmet");
        
        // Parse material
        Material expectedMaterial;
        try {
            expectedMaterial = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            expectedMaterial = Material.DIAMOND_HELMET;
        }
        
        // Check material
        if (item.getType() != expectedMaterial) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        // Check display name
        String expectedDisplayName = ChatColor.translateAlternateColorCodes('&', displayName);
        if (!expectedDisplayName.equals(meta.getDisplayName())) {
            return false;
        }
        
        // Check custom model data
        return meta.hasCustomModelData() && meta.getCustomModelData() == customModelData;
    }
    
    public List<String> getRecipeIngredients() {
        return customItemsConfig.getStringList("reinforcement_powder.recipe.ingredients");
    }
    
    public boolean isShapelessRecipe() {
        return customItemsConfig.getBoolean("reinforcement_powder.recipe.shapeless", true);
    }
    
    public String getDisplayName() {
        String displayName = customItemsConfig.getString("reinforcement_powder.display_name", "&7Concrete Reinforcement Powder");
        return ChatColor.translateAlternateColorCodes('&', displayName);
    }
    
    public FileConfiguration getCustomItemsConfig() {
        return customItemsConfig;
    }
}