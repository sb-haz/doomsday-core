package gg.doomsday.core.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import gg.doomsday.core.gui.utils.ItemBuilder;
import gg.doomsday.core.nations.Disaster;
import gg.doomsday.core.nations.Nation;
import gg.doomsday.core.nations.NationManager;
import gg.doomsday.core.nations.NationPlayerManager;
import gg.doomsday.core.utils.NationColors;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

public class NationGUI implements Listener {
    
    private final JavaPlugin plugin;
    private final NationManager nationManager;
    private final NationPlayerManager playerManager;
    
    // Navigation stack - stores the full navigation history for each player
    private final Map<UUID, Stack<String>> navigationStack = new ConcurrentHashMap<>();
    
    public NationGUI(JavaPlugin plugin, NationManager nationManager, NationPlayerManager playerManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.playerManager = playerManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    // Navigation helper methods
    private void pushToNavigationStack(Player player, String guiTitle) {
        UUID playerId = player.getUniqueId();
        navigationStack.computeIfAbsent(playerId, k -> new Stack<>()).push(guiTitle);
    }
    
    public void pushToNavigationStackFromExternal(Player player, String guiTitle) {
        pushToNavigationStack(player, guiTitle);
    }
    
    private String popFromNavigationStack(Player player) {
        UUID playerId = player.getUniqueId();
        Stack<String> stack = navigationStack.get(playerId);
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return stack.pop();
    }
    
    private String peekNavigationStack(Player player) {
        UUID playerId = player.getUniqueId();
        Stack<String> stack = navigationStack.get(playerId);
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return stack.peek();
    }
    
    private void clearNavigationStack(Player player) {
        navigationStack.remove(player.getUniqueId());
    }
    
    private boolean hasBackButton(String currentTitle, Player player) {
        return peekNavigationStack(player) != null;
    }
    
    private void handleBackNavigation(Player player) {
        String previousGUITitle = popFromNavigationStack(player);
        if (previousGUITitle == null) {
            player.closeInventory();
            return;
        }
        
        // Navigate back based on the previous GUI title
        // Note: We don't push this navigation to the stack since it's a back action
        if (previousGUITitle.equals("Select Your Nation")) {
            openNationSelectionGUIWithoutPush(player);
        } else if (previousGUITitle.equals("Nations Overview")) {
            openNationsListGUIWithoutPush(player);
        } else if (previousGUITitle.endsWith(" Details")) {
            String nationName = previousGUITitle.replace(" Details", "");
            String nationId = getNationIdFromDisplayName(nationName);
            if (nationId != null) {
                openNationDetailsGUIWithoutPush(player, nationId);
            }
        } else if (previousGUITitle.endsWith(" - Main Menu")) {
            // This is from NationsCommand - need to trigger it
            player.closeInventory();
            player.performCommand("nations");
        } else if (previousGUITitle.endsWith(" Missiles")) {
            String nationName = previousGUITitle.replace(" Missiles", "");
            String nationId = getNationIdFromDisplayName(nationName);
            if (nationId != null) {
                openMissilesGUIWithoutPush(player, nationId);
            }
        } else if (previousGUITitle.endsWith(" Defenses")) {
            String nationName = previousGUITitle.replace(" Defenses", "");
            String nationId = getNationIdFromDisplayName(nationName);
            if (nationId != null) {
                openDefenseGUIWithoutPush(player, nationId);
            }
        } else if (previousGUITitle.endsWith(" Disasters")) {
            String nationName = previousGUITitle.replace(" Disasters", "");
            String nationId = getNationIdFromDisplayName(nationName);
            if (nationId != null) {
                openDisastersGUIWithoutPush(player, nationId);
            }
        } else if (previousGUITitle.equals("All Missile Types")) {
            openAllMissilesGUIWithoutPush(player);
        } else if (previousGUITitle.equals("All Anti-Air Systems")) {
            openAllAntiAirGUIWithoutPush(player);
        } else if (previousGUITitle.equals("All Natural Disasters")) {
            openAllDisastersGUIWithoutPush(player);
        } else {
            // Default fallback
            player.closeInventory();
        }
    }
    
    public void openNationSelectionGUI(Player player) {
        openNationSelectionGUI(player, null);
    }
    
    public void openNationSelectionGUI(Player player, String fromGUI) {
        if (fromGUI != null) {
            pushToNavigationStack(player, fromGUI);
        } else {
            clearNavigationStack(player);
        }
        
        Inventory gui = Bukkit.createInventory(null, 54, "Select Your Nation");
        
        // Add glass pane border
        addGlassPaneBorder(gui);
        
        UUID playerId = player.getUniqueId();
        String currentNation = playerManager.getPlayerNation(playerId);
        boolean canSwitch = playerManager.canPlayerSwitch(playerId);
        
        gui.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                .setDisplayName("&6&l" + player.getName())
                .setLore(
                    "&fYour Nation: " + (currentNation != null ?
                        getColoredNationName(currentNation) : "&fNone"),
                    "&fCan Switch: " + (canSwitch ? "&aYes" : "&cNo")
                )
                .setSkullOwner(player.getName())
                .build());
        
        // Arrange nations in + symbol (geographic layout)
        // Europe (North)
        Nation europe = nationManager.getAllNations().get("europe");
        if (europe != null) {
            ItemStack europeItem = createNationItem(europe, Material.GREEN_WOOL, currentNation, canSwitch);
            gui.setItem(13, europeItem); // Top center
        }
        
        // America (West)  
        Nation america = nationManager.getAllNations().get("america");
        if (america != null) {
            ItemStack americaItem = createNationItem(america, Material.BLUE_WOOL, currentNation, canSwitch);
            gui.setItem(21, americaItem); // Left center
        }
        
        // Africa (Center)
        Nation africa = nationManager.getAllNations().get("africa");
        if (africa != null) {
            ItemStack africaItem = createNationItem(africa, Material.ORANGE_WOOL, currentNation, canSwitch);
            gui.setItem(22, africaItem); // Center
        }
        
        // Asia (East)
        Nation asia = nationManager.getAllNations().get("asia");
        if (asia != null) {
            ItemStack asiaItem = createNationItem(asia, Material.LIME_WOOL, currentNation, canSwitch);
            gui.setItem(23, asiaItem); // Right center
        }
        
        // Antarctica (South)
        Nation antarctica = nationManager.getAllNations().get("antarctica");
        if (antarctica != null) {
            ItemStack antarcticaItem = createNationItem(antarctica, Material.WHITE_WOOL, currentNation, canSwitch);
            gui.setItem(31, antarcticaItem); // Bottom center
        }
        
        if (currentNation != null && canSwitch) {
            gui.setItem(49, new ItemBuilder(Material.BARRIER)
                    .setDisplayName("&c&lLeave Current Nation")
                    .setLore(
                        "&fLeave your current nation",
                        "&cWarning: You may not be able to rejoin immediately!",
                        "",
                        "&eClick to leave " + nationManager.getAllNations().get(currentNation).getDisplayName()
                    )
                    .build());
        }
        
        // Add back button if there's a previous GUI
        if (hasBackButton("Select Your Nation", player)) {
            gui.setItem(45, new ItemBuilder(Material.ARROW)
                    .setDisplayName("&f« Back")
                    .setLore("&fReturn to previous menu")
                    .build());
        }
        
        player.openInventory(gui);
    }
    
