package gg.doomsday.core.gui.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Utility class for creating and managing GUI layouts
 */
public class GUIBuilder {
    
    public static final String MAIN_GUI_TITLE = "Doomsday Control Panel";
    public static final String ROCKETS_GUI_TITLE = "Rocket Arsenal";
    public static final String ANTIAIR_GUI_TITLE = "Anti-Air Defense Grid";
    public static final String ITEMS_GUI_TITLE = "Custom Items";
    public static final String NATIONS_GUI_TITLE = "Nations & Disasters";
    public static final String MISSILE_INFO_GUI_TITLE = "Missile Information";
    public static final String ANTIAIR_INFO_GUI_TITLE = "Anti-Air Information";
    
    private final Inventory inventory;
    
    public GUIBuilder(String title, int size) {
        this.inventory = Bukkit.createInventory(null, size, title);
    }
    
    public GUIBuilder setItem(int slot, ItemStack item) {
        inventory.setItem(slot, item);
        return this;
    }
    
    public GUIBuilder fillBorder(Material material) {
        ItemStack borderItem = ItemBuilder.createButton(material, " ");
        
        // Top and bottom rows
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, borderItem);
            inventory.setItem(inventory.getSize() - 9 + i, borderItem);
        }
        
        // Left and right columns
        for (int i = 9; i < inventory.getSize() - 9; i += 9) {
            inventory.setItem(i, borderItem);
            inventory.setItem(i + 8, borderItem);
        }
        
        return this;
    }
    
    public GUIBuilder addBackButton(int slot) {
        ItemStack backButton = ItemBuilder.createButton(
            Material.ARROW, 
            "§f§lBack to Main Menu", 
            "§7Click to return"
        );
        return setItem(slot, backButton);
    }
    
    public GUIBuilder addCloseButton(int slot) {
        ItemStack closeButton = ItemBuilder.createButton(
            Material.BARRIER, 
            "§c§lClose Menu", 
            "§7Click to close this menu"
        );
        return setItem(slot, closeButton);
    }
    
    public Inventory build() {
        return inventory;
    }
    
    // Static convenience methods
    public static Inventory createMainGUI() {
        return new GUIBuilder(MAIN_GUI_TITLE, 54)
                .addCloseButton(53)
                .build();
    }
    
    public static Inventory createRocketsGUI() {
        return new GUIBuilder(ROCKETS_GUI_TITLE, 54)
                .addBackButton(53)
                .build();
    }
    
    public static Inventory createAntiAirGUI() {
        return new GUIBuilder(ANTIAIR_GUI_TITLE, 54)
                .addBackButton(53)
                .build();
    }
    
    public static Inventory createItemsGUI() {
        return new GUIBuilder(ITEMS_GUI_TITLE, 54)
                .addBackButton(53)
                .build();
    }
    
    public static Inventory createMissileInfoGUI() {
        return new GUIBuilder(MISSILE_INFO_GUI_TITLE, 27)
                .build();
    }
    
    public static Inventory createAntiAirInfoGUI() {
        return new GUIBuilder(ANTIAIR_INFO_GUI_TITLE, 27)
                .build();
    }
}