package gg.doomsday.core.defense;

import gg.doomsday.core.DoomsdayCore;
import gg.doomsday.core.fuel.AntiAirFuelManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.concurrent.ThreadLocalRandom;

public class AntiAirDefense {
    private final Location location;
    private final String name;
    private final String displayName;
    private final double range;
    private final double accuracy;
    private final double interceptorSpeed;
    private final double reloadTime;
    private final double startupTime;
    private final boolean automatic;
    private final JavaPlugin plugin;
    
    private long lastShotTime;
    private boolean operational;

    public AntiAirDefense(JavaPlugin plugin, String name, String displayName, Location location, double range, 
                         double accuracy, double interceptorSpeed, double reloadTime, double startupTime, boolean automatic) {
        this.plugin = plugin;
        this.name = name;
        this.displayName = displayName;
        this.location = location.clone();
        this.range = range;
        this.accuracy = accuracy;
        this.interceptorSpeed = interceptorSpeed;
        this.reloadTime = reloadTime;
        this.startupTime = startupTime;
        this.automatic = automatic;
        this.lastShotTime = 0;
        this.operational = true;
    }

    public boolean canIntercept(Location missileStart, Location missileEnd, double missileSpeed) {
        plugin.getLogger().info("=== " + name + " canIntercept check ===");
        
        if (!operational) {
            plugin.getLogger().info(name + " - FAILED: Not operational");
            return false;
        }

        if (System.currentTimeMillis() - lastShotTime < reloadTime * 1000) {
            plugin.getLogger().info(name + " - FAILED: In reload period");
            return false;
        }

        // Check if manual defense needs a player nearby
        if (!automatic) {
            boolean playerNearby = false;
            double closestPlayerDistance = Double.MAX_VALUE;
            for (Player player : location.getWorld().getPlayers()) {
                double distance = player.getLocation().distance(location);
                if (distance < closestPlayerDistance) {
                    closestPlayerDistance = distance;
                }
                if (distance <= 5.0) { // Increased from 3.0 to 5.0 blocks
                    playerNearby = true;
                    break;
                }
            }
            if (!playerNearby) {
                plugin.getLogger().info("Manual defense '" + name + "' cannot engage - no player nearby (closest: " + String.format("%.1f", closestPlayerDistance) + " blocks)");
                return false; // Manual defense needs operator
            } else {
                plugin.getLogger().info("Manual defense '" + name + "' has operator nearby (" + String.format("%.1f", closestPlayerDistance) + " blocks)");
            }
        }

        double distanceToTarget = location.distance(missileEnd);
        plugin.getLogger().info(name + " - Target distance: " + String.format("%.1f", distanceToTarget) + " (range: " + range + ")");
        if (distanceToTarget > range) {
            plugin.getLogger().info(name + " - FAILED: Target out of range");
            return false;
        }

        // Always attempt interception if within range and operational
        // Let the actual pursuit determine success/failure
        plugin.getLogger().info(name + " - ALL CHECKS PASSED - Will attempt interception");
        
        return true;
    }

    public boolean attemptIntercept(Location missileStart, Location missileEnd, double missileSpeed, BlockDisplay missileEntity) {
        if (!canIntercept(missileStart, missileEnd, missileSpeed)) {
            return false;
        }

        // Check fuel requirements
        DoomsdayCore doomsdayCore = (DoomsdayCore) plugin;
        AntiAirFuelManager fuelManager = doomsdayCore.getAntiAirFuelManager();
        int fuelRequired = fuelManager.getFuelRequirement(name);
        
        if (!fuelManager.hasSufficientFuel(name, fuelRequired)) {
            int currentFuel = fuelManager.getFuel(name);
            plugin.getLogger().info("Anti-air defense '" + name + "' has insufficient fuel (" + currentFuel + "/" + fuelRequired + ")");
            return false;
        }

        plugin.getLogger().info("Anti-air defense '" + name + "' detected threat, preparing to engage...");
        
        // Startup delay - defense needs time to acquire target and launch
        new BukkitRunnable() {
            @Override
            public void run() {
                if (missileEntity == null || missileEntity.isDead()) {
                    plugin.getLogger().info("Anti-air defense '" + name + "' - target lost during startup");
                    return;
                }
                
                // Set reload time when actually firing, not when detecting
                lastShotTime = System.currentTimeMillis();
                
                // Consume fuel when actually firing
                DoomsdayCore doomsdayCore = (DoomsdayCore) plugin;
                AntiAirFuelManager fuelManager = doomsdayCore.getAntiAirFuelManager();
                int fuelRequired = fuelManager.getFuelRequirement(name);
                fuelManager.consumeFuel(name, fuelRequired);
                
                plugin.getLogger().info("Anti-air defense '" + name + "' launching interceptor!");
                
                // Send launch message using configurable messaging system
                String launchMessage = doomsdayCore.getMessageManager().getMessage("antiair.launched", "displayName", displayName);
                doomsdayCore.getMessagingManager().sendAntiAirMessage(launchMessage, location, name);
                
                // Determine if it will hit based on accuracy (but don't tell players yet)
                double hitChance = accuracy;
                boolean willHit = ThreadLocalRandom.current().nextDouble() <= hitChance;
                
                if (willHit) {
                    plugin.getLogger().info("Anti-air defense '" + name + "' interceptor will hit target");
                } else {
                    plugin.getLogger().info("Anti-air defense '" + name + "' interceptor will miss target");
                }
                
                fireInterceptor(missileEntity, willHit);
            }
        }.runTaskLater(plugin, (long)(startupTime * 20)); // Configurable startup time
        
        return false; // Don't immediately stop missile, let interceptor catch it
    }

