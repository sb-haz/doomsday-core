package gg.doomsday.core.gui.nations;

import gg.doomsday.core.gui.framework.GUIBuilder;
import gg.doomsday.core.gui.framework.GUIManager;
import gg.doomsday.core.gui.framework.NavigationGUI;
import gg.doomsday.core.gui.utils.ItemBuilder;
import gg.doomsday.core.gui.utils.GUIColors;
import gg.doomsday.core.nations.Nation;
import gg.doomsday.core.nations.NationManager;
import gg.doomsday.core.nations.NationPlayerManager;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

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
        
        // Add title
        Nation nation = nationManager.getAllNations().get(nationId);
        GUIBuilder.addTitleItem(gui, 4, Material.TNT, 
            GUIColors.ERROR + GUIColors.BOLD + getNationDisplayName(nationManager, nationId) + " Arsenal",
            GUIColors.secondary("Missile types available to this nation"),
            GUIColors.secondary("Total missiles: ") + GUIColors.TEXT_PRIMARY + (nation != null && nation.getMissileTypes() != null ? nation.getMissileTypes().size() : 0));
        
        // Display nation's missiles
        if (nation != null && nation.getMissileTypes() != null && !nation.getMissileTypes().isEmpty()) {
            JavaPlugin plugin = nationManager.getPlugin();
            ConfigurationSection rocketsSection = plugin.getConfig().getConfigurationSection("rockets");
            
            if (rocketsSection != null) {
                int slot = 10; // Start after border and title
                int itemsPerRow = 7;
                int currentRow = 0;
                int itemsInCurrentRow = 0;
                
                for (String missileType : nation.getMissileTypes()) {
                    // Skip to next row if we've filled current row
                    if (itemsInCurrentRow >= itemsPerRow) {
                        currentRow++;
                        itemsInCurrentRow = 0;
                        slot = 10 + (currentRow * 9); // Move to start of next row
                    }
                    
                    // Don't go past the available slots
                    if (slot >= 44) break; // Stop before the back button row
                    
                    ConfigurationSection rocket = rocketsSection.getConfigurationSection(missileType);
                    if (rocket != null) {
                        gui.setItem(slot, createMissileItem(missileType, rocket));
                        slot++;
                        itemsInCurrentRow++;
                    }
                }
            }
        } else {
            // No missiles available
            gui.setItem(22, new ItemBuilder(Material.BARRIER)
                    .setDisplayName(GUIColors.ERROR + GUIColors.BOLD + "No Missiles Available")
                    .setLore(
                        GUIColors.secondary("This nation has no missile types"),
                        GUIColors.secondary("configured in the system."),
                        GUIColors.secondary("Contact an administrator if this"),
                        GUIColors.secondary("seems incorrect.")
                    )
                    .build());
        }
        
        // Add missile count summary
        gui.setItem(49, new ItemBuilder(Material.COMPASS)
                .setDisplayName(GUIColors.TEXT_ACCENT + GUIColors.BOLD + "Missile Summary")
                .setLore(
                    GUIColors.keyValue("Available Types", String.valueOf(nation != null && nation.getMissileTypes() != null ? nation.getMissileTypes().size() : 0)),
                    GUIColors.keyValue("Nation", getNationDisplayName(nationManager, nationId)),
                    "",
                    GUIColors.secondary("Click a missile to view details")
                )
                .build());
        
        return gui;
    }
    
    private ItemStack createMissileItem(String missileType, ConfigurationSection rocket) {
        String displayName = rocket.getString("displayName", missileType.toUpperCase());
        String explosionType = rocket.getString("explosionType", "STANDARD");
        double speed = rocket.getDouble("speed", 1.0);
        double arcScale = rocket.getDouble("arcScale", 1.0);
        
        // Create lore with missile specifications
        List<String> lore = new ArrayList<>();
        lore.add(GUIColors.keyValue("Type", displayName));
        lore.add(GUIColors.keyValue("Explosion", explosionType));
        lore.add(GUIColors.keyValue("Speed", String.format("%.1f", speed) + " blocks/tick"));
        lore.add(GUIColors.keyValue("Arc Scale", String.format("%.1f", arcScale)));
        lore.add("");
        
        // Add launch coordinates if available
        if (rocket.contains("start")) {
            double startX = rocket.getDouble("start.x", 0);
            double startY = rocket.getDouble("start.y", 0);
            double startZ = rocket.getDouble("start.z", 0);
            lore.add(GUIColors.keyValue("Launch", String.format("%.0f, %.0f, %.0f", startX, startY, startZ)));
        }
        
        if (rocket.contains("end")) {
            double endX = rocket.getDouble("end.x", 0);
            double endY = rocket.getDouble("end.y", 0);
            double endZ = rocket.getDouble("end.z", 0);
            lore.add(GUIColors.keyValue("Target", String.format("%.0f, %.0f, %.0f", endX, endY, endZ)));
        }
        
        lore.add("");
        lore.add(GUIColors.accent("Click for more details"));
        lore.add(GUIColors.keyValue("Command", "/rocket " + missileType));
        
        // Choose material based on explosion type
        Material material = getMaterialForExplosionType(explosionType);
        
        return new ItemBuilder(material)
                .setDisplayName(GUIColors.MISSILE + GUIColors.BOLD + displayName)
                .setLore(lore)
                .build();
    }
    
    private Material getMaterialForExplosionType(String explosionType) {
        switch (explosionType.toUpperCase()) {
            case "NUCLEAR":
                return Material.BEACON;
            case "POWERFUL":
                return Material.TNT;
            case "HORIZONTAL_SPREAD":
                return Material.FIRE_CHARGE;
            case "BUNKER_BUSTER":
                return Material.HEAVY_WEIGHTED_PRESSURE_PLATE;
            case "CLUSTER":
                return Material.FIREWORK_ROCKET;
            case "EMP":
                return Material.REDSTONE_BLOCK;
            default:
                return Material.TNT;
        }
    }
    
    @Override
    protected boolean handleSpecificClick(Player player, int slot, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        
        // Check if it's a missile item (not border, title, or summary items)
        if (slot >= 10 && slot < 44 && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String displayName = item.getItemMeta().getDisplayName();
            
            // Extract missile type from the item lore
            if (item.getItemMeta().hasLore()) {
                List<String> lore = item.getItemMeta().getLore();
                String missileType = extractMissileTypeFromLore(lore);
                
                if (missileType != null) {
                    // Show detailed information or perform action
                    player.sendMessage(GUIColors.keyValue("Missile", displayName));
                    player.sendMessage(GUIColors.keyValue("Type", missileType));
                    player.sendMessage(GUIColors.secondary("Use ") + GUIColors.TEXT_PRIMARY + "/rocket " + missileType + GUIColors.secondary(" to launch"));
                    
                    // Optional: Could open a detailed info GUI or launch the missile
                    // For now, just provide information to the player
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private String extractMissileTypeFromLore(List<String> lore) {
        // Look for the command line in the lore
        for (String line : lore) {
            if (line.contains("/rocket ")) {
                // Extract missile type from "/rocket <type>" command
                String stripped = line.replaceAll("§[0-9a-fk-or]", ""); // Remove color codes
                if (stripped.contains("Command: /rocket ")) {
                    return stripped.substring(stripped.indexOf("/rocket ") + 8).trim();
                }
            }
        }
        return null;
    }
}