    private void openNationSelectionGUIWithoutPush(Player player) {
        // Open without modifying navigation stack - for back navigation
        openNationSelectionGUI(player, null);
    }
    
    public void openNationsListGUI(Player player) {
        openNationsListGUI(player, null);
    }
    
    public void openNationsListGUI(Player player, String fromGUI) {
        if (fromGUI != null) {
            pushToNavigationStack(player, fromGUI);
        } else {
            clearNavigationStack(player);
        }
        
        Inventory gui = Bukkit.createInventory(null, 54, "Nations Overview");
        
        // Add glass pane border
        addGlassPaneBorder(gui);
        
        UUID playerId = player.getUniqueId();
        String currentNation = playerManager.getPlayerNation(playerId);
        
        // Add player info at center top
        gui.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                .setDisplayName("&6&l" + player.getName())
                .setLore(
                    "&fExplore and learn about nations",
                    "",
                    currentNation == null ? "&cYou are not in any nation!" : "&aYou are in " + nationManager.getAllNations().get(currentNation).getDisplayName(),
                    "",
                    "&eClick on a nation to view details!"
                )
                .setSkullOwner(player.getName())
                .build());
        
        // Arrange nations in same + symbol layout as selection
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
        
        // Add back button if there's a previous GUI
        if (hasBackButton("Nations Overview", player)) {
            gui.setItem(45, new ItemBuilder(Material.ARROW)
                    .setDisplayName("&7« Back")
                    .setLore("&7Return to previous menu")
                    .build());
        }
        
        // Add the comprehensive overview buttons (bottom right)
        // All Missiles Info
        gui.setItem(51, new ItemBuilder(Material.FIREWORK_ROCKET)
                .setDisplayName("&c&lAll Missile Types")
                .setLore(
                    "&7View complete missile database:",
                    "&7• All 5 missile types (R1-R5)",
                    "&7• Damage and effect comparisons",
                    "&7• Which nations have what",
                    "",
                    "&eClick to view all missiles!"
                )
                .build());
        
        // All Anti-Air Info
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
        
        // All Disasters Info
        gui.setItem(53, new ItemBuilder(Material.NETHER_STAR)
                .setDisplayName("&4&lAll Natural Disasters")
                .setLore(
                    "&7Complete disaster encyclopedia:",
                    "&7• All 10 disaster types",
                    "&7• Effects and descriptions",
                    "&7• Nation-specific information",
                    "",
                    "&eClick to view all disasters!"
                )
                .build());
        