    private void fireInterceptor(BlockDisplay missileTarget, boolean willHit) {
        playDefenseSound(location, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 2.0f, 1.2f);
        
        Location interceptorStart = location.clone().add(0, 2, 0); // Launch from 2 blocks higher than beacon
        
        BlockData ironBlock = Material.IRON_BLOCK.createBlockData();
        BlockDisplay interceptor = location.getWorld().spawn(interceptorStart, BlockDisplay.class, display -> {
            display.setBlock(ironBlock);
            Transformation transform = new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(0.5f, 0.5f, 0.5f),
                    new AxisAngle4f(0, 0, 0, 1)
            );
            display.setTransformation(transform);
        });

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 200; // Increased max ticks for tracking
            final Location startLoc = interceptorStart.clone();
            double lastDistanceToTarget = Double.MAX_VALUE;
            
            @Override
            public void run() {
                if (interceptor.isDead() || !interceptor.isValid() || ticks >= maxTicks) {
                    if (!interceptor.isDead()) {
                        plugin.getLogger().info("'" + name + "' interceptor timed out - failed to reach target (ticks: " + ticks + "/" + maxTicks + ")");
                        
                        // Send failure message to players using configurable messaging system
                        DoomsdayCore doomsdayCore = (DoomsdayCore) plugin;
                        String failMessage = doomsdayCore.getMessageManager().getMessage("antiair.failed", "displayName", displayName);
                        doomsdayCore.getMessagingManager().sendAntiAirMessage(failMessage, location, name);
                        
                        interceptor.remove();
                    } else {
                        plugin.getLogger().info("'" + name + "' interceptor was already dead/invalid");
                    }
                    cancel();
                    return;
                }
                
                Location interceptorLoc = interceptor.getLocation();
                
                // Check if interceptor is beyond max range from defense location
                double distanceFromDefense = interceptorLoc.distance(startLoc);
                if (distanceFromDefense > range * 1.5) { // 1.5x range as max pursuit distance
                    plugin.getLogger().info("'" + name + "' interceptor reached maximum range - failed to intercept");
                    
                    // Send failure message to players using configurable messaging system
                    DoomsdayCore doomsdayCore = (DoomsdayCore) plugin;
                    String failMessage = doomsdayCore.getMessageManager().getMessage("antiair.failed", "displayName", displayName);
                    doomsdayCore.getMessagingManager().sendAntiAirMessage(failMessage, location, name);
                    
                    createInterceptExplosion(interceptorLoc);
                    interceptor.remove();
                    cancel();
                    return;
                }
                
                // Check if target missile is still alive
                if (missileTarget == null || missileTarget.isDead() || !missileTarget.isValid()) {
                    plugin.getLogger().info("Interceptor from '" + name + "' lost target - missile destroyed or out of range");
                    interceptor.remove();
                    cancel();
                    return;
                }
                
                Location missileCurrentLoc = missileTarget.getLocation();
                
                // Calculate direction to current missile position with some prediction
                Vector toMissile = missileCurrentLoc.toVector().subtract(interceptorLoc.toVector());
                double distanceToMissile = toMissile.length();
                
                // Debug logging every 20 ticks (1 second)
                if (ticks % 20 == 0) {
                    plugin.getLogger().info("'" + name + "' interceptor tracking: distance to missile = " + String.format("%.1f", distanceToMissile) + " blocks, last distance = " + String.format("%.1f", lastDistanceToTarget));
                }
                
                // Check if interceptor is getting further away (missed/can't catch up)
                if (ticks > 60 && distanceToMissile > lastDistanceToTarget + 2) { // Reduced threshold and increased time
                    plugin.getLogger().info("'" + name + "' interceptor falling behind - failed to intercept");
                    
                    // Send failure message to players using configurable messaging system
                    DoomsdayCore doomsdayCore = (DoomsdayCore) plugin;
                    String failMessage = doomsdayCore.getMessageManager().getMessage("antiair.failed", "displayName", displayName);
                    doomsdayCore.getMessagingManager().sendAntiAirMessage(failMessage, location, name);
                    
                    createInterceptExplosion(interceptorLoc);
                    interceptor.remove();
                    cancel();
                    return;
                }
                
                lastDistanceToTarget = distanceToMissile;
                
                // Check if interceptor is close enough to detonate
                if (distanceToMissile < 5.0) { // Increased proximity range
                    if (willHit) {
                        plugin.getLogger().info("'" + name + "' interceptor hit target - missile destroyed!");
                        createInterceptExplosion(interceptorLoc);
                        if (!missileTarget.isDead()) {
                            missileTarget.remove();
                        }
                        // Notify players of successful interception
                        DoomsdayCore doomsdayCore = (DoomsdayCore) plugin;
                        String interceptMessage;
                        
                        // For manual AA systems, find the operating player and credit them
                        if (!automatic) {
                            Player operator = null;
                            double closestDistance = Double.MAX_VALUE;
                            for (Player player : location.getWorld().getPlayers()) {
                                double distance = player.getLocation().distance(location);
                                if (distance <= 5.0 && distance < closestDistance) {
                                    closestDistance = distance;
                                    operator = player;
                                }
                            }
                            
                            if (operator != null) {
                                interceptMessage = doomsdayCore.getMessageManager().getMessage("antiair.intercepted_by_player",
                                    new String[]{"displayName", "playerName"}, new String[]{displayName, operator.getName()});
                            } else {
                                interceptMessage = doomsdayCore.getMessageManager().getMessage("antiair.intercepted", "displayName", displayName);
                            }
                        } else {
                            interceptMessage = doomsdayCore.getMessageManager().getMessage("antiair.intercepted", "displayName", displayName);
                        }

                        doomsdayCore.getMessagingManager().sendAntiAirMessage(interceptMessage, interceptorLoc, name);
                    } else {
                        plugin.getLogger().info("'" + name + "' interceptor missed - detonated near target but failed to destroy");
                        createInterceptExplosion(interceptorLoc);
                        // Notify players of failed interception using configurable messaging system
                        DoomsdayCore doomsdayCore = (DoomsdayCore) plugin;
                        String failMessage = doomsdayCore.getMessageManager().getMessage("antiair.failed", "displayName", displayName);
                        doomsdayCore.getMessagingManager().sendAntiAirMessage(failMessage, interceptorLoc, name);
                        // Missile continues flying - no removal
                    }
                    interceptor.remove();
                    cancel();
                    return;
                }
                
                // Move towards current missile position
                Vector direction = toMissile.normalize();
                Location next = interceptorLoc.clone().add(direction.multiply(interceptorSpeed));
                
                interceptor.teleport(next);
                
                // Visual effects
                next.getWorld().spawnParticle(Particle.SMOKE_NORMAL, interceptorLoc, 3, 0.1, 0.1, 0.1, 0.02);
                next.getWorld().spawnParticle(Particle.FLAME, interceptorLoc, 1, 0.05, 0.05, 0.05, 0.01);
                
                ticks++;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void createInterceptExplosion(Location location) {
        World world = location.getWorld();
        
        playDefenseSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.5f);
        
        world.spawnParticle(Particle.EXPLOSION_LARGE, location, 5, 1.0, 1.0, 1.0, 0.0);
        world.spawnParticle(Particle.FLAME, location, 20, 2.0, 2.0, 2.0, 0.1);
        world.spawnParticle(Particle.SMOKE_LARGE, location, 10, 1.5, 1.5, 1.5, 0.05);
    }

