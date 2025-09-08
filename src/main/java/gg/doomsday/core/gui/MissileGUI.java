package gg.doomsday.core.gui;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import gg.doomsday.core.gui.utils.GUIBuilder;
import gg.doomsday.core.gui.utils.ItemBuilder;
import gg.doomsday.core.services.MissileService;
import gg.doomsday.core.fuel.MissileFuelManager;
import gg.doomsday.core.DoomsdayCore;

import java.util.List;

/**
 * Handles all missile-related GUI operations
 */
public class MissileGUI implements Listener {
    
    private final JavaPlugin plugin;
    private final MissileService missileService;
    
    public MissileGUI(JavaPlugin plugin, MissileService missileService) {
        this.plugin = plugin;
        this.missileService = missileService;
        // Register event listener to prevent item extraction
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Create the main rocket arsenal GUI
     */
    public Inventory createRocketsGUI() {
        ConfigurationSection rocketsSection = plugin.getConfig().getConfigurationSection("rockets");
        if (rocketsSection == null) {
            return null;
        }
        
        Inventory gui = GUIBuilder.createRocketsGUI();
        
        // Organize rockets by country
        int slot = 0;
        
        // USA Rockets
        gui.setItem(slot++, createCountryHeader("§cUnited States", "Strategic missile systems", "positioned in the western region", Material.RED_BANNER));
        for (String rocketKey : rocketsSection.getKeys(false)) {
            if (rocketKey.contains("america")) {
                gui.setItem(slot++, createRocketItem(rocketKey, rocketsSection));
            }
        }
        
        // Move to next row for next country
        slot = ((slot + 8) / 9) * 9;
        
        // Asia Rockets
        gui.setItem(slot++, createCountryHeader("§6Asian Coalition", "Advanced missile systems", "positioned in the eastern region", Material.YELLOW_BANNER));
        for (String rocketKey : rocketsSection.getKeys(false)) {
            if (rocketKey.contains("asia")) {
                gui.setItem(slot++, createRocketItem(rocketKey, rocketsSection));
            }
        }
        
        // Move to next row
        slot = ((slot + 8) / 9) * 9;
        
        // Antarctica Rockets
        gui.setItem(slot++, createCountryHeader("§fAntarctica", "Experimental weapons testing", "positioned in the northern region", Material.WHITE_BANNER));
        for (String rocketKey : rocketsSection.getKeys(false)) {
            if (rocketKey.contains("antarctica")) {
                gui.setItem(slot++, createRocketItem(rocketKey, rocketsSection));
            }
        }
        
        // Move to next row
        slot = ((slot + 8) / 9) * 9;
        
        // Africa Rockets
        gui.setItem(slot++, createCountryHeader("§2African Federation", "Regional defense systems", "positioned in the central region", Material.GREEN_BANNER));
        for (String rocketKey : rocketsSection.getKeys(false)) {
            if (rocketKey.contains("africa")) {
                gui.setItem(slot++, createRocketItem(rocketKey, rocketsSection));
            }
        }
        
        // Move to next row
        slot = ((slot + 8) / 9) * 9;
        
        // Europe Rockets
        gui.setItem(slot++, createCountryHeader("§9European Union", "Precision strike capabilities", "positioned in the southern region", Material.BLUE_BANNER));
        for (String rocketKey : rocketsSection.getKeys(false)) {
            if (rocketKey.contains("europe")) {
                gui.setItem(slot++, createRocketItem(rocketKey, rocketsSection));
            }
        }
        
        return gui;
    }
    
    /**
     * Create the missile information GUI for a specific missile
     */
    public Inventory createMissileInfoGUI(String rocketKey) {
        MissileService.MissileInfo info = missileService.getMissileInfo(rocketKey);
        if (info == null) {
            return null;
        }
        
        DoomsdayCore doomsdayCore = (DoomsdayCore) plugin;
        MissileFuelManager fuelManager = doomsdayCore.getFuelManager();
        
        // Get fuel information
        ConfigurationSection rocket = plugin.getConfig().getConfigurationSection("rockets." + rocketKey);
        int fuelRequired = rocket != null ? rocket.getInt("fuelRequired", 0) : 0;
        int fuelAvailable = fuelManager.getFuel(rocketKey);
        
        Inventory gui = GUIBuilder.createMissileInfoGUI();
        
        // Main info item
        ItemStack infoItem = new ItemBuilder(Material.TNT)
                .setDisplayName("§c" + info.getDisplayName())
                .setLore(
                    "§7Missile System Information",
                    "",
                    "§7Explosion Type: §6" + info.getExplosionType(),
                    "§7Speed: §f" + info.getSpeed(),
                    "§7Arc Scale: §f" + info.getArcScale(),
                    "",
                    "§7Launch Position:",
                    "§f  " + formatLocation(info.getStartLocation()),
                    "§7Target Position:",
                    "§f  " + formatLocation(info.getEndLocation()),
                    "",
                    "§7Fuel Required: §6" + fuelRequired,
                    "§7Fuel Available: " + (fuelAvailable >= fuelRequired ? "§a" : "§c") + fuelAvailable,
                    "",
                    "§8Rocket Key: " + rocketKey
                )
                .build();
        gui.setItem(13, infoItem);
        
        // Launch button - only enabled if enough fuel
        boolean canLaunch = fuelAvailable >= fuelRequired;
        ItemStack launchItem = new ItemBuilder(canLaunch ? Material.FIRE_CHARGE : Material.BARRIER)
                .setDisplayName(canLaunch ? "§a§lLaunch Missile" : "§c§lInsufficient Fuel")
                .setLore(
                    canLaunch ? "§7Click to launch " + info.getDisplayName() : "§7Not enough fuel to launch",
                    "",
                    canLaunch ? "§e> Click to execute launch" : "§c> Need " + (fuelRequired - fuelAvailable) + " more fuel"
                )
                .build();
        gui.setItem(11, launchItem);
        
        // Fuel depot button
        ItemStack fuelItem = new ItemBuilder(Material.BLAZE_POWDER)
                .setDisplayName("§6§lFuel Depot")
                .setLore(
                    "§7Manage missile fuel",
                    "",
                    "§7Current Fuel: §6" + fuelAvailable,
                    "§7Required: §e" + fuelRequired,
                    "",
                    "§e> Click to deposit rocket fuel"
                )
                .build();
        gui.setItem(12, fuelItem);
        
        
        return gui;
    }
    
    /**
     * Handle clicking on a rocket item in the main rockets GUI
     */
    public boolean handleRocketClick(Player player, String displayName, boolean isLeftClick) {
        plugin.getLogger().info("=== HANDLE ROCKET CLICK DEBUG ===");
        plugin.getLogger().info("Player: " + player.getName());
        plugin.getLogger().info("Display Name: '" + displayName + "'");
        plugin.getLogger().info("Is Left Click: " + isLeftClick);
        
        // Close inventory immediately
        player.closeInventory();
        
        // Extract rocket key from hidden text in display name
        String[] parts = displayName.split("§8");
        if (parts.length < 2) {
            plugin.getLogger().warning("Could not extract rocket key from display name using §8 split");
            return tryRocketClickByDisplayName(player, displayName, isLeftClick);
        }
        
        String rocketKey = parts[1];
        plugin.getLogger().info("Extracted rocket key: '" + rocketKey + "'");
        
        MissileService.MissileInfo info = missileService.getMissileInfo(rocketKey);
        if (info == null) {
            plugin.getLogger().warning("Could not find rocket info for key: " + rocketKey);
            return false;
        }
        
        if (isLeftClick) {
            plugin.getLogger().info("LEFT CLICK - Launching missile");
            return missileService.launchMissileViaGUI(player, rocketKey);
        } else {
            plugin.getLogger().info("RIGHT CLICK - Teleporting to target");
            Location teleportLoc = new Location(player.getWorld(),
                info.getEndLocation().getX(),
                info.getEndLocation().getY() + 1,
                info.getEndLocation().getZ()
            );
            player.teleport(teleportLoc);
            player.sendMessage("§a§lTeleported to " + info.getDisplayName() + " target position!");
            return true;
        }
    }
    
    /**
     * Handle missile info GUI clicks
     */
    public boolean handleMissileInfoClick(Player player, String displayName, Inventory inventory) {
        if (displayName.contains("Launch Missile")) {
            return handleMissileInfoLaunch(player, inventory);
        } else if (displayName.contains("Fuel Depot")) {
            return handleFuelDepotClick(player, inventory);
        }
        return false;
    }
    
    /**
     * Create fuel depot GUI for a specific missile
     */
    public Inventory createFuelDepotGUI(String rocketKey) {
        MissileService.MissileInfo info = missileService.getMissileInfo(rocketKey);
        if (info == null) {
            return null;
        }
        
        DoomsdayCore doomsdayCore = (DoomsdayCore) plugin;
        MissileFuelManager fuelManager = doomsdayCore.getFuelManager();
        
        // Get fuel information
        ConfigurationSection rocket = plugin.getConfig().getConfigurationSection("rockets." + rocketKey);
        int fuelRequired = rocket != null ? rocket.getInt("fuelRequired", 0) : 0;
        int fuelAvailable = fuelManager.getFuel(rocketKey);
        
        // Use the proper foundation classes
        Inventory gui = gg.doomsday.core.gui.framework.GUIBuilder.createInventory("Missile Fuel Depot", 27);
        
        // Add glass pane border
        ItemStack glassBorder = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .setDisplayName(" ")
                .build();
        
        // Fill border slots for 27-slot GUI
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, glassBorder); // Top row
            gui.setItem(18 + i, glassBorder); // Bottom row
        }
        gui.setItem(9, glassBorder); // Left middle
        gui.setItem(17, glassBorder); // Right middle
        
