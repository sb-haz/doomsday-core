package gg.doomsday.core.explosions;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import gg.doomsday.core.config.ConfigManager;
import gg.doomsday.core.defense.ReinforcedBlockManager;

public class ExplosionHandler implements Listener {
    
    private final JavaPlugin plugin;
    private final Map<String, Long> protectedBlocks = new HashMap<>();
    private final ReinforcedBlockManager reinforcedBlockManager;

    public enum ExplosionType {
        DEFAULT,
        BUNKER_BUSTER,
        HORIZONTAL_SPREAD,
        POWERFUL,
        NUCLEAR
    }

    public ExplosionHandler(JavaPlugin plugin, ReinforcedBlockManager reinforcedBlockManager) {
        this.plugin = plugin;
        this.reinforcedBlockManager = reinforcedBlockManager;
    }

    public void handleExplosion(Location loc, String explosionTypeStr) {
        ExplosionType explosionType = parseExplosionType(explosionTypeStr);
        
        switch (explosionType) {
            case DEFAULT:
                placePrimedTNT(loc);
                break;
            case BUNKER_BUSTER:
                createBunkerBusterExplosion(loc);
                break;
            case HORIZONTAL_SPREAD:
                createHorizontalSpreadExplosion(loc);
                break;
            case POWERFUL:
                createPowerfulExplosion(loc);
                break;
            case NUCLEAR:
                createNuclearExplosion(loc);
                break;
        }
    }

    private ExplosionType parseExplosionType(String explosionTypeStr) {
        if (explosionTypeStr == null) return ExplosionType.DEFAULT;
        try {
            return ExplosionType.valueOf(explosionTypeStr.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Unknown explosion type '" + explosionTypeStr + "', using DEFAULT");
            return ExplosionType.DEFAULT;
        }
    }

    private void placePrimedTNT(Location loc) {
        Location snap = loc.clone();
        snap.setX(Math.floor(snap.getX()) + 0.5);
        snap.setY(Math.floor(snap.getY()));
        snap.setZ(Math.floor(snap.getZ()) + 0.5);

        snap.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, snap, 1);
        TNTPrimed tnt = snap.getWorld().spawn(snap, TNTPrimed.class);
        tnt.setFuseTicks(1);
    }

