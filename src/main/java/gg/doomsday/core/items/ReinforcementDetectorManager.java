package gg.doomsday.core.items;

import gg.doomsday.core.defense.ReinforcedBlockManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class ReinforcementDetectorManager {
    
    private final JavaPlugin plugin;
    private final ReinforcedBlockManager reinforcedBlockManager;
    private final CustomItemManager customItemManager;
    private final Set<Player> activeDetectors = new HashSet<>();
    private BukkitRunnable detectorTask;
    
    public ReinforcementDetectorManager(JavaPlugin plugin, ReinforcedBlockManager reinforcedBlockManager, CustomItemManager customItemManager) {
        this.plugin = plugin;
        this.reinforcedBlockManager = reinforcedBlockManager;
        this.customItemManager = customItemManager;
        startDetectorTask();
    }
    
    private void startDetectorTask() {
        detectorTask = new BukkitRunnable() {
            int pulseCounter = 0;
            
            @Override
            public void run() {
                updateDetectorEffects(pulseCounter % 4); // Pulse every 4 ticks for fast pulsing
                pulseCounter++;
            }
        };
        detectorTask.runTaskTimer(plugin, 0L, 5L); // Run every 5 ticks (4 times per second) for smooth pulsing
    }
    
    private void updateDetectorEffects(int pulsePhase) {
        activeDetectors.clear();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isWearingDetectorHelmet(player)) {
                activeDetectors.add(player);
                showReinforcedBlocks(player, pulsePhase);
            }
        }
    }
    
    private boolean isWearingDetectorHelmet(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        return customItemManager.isReinforcementDetectorHelmet(helmet);
    }
    
    private void showReinforcedBlocks(Player player, int pulsePhase) {
        Location playerLocation = player.getLocation();
        int range = 16;
        
        for (String blockKey : reinforcedBlockManager.getAllReinforcedBlocks()) {
            Location blockLocation = reinforcedBlockManager.getLocationFromKey(blockKey);
            
            if (blockLocation == null || !blockLocation.getWorld().equals(playerLocation.getWorld())) {
                continue;
            }
            
            // Check if block is within range
            if (blockLocation.distance(playerLocation) <= range) {
                // Check if the block still exists and is still reinforced
                if (reinforcedBlockManager.isReinforced(blockLocation)) {
                    createPulsingCornerEffect(player, blockLocation, pulsePhase);
                }
            }
        }
    }
    
    private void createPulsingCornerEffect(Player player, Location blockLocation, int pulsePhase) {
        // Create pulsing particles on the 6 flat faces of the block
        // Each face shows 1 particle at the center of that face
        
        Location blockCenter = blockLocation.clone().add(0.5, 0.5, 0.5);
        
        // Define the 6 faces: top, bottom, north, south, east, west
        double[][] faceOffsets = {
            {0.0, 0.5, 0.0},   // Top face
            {0.0, -0.5, 0.0},  // Bottom face
            {0.0, 0.0, -0.5},  // North face
            {0.0, 0.0, 0.5},   // South face
            {0.5, 0.0, 0.0},   // East face
            {-0.5, 0.0, 0.0}   // West face
        };
        
        // Show different faces based on pulse phase for pulsing effect
        for (int i = 0; i < faceOffsets.length; i++) {
            // Show 3 faces, then the other 3 faces, creating pulsing effect
            if ((i + pulsePhase) % 2 == 0) {
                Location faceLocation = blockCenter.clone().add(
                    faceOffsets[i][0], 
                    faceOffsets[i][1], 
                    faceOffsets[i][2]
                );
                
                player.spawnParticle(
                    Particle.BUBBLE_POP,
                    faceLocation,
                    3,
                    0.1, 0.1, 0.1,
                    0.0,
                    new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(255, 100, 0), 2.0f)
                );
            }
        }
    }
    
    public void shutdown() {
        if (detectorTask != null) {
            detectorTask.cancel();
        }
        activeDetectors.clear();
    }
    
    public boolean isPlayerUsingDetector(Player player) {
        return activeDetectors.contains(player);
    }
    
    public ItemStack createDetectorHelmet() {
        return customItemManager.createReinforcementDetectorHelmet();
    }
    
    public ItemStack createDetectorHelmet(int amount) {
        ItemStack helmet = customItemManager.createReinforcementDetectorHelmet();
        helmet.setAmount(amount);
        return helmet;
    }
}