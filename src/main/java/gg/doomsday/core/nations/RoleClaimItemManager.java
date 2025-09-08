package gg.doomsday.core.nations;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoleClaimItemManager {
    
    private final JavaPlugin plugin;
    private File itemsFile;
    private FileConfiguration itemsConfig;
    
    public RoleClaimItemManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadItemsConfig();
    }
    
    private void loadItemsConfig() {
        itemsFile = new File(plugin.getDataFolder(), "items.yml");
        
        if (!itemsFile.exists()) {
            plugin.saveResource("items.yml", false);
            plugin.getLogger().info("Created default items.yml configuration");
        }
        
        itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
        
        InputStream defaultStream = plugin.getResource("items.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            itemsConfig.setDefaults(defaultConfig);
        }
        
        plugin.getLogger().info("Loaded role claim items configuration");
    }
    
    public void reloadItemsConfig() {
        itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
        
        InputStream defaultStream = plugin.getResource("items.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            itemsConfig.setDefaults(defaultConfig);
        }
        
        plugin.getLogger().info("Reloaded role claim items configuration");
    }
    
    public ItemStack createRoleClaimItem(NationRole role) {
        String itemKey = role.getDisplayName().toLowerCase() + "_claim";
        
        if (!itemsConfig.contains(itemKey)) {
            plugin.getLogger().warning("No configuration found for role claim item: " + itemKey);
            return null;
        }
        
        // Load configuration values
        String materialName = itemsConfig.getString(itemKey + ".material", "PAPER");
        int customModelData = itemsConfig.getInt(itemKey + ".custom_model_data", 0);
        String displayName = itemsConfig.getString(itemKey + ".display_name", "&7" + role.getDisplayName() + " Badge");
        List<String> loreLines = itemsConfig.getStringList(itemKey + ".lore");
        
        // Parse material
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material '" + materialName + "' in items.yml for " + itemKey + ", using PAPER");
            material = Material.PAPER;
        }
        
        // Create item
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta == null) {
            plugin.getLogger().warning("Could not get ItemMeta for material " + material + " in " + itemKey);
            return null;
        }
        
        // Set display name with color codes
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
        
        // Set lore with color codes
        List<String> processedLore = new ArrayList<>();
        for (String line : loreLines) {
            processedLore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(processedLore);
        
        // Set custom model data
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    public NationRole getRoleFromItem(ItemStack item) {
        if (item == null) return null;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        
        for (NationRole role : NationRole.getClaimableRoles()) {
            if (isRoleClaimItem(item, role)) {
                return role;
            }
        }
        
        return null;
    }
    
    public boolean isRoleClaimItem(ItemStack item, NationRole role) {
        if (item == null || role == null) return false;
        
        String itemKey = role.getDisplayName().toLowerCase() + "_claim";
        
        if (!itemsConfig.contains(itemKey)) {
            return false;
        }
        
        // Load configuration values for comparison
        String materialName = itemsConfig.getString(itemKey + ".material", "PAPER");
        int customModelData = itemsConfig.getInt(itemKey + ".custom_model_data", 0);
        String displayName = itemsConfig.getString(itemKey + ".display_name", "&7" + role.getDisplayName() + " Badge");
        
        // Parse expected material
        Material expectedMaterial;
        try {
            expectedMaterial = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            expectedMaterial = Material.PAPER;
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
        
        // Check custom model data (if specified)
        if (customModelData > 0) {
            return meta.hasCustomModelData() && meta.getCustomModelData() == customModelData;
        }
        
        return true;
    }
    
    public boolean isAnyRoleClaimItem(ItemStack item) {
        return getRoleFromItem(item) != null;
    }
    
    public Map<NationRole, ItemStack> getAllRoleClaimItems() {
        Map<NationRole, ItemStack> items = new HashMap<>();
        
        for (NationRole role : NationRole.getClaimableRoles()) {
            ItemStack item = createRoleClaimItem(role);
            if (item != null) {
                items.put(role, item);
            }
        }
        
        return items;
    }
    
    public FileConfiguration getItemsConfig() {
        return itemsConfig;
    }
}