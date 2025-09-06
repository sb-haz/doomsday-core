package gg.doomsday.core.gui.nations;

import gg.doomsday.core.gui.framework.GUIBuilder;
import gg.doomsday.core.gui.framework.GUIManager;
import gg.doomsday.core.gui.framework.NavigationGUI;
import gg.doomsday.core.nations.NationManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class AllMissilesGUI extends NavigationGUI {
    
    private final NationManager nationManager;
    
    public AllMissilesGUI(GUIManager guiManager, NationManager nationManager) {
        super("All Missile Types", 54, guiManager);
        this.nationManager = nationManager;
    }
    
    @Override
    public Inventory build(Player player) {
        Inventory gui = GUIBuilder.createInventory(title, size);
        GUIBuilder.addGlassPaneBorder(gui);
        addBackButton(gui, 45, player);
        
        GUIBuilder.addTitleItem(gui, 4, org.bukkit.Material.TNT, 
            "&c&lComplete Missile Arsenal",
            "&7All missile types in the system",
            "&7Learn about damage, effects, and usage");
        
        return gui;
    }
    
    @Override
    protected boolean handleSpecificClick(Player player, int slot, ItemStack item) {
        return false;
    }
}