    private Location calculateInterceptPoint(Location start, Location end, double flightTime) {
        Vector trajectory = end.toVector().subtract(start.toVector());
        Vector midPoint = start.toVector().add(trajectory.multiply(0.7));
        return new Location(start.getWorld(), midPoint.getX(), midPoint.getY(), midPoint.getZ());
    }

    private void playDefenseSound(Location location, Sound sound, float volume, float pitch) {
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distance(location) <= 80) {
                player.playSound(location, sound, volume, pitch);
            }
        }
    }

    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public Location getLocation() { return location.clone(); }
    public double getRange() { return range; }
    public double getAccuracy() { return accuracy; }
    public double getInterceptorSpeed() { return interceptorSpeed; }
    public double getReloadTime() { return reloadTime; }
    public double getStartupTime() { return startupTime; }
    public boolean isOperational() { return operational; }
    public boolean isAutomatic() { return automatic; }
    
    public void setOperational(boolean operational) { this.operational = operational; }
    
    public String getStatusString() {
        String mode = automatic ? "AUTO" : "MANUAL";
        return String.format("§7%s (%s): §%s%s §7[%s] §7Range: §f%.0fm §7Accuracy: §f%d%% §7Speed: §f%.1f §7Reload: §f%.1fs §7Startup: §f%.1fs", 
            displayName,
            name, 
            operational ? "a" : "c", 
            operational ? "ONLINE" : "OFFLINE",
            mode,
            range,
            (int)(accuracy * 100),
            interceptorSpeed,
            reloadTime,
            startupTime
        );
    }
}