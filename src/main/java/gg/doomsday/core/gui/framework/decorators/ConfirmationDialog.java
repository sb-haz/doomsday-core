package gg.doomsday.core.gui.framework.decorators;

import gg.doomsday.core.gui.framework.GUI;
import gg.doomsday.core.gui.framework.GUIBuilder;
import gg.doomsday.core.gui.framework.GUIManager;
import gg.doomsday.core.gui.utils.ItemBuilder;
import gg.doomsday.core.gui.utils.GUIColors;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

/**
 * A confirmation dialog GUI that can be used as an overlay
 * Follows the decorator pattern to wrap any action with confirmation
 */
public class ConfirmationDialog extends GUI {
    
    private final GUIManager guiManager;
    private final String message;
    private final Consumer<Player> onConfirm;
    private final Consumer<Player> onCancel;
    private final Material confirmMaterial;
    private final Material cancelMaterial;
    
    public ConfirmationDialog(String title, String message, 
                            Consumer<Player> onConfirm, Consumer<Player> onCancel,
                            GUIManager guiManager) {
        this(title, message, onConfirm, onCancel, guiManager, 
             Material.EMERALD_BLOCK, Material.REDSTONE_BLOCK);
    }
    
    public ConfirmationDialog(String title, String message, 
                            Consumer<Player> onConfirm, Consumer<Player> onCancel,
                            GUIManager guiManager, Material confirmMaterial, Material cancelMaterial) {
        super(title, 27);
        this.guiManager = guiManager;
        this.message = message;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
        this.confirmMaterial = confirmMaterial;
        this.cancelMaterial = cancelMaterial;
    }
    
    @Override
    public Inventory build(Player player) {
        Inventory gui = GUIBuilder.createInventory(title, size);
        
        // Add border
        addBorder(gui);
        
        // Add message
        gui.setItem(13, new ItemBuilder(Material.PAPER)
                .setDisplayName(GUIColors.TEXT_PRIMARY + message)
                .setLore(
                    "",
                    GUIColors.secondary("Choose your action:")
                )
                .build());
        
        // Confirm button
        gui.setItem(11, new ItemBuilder(confirmMaterial)
                .setDisplayName(GUIColors.SUCCESS + GUIColors.BOLD + "CONFIRM")
                .setLore(GUIColors.secondary("Click to confirm action"))
                .build());
        
        // Cancel button
        gui.setItem(15, new ItemBuilder(cancelMaterial)
                .setDisplayName(GUIColors.ERROR + GUIColors.BOLD + "CANCEL")
                .setLore(GUIColors.secondary("Click to cancel"))
                .build());
        
        return gui;
    }
    
    @Override
    public boolean handleClick(Player player, int slot, ItemStack item) {
        if (slot == 11) { // Confirm
            if (onConfirm != null) {
                onConfirm.accept(player);
            }
            return true;
        } else if (slot == 15) { // Cancel
            if (onCancel != null) {
                onCancel.accept(player);
            } else {
                // Default: navigate back or close
                if (guiManager.hasNavigationHistory(player)) {
                    guiManager.navigateBack(player);
                } else {
                    player.closeInventory();
                }
            }
            return true;
        }
        return false;
    }
    
    private void addBorder(Inventory gui) {
        ItemStack borderItem = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .setDisplayName("")
                .build();
        
        // Top and bottom rows
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, borderItem);
            gui.setItem(18 + i, borderItem);
        }
        
        // Side columns
        gui.setItem(9, borderItem);
        gui.setItem(17, borderItem);
    }
    
    // Factory methods for common confirmation dialogs
    public static ConfirmationDialog createDeleteConfirmation(String itemName, Consumer<Player> onConfirm, GUIManager guiManager) {
        return new ConfirmationDialog(
            "Confirm Deletion",
            "Delete " + itemName + "?",
            onConfirm,
            null,
            guiManager,
            Material.BARRIER,
            Material.GRAY_WOOL
        );
    }
    
    public static ConfirmationDialog createJoinConfirmation(String nationName, Consumer<Player> onConfirm, GUIManager guiManager) {
        return new ConfirmationDialog(
            "Join " + nationName + "?",
            "Join this nation?",
            onConfirm,
            null,
            guiManager
        );
    }
    
    public static ConfirmationDialog createLeaveConfirmation(String nationName, Consumer<Player> onConfirm, GUIManager guiManager) {
        return new ConfirmationDialog(
            "Leave " + nationName + "?",
            "Leave your current nation?",
            onConfirm,
            null,
            guiManager,
            Material.BARRIER,
            Material.GREEN_WOOL
        );
    }
}