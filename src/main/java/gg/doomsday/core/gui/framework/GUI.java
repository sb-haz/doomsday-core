package gg.doomsday.core.gui.framework;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Abstract base class for all GUI implementations
 */
public abstract class GUI {
    
    protected final String title;
    protected final int size;
    
    protected GUI(String title, int size) {
        this.title = title;
        this.size = size;
    }
    
    /**
     * Build the GUI inventory with all items
     * @param player The player viewing the GUI
     * @return The built inventory
     */
    public abstract Inventory build(Player player);
    
    /**
     * Handle a click event in this GUI
     * @param player The player who clicked
     * @param slot The slot that was clicked
     * @param item The item that was clicked
     * @return true if the click was handled, false otherwise
     */
    public abstract boolean handleClick(Player player, int slot, ItemStack item);
    
    /**
     * Called when the GUI is opened for a player
     * @param player The player
     */
    public void onOpen(Player player) {
        // Override if needed
    }
    
    /**
     * Called when the GUI is closed for a player
     * @param player The player
     */
    public void onClose(Player player) {
        // Override if needed
    }
    
    public String getTitle() {
        return title;
    }
    
    public int getSize() {
        return size;
    }
}