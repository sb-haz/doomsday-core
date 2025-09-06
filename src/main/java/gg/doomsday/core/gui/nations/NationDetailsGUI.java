package gg.doomsday.core.gui.nations;

import gg.doomsday.core.gui.framework.GUIBuilder;
import gg.doomsday.core.gui.framework.GUIManager;
import gg.doomsday.core.gui.framework.NavigationGUI;
import gg.doomsday.core.gui.utils.ItemBuilder;
import gg.doomsday.core.nations.Nation;
import gg.doomsday.core.nations.NationManager;
import gg.doomsday.core.nations.NationPlayerManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * GUI showing details for a specific nation
 */
public class NationDetailsGUI extends NavigationGUI {
    
    private final NationManager nationManager;
    private final NationPlayerManager playerManager;
    private final String nationId;
    
    public NationDetailsGUI(GUIManager guiManager, NationManager nationManager, NationPlayerManager playerManager, String nationId) {
        super(getNationDisplayName(nationManager, nationId) + " Details", 54, guiManager);
        this.nationManager = nationManager;
        this.playerManager = playerManager;
        this.nationId = nationId;
    }
    
    private static String getNationDisplayName(NationManager nationManager, String nationId) {
        Nation nation = nationManager.getAllNations().get(nationId);
        return nation != null ? nation.getDisplayName() : nationId.toUpperCase();
    }
    
    @Override
    public Inventory build(Player player) {
        Nation nation = nationManager.getAllNations().get(nationId);
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Nation not found!");
            return GUIBuilder.createInventory(title, size);
        }
        
        Inventory gui = GUIBuilder.createInventory(title, size);
        
        // Add glass pane border
        GUIBuilder.addGlassPaneBorder(gui);
        
        UUID playerId = player.getUniqueId();
        String currentNation = playerManager.getPlayerNation(playerId);
        boolean isMyNation = nation.getId().equals(currentNation);
        
        // Add back button if there's navigation history
        addBackButton(gui, 45, player);
        
        // Nation info item (center top)
        gui.setItem(13, new ItemBuilder(getNationMaterial(nation.getId()))
                .setDisplayName("&6&l" + nation.getDisplayName())
                .setLore(
                    "&7Region: &f" + getLocationDescription(nation.getId()),
                    "&7Population: &f" + nation.getTotalPlayers(),
                    "&7Borders:",
                    "&7  X: &f" + nation.getBorders().getMinX() + " &7to &f" + nation.getBorders().getMaxX(),
                    "&7  Z: &f" + nation.getBorders().getMinZ() + " &7to &f" + nation.getBorders().getMaxZ(),
                    "&7  Y: &f" + nation.getBorders().getMinY() + " &7to &f" + nation.getBorders().getMaxY(),
                    "",
                    isMyNation ? "&a✓ Your Nation" : "&7Foreign Nation"
                )
                .build());
        
        // Missiles section
        gui.setItem(20, new ItemBuilder(Material.TNT)
                .setDisplayName("&c&lMissiles")
                .setLore("&7Click to view missile arsenal")
                .build());
        
        // Defense section
        gui.setItem(22, new ItemBuilder(Material.CROSSBOW)
                .setDisplayName("&9&lDefenses")
                .setLore("&7Click to view defense systems")
                .build());
        
        // Disasters section
        gui.setItem(24, new ItemBuilder(Material.BLAZE_POWDER)
                .setDisplayName("&4&lNatural Disasters")
                .setLore("&7Click to view disaster information")
                .build());
        
        return gui;
    }
    
    @Override
    protected boolean handleSpecificClick(Player player, int slot, ItemStack item) {
        switch (slot) {
            case 20: // Missiles
                guiManager.openGUI(player, new NationMissilesGUI(guiManager, nationManager, playerManager, nationId));
                return true;
            case 22: // Defense
                guiManager.openGUI(player, new NationDefenseGUI(guiManager, nationManager, playerManager, nationId));
                return true;
            case 24: // Disasters
                guiManager.openGUI(player, new NationDisastersGUI(guiManager, nationManager, playerManager, nationId));
                return true;
        }
        return false;
    }
    
    private Material getNationMaterial(String nationId) {
        switch (nationId.toLowerCase()) {
            case "america": return Material.BLUE_WOOL;
            case "europe": return Material.GREEN_WOOL;
            case "africa": return Material.ORANGE_WOOL;
            case "asia": return Material.LIME_WOOL;
            case "antarctica": return Material.WHITE_WOOL;
            default: return Material.GRAY_WOOL;
        }
    }
    
    private String getLocationDescription(String nationId) {
        switch (nationId.toLowerCase()) {
            case "america": return "Western Hemisphere";
            case "europe": return "Northern Region";
            case "africa": return "Central Region";
            case "asia": return "Eastern Hemisphere";
            case "antarctica": return "Southern Pole";
            default: return "Unknown Region";
        }
    }
}