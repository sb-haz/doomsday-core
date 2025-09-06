package gg.doomsday.core.gui.nations;

import gg.doomsday.core.gui.framework.GUIBuilder;
import gg.doomsday.core.gui.framework.GUIManager;
import gg.doomsday.core.gui.framework.NavigationGUI;
import gg.doomsday.core.gui.utils.ItemBuilder;
import gg.doomsday.core.nations.Nation;
import gg.doomsday.core.nations.NationManager;
import gg.doomsday.core.nations.NationPlayerManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * GUI showing overview of all nations
 */
public class NationsOverviewGUI extends NavigationGUI {
    
    private final NationManager nationManager;
    private final NationPlayerManager playerManager;
    
    public NationsOverviewGUI(GUIManager guiManager, NationManager nationManager, NationPlayerManager playerManager) {
        super("Nations Overview", 54, guiManager);
        this.nationManager = nationManager;
        this.playerManager = playerManager;
    }
    
    @Override
    public Inventory build(Player player) {
        Inventory gui = GUIBuilder.createInventory(title, size);
        
        // Add glass pane border
        GUIBuilder.addGlassPaneBorder(gui);
        
        UUID playerId = player.getUniqueId();
        String currentNation = playerManager.getPlayerNation(playerId);
        
        // Arrange nations in + symbol (geographic layout)
        // Europe (North)
        Nation europe = nationManager.getAllNations().get("europe");
        if (europe != null) {
            ItemStack europeItem = createNationInfoItem(europe, Material.GREEN_WOOL, currentNation);
            gui.setItem(13, europeItem); // Top center
        }
        
        // America (West)  
        Nation america = nationManager.getAllNations().get("america");
        if (america != null) {
            ItemStack americaItem = createNationInfoItem(america, Material.BLUE_WOOL, currentNation);
            gui.setItem(21, americaItem); // Left center
        }
        
        // Africa (Center)
        Nation africa = nationManager.getAllNations().get("africa");
        if (africa != null) {
            ItemStack africaItem = createNationInfoItem(africa, Material.ORANGE_WOOL, currentNation);
            gui.setItem(22, africaItem); // Center
        }
        
        // Asia (East)
        Nation asia = nationManager.getAllNations().get("asia");
        if (asia != null) {
            ItemStack asiaItem = createNationInfoItem(asia, Material.LIME_WOOL, currentNation);
            gui.setItem(23, asiaItem); // Right center
        }
        
        // Antarctica (South)
        Nation antarctica = nationManager.getAllNations().get("antarctica");
        if (antarctica != null) {
            ItemStack antarcticaItem = createNationInfoItem(antarctica, Material.WHITE_WOOL, currentNation);
            gui.setItem(31, antarcticaItem); // Bottom center
        }
        
        // Add back button if there's navigation history
        addBackButton(gui, 45, player);
        
        // Add comprehensive overview buttons (bottom right)
        gui.setItem(51, new ItemBuilder(Material.FIREWORK_ROCKET)
                .setDisplayName("&c&lAll Missile Types")
                .setLore(
                    "&7View complete missile database:",
                    "&7• All missile types",
                    "&7• Damage and effect comparisons",
                    "&7• Which nations have what",
                    "",
                    "&eClick to view all missiles!"
                )
                .build());
        
        gui.setItem(52, new ItemBuilder(Material.CROSSBOW)
                .setDisplayName("&9&lAll Anti-Air Systems")
                .setLore(
                    "&7View defensive systems:",
                    "&7• Automated vs manual systems",
                    "&7• Range and accuracy stats",
                    "&7• Strategic locations",
                    "",
                    "&eClick to view defenses!"
                )
                .build());
        
        gui.setItem(53, new ItemBuilder(Material.NETHER_STAR)
                .setDisplayName("&4&lAll Natural Disasters")
                .setLore(
                    "&7Complete disaster encyclopedia:",
                    "&7• All disaster types",
                    "&7• Effects and descriptions",
                    "&7• Nation-specific information",
                    "",
                    "&eClick to view all disasters!"
                )
                .build());
        
        return gui;
    }
    
    @Override
    protected boolean handleSpecificClick(Player player, int slot, ItemStack item) {
        String nationId = null;
        
        switch (slot) {
            case 13: // Europe
                nationId = "europe";
                break;
            case 21: // America
                nationId = "america";
                break;
            case 22: // Africa
                nationId = "africa";
                break;
            case 23: // Asia
                nationId = "asia";
                break;
            case 31: // Antarctica
                nationId = "antarctica";
                break;
            case 51: // All Missiles
                guiManager.openGUI(player, new AllMissilesGUI(guiManager, nationManager));
                return true;
            case 52: // All Anti-Air
                // TODO: Implement AllAntiAirGUI
                return true;
            case 53: // All Disasters
                // TODO: Implement AllDisastersGUI
                return true;
        }
        
        if (nationId != null) {
            guiManager.openGUI(player, new NationDetailsGUI(guiManager, nationManager, playerManager, nationId));
            return true;
        }
        
        return false;
    }
    
    private ItemStack createNationInfoItem(Nation nation, Material material, String currentNation) {
        List<String> lore = new ArrayList<>();
        
        lore.add("&7Population: &f" + nation.getTotalPlayers());
        lore.add("&7Region: &f" + getLocationDescription(nation.getId()));
        lore.add("");
        
        // Add missile count
        if (nation.getMissileTypes() != null) {
            lore.add("&c⚡ &fMissiles: &7" + nation.getMissileTypes().size());
        } else {
            lore.add("&c⚡ &fMissiles: &70");
        }
        
        // Add disaster count
        if (nation.getDisasters() != null) {
            lore.add("&4☄ &fDisasters: &7" + nation.getDisasters().size());
        } else {
            lore.add("&4☄ &fDisasters: &70");
        }
        
        lore.add("");
        
        if (nation.getId().equals(currentNation)) {
            lore.add("&a✓ Your Nation");
        } else {
            lore.add("&eClick to view details");
        }
        
        return new ItemBuilder(material)
                .setDisplayName(getColoredNationName(nation.getId()))
                .setLore(lore)
                .build();
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
    
    private String getColoredNationName(String nationId) {
        switch (nationId.toLowerCase()) {
            case "america": return "&9&lAmerica";
            case "europe": return "&a&lEurope";
            case "africa": return "&6&lAfrica";
            case "asia": return "&2&lAsia";
            case "antarctica": return "&f&lAntarctica";
            default: return "&7&l" + nationId.toUpperCase();
        }
    }
}