package gg.doomsday.core.gui.nations;

import gg.doomsday.core.gui.framework.GUIBuilder;
import gg.doomsday.core.gui.framework.GUIManager;
import gg.doomsday.core.gui.framework.NavigationGUI;
import gg.doomsday.core.gui.utils.ItemBuilder;
import gg.doomsday.core.gui.utils.GUIColors;
import gg.doomsday.core.defense.AntiAirDefenseManager;
import gg.doomsday.core.defense.AntiAirDefense;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI showing all anti-air defense systems in the game
 */
public class AllAntiAirGUI extends NavigationGUI {
    
    private final AntiAirDefenseManager antiAirManager;
    
    public AllAntiAirGUI(GUIManager guiManager, AntiAirDefenseManager antiAirManager) {
        super("All Anti-Air Systems", 54, guiManager);
        this.antiAirManager = antiAirManager;
    }
    
    @Override
    public Inventory build(Player player) {
        Inventory gui = GUIBuilder.createInventory(title, size);
        GUIBuilder.addGlassPaneBorder(gui);
        addBackButton(gui, 45, player);
        
        GUIBuilder.addTitleItem(gui, 4, Material.IRON_BLOCK, 
            GUIColors.DEFENSE + GUIColors.BOLD + "All Anti-Air Defense Systems",
            GUIColors.secondary("Complete overview of defense systems"),
            GUIColors.secondary("Learn about range, accuracy, and capabilities"));
        
        // Get all anti-air defenses
        List<AntiAirDefense> allDefenses = antiAirManager.getDefenses();
        
        int slot = 10; // Start after border and title
        int itemsPerRow = 7;
        int currentRow = 0;
        int itemsInCurrentRow = 0;
        
        for (AntiAirDefense defense : allDefenses) {
            // Skip to next row if we've filled current row
            if (itemsInCurrentRow >= itemsPerRow) {
                currentRow++;
                itemsInCurrentRow = 0;
                slot = 10 + (currentRow * 9); // Move to start of next row
            }
            
            // Don't go past the available slots
            if (slot >= 44) break; // Stop before the back button row
            
            // Create defense item
            List<String> lore = new ArrayList<>();
            lore.add("&7Type: &f" + defense.getName());
            lore.add("&7Range: &f" + String.format("%.0f", defense.getRange()) + " blocks");
            lore.add("&7Accuracy: &f" + String.format("%.0f", defense.getAccuracy() * 100) + "%");
            lore.add("&7Speed: &f" + String.format("%.1f", defense.getInterceptorSpeed()) + " blocks/tick");
            lore.add("&7Reload Time: &f" + String.format("%.1f", defense.getReloadTime()) + "s");
            lore.add("&7Startup Time: &f" + String.format("%.1f", defense.getStartupTime()) + "s");
            lore.add("&7Mode: &f" + (defense.isAutomatic() ? "Automatic" : "Manual"));
            lore.add("&7Status: " + (defense.isOperational() ? "&aOnline" : "&cOffline"));
            lore.add("");
            lore.add("&7Location: &f" + 
                String.format("%.0f, %.0f, %.0f", 
                    defense.getLocation().getX(),
                    defense.getLocation().getY(),
                    defense.getLocation().getZ()));
            
            Material material = defense.isOperational() ? Material.IRON_BLOCK : Material.REDSTONE_BLOCK;
            String nameColor = defense.isOperational() ? "&a" : "&c";
            
            gui.setItem(slot, new ItemBuilder(material)
                    .setDisplayName(nameColor + defense.getDisplayName())
                    .setLore(lore)
                    .build());
            
            slot++;
            itemsInCurrentRow++;
        }
        
        // Add summary item
        gui.setItem(49, new ItemBuilder(Material.BEACON)
                .setDisplayName("&e&lDefense Summary")
                .setLore(
                    "&7Total Systems: &f" + allDefenses.size(),
                    "&7Online Systems: &f" + allDefenses.stream().mapToInt(d -> d.isOperational() ? 1 : 0).sum(),
                    "&7Automatic Systems: &f" + allDefenses.stream().mapToInt(d -> d.isAutomatic() ? 1 : 0).sum(),
                    "&7Manual Systems: &f" + allDefenses.stream().mapToInt(d -> !d.isAutomatic() ? 1 : 0).sum()
                )
                .build());
        
        return gui;
    }
    
    @Override
    protected boolean handleSpecificClick(Player player, int slot, ItemStack item) {
        // No specific click handling for anti-air systems overview
        return false;
    }
}