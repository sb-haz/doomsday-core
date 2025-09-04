package gg.doomsday.core;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import gg.doomsday.core.gui.MissileGUI;
import gg.doomsday.core.gui.utils.GUIBuilder;
import gg.doomsday.core.gui.utils.ItemBuilder;
import gg.doomsday.core.services.MissileService;
import gg.doomsday.core.defense.AntiAirDefense;
import gg.doomsday.core.defense.AntiAirDefenseManager;
import gg.doomsday.core.items.ReinforcementHandler;
import gg.doomsday.core.items.ReinforcementDetectorManager;
import gg.doomsday.core.nations.NationManager;
import gg.doomsday.core.nations.Nation;
import gg.doomsday.core.nations.Disaster;

import java.util.ArrayList;
import java.util.List;

public class GUIManager implements Listener {
    
    private final JavaPlugin plugin;
    private final ReinforcementHandler reinforcementHandler;
    private final ReinforcementDetectorManager detectorManager;
    private final AntiAirDefenseManager antiAirManager;
    private final MissileService missileService;
    private final MissileGUI missileGUI;
    private final NationManager nationManager;
    
    public GUIManager(JavaPlugin plugin, ReinforcementHandler reinforcementHandler, 
                     ReinforcementDetectorManager detectorManager, AntiAirDefenseManager antiAirManager, 
                     MissileService missileService, NationManager nationManager) {
        this.plugin = plugin;
        this.reinforcementHandler = reinforcementHandler;
        this.detectorManager = detectorManager;
        this.antiAirManager = antiAirManager;
        this.missileService = missileService;
        this.nationManager = nationManager;
        this.missileGUI = new MissileGUI(plugin, missileService);
        
        // Register event listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    public void openMainGUI(Player player) {
        Inventory gui = GUIBuilder.createMainGUI();
        
        // Rockets item
        ItemStack rocketsItem = ItemBuilder.createButton(Material.TNT, "§c§lRocket Arsenal", 
            "§7Click to view all configured rockets",
            "§7and teleport to launch positions",
            "",
            "§e> Click to open rocket menu"
        );
        gui.setItem(20, rocketsItem);
        
        // Anti-Air item
        ItemStack antiAirItem = ItemBuilder.createButton(Material.CROSSBOW, "§e§lAnti-Air Defenses", 
            "§7Click to view all defense installations",
            "§7and teleport to their positions", 
            "",
            "§e> Click to open defense menu"
        );
        gui.setItem(22, antiAirItem);
        
        // Custom Items
        ItemStack customItemsItem = ItemBuilder.createButton(Material.CLAY_BALL, "§b§lCustom Items",
            "§7View and obtain custom items",
            "§7for reinforcement and detection",
            "",
            "§e> Click to open items menu"
        );
        gui.setItem(24, customItemsItem);
        
        // Nations System
        ItemStack nationsItem = ItemBuilder.createButton(Material.FILLED_MAP, "§6§lNations & Disasters",
            "§7View information about all nations",
            "§7and their natural disasters",
            "§7Check current location and disasters",
            "",
            "§e> Click to open nations menu"
        );
        gui.setItem(40, nationsItem);
        
        player.openInventory(gui);
    }
    