        // Missile info item (center top)
        ItemStack missileItem = new ItemBuilder(Material.TNT)
                .setDisplayName("§c" + info.getDisplayName())
                .setLore(
                    "§7Fuel Management System",
                    "",
                    "§7Required: §6" + fuelRequired,
                    "§7Available: " + (fuelAvailable >= fuelRequired ? "§a" : "§c") + fuelAvailable,
                    "",
                    "§8" + rocketKey
                )
                .build();
        gui.setItem(4, missileItem);
        
        // Deposit 1 fuel button
        ItemStack deposit1Item = new ItemBuilder(Material.BLAZE_POWDER)
                .setDisplayName("§6§lDeposit 1 Fuel")
                .setLore(
                    "§7Deposits 1 rocket fuel from inventory",
                    "",
                    "§e> Click to deposit"
                )
                .build();
        gui.setItem(11, deposit1Item);
        
        // Deposit 10 fuel button
        ItemStack deposit10Item = new ItemBuilder(Material.BLAZE_POWDER)
                .setDisplayName("§6§lDeposit 10 Fuel")
                .setLore(
                    "§7Deposits 10 rocket fuel from inventory", 
                    "",
                    "§e> Click to deposit"
                )
                .build();
        gui.setItem(13, deposit10Item);
        
        // Deposit 64 fuel button
        ItemStack deposit64Item = new ItemBuilder(Material.BLAZE_POWDER)
                .setDisplayName("§6§lDeposit 64 Fuel")
                .setLore(
                    "§7Deposits 64 rocket fuel from inventory",
                    "",
                    "§e> Click to deposit"
                )
                .build();
        gui.setItem(15, deposit64Item);
        
