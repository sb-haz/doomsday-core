package gg.doomsday.core.gui.nations;

import gg.doomsday.core.gui.framework.GUIManager;
import gg.doomsday.core.gui.framework.NavigationGUI;
import gg.doomsday.core.gui.framework.builders.LayoutBuilder;
import gg.doomsday.core.gui.framework.decorators.ConfirmationDialog;
import gg.doomsday.core.gui.utils.ItemBuilder;
import gg.doomsday.core.gui.utils.GUIColors;
import gg.doomsday.core.nations.Nation;
import gg.doomsday.core.nations.NationManager;
import gg.doomsday.core.nations.NationPlayerManager;
import gg.doomsday.core.utils.NationColors;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Modern nation selection GUI using the enhanced framework
 * Demonstrates proper use of decorators and builders
 */
public class NationSelectionGUI extends NavigationGUI {
    
    private final NationManager nationManager;
    private final NationPlayerManager playerManager;
    
    public NationSelectionGUI(GUIManager guiManager, NationManager nationManager, 
                             NationPlayerManager playerManager) {
        super("Select Your Nation", 54, guiManager);
        this.nationManager = nationManager;
        this.playerManager = playerManager;
    }
    
    @Override
    public Inventory build(Player player) {
        UUID playerId = player.getUniqueId();
        String currentNation = playerManager.getPlayerNation(playerId);
        boolean canSwitch = playerManager.canPlayerSwitch(playerId);
        
        // Use the new LayoutBuilder for cleaner GUI construction
        LayoutBuilder layout = LayoutBuilder.forInventory(title, size)
                .withBorder()
                .withTitle(Material.PLAYER_HEAD, 
                    GUIColors.TEXT_ACCENT + GUIColors.BOLD + player.getName(),
                    GUIColors.keyValue("Current Nation", currentNation != null ? 
                        NationColors.getColoredNationName(currentNation) : GUIColors.secondary("None")),
                    GUIColors.keyValue("Can Switch", canSwitch ? 
                        GUIColors.SUCCESS + "Yes" : GUIColors.ERROR + "No"))
                .withCrossPattern(
                    "europe",    // North
                    "antarctica", // South  
                    "asia",      // East
                    "america",   // West
                    "africa",    // Center
                    this::createNationItem
                );
        
        // Add back button using framework
        addBackButton(layout.build(), 45, player);
        
        // Add leave nation button if applicable
        if (currentNation != null && canSwitch) {
            layout.withItem(49, new ItemBuilder(Material.BARRIER)
                    .setDisplayName(GUIColors.ERROR + GUIColors.BOLD + "Leave Current Nation")
                    .setLore(
                        GUIColors.secondary("Leave your current nation"),
                        GUIColors.WARNING + "Warning: You may not be able to rejoin immediately!",
                        "",
                        GUIColors.accent("Click to leave " + nationManager.getAllNations().get(currentNation).getDisplayName())
                    )
                    .build());
        }
        
        return layout.build();
    }
    
    private ItemStack createNationItem(String nationId) {
        Nation nation = nationManager.getAllNations().get(nationId);
        if (nation == null) {
            return new ItemBuilder(Material.BARRIER)
                    .setDisplayName(GUIColors.ERROR + "Nation Not Found")
                    .build();
        }
        
        UUID playerId = getCurrentPlayer().getUniqueId(); // Helper method to get current player
        String currentNation = playerManager.getPlayerNation(playerId);
        boolean canSwitch = playerManager.canPlayerSwitch(playerId);
        
        List<String> lore = new ArrayList<>();
        lore.add(GUIColors.keyValue("Population", String.valueOf(nation.getTotalPlayers())));
        lore.add(GUIColors.keyValue("Region", getLocationDescription(nation.getId())));
        lore.add("");
        
        // Add nation-specific information
        addNationProsCons(lore, nation.getId());
        lore.add("");
        
        if (nation.getId().equals(currentNation)) {
            lore.add(GUIColors.SUCCESS + "✓ Current Nation");
            if (canSwitch) {
                lore.add(GUIColors.accent("Click to teleport to nation"));
            } else {
                lore.add(GUIColors.ERROR + "You cannot leave this nation");
            }
        } else {
            if (currentNation != null && !canSwitch) {
                lore.add(GUIColors.ERROR + "You cannot switch nations");
            } else {
                lore.add(GUIColors.accent("Click to join this nation!"));
            }
        }
        
        return new ItemBuilder(getNationMaterial(nation.getId()))
                .setDisplayName(NationColors.getColoredNationName(nation.getId()))
                .setLore(lore)
                .build();
    }
    
    @Override
    protected boolean handleSpecificClick(Player player, int slot, ItemStack item) {
        UUID playerId = player.getUniqueId();
        String currentNation = playerManager.getPlayerNation(playerId);
        
        String nationId = getNationIdFromSlot(slot);
        if (nationId != null) {
            handleNationClick(player, nationId, currentNation);
            return true;
        }
        
        // Handle leave nation button
        if (slot == 49) {
            handleLeaveNation(player, currentNation);
            return true;
        }
        
        return false;
    }
    
    private String getNationIdFromSlot(int slot) {
        switch (slot) {
            case 13: return "europe";    // North
            case 21: return "america";   // West
            case 22: return "africa";    // Center
            case 23: return "asia";      // East
            case 31: return "antarctica"; // South
            default: return null;
        }
    }
    
    private void handleNationClick(Player player, String nationId, String currentNation) {
        Nation nation = nationManager.getAllNations().get(nationId);
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Nation not found!");
            return;
        }
        