        player.openInventory(gui);
    }
    
    private void openNationsListGUIWithoutPush(Player player) {
        // Open without modifying navigation stack - for back navigation
        openNationsListGUI(player, null);
    }
    
    private ItemStack createNationInfoItem(Nation nation, Material material, String currentNation) {
        List<String> lore = new ArrayList<>();
        
        int onlineCount = playerManager.getOnlinePlayerCountInNation(nation.getId());
        lore.add("&7Population: &f" + nation.getTotalPlayers());
        lore.add("&7Region: &f" + getLocationDescription(nation.getId()));
        lore.add("");
        
        // Add missile count
        int missileCount = nation.getMissileTypes() != null ? nation.getMissileTypes().size() : 0;
        lore.add("&6Missiles: &f" + missileCount + " types");
        
        // Add disaster count
        int disasterCount = nation.getDisasters().size();
        lore.add("&cDisasters: &f" + disasterCount + " types");
        lore.add("");
        
        if (nation.getId().equals(currentNation)) {
            lore.add("&a✓ Your Nation");
        } else {
            lore.add("&7Foreign Nation");
        }
        
        lore.add("");
        lore.add("&eClick to view details!");
        
        return new ItemBuilder(material)
                .setDisplayName("&6&l" + nation.getDisplayName())
                .setLore(lore)
                .build();
    }
    
    public void openNationDetailsGUI(Player player, String nationId) {
        openNationDetailsGUI(player, nationId, null);
    }
    
    public void openNationDetailsGUI(Player player, String nationId, String fromGUI) {
        if (fromGUI != null) {
            pushToNavigationStack(player, fromGUI);
        }
        
        Nation nation = nationManager.getAllNations().get(nationId);
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Nation not found!");
            return;
        }
        
        Inventory gui = Bukkit.createInventory(null, 54, nation.getDisplayName() + " Details");
        
        // Add glass pane border
        addGlassPaneBorder(gui);
        
        UUID playerId = player.getUniqueId();
        String currentNation = playerManager.getPlayerNation(playerId);
        boolean isMyNation = nation.getId().equals(currentNation);
        
        // Add back button if there's a previous GUI
        if (hasBackButton(nation.getDisplayName() + " Details", player)) {
            gui.setItem(45, new ItemBuilder(Material.ARROW)
                    .setDisplayName("&7« Back")
                    .setLore("&7Return to previous menu")
                    .build());
        }
        
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
                .setLore("&7Click to view disaster info")
                .build());
        
        
        player.openInventory(gui);
    }
    
    private void addNationProsCons(List<String> lore, String nationId) {
        switch (nationId.toLowerCase()) {
            case "america":
                lore.add("&aPros:");
                lore.add("&7• &fPowerful long-range missiles");
                lore.add("&7• &fAdvanced defense systems");
                lore.add("&7• &fStrong economy");
                lore.add("&cCons:");
                lore.add("&7• &fVulnerable to natural disasters");
                lore.add("&7• &fTarget for other nations");
                break;
                
            case "europe":
                lore.add("&aPros:");
                lore.add("&7• &fBalanced military capabilities");
                lore.add("&7• &fStrategic central location");
                lore.add("&7• &fDiverse technology");
                lore.add("&cCons:");
                lore.add("&7• &fFrequent storms and flooding");
                lore.add("&7• &fModerate defense strength");
                break;
                
            case "africa":
                lore.add("&aPros:");
                lore.add("&7• &fRich natural resources");
                lore.add("&7• &fCentral strategic position");
                lore.add("&7• &fGrowing military potential");
                lore.add("&cCons:");
                lore.add("&7• &fHarsh environmental conditions");
                lore.add("&7• &fLimited advanced weaponry");
                break;
                
            case "asia":
                lore.add("&aPros:");
                lore.add("&7• &fLarge population advantage");
                lore.add("&7• &fAdvanced missile technology");
                lore.add("&7• &fStrong manufacturing base");
                lore.add("&cCons:");
                lore.add("&7• &fDense urban vulnerability");
                lore.add("&7• &fResource competition");
                break;
                
            case "antarctica":
                lore.add("&aPros:");
                lore.add("&7• &fExtreme weather protection");
                lore.add("&7• &fIsolated and defensive");
                lore.add("&7• &fUnique strategic advantages");
                lore.add("&cCons:");
                lore.add("&7• &fLimited resources");
                lore.add("&7• &fHarsh living conditions");
                break;
                
            default:
                lore.add("&7No information available");
                break;
        }
    }

    private ItemStack createNationItem(Nation nation, Material material, String currentNation, boolean canSwitch) {
        List<String> lore = new ArrayList<>();
        
        lore.add("&7Population: &f" + nation.getTotalPlayers());
        lore.add("&7Region: &f" + getLocationDescription(nation.getId()));
        lore.add("");
        
        // Add nation-specific pros and cons
        addNationProsCons(lore, nation.getId());
        lore.add("");
        
        if (nation.getId().equals(currentNation)) {
            lore.add("&a✓ Current Nation");
            if (canSwitch) {
                lore.add("&eClick to teleport to nation");
            } else {
                lore.add("&cYou cannot leave this nation");
            }
        } else {
            if (currentNation != null && !canSwitch) {
                lore.add("&cYou cannot switch nations");
            } else {
                lore.add("&eClick to join this nation!");
            }
        }
        
        return new ItemBuilder(material)
                .setDisplayName(getColoredNationName(nation.getId()))
                .setLore(lore)
                .build();
    }
    
    // WithoutPush methods for back navigation - these don't modify the navigation stack
    private void openNationDetailsGUIWithoutPush(Player player, String nationId) {
        openNationDetailsGUI(player, nationId, null);
    }
    
    private void openMissilesGUIWithoutPush(Player player, String nationId) {
        openMissilesGUI(player, nationId, null);
    }
    
    private void openDefenseGUIWithoutPush(Player player, String nationId) {
        openDefenseGUI(player, nationId, null);
    }
    
    private void openDisastersGUIWithoutPush(Player player, String nationId) {
        openDisastersGUI(player, nationId, null);
    }
    
    private void openAllMissilesGUIWithoutPush(Player player) {
        openAllMissilesGUI(player, null);
    }
    
    private void openAllAntiAirGUIWithoutPush(Player player) {
        openAllAntiAirGUI(player, null);
    }
    
    private void openAllDisastersGUIWithoutPush(Player player) {
        openAllDisastersGUI(player, null);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        // Handle nation selection GUI
        if (title.equals("Select Your Nation")) {
            handleNationSelectionClick(event, player);
            return;
        }
        
        // Handle nations overview GUI
        if (title.equals("Nations Overview")) {
            handleNationsOverviewClick(event, player);
            return;
        }
        
        // Handle nation details GUI
        if (title.endsWith(" Details")) {
            handleNationDetailsClick(event, player);
            return;
        }
        
        // Handle missiles GUI
        if (title.endsWith(" Missiles")) {
            handleMissilesClick(event, player);
            return;
        }
        
        // Handle defenses GUI
        if (title.endsWith(" Defenses")) {
            handleDefensesClick(event, player);
            return;
        }
        
        // Handle disasters GUI
        if (title.endsWith(" Disasters")) {
            handleDisastersClick(event, player);
            return;
        }
        
        // Handle confirmation GUIs
        if (title.startsWith("Join ")) {
            handleConfirmationClick(event, player);
            return;
        }
        
        // Handle comprehensive overview GUIs
        if (title.equals("All Missile Types") || title.equals("All Anti-Air Systems") || title.equals("All Natural Disasters")) {
            handleComprehensiveOverviewClick(event, player);
            return;
        }
    }
    
    private void handleNationsOverviewClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        switch (event.getSlot()) {
            case 13: // Europe (North)
                openNationDetailsGUI(player, "europe", "Nations Overview");
                break;
            case 21: // America (West)
                openNationDetailsGUI(player, "america", "Nations Overview");
                break;
            case 22: // Africa (Center)
                openNationDetailsGUI(player, "africa", "Nations Overview");
                break;
            case 23: // Asia (East)
                openNationDetailsGUI(player, "asia", "Nations Overview");
                break;
            case 31: // Antarctica (South)
                openNationDetailsGUI(player, "antarctica", "Nations Overview");
                break;
            case 45: // Back
                handleBackNavigation(player);
                break;
            case 51: // All Missiles
                openAllMissilesGUI(player, "Nations Overview");
                break;
            case 52: // All Anti-Air
                openAllAntiAirGUI(player, "Nations Overview");
                break;
            case 53: // All Disasters
                openAllDisastersGUI(player, "Nations Overview");
                break;
            default:
                break;
        }
    }
    
    private void handleNationDetailsClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        String title = event.getView().getTitle();
        String nationName = title.replace(" Details", "");
        String nationId = getNationIdFromDisplayName(nationName);
        
        if (nationId == null) {
            player.sendMessage(ChatColor.RED + "Nation not found!");
            return;
        }
        
        switch (event.getSlot()) {
            case 20: // Missiles
                openMissilesGUI(player, nationId, title);
                break;
            case 22: // Defense
                openDefenseGUI(player, nationId, title);
                break;
            case 24: // Disasters
                openDisastersGUI(player, nationId, title);
                break;
            case 45: // Back button
                handleBackNavigation(player);
                break;
            default:
                break;
        }
    }
    
    private void handleMissilesClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        if (event.getSlot() == 45) { // Back button
            handleBackNavigation(player);
        }
    }
    
    private void handleDefensesClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        if (event.getSlot() == 45) { // Back button
            handleBackNavigation(player);
        }
    }
    
    private void handleDisastersClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        if (event.getSlot() == 45) { // Back button
            handleBackNavigation(player);
        }
    }
    
    private void handleNationSelectionClick(InventoryClickEvent event, Player player) {
        
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        UUID playerId = player.getUniqueId();
        String currentNation = playerManager.getPlayerNation(playerId);
        
        switch (event.getSlot()) {
            case 13: // Europe (North)
                handleNationClick(player, "europe", currentNation);
                break;
            case 21: // America (West)
                handleNationClick(player, "america", currentNation);
                break;
            case 22: // Africa (Center)
                handleNationClick(player, "africa", currentNation);
                break;
            case 23: // Asia (East)
                handleNationClick(player, "asia", currentNation);
                break;
            case 31: // Antarctica (South)
                handleNationClick(player, "antarctica", currentNation);
                break;
            case 45: // Back button (if exists)
                handleBackNavigation(player);
                break;
            case 49: // Leave Nation
                handleLeaveNation(player, currentNation);
                break;
            default:
                break;
        }
    }
    
    private void handleNationClick(Player player, String nationId, String currentNation) {
        Nation nation = nationManager.getAllNations().get(nationId);
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Nation not found!");
            return;
        }
        
        UUID playerId = player.getUniqueId();
        
        if (nationId.equals(currentNation)) {
            teleportToNation(player, nation);
            player.closeInventory();
            return;
        }
        
        if (!playerManager.canPlayerSwitch(playerId)) {
            if (currentNation != null) {
                player.sendMessage(ChatColor.RED + "You cannot leave your current nation!");
            } else {
                player.sendMessage(ChatColor.RED + "You cannot switch nations at this time!");
            }
            return;
        }
        
        // Open confirmation GUI
        openConfirmationGUI(player, nation, currentNation);
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
        
        if (playerManager.leaveNation(player, true)) {
            Nation nation = nationManager.getAllNations().get(currentNation);
            String nationName = nation != null ? nation.getDisplayName() : currentNation;
            
            player.sendMessage(ChatColor.YELLOW + "You have left " + nationName + ".");
            player.closeInventory();
            
            openNationSelectionGUI(player);
        } else {
            player.sendMessage(ChatColor.RED + "Failed to leave your nation!");
        }
    }
    
    private void handleComprehensiveOverviewClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        if (event.getSlot() == 45) { // Back button
            handleBackNavigation(player);
        }
    }
    
    private void teleportToNation(Player player, Nation nation) {
        // Calculate actual center from nation borders instead of using hardcoded values
        int centerX = nation.getBorders().getCenterX();
        int centerZ = nation.getBorders().getCenterZ();
        
        Location teleportLoc = new Location(
            player.getWorld(),
            centerX,
            player.getWorld().getHighestBlockYAt(centerX, centerZ) + 1,
            centerZ
        );
        
        player.teleport(teleportLoc);
        player.sendMessage(ChatColor.GREEN + "Teleported to " + NationColors.getColoredNationName(nation.getId()) + ChatColor.GREEN + " center!");
    }
    
    private void openConfirmationGUI(Player player, Nation nation, String currentNation) {
        Inventory confirmGui = Bukkit.createInventory(null, 27, "Join " + nation.getDisplayName() + "?");
        
        // Add glass pane border (smaller GUI)
        addGlassPaneBorderSmall(confirmGui);
        
        
        // Confirm button (Yes)
        confirmGui.setItem(12, new ItemBuilder(Material.EMERALD_BLOCK)
                .setDisplayName("&a&lYES")
                .setLore("&7Click to join " + getColoredNationName(nation.getId()))
                .build());
        
        // Cancel button (No)  
        confirmGui.setItem(14, new ItemBuilder(Material.REDSTONE_BLOCK)
                .setDisplayName("&c&lNO")
                .setLore("&7Return to menu")
                .build());
        
        player.openInventory(confirmGui);
    }
    
    private void handleConfirmationClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        String title = event.getView().getTitle();
        String nationName = title.replace("Join ", "").replace("?", "");
        
        // Find the nation by display name
        Nation targetNation = null;
        for (Nation nation : nationManager.getAllNations().values()) {
            if (nation.getDisplayName().equals(nationName)) {
                targetNation = nation;
                break;
            }
        }
        
        if (targetNation == null) {
            player.sendMessage(ChatColor.RED + "Nation not found!");
            player.closeInventory();
            return;
        }
        
        UUID playerId = player.getUniqueId();
        String currentNation = playerManager.getPlayerNation(playerId);
        
        switch (event.getSlot()) {
            case 12: // Yes - Join Nation
                if (playerManager.joinNation(player, targetNation.getId())) {
                    player.sendMessage(ChatColor.GREEN + "✓ Successfully joined " + NationColors.getColoredNationName(targetNation.getId()) + ChatColor.GREEN + "!");
                    player.sendMessage(ChatColor.YELLOW + "Welcome to your new nation!");
                    
                    teleportToNation(player, targetNation);
                    player.closeInventory();
                } else {
                    player.sendMessage(ChatColor.RED + "Failed to join " + NationColors.getColoredNationName(targetNation.getId()) + ChatColor.RED + "!");
                    player.closeInventory();
                }
                break;
                
            case 14: // No - Cancel
                openNationSelectionGUI(player);
                break;
                
            default:
                break;
        }
    }
    
    private String getLocationDescription(String nationId) {
        switch (nationId.toLowerCase()) {
            case "america":
                return "West";
            case "europe":
                return "North";
            case "africa":
                return "Central";
            case "asia":
                return "East";
            case "antarctica":
                return "South";
            default:
                return "Unknown";
        }
    }
    
    private String getColoredNationName(String nationId) {
        return NationColors.getColoredNationName(nationId);
    }
    
    private String formatDisasterName(String disasterId) {
        String[] words = disasterId.replace("_", " ").toLowerCase().split(" ");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }
        
        return result.toString().trim();
    }
    
    private List<String> getMissilesFromConfig() {
        List<String> missiles = new ArrayList<>();
        try {
            if (plugin.getConfig().contains("rockets")) {
                for (String rocketKey : plugin.getConfig().getConfigurationSection("rockets").getKeys(false)) {
                    String displayName = plugin.getConfig().getString("rockets." + rocketKey + ".displayName", rocketKey.toUpperCase());
                    missiles.add(displayName);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not load missiles from config: " + e.getMessage());
        }
        return missiles;
    }
    
    private Material getNationMaterial(String nationId) {
        switch (nationId.toLowerCase()) {
            case "america":
                return Material.DIAMOND_BLOCK;
            case "europe":
                return Material.EMERALD_BLOCK;
            case "africa":
                return Material.SANDSTONE;
            case "asia":
                return Material.GOLD_BLOCK;
            case "antarctica":
                return Material.ICE;
            default:
                return Material.STONE;
        }
    }
    
    private String getMissileDisplayName(String missileKey) {
        try {
            String displayName = plugin.getConfig().getString("rockets." + missileKey + ".displayName");
            return displayName != null ? displayName : missileKey.toUpperCase();
        } catch (Exception e) {
            return missileKey.toUpperCase();
        }
    }
    
    public void openMissilesGUI(Player player, String nationId) {
        openMissilesGUI(player, nationId, null);
    }
    
    public void openMissilesGUI(Player player, String nationId, String fromGUI) {
        if (fromGUI != null) {
            pushToNavigationStack(player, fromGUI);
        }
        
        Nation nation = nationManager.getAllNations().get(nationId);
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Nation not found!");
            return;
        }
        
        Inventory gui = Bukkit.createInventory(null, 54, nation.getDisplayName() + " Missiles");
        
        // Add glass pane border
        addGlassPaneBorder(gui);
        
        // Add back button if there's a previous GUI
        if (hasBackButton(nation.getDisplayName() + " Missiles", player)) {
            gui.setItem(45, new ItemBuilder(Material.ARROW)
                    .setDisplayName("&7« Back")
                    .setLore("&7Return to previous menu")
                    .build());
        }
        
        // Title item
        gui.setItem(4, new ItemBuilder(Material.TNT)
                .setDisplayName("&c&l" + nation.getDisplayName() + " Arsenal")
                .setLore(
                    "&7Missile types available to this nation",
                    "&7Total: &f" + (nation.getMissileTypes() != null ? nation.getMissileTypes().size() : 0)
                )
                .build());
        
        // Display missiles
        if (nation.getMissileTypes() != null && !nation.getMissileTypes().isEmpty()) {
            int slot = 10;
            for (String missileKey : nation.getMissileTypes()) {
                if (slot > 43) break; // Don't overflow GUI
                
                String displayName = getMissileDisplayName(missileKey);
                String explosionType = plugin.getConfig().getString("rockets." + missileKey + ".explosionType", "DEFAULT");
                double speed = plugin.getConfig().getDouble("rockets." + missileKey + ".speed", 1.0);
                
                Material material = getMissileMaterial(missileKey);
                
                // Get additional config details
                double arcScale = plugin.getConfig().getDouble("rockets." + missileKey + ".arcScale", 1.0);
                
                gui.setItem(slot, new ItemBuilder(material)
                        .setDisplayName("&6&l" + displayName)
                        .setLore(
                            "&7Type: &f" + explosionType,
                            "&7Speed: &f" + speed,
                            "&7Arc Scale: &f" + arcScale,
                            "",
                            getMissileDescription(missileKey)
                        )
                        .build());
                
                slot++;
                if (slot % 9 == 8) slot += 2; // Skip to next row, avoid edges
            }
        } else {
            gui.setItem(22, new ItemBuilder(Material.BARRIER)
                    .setDisplayName("&c&lNo Missiles")
                    .setLore("&7This nation has no configured missiles")
                    .build());
        }
        
        player.openInventory(gui);
    }
    
    public void openDefenseGUI(Player player, String nationId) {
        openDefenseGUI(player, nationId, null);
    }
    
    public void openDefenseGUI(Player player, String nationId, String fromGUI) {
        if (fromGUI != null) {
            pushToNavigationStack(player, fromGUI);
        }
        
        Nation nation = nationManager.getAllNations().get(nationId);
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Nation not found!");
            return;
        }
        
        Inventory gui = Bukkit.createInventory(null, 54, nation.getDisplayName() + " Defenses");
        
        // Add glass pane border
        addGlassPaneBorder(gui);
        
        // Add back button if there's a previous GUI
        if (hasBackButton(nation.getDisplayName() + " Defenses", player)) {
            gui.setItem(45, new ItemBuilder(Material.ARROW)
                    .setDisplayName("&7« Back")
                    .setLore("&7Return to previous menu")
                    .build());
        }
        
        // Title item
        gui.setItem(4, new ItemBuilder(Material.CROSSBOW)
                .setDisplayName("&9&l" + nation.getDisplayName() + " Defense Systems")
                .setLore("&7Anti-air and defensive capabilities")
                .build());
        
        // Anti-Air Systems
        gui.setItem(20, new ItemBuilder(Material.TARGET)
                .setDisplayName("&a&lAnti-Air Systems")
                .setLore(
                    "&7Automated defense platforms",
                    "&7• Patriot missile batteries",
                    "&7• THAAD interceptors", 
                    "&7• Iron Dome systems",
                    "&7Range: 50-200 blocks",
                    "&7Accuracy: 65-95%"
                )
                .build());
        
        // Reinforcement Systems
        gui.setItem(22, new ItemBuilder(Material.OBSIDIAN)
                .setDisplayName("&7&lReinforced Structures")
                .setLore(
                    "&7Block reinforcement capabilities",
                    "&7• Reinforcement powder usage",
                    "&7• Strategic bunker locations",
                    "&7• Blast-resistant construction",
                    "&7Protection Level: Variable"
                )
                .build());
        
        // Strategic Defense
        gui.setItem(24, new ItemBuilder(Material.SHIELD)
                .setDisplayName("&6&lStrategic Defense")
                .setLore(
                    "&7Overall defensive rating",
                    "&7Location Advantage: &f" + getLocationAdvantage(nation.getId()),
                    "&7Natural Barriers: &f" + getNaturalBarriers(nation.getId()),
                    "&7Defense Readiness: &f" + getDefenseReadiness(nation.getId())
                )
                .build());
        
        player.openInventory(gui);
    }
    
    public void openDisastersGUI(Player player, String nationId) {
        openDisastersGUI(player, nationId, null);
    }
    
    public void openDisastersGUI(Player player, String nationId, String fromGUI) {
        if (fromGUI != null) {
            pushToNavigationStack(player, fromGUI);
        }
        
        Nation nation = nationManager.getAllNations().get(nationId);
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Nation not found!");
            return;
        }
        
        Inventory gui = Bukkit.createInventory(null, 54, nation.getDisplayName() + " Disasters");
        
        // Add glass pane border
        addGlassPaneBorder(gui);
        
        // Add back button if there's a previous GUI
        if (hasBackButton(nation.getDisplayName() + " Disasters", player)) {
            gui.setItem(45, new ItemBuilder(Material.ARROW)
                    .setDisplayName("&7« Back")
                    .setLore("&7Return to previous menu")
                    .build());
        }
        
        // Title item
        gui.setItem(4, new ItemBuilder(Material.BLAZE_POWDER)
                .setDisplayName("&4&l" + nation.getDisplayName() + " Natural Disasters")
                .setLore(
                    "&7Disasters that can affect this nation",
                    "&7Total Types: &f" + nation.getDisasters().size()
                )
                .build());
        
        // Display disasters
        int slot = 10;
        for (Disaster disaster : nation.getDisasters().values()) {
            if (slot > 43) break; // Don't overflow GUI
            
            String disasterName = formatDisasterName(disaster.getId());
            boolean isActive = disaster.isActive();
            
            Material material = getDisasterMaterial(disaster.getId());
            
            List<String> lore = new ArrayList<>();
            lore.add("&7Status: " + (isActive ? "&4Active" : "&7Inactive"));
            if (isActive) {
                long timeLeft = disaster.getEndTime() - System.currentTimeMillis();
                if (timeLeft > 0) {
                    lore.add("&7Ends in: &f" + (timeLeft / 1000) + "s");
                }
            }
            lore.add("");
            lore.add("&7Effect: &f" + getDisasterDescription(disaster.getId()));
            
            gui.setItem(slot, new ItemBuilder(material)
                    .setDisplayName((isActive ? "&4&l" : "&6&l") + disasterName)
                    .setLore(lore)
                    .build());
            
            slot++;
            if (slot % 9 == 8) slot += 2; // Skip to next row, avoid edges
        }
        
        player.openInventory(gui);
    }
    
    private String getNationIdFromDisplayName(String displayName) {
        for (Nation nation : nationManager.getAllNations().values()) {
            if (nation.getDisplayName().equals(displayName)) {
                return nation.getId();
            }
        }
        return null;
    }
    
    private Material getMissileMaterial(String missileKey) {
        if (missileKey.contains("r5") || missileKey.toLowerCase().contains("nuclear")) return Material.TNT;
        if (missileKey.contains("r4")) return Material.FIRE_CHARGE;
        if (missileKey.contains("r3")) return Material.FIREWORK_ROCKET;
        if (missileKey.contains("r2")) return Material.CROSSBOW;
        return Material.BOW;
    }
    
    private Material getDisasterMaterial(String disasterId) {
        String id = disasterId.toLowerCase();
        if (id.contains("meteor")) return Material.FIRE_CHARGE;
        if (id.contains("wildfire")) return Material.BLAZE_POWDER;
        if (id.contains("tornado")) return Material.GRAY_DYE;
        if (id.contains("flood")) return Material.WATER_BUCKET;
        if (id.contains("plague")) return Material.POISONOUS_POTATO;
        if (id.contains("storm")) return Material.TRIDENT;
        if (id.contains("drought")) return Material.DEAD_BUSH;
        if (id.contains("sandstorm")) return Material.SAND;
        if (id.contains("blizzard")) return Material.SNOWBALL;
        if (id.contains("ice")) return Material.ICE;
        return Material.BARRIER;
    }
    
    private String getClimateDescription(String nationId) {
        switch (nationId.toLowerCase()) {
            case "america": return "Temperate";
            case "europe": return "Continental";
            case "africa": return "Tropical";
            case "asia": return "Diverse";
            case "antarctica": return "Polar";
            default: return "Unknown";
        }
    }
    
    private String getDisasterResistance(String nationId) {
        switch (nationId.toLowerCase()) {
            case "america": return "Fire Resistant";
            case "europe": return "Flood Prepared";
            case "africa": return "Drought Adapted";
            case "asia": return "Multi-Hazard";
            case "antarctica": return "Cold Hardy";
            default: return "Standard";
        }
    }
    
    private String getStrategicValue(String nationId) {
        switch (nationId.toLowerCase()) {
            case "america": return "High (Resources)";
            case "europe": return "High (Technology)";
            case "africa": return "Medium (Central)";
            case "asia": return "High (Population)";
            case "antarctica": return "Low (Isolated)";
            default: return "Medium";
        }
    }
    
    private String getLocationAdvantage(String nationId) {
        switch (nationId.toLowerCase()) {
            case "america": return "High (Western Position)";
            case "europe": return "Strategic (Northern)";
            case "africa": return "Central Hub";
            case "asia": return "Eastern Stronghold";
            case "antarctica": return "Isolated Defense";
            default: return "Standard";
        }
    }
    
    private String getNaturalBarriers(String nationId) {
        switch (nationId.toLowerCase()) {
            case "america": return "Mountain Ranges";
            case "europe": return "Rivers & Forests";
            case "africa": return "Desert Terrain";
            case "asia": return "Varied Geography";
            case "antarctica": return "Extreme Cold";
            default: return "Standard Terrain";
        }
    }
    
    private String getDefenseReadiness(String nationId) {
        switch (nationId.toLowerCase()) {
            case "america": return "Advanced";
            case "europe": return "Modern";
            case "africa": return "Developing";
            case "asia": return "High-Tech";
            case "antarctica": return "Minimal";
            default: return "Standard";
        }
    }
    
    private String getDisasterDescription(String disasterId) {
        String id = disasterId.toLowerCase();
        if (id.contains("meteor")) return "TNT meteors with rare ore drops";
        if (id.contains("wildfire")) return "Spreading fires through forests";
        if (id.contains("tornado")) return "Moving vortex causing chaos";
        if (id.contains("flood")) return "Rivers overflow, farmland wiped";
        if (id.contains("plague")) return "Sickness spreads, causes weakness";
        if (id.contains("storm")) return "Lightning strikes with fire";
        if (id.contains("drought")) return "Water sources dry up, crops fail";
        if (id.contains("sandstorm")) return "Massive visibility reduction";
        if (id.contains("blizzard")) return "Heavy snow, near-zero visibility";
        if (id.contains("ice")) return "Freezing conditions, water turns ice";
        return "Unknown disaster effect";
    }
    
    // Methods moved from NationsCommand for comprehensive overviews
    private void openAllMissilesGUI(Player player) {
        openAllMissilesGUI(player, null);
    }
    
    private void openAllMissilesGUI(Player player, String fromGUI) {
        if (fromGUI != null) {
            pushToNavigationStack(player, fromGUI);
        }
        
        Inventory gui = Bukkit.createInventory(null, 54, "All Missile Types");
        
        // Add glass pane border
        addGlassPaneBorder(gui);
        
        // Add back button if there's a previous GUI
        if (hasBackButton("All Missile Types", player)) {
            gui.setItem(45, new ItemBuilder(Material.ARROW)
                    .setDisplayName("&7« Back")
                    .setLore("&7Return to previous menu")
                    .build());
        }
        
        // Title
        gui.setItem(4, new ItemBuilder(Material.TNT)
                .setDisplayName("&c&lComplete Missile Arsenal")
                .setLore(
                    "&7All missile types in the system",
                    "&7Learn about damage, effects, and usage"
                )
                .build());
        
        // Display missiles from config
        if (plugin.getConfig().contains("rockets")) {
            int slot = 19;
            for (String missileKey : plugin.getConfig().getConfigurationSection("rockets").getKeys(false)) {
                if (slot > 25) break;
                
                String displayName = plugin.getConfig().getString("rockets." + missileKey + ".displayName", missileKey.toUpperCase());
                String explosionType = plugin.getConfig().getString("rockets." + missileKey + ".explosionType", "DEFAULT");
                double speed = plugin.getConfig().getDouble("rockets." + missileKey + ".speed", 1.0);
                
                // Get additional config details
                double arcScale = plugin.getConfig().getDouble("rockets." + missileKey + ".arcScale", 1.0);
                
                List<String> lore = new ArrayList<>();
                lore.add("&7Type: &f" + explosionType);
                lore.add("&7Speed: &f" + speed);
                lore.add("&7Arc Scale: &f" + arcScale);
                lore.add("");
                lore.add("&7Nations with this missile:");
                
                // Check which nations have this missile
                for (Nation nation : nationManager.getAllNations().values()) {
                    if (nation.getMissileTypes() != null && nation.getMissileTypes().contains(missileKey)) {
                        lore.add("&7• &f" + nation.getDisplayName());
                    }
                }
                
                lore.add("");
                lore.add(getMissileDescription(missileKey));
                
                gui.setItem(slot, new ItemBuilder(getMissileMaterial(missileKey))
                        .setDisplayName("&6&l" + displayName)
                        .setLore(lore)
                        .build());
                
                slot++;
            }
        }
        
        player.openInventory(gui);
    }
    
    private void openAllAntiAirGUI(Player player) {
        openAllAntiAirGUI(player, null);
    }
    
    private void openAllAntiAirGUI(Player player, String fromGUI) {
        if (fromGUI != null) {
            pushToNavigationStack(player, fromGUI);
        }
        
        Inventory gui = Bukkit.createInventory(null, 54, "All Anti-Air Systems");
        
        // Add glass pane border
        addGlassPaneBorder(gui);
        
        // Add back button if there's a previous GUI
        if (hasBackButton("All Anti-Air Systems", player)) {
            gui.setItem(45, new ItemBuilder(Material.ARROW)
                    .setDisplayName("&7« Back")
                    .setLore("&7Return to previous menu")
                    .build());
        }
        
        // Title
        gui.setItem(4, new ItemBuilder(Material.CROSSBOW)
                .setDisplayName("&9&lAnti-Aircraft Defense Systems")
                .setLore(
                    "&7Overview of all defensive systems",
                    "&7Learn about interception capabilities"
                )
                .build());
        
        // Info about anti-air systems
        gui.setItem(20, new ItemBuilder(Material.TARGET)
                .setDisplayName("&a&lAutomated Systems")
                .setLore(
                    "&7AI-controlled interceptor systems",
                    "&7• Automatic threat detection",
                    "&7• High response time",
                    "&7• Configurable accuracy",
                    "&7• 24/7 operational capability"
                )
                .build());
        
        gui.setItem(22, new ItemBuilder(Material.LEVER)
                .setDisplayName("&e&lManual Systems")
                .setLore(
                    "&7Player-operated defense systems",
                    "&7• Requires nearby operators",
                    "&7• Higher accuracy potential",
                    "&7• Strategic positioning",
                    "&7• Human decision making"
                )
                .build());
        
        gui.setItem(24, new ItemBuilder(Material.REDSTONE)
                .setDisplayName("&c&lSystem Specifications")
                .setLore(
                    "&7Technical information:",
                    "&7• Range: 50-200 blocks",
                    "&7• Accuracy: 65-95%",
                    "&7• Reload time: 3-8 seconds",
                    "&7• Interceptor speed: 2-4x",
                    "&7• Multiple targeting modes"
                )
                .build());
        
        player.openInventory(gui);
    }
    
    private void openAllDisastersGUI(Player player) {
        openAllDisastersGUI(player, null);
    }
    
    private void openAllDisastersGUI(Player player, String fromGUI) {
        if (fromGUI != null) {
            pushToNavigationStack(player, fromGUI);
        }
        
        Inventory gui = Bukkit.createInventory(null, 54, "All Natural Disasters");
        
        // Add glass pane border
        addGlassPaneBorder(gui);
        
        // Add back button if there's a previous GUI
        if (hasBackButton("All Natural Disasters", player)) {
            gui.setItem(45, new ItemBuilder(Material.ARROW)
                    .setDisplayName("&7« Back")
                    .setLore("&7Return to previous menu")
                    .build());
        }
        
        // Title
        gui.setItem(4, new ItemBuilder(Material.NETHER_STAR)
                .setDisplayName("&4&lNatural Disasters Encyclopedia")
                .setLore(
                    "&7Complete guide to all disaster types",
                    "&7Learn about effects and nation impacts"
                )
                .build());
        
        // America disasters
        gui.setItem(11, new ItemBuilder(Material.FIRE_CHARGE)
                .setDisplayName("&c&lAmerica Disasters")
                .setLore(
                    "&6Meteor Showers:",
                    "&7• TNT meteors with flame trails",
                    "&7• Rare ore drops on impact",
                    "",
                    "&6Wildfires:",
                    "&7• Spreads fire through forests",
                    "&7• Burns grasslands during dry periods",
                    "",
                    "&6Tornadoes:",
                    "&7• Moving vortex pulls players",
                    "&7• Creates widespread chaos"
                )
                .build());
        
        // Europe disasters
        gui.setItem(13, new ItemBuilder(Material.WATER_BUCKET)
                .setDisplayName("&a&lEurope Disasters")
                .setLore(
                    "&9Flooding:",
                    "&7• Rivers overflow extensively",
                    "&7• Farmland gets completely wiped",
                    "",
                    "&5Plagues:",
                    "&7• Sickness spreads slowly",
                    "&7• Causes hunger and weakness",
                    "",
                    "&8Storms:",
                    "&7• Lightning strikes with fire chance",
                    "&7• Environmental damage"
                )
                .build());
        
        // Africa disasters
        gui.setItem(15, new ItemBuilder(Material.DEAD_BUSH)
                .setDisplayName("&6&lAfrica Disasters")
                .setLore(
                    "&6Droughts:",
                    "&7• Water sources completely dry up",
                    "&7• Crops fail, severe hunger effects",
                    "",
                    "&e Sandstorms:",
                    "&7• Massive visibility reduction",
                    "&7• Blindness and slow movement"
                )
                .build());
        
        // Antarctica disasters
        gui.setItem(31, new ItemBuilder(Material.SNOWBALL)
                .setDisplayName("&f&lAntarctica Disasters")
                .setLore(
                    "&fBlizzards:",
                    "&7• Heavy snow reduces visibility",
                    "&7• Near-zero visibility conditions",
                    "&7• Severe movement penalties",
                    "",
                    "&bIce Storms:",
                    "&7• Freezing conditions everywhere",
                    "&7• Water turns to ice blocks",
                    "&7• Cold damage to players"
                )
                .build());
        
        player.openInventory(gui);
    }
    
    private String getMissileDescription(String missileKey) {
        switch (missileKey.toLowerCase()) {
            case "r1": return "&7Standard explosive with reliable damage";
            case "r2": return "&7Bunker buster that drills deep underground";
            case "r3": return "&7Cluster bomb with horizontal spread explosions";
            case "r4": return "&7Thermobaric with magma block placement";
            case "r5": return "&7Nuclear with massive mushroom cloud";
            default: return "&7Advanced military-grade missile system";
        }
    }
    
    // Helper method to add glass pane borders to 54-slot GUIs
    private void addGlassPaneBorder(Inventory gui) {
        ItemStack glassPaneItem = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .setDisplayName("&7")
                .build();
        
        // Top row (0-8)
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, glassPaneItem);
        }
        
        // Bottom row (45-53)
        for (int i = 45; i < 54; i++) {
            gui.setItem(i, glassPaneItem);
        }
        
        // Left and right columns
        for (int row = 1; row < 5; row++) { // rows 1-4
            gui.setItem(row * 9, glassPaneItem); // left column
            gui.setItem(row * 9 + 8, glassPaneItem); // right column
        }
    }
    
    // Helper method to add glass pane borders to smaller GUIs (27 slots)
    private void addGlassPaneBorderSmall(Inventory gui) {
        ItemStack glassPaneItem = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .setDisplayName("&7")
                .build();
        
        // Top row (0-8)
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, glassPaneItem);
        }
        
        // Bottom row (18-26)
        for (int i = 18; i < 27; i++) {
            gui.setItem(i, glassPaneItem);
        }
        
        // Left and right columns for middle row
        gui.setItem(9, glassPaneItem);  // left
        gui.setItem(17, glassPaneItem); // right
    }
}