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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NationGUI implements Listener {
    
    private final JavaPlugin plugin;
    private final NationManager nationManager;
    private final NationPlayerManager playerManager;
    
    public NationGUI(JavaPlugin plugin, NationManager nationManager, NationPlayerManager playerManager) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.playerManager = playerManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    public void openNationSelectionGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "Select Your Nation");
        
        UUID playerId = player.getUniqueId();
        String currentNation = playerManager.getPlayerNation(playerId);
        boolean canSwitch = playerManager.canPlayerSwitch(playerId);
        
        gui.setItem(53, new ItemBuilder(Material.COMPASS)
                .setDisplayName("&6&lNation Selection")
                .setLore(
                    "&7Choose your nation to join the fight!",
                    "",
                    "&7Current Nation: &f" + (currentNation != null ? 
                        nationManager.getAllNations().get(currentNation).getDisplayName() : "None"),
                    "&7Can Switch: " + (canSwitch ? "&aYes" : "&cNo"),
                    "",
                    "&eClick on a nation below to join!"
                )
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
                        "&7Leave your current nation",
                        "&cWarning: You may not be able to rejoin immediately!",
                        "",
                        "&eClick to leave " + nationManager.getAllNations().get(currentNation).getDisplayName()
                    )
                    .build());
        }
        
        
        player.openInventory(gui);
    }
    
    public void openNationsListGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "Nations Overview");
        
        UUID playerId = player.getUniqueId();
        String currentNation = playerManager.getPlayerNation(playerId);
        
        // Add info compass at top right
        gui.setItem(53, new ItemBuilder(Material.COMPASS)
                .setDisplayName("&6&lNations Overview")
                .setLore(
                    "&7Explore and learn about nations",
                    "",
                    "&7Your Nation: &f" + (currentNation != null ? 
                        nationManager.getAllNations().get(currentNation).getDisplayName() : "None"),
                    "",
                    "&eClick on a nation to view details!"
                )
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
        
        player.openInventory(gui);
    }
    
    private ItemStack createNationInfoItem(Nation nation, Material material, String currentNation) {
        List<String> lore = new ArrayList<>();
        
        int onlineCount = playerManager.getOnlinePlayerCountInNation(nation.getId());
        lore.add("&7Citizens: &a" + onlineCount + "&7/&f" + nation.getTotalPlayers());
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
        Nation nation = nationManager.getAllNations().get(nationId);
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Nation not found!");
            return;
        }
        
        Inventory gui = Bukkit.createInventory(null, 54, nation.getDisplayName() + " Details");
        
        UUID playerId = player.getUniqueId();
        String currentNation = playerManager.getPlayerNation(playerId);
        boolean isMyNation = nation.getId().equals(currentNation);
        
        // Back button
        gui.setItem(45, new ItemBuilder(Material.ARROW)
                .setDisplayName("&7« Back to Nations")
                .setLore("&7Return to nations overview")
                .build());
        
        // Nation info item (center top)
        gui.setItem(13, new ItemBuilder(getNationMaterial(nation.getId()))
                .setDisplayName("&6&l" + nation.getDisplayName())
                .setLore(
                    "&7Region: &f" + getLocationDescription(nation.getId()),
                    "&7Citizens: &a" + playerManager.getOnlinePlayerCountInNation(nation.getId()) + "&7/&f" + nation.getTotalPlayers(),
                    "&7Borders: &f" + nation.getBorders(),
                    "",
                    isMyNation ? "&a✓ Your Nation" : "&7Foreign Nation"
                )
                .build());
        
        // Missiles section
        gui.setItem(20, new ItemBuilder(Material.TNT)
                .setDisplayName("&c&lMissiles")
                .setLore("&7Click to view missile arsenal")
                .build());
        
        // Statistics section
        gui.setItem(22, new ItemBuilder(Material.BOOK)
                .setDisplayName("&e&lStatistics")
                .setLore("&7Click to view nation stats")
                .build());
        
        // Disasters section
        gui.setItem(24, new ItemBuilder(Material.BLAZE_POWDER)
                .setDisplayName("&4&lNatural Disasters")
                .setLore("&7Click to view disaster info")
                .build());
        
        // Teleport to nation (if not current nation or if current nation)
        if (isMyNation) {
            gui.setItem(40, new ItemBuilder(Material.ENDER_PEARL)
                    .setDisplayName("&a&lTeleport to Nation Center")
                    .setLore("&7Click to teleport to your nation")
                    .build());
        } else {
            gui.setItem(40, new ItemBuilder(Material.ENDER_EYE)
                    .setDisplayName("&e&lTeleport to Nation")
                    .setLore(
                        "&7Visit " + nation.getDisplayName(),
                        "&7Click to teleport"
                    )
                    .build());
        }
        
        player.openInventory(gui);
    }
    
    private ItemStack createNationItem(Nation nation, Material material, String currentNation, boolean canSwitch) {
        List<String> lore = new ArrayList<>();
        
        int onlineCount = playerManager.getOnlinePlayerCountInNation(nation.getId());
        lore.add("&7Citizens: &a" + onlineCount + "&7/&f" + nation.getTotalPlayers());
        lore.add("&7Region: &f" + getLocationDescription(nation.getId()));
        lore.add("");
        
        lore.add("&6Available Missiles:");
        if (nation.getMissileTypes() != null && !nation.getMissileTypes().isEmpty()) {
            for (String missileKey : nation.getMissileTypes()) {
                String displayName = getMissileDisplayName(missileKey);
                lore.add("&7• &f" + displayName);
            }
        } else {
            lore.add("&7• &cNone configured");
        }
        lore.add("");
        
        lore.add("&cNatural Disasters:");
        for (Disaster disaster : nation.getDisasters().values()) {
            String status = disaster.isActive() ? " &4(ACTIVE)" : "";
            String disasterName = formatDisasterName(disaster.getId());
            lore.add("&7• &f" + disasterName + status);
        }
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
                .setDisplayName("&6&l" + nation.getDisplayName())
                .setLore(lore)
                .build();
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
        
        // Handle statistics GUI
        if (title.endsWith(" Statistics")) {
            handleStatisticsClick(event, player);
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
    }
    
    private void handleNationsOverviewClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        switch (event.getSlot()) {
            case 13: // Europe (North)
                openNationDetailsGUI(player, "europe");
                break;
            case 21: // America (West)
                openNationDetailsGUI(player, "america");
                break;
            case 22: // Africa (Center)
                openNationDetailsGUI(player, "africa");
                break;
            case 23: // Asia (East)
                openNationDetailsGUI(player, "asia");
                break;
            case 31: // Antarctica (South)
                openNationDetailsGUI(player, "antarctica");
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
                openMissilesGUI(player, nationId);
                break;
            case 22: // Statistics
                openStatisticsGUI(player, nationId);
                break;
            case 24: // Disasters
                openDisastersGUI(player, nationId);
                break;
            case 40: // Teleport
                Nation nation = nationManager.getAllNations().get(nationId);
                if (nation != null) {
                    teleportToNation(player, nation);
                    player.closeInventory();
                }
                break;
            case 45: // Back button
                openNationsListGUI(player);
                break;
            default:
                break;
        }
    }
    
    private void handleMissilesClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        if (event.getSlot() == 45) { // Back button
            String title = event.getView().getTitle();
            String nationName = title.replace(" Missiles", "");
            String nationId = getNationIdFromDisplayName(nationName);
            if (nationId != null) {
                openNationDetailsGUI(player, nationId);
            }
        }
    }
    
    private void handleStatisticsClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        if (event.getSlot() == 45) { // Back button
            String title = event.getView().getTitle();
            String nationName = title.replace(" Statistics", "");
            String nationId = getNationIdFromDisplayName(nationName);
            if (nationId != null) {
                openNationDetailsGUI(player, nationId);
            }
        }
    }
    
    private void handleDisastersClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        if (event.getSlot() == 45) { // Back button
            String title = event.getView().getTitle();
            String nationName = title.replace(" Disasters", "");
            String nationId = getNationIdFromDisplayName(nationName);
            if (nationId != null) {
                openNationDetailsGUI(player, nationId);
            }
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
        player.sendMessage(ChatColor.GREEN + "Teleported to " + nation.getDisplayName() + " center!");
    }
    
    private void openConfirmationGUI(Player player, Nation nation, String currentNation) {
        Inventory confirmGui = Bukkit.createInventory(null, 27, "Join " + nation.getDisplayName() + "?");
        
        
        // Confirm button (Yes)
        confirmGui.setItem(11, new ItemBuilder(Material.EMERALD_BLOCK)
                .setDisplayName("&a&lYES - Join Nation")
                .setLore(
                    "&7Click to join " + nation.getDisplayName(),
                    currentNation != null ? "&7This will leave your current nation" : "",
                    "&7You will be teleported to the nation center"
                )
                .build());
        
        // Cancel button (No)
        confirmGui.setItem(15, new ItemBuilder(Material.REDSTONE_BLOCK)
                .setDisplayName("&c&lNO - Cancel")
                .setLore("&7Return to nation selection")
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
            case 11: // Yes - Join Nation
                if (playerManager.joinNation(player, targetNation.getId())) {
                    player.sendMessage(ChatColor.GREEN + "✓ Successfully joined " + targetNation.getDisplayName() + "!");
                    player.sendMessage(ChatColor.YELLOW + "Welcome to your new nation!");
                    
                    teleportToNation(player, targetNation);
                    player.closeInventory();
                } else {
                    player.sendMessage(ChatColor.RED + "Failed to join " + targetNation.getDisplayName() + "!");
                    player.closeInventory();
                }
                break;
                
            case 15: // No - Cancel
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
        Nation nation = nationManager.getAllNations().get(nationId);
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Nation not found!");
            return;
        }
        
        Inventory gui = Bukkit.createInventory(null, 54, nation.getDisplayName() + " Missiles");
        
        // Back button
        gui.setItem(45, new ItemBuilder(Material.ARROW)
                .setDisplayName("&7« Back to " + nation.getDisplayName())
                .setLore("&7Return to nation details")
                .build());
        
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
                
                gui.setItem(slot, new ItemBuilder(material)
                        .setDisplayName("&6&l" + displayName)
                        .setLore(
                            "&7Type: &f" + explosionType,
                            "&7Speed: &f" + speed,
                            "&7Key: &f" + missileKey,
                            "",
                            "&eUse: &f/r " + missileKey
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
    
    public void openStatisticsGUI(Player player, String nationId) {
        Nation nation = nationManager.getAllNations().get(nationId);
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Nation not found!");
            return;
        }
        
        Inventory gui = Bukkit.createInventory(null, 54, nation.getDisplayName() + " Statistics");
        
        // Back button
        gui.setItem(45, new ItemBuilder(Material.ARROW)
                .setDisplayName("&7« Back to " + nation.getDisplayName())
                .setLore("&7Return to nation details")
                .build());
        
        // Title item
        gui.setItem(4, new ItemBuilder(Material.BOOK)
                .setDisplayName("&e&l" + nation.getDisplayName() + " Statistics")
                .setLore("&7Detailed information about this nation")
                .build());
        
        // Population stats
        gui.setItem(20, new ItemBuilder(Material.PLAYER_HEAD)
                .setDisplayName("&a&lPopulation")
                .setLore(
                    "&7Online Citizens: &a" + playerManager.getOnlinePlayerCountInNation(nation.getId()),
                    "&7Total Citizens: &f" + nation.getTotalPlayers(),
                    "&7Region: &f" + getLocationDescription(nation.getId())
                )
                .build());
        
        // Territory stats
        gui.setItem(22, new ItemBuilder(Material.MAP)
                .setDisplayName("&6&lTerritory")
                .setLore(
                    "&7Borders: &f" + nation.getBorders(),
                    "&7Area: &f129x129 blocks",
                    "&7Climate: &f" + getClimateDescription(nation.getId())
                )
                .build());
        
        // Military stats
        gui.setItem(24, new ItemBuilder(Material.DIAMOND_SWORD)
                .setDisplayName("&c&lMilitary")
                .setLore(
                    "&7Missile Types: &f" + (nation.getMissileTypes() != null ? nation.getMissileTypes().size() : 0),
                    "&7Disaster Resistance: &f" + getDisasterResistance(nation.getId()),
                    "&7Strategic Value: &f" + getStrategicValue(nation.getId())
                )
                .build());
        
        player.openInventory(gui);
    }
    
    public void openDisastersGUI(Player player, String nationId) {
        Nation nation = nationManager.getAllNations().get(nationId);
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Nation not found!");
            return;
        }
        
        Inventory gui = Bukkit.createInventory(null, 54, nation.getDisplayName() + " Disasters");
        
        // Back button
        gui.setItem(45, new ItemBuilder(Material.ARROW)
                .setDisplayName("&7« Back to " + nation.getDisplayName())
                .setLore("&7Return to nation details")
                .build());
        
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
}