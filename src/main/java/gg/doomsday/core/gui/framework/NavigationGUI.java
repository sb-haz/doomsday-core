package gg.doomsday.core.gui.framework;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import gg.doomsday.core.gui.utils.ItemBuilder;

/**
 * Base class for GUIs that support navigation (back buttons, etc.)
 */
public abstract class NavigationGUI extends GUI {
    
    protected final GUIManager guiManager;
    
    protected NavigationGUI(String title, int size, GUIManager guiManager) {
        super(title, size);
        this.guiManager = guiManager;
    }
    
    /**
     * Check if this GUI should show a back button
     * @param player The player
     * @return true if back button should be shown
     */
    protected boolean shouldShowBackButton(Player player) {
        return guiManager.hasNavigationHistory(player);
    }
    
    /**
     * Add a back button to the inventory at the specified slot
     * @param inventory The inventory
     * @param slot The slot to place the back button
     * @param player The player
     */
    protected void addBackButton(Inventory inventory, int slot, Player player) {
        if (shouldShowBackButton(player)) {
            inventory.setItem(slot, new ItemBuilder(Material.ARROW)
                    .setDisplayName("&7« Back")
                    .setLore("&7Return to previous menu")
                    .build());
        }
    }
    
    /**
     * Handle a back button click
     * @param player The player who clicked
     * @return true if navigation occurred
     */
    protected boolean handleBackClick(Player player) {
        return guiManager.navigateBack(player);
    }
    
    /**
     * Check if a slot is the back button
     * @param slot The slot
     * @return true if this is a back button slot
     */
    protected boolean isBackButton(int slot) {
        return slot == 45; // Default back button position (bottom left)
    }
    
    @Override
    public boolean handleClick(Player player, int slot, ItemStack item) {
        if (isBackButton(slot)) {
            return handleBackClick(player);
        }
        return handleSpecificClick(player, slot, item);
    }
    
    /**
     * Handle clicks specific to this GUI (excluding back button)
     * @param player The player who clicked
     * @param slot The slot that was clicked
     * @param item The item that was clicked
     * @return true if the click was handled
     */
    protected abstract boolean handleSpecificClick(Player player, int slot, ItemStack item);
}