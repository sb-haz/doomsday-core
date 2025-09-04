package gg.doomsday.core.nations;

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
    private final Map<String, BukkitRunnable> activeEffects;
    private final Map<String, Set<Location>> floodedBlocks;
    private final Map<String, Set<Location>> droughtBlocks;

    public DisasterEffectsHandler(JavaPlugin plugin, NationPlayerManager nationPlayerManager) {
        this.plugin = plugin;
        this.nationPlayerManager = nationPlayerManager;
        this.activeEffects = new HashMap<>();
        this.floodedBlocks = new HashMap<>();
        this.droughtBlocks = new HashMap<>();
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
            case "sandstorms":
                startSandstorm(nation, disaster, effectKey);
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
        Location meteorEnd = new Location(world, x, world.getHighestBlockYAt(x, z), z);
        
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
        
        // On impact, drop rare ores
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Location impact = meteor.getLocation();
            
            // Drop rare materials
            if (ThreadLocalRandom.current().nextDouble() < 0.7) {
                world.dropItem(impact, new org.bukkit.inventory.ItemStack(Material.IRON_ORE, 2 + ThreadLocalRandom.current().nextInt(4)));
            }
            if (ThreadLocalRandom.current().nextDouble() < 0.4) {
                world.dropItem(impact, new org.bukkit.inventory.ItemStack(Material.GOLD_ORE, 1 + ThreadLocalRandom.current().nextInt(3)));
            }
            if (ThreadLocalRandom.current().nextDouble() < 0.2) {
                world.dropItem(impact, new org.bukkit.inventory.ItemStack(Material.DIAMOND_ORE, 1));
            }
            
        }, 40L);
    }

    private void startWildfire(Nation nation, Disaster disaster, String effectKey) {
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
                
                // Spread fire every 5 seconds
                if (ticks % 100 == 0) {
                    spreadFire(nation);
                }
                
                ticks++;
            }
        };
        
        fireTask.runTaskTimer(plugin, 0L, 1L);
        activeEffects.put(effectKey, fireTask);
    }
    
    private void spreadFire(Nation nation) {
        World world = Bukkit.getWorlds().get(0);
        NationBorders borders = nation.getBorders();
        
        for (int attempts = 0; attempts < 15; attempts++) {
            int x = ThreadLocalRandom.current().nextInt(borders.getMinX(), borders.getMaxX());
            int z = ThreadLocalRandom.current().nextInt(borders.getMinZ(), borders.getMaxZ());
            int y = world.getHighestBlockYAt(x, z);
            
            Block block = world.getBlockAt(x, y, z);
            Block above = block.getRelative(BlockFace.UP);
            
            // Check if it's a flammable block (wood, leaves, grass)
            if ((block.getType().name().contains("LOG") || 
                 block.getType().name().contains("LEAVES") || 
                 block.getType() == Material.GRASS_BLOCK ||
                 block.getType() == Material.TALL_GRASS) && 
                 above.getType() == Material.AIR) {
                
                above.setType(Material.FIRE);
                world.spawnParticle(Particle.FLAME, above.getLocation().add(0.5, 0.5, 0.5), 10, 0.5, 0.5, 0.5, 0.1);
                world.playSound(above.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 0.5f, 1.0f);
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
            final double moveX = ThreadLocalRandom.current().nextDouble(-0.5, 0.5);
            final double moveZ = ThreadLocalRandom.current().nextDouble(-0.5, 0.5);
            
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
                
                // Tornado visual effects
                for (int y = 0; y < 20; y++) {
                    double radius = (20 - y) * 0.3;
                    for (int angle = 0; angle < 360; angle += 30) {
                        double radAngle = Math.toRadians(angle + ticks * 10);
                        double x = tornadoLoc.getX() + radius * Math.cos(radAngle);
                        double z = tornadoLoc.getZ() + radius * Math.sin(radAngle);
                        
                        Location particleLoc = new Location(world, x, tornadoLoc.getY() + y, z);
                        world.spawnParticle(Particle.CLOUD, particleLoc, 1, 0.1, 0.1, 0.1, 0.1);
                    }
                }
                
                // Only affect players who are members of this nation and are near the tornado
                for (Player player : getOnlinePlayersInNation(nation.getId())) {
                    if (player.getLocation().distance(tornadoLoc) <= 8) {
                        Vector pull = tornadoLoc.toVector().subtract(player.getLocation().toVector()).normalize();
                        pull.setY(0.5); // Lift up
                        player.setVelocity(pull.multiply(0.3));
                        
                        // Nausea effect (use appropriate name based on version)
                        PotionEffectType nauseaEffect = PotionEffectType.getByName("NAUSEA");
                        if (nauseaEffect == null) nauseaEffect = PotionEffectType.getByName("CONFUSION");
                        if (nauseaEffect != null) {
                            player.addPotionEffect(new PotionEffect(nauseaEffect, 60, 1));
                        }
                    }
                }
                
                // Sound effects
                if (ticks % 20 == 0) {
                    world.playSound(tornadoLoc, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.5f);
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
        NationBorders borders = nation.getBorders();
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
                
                // Expand flood every 10 seconds
                if (ticks % 200 == 0) {
                    expandFlood(nation, floodBlocks);
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
        
        for (int attempts = 0; attempts < 25; attempts++) {
            int x = ThreadLocalRandom.current().nextInt(borders.getMinX(), borders.getMaxX());
            int z = ThreadLocalRandom.current().nextInt(borders.getMinZ(), borders.getMaxZ());
            int y = world.getHighestBlockYAt(x, z);
            
            // Find low areas (rivers, valleys)
            if (y < 65) {
                Location floodLoc = new Location(world, x, y + 1, z);
                Block block = floodLoc.getBlock();
                
                if (block.getType() == Material.AIR) {
                    block.setType(Material.WATER);
                    floodBlocks.add(floodLoc);
                    
                    world.spawnParticle(Particle.WATER_SPLASH, floodLoc.add(0.5, 0.5, 0.5), 5, 0.5, 0.5, 0.5, 0.1);
                    
                    // Destroy crops
                    Block below = block.getRelative(BlockFace.DOWN);
                    if (below.getType().name().contains("CROP") || below.getType().name().contains("WHEAT") || 
                        below.getType().name().contains("CARROT") || below.getType().name().contains("POTATO")) {
                        below.setType(Material.FARMLAND);
                    }
                }
            }
        }
    }

    private void startPlague(Nation nation, Disaster disaster, String effectKey) {
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
                
                // Affect players every 30 seconds
                if (ticks % 600 == 0) {
                    spreadPlague(nation);
                }
                
                ticks++;
            }
        };
        
        plagueTask.runTaskTimer(plugin, 0L, 1L);
        activeEffects.put(effectKey, plagueTask);
    }
    
    private void spreadPlague(Nation nation) {
        // Only affect players who are members of this nation
        for (Player player : getOnlinePlayersInNation(nation.getId())) {
            // Slow hunger drain
            player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 1200, 0)); // 1 minute
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 600, 0)); // 30 seconds
            
            if (ThreadLocalRandom.current().nextDouble() < 0.3) {
                player.sendMessage("§2§l[PLAGUE] §7You feel a creeping sickness...");
            }
        }
    }

    private void startStorms(Nation nation, Disaster disaster, String effectKey) {
        World world = Bukkit.getWorlds().get(0);
        NationBorders borders = nation.getBorders();
        
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
                
                // Lightning strikes every 5-10 seconds
                if (ticks % (100 + ThreadLocalRandom.current().nextInt(100)) == 0) {
                    strikeLightning(nation);
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
        if (ThreadLocalRandom.current().nextDouble() < 0.4) {
            Block above = strikeLoc.getBlock().getRelative(BlockFace.UP);
            if (above.getType() == Material.AIR) {
                above.setType(Material.FIRE);
            }
        }
    }

    // AFRICA DISASTERS
    private void startDrought(Nation nation, Disaster disaster, String effectKey) {
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
                
                // Dry up water every 15 seconds
                if (ticks % 300 == 0) {
                    dryUpWater(nation, driedBlocks);
                }
                
                // Affect players every 20 seconds
                if (ticks % 400 == 0) {
                    affectPlayersWithDrought(nation);
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
        
        for (int attempts = 0; attempts < 20; attempts++) {
            int x = ThreadLocalRandom.current().nextInt(borders.getMinX(), borders.getMaxX());
            int z = ThreadLocalRandom.current().nextInt(borders.getMinZ(), borders.getMaxZ());
            
            for (int y = borders.getMinY(); y <= borders.getMaxY(); y++) {
                Block block = world.getBlockAt(x, y, z);
                if (block.getType() == Material.WATER) {
                    block.setType(Material.AIR);
                    driedBlocks.add(block.getLocation());
                    
                    world.spawnParticle(Particle.CLOUD, block.getLocation().add(0.5, 0.5, 0.5), 3, 0.5, 0.5, 0.5, 0.0);
                    break;
                }
            }
        }
        
        // Destroy crops
        for (int attempts = 0; attempts < 10; attempts++) {
            int x = ThreadLocalRandom.current().nextInt(borders.getMinX(), borders.getMaxX());
            int z = ThreadLocalRandom.current().nextInt(borders.getMinZ(), borders.getMaxZ());
            int y = world.getHighestBlockYAt(x, z);
            
            Block block = world.getBlockAt(x, y, z);
            if (block.getType().name().contains("CROP") || block.getType().name().contains("WHEAT") || 
                block.getType().name().contains("CARROT") || block.getType().name().contains("POTATO")) {
                block.setType(Material.DEAD_BUSH);
            }
        }
    }
    
    private void affectPlayersWithDrought(Nation nation) {
        // Only affect players who are members of this nation
        for (Player player : getOnlinePlayersInNation(nation.getId())) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 600, 1)); // 30 seconds, level 2
            
            if (ThreadLocalRandom.current().nextDouble() < 0.2) {
                player.sendMessage("§6§l[DROUGHT] §7The scorching heat drains your energy...");
            }
        }
    }

    private void startSandstorm(Nation nation, Disaster disaster, String effectKey) {
        BukkitRunnable sandstormTask = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = disaster.getDuration();
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    activeEffects.remove(effectKey);
                    return;
                }
                
                // Apply effects every 5 seconds
                if (ticks % 100 == 0) {
                    applySandstormEffects(nation);
                }
                
                ticks++;
            }
        };
        
        sandstormTask.runTaskTimer(plugin, 0L, 1L);
        activeEffects.put(effectKey, sandstormTask);
    }
    
    private void applySandstormEffects(Nation nation) {
        World world = Bukkit.getWorlds().get(0);
        NationBorders borders = nation.getBorders();
        
        // Only affect players who are members of this nation
        for (Player player : getOnlinePlayersInNation(nation.getId())) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 200, 0)); // 10 seconds
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 200, 1)); // 10 seconds, level 2
            
            // Sand particles around player
            world.spawnParticle(Particle.BLOCK_DUST, player.getLocation().add(0, 1, 0), 20, 2, 2, 2, 0.1, Material.SAND.createBlockData());
        }
        
        // Environmental sand particles
        for (int i = 0; i < 15; i++) {
            int x = ThreadLocalRandom.current().nextInt(borders.getMinX(), borders.getMaxX());
            int z = ThreadLocalRandom.current().nextInt(borders.getMinZ(), borders.getMaxZ());
            int y = world.getHighestBlockYAt(x, z) + 5 + ThreadLocalRandom.current().nextInt(10);
            
            Location particleLoc = new Location(world, x, y, z);
            world.spawnParticle(Particle.BLOCK_DUST, particleLoc, 5, 3, 3, 3, 0.1, Material.SAND.createBlockData());
        }
    }

    // ANTARCTICA DISASTERS
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
                
                // Apply effects every 3 seconds
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
        
        // Only affect players who are members of this nation
        for (Player player : getOnlinePlayersInNation(nation.getId())) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 120, 0)); // 6 seconds
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 200, 2)); // 10 seconds, level 3
            // Mining fatigue effect (use appropriate name based on version)
            PotionEffectType miningFatigueEffect = PotionEffectType.getByName("MINING_FATIGUE");
            if (miningFatigueEffect == null) miningFatigueEffect = PotionEffectType.getByName("SLOW_DIGGING");
            if (miningFatigueEffect != null) {
                player.addPotionEffect(new PotionEffect(miningFatigueEffect, 200, 1)); // 10 seconds
            }
            
            // Snow particles around player
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
        
        // Occasionally place snow
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
        NationBorders borders = nation.getBorders();
        
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
                
                // Apply effects every 4 seconds
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
        
        // Only affect players who are members of this nation
        for (Player player : getOnlinePlayersInNation(nation.getId())) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 300, 3)); // 15 seconds, level 4
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 300, 1)); // 15 seconds
            player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 200, 0)); // 10 seconds
            
            // Ice particles and damage
            world.spawnParticle(Particle.BLOCK_CRACK, player.getLocation().add(0, 1, 0), 15, 2, 2, 2, 0.1, Material.ICE.createBlockData());
            
            if (ThreadLocalRandom.current().nextDouble() < 0.1) {
                player.damage(1.0); // Small damage from ice
                player.sendMessage("§b§l[ICE STORM] §7Sharp ice crystals cut through the air!");
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

    /**
     * Helper method to get all online players in a nation using the centralized tracking system
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
        
        // Cleanup floods and droughts
        for (String effectKey : new HashSet<>(floodedBlocks.keySet())) {
            cleanupFlooding(effectKey);
        }
        
        for (String effectKey : new HashSet<>(droughtBlocks.keySet())) {
            cleanupDrought(effectKey);
        }
    }
}