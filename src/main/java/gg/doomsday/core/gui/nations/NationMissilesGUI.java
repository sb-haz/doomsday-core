package gg.doomsday.core.gui.nations;

import gg.doomsday.core.gui.framework.GUIBuilder;
import gg.doomsday.core.gui.framework.GUIManager;
import gg.doomsday.core.gui.framework.NavigationGUI;
import gg.doomsday.core.nations.NationManager;
import gg.doomsday.core.nations.NationPlayerManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * GUI showing missiles for a specific nation
 */
public class NationMissilesGUI extends NavigationGUI {
    
    private final NationManager nationManager;
    private final NationPlayerManager playerManager;
    private final String nationId;
    
    public NationMissilesGUI(GUIManager guiManager, NationManager nationManager, NationPlayerManager playerManager, String nationId) {
        super(getNationDisplayName(nationManager, nationId) + " Missiles", 54, guiManager);
        this.nationManager = nationManager;
        this.playerManager = playerManager;
        this.nationId = nationId;
    }
    
    private static String getNationDisplayName(NationManager nationManager, String nationId) {
        var nation = nationManager.getAllNations().get(nationId);
        return nation != null ? nation.getDisplayName() : nationId.toUpperCase();
    }
    
    @Override
    public Inventory build(Player player) {
        Inventory gui = GUIBuilder.createInventory(title, size);
        GUIBuilder.addGlassPaneBorder(gui);
        addBackButton(gui, 45, player);
        
        // TODO: Implement missile display logic
        GUIBuilder.addTitleItem(gui, 4, org.bukkit.Material.TNT, 
            "&c&l" + getNationDisplayName(nationManager, nationId) + " Arsenal",
            "&7Missile types available to this nation");
        
        return gui;
    }
    
    @Override
    protected boolean handleSpecificClick(Player player, int slot, ItemStack item) {
        // TODO: Implement missile click handling
        return false;
    }
}