    private void createBunkerBusterExplosion(Location loc) {
        final Location impactPoint = loc.clone();
        impactPoint.getWorld().createExplosion(impactPoint, 4.0f);
        
        new BukkitRunnable() {
            int depth = 1;
            final int maxDepth = 15;
            
            @Override
            public void run() {
                if (depth > maxDepth) {
                    cancel();
                    return;
                }
                
                Location drillLoc = impactPoint.clone().subtract(0, depth, 0);
                
                drillLoc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, drillLoc, 5, 0.8, 0.8, 0.8, 0.2);
                drillLoc.getWorld().spawnParticle(Particle.BLOCK_CRACK, drillLoc, 25, 1.5, 1.5, 1.5, 0.4, Material.STONE.createBlockData());
                drillLoc.getWorld().spawnParticle(Particle.SMOKE_LARGE, drillLoc, 12, 1.0, 1.0, 1.0, 0.2);
                
                float explosionPower = 5.0f + (depth * 0.3f);
                drillLoc.getWorld().createExplosion(drillLoc, explosionPower);
                playRocketSound(drillLoc, Sound.ENTITY_GENERIC_EXPLODE, 2.8f, 0.6f + (depth * 0.02f), 70.0);
                
                depth += 1;
            }
        }.runTaskTimer(plugin, 8L, 4L);
    }

    private void createHorizontalSpreadExplosion(Location loc) {
        final Location centerPoint = loc.clone();
        centerPoint.getWorld().createExplosion(centerPoint, 4.5f);
        
        new BukkitRunnable() {
            int currentWave = 1;
            
            @Override
            public void run() {
                if (currentWave > 4) {
                    playRocketSound(centerPoint, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.8f, 100.0);
                    playRocketSound(centerPoint, Sound.ENTITY_WITHER_BREAK_BLOCK, 2.0f, 0.6f, 80.0);
                    cancel();
                    return;
                }
                
                double radius = currentWave * 3.5;
                int explosions = currentWave * 8;
                
                for (int i = 0; i < explosions; i++) {
                    double angle = (2.0 * Math.PI * i) / explosions;
                    double x = centerPoint.getX() + radius * Math.cos(angle);
                    double z = centerPoint.getZ() + radius * Math.sin(angle);
                    
                    Location spreadLoc = new Location(centerPoint.getWorld(), x, centerPoint.getY(), z);
                    Location groundLoc = spreadLoc.clone();
                    
                    while (groundLoc.getY() > centerPoint.getY() - 4 && groundLoc.getBlock().getType().isAir()) {
                        groundLoc.subtract(0, 1, 0);
                    }
                    groundLoc.add(0, 1, 0);
                    
                    groundLoc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, groundLoc, 3, 0.8, 0.8, 0.8, 0.2);
                    groundLoc.getWorld().spawnParticle(Particle.LAVA, groundLoc, 12, 1.5, 0.8, 1.5, 0.2);
                    
                    float power = 3.5f + (currentWave * 0.3f);
                    groundLoc.getWorld().createExplosion(groundLoc, power);
                }
                
                currentWave++;
            }
        }.runTaskTimer(plugin, 5L, 20L); // 20 ticks (1 second) between waves - longer gaps for protection
    }

    private void createPowerfulExplosion(Location loc) {
        Location snap = loc.clone();
        snap.setX(Math.floor(snap.getX()) + 0.5);
        snap.setY(Math.floor(snap.getY()));
        snap.setZ(Math.floor(snap.getZ()) + 0.5);

        snap.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, snap, 8, 3.0, 3.0, 3.0, 0.5);
        snap.getWorld().spawnParticle(Particle.LAVA, snap, 40, 4.0, 3.0, 4.0, 0.3);
        snap.getWorld().spawnParticle(Particle.FLAME, snap, 60, 3.5, 2.0, 3.5, 0.4);
        
        snap.getWorld().createExplosion(snap, 8.0f);
        
        new BukkitRunnable() {
            int count = 0;
            final int maxExplosions = 15;
            
            @Override
            public void run() {
                if (count >= maxExplosions) {
                    cancel();
                    return;
                }
                
                double offsetX = ThreadLocalRandom.current().nextGaussian() * 6.0;
                double offsetY = ThreadLocalRandom.current().nextGaussian() * 3.0;
                double offsetZ = ThreadLocalRandom.current().nextGaussian() * 6.0;
                
                Location explosionLoc = snap.clone().add(offsetX, offsetY, offsetZ);
                explosionLoc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, explosionLoc, 5, 1.5, 1.5, 1.5, 0.3);
                explosionLoc.getWorld().createExplosion(explosionLoc, 5.5f);
                
                for (int i = 0; i < 2; i++) {
                    double magmaX = explosionLoc.getX() + ThreadLocalRandom.current().nextGaussian() * 2.0;
                    double magmaZ = explosionLoc.getZ() + ThreadLocalRandom.current().nextGaussian() * 2.0;
                    
                    Location magmaLoc = new Location(explosionLoc.getWorld(), magmaX, explosionLoc.getY(), magmaZ);
                    
                    while (magmaLoc.getY() > explosionLoc.getY() - 8 && magmaLoc.getBlock().getType().isAir()) {
                        magmaLoc.subtract(0, 1, 0);
                    }
                    
                    for (int layer = 0; layer < 2; layer++) {
                        Location fillLoc = magmaLoc.clone().add(0, layer + 1, 0);
                        if (fillLoc.getBlock().getType().isAir()) {
                            fillLoc.getBlock().setType(Material.MAGMA_BLOCK);
                        }
                    }
                }
                
                count++;
            }
        }.runTaskTimer(plugin, 2L, 2L);
        
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 100;
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }
                
                for (int i = 0; i < 3; i++) {
                    double magmaX = snap.getX() + ThreadLocalRandom.current().nextGaussian() * 4.0;
                    double magmaZ = snap.getZ() + ThreadLocalRandom.current().nextGaussian() * 4.0;
                    
                    Location magmaLoc = new Location(snap.getWorld(), magmaX, snap.getY(), magmaZ);
                    
                    while (magmaLoc.getY() > snap.getY() - 10 && magmaLoc.getBlock().getType().isAir()) {
                        magmaLoc.subtract(0, 1, 0);
                    }
                    
                    int layers = ThreadLocalRandom.current().nextInt(2) + 1;
                    for (int layer = 0; layer < layers; layer++) {
                        Location fillLoc = magmaLoc.clone().add(0, layer + 1, 0);
                        if (fillLoc.getBlock().getType().isAir()) {
                            fillLoc.getBlock().setType(Material.MAGMA_BLOCK);
                            fillLoc.getWorld().spawnParticle(Particle.LAVA, fillLoc, 2, 0.3, 0.3, 0.3, 0.1);
                        }
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 10L, 1L);
        
        playRocketSound(snap, Sound.ENTITY_WITHER_SPAWN, 2.5f, 0.5f, ((gg.doomsday.core.DoomsdayCore) plugin).getConfigManager().getRocketsConfig().getDouble("soundRadius", 100.0));
        playRocketSound(snap, Sound.ENTITY_GENERIC_EXPLODE, 3.5f, 0.3f, ((gg.doomsday.core.DoomsdayCore) plugin).getConfigManager().getRocketsConfig().getDouble("soundRadius", 100.0));
        playRocketSound(snap, Sound.BLOCK_LAVA_POP, 2.0f, 0.8f, ((gg.doomsday.core.DoomsdayCore) plugin).getConfigManager().getRocketsConfig().getDouble("soundRadius", 100.0));
    }

    private void createNuclearExplosion(Location loc) {
        final Location ground = loc.clone();
        ground.setY(Math.floor(ground.getY()));
        
        // Initial massive explosion with white flash
        createNuclearFlash(ground);
        
        ground.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, ground, 15, 8.0, 5.0, 8.0, 1.0);
        ground.getWorld().spawnParticle(Particle.FLAME, ground, 100, 8.0, 5.0, 8.0, 0.8);
        ground.getWorld().spawnParticle(Particle.ASH, ground, 200, 10.0, 8.0, 10.0, 0.5);
        ground.getWorld().createExplosion(ground, 12.0f);
        
        // Create horizontal blast waves (larger than horizontal spread)
        new BukkitRunnable() {
            @Override
            public void run() {
                for (int wave = 1; wave <= 6; wave++) {
                    double radius = wave * 5.0;
                    int explosions = wave * 12;
                    
                    for (int i = 0; i < explosions; i++) {
                        double angle = (2.0 * Math.PI * i) / explosions;
                        double x = ground.getX() + radius * Math.cos(angle);
                        double z = ground.getZ() + radius * Math.sin(angle);
                        
                        Location blastLoc = new Location(ground.getWorld(), x, ground.getY(), z);
                        Location groundLoc = blastLoc.clone();
                        
                        while (groundLoc.getY() > ground.getY() - 5 && groundLoc.getBlock().getType().isAir()) {
                            groundLoc.subtract(0, 1, 0);
                        }
                        groundLoc.add(0, 1, 0);
                        
                        groundLoc.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, groundLoc, 5, 1.5, 1.5, 1.5, 0.3);
                        groundLoc.getWorld().spawnParticle(Particle.WHITE_ASH, groundLoc, 20, 2.0, 1.0, 2.0, 0.3);
                        
                        float power = 4.0f + (wave * 0.5f);
                        groundLoc.getWorld().createExplosion(groundLoc, power);
                    }
                }
                
                playRocketSound(ground, Sound.ENTITY_GENERIC_EXPLODE, 4.0f, 0.3f, 200.0);
                playRocketSound(ground, Sound.ENTITY_WITHER_SPAWN, 3.0f, 0.2f, 150.0);
                cancel();
            }
        }.runTaskTimer(plugin, 3L, 1L);
        
        // Create expanding shockwave rings
        createShockwaveRings(ground);
        
        // Smoke rings will be created inside createAnimatedMushroomCloud
        
        // Spread fire to nearby areas
        createFireSpread(ground);
        
        // Create massive crater
        createNuclearCrater(ground);
        
        // Create static mushroom cloud
        createStaticMushroomCloud(ground);
        
        // Nuclear flash effect
        playRocketSound(ground, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 4.0f, 0.8f, 300.0);
        playRocketSound(ground, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 3.5f, 0.5f, 250.0);
    }
    
    private void createNuclearFlash(Location center) {
        int flashRadius = ((gg.doomsday.core.DoomsdayCore) plugin).getConfigManager().getNuclearConfig().getInt("nuclear.effects.flashRadius", 100);
        int headShakeRadius = ((gg.doomsday.core.DoomsdayCore) plugin).getConfigManager().getNuclearConfig().getInt("nuclear.effects.headShakeRadius", 60);
        
        // Create blinding white flash for nearby players
        for (Player player : center.getWorld().getPlayers()) {
            double distance = player.getLocation().distance(center);
            if (distance <= flashRadius) {
                // Flash intensity based on distance
                double intensity = 1.0 - (distance / flashRadius);
                int flashDuration = (int) (60 * intensity); // 0-3 seconds
                
                // Add head shake effect for nearby players
                if (distance <= headShakeRadius) {
                    createHeadShakeEffect(player, center, distance, headShakeRadius);
                }
                
                // Create white flash effect with glowing and particles (avoid blindness as it's black)
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.GLOWING, flashDuration, 2));
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.NIGHT_VISION, flashDuration, 2));
                
                // Massive white particle explosion around player (no glass blocks)
                Location playerLoc = player.getLocation().add(0, 1, 0);
                player.getWorld().spawnParticle(Particle.END_ROD, playerLoc, 
                    (int)(120 * intensity), 6, 4, 6, 0.6);
                player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, playerLoc, 
                    (int)(100 * intensity), 5, 3, 5, 1.0);
                player.getWorld().spawnParticle(Particle.FLASH, playerLoc, 
                    (int)(30 * intensity), 4, 3, 4, 0.2);
                
                // Add more intense white particles instead of glass blocks
                player.getWorld().spawnParticle(Particle.CRIT, playerLoc, 
                    (int)(60 * intensity), 4, 2, 4, 0.8);
                player.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, playerLoc, 
                    (int)(40 * intensity), 3, 2, 3, 0.5);
                
                // Flash sound based on distance
                float volume = (float) Math.max(0.3, intensity * 2.0);
                player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, volume, 1.5f);
                
                // Send flash message
                if (intensity > 0.7) {
                    player.sendMessage("§f§l⚡ NUCLEAR FLASH! ⚡");
                } else if (intensity > 0.4) {
                    player.sendMessage("§7⚡ Blinding flash in the distance...");
                }
            }
        }
    }
    
    private void createHeadShakeEffect(Player player, Location explosionCenter, double distance, int maxRadius) {
        double shakeIntensity = 1.0 - (distance / maxRadius);
        int shakeDuration = 4; // Fixed 0.2 seconds (4 ticks)
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= shakeDuration || !player.isOnline()) {
                    cancel();
                    return;
                }
                
                // Create random head shake by adjusting player's look direction
                Location currentLook = player.getLocation();
                
                // Random shake values - stronger for closer players
                float shakePitch = (float) ((Math.random() - 0.5) * 6 * shakeIntensity);
                float shakeYaw = (float) ((Math.random() - 0.5) * 8 * shakeIntensity);
                
                // Apply shake
                currentLook.setPitch(currentLook.getPitch() + shakePitch);
                currentLook.setYaw(currentLook.getYaw() + shakeYaw);
                
                player.teleport(currentLook);
                
                ticks++;
            }
        }.runTaskTimer(plugin, 1L, 2L); // Start immediately, shake every 2 ticks
    }
    
    
    private void createShockwaveRings(Location center) {
        int shockwaveRadius = ((gg.doomsday.core.DoomsdayCore) plugin).getConfigManager().getNuclearConfig().getInt("nuclear.effects.shockwaveRadius", 80);
        
        new BukkitRunnable() {
            double currentRadius = 5.0;
            int tick = 0;
            
            @Override
            public void run() {
                if (currentRadius > shockwaveRadius) {
                    cancel();
                    return;
                }
                
                // Create shockwave ring with particles
                for (int angle = 0; angle < 360; angle += 10) {
                    double radians = Math.toRadians(angle);
                    double x = currentRadius * Math.cos(radians);
                    double z = currentRadius * Math.sin(radians);
                    
                    Location ringLoc = center.clone().add(x, 2, z);
                    
                    // Multiple particle types for dramatic effect
                    center.getWorld().spawnParticle(Particle.SMOKE_LARGE, ringLoc, 3, 0.5, 1.0, 0.5, 0.1);
                    center.getWorld().spawnParticle(Particle.CLOUD, ringLoc, 2, 0.3, 0.5, 0.3, 0.2);
                    
                    // Add dust clouds at ground level
                    Location groundRing = ringLoc.clone().subtract(0, 1, 0);
                    center.getWorld().spawnParticle(Particle.SMOKE_NORMAL, groundRing, 2, 0.2, 0.2, 0.2, 0.05);
                }
                
                // Play shockwave sound to nearby players
                if (tick % 5 == 0) { // Every 0.25 seconds
                    for (Player player : center.getWorld().getPlayers()) {
                        double distance = player.getLocation().distance(center);
                        if (distance <= currentRadius + 10 && distance >= currentRadius - 5) {
                            float volume = (float) Math.max(0.2, 1.0 - distance / shockwaveRadius);
                            float pitch = (float) (0.5 + (currentRadius / shockwaveRadius) * 0.8);
                            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, volume, pitch);
                            player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, volume * 0.7f, 0.3f);
                            
                            // Knockback effect for close players
                            if (distance < currentRadius + 5) {
                                org.bukkit.util.Vector direction = player.getLocation().toVector()
                                    .subtract(center.toVector()).normalize();
                                double knockbackStrength = Math.max(0.1, 1.5 - distance / 50);
                                direction.multiply(knockbackStrength);
                                direction.setY(Math.max(0.2, direction.getY())); // Slight upward push
                                player.setVelocity(direction);
                            }
                        }
                    }
                }
                
                currentRadius += 2.5; // Ring expansion speed
                tick++;
            }
        }.runTaskTimer(plugin, 3L, 1L); // Start after initial explosion
    }
    
    private void createFireSpread(Location center) {
        int fireRadius = ((gg.doomsday.core.DoomsdayCore) plugin).getConfigManager().getNuclearConfig().getInt("nuclear.effects.fireSpreadRadius", 35);
        
        // Enhanced fire spread with more fires
        new BukkitRunnable() {
            int fires = 0;
            final int maxFires = 300; // Increased from 150 to 300 fires
            
            @Override
            public void run() {
                if (fires >= maxFires) {
                    cancel();
                    return;
                }
                
                // Spread more fires in batches
                for (int i = 0; i < 12 && fires < maxFires; i++) { // Increased batch size
                    double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
                    double radius = ThreadLocalRandom.current().nextDouble() * fireRadius;
                    
                    // Even more bias towards center for intense fire zone
                    radius = Math.pow(radius / fireRadius, 0.4) * fireRadius;
                    
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);
                    
                    Location fireLoc = center.clone().add(x, 0, z);
                    
                    // Find ground level
                    while (fireLoc.getY() > center.getY() - 10 && fireLoc.getBlock().getType().isAir()) {
                        fireLoc.subtract(0, 1, 0);
                    }
                    fireLoc.add(0, 1, 0); // One block above ground
                    
                    // Only set fire if there's a solid block below and air above
                    if (!fireLoc.getBlock().getType().isAir() || 
                        fireLoc.clone().subtract(0, 1, 0).getBlock().getType().isAir()) {
                        continue;
                    }
                    
                    // Set fire with random duration
                    fireLoc.getBlock().setType(Material.FIRE);
                    
                    // Add fire particles for immediate visual effect
                    fireLoc.getWorld().spawnParticle(Particle.FLAME, fireLoc, 8, 0.4, 0.4, 0.4, 0.15);
                    fireLoc.getWorld().spawnParticle(Particle.SMOKE_NORMAL, fireLoc, 5, 0.3, 0.6, 0.3, 0.08);
                    fireLoc.getWorld().spawnParticle(Particle.LAVA, fireLoc, 2, 0.2, 0.2, 0.2, 0.05);
                    
                    fires++;
                }
            }
        }.runTaskTimer(plugin, 10L, 1L); // Faster fire spread - every tick instead of every 2 ticks
        
        // Add post-explosion debris and blocks
        createPostExplosionDebris(center, fireRadius);
    }
    
    private void createPostExplosionDebris(Location center, int radius) {
        // Create scattered debris blocks after explosion
        new BukkitRunnable() {
            int blocksPlaced = 0;
            final int maxBlocks = 200; // Total debris blocks
            
            @Override
            public void run() {
                if (blocksPlaced >= maxBlocks) {
                    cancel();
                    return;
                }
                
                // Place debris blocks in batches
                for (int i = 0; i < 6 && blocksPlaced < maxBlocks; i++) {
                    double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
                    double debrisRadius = ThreadLocalRandom.current().nextDouble() * radius * 1.2; // Slightly beyond fire radius
                    
                    // Bias towards center for more debris near ground zero
                    debrisRadius = Math.pow(debrisRadius / (radius * 1.2), 0.5) * radius * 1.2;
                    
                    double x = debrisRadius * Math.cos(angle);
                    double z = debrisRadius * Math.sin(angle);
                    
                    Location debrisLoc = center.clone().add(x, 0, z);
                    
                    // Find ground level
                    while (debrisLoc.getY() > center.getY() - 10 && debrisLoc.getBlock().getType().isAir()) {
                        debrisLoc.subtract(0, 1, 0);
                    }
                    
                    // Place debris on ground or replace existing blocks
                    Material currentBlock = debrisLoc.getBlock().getType();
                    if (currentBlock == Material.BEDROCK) continue; // Don't replace bedrock
                    
                    // Choose random debris material based on distance from center
                    Material debrisMaterial = chooseDebrisMaterial(debrisRadius, radius);
                    
                    // Sometimes stack debris vertically
                    int stackHeight = ThreadLocalRandom.current().nextInt(3) + 1; // 1-3 blocks high
                    for (int height = 0; height < stackHeight; height++) {
                        Location stackLoc = debrisLoc.clone().add(0, height, 0);
                        if (stackLoc.getBlock().getType().isAir() || 
                            (stackLoc.getBlock().getType() != Material.BEDROCK && height == 0)) {
                            stackLoc.getBlock().setType(debrisMaterial);
                            
                            // Add particles for debris placement
                            if (height == stackHeight - 1) { // Only on top block
                                stackLoc.getWorld().spawnParticle(Particle.BLOCK_CRACK, stackLoc.add(0.5, 1, 0.5), 
                                    3, 0.3, 0.3, 0.3, 0.1, debrisMaterial.createBlockData());
                            }
                        }
                    }
                    
                    blocksPlaced++;
                }
            }
            
            private Material chooseDebrisMaterial(double distanceFromCenter, int maxRadius) {
                double distanceRatio = distanceFromCenter / maxRadius;
                double random = ThreadLocalRandom.current().nextDouble();
                
                if (distanceRatio < 0.3) { // Close to center - more intense materials
                    if (random < 0.4) return Material.MAGMA_BLOCK;
                    else if (random < 0.7) return Material.BLACKSTONE;
                    else if (random < 0.85) return Material.OBSIDIAN;
                    else return Material.NETHERRACK;
                } else if (distanceRatio < 0.6) { // Medium distance - mixed debris
                    if (random < 0.3) return Material.MAGMA_BLOCK;
                    else if (random < 0.5) return Material.BLACKSTONE;
                    else if (random < 0.7) return Material.COBBLESTONE;
                    else if (random < 0.85) return Material.ANDESITE;
                    else return Material.GRAVEL;
                } else { // Far from center - lighter debris
                    if (random < 0.2) return Material.COBBLESTONE;
                    else if (random < 0.4) return Material.GRAVEL;
                    else if (random < 0.6) return Material.ANDESITE;
                    else if (random < 0.8) return Material.DIORITE;
                    else return Material.GRANITE;
                }
            }
        }.runTaskTimer(plugin, 15L, 3L); // Start after 0.75 seconds, place debris every 3 ticks
        
        // Create magma pools and hot spots
        createMagmaPools(center, radius);
    }
    
    private void createMagmaPools(Location center, int radius) {
        // Create magma pools and lava spots
        new BukkitRunnable() {
            int poolsCreated = 0;
            final int maxPools = 15; // Total magma pools
            
            @Override
            public void run() {
                if (poolsCreated >= maxPools) {
                    cancel();
                    return;
                }
                
                double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
                double poolRadius = ThreadLocalRandom.current().nextDouble() * radius * 0.8; // Within 80% of fire radius
                
                // Strong bias towards center for magma pools
                poolRadius = Math.pow(poolRadius / (radius * 0.8), 0.3) * radius * 0.8;
                
                double x = poolRadius * Math.cos(angle);
                double z = poolRadius * Math.sin(angle);
                
                Location poolCenter = center.clone().add(x, 0, z);
                
                // Find ground level
                while (poolCenter.getY() > center.getY() - 10 && poolCenter.getBlock().getType().isAir()) {
                    poolCenter.subtract(0, 1, 0);
                }
                
                // Create small magma/lava pool
                int poolSize = ThreadLocalRandom.current().nextInt(3) + 2; // 2-4 block radius
                for (int px = -poolSize; px <= poolSize; px++) {
                    for (int pz = -poolSize; pz <= poolSize; pz++) {
                        if (Math.sqrt(px*px + pz*pz) <= poolSize) {
                            Location poolLoc = poolCenter.clone().add(px, 0, pz);
                            
                            if (poolLoc.getBlock().getType() != Material.BEDROCK) {
                                // Center of pool gets lava, edges get magma blocks
                                if (Math.sqrt(px*px + pz*pz) <= poolSize * 0.4) {
                                    poolLoc.getBlock().setType(Material.LAVA);
                                } else {
                                    poolLoc.getBlock().setType(Material.MAGMA_BLOCK);
                                }
                                
                                // Add particles above magma pools
                                if (ThreadLocalRandom.current().nextDouble() < 0.3) {
                                    Location particleLoc = poolLoc.clone().add(0.5, 1.5, 0.5);
                                    poolLoc.getWorld().spawnParticle(Particle.LAVA, particleLoc, 2, 0.3, 0.3, 0.3, 0.1);
                                    poolLoc.getWorld().spawnParticle(Particle.SMOKE_NORMAL, particleLoc, 1, 0.2, 0.2, 0.2, 0.05);
                                }
                            }
                        }
                    }
                }
                
                poolsCreated++;
            }
        }.runTaskTimer(plugin, 20L, 8L); // Start after 1 second, create pools every 8 ticks
    }
    
    private void createNuclearCrater(Location center) {
        int craterDepth = ((gg.doomsday.core.DoomsdayCore) plugin).getConfigManager().getNuclearConfig().getInt("nuclear.crater.depth", 15);
        int craterRadius = ((gg.doomsday.core.DoomsdayCore) plugin).getConfigManager().getNuclearConfig().getInt("nuclear.crater.radius", 25);
        
        // Create crater using multiple overlapping explosions in a bowl formation
        new BukkitRunnable() {
            int phase = 0;
            final int maxPhases = 4;
            
            @Override
            public void run() {
                if (phase >= maxPhases) {
                    // Add lava pools at bottom when finished
                    addLavaPools(center, craterRadius, craterDepth);
                    cancel();
                    return;
                }
                
                // Create overlapping explosion pattern based on phase
                switch (phase) {
                    case 0:
                        // Center explosion - deepest
                        createCraterExplosions(center, 0, 8, craterDepth, 8.0f);
                        break;
                    case 1:
                        // Inner ring explosions
                        createCraterExplosions(center, 8, 16, (int)(craterDepth * 0.7), 6.0f);
                        break;
                    case 2:
                        // Middle ring explosions
                        createCraterExplosions(center, 16, 22, (int)(craterDepth * 0.4), 4.5f);
                        break;
                    case 3:
                        // Outer ring explosions
                        createCraterExplosions(center, 22, craterRadius, (int)(craterDepth * 0.2), 3.0f);
                        break;
                }
                
                phase++;
            }
            
            private void createCraterExplosions(Location centerLoc, int innerRadius, int outerRadius, int explosionDepth, float power) {
                int explosionCount = Math.max(6, (outerRadius - innerRadius) * 2);
                
                for (int i = 0; i < explosionCount; i++) {
                    double angle = (2.0 * Math.PI * i) / explosionCount;
                    double radius = innerRadius + ThreadLocalRandom.current().nextDouble() * (outerRadius - innerRadius);
                    
                    double x = centerLoc.getX() + radius * Math.cos(angle);
                    double z = centerLoc.getZ() + radius * Math.sin(angle);
                    
                    // Calculate explosion depth based on distance from center
                    double centerDistance = Math.sqrt(Math.pow(x - centerLoc.getX(), 2) + Math.pow(z - centerLoc.getZ(), 2));
                    double depthRatio = Math.max(0.1, 1.0 - (centerDistance / craterRadius));
                    int actualDepth = (int) (explosionDepth * Math.pow(depthRatio, 0.8));
                    
                    // Create explosions at multiple depths for bowl effect
                    for (int depth = 0; depth >= -actualDepth; depth -= 3) {
                        Location explosionLoc = new Location(centerLoc.getWorld(), x, centerLoc.getY() + depth, z);
                        
                        // Smaller explosions at greater depth
                        float depthPower = power * (1.0f - Math.abs(depth) / (float)explosionDepth * 0.5f);
                        explosionLoc.getWorld().createExplosion(explosionLoc, Math.max(2.0f, depthPower), false, true);
                    }
                }
            }
            
            private void addLavaPools(Location centerLoc, int radius, int depth) {
                // Add several lava pools at different depths in the crater
                for (int i = 0; i < 5; i++) {
                    double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
                    double poolRadius = ThreadLocalRandom.current().nextDouble() * (radius * 0.4);
                    
                    int x = (int) (poolRadius * Math.cos(angle));
                    int z = (int) (poolRadius * Math.sin(angle));
                    
                    // Place lava at calculated depth for this location
                    double centerDistance = Math.sqrt(x*x + z*z);
                    double depthRatio = Math.max(0.1, 1.0 - (centerDistance / radius));
                    int lavaDepth = (int) (depth * Math.pow(depthRatio, 0.8)) - 2;
                    
                    Location lavaLoc = centerLoc.clone().add(x, -lavaDepth, z);
                    
                    // Create small lava pool
                    for (int lx = -1; lx <= 1; lx++) {
                        for (int lz = -1; lz <= 1; lz++) {
                            Location poolLoc = lavaLoc.clone().add(lx, 0, lz);
                            if (poolLoc.getBlock().getType().isAir() || poolLoc.getBlock().getType() == Material.STONE) {
                                poolLoc.getBlock().setType(Material.LAVA);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 15L, 8L); // Start after 0.75 seconds, 8 ticks between phases
    }
    
    private void createStaticMushroomCloud(Location center) {
        // Get config values
        int cloudDuration = ((gg.doomsday.core.DoomsdayCore) plugin).getConfigManager().getNuclearConfig().getInt("nuclear.cloudDuration", 15);
        int particleDuration = ((gg.doomsday.core.DoomsdayCore) plugin).getConfigManager().getNuclearConfig().getInt("nuclear.particleDuration", 6);
        int baseRadius = ((gg.doomsday.core.DoomsdayCore) plugin).getConfigManager().getNuclearConfig().getInt("nuclear.cloud.baseRadius", 20);
        int stemHeight = ((gg.doomsday.core.DoomsdayCore) plugin).getConfigManager().getNuclearConfig().getInt("nuclear.cloud.stemHeight", 45);
        int capHeight = ((gg.doomsday.core.DoomsdayCore) plugin).getConfigManager().getNuclearConfig().getInt("nuclear.cloud.capHeight", 35);
        double cloudDensity = ((gg.doomsday.core.DoomsdayCore) plugin).getConfigManager().getNuclearConfig().getDouble("nuclear.cloud.density", 0.3);
        int innerRadius = ((gg.doomsday.core.DoomsdayCore) plugin).getConfigManager().getNuclearConfig().getInt("nuclear.rings.innerRadius", 25);
        int outerRadius = ((gg.doomsday.core.DoomsdayCore) plugin).getConfigManager().getNuclearConfig().getInt("nuclear.rings.outerRadius", 45);
        double ringDensity = ((gg.doomsday.core.DoomsdayCore) plugin).getConfigManager().getNuclearConfig().getDouble("nuclear.rings.density", 0.5);
        
        java.util.List<BlockDisplay> allBlocks = new java.util.ArrayList<>();
        BlockData whiteWool = Material.WHITE_WOOL.createBlockData();
        
        // Create ground layer (much wider base)
        for (int y = -2; y <= 6; y++) {
            double layerRadius = (baseRadius * 1.8) - Math.abs(y) * 1.5; // Much wider base
            for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 16) {
                for (double r = layerRadius - 8; r <= layerRadius; r += 1.0) {
                    if (ThreadLocalRandom.current().nextDouble() > 0.8) continue;
                    
                    double x = r * Math.cos(angle);
                    double z = r * Math.sin(angle);
                    x += (ThreadLocalRandom.current().nextDouble() - 0.5) * 2.0;
                    z += (ThreadLocalRandom.current().nextDouble() - 0.5) * 2.0;
                    
                    Location loc = center.clone().add(x, y, z);
                    createCloudBlock(loc, allBlocks, whiteWool, 1.0f);
                }
            }
        }
        
        // Create stem (wider to support fat mushroom cap)
        for (int y = 6; y <= stemHeight; y += 1) {
            double stemRadius = 3.0 + (y / (double)stemHeight) * (baseRadius * 0.5); // Wider stem
            for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 12) {
                for (double r = Math.max(0, stemRadius - 2.0); r <= stemRadius; r += 0.8) {
                    if (ThreadLocalRandom.current().nextDouble() > 0.7) continue;
                    
                    double x = r * Math.cos(angle);
                    double z = r * Math.sin(angle);
                    x += (ThreadLocalRandom.current().nextDouble() - 0.5) * 1.0;
                    z += (ThreadLocalRandom.current().nextDouble() - 0.5) * 1.0;
                    
                    Location loc = center.clone().add(x, y, z);
                    createCloudBlock(loc, allBlocks, whiteWool, 1.0f);
                }
            }
        }
        
        // Create cap (much fatter mushroom shape)
        int capStart = stemHeight - 8;
        for (int y = capStart; y <= capStart + capHeight; y += 1) {
            double progress = (double)(y - capStart) / capHeight;
            // Much fatter mushroom shape - massive bulbous cap
            double capRadius;
            if (progress < 0.2) {
                // Bottom of cap - connection to stem
                capRadius = baseRadius * (0.6 + progress * 1.0);
            } else if (progress < 0.6) {
                // Middle of cap - extremely wide bulbous part
                capRadius = baseRadius * (1.6 + (progress - 0.2) * 1.5); // Much wider
            } else if (progress < 0.8) {
                // Upper middle - still very wide
                capRadius = baseRadius * (2.2 + (progress - 0.6) * 0.3);
            } else {
                // Top of cap - tapers off but still wide
                capRadius = baseRadius * (2.5 - (progress - 0.8) * 1.2);
            }
            
            for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 18) {
                for (double r = Math.max(0, capRadius - 6); r <= capRadius; r += 1.0) {
                    if (ThreadLocalRandom.current().nextDouble() > 0.75) continue;
                    
                    double x = r * Math.cos(angle);
                    double z = r * Math.sin(angle);
                    x += (ThreadLocalRandom.current().nextDouble() - 0.5) * 2.5;
                    z += (ThreadLocalRandom.current().nextDouble() - 0.5) * 2.5;
                    
                    Location loc = center.clone().add(x, y, z);
                    createCloudBlock(loc, allBlocks, whiteWool, 1.0f);
                }
            }
        }
        
        // Create single ring around the stem (scaled for wider cloud)
        double stemRingRadius = 18; // Larger radius for wider cloud
        int stemRingHeight = 20; // Positioned around mid-stem
        
        for (int angle = 0; angle < 360; angle += (int)(3 / ringDensity)) {
            double radians = Math.toRadians(angle);
            double x = stemRingRadius * Math.cos(radians);
            double z = stemRingRadius * Math.sin(radians);
            
            Location loc = center.clone().add(x, stemRingHeight, z);
            createCloudBlock(loc, allBlocks, whiteWool, 1.0f);
        }
        
        // Handle cleanup and particles
        final int cloudTicks = cloudDuration * 20;
        final int particleTicks = particleDuration * 20;
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= cloudTicks) {
                    // Clean up all blocks
                    for (BlockDisplay block : allBlocks) {
                        if (block.isValid() && !block.isDead()) {
                            block.remove();
                        }
                    }
                    cancel();
                    return;
                }
                
                // Black particle effects for configured duration
                if (ticks < particleTicks) {
                    addBlackParticles(center, ticks, particleTicks);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 20L, 1L); // Start after 1 second, run every tick
    }
    
    private void createCloudBlock(Location loc, java.util.List<BlockDisplay> blockList, BlockData blockType, float scale) {
        BlockDisplay cloud = loc.getWorld().spawn(loc, BlockDisplay.class, display -> {
            display.setBlock(blockType);
            
            // Use the provided scale directly (should be 1.0f for normal size)
            float finalScale = scale + ThreadLocalRandom.current().nextFloat() * 0.1f;
            Transformation transform = new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(finalScale, finalScale, finalScale),
                new AxisAngle4f(0, 0, 0, 1)
            );
            display.setTransformation(transform);
        });
        
        blockList.add(cloud);
    }
    
    private void createFullMushroomCloud(Location center, java.util.List<BlockDisplay> cloudBlocks) {
        BlockData whiteWool = Material.WHITE_WOOL.createBlockData();
        
        // Create denser ground layer - more blocks for better appearance
        for (int y = -2; y <= 4; y++) {
            double baseRadius = 20 - Math.abs(y) * 1.0;
            for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 12) { // More angles for density
                // Create more blocks across the radius for denser appearance
                for (double r = baseRadius - 5; r <= baseRadius; r += 1.0) { // Wider shell, smaller gaps
                    if (ThreadLocalRandom.current().nextDouble() < 0.15) continue; // Less skipping
                    
                    double x = r * Math.cos(angle);
                    double z = r * Math.sin(angle);
                    
                    x += (ThreadLocalRandom.current().nextDouble() - 0.5) * 1.5;
                    z += (ThreadLocalRandom.current().nextDouble() - 0.5) * 1.5;
                    
                    Location groundLoc = center.clone().add(x, y, z);
                    spawnCloudBlock(groundLoc, cloudBlocks, whiteWool, 0.3f);
                }
            }
        }
        
        // Create denser stem - more blocks for better appearance
        for (int y = 6; y <= 45; y += 2) {
            double stemRadius;
            if (y < 15) {
                stemRadius = 3.0;
            } else if (y < 25) {
                stemRadius = 3.5 + (y - 15) * 0.2;
            } else if (y < 35) {
                stemRadius = 5.5 + (y - 25) * 0.4;
            } else {
                stemRadius = 9.5 + (y - 35) * 0.5;
            }
            
            for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 14) { // More angles for density
                // Create more blocks across the radius for denser stem
                for (double r = Math.max(0, stemRadius - 3); r <= stemRadius; r += 0.8) { // Wider shell, smaller gaps
                    // Less hollow - only skip center if very wide
                    if (r < stemRadius - 2.0 && stemRadius > 6.0 && ThreadLocalRandom.current().nextDouble() < 0.6) continue;
                    if (ThreadLocalRandom.current().nextDouble() < 0.05) continue; // Much less skipping
                    
                    double x = r * Math.cos(angle);
                    double z = r * Math.sin(angle);
                    
                    x += (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.6;
                    z += (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.6;
                    
                    Location stemLoc = center.clone().add(x, y, z);
                    spawnCloudBlock(stemLoc, cloudBlocks, whiteWool, 0.3f);
                }
            }
        }
        
        // Create hollow mushroom cap - only outer shell
        for (int y = 35; y <= 70; y += 2) {
            double baseRadius = 15.0;
            double capRadius;
            
            if (y <= 45) {
                capRadius = Math.max(baseRadius - 2, baseRadius + (y - 35) * 0.8);
            } else if (y <= 55) {
                capRadius = baseRadius + 8 + (y - 45) * 1.5;
            } else if (y <= 65) {
                capRadius = baseRadius + 23 + (y - 55) * 0.5;
            } else {
                capRadius = Math.max(10.0, baseRadius + 28 - (y - 65) * 4.0);
            }
            
            for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 16) { // More angles for density
                // Create more blocks across the radius for denser cap
                double minRadius = Math.max(0, capRadius - 6); // Wider shell
                for (double r = minRadius; r <= capRadius; r += 1.2) { // Smaller steps for more blocks
                    // Less hollow - only skip deep interior of large caps
                    if (r < capRadius - 3.0 && capRadius > 12.0) {
                        if (ThreadLocalRandom.current().nextDouble() < 0.5) continue; // Skip only 50% of inner blocks
                    }
                    if (ThreadLocalRandom.current().nextDouble() < 0.05) continue; // Much less skipping
                    
                    double x = r * Math.cos(angle);
                    double z = r * Math.sin(angle);
                    
                    double randomFactor = Math.min(3.0, r * 0.2);
                    x += (ThreadLocalRandom.current().nextDouble() - 0.5) * randomFactor;
                    z += (ThreadLocalRandom.current().nextDouble() - 0.5) * randomFactor;
                    
                    Location capLoc = center.clone().add(x, y, z);
                    spawnCloudBlock(capLoc, cloudBlocks, whiteWool, 0.4f);
                }
            }
        }
        
        // Create hollow transition layer - only outer shell for connection
        for (int y = 40; y <= 48; y += 2) { // Less dense, larger steps
            double transitionRadius = 12.0 + (y - 40) * 1.5;
            
            for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 12) {
                // Only outer shell for hollow transition
                for (double r = Math.max(0, transitionRadius - 2); r <= transitionRadius; r += 1.0) {
                    if (r < transitionRadius - 1.0 && ThreadLocalRandom.current().nextDouble() < 0.7) continue; // Skip inner
                    if (ThreadLocalRandom.current().nextDouble() < 0.05) continue;
                    
                    double x = r * Math.cos(angle);
                    double z = r * Math.sin(angle);
                    
                    x += (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.4;
                    z += (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.4;
                    
                    Location transitionLoc = center.clone().add(x, y, z);
                    spawnCloudBlock(transitionLoc, cloudBlocks, whiteWool, 0.35f);
                }
            }
        }
    }
    
    private void spawnCloudBlock(Location loc, java.util.List<BlockDisplay> cloudBlocks, BlockData blockType, float initialScale) {
        BlockDisplay cloud = loc.getWorld().spawn(loc, BlockDisplay.class, display -> {
            display.setBlock(blockType);
            
            // Start small, will grow during animation
            float scale = initialScale + ThreadLocalRandom.current().nextFloat() * 0.2f;
            Transformation transform = new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f(0, 0, 0, 1)
            );
            display.setTransformation(transform);
        });
        
        cloudBlocks.add(cloud);
    }
    
    private void scaleMushroomCloud(java.util.List<BlockDisplay> cloudBlocks, double growthProgress) {
        // Calculate target scale - grows from 0.2 to 2.0 for massive cloud
        float targetScale = (float) (0.2 + growthProgress * 1.8);
        
        for (BlockDisplay cloud : cloudBlocks) {
            if (cloud.isValid() && !cloud.isDead()) {
                Transformation current = cloud.getTransformation();
                Vector3f currentScale = current.getScale();
                
                // Gradually scale up slower for 30 second growth
                float newScale = Math.min(targetScale, currentScale.x() + 0.005f);
                
                Transformation newTransform = new Transformation(
                    current.getTranslation(),
                    current.getLeftRotation(),
                    new Vector3f(newScale, newScale, newScale),
                    current.getRightRotation()
                );
                
                cloud.setTransformation(newTransform);
            }
        }
    }
    
    
    private void animateExistingClouds(java.util.List<BlockDisplay> cloudBlocks) {
        for (BlockDisplay cloud : cloudBlocks) {
            if (cloud.isValid() && !cloud.isDead()) {
                Transformation current = cloud.getTransformation();
                
                // Gradually grow to full size
                Vector3f currentScale = current.getScale();
                float targetScale = 0.8f + ThreadLocalRandom.current().nextFloat() * 0.4f;
                float newScale = Math.min(targetScale, currentScale.x() + 0.02f);
                
                // Add slight random movement
                Vector3f translation = current.getTranslation();
                float moveX = translation.x() + (ThreadLocalRandom.current().nextFloat() - 0.5f) * 0.1f;
                float moveY = translation.y() + (ThreadLocalRandom.current().nextFloat() - 0.5f) * 0.05f;
                float moveZ = translation.z() + (ThreadLocalRandom.current().nextFloat() - 0.5f) * 0.1f;
                
                Transformation newTransform = new Transformation(
                    new Vector3f(moveX, moveY, moveZ),
                    current.getLeftRotation(),
                    new Vector3f(newScale, newScale, newScale),
                    current.getRightRotation()
                );
                
                cloud.setTransformation(newTransform);
            }
        }
    }
    
    private void fadeCloudBlocks(java.util.List<BlockDisplay> cloudBlocks, double dissipateProgress) {
        java.util.Iterator<BlockDisplay> iterator = cloudBlocks.iterator();
        
        while (iterator.hasNext()) {
            BlockDisplay cloud = iterator.next();
            
            if (!cloud.isValid() || cloud.isDead()) {
                iterator.remove();
                continue;
            }
            
            // Randomly remove blocks during dissipation
            if (ThreadLocalRandom.current().nextDouble() < dissipateProgress * 0.03) {
                cloud.remove();
                iterator.remove();
                continue;
            }
            
            // Shrink remaining blocks
            Transformation current = cloud.getTransformation();
            Vector3f currentScale = current.getScale();
            float shrinkFactor = (float) (1.0 - dissipateProgress * 0.02);
            float newScale = Math.max(0.1f, currentScale.x() * shrinkFactor);
            
            Transformation newTransform = new Transformation(
                current.getTranslation(),
                current.getLeftRotation(),
                new Vector3f(newScale, newScale, newScale),
                current.getRightRotation()
            );
            
            cloud.setTransformation(newTransform);
        }
    }
    
    private void addBlackParticles(Location center, int ticks, int maxTicks) {
        // Calculate particle intensity - more at start, fewer over time
        double intensity = 1.0 - ((double) ticks / maxTicks);
        int particleCount = (int) (15 * intensity) + 3; // 3-18 particles
        
        for (int i = 0; i < particleCount; i++) {
            // Create expanding ring of particles
            double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
            double radius = 8 + (ticks * 0.1) + ThreadLocalRandom.current().nextGaussian() * 3;
            
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            double y = center.getY() + ThreadLocalRandom.current().nextGaussian() * 8; // Spread vertically
            
            Location particleLoc = new Location(center.getWorld(), x, y, z);
            
            // Black smoke particles
            center.getWorld().spawnParticle(Particle.SMOKE_LARGE, particleLoc, 1, 0.3, 0.3, 0.3, 0.02);
            
            // Dense black particles in center
            if (radius < 12) {
                center.getWorld().spawnParticle(Particle.SMOKE_NORMAL, particleLoc, 2, 0.5, 0.5, 0.5, 0.01);
            }
            
            // Ash particles
            if (ThreadLocalRandom.current().nextDouble() < 0.4) {
                center.getWorld().spawnParticle(Particle.ASH, particleLoc, 1, 0.8, 0.8, 0.8, 0.03);
            }
            
            // Occasional ember particles for heat effect
            if (ThreadLocalRandom.current().nextDouble() < 0.2 && ticks < maxTicks / 2) {
                center.getWorld().spawnParticle(Particle.LAVA, particleLoc, 1, 0.3, 0.3, 0.3, 0.01);
            }
        }
        
        // Ground-level smoke rings
        if (ticks % 5 == 0) { // Every 0.25 seconds
            double ringRadius = 5 + (ticks * 0.2);
            for (int i = 0; i < 12; i++) {
                double angle = (i * 2 * Math.PI) / 12;
                double x = center.getX() + ringRadius * Math.cos(angle);
                double z = center.getZ() + ringRadius * Math.sin(angle);
                
                Location ringLoc = new Location(center.getWorld(), x, center.getY() + 1, z);
                center.getWorld().spawnParticle(Particle.SMOKE_LARGE, ringLoc, 2, 0.2, 0.2, 0.2, 0.01);
            }
        }
    }

    private void playRocketSound(Location location, Sound sound, float volume, float pitch, double radius) {
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distance(location) <= radius) {
                player.playSound(location, sound, volume, pitch);
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        plugin.getLogger().info("EntityExplode event triggered! Entity: " + event.getEntity().getType() + " Blocks affected: " + event.blockList().size());
        processExplosion(event.blockList());
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        plugin.getLogger().info("BlockExplode event triggered! Block: " + event.getBlock().getType() + " Blocks affected: " + event.blockList().size());
        processExplosion(event.blockList());
    }

    private void processExplosion(java.util.List<Block> blockList) {
        Iterator<Block> iterator = blockList.iterator();
        long currentTime = System.currentTimeMillis();
        
        while (iterator.hasNext()) {
            Block block = iterator.next();
            Material material = block.getType();
            String blockKey = block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
            
            if (protectedBlocks.containsKey(blockKey)) {
                long protectedUntil = protectedBlocks.get(blockKey);
                if (currentTime < protectedUntil) {
                    iterator.remove();
                    plugin.getLogger().info("Protected block: " + material + " at " + block.getX() + "," + block.getY() + "," + block.getZ());
                    continue;
                }
                protectedBlocks.remove(blockKey);
            }
            
            // Check if block is reinforced and get its resistance
            double resistanceChance = 0.0;
            boolean isReinforced = reinforcedBlockManager.isReinforced(block.getLocation());
            
            if (isReinforced) {
                resistanceChance = reinforcedBlockManager.getResistance(block.getLocation());
            } else {
                // Check for special blocks that always have resistance (obsidian, bedrock)
                resistanceChance = getLegacyBlockResistance(material);
            }
            
            if (resistanceChance > 0.0) {
                double random = ThreadLocalRandom.current().nextDouble();
                if (random < resistanceChance) {
                    // Block survived the explosion
                    iterator.remove();
                    // For reinforced blocks with high resistance, protect them longer during multi-wave explosions
                    long protectionTime = isReinforced && resistanceChance >= 0.8 ? 10000L : 500L; // 10 seconds for high resistance reinforced blocks
                    protectedBlocks.put(blockKey, currentTime + protectionTime - 500L); // Adjust so cleanup works correctly
                    String blockType = isReinforced ? "reinforced " + material.name().toLowerCase() : material.name().toLowerCase();
                    plugin.getLogger().info("Saved " + blockType + " block at " + block.getX() + "," + block.getY() + "," + block.getZ() + " (resistance: " + (resistanceChance*100) + "%, protected for " + (protectionTime/1000) + "s)");
                } else if (isReinforced) {
                    // Reinforced block failed its resistance check - will be destroyed
                    // Remove it from the reinforced blocks list
                    reinforcedBlockManager.removeReinforcement(block.getLocation());
                    plugin.getLogger().info("Reinforced " + material.name().toLowerCase() + " block destroyed by explosion at " + block.getX() + "," + block.getY() + "," + block.getZ() + " (failed " + (resistanceChance*100) + "% resistance check)");
                }
            } else if (isReinforced) {
                // Block was reinforced but had no resistance (shouldn't happen, but just in case)
                reinforcedBlockManager.removeReinforcement(block.getLocation());
                plugin.getLogger().info("Reinforced " + material.name().toLowerCase() + " block destroyed by explosion at " + block.getX() + "," + block.getY() + "," + block.getZ() + " (no resistance configured)");
            }
        }
        
        cleanupOldProtections(currentTime);
    }

    private double getLegacyBlockResistance(Material material) {
        // Only certain special blocks have inherent resistance (not reinforced)
        switch (material) {
            case OBSIDIAN:
                return 0.95;
            case BEDROCK:
                return 1.0;
            default:
                return 0.0;
        }
    }

    private void cleanupOldProtections(long currentTime) {
        protectedBlocks.entrySet().removeIf(entry -> currentTime >= entry.getValue());
    }
}