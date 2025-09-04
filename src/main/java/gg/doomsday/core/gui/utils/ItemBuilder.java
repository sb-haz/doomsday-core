package gg.doomsday.core.gui.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for creating ItemStacks with consistent formatting
 */
public class ItemBuilder {
    
    private final ItemStack item;
    private final ItemMeta meta;
    
    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }
    
    public ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material, amount);
        this.meta = item.getItemMeta();
    }
    
    public ItemBuilder setDisplayName(String name) {
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        return this;
    }
    
    public ItemBuilder setLore(String... lore) {
        List<String> loreList = new ArrayList<>();
        for (String line : lore) {
            loreList.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(loreList);
        return this;
    }
    
    public ItemBuilder setLore(List<String> lore) {
        List<String> loreList = new ArrayList<>();
        for (String line : lore) {
            loreList.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(loreList);
        return this;
    }
    
    public ItemBuilder addLore(String... additionalLore) {
        List<String> currentLore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        for (String line : additionalLore) {
            currentLore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(currentLore);
        return this;
    }
    
    public ItemBuilder hideAttributes() {
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        return this;
    }
    
    public ItemBuilder hideEnchants() {
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        return this;
    }
    
    public ItemBuilder hideAll() {
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.values());
        return this;
    }
    
    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
    
    // Static convenience methods
    public static ItemStack createItem(Material material, String name, String... lore) {
        return new ItemBuilder(material)
                .setDisplayName(name)
                .setLore(lore)
                .build();
    }
    
    public static ItemStack createButton(Material material, String name, String... lore) {
        return new ItemBuilder(material)
                .setDisplayName(name)
                .setLore(lore)
                .hideAll()
                .build();
    }
}