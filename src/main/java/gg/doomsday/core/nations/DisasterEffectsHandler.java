package gg.doomsday.core.nations;

import gg.doomsday.core.managers.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class DisasterEffectsHandler {
    private final JavaPlugin plugin;
    private final NationPlayerManager nationPlayerManager;
    private final MessageManager messageManager;
    private final Map<String, BukkitRunnable> activeEffects;
    private final Map<String, Set<Location>> floodedBlocks;
    private final Map<String, Set<Location>> droughtBlocks;
    
    // Plague tracking
    private final Map<String, Set<UUID>> infectedPlayers;
    private final Map<String, Map<UUID, Long>> thirstyPlayers;
    
    // Configuration values
    private final int PLAGUE_SPREAD_RADIUS = 10;
    private final boolean PLAGUE_CAN_KILL = true;
    private final int DROUGHT_THIRST_DAMAGE = 1;
    private final long DROUGHT_DAMAGE_INTERVAL = 10000; // 10 seconds in milliseconds

    public DisasterEffectsHandler(JavaPlugin plugin, NationPlayerManager nationPlayerManager) {
        this.plugin = plugin;
        this.nationPlayerManager = nationPlayerManager;
        this.messageManager = new MessageManager(plugin);
        this.activeEffects = new HashMap<>();
        this.floodedBlocks = new HashMap<>();
        this.droughtBlocks = new HashMap<>();
        this.infectedPlayers = new HashMap<>();
        this.thirstyPlayers = new HashMap<>();
    }

    public void triggerDisaster(Nation nation, Disaster disaster) {
        String effectKey = nation.getId() + "_" + disaster.getId();
        
        // Stop any existing effect for this disaster
        stopDisaster(nation, disaster);
        
        // Start the appropriate disaster effect
        switch (disaster.getId()) {
            // America disasters
            case "meteor_showers":
                startMeteorShower(nation, disaster, effectKey);
                break;
            case "wildfires":
                startWildfire(nation, disaster, effectKey);
                break;
            case "tornadoes":
                startTornado(nation, disaster, effectKey);
                break;
            
            // Europe disasters
            case "flooding":
                startFlooding(nation, disaster, effectKey);
                break;
            case "plagues":
                startPlague(nation, disaster, effectKey);
                break;
            case "storms":
                startStorms(nation, disaster, effectKey);
                break;
            
            // Africa disasters
            case "droughts":
                startDrought(nation, disaster, effectKey);
                break;
            
            // Antarctica disasters
            case "blizzards":
                startBlizzard(nation, disaster, effectKey);
                break;
            case "ice_storms":
                startIceStorm(nation, disaster, effectKey);
                break;
                
            default:
                plugin.getLogger().warning("Unknown disaster type: " + disaster.getId());
        }
    }

    public void stopDisaster(Nation nation, Disaster disaster) {
        String effectKey = nation.getId() + "_" + disaster.getId();
        
        // Cancel running effect
        BukkitRunnable effect = activeEffects.get(effectKey);
        if (effect != null) {
            effect.cancel();
            activeEffects.remove(effectKey);
        }
        
        // Cleanup specific disaster effects
        switch (disaster.getId()) {
            case "flooding":
                cleanupFlooding(effectKey);
                break;
            case "droughts":
                cleanupDrought(effectKey);
                cleanupThirst(effectKey);
                break;
            case "plagues":
                cleanupPlague(effectKey);
                break;
        }
    }

    // AMERICA DISASTERS
    private void startMeteorShower(Nation nation, Disaster disaster, String effectKey) {
        BukkitRunnable meteorTask = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = disaster.getDuration();
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    activeEffects.remove(effectKey);
                    return;
                }
                
                // Spawn meteors every 3-5 seconds
                if (ticks % (60 + ThreadLocalRandom.current().nextInt(40)) == 0) {
                    spawnMeteor(nation);
                }
                
                ticks++;
            }
        };
        
        meteorTask.runTaskTimer(plugin, 0L, 1L);
        activeEffects.put(effectKey, meteorTask);
    }
    
    private void spawnMeteor(Nation nation) {
        World world = Bukkit.getWorlds().get(0);
        NationBorders borders = nation.getBorders();
        
        // Random location within nation
        int x = ThreadLocalRandom.current().nextInt(borders.getMinX(), borders.getMaxX());
        int z = ThreadLocalRandom.current().nextInt(borders.getMinZ(), borders.getMaxZ());
        int y = world.getHighestBlockYAt(x, z) + 20;
        
        Location meteorStart = new Location(world, x, y, z);
        
        // Create meteor effect
        TNTPrimed meteor = world.spawn(meteorStart, TNTPrimed.class);
        meteor.setFuseTicks(40); // 2 seconds to impact
        
        // Meteor trail effect
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 40 || meteor.isDead()) {
                    cancel();
                    return;
                }
                
                Location loc = meteor.getLocation();
                world.spawnParticle(Particle.FLAME, loc, 5, 0.2, 0.2, 0.2, 0.1);
                world.spawnParticle(Particle.SMOKE_LARGE, loc, 3, 0.3, 0.3, 0.3, 0.0);
                
                if (ticks % 10 == 0) {
                    world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.5f, 0.8f);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        
        // On impact, drop valuable ores including diamonds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Location impact = meteor.getLocation();
            
            // Drop rare materials with better rewards
            if (ThreadLocalRandom.current().nextDouble() < 0.8) {
                world.dropItem(impact, new ItemStack(Material.IRON_ORE, 3 + ThreadLocalRandom.current().nextInt(5)));
            }
            if (ThreadLocalRandom.current().nextDouble() < 0.6) {
                world.dropItem(impact, new ItemStack(Material.GOLD_ORE, 2 + ThreadLocalRandom.current().nextInt(4)));
            }
            if (ThreadLocalRandom.current().nextDouble() < 0.4) {
                world.dropItem(impact, new ItemStack(Material.DIAMOND_ORE, 1 + ThreadLocalRandom.current().nextInt(3)));
            }
            if (ThreadLocalRandom.current().nextDouble() < 0.2) {
                world.dropItem(impact, new ItemStack(Material.EMERALD_ORE, 1 + ThreadLocalRandom.current().nextInt(2)));
            }
            // Sometimes drop rare blocks
            if (ThreadLocalRandom.current().nextDouble() < 0.1) {
                world.dropItem(impact, new ItemStack(Material.ANCIENT_DEBRIS, 1));
            }
            
        }, 40L);
    }

    private void startWildfire(Nation nation, Disaster disaster, String effectKey) {
        // Create 2-3 smaller fire centers instead of spreading everywhere
        List<Location> fireCenters = new ArrayList<>();
        World world = Bukkit.getWorlds().get(0);
        NationBorders borders = nation.getBorders();
        
        // Create 2-3 fire centers
        for (int i = 0; i < 2 + ThreadLocalRandom.current().nextInt(2); i++) {
            int x = ThreadLocalRandom.current().nextInt(borders.getMinX(), borders.getMaxX());
            int z = ThreadLocalRandom.current().nextInt(borders.getMinZ(), borders.getMaxZ());
            int y = world.getHighestBlockYAt(x, z);
            fireCenters.add(new Location(world, x, y, z));
        }
        
        BukkitRunnable fireTask = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = disaster.getDuration();
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    activeEffects.remove(effectKey);
                    return;
                }
                
                // Spread fire more slowly and in smaller areas
                if (ticks % 200 == 0) { // Every 10 seconds instead of 5
                    for (Location center : fireCenters) {
                        spreadFireAroundCenter(center, 15); // 15 block radius
                    }
                }
                
                ticks++;
            }
        };
        
        fireTask.runTaskTimer(plugin, 0L, 1L);
        activeEffects.put(effectKey, fireTask);
    }
    
    private void spreadFireAroundCenter(Location center, int radius) {
        World world = center.getWorld();
        
        // Only spread 3-5 fires per center per tick
        for (int attempts = 0; attempts < 5; attempts++) {
            int x = center.getBlockX() + ThreadLocalRandom.current().nextInt(-radius, radius + 1);
            int z = center.getBlockZ() + ThreadLocalRandom.current().nextInt(-radius, radius + 1);
            int y = world.getHighestBlockYAt(x, z);
            
            Block block = world.getBlockAt(x, y, z);
            Block above = block.getRelative(BlockFace.UP);
            
            // Check if it's a flammable block
            if ((block.getType().name().contains("LOG") || 
                 block.getType().name().contains("LEAVES") || 
                 block.getType() == Material.GRASS_BLOCK ||
                 block.getType() == Material.TALL_GRASS) && 
                 above.getType() == Material.AIR) {
                
                above.setType(Material.FIRE);
                world.spawnParticle(Particle.FLAME, above.getLocation().add(0.5, 0.5, 0.5), 10, 0.5, 0.5, 0.5, 0.1);
                world.playSound(above.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 0.3f, 1.0f);
            }
        }
    }

    private void startTornado(Nation nation, Disaster disaster, String effectKey) {
        World world = Bukkit.getWorlds().get(0);
        NationBorders borders = nation.getBorders();
        
        // Random tornado path
        int startX = ThreadLocalRandom.current().nextInt(borders.getMinX(), borders.getMaxX());
        int startZ = ThreadLocalRandom.current().nextInt(borders.getMinZ(), borders.getMaxZ());
        
        BukkitRunnable tornadoTask = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = disaster.getDuration();
            double currentX = startX;
            double currentZ = startZ;
            final double moveX = ThreadLocalRandom.current().nextDouble(-0.3, 0.3);
            final double moveZ = ThreadLocalRandom.current().nextDouble(-0.3, 0.3);
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    activeEffects.remove(effectKey);
                    return;
                }
                
                // Move tornado
                currentX += moveX;
                currentZ += moveZ;
                
                Location tornadoLoc = new Location(world, currentX, world.getHighestBlockYAt((int)currentX, (int)currentZ), currentZ);
                
                // Enhanced tornado visual effects
                for (int y = 0; y < 25; y++) {
                    double radius = Math.max(0.5, (25 - y) * 0.4);
                    for (int angle = 0; angle < 360; angle += 20) {
                        double radAngle = Math.toRadians(angle + ticks * 15); // Faster spinning
                        double x = tornadoLoc.getX() + radius * Math.cos(radAngle);
                        double z = tornadoLoc.getZ() + radius * Math.sin(radAngle);
                        
                        Location particleLoc = new Location(world, x, tornadoLoc.getY() + y, z);
                        world.spawnParticle(Particle.CLOUD, particleLoc, 2, 0.1, 0.1, 0.1, 0.2);
                        world.spawnParticle(Particle.CRIT, particleLoc, 1, 0.2, 0.2, 0.2, 0.1);
                    }
                }
                
                // Enhanced player effects - throw and spin players
                for (Player player : getOnlinePlayersInNation(nation.getId())) {
                    double distance = player.getLocation().distance(tornadoLoc);
                    if (distance <= 12) { // Larger effect radius
                        // Create spinning motion
                        double angle = Math.toRadians(ticks * 20); // Fast spinning
                        Vector pull = new Vector(
                            Math.cos(angle) * 0.5,
                            0.8 + ThreadLocalRandom.current().nextDouble() * 0.6, // Random upward force
                            Math.sin(angle) * 0.5
                        );
                        
                        // Add inward pull toward tornado center
                        Vector toCenter = tornadoLoc.toVector().subtract(player.getLocation().toVector()).normalize();
                        pull.add(toCenter.multiply(0.3));
                        
                        player.setVelocity(pull);
                        
                        // Confusion and nausea effects
                        PotionEffectType nauseaEffect = PotionEffectType.getByName("NAUSEA");
                        if (nauseaEffect == null) nauseaEffect = PotionEffectType.getByName("CONFUSION");
                        if (nauseaEffect != null) {
                            player.addPotionEffect(new PotionEffect(nauseaEffect, 100, 2));
                        }
                        
                        // Slowness to simulate being caught in tornado
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 1));
                        
                        // Particle effects around player
                        world.spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 10, 1, 1, 1, 0.3);
                    }
                }
                
                // Sound effects
                if (ticks % 15 == 0) {
                    world.playSound(tornadoLoc, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.3f);
                }
                
                ticks++;
            }
        };
        
        tornadoTask.runTaskTimer(plugin, 0L, 2L);
        activeEffects.put(effectKey, tornadoTask);
    }

    // EUROPE DISASTERS
    private void startFlooding(Nation nation, Disaster disaster, String effectKey) {
        World world = Bukkit.getWorlds().get(0);
        Set<Location> floodBlocks = new HashSet<>();
        
        BukkitRunnable floodTask = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = disaster.getDuration();
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    activeEffects.remove(effectKey);
                    return;
                }
                
                // Expand flood every 8 seconds
                if (ticks % 160 == 0) {
                    expandFlood(nation, floodBlocks);
                }
                
                // Apply water damage to players in flooded areas
                if (ticks % 100 == 0) {
                    applyFloodEffects(nation, floodBlocks);
                }
                
                ticks++;
            }
        };
        
        floodedBlocks.put(effectKey, floodBlocks);
        floodTask.runTaskTimer(plugin, 0L, 1L);
        activeEffects.put(effectKey, floodTask);
    }
    
    private void expandFlood(Nation nation, Set<Location> floodBlocks) {
        World world = Bukkit.getWorlds().get(0);
        NationBorders borders = nation.getBorders();
        
        for (int attempts = 0; attempts < 30; attempts++) {
            int x = ThreadLocalRandom.current().nextInt(borders.getMinX(), borders.getMaxX());
            int z = ThreadLocalRandom.current().nextInt(borders.getMinZ(), borders.getMaxZ());
            int y = world.getHighestBlockYAt(x, z);
            
            // Target low areas and near existing water
            if (y < 67 || isNearWater(world, x, y, z)) {
                Location floodLoc = new Location(world, x, y + 1, z);
                Block block = floodLoc.getBlock();
                
                if (block.getType() == Material.AIR) {
                    block.setType(Material.WATER);
                    floodBlocks.add(floodLoc);
                    
                    world.spawnParticle(Particle.WATER_SPLASH, floodLoc.add(0.5, 0.5, 0.5), 8, 0.5, 0.5, 0.5, 0.1);
                    
                    // Destroy crops
                    Block below = block.getRelative(BlockFace.DOWN);
                    if (isCrop(below.getType())) {
                        below.setType(Material.FARMLAND);
                    }
                }
            }
        }
    }
    
    private void applyFloodEffects(Nation nation, Set<Location> floodBlocks) {
        for (Player player : getOnlinePlayersInNation(nation.getId())) {
            Location playerLoc = player.getLocation();
            
            // Check if player is in flooded area
            for (Location floodLoc : floodBlocks) {
                if (playerLoc.distance(floodLoc) < 2.0) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 1));
                    if (ThreadLocalRandom.current().nextDouble() < 0.1) {
                        player.damage(1.0); // Drowning damage
                    }
                    break;
                }
            }
        }
    }
    
    private boolean isNearWater(World world, int x, int y, int z) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (world.getBlockAt(x + dx, y, z + dz).getType() == Material.WATER) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean isCrop(Material material) {
        String name = material.name();
        return name.contains("CROP") || name.contains("WHEAT") || 
               name.contains("CARROT") || name.contains("POTATO") ||
               name.contains("BEETROOT");
    }

    private void startPlague(Nation nation, Disaster disaster, String effectKey) {
        Set<UUID> infected = new HashSet<>();
        infectedPlayers.put(effectKey, infected);
        
        // Start with 1-2 patient zeros
        List<Player> nationPlayers = getOnlinePlayersInNation(nation.getId());
        if (!nationPlayers.isEmpty()) {
            for (int i = 0; i < Math.min(2, nationPlayers.size()); i++) {
                Player patient = nationPlayers.get(ThreadLocalRandom.current().nextInt(nationPlayers.size()));
                infected.add(patient.getUniqueId());
                patient.sendMessage(messageManager.getMessage("disasters.plagues.infected"));
            }
        }
        
        BukkitRunnable plagueTask = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = disaster.getDuration();
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    activeEffects.remove(effectKey);
                    return;
                }
                
                // Spread plague every 10 seconds
                if (ticks % 200 == 0) {
                    spreadPlague(nation, infected);
                }
                
                // Apply plague effects every 30 seconds
                if (ticks % 600 == 0) {
                    applyPlagueEffects(infected);
                }
                
                ticks++;
            }
        };
        
        plagueTask.runTaskTimer(plugin, 0L, 1L);
        activeEffects.put(effectKey, plagueTask);
    }
    
    private void spreadPlague(Nation nation, Set<UUID> infected) {
        List<Player> allPlayers = getOnlinePlayersInNation(nation.getId());
        List<UUID> newInfections = new ArrayList<>();
        
        for (UUID infectedId : infected) {
            Player infectedPlayer = Bukkit.getPlayer(infectedId);
            if (infectedPlayer == null || !infectedPlayer.isOnline()) continue;
            
            // Check for nearby players to infect
            for (Player nearbyPlayer : allPlayers) {
                if (infected.contains(nearbyPlayer.getUniqueId())) continue;
                
                if (infectedPlayer.getLocation().distance(nearbyPlayer.getLocation()) <= PLAGUE_SPREAD_RADIUS) {
                    if (ThreadLocalRandom.current().nextDouble() < 0.3) { // 30% infection chance
                        newInfections.add(nearbyPlayer.getUniqueId());
                        nearbyPlayer.sendMessage(messageManager.getMessage("disasters.plagues.spreading"));
                    }
                }
            }
        }
        
        infected.addAll(newInfections);
    }
    
    private void applyPlagueEffects(Set<UUID> infected) {
        for (UUID playerId : infected) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) continue;
            
            // Apply plague effects
            player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 1200, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 600, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 0));
            
            // Chance to deal damage or kill if configured
            if (PLAGUE_CAN_KILL && ThreadLocalRandom.current().nextDouble() < 0.05) { // 5% chance
                player.damage(2.0);
            }
            
            // Visual effects
            player.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, 
                player.getLocation().add(0, 2, 0), 3, 0.5, 0.5, 0.5, 0.0);
        }
    }

    private void startStorms(Nation nation, Disaster disaster, String effectKey) {
        World world = Bukkit.getWorlds().get(0);
        
        BukkitRunnable stormTask = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = disaster.getDuration();
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    activeEffects.remove(effectKey);
                    return;
                }
                
                // Lightning strikes every 3-8 seconds
                if (ticks % (60 + ThreadLocalRandom.current().nextInt(100)) == 0) {
                    strikeLightning(nation);
                }
                
                // Rain effects every 5 seconds
                if (ticks % 100 == 0) {
                    createRainEffects(nation);
                }
                
                ticks++;
            }
        };
        
        stormTask.runTaskTimer(plugin, 0L, 1L);
        activeEffects.put(effectKey, stormTask);
    }
    
    private void strikeLightning(Nation nation) {
        World world = Bukkit.getWorlds().get(0);
        NationBorders borders = nation.getBorders();
        
        int x = ThreadLocalRandom.current().nextInt(borders.getMinX(), borders.getMaxX());
        int z = ThreadLocalRandom.current().nextInt(borders.getMinZ(), borders.getMaxZ());
        int y = world.getHighestBlockYAt(x, z);
        
        Location strikeLoc = new Location(world, x, y, z);
        world.strikeLightning(strikeLoc);
        
        // Chance to start fires
        if (ThreadLocalRandom.current().nextDouble() < 0.3) {
            Block above = strikeLoc.getBlock().getRelative(BlockFace.UP);
            if (above.getType() == Material.AIR) {
                above.setType(Material.FIRE);
            }
        }
        
        // Damage nearby players
        for (Player player : getOnlinePlayersInNation(nation.getId())) {
            if (player.getLocation().distance(strikeLoc) <= 5.0) {
                player.damage(3.0);
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
            }
        }
    }
    
    private void createRainEffects(Nation nation) {
        World world = Bukkit.getWorlds().get(0);
        NationBorders borders = nation.getBorders();
        
        // Create rain particle effects
        for (int i = 0; i < 20; i++) {
            int x = ThreadLocalRandom.current().nextInt(borders.getMinX(), borders.getMaxX());
            int z = ThreadLocalRandom.current().nextInt(borders.getMinZ(), borders.getMaxZ());
            int y = world.getHighestBlockYAt(x, z) + 10;
            
            Location rainLoc = new Location(world, x, y, z);
            world.spawnParticle(Particle.WATER_DROP, rainLoc, 3, 2, 0, 2, 0.5);
        }
    }

    // AFRICA DISASTERS
    private void startDrought(Nation nation, Disaster disaster, String effectKey) {
        Map<UUID, Long> thirstMap = new HashMap<>();
        thirstyPlayers.put(effectKey, thirstMap);
        
        World world = Bukkit.getWorlds().get(0);
        NationBorders borders = nation.getBorders();
        Set<Location> driedBlocks = new HashSet<>();
        
        BukkitRunnable droughtTask = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = disaster.getDuration();
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    activeEffects.remove(effectKey);
                    return;
                }
                
                // Dry up water every 20 seconds
                if (ticks % 400 == 0) {
                    dryUpWater(nation, driedBlocks);
                }
                
                // Apply thirst effects every 5 seconds
                if (ticks % 100 == 0) {
                    applyThirstEffects(nation, thirstMap);
                }
                
                ticks++;
            }
        };
        
        droughtBlocks.put(effectKey, driedBlocks);
        droughtTask.runTaskTimer(plugin, 0L, 1L);
        activeEffects.put(effectKey, droughtTask);
    }
    
    private void dryUpWater(Nation nation, Set<Location> driedBlocks) {
        World world = Bukkit.getWorlds().get(0);
        NationBorders borders = nation.getBorders();
        
        for (int attempts = 0; attempts < 25; attempts++) {
            int x = ThreadLocalRandom.current().nextInt(borders.getMinX(), borders.getMaxX());
            int z = ThreadLocalRandom.current().nextInt(borders.getMinZ(), borders.getMaxZ());
            
            for (int y = borders.getMinY(); y <= borders.getMaxY(); y++) {
                Block block = world.getBlockAt(x, y, z);
                if (block.getType() == Material.WATER) {
                    block.setType(Material.AIR);
                    driedBlocks.add(block.getLocation());
                    
                    world.spawnParticle(Particle.SMOKE_NORMAL, block.getLocation().add(0.5, 0.5, 0.5), 5, 0.5, 0.5, 0.5, 0.0);
                    break;
                }
            }
        }
        
        // Destroy crops
        for (int attempts = 0; attempts < 15; attempts++) {
            int x = ThreadLocalRandom.current().nextInt(borders.getMinX(), borders.getMaxX());
            int z = ThreadLocalRandom.current().nextInt(borders.getMinZ(), borders.getMaxZ());
            int y = world.getHighestBlockYAt(x, z);
            
            Block block = world.getBlockAt(x, y, z);
            if (isCrop(block.getType())) {
                block.setType(Material.DEAD_BUSH);
            }
        }
    }
    
    private void applyThirstEffects(Nation nation, Map<UUID, Long> thirstMap) {
        long currentTime = System.currentTimeMillis();
        
        for (Player player : getOnlinePlayersInNation(nation.getId())) {
            UUID playerId = player.getUniqueId();
            
            // Check if player drank water recently (has water bucket or water bottle in inventory)
            boolean hasWater = player.getInventory().contains(Material.WATER_BUCKET) || 
                             player.getInventory().contains(Material.POTION); // Water bottles are potions
            
            if (hasWater) {
                thirstMap.remove(playerId); // Reset thirst if they have water
                continue;
            }
            
            Long lastThirstTime = thirstMap.get(playerId);
            if (lastThirstTime == null) {
                thirstMap.put(playerId, currentTime);
                player.sendMessage(messageManager.getMessage("disasters.droughts.thirst"));
            } else {
                long timeSinceThirsty = currentTime - lastThirstTime;
                
                if (timeSinceThirsty > DROUGHT_DAMAGE_INTERVAL) {
                    // Start taking damage from thirst
                    player.damage(DROUGHT_THIRST_DAMAGE);
                    player.sendMessage(messageManager.getMessage("disasters.droughts.dying"));
                    
                    // Apply hunger and weakness
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 200, 2));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 1));
                    
                    // Update last damage time
                    thirstMap.put(playerId, currentTime);
                }
            }
        }
    }

    // ANTARCTICA DISASTERS (keeping existing implementations)
    private void startBlizzard(Nation nation, Disaster disaster, String effectKey) {
        BukkitRunnable blizzardTask = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = disaster.getDuration();
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    activeEffects.remove(effectKey);
                    return;
                }
                
                if (ticks % 60 == 0) {
                    applyBlizzardEffects(nation);
                }
                
                ticks++;
            }
        };
        
        blizzardTask.runTaskTimer(plugin, 0L, 1L);
        activeEffects.put(effectKey, blizzardTask);
    }
    
    private void applyBlizzardEffects(Nation nation) {
        World world = Bukkit.getWorlds().get(0);
        NationBorders borders = nation.getBorders();
        
        for (Player player : getOnlinePlayersInNation(nation.getId())) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 120, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 200, 2));
            
            PotionEffectType miningFatigueEffect = PotionEffectType.getByName("MINING_FATIGUE");
            if (miningFatigueEffect == null) miningFatigueEffect = PotionEffectType.getByName("SLOW_DIGGING");
            if (miningFatigueEffect != null) {
                player.addPotionEffect(new PotionEffect(miningFatigueEffect, 200, 1));
            }
            
            world.spawnParticle(Particle.SNOWBALL, player.getLocation().add(0, 2, 0), 30, 3, 3, 3, 0.2);
        }
        
        // Environmental snow effects
        for (int i = 0; i < 25; i++) {
            int x = ThreadLocalRandom.current().nextInt(borders.getMinX(), borders.getMaxX());
            int z = ThreadLocalRandom.current().nextInt(borders.getMinZ(), borders.getMaxZ());
            int y = world.getHighestBlockYAt(x, z) + 5 + ThreadLocalRandom.current().nextInt(15);
            
            Location particleLoc = new Location(world, x, y, z);
            world.spawnParticle(Particle.SNOWBALL, particleLoc, 8, 4, 4, 4, 0.1);
        }
        
        if (ThreadLocalRandom.current().nextDouble() < 0.3) {
            int x = ThreadLocalRandom.current().nextInt(borders.getMinX(), borders.getMaxX());
            int z = ThreadLocalRandom.current().nextInt(borders.getMinZ(), borders.getMaxZ());
            int y = world.getHighestBlockYAt(x, z);
            
            Block block = world.getBlockAt(x, y + 1, z);
            if (block.getType() == Material.AIR) {
                block.setType(Material.SNOW);
            }
        }
    }

    private void startIceStorm(Nation nation, Disaster disaster, String effectKey) {
        World world = Bukkit.getWorlds().get(0);
        
        BukkitRunnable iceStormTask = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = disaster.getDuration();
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    activeEffects.remove(effectKey);
                    return;
                }
                
                if (ticks % 80 == 0) {
                    applyIceStormEffects(nation);
                }
                
                ticks++;
            }
        };
        
        iceStormTask.runTaskTimer(plugin, 0L, 1L);
        activeEffects.put(effectKey, iceStormTask);
    }
    
    private void applyIceStormEffects(Nation nation) {
        World world = Bukkit.getWorlds().get(0);
        NationBorders borders = nation.getBorders();
        
        for (Player player : getOnlinePlayersInNation(nation.getId())) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 300, 3));
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 300, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 200, 0));
            
            world.spawnParticle(Particle.BLOCK_CRACK, player.getLocation().add(0, 1, 0), 15, 2, 2, 2, 0.1, Material.ICE.createBlockData());
            
            if (ThreadLocalRandom.current().nextDouble() < 0.1) {
                player.damage(1.0);
                player.sendMessage(messageManager.getMessage("disasters.ice_storms.damage"));
            }
        }
        
        // Freeze water sources
        for (int attempts = 0; attempts < 10; attempts++) {
            int x = ThreadLocalRandom.current().nextInt(borders.getMinX(), borders.getMaxX());
            int z = ThreadLocalRandom.current().nextInt(borders.getMinZ(), borders.getMaxZ());
            
            for (int y = borders.getMinY(); y <= borders.getMaxY(); y++) {
                Block block = world.getBlockAt(x, y, z);
                if (block.getType() == Material.WATER) {
                    block.setType(Material.ICE);
                    world.spawnParticle(Particle.BLOCK_CRACK, block.getLocation().add(0.5, 0.5, 0.5), 5, 0.5, 0.5, 0.5, 0.1, Material.ICE.createBlockData());
                    break;
                }
            }
        }
    }

    // CLEANUP METHODS
    private void cleanupFlooding(String effectKey) {
        Set<Location> floodBlocks = floodedBlocks.get(effectKey);
        if (floodBlocks != null) {
            for (Location loc : floodBlocks) {
                Block block = loc.getBlock();
                if (block.getType() == Material.WATER) {
                    block.setType(Material.AIR);
                }
            }
            floodedBlocks.remove(effectKey);
        }
    }

    private void cleanupDrought(String effectKey) {
        Set<Location> driedBlocks = droughtBlocks.get(effectKey);
        if (driedBlocks != null) {
            for (Location loc : driedBlocks) {
                Block block = loc.getBlock();
                if (block.getType() == Material.AIR) {
                    block.setType(Material.WATER);
                }
            }
            droughtBlocks.remove(effectKey);
        }
    }
    
    private void cleanupPlague(String effectKey) {
        infectedPlayers.remove(effectKey);
    }
    
    private void cleanupThirst(String effectKey) {
        thirstyPlayers.remove(effectKey);
    }

    /**
     * Helper method to get all online players in a nation
     */
    private List<Player> getOnlinePlayersInNation(String nationId) {
        List<Player> players = new ArrayList<>();
        for (UUID playerId : nationPlayerManager.getOnlinePlayersInNation(nationId)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                players.add(player);
            }
        }
        return players;
    }

    public void shutdown() {
        // Cancel all active effects
        for (BukkitRunnable effect : activeEffects.values()) {
            if (effect != null) {
                effect.cancel();
            }
        }
        activeEffects.clear();
        
        // Cleanup all disaster effects
        for (String effectKey : new HashSet<>(floodedBlocks.keySet())) {
            cleanupFlooding(effectKey);
        }
        
        for (String effectKey : new HashSet<>(droughtBlocks.keySet())) {
            cleanupDrought(effectKey);
        }
        
        infectedPlayers.clear();
        thirstyPlayers.clear();
    }
}