    public void openNationsGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, GUIBuilder.NATIONS_GUI_TITLE);
        
        // Get player's current nation
        Location playerLoc = player.getLocation();
        Nation currentNation = nationManager.getNationAt(playerLoc);
        
        // Current Location Info (top center)
        List<String> locationLore = new ArrayList<>();
        locationLore.add("§7Your current coordinates:");
        locationLore.add("§f  X: " + String.format("%.1f", playerLoc.getX()));
        locationLore.add("§f  Y: " + String.format("%.1f", playerLoc.getY()));
        locationLore.add("§f  Z: " + String.format("%.1f", playerLoc.getZ()));
        locationLore.add("");
        if (currentNation != null) {
            locationLore.add("§7You are currently in:");
            locationLore.add("§a§l" + currentNation.getDisplayName());
            
            // Check for active disasters
            boolean hasActiveDisasters = false;
            for (Disaster disaster : currentNation.getDisasters().values()) {
                if (disaster.isActive()) {
                    if (!hasActiveDisasters) {
                        locationLore.add("");
                        locationLore.add("§c§lACTIVE DISASTERS:");
                        hasActiveDisasters = true;
                    }
                    long remainingTime = (disaster.getEndTime() - System.currentTimeMillis()) / 1000;
                    locationLore.add("§c• " + disaster.getId().replace("_", " ").toUpperCase() + 
                        " §7(ends in " + remainingTime + "s)");
                }
            }
            if (!hasActiveDisasters) {
                locationLore.add("§a§lNo active disasters");
            }
        } else {
            locationLore.add("§c§lOutside all nation borders");
        }
        
        ItemStack locationItem = ItemBuilder.createItem(Material.COMPASS, "§e§lYour Location", locationLore.toArray(new String[0]));
        gui.setItem(4, locationItem);
        
        // Nation items in their geographic positions
        // Africa (center) - slot 22
        addNationItem(gui, 22, nationManager.getAllNations().get("africa"), Material.ORANGE_WOOL);
        
        // America (west) - slot 21  
        addNationItem(gui, 21, nationManager.getAllNations().get("america"), Material.RED_WOOL);
        
        // Europe (north) - slot 13
        addNationItem(gui, 13, nationManager.getAllNations().get("europe"), Material.BLUE_WOOL);
        
        // Asia (east) - slot 23
        addNationItem(gui, 23, nationManager.getAllNations().get("asia"), Material.LIME_WOOL);
        
        // Antarctica (south) - slot 31
        addNationItem(gui, 31, nationManager.getAllNations().get("antarctica"), Material.WHITE_WOOL);
        
        // Back button
        ItemStack backItem = ItemBuilder.createItem(Material.ARROW, "§f§lBack to Main Menu", "§7Click to return");
        gui.setItem(53, backItem);
        
        player.openInventory(gui);
    }
    
    private void addNationItem(Inventory gui, int slot, Nation nation, Material material) {
        if (nation == null) return;
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Borders:");
        lore.add("§f  X: " + nation.getBorders().getMinX() + " to " + nation.getBorders().getMaxX());
        lore.add("§f  Z: " + nation.getBorders().getMinZ() + " to " + nation.getBorders().getMaxZ());
        lore.add("§f  Y: " + nation.getBorders().getMinY() + " to " + nation.getBorders().getMaxY());
        lore.add("");
        
        // List disasters
        lore.add("§7Possible Disasters:");
        for (Disaster disaster : nation.getDisasters().values()) {
            String status = disaster.isEnabled() ? "§a✓" : "§c✗";
            String active = disaster.isActive() ? " §c[ACTIVE]" : "";
            lore.add("§f  " + status + " " + disaster.getId().replace("_", " ").toUpperCase() + active);
        }
        lore.add("");
        
        // Active disasters count
        int activeCount = 0;
        for (Disaster disaster : nation.getDisasters().values()) {
            if (disaster.isActive()) activeCount++;
        }
        
        if (activeCount > 0) {
            lore.add("§c§lActive Disasters: " + activeCount);
        } else {
            lore.add("§a§lNo Active Disasters");
        }
        
        lore.add("");
        lore.add("§e> Click to teleport to nation center");
        
        ItemStack nationItem = ItemBuilder.createItem(material, "§6§l" + nation.getDisplayName(), lore.toArray(new String[0]));
        
        // Store nation ID in item meta for click handling - using NBT-like approach with lore
        ItemMeta meta = nationItem.getItemMeta();
        List<String> currentLore = meta.getLore();
        currentLore.add("§8§0nation_id:" + nation.getId()); // Hidden at bottom of lore
        meta.setLore(currentLore);
        nationItem.setItemMeta(meta);
        
        gui.setItem(slot, nationItem);
    }
    
    public void openRocketsGUI(Player player) {
        Inventory gui = missileGUI.createRocketsGUI();
        if (gui == null) {
            player.sendMessage("§cNo rockets configured!");
            return;
        }
        player.openInventory(gui);
    }
    
    public void openMissileInfoGUI(Player player, String rocketKey) {
        Inventory gui = missileGUI.createMissileInfoGUI(rocketKey);
        if (gui == null) {
            player.sendMessage("§cMissile configuration not found!");
            return;
        }
        player.openInventory(gui);
    }
    
    private ItemStack createRocketItem(String rocketKey, ConfigurationSection rocketsSection) {
        ConfigurationSection rocket = rocketsSection.getConfigurationSection(rocketKey);
        if (rocket == null) return null;
        
        String displayName = rocket.getString("displayName", rocketKey.toUpperCase());
        String explosionType = rocket.getString("explosionType", "DEFAULT");
        double startX = rocket.getDouble("start.x");
        double startY = rocket.getDouble("start.y");
        double startZ = rocket.getDouble("start.z");
        double endX = rocket.getDouble("end.x");
        double endY = rocket.getDouble("end.y");
        double endZ = rocket.getDouble("end.z");
        double speed = rocket.getDouble("speed", 1.0);
        
        Material iconMaterial = getMaterialForExplosionType(explosionType);
        
        ItemStack rocketItem = ItemBuilder.createItem(iconMaterial, "§c" + displayName,
            "§7Explosion Type: §6" + explosionType,
            "§7Speed: §f" + speed,
            "",
            "§7Launch Position:",
            "§f  X: " + (int)startX + " Y: " + (int)startY + " Z: " + (int)startZ,
            "§7Target Position:",
            "§f  X: " + (int)endX + " Y: " + (int)endY + " Z: " + (int)endZ,
            "",
            "§e> Left-click to teleport to launch position",
            "§e> Right-click to teleport to target position"
        );
        
        // Store rocket key in item meta for click handling (using invisible characters)
        ItemMeta meta = rocketItem.getItemMeta();
        meta.setDisplayName("§c" + displayName + "§0§8" + rocketKey); // Hidden with darker gray
        rocketItem.setItemMeta(meta);
        
        return rocketItem;
    }
    
    public void openAntiAirGUI(Player player) {
        List<AntiAirDefense> defenses = antiAirManager.getDefenses();
        if (defenses.isEmpty()) {
            player.sendMessage("§cNo anti-air defenses configured!");
            return;
        }
        
        Inventory gui = Bukkit.createInventory(null, 54, GUIBuilder.ANTIAIR_GUI_TITLE);
        
        // Organize defenses by country
        int slot = 0;
        
        // USA Defenses
        ItemStack usaHeader = ItemBuilder.createItem(Material.RED_BANNER, "§cUnited States",
            "§7Advanced automated systems",
            "§7protecting the western region"
        );
        gui.setItem(slot++, usaHeader);
        
        for (AntiAirDefense defense : defenses) {
            if (defense.getName().contains("america")) {
                gui.setItem(slot++, createDefenseItem(defense));
            }
        }
        
        // Move to next row
        slot = ((slot + 8) / 9) * 9;
        
        // Asia Defenses
        ItemStack asiaHeader = ItemBuilder.createItem(Material.YELLOW_BANNER, "§6Asian Coalition",
            "§7Sophisticated interception systems", 
            "§7guarding the eastern region"
        );
        gui.setItem(slot++, asiaHeader);
        
        for (AntiAirDefense defense : defenses) {
            if (defense.getName().contains("asia")) {
                gui.setItem(slot++, createDefenseItem(defense));
            }
        }
        
        // Move to next row
        slot = ((slot + 8) / 9) * 9;
        
        // Antarctica Defenses
        ItemStack antarcticaHeader = ItemBuilder.createItem(Material.WHITE_BANNER, "§fAntarctica",
            "§7Experimental defense systems",
            "§7securing the northern region"
        );
        gui.setItem(slot++, antarcticaHeader);
        
        for (AntiAirDefense defense : defenses) {
            if (defense.getName().contains("antarctica")) {
                gui.setItem(slot++, createDefenseItem(defense));
            }
        }
        
        // Move to next row
        slot = ((slot + 8) / 9) * 9;
        
        // Africa Defenses
        ItemStack africaHeader = ItemBuilder.createItem(Material.GREEN_BANNER, "§2African Federation",
            "§7Regional protection systems",
            "§7covering the central region"
        );
        gui.setItem(slot++, africaHeader);
        
        for (AntiAirDefense defense : defenses) {
            if (defense.getName().contains("africa")) {
                gui.setItem(slot++, createDefenseItem(defense));
            }
        }
        
        // Move to next row
        slot = ((slot + 8) / 9) * 9;
        
        // Europe Defenses
        ItemStack europeHeader = ItemBuilder.createItem(Material.BLUE_BANNER, "§9European Union",
            "§7Coordinated defense network",
            "§7protecting the southern region"
        );
        gui.setItem(slot++, europeHeader);
        
        for (AntiAirDefense defense : defenses) {
            if (defense.getName().contains("europe")) {
                gui.setItem(slot++, createDefenseItem(defense));
            }
        }
        
        // Back button
        ItemStack backItem = ItemBuilder.createItem(Material.ARROW, "§f§lBack to Main Menu", "§7Click to return");
        gui.setItem(53, backItem);
        
        player.openInventory(gui);
    }
    
    private ItemStack createDefenseItem(AntiAirDefense defense) {
        Location loc = defense.getLocation();
        String status = defense.isOperational() ? "§aONLINE" : "§cOFFLINE";
        String type = defense.getName().contains("patriot") || defense.getName().contains("aegis") || 
                     defense.getName().contains("core_defense") || defense.getName().contains("hq9") ||
                     defense.getName().contains("s400") ? "AUTO" : "MANUAL";
        
        // Get display name from config if available
        String displayName = defense.getName()
            .replace("_", " ")
            .toUpperCase()
            .replace("AMERICA", "USA")
            .replace("ASIA", "")
            .replace("ANTARCTICA", "")
            .replace("AFRICA", "")
            .replace("EUROPE", "");
        
        ItemStack defenseItem = ItemBuilder.createItem(Material.CROSSBOW, "§e" + displayName,
            "§7Status: " + status,
            "§7Type: §6" + type,
            "§7Range: §f" + (int)defense.getRange() + " blocks",
            "§7Accuracy: §f" + (int)(defense.getAccuracy() * 100) + "%",
            "",
            "§7Position:",
            "§f  X: " + (int)loc.getX() + " Y: " + (int)loc.getY() + " Z: " + (int)loc.getZ(),
            "",
            "§e> Click to teleport to defense position"
        );
        
        // Store defense name in item meta for click handling
        ItemMeta meta = defenseItem.getItemMeta();
        meta.setDisplayName(meta.getDisplayName() + "§0§r" + defense.getName()); // Hidden defense name
        defenseItem.setItemMeta(meta);
        
        return defenseItem;
    }
    
    public void openCustomItemsGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, GUIBuilder.ITEMS_GUI_TITLE);
        
        // Reinforcement Powder
        ItemStack reinforcementPowder = reinforcementHandler.getReinforcementPowder();
        ItemMeta powderMeta = reinforcementPowder.getItemMeta();
        List<String> powderLore = new ArrayList<>(powderMeta.getLore());
        powderLore.add("");
        powderLore.add("§e> Click to get 1x Reinforcement Powder");
        powderLore.add("§e> Shift-click to get 16x Reinforcement Powder");
        powderMeta.setLore(powderLore);
        reinforcementPowder.setItemMeta(powderMeta);
        gui.setItem(20, reinforcementPowder);
        
        // Detector Helmet
        ItemStack detectorHelmet = detectorManager.createDetectorHelmet();
        ItemMeta helmetMeta = detectorHelmet.getItemMeta();
        List<String> helmetLore = new ArrayList<>(helmetMeta.getLore());
        helmetLore.add("");
        helmetLore.add("§e> Click to get 1x Detection Helmet");
        helmetMeta.setLore(helmetLore);
        detectorHelmet.setItemMeta(helmetMeta);
        gui.setItem(24, detectorHelmet);
        
        // Recipe info item
        ItemStack recipeInfo = ItemBuilder.createItem(Material.CRAFTING_TABLE, "§6§lReinforcement Recipe",
            "§7Crafting recipe for reinforcement powder:",
            "",
            "§f1x Iron Ingot + 1x Stone = 1x Reinforcement Powder",
            "§7(Shapeless recipe - any arrangement)",
            "",
            "§7Use reinforcement powder to strengthen blocks",
            "§7against explosions by right-clicking on them."
        );
        gui.setItem(22, recipeInfo);
        
        // Back button
        ItemStack backItem = ItemBuilder.createItem(Material.ARROW, "§f§lBack to Main Menu", "§7Click to return");
        gui.setItem(53, backItem);
        
        player.openInventory(gui);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        String title = event.getView().getTitle();
        plugin.getLogger().info("=== GUI CLICK DEBUG ===");
        plugin.getLogger().info("Player: " + player.getName());
        plugin.getLogger().info("GUI Title: " + title);
        
        // Check if it's one of our GUIs
        if (!title.equals(GUIBuilder.MAIN_GUI_TITLE) && !title.equals(GUIBuilder.ROCKETS_GUI_TITLE) && 
            !title.equals(GUIBuilder.ANTIAIR_GUI_TITLE) && !title.equals(GUIBuilder.ITEMS_GUI_TITLE) &&
            !title.equals(GUIBuilder.NATIONS_GUI_TITLE) &&
            !title.equals(GUIBuilder.MISSILE_INFO_GUI_TITLE) && !title.equals(GUIBuilder.ANTIAIR_INFO_GUI_TITLE)) {
            plugin.getLogger().info("Not our GUI, ignoring");
            return;
        }
        
        plugin.getLogger().info("Confirmed our GUI, cancelling event");
        event.setCancelled(true); // Prevent taking items
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) {
            plugin.getLogger().info("Clicked item is null");
            return;
        }
        
        if (!clickedItem.hasItemMeta()) {
            plugin.getLogger().info("Clicked item has no meta");
            return;
        }
        
        String displayName = clickedItem.getItemMeta().getDisplayName();
        plugin.getLogger().info("Item Display Name: '" + displayName + "'");
        plugin.getLogger().info("Click Type: " + (event.isLeftClick() ? "LEFT" : "RIGHT"));
        
        // Handle main GUI clicks
        if (title.equals(GUIBuilder.MAIN_GUI_TITLE)) {
            if (displayName.contains("Rocket Arsenal")) {
                openRocketsGUI(player);
            } else if (displayName.contains("Anti-Air Defenses")) {
                openAntiAirGUI(player);
            } else if (displayName.contains("Custom Items")) {
                openCustomItemsGUI(player);
            } else if (displayName.contains("Nations & Disasters")) {
                openNationsGUI(player);
            }
        }
        
        // Handle rockets GUI clicks
        else if (title.equals(GUIBuilder.ROCKETS_GUI_TITLE)) {
            plugin.getLogger().info("Processing rockets GUI click");
            if (displayName.contains("Back to Main Menu")) {
                plugin.getLogger().info("Back button clicked");
                openMainGUI(player);
            } else if (displayName.startsWith("§c") && !displayName.contains("Banner")) { // Rocket item (red text, not banner)
                plugin.getLogger().info("Rocket item clicked - calling missileGUI.handleRocketClick");
                missileGUI.handleRocketClick(player, displayName, event.isLeftClick());
            } else {
                plugin.getLogger().info("Item didn't match rocket criteria - DisplayName: '" + displayName + "', startsWith §c: " + displayName.startsWith("§c") + ", contains Banner: " + displayName.contains("Banner"));
            }
        }
        
        // Handle anti-air GUI clicks  
        else if (title.equals(GUIBuilder.ANTIAIR_GUI_TITLE)) {
            if (displayName.contains("Back to Main Menu")) {
                openMainGUI(player);
            } else if (displayName.startsWith("§e") && !displayName.contains("Banner")) { // Defense item (yellow text, not banner)
                handleAntiAirClick(player, displayName);
            }
        }
        
        // Handle custom items GUI clicks
        else if (title.equals(GUIBuilder.ITEMS_GUI_TITLE)) {
            if (displayName.contains("Back to Main Menu")) {
                openMainGUI(player);
            } else if (displayName.contains("Reinforcement Powder")) {
                int amount = event.isShiftClick() ? 16 : 1;
                player.getInventory().addItem(reinforcementHandler.getReinforcementPowder(amount));
                player.sendMessage("§a§lGiven " + amount + "x Reinforcement Powder!");
                player.closeInventory();
            } else if (displayName.contains("Detection Helmet")) {
                player.getInventory().addItem(detectorManager.createDetectorHelmet());
                player.sendMessage("§b§lGiven 1x Reinforcement Detection Helmet!");
                player.closeInventory();
            }
        }
        
        // Handle nations GUI clicks
        else if (title.equals(GUIBuilder.NATIONS_GUI_TITLE)) {
            if (displayName.contains("Back to Main Menu")) {
                openMainGUI(player);
            } else if (displayName.startsWith("§6§l") && !displayName.contains("Your Location")) { // Nation item (gold text, not location)
                handleNationClick(player, clickedItem);
            }
        }
        
        // Handle missile information GUI clicks
        else if (title.equals(GUIBuilder.MISSILE_INFO_GUI_TITLE)) {
            plugin.getLogger().info("Processing missile info GUI click");
            missileGUI.handleMissileInfoClick(player, displayName, event.getInventory());
        }
        
        // Handle anti-air information GUI clicks
        else if (title.equals(GUIBuilder.ANTIAIR_INFO_GUI_TITLE)) {
            plugin.getLogger().info("Processing anti-air info GUI click");
            if (displayName.contains("Take Offline") || displayName.contains("Bring Online")) {
                plugin.getLogger().info("Toggle button clicked");
                handleAntiAirInfoToggle(player, event.getInventory(), displayName);
            } else if (displayName.contains("Teleport to Defense")) {
                plugin.getLogger().info("Teleport to Defense button clicked");
                handleAntiAirInfoTeleport(player, event.getInventory());
            }
        }
    }
    
    
    private void handleNationClick(Player player, ItemStack clickedItem) {
        plugin.getLogger().info("Handling nation click");
        
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            plugin.getLogger().warning("Clicked item has no metadata");
            player.sendMessage("§cError: Could not identify nation");
            return;
        }
        
        // Extract nation ID from lore
        List<String> lore = clickedItem.getItemMeta().getLore();
        String nationId = null;
        
        for (String line : lore) {
            if (line.startsWith("§8§0nation_id:")) {
                nationId = line.substring("§8§0nation_id:".length());
                break;
            }
        }
        
        if (nationId == null) {
            plugin.getLogger().warning("Could not find nation ID in item lore");
            player.sendMessage("§cError: Could not identify nation");
            return;
        }
        
        plugin.getLogger().info("Extracted nation ID: " + nationId);
        
        // Close inventory
        player.closeInventory();
        
        Nation nation = nationManager.getAllNations().get(nationId);
        
        if (nation == null) {
            plugin.getLogger().warning("Nation not found: " + nationId);
            player.sendMessage("§cError: Nation not found");
            return;
        }
        
        // Teleport to nation center using calculated center from borders
        int centerX = nation.getBorders().getCenterX();
        int centerZ = nation.getBorders().getCenterZ();
        Location centerLoc = new Location(
            player.getWorld(),
            centerX,
            player.getWorld().getHighestBlockYAt(centerX, centerZ) + 1, // Safe Y level
            centerZ
        );
        
        player.teleport(centerLoc);
        player.sendMessage("§a§lTeleported to " + nation.getDisplayName() + "!");
        
        // Show current disasters if any
        boolean hasActiveDisasters = false;
        for (Disaster disaster : nation.getDisasters().values()) {
            if (disaster.isActive()) {
                if (!hasActiveDisasters) {
                    player.sendMessage("§c§lActive Disasters in " + nation.getDisplayName() + ":");
                    hasActiveDisasters = true;
                }
                long remainingTime = (disaster.getEndTime() - System.currentTimeMillis()) / 1000;
                player.sendMessage("§c• " + disaster.getId().replace("_", " ").toUpperCase() + 
                    " §7(ends in " + remainingTime + "s)");
            }
        }
        if (!hasActiveDisasters) {
            player.sendMessage("§a§lNo active disasters in " + nation.getDisplayName());
        }
    }

    private void handleAntiAirClick(Player player, String displayName) {
        plugin.getLogger().info("Handling anti-air click: " + displayName);
        
        // Close inventory immediately
        player.closeInventory();
        
        // Remove color codes to get clean display name
        String cleanName = displayName.replaceAll("§[0-9a-fk-or]", "");
        plugin.getLogger().info("Trying to match defense by clean display name: " + cleanName);
        
        // Try to find defense by matching display name patterns
        List<AntiAirDefense> defenses = antiAirManager.getDefenses();
        for (AntiAirDefense defense : defenses) {
            // Create expected display name format
            String expectedName = defense.getName()
                .replace("_", " ")
                .toUpperCase()
                .replace("AMERICA", "USA")
                .replace("ASIA", "")
                .replace("ANTARCTICA", "")
                .replace("AFRICA", "")
                .replace("EUROPE", "");
                
            if (expectedName.equals(cleanName)) {
                plugin.getLogger().info("Found matching defense: " + defense.getName());
                
                Location teleportLoc = defense.getLocation().clone().add(0, 1, 0);
                player.teleport(teleportLoc);
                player.sendMessage("§a§lTeleported to " + defense.getName() + " defense position!");
                return;
            }
        }
        
        plugin.getLogger().warning("Could not find defense with display name: " + cleanName);
    }
    
    private Material getMaterialForExplosionType(String explosionType) {
        return Material.TNT; // Use TNT for all missile types
    }
    
    
    private void handleAntiAirInfoToggle(Player player, Inventory inventory, String displayName) {
        plugin.getLogger().info("=== HANDLE ANTI-AIR INFO TOGGLE DEBUG ===");
        
        // Find the defense info item to extract defense name
        ItemStack infoItem = inventory.getItem(13);
        if (infoItem == null || !infoItem.hasItemMeta()) {
            plugin.getLogger().warning("Could not find defense info item in GUI");
            player.sendMessage("§cError: Could not identify defense system");
            player.closeInventory();
            return;
        }
        
        // Extract defense name from the item display name
        String defenseName = infoItem.getItemMeta().getDisplayName()
            .replaceAll("§[0-9a-fk-or]", "") // Remove color codes
            .replace(" ", "_")
            .toLowerCase();
        
        plugin.getLogger().info("Extracted defense name: " + defenseName);
        
        // Close inventory and toggle defense
        player.closeInventory();
        boolean bringOnline = displayName.contains("Bring Online");
        antiAirManager.setDefenseOperational(defenseName, bringOnline);
        
        String status = bringOnline ? "ONLINE" : "OFFLINE";
        player.sendMessage("§a§lDefense system '" + defenseName + "' set to " + status + "!");
    }
    
    private void handleAntiAirInfoTeleport(Player player, Inventory inventory) {
        plugin.getLogger().info("=== HANDLE ANTI-AIR INFO TELEPORT DEBUG ===");
        
        // Find the defense info item to extract coordinates
        ItemStack infoItem = inventory.getItem(13);
        if (infoItem == null || !infoItem.hasItemMeta()) {
            plugin.getLogger().warning("Could not find defense info item in GUI");
            player.sendMessage("§cError: Could not identify defense location");
            player.closeInventory();
            return;
        }
        
        List<String> lore = infoItem.getItemMeta().getLore();
        double defenseX = 0, defenseY = 0, defenseZ = 0;
        boolean foundPosition = false;
        
        for (String line : lore) {
            String cleanLine = line.replaceAll("§[0-9a-fk-or]", ""); // Remove color codes
            if (cleanLine.contains("Position:")) {
                // Next line should contain coordinates
                int index = lore.indexOf(line);
                if (index + 1 < lore.size()) {
                    String coordLine = lore.get(index + 1).replaceAll("§[0-9a-fk-or]", "");
                    // Parse "  X: 123 Y: 45 Z: 678"
                    try {
                        String[] parts = coordLine.trim().split(" ");
                        for (int i = 0; i < parts.length - 1; i++) {
                            if (parts[i].equals("X:")) {
                                defenseX = Double.parseDouble(parts[i + 1]);
                            } else if (parts[i].equals("Y:")) {
                                defenseY = Double.parseDouble(parts[i + 1]);
                            } else if (parts[i].equals("Z:")) {
                                defenseZ = Double.parseDouble(parts[i + 1]);
                            }
                        }
                        foundPosition = true;
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error parsing defense coordinates: " + e.getMessage());
                    }
                }
                break;
            }
        }
        
        if (!foundPosition) {
            plugin.getLogger().warning("Could not extract defense coordinates from anti-air info GUI");
            player.sendMessage("§cError: Could not identify defense location");
            player.closeInventory();
            return;
        }
        
        plugin.getLogger().info("Extracted defense coordinates: " + defenseX + ", " + defenseY + ", " + defenseZ);
        
        // Close inventory and teleport
        player.closeInventory();
        Location teleportLoc = new Location(player.getWorld(), defenseX, defenseY + 1, defenseZ);
        player.teleport(teleportLoc);
        player.sendMessage("§a§lTeleported to defense system location!");
    }
    
}