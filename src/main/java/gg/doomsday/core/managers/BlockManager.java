package gg.doomsday.core.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import gg.doomsday.core.GUIManager;
import gg.doomsday.core.defense.AntiAirDefense;
import gg.doomsday.core.defense.AntiAirDefenseManager;
import gg.doomsday.core.gui.utils.ItemBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages physical blocks placed in the world for missiles and anti-air defenses
 */
public class BlockManager implements Listener {
    
    private final JavaPlugin plugin;
    private final AntiAirDefenseManager antiAirManager;
    private final GUIManager guiManager;
    private final Map<Location, String> missileBlocks = new HashMap<>();
    private final Map<Location, String> antiAirBlocks = new HashMap<>();
    
    public BlockManager(JavaPlugin plugin, AntiAirDefenseManager antiAirManager, GUIManager guiManager) {
        this.plugin = plugin;
        this.antiAirManager = antiAirManager;
        this.guiManager = guiManager;
        
        // Register event listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    public void placeAllBlocks(World world) {
        placeMissileBlocks(world);
        placeAntiAirBlocks(world);
        plugin.getLogger().info("Placed all missile and anti-air blocks");
    }
    
    private void placeMissileBlocks(World world) {
        missileBlocks.clear();
        
        ConfigurationSection rocketsSection = plugin.getConfig().getConfigurationSection("rockets");
        if (rocketsSection == null) return;
        
        for (String rocketKey : rocketsSection.getKeys(false)) {
            ConfigurationSection rocket = rocketsSection.getConfigurationSection(rocketKey);
            if (rocket != null) {
                double x = rocket.getDouble("start.x");
                double y = rocket.getDouble("start.y");
                double z = rocket.getDouble("start.z");
                
                Location blockLoc = new Location(world, x, y, z);
                Block block = blockLoc.getBlock();
                block.setType(Material.TNT);
                
                missileBlocks.put(blockLoc, rocketKey);
                
                plugin.getLogger().info("Placed TNT block for " + rocketKey + " at " + 
                    (int)x + "," + (int)y + "," + (int)z);
            }
        }
    }
    
    private void placeAntiAirBlocks(World world) {
        antiAirBlocks.clear();
        
        List<AntiAirDefense> defenses = antiAirManager.getDefenses();
        for (AntiAirDefense defense : defenses) {
            Location defenseLocation = defense.getLocation();
            Location blockLoc = new Location(world, 
                defenseLocation.getX(), 
                defenseLocation.getY(), 
                defenseLocation.getZ()
            );
            
            Block block = blockLoc.getBlock();
            block.setType(Material.BEACON);
            
            antiAirBlocks.put(blockLoc, defense.getName());
            
            plugin.getLogger().info("Placed beacon for " + defense.getName() + " at " + 
                (int)blockLoc.getX() + "," + (int)blockLoc.getY() + "," + (int)blockLoc.getZ());
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        // Handle GUI item interactions (right-click with item in hand)
        if ((event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR || 
             event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) && item != null) {
            
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                String displayName = item.getItemMeta().getDisplayName();
                
                // Handle Nation Information item
                if (displayName.equals("§6Nation Information")) {
                    event.setCancelled(true);
                    player.performCommand("nation");
                    return;
                }
                
                // Handle Rocket Control Panel item
                if (displayName.equals("§cRocket Control Panel")) {
                    event.setCancelled(true);
                    player.performCommand("rocket show");
                    return;
                }
            }
        }
        
        // Handle block interactions (existing logic)
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;
        
        Location blockLoc = clickedBlock.getLocation();
        
        // Check if it's a missile block (TNT)
        if (missileBlocks.containsKey(blockLoc) && clickedBlock.getType() == Material.TNT) {
            event.setCancelled(true);
            String rocketKey = missileBlocks.get(blockLoc);
            guiManager.openMissileInfoGUI(player, rocketKey);
            return;
        }
        
        // Check if it's an anti-air block (Beacon)
        if (antiAirBlocks.containsKey(blockLoc) && clickedBlock.getType() == Material.BEACON) {
            event.setCancelled(true);
            String defenseName = antiAirBlocks.get(blockLoc);
            openAntiAirInfoGUI(player, defenseName);
            return;
        }
    }
    
    private void openAntiAirInfoGUI(Player player, String defenseName) {
        AntiAirDefense defense = antiAirManager.getDefenseByName(defenseName);
        if (defense == null) {
            player.sendMessage("§cAnti-air defense not found!");
            return;
        }
        
        Inventory gui = Bukkit.createInventory(null, 27, "Anti-Air Information");
        
        Location loc = defense.getLocation();
        String status = defense.isOperational() ? "§aONLINE" : "§cOFFLINE";
        String type = defenseName.contains("patriot") || defenseName.contains("aegis") || 
                     defenseName.contains("core_defense") || defenseName.contains("hq9") ||
                     defenseName.contains("s400") ? "AUTOMATIC" : "MANUAL";
        
        // Main info item
        ItemStack infoItem = ItemBuilder.createItem(Material.BEACON, "§e" + defenseName.replace("_", " ").toUpperCase(),
            "§7Anti-Air Defense System",
            "",
            "§7Status: " + status,
            "§7Type: §6" + type,
            "§7Range: §f" + (int)defense.getRange() + " blocks",
            "§7Accuracy: §f" + (int)(defense.getAccuracy() * 100) + "%",
            "§7Interceptor Speed: §f" + defense.getInterceptorSpeed(),
            "§7Reload Time: §f" + defense.getReloadTime() + "s",
            "§7Startup Time: §f" + defense.getStartupTime() + "s",
            "",
            "§7Position:",
            "§f  X: " + (int)loc.getX() + " Y: " + (int)loc.getY() + " Z: " + (int)loc.getZ()
        );
        gui.setItem(13, infoItem);
        
        // Toggle online/offline button
        ItemStack toggleItem;
        if (defense.isOperational()) {
            toggleItem = ItemBuilder.createItem(Material.RED_DYE, "§c§lTake Offline",
                "§7Click to disable this defense system",
                "",
                "§e> Click to set defense to OFFLINE"
            );
        } else {
            toggleItem = ItemBuilder.createItem(Material.GREEN_DYE, "§a§lBring Online",
                "§7Click to enable this defense system", 
                "",
                "§e> Click to set defense to ONLINE"
            );
        }
        gui.setItem(11, toggleItem);
        
        // Teleport to defense button
        ItemStack teleportItem = ItemBuilder.createItem(Material.ENDER_PEARL, "§b§lTeleport to Defense",
            "§7Teleport to this defense position",
            "",
            "§e> Click to teleport to defense location"
        );
        gui.setItem(15, teleportItem);
        
        player.openInventory(gui);
    }
    
    public void removeAllBlocks(World world) {
        // Remove missile blocks
        for (Location loc : missileBlocks.keySet()) {
            Block block = loc.getBlock();
            if (block.getType() == Material.TNT) {
                block.setType(Material.AIR);
            }
        }
        
        // Remove anti-air blocks
        for (Location loc : antiAirBlocks.keySet()) {
            Block block = loc.getBlock();
            if (block.getType() == Material.BEACON) {
                block.setType(Material.AIR);
            }
        }
        
        missileBlocks.clear();
        antiAirBlocks.clear();
        plugin.getLogger().info("Removed all missile and anti-air blocks");
    }
    
    public Map<Location, String> getMissileBlocks() {
        return new HashMap<>(missileBlocks);
    }
    
    public Map<Location, String> getAntiAirBlocks() {
        return new HashMap<>(antiAirBlocks);
    }
}