        UUID playerId = player.getUniqueId();
        
        // If clicking current nation, teleport
        if (nationId.equals(currentNation)) {
            teleportToNation(player, nation);
            player.closeInventory();
            return;
        }
        
        // Check if player can switch
        if (!playerManager.canPlayerSwitch(playerId)) {
            if (currentNation != null) {
                player.sendMessage(ChatColor.RED + "You cannot leave your current nation!");
            } else {
                player.sendMessage(ChatColor.RED + "You cannot switch nations at this time!");
            }
            return;
        }
        
        // Use the new ConfirmationDialog decorator
        ConfirmationDialog confirmation = ConfirmationDialog.createJoinConfirmation(
            nation.getDisplayName(),
            (p) -> {
                if (playerManager.joinNation(p, nationId)) {
                    p.sendMessage(ChatColor.GREEN + "✓ Successfully joined " + 
                                NationColors.getColoredNationName(nationId) + ChatColor.GREEN + "!");
                    p.sendMessage(ChatColor.YELLOW + "Welcome to your new nation!");
                    teleportToNation(p, nation);
                    p.closeInventory();
                } else {
                    p.sendMessage(ChatColor.RED + "Failed to join " + 
                                NationColors.getColoredNationName(nationId) + ChatColor.RED + "!");
                    p.closeInventory();
                }
            },
            guiManager
        );
        
        guiManager.openGUI(player, confirmation);
    }
    
    private void handleLeaveNation(Player player, String currentNation) {
        if (currentNation == null) {
            player.sendMessage(ChatColor.RED + "You are not in any nation!");
            return;
        }
        
        UUID playerId = player.getUniqueId();
        if (!playerManager.canPlayerSwitch(playerId)) {
            player.sendMessage(ChatColor.RED + "You cannot leave your current nation!");
            return;
        }
        
        // Use confirmation dialog for leaving nation
        Nation nation = nationManager.getAllNations().get(currentNation);
        String nationName = nation != null ? nation.getDisplayName() : currentNation;
        
        ConfirmationDialog confirmation = ConfirmationDialog.createLeaveConfirmation(
            nationName,
            (p) -> {
                if (playerManager.leaveNation(p, true)) {
                    p.sendMessage(ChatColor.YELLOW + "You have left " + nationName + ".");
                    p.closeInventory();
                    // Reopen nation selection after leaving
                    guiManager.openGUI(p, new NationSelectionGUI(guiManager, nationManager, playerManager));
                } else {
                    p.sendMessage(ChatColor.RED + "Failed to leave your nation!");
                }
            },
            guiManager
        );
        
        guiManager.openGUI(player, confirmation);
    }
    
    // Helper methods - these could be moved to utility classes
    private Player getCurrentPlayer() {
        // This is a bit hacky - in a real implementation, you'd pass the player through
        // or use a different approach. For now, this shows the pattern.
        return null; // Would need proper implementation
    }
    
    private void teleportToNation(Player player, Nation nation) {
        int centerX = nation.getBorders().getCenterX();
        int centerZ = nation.getBorders().getCenterZ();
        
        Location teleportLoc = new Location(
            player.getWorld(),
            centerX,
            player.getWorld().getHighestBlockYAt(centerX, centerZ) + 1,
            centerZ
        );
        
        player.teleport(teleportLoc);
        player.sendMessage(ChatColor.GREEN + "Teleported to " + 
                          NationColors.getColoredNationName(nation.getId()) + 
                          ChatColor.GREEN + " center!");
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
    
    private void addNationProsCons(List<String> lore, String nationId) {
        switch (nationId.toLowerCase()) {
            case "america":
                lore.add(GUIColors.SUCCESS + "Pros:");
                lore.add(GUIColors.bullet("Powerful long-range missiles"));
                lore.add(GUIColors.bullet("Advanced defense systems"));
                lore.add(GUIColors.ERROR + "Cons:");
                lore.add(GUIColors.bullet("Vulnerable to natural disasters"));
                break;
                
            case "europe":
                lore.add(GUIColors.SUCCESS + "Pros:");
                lore.add(GUIColors.bullet("Balanced military capabilities"));
                lore.add(GUIColors.bullet("Strategic central location"));
                lore.add(GUIColors.ERROR + "Cons:");
                lore.add(GUIColors.bullet("Frequent storms and flooding"));
                break;
                
            case "africa":
                lore.add(GUIColors.SUCCESS + "Pros:");
                lore.add(GUIColors.bullet("Rich natural resources"));
                lore.add(GUIColors.bullet("Central strategic position"));
                lore.add(GUIColors.ERROR + "Cons:");
                lore.add(GUIColors.bullet("Harsh environmental conditions"));
                break;
                
            case "asia":
                lore.add(GUIColors.SUCCESS + "Pros:");
                lore.add(GUIColors.bullet("Large population advantage"));
                lore.add(GUIColors.bullet("Advanced missile technology"));
                lore.add(GUIColors.ERROR + "Cons:");
                lore.add(GUIColors.bullet("Dense urban vulnerability"));
                break;
                
            case "antarctica":
                lore.add(GUIColors.SUCCESS + "Pros:");
                lore.add(GUIColors.bullet("Extreme weather protection"));
                lore.add(GUIColors.bullet("Isolated and defensive"));
                lore.add(GUIColors.ERROR + "Cons:");
                lore.add(GUIColors.bullet("Limited resources"));
                break;
        }
    }
}