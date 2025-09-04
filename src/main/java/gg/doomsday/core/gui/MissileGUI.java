package gg.doomsday.core.gui;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import gg.doomsday.core.gui.utils.GUIBuilder;
import gg.doomsday.core.gui.utils.ItemBuilder;
import gg.doomsday.core.services.MissileService;

import java.util.List;

/**
 * Handles all missile-related GUI operations
 */
public class MissileGUI {
    
    private final JavaPlugin plugin;
    private final MissileService missileService;
    
    public MissileGUI(JavaPlugin plugin, MissileService missileService) {
        this.plugin = plugin;
        this.missileService = missileService;
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
                    "§e> Use /r " + info.getKey() + " to launch this missile"
                )
                .build();
        gui.setItem(13, infoItem);
        
        // Launch button
        ItemStack launchItem = new ItemBuilder(Material.FIRE_CHARGE)
                .setDisplayName("§a§lLaunch Missile")
                .setLore(
                    "§7Click to launch " + info.getDisplayName(),
                    "",
                    "§e> Click to execute launch command"
                )
                .build();
        gui.setItem(11, launchItem);
        
        // Teleport to target button
        ItemStack targetItem = new ItemBuilder(Material.ENDER_PEARL)
                .setDisplayName("§b§lTeleport to Target")
                .setLore(
                    "§7Teleport to the target location",
                    "",
                    "§e> Click to teleport to target coordinates"
                )
                .build();
        gui.setItem(15, targetItem);
        
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
        } else if (displayName.contains("Teleport to Target")) {
            return handleMissileInfoTeleport(player, inventory);
        }
        return false;
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
        for (String line : lore) {
            if (line.contains("Use /r ") && line.contains(" to launch")) {
                String rocketKey = extractRocketKeyFromLore(line);
                if (rocketKey != null) {
                    player.closeInventory();
                    return missileService.launchMissileViaGUI(player, rocketKey);
                }
            }
        }
        
        player.sendMessage("§cError: Could not identify missile to launch");
        player.closeInventory();
        return false;
    }
    
    private boolean handleMissileInfoTeleport(Player player, Inventory inventory) {
        // Extract target coordinates from info item lore
        ItemStack infoItem = inventory.getItem(13);
        if (infoItem == null || !infoItem.hasItemMeta()) {
            player.sendMessage("§cError: Could not identify target location");
            player.closeInventory();
            return false;
        }
        
        List<String> lore = infoItem.getItemMeta().getLore();
        Location targetLocation = extractTargetLocationFromLore(lore);
        
        if (targetLocation == null) {
            player.sendMessage("§cError: Could not identify target location");
            player.closeInventory();
            return false;
        }
        
        player.closeInventory();
        Location teleportLoc = new Location(player.getWorld(), targetLocation.getX(), targetLocation.getY() + 1, targetLocation.getZ());
        player.teleport(teleportLoc);
        player.sendMessage("§a§lTeleported to missile target location!");
        return true;
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
    
    private Location extractTargetLocationFromLore(List<String> lore) {
        for (int i = 0; i < lore.size() - 1; i++) {
            String line = lore.get(i);
            if (line.contains("Target Position:")) {
                String coordLine = lore.get(i + 1).replaceAll("§[0-9a-fk-or]", "");
                try {
                    String[] parts = coordLine.trim().split(" ");
                    double x = 0, y = 0, z = 0;
                    for (int j = 0; j < parts.length - 1; j++) {
                        if (parts[j].equals("X:")) x = Double.parseDouble(parts[j + 1]);
                        else if (parts[j].equals("Y:")) y = Double.parseDouble(parts[j + 1]);
                        else if (parts[j].equals("Z:")) z = Double.parseDouble(parts[j + 1]);
                    }
                    return new Location(null, x, y, z);
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }
}