        // Back button
        ItemStack backItem = new ItemBuilder(Material.ARROW)
                .setDisplayName("§7§lBack")
                .setLore(
                    "§7Return to missile information"
                )
                .build();
        gui.setItem(22, backItem);
        
        return gui;
    }
    
    /**
     * Handle fuel depot GUI clicks  
     */
    public boolean handleFuelDepotClick(Player player, String displayName, Inventory inventory) {
        // Extract rocket key from missile info item in slot 4
        ItemStack infoItem = inventory.getItem(4);
        if (infoItem == null || !infoItem.hasItemMeta()) {
            player.sendMessage("§cError: Could not identify missile for fuel depot");
            player.closeInventory();
            return false;
        }
        
        List<String> lore = infoItem.getItemMeta().getLore();
        String rocketKey = extractRocketKeyFromInfoLore(lore);
        
        if (rocketKey == null) {
            player.sendMessage("§cError: Could not identify missile for fuel depot");
            player.closeInventory();
            return false;
        }
        
        if (displayName.contains("Deposit") && displayName.contains("Fuel")) {
            return handleFuelDeposit(player, displayName, rocketKey);
        } else if (displayName.contains("Back")) {
            // Open missile info GUI
            Inventory missileInfoGUI = createMissileInfoGUI(rocketKey);
            if (missileInfoGUI != null) {
                player.openInventory(missileInfoGUI);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Handle fuel depot button click from missile info GUI
     */
    private boolean handleFuelDepotClick(Player player, Inventory inventory) {
        // Extract rocket key from info item
        ItemStack infoItem = inventory.getItem(13);
        if (infoItem == null || !infoItem.hasItemMeta()) {
            player.sendMessage("§cError: Could not identify missile for fuel depot");
            player.closeInventory();
            return false;
        }
        
        List<String> lore = infoItem.getItemMeta().getLore();
        String rocketKey = extractRocketKeyFromInfoLore(lore);
        
        if (rocketKey == null) {
            player.sendMessage("§cError: Could not identify missile for fuel depot");
            player.closeInventory();
            return false;
        }
        
        // Open fuel depot GUI
        Inventory fuelDepotGUI = createFuelDepotGUI(rocketKey);
        if (fuelDepotGUI != null) {
            player.closeInventory();
            player.openInventory(fuelDepotGUI);
            return true;
        }
        
        return false;
    }
    
    /**
     * Handle fuel deposit action
     */
    private boolean handleFuelDeposit(Player player, String displayName, String rocketKey) {
        int depositAmount = 1;
        
        // Determine deposit amount from display name
        if (displayName.contains("64")) {
            depositAmount = 64;
        } else if (displayName.contains("10")) {
            depositAmount = 10;
        } // else defaults to 1
        
        // Check if player has enough rocket fuel in inventory
        int availableFuel = countRocketFuelInInventory(player);
        if (availableFuel < depositAmount) {
            player.sendMessage("§cYou need " + depositAmount + " rocket fuel but only have " + availableFuel + "!");
            return false;
        }
        
        // Remove fuel from inventory first
        if (!removeRocketFuelFromInventory(player, depositAmount)) {
            player.sendMessage("§cFailed to remove rocket fuel from inventory!");
            return false;
        }
        
        // Add fuel to missile
        DoomsdayCore doomsdayCore = (DoomsdayCore) plugin;
        MissileFuelManager fuelManager = doomsdayCore.getFuelManager();
        
        if (fuelManager.addFuel(rocketKey, depositAmount)) {
            player.sendMessage("§a§l✅ DEPOSITED " + depositAmount + " FUEL!");
            
            // Refresh fuel depot GUI immediately 
            Inventory newFuelDepotGUI = createFuelDepotGUI(rocketKey);
            if (newFuelDepotGUI != null) {
                player.openInventory(newFuelDepotGUI);
            }
            return true;
        } else {
            // If fuel manager fails, give fuel back to player
            DoomsdayCore core = (DoomsdayCore) plugin;
            var customItemManager = core.getReinforcementHandler().getCustomItemManager();
            ItemStack refund = customItemManager.createRocketFuel(depositAmount);
            player.getInventory().addItem(refund);
            player.sendMessage("§cFailed to deposit fuel! Items returned to inventory.");
            return false;
        }
    }
    
    /**
     * Count rocket fuel items in player inventory
     */
    private int countRocketFuelInInventory(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isRocketFuel(item)) {
                count += item.getAmount();
            }
        }
        return count;
    }
    
    /**
     * Remove rocket fuel from player inventory
     */
    private boolean removeRocketFuelFromInventory(Player player, int amount) {
        int remaining = amount;
        
        for (ItemStack item : player.getInventory().getContents()) {
            if (remaining <= 0) break;
            
            if (item != null && isRocketFuel(item)) {
                int itemAmount = item.getAmount();
                if (itemAmount <= remaining) {
                    remaining -= itemAmount;
                    item.setAmount(0);
                } else {
                    item.setAmount(itemAmount - remaining);
                    remaining = 0;
                }
            }
        }
        
        return remaining == 0;
    }
    
    /**
     * Check if an item is rocket fuel
     */
    private boolean isRocketFuel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        String displayName = item.getItemMeta().getDisplayName();
        return displayName != null && displayName.contains("Rocket Fuel");
    }
    
    /**
     * Extract rocket key from missile info lore
     */
    private String extractRocketKeyFromInfoLore(List<String> lore) {
        // Look for a line starting with "§8" (hidden text) which contains the rocket key
        for (String line : lore) {
            if (line.startsWith("§8") && !line.contains("Rocket Key:")) {
                // Remove color codes and return the rocket key
                return line.replaceAll("§[0-9a-fk-or]", "").trim();
            }
        }
        
        // Fallback: look for "Rocket Key:" format  
        for (String line : lore) {
            if (line.contains("Rocket Key:")) {
                return line.replaceAll("§[0-9a-fk-or]", "").replace("Rocket Key:", "").trim();
            }
        }
        
        return null;
    }
    
    private ItemStack createCountryHeader(String countryName, String description1, String description2, Material bannerMaterial) {
        return new ItemBuilder(bannerMaterial)
                .setDisplayName(countryName)
                .setLore(
                    "§7" + description1,
                    "§7" + description2
                )
                .build();
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
        
        ItemStack rocketItem = new ItemBuilder(Material.TNT)
                .setDisplayName("§c" + displayName + "§0§8" + rocketKey) // Hidden rocket key
                .setLore(
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
                )
                .build();
        
        return rocketItem;
    }
    
    private String formatLocation(Location location) {
        return "X: " + (int)location.getX() + " Y: " + (int)location.getY() + " Z: " + (int)location.getZ();
    }
    
    private boolean tryRocketClickByDisplayName(Player player, String displayName, boolean isLeftClick) {
        // Extract clean display name
        String cleanName = displayName
                .replaceAll("§[0-9a-fk-or]", "")
                .replaceAll("(trident_america|tomahawk_america_north|tomahawk_america_south|df21_asia|iskander_asia|v2_antarctica|tsar_antarctica|scud_africa|fadjar_africa|stormshadow_europe|scalp_europe).*", "")
                .trim();
        
        ConfigurationSection rocketsSection = plugin.getConfig().getConfigurationSection("rockets");
        if (rocketsSection == null) return false;
        
        for (String rocketKey : rocketsSection.getKeys(false)) {
            ConfigurationSection rocket = rocketsSection.getConfigurationSection(rocketKey);
            if (rocket != null) {
                String rocketDisplayName = rocket.getString("displayName", "");
                if (rocketDisplayName.equals(cleanName)) {
                    if (isLeftClick) {
                        return missileService.launchMissileViaGUI(player, rocketKey);
                    } else {
                        Location teleportLoc = new Location(player.getWorld(),
                            rocket.getDouble("end.x"),
                            rocket.getDouble("end.y") + 1,
                            rocket.getDouble("end.z")
                        );
                        player.teleport(teleportLoc);
                        player.sendMessage("§a§lTeleported to " + rocketDisplayName + " target position!");
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    private boolean handleMissileInfoLaunch(Player player, Inventory inventory) {
        // Extract rocket key from info item lore
        ItemStack infoItem = inventory.getItem(13);
        if (infoItem == null || !infoItem.hasItemMeta()) {
            player.sendMessage("§cError: Could not identify missile to launch");
            player.closeInventory();
            return false;
        }
        
        List<String> lore = infoItem.getItemMeta().getLore();
        String rocketKey = extractRocketKeyFromInfoLore(lore);
        
        if (rocketKey != null) {
            player.closeInventory();
            return missileService.launchMissileViaGUI(player, rocketKey);
        }
        
        player.sendMessage("§cError: Could not identify missile to launch");
        player.closeInventory();
        return false;
    }
    
    
    private String extractRocketKeyFromLore(String loreLine) {
        String cleanLine = loreLine.replaceAll("§[0-9a-fk-or]", "");
        String[] parts = cleanLine.split(" ");
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].equals("/r")) {
                return parts[i + 1];
            }
        }
        return null;
    }
    
    /**
     * Event handler to prevent item extraction from GUIs and handle clicks
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        ItemStack clickedItem = event.getCurrentItem();
        
        // Handle missile info GUI clicks
        if (title.equals("Missile Information")) {
            event.setCancelled(true); // CRITICAL: Prevent item extraction
            
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
            
            String displayName = clickedItem.getItemMeta() != null ? 
                clickedItem.getItemMeta().getDisplayName() : "";
            
            // Handle button clicks
            handleMissileInfoClick(player, displayName, event.getInventory());
            return;
        }
        
        // Handle fuel depot GUI clicks  
        if (title.equals("Missile Fuel Depot")) {
            event.setCancelled(true); // CRITICAL: Prevent item extraction
            
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
            
            String displayName = clickedItem.getItemMeta() != null ? 
                clickedItem.getItemMeta().getDisplayName() : "";
            
            // Handle fuel depot button clicks
            handleFuelDepotClick(player, displayName, event.getInventory());
            return;
        }
        
        // Handle main rockets GUI clicks
        if (title.equals("Rocket Arsenal")) {
            event.setCancelled(true); // CRITICAL: Prevent item extraction
            
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
            
            String displayName = clickedItem.getItemMeta() != null ? 
                clickedItem.getItemMeta().getDisplayName() : "";
            
            // Handle rocket selection clicks
            boolean isLeftClick = event.getClick().isLeftClick();
            handleRocketClick(player, displayName, isLeftClick);
            return;
        }
    }
    
}