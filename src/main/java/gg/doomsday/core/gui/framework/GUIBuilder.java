package gg.doomsday.core.gui.framework;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import gg.doomsday.core.gui.utils.ItemBuilder;

/**
 * Utility class for building common GUI patterns
 */
public class GUIBuilder {
    
    /**
     * Create an inventory with the specified title and size
     * @param title The inventory title
     * @param size The inventory size (must be multiple of 9)
     * @return The created inventory
     */
    public static Inventory createInventory(String title, int size) {
        return Bukkit.createInventory(null, size, title);
    }
    
    /**
     * Add a glass pane border to a 54-slot inventory
     * @param inventory The inventory
     */
    public static void addGlassPaneBorder(Inventory inventory) {
        if (inventory.getSize() != 54) return;
        
        ItemStack glassPaneItem = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .setDisplayName(" ")
                .build();
        
        // Top and bottom rows
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, glassPaneItem); // Top row
            inventory.setItem(45 + i, glassPaneItem); // Bottom row
        }
        
        // Left and right columns (excluding corners)
        for (int i = 1; i < 5; i++) {
            inventory.setItem(i * 9, glassPaneItem); // Left column
            inventory.setItem(i * 9 + 8, glassPaneItem); // Right column
        }
    }
    
    /**
     * Add a back button at the specified slot
     * @param inventory The inventory
     * @param slot The slot position
     */
    public static void addBackButton(Inventory inventory, int slot) {
        inventory.setItem(slot, new ItemBuilder(Material.ARROW)
                .setDisplayName("&7« Back")
                .setLore("&7Return to previous menu")
                .build());
    }
    
    /**
     * Add a title item at the specified slot
     * @param inventory The inventory
     * @param slot The slot position
     * @param material The item material
     * @param title The display name
     * @param lore The lore lines
     */
    public static void addTitleItem(Inventory inventory, int slot, Material material, String title, String... lore) {
        inventory.setItem(slot, new ItemBuilder(material)
                .setDisplayName(title)
                .setLore(lore)
                .build());
    }
    
    /**
     * Create a pagination layout helper
     * @param totalItems Total number of items to paginate
     * @param itemsPerPage Items per page
     * @return Pagination info
     */
    public static PaginationInfo createPagination(int totalItems, int itemsPerPage) {
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
        return new PaginationInfo(totalItems, itemsPerPage, totalPages);
    }
    
    /**
     * Get the content slots for a 54-slot GUI (excluding border)
     * @return Array of content slot indices
     */
    public static int[] getContentSlots54() {
        return new int[] {
            10, 11, 12, 13, 14, 15, 16,  // Row 2
            19, 20, 21, 22, 23, 24, 25,  // Row 3
            28, 29, 30, 31, 32, 33, 34,  // Row 4
            37, 38, 39, 40, 41, 42, 43   // Row 5
        };
    }
    
    /**
     * Get the content slots for a 27-slot GUI
     * @return Array of content slot indices
     */
    public static int[] getContentSlots27() {
        return new int[] {
            10, 11, 12, 13, 14, 15, 16,  // Middle row
        };
    }
    
    /**
     * Information about pagination
     */
    public static class PaginationInfo {
        public final int totalItems;
        public final int itemsPerPage;
        public final int totalPages;
        
        public PaginationInfo(int totalItems, int itemsPerPage, int totalPages) {
            this.totalItems = totalItems;
            this.itemsPerPage = itemsPerPage;
            this.totalPages = totalPages;
        }
        
        public int getStartIndex(int page) {
            return page * itemsPerPage;
        }
        
        public int getEndIndex(int page) {
            return Math.min((page + 1) * itemsPerPage, totalItems);
        }
        
        public boolean hasNextPage(int currentPage) {
            return currentPage < totalPages - 1;
        }
        
        public boolean hasPreviousPage(int currentPage) {
            return currentPage > 0;
        }
    }
}