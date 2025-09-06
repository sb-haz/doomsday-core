package gg.doomsday.core.gui.framework;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central manager for all GUI operations and navigation
 */
public class GUIManager implements Listener {
    
    private final JavaPlugin plugin;
    
    // Track current GUI for each player
    private final Map<UUID, GUI> currentGUI = new ConcurrentHashMap<>();
    
    // Navigation stack for each player
    private final Map<UUID, Stack<String>> navigationStack = new ConcurrentHashMap<>();
    
    // Registry of all available GUIs
    private final Map<String, GUIFactory> guiRegistry = new ConcurrentHashMap<>();
    
    public GUIManager(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Register a GUI type with the manager
     * @param identifier Unique identifier for this GUI type
     * @param factory Factory to create instances of this GUI
     */
    public void registerGUI(String identifier, GUIFactory factory) {
        guiRegistry.put(identifier, factory);
    }
    
    /**
     * Open a GUI for a player
     * @param player The player
     * @param gui The GUI to open
     */
    public void openGUI(Player player, GUI gui) {
        openGUI(player, gui, true);
    }
    
    /**
     * Open a GUI for a player
     * @param player The player
     * @param gui The GUI to open
     * @param addToHistory Whether to add current GUI to navigation history
     */
    public void openGUI(Player player, GUI gui, boolean addToHistory) {
        UUID playerId = player.getUniqueId();
        
        // Add current GUI to navigation history if requested
        if (addToHistory) {
            GUI current = currentGUI.get(playerId);
            if (current != null) {
                pushToNavigationStack(playerId, current.getTitle());
            }
        }
        
        // Build and open the new GUI
        Inventory inventory = gui.build(player);
        player.openInventory(inventory);
        
        // Track the current GUI
        currentGUI.put(playerId, gui);
        
        // Notify GUI of open event
        gui.onOpen(player);
    }
    
    /**
     * Navigate back to the previous GUI
     * @param player The player
     * @return true if navigation occurred
     */
    public boolean navigateBack(Player player) {
        UUID playerId = player.getUniqueId();
        String previousGUITitle = popFromNavigationStack(playerId);
        
        if (previousGUITitle == null) {
            player.closeInventory();
            return false;
        }
        
        // Find and open the previous GUI
        GUI previousGUI = createGUI(previousGUITitle, player);
        if (previousGUI != null) {
            openGUI(player, previousGUI, false); // Don't add to history when going back
            return true;
        }
        
        return false;
    }
    
    /**
     * Create a GUI instance from a title
     * @param title The GUI title
     * @param player The player (for context)
     * @return The GUI instance or null if not found
     */
    private GUI createGUI(String title, Player player) {
        for (Map.Entry<String, GUIFactory> entry : guiRegistry.entrySet()) {
            GUI gui = entry.getValue().create(title, player);
            if (gui != null) {
                return gui;
            }
        }
        return null;
    }
    
    /**
     * Check if a player has navigation history
     * @param player The player
     * @return true if they can navigate back
     */
    public boolean hasNavigationHistory(Player player) {
        UUID playerId = player.getUniqueId();
        Stack<String> stack = navigationStack.get(playerId);
        return stack != null && !stack.isEmpty();
    }
    
    /**
     * Clear navigation history for a player
     * @param player The player
     */
    public void clearNavigationHistory(Player player) {
        navigationStack.remove(player.getUniqueId());
    }
    
    /**
     * Push a GUI title to the navigation stack
     * @param playerId The player's UUID
     * @param guiTitle The GUI title
     */
    public void pushToNavigationStack(UUID playerId, String guiTitle) {
        navigationStack.computeIfAbsent(playerId, k -> new Stack<>()).push(guiTitle);
    }
    
    /**
     * Pop from the navigation stack
     * @param playerId The player's UUID
     * @return The previous GUI title or null
     */
    private String popFromNavigationStack(UUID playerId) {
        Stack<String> stack = navigationStack.get(playerId);
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return stack.pop();
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();
        
        GUI currentPlayerGUI = currentGUI.get(playerId);
        if (currentPlayerGUI == null) return;
        
        // Check if this click is for the current GUI
        if (!event.getView().getTitle().equals(currentPlayerGUI.getTitle())) {
            return;
        }
        
        event.setCancelled(true);
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;
        
        // Let the GUI handle the click
        currentPlayerGUI.handleClick(player, event.getSlot(), clickedItem);
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        GUI currentPlayerGUI = currentGUI.get(playerId);
        if (currentPlayerGUI != null) {
            // Notify GUI of close event
            currentPlayerGUI.onClose(player);
            
            // Clean up tracking
            currentGUI.remove(playerId);
        }
    }
    
    /**
     * Clean up data for a player (e.g., when they disconnect)
     * @param player The player
     */
    public void cleanup(Player player) {
        UUID playerId = player.getUniqueId();
        currentGUI.remove(playerId);
        navigationStack.remove(playerId);
    }
}

