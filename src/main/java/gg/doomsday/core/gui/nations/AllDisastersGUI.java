package gg.doomsday.core.gui.nations;

import gg.doomsday.core.gui.framework.GUIBuilder;
import gg.doomsday.core.gui.framework.GUIManager;
import gg.doomsday.core.gui.framework.NavigationGUI;
import gg.doomsday.core.gui.utils.ItemBuilder;
import gg.doomsday.core.nations.NationManager;
import gg.doomsday.core.nations.Nation;
import gg.doomsday.core.nations.Disaster;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * GUI showing all disaster types across all nations
 */
public class AllDisastersGUI extends NavigationGUI {
    
    private final NationManager nationManager;
    
    public AllDisastersGUI(GUIManager guiManager, NationManager nationManager) {
        super("All Disaster Types", 54, guiManager);
        this.nationManager = nationManager;
    }
    
    @Override
    public Inventory build(Player player) {
        Inventory gui = GUIBuilder.createInventory(title, size);
        GUIBuilder.addGlassPaneBorder(gui);
        addBackButton(gui, 45, player);
        
        GUIBuilder.addTitleItem(gui, 4, Material.LAVA_BUCKET, 
            "&c&lAll Natural Disasters",
            "&7Complete overview of all disaster types",
            "&7Learn about effects and which nations they affect");
        
        // Collect all unique disaster types across all nations
        Map<String, Set<String>> disasterToNations = new HashMap<>();
        Map<String, Disaster> disasterExamples = new HashMap<>();
        
        for (Map.Entry<String, Nation> entry : nationManager.getAllNations().entrySet()) {
            String nationId = entry.getKey();
            Nation nation = entry.getValue();
            
            Map<String, Disaster> disasters = nation.getDisasters();
            if (disasters != null) {
                for (Map.Entry<String, Disaster> disasterEntry : disasters.entrySet()) {
                    String disasterType = disasterEntry.getKey();
                    Disaster disaster = disasterEntry.getValue();
                    
                    disasterToNations.computeIfAbsent(disasterType, k -> new HashSet<>()).add(nation.getDisplayName());
                    disasterExamples.putIfAbsent(disasterType, disaster);
                }
            }
        }
        
        int slot = 10; // Start after border and title
        int itemsPerRow = 7;
        int currentRow = 0;
        int itemsInCurrentRow = 0;
        
        for (Map.Entry<String, Set<String>> entry : disasterToNations.entrySet()) {
            // Skip to next row if we've filled current row
            if (itemsInCurrentRow >= itemsPerRow) {
                currentRow++;
                itemsInCurrentRow = 0;
                slot = 10 + (currentRow * 9); // Move to start of next row
            }
            
            // Don't go past the available slots
            if (slot >= 44) break; // Stop before the back button row
            
            String disasterType = entry.getKey();
            Set<String> affectedNations = entry.getValue();
            Disaster example = disasterExamples.get(disasterType);
            
            // Create disaster item
            List<String> lore = new ArrayList<>();
            lore.add("&7Type: &f" + formatDisasterName(disasterType));
            if (example != null) {
                lore.add("&7Duration: &f" + String.format("%.1f", example.getDuration() * 0.05) + " seconds");
                lore.add("&7Min Interval: &f" + String.format("%.1f", example.getMinInterval() * 0.05) + " seconds");
                lore.add("&7Max Interval: &f" + String.format("%.1f", example.getMaxInterval() * 0.05) + " seconds");
                lore.add("&7Probability: &f" + String.format("%.0f", example.getProbability() * 100) + "%");
                lore.add("&7Enabled: " + (example.isEnabled() ? "&aYes" : "&cNo"));
                if (example.getMessage() != null && !example.getMessage().isEmpty()) {
                    lore.add("&7Message: &f" + example.getMessage());
                }
            }
            lore.add("");
            lore.add("&7Affects Nations:");
            for (String nationName : affectedNations) {
                lore.add("&7• &f" + nationName);
            }
            
            Material material = getMaterialForDisaster(disasterType);
            String nameColor = (example != null && example.isEnabled()) ? "&c" : "&7";
            
            gui.setItem(slot, new ItemBuilder(material)
                    .setDisplayName(nameColor + "&l" + formatDisasterName(disasterType))
                    .setLore(lore)
                    .build());
            
            slot++;
            itemsInCurrentRow++;
        }
        
        // Add summary item
        int totalDisasterTypes = disasterToNations.size();
        int enabledDisasterTypes = (int) disasterExamples.values().stream().filter(Disaster::isEnabled).count();
        
        gui.setItem(49, new ItemBuilder(Material.CLOCK)
                .setDisplayName("&e&lDisaster Summary")
                .setLore(
                    "&7Total Disaster Types: &f" + totalDisasterTypes,
                    "&7Enabled Types: &f" + enabledDisasterTypes,
                    "&7Disabled Types: &f" + (totalDisasterTypes - enabledDisasterTypes),
                    "&7Nations with Disasters: &f" + nationManager.getAllNations().size()
                )
                .build());
        
        return gui;
    }
    
    private String formatDisasterName(String disasterType) {
        return Arrays.stream(disasterType.split("_"))
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
                .reduce((a, b) -> a + " " + b)
                .orElse(disasterType);
    }
    
    private Material getMaterialForDisaster(String disasterType) {
        switch (disasterType.toLowerCase()) {
            case "droughts":
                return Material.DEAD_BUSH;
            case "earthquakes":
                return Material.STONE;
            case "floods":
                return Material.WATER_BUCKET;
            case "hurricanes":
            case "typhoons":
                return Material.GRAY_WOOL;
            case "volcanic_eruptions":
                return Material.LAVA_BUCKET;
            case "tsunamis":
                return Material.BLUE_ICE;
            case "blizzards":
                return Material.SNOW_BLOCK;
            case "tornadoes":
                return Material.WHITE_WOOL;
            case "wildfires":
                return Material.FIRE_CHARGE;
            case "avalanches":
                return Material.ICE;
            default:
                return Material.BARRIER;
        }
    }
    
    @Override
    protected boolean handleSpecificClick(Player player, int slot, ItemStack item) {
        // No specific click handling for disasters overview
        return false;
    }
}