package gg.doomsday.core.gui.framework.decorators;

import gg.doomsday.core.gui.framework.GUIBuilder;
import gg.doomsday.core.gui.framework.GUIManager;
import gg.doomsday.core.gui.framework.NavigationGUI;
import gg.doomsday.core.gui.utils.ItemBuilder;
import gg.doomsday.core.gui.utils.GUIColors;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A paginated GUI decorator that automatically handles pagination
 * Wraps any list-based content with navigation controls
 */
public class PaginatedGUI<T> extends NavigationGUI {
    
    private final List<T> items;
    private final BiFunction<T, Integer, ItemStack> itemRenderer;
    private final Consumer<Player> onItemClick;
    private final int itemsPerPage;
    private final List<Integer> contentSlots;
    
    // Track current page for each player
    private final Map<String, Integer> playerPages = new ConcurrentHashMap<>();
    
    public PaginatedGUI(String title, List<T> items, 
                       BiFunction<T, Integer, ItemStack> itemRenderer,
                       Consumer<Player> onItemClick,
                       GUIManager guiManager) {
        this(title, items, itemRenderer, onItemClick, guiManager, 54);
    }
    
    public PaginatedGUI(String title, List<T> items, 
                       BiFunction<T, Integer, ItemStack> itemRenderer,
                       Consumer<Player> onItemClick,
                       GUIManager guiManager, int size) {
        super(title, size, guiManager);
        this.items = items;
        this.itemRenderer = itemRenderer;
        this.onItemClick = onItemClick;
        
        // Get content slots (excluding borders)
        int[] slots = size == 54 ? GUIBuilder.getContentSlots54() : GUIBuilder.getContentSlots27();
        this.contentSlots = Arrays.stream(slots).boxed().collect(Collectors.toList());
        this.itemsPerPage = contentSlots.size();
    }
    
    @Override
    public Inventory build(Player player) {
        Inventory gui = GUIBuilder.createInventory(title, size);
        GUIBuilder.addGlassPaneBorder(gui);
        
        int currentPage = playerPages.getOrDefault(player.getName(), 0);
        int totalPages = (int) Math.ceil((double) items.size() / itemsPerPage);
        
        // Add items for current page
        addPageItems(gui, currentPage);
        
        // Add pagination controls
        addPaginationControls(gui, currentPage, totalPages, player);
        
        // Add back button
        addBackButton(gui, 45, player);
        
        // Add page info
        addPageInfo(gui, currentPage, totalPages);
        
        return gui;
    }
    
    private void addPageItems(Inventory gui, int currentPage) {
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, items.size());
        
        int slotIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            if (slotIndex >= contentSlots.size()) break;
            
            T item = items.get(i);
            ItemStack itemStack = itemRenderer.apply(item, i);
            gui.setItem(contentSlots.get(slotIndex), itemStack);
            slotIndex++;
        }
    }
    
    private void addPaginationControls(Inventory gui, int currentPage, int totalPages, Player player) {
        // Previous page button
        if (currentPage > 0) {
            gui.setItem(46, new ItemBuilder(Material.ARROW)
                    .setDisplayName(GUIColors.TEXT_ACCENT + "« Previous Page")
                    .setLore(
                        GUIColors.keyValue("Current Page", String.valueOf(currentPage + 1)),
                        GUIColors.keyValue("Total Pages", String.valueOf(totalPages))
                    )
                    .build());
        }
        
        // Next page button
        if (currentPage < totalPages - 1) {
            gui.setItem(52, new ItemBuilder(Material.ARROW)
                    .setDisplayName(GUIColors.TEXT_ACCENT + "Next Page »")
                    .setLore(
                        GUIColors.keyValue("Current Page", String.valueOf(currentPage + 1)),
                        GUIColors.keyValue("Total Pages", String.valueOf(totalPages))
                    )
                    .build());
        }
    }
    
    private void addPageInfo(Inventory gui, int currentPage, int totalPages) {
        gui.setItem(49, new ItemBuilder(Material.BOOK)
                .setDisplayName(GUIColors.TEXT_PRIMARY + GUIColors.BOLD + "Page Information")
                .setLore(
                    GUIColors.keyValue("Current Page", String.valueOf(currentPage + 1) + " / " + totalPages),
                    GUIColors.keyValue("Total Items", String.valueOf(items.size())),
                    GUIColors.keyValue("Items Per Page", String.valueOf(itemsPerPage)),
                    "",
                    GUIColors.secondary("Use arrows to navigate pages")
                )
                .build());
    }
    
    @Override
    protected boolean handleSpecificClick(Player player, int slot, ItemStack item) {
        int currentPage = playerPages.getOrDefault(player.getName(), 0);
        int totalPages = (int) Math.ceil((double) items.size() / itemsPerPage);
        
        // Handle pagination controls
        if (slot == 46 && currentPage > 0) { // Previous page
            playerPages.put(player.getName(), currentPage - 1);
            refreshGUI(player);
            return true;
        } else if (slot == 52 && currentPage < totalPages - 1) { // Next page
            playerPages.put(player.getName(), currentPage + 1);
            refreshGUI(player);
            return true;
        }
        
        // Handle content item clicks
        if (contentSlots.contains(slot)) {
            int slotIndex = contentSlots.indexOf(slot);
            int itemIndex = (currentPage * itemsPerPage) + slotIndex;
            
            if (itemIndex < items.size() && onItemClick != null) {
                onItemClick.accept(player);
                return true;
            }
        }
        
        return false;
    }
    
    private void refreshGUI(Player player) {
        guiManager.openGUI(player, this, false); // Don't add to history when refreshing
    }
    
    @Override
    public void onClose(Player player) {
        super.onClose(player);
        playerPages.remove(player.getName()); // Clean up page tracking
    }
    
    // Utility methods
    public void goToPage(Player player, int page) {
        int totalPages = (int) Math.ceil((double) items.size() / itemsPerPage);
        if (page >= 0 && page < totalPages) {
            playerPages.put(player.getName(), page);
            refreshGUI(player);
        }
    }
    
    public int getCurrentPage(Player player) {
        return playerPages.getOrDefault(player.getName(), 0);
    }
    
    public int getTotalPages() {
        return (int) Math.ceil((double) items.size() / itemsPerPage);
    }
}