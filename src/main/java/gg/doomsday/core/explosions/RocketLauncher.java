package gg.doomsday.core.explosions;

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

import gg.doomsday.core.config.ConfigManager;
import gg.doomsday.core.defense.AntiAirDefenseManager;
import gg.doomsday.core.defense.ReinforcedBlockManager;

public class RocketLauncher {
    
    private final JavaPlugin plugin;
    private final ExplosionHandler explosionHandler;
    private final ReinforcedBlockManager reinforcedBlockManager;
    private final AntiAirDefenseManager antiAirManager;

    public RocketLauncher(JavaPlugin plugin, ExplosionHandler explosionHandler, ReinforcedBlockManager reinforcedBlockManager, AntiAirDefenseManager antiAirManager) {
        this.plugin = plugin;
        this.explosionHandler = explosionHandler;
        this.reinforcedBlockManager = reinforcedBlockManager;
        this.antiAirManager = antiAirManager;
    }

    public void spawnRocket(Location start, Location end, double smokeOffset, double speed, double arcScale, String soundStr, String explosionTypeStr) {
        // Launch from 2 blocks higher than defined position
        Location launchPos = start.clone().add(0, 2, 0);
        
        Sound launchSound = parseSoundOrDefault(soundStr, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH);

        BlockData tntBlock = Material.TNT.createBlockData();
        BlockDisplay rocket = launchPos.getWorld().spawn(launchPos.clone().add(0, 0.5, 0), BlockDisplay.class, display -> {
            display.setBlock(tntBlock);
            Transformation transform = new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(1.0f, 1.0f, 1.0f),
                    new AxisAngle4f(0, 0, 0, 1)
            );
            display.setTransformation(transform);
        });

        final double TPS = 20.0;
        final double dtSeconds = Math.max(0.01, speed) / TPS;
        final double G = 12.0;

        Vector disp = end.toVector().subtract(launchPos.toVector());
        Vector dispHoriz = disp.clone().setY(0);
        double horizDist = dispHoriz.length();

        double Tbase = clamp(horizDist / 8.0, 2.0, 12.0);
        double T = Tbase;

        double arc = clamp(arcScale, 0.1, 3.0);
        Vector gVec = new Vector(0, -(G * arc), 0);

        Vector v0 = disp.clone().subtract(gVec.clone().multiply(0.5 * T * T)).multiply(1.0 / T);

        final double soundRadius = ((gg.doomsday.core.DoomsdayCore) plugin).getConfigManager().getRocketsConfig().getDouble("soundRadius", 100.0);

        playRocketSound(launchPos, launchSound, 2.0f, 0.9f, soundRadius);

        new BukkitRunnable() {
            double t = 0.0;
            Vector lastVel = v0.clone();
            boolean exploded = false;
            boolean checkedForIntercept = false;
            Location lastPosition = launchPos.clone().add(0, 0.5, 0); // Start at actual launch position

            @Override public void run() {
                if (rocket.isDead() || !rocket.isValid()) { 
                    cancel(); 
                    return; 
                }

                // Calculate next position based on current trajectory phase
                Location targetPos;
                Vector currentVelocity;

                if (t < T) {
                    // Arc trajectory phase - calculate physics-based position
                    Vector vt = v0.clone().add(gVec.clone().multiply(t));
                    Vector pt = launchPos.toVector()
                            .add(v0.clone().multiply(t))
                            .add(gVec.clone().multiply(0.5 * t * t));

                    targetPos = vectorToLocation(pt, launchPos.getWorld());
                    currentVelocity = vt.clone();
                    
                    // Store velocity for potential straight-line continuation
                    if (currentVelocity.length() > 1e-6) {
                        lastVel = currentVelocity.clone();
                    }
                } else {
                    // Straight line continuation phase
                    double extraTime = t - T;
                    Vector straightVel = lastVel.clone().normalize().multiply(12.0); // Constant speed
                    
                    // Continue from end point in straight line
                    targetPos = end.clone().add(straightVel.clone().multiply(extraTime));
                    currentVelocity = straightVel;
                    
                    plugin.getLogger().info("Rocket in straight-line phase: extraTime=" + String.format("%.2f", extraTime) + 
                        ", pos=" + String.format("%.1f,%.1f,%.1f", targetPos.getX(), targetPos.getY(), targetPos.getZ()));
                }

                // Check for ground collision (y <= 0)
                if (targetPos.getY() <= 0) {
                    plugin.getLogger().info("Rocket reached bedrock level (y=" + targetPos.getY() + "), disappearing...");
                    rocket.remove();
                    cancel();
                    return;
                }

                // ROBUST COLLISION DETECTION - Ray-cast from current to target position
                boolean hitBlock = performRaycastCollision(lastPosition, targetPos);
                if (hitBlock) {
                    return; // Explosion already handled in raycast method
                }

                // Anti-air interception check
                if (!checkedForIntercept && t > T * 0.15) {
                    checkedForIntercept = true;
                    boolean intercepted = antiAirManager.checkForInterception(start, end, speed, rocket);
                    if (intercepted) {
                        plugin.getLogger().info("Missile intercepted by anti-air defenses!");
                        cancel();
                        return;
                    }
                }

                // Update rocket position and orientation
                rocket.teleport(targetPos);
                
                // Apply rotation based on velocity direction
                Vector direction = currentVelocity.clone().normalize();
                applyUpToDirectionRotation(rocket, direction);

                // Create smoke trail effects
                createSmokeTrail(targetPos, direction, smokeOffset);

                // Update position tracking
                lastPosition = targetPos.clone();
                t += dtSeconds;

                // Timeout safety - remove after extended flight time
                if (t > T + 15.0) {
                    plugin.getLogger().info("Rocket flight timeout after " + String.format("%.1f", t) + " seconds, removing...");
                    rocket.remove();
                    cancel();
                }
            }

            private boolean performRaycastCollision(Location from, Location to) {
                if (from == null || to == null || from.getWorld() != to.getWorld()) {
                    plugin.getLogger().warning("Invalid collision check parameters");
                    return false;
                }

                Vector movement = to.toVector().subtract(from.toVector());
                double totalDistance = movement.length();
                
                if (totalDistance <= 0.01) return false; // Too small to matter

                Vector direction = movement.normalize();
                double stepSize = 0.1; // Very fine steps for accuracy
                
                // Check every 0.1 blocks along the path
                for (double distance = stepSize; distance <= totalDistance; distance += stepSize) {
                    Location checkPoint = from.clone().add(direction.clone().multiply(distance));
                    
                    // Safety check for world boundaries
                    if (checkPoint.getY() < 0 || checkPoint.getY() > 320) {
                        continue;
                    }
                    
                    try {
                        // Ensure we check the block at this exact position
                        if (!checkPoint.getBlock().getType().isAir()) {
                            // Found solid block - calculate impact point (slightly back from collision)
                            Location impactPoint = from.clone().add(direction.clone().multiply(Math.max(0, distance - stepSize*0.5)));
                            
                            plugin.getLogger().info("COLLISION DETECTED: Rocket hit " + 
                                checkPoint.getBlock().getType().name() + " at " +
                                checkPoint.getBlockX() + "," + checkPoint.getBlockY() + "," + checkPoint.getBlockZ() +
                                " - Exploding at impact point " +
                                String.format("%.1f,%.1f,%.1f", impactPoint.getX(), impactPoint.getY(), impactPoint.getZ()));
                            
                            explodeAt(impactPoint);
                            return true;
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error checking block at " + checkPoint + ": " + e.getMessage());
                        // If we can't check the block, assume collision to be safe
                        explodeAt(checkPoint);
                        return true;
                    }
                }
                
                // Final safety check - if we're at the target position, check if it's solid
                try {
                    if (!to.getBlock().getType().isAir()) {
                        plugin.getLogger().info("Target destination has solid block, exploding there");
                        explodeAt(to);
                        return true;
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error checking target block, exploding as safety measure");
                    explodeAt(to);
                    return true;
                }
                
                return false;
            }

            private void createSmokeTrail(Location center, Vector direction, double smokeOffset) {
                Location trailLoc = center.clone().add(direction.clone().multiply(-0.9));
                trailLoc.add(0, smokeOffset, 0);

                center.getWorld().spawnParticle(Particle.SMOKE_LARGE, trailLoc, 2, 0.10, 0.10, 0.10, 0.01);
                center.getWorld().spawnParticle(Particle.FLAME, trailLoc, 1, 0.05, 0.05, 0.05, 0.01);
                
                // Trail particles behind rocket
                for (double d = 0.5; d <= 2.0; d += 0.5) {
                    Location step = center.clone().add(direction.clone().multiply(-d));
                    step.add(0, smokeOffset, 0);
                    center.getWorld().spawnParticle(Particle.SMOKE_NORMAL, step, 1, 0.03, 0.03, 0.03, 0.0);
                }
            }

            private void explodeAt(Location where) {
                if (exploded) return;
                exploded = true;
                
                // Ensure explosion location is valid
                if (where == null || where.getWorld() == null) {
                    plugin.getLogger().warning("Invalid explosion location, using rocket position");
                    where = rocket.getLocation();
                }
                
                plugin.getLogger().info("MISSILE EXPLODING at " + 
                    String.format("%.2f,%.2f,%.2f", where.getX(), where.getY(), where.getZ()));
                
                // Play explosion sound and trigger explosion
                playRocketSound(where, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 1.0f, soundRadius);
                explosionHandler.handleExplosion(where, explosionTypeStr);
                
                // Clean up rocket entity
                if (rocket != null && !rocket.isDead()) {
                    rocket.remove();
                }
                cancel();
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void applyUpToDirectionRotation(BlockDisplay display, Vector dirBukkit) {
        Vector3f up = new Vector3f(0f, 1f, 0f);
        Vector3f dir = new Vector3f((float) dirBukkit.getX(), (float) dirBukkit.getY(), (float) dirBukkit.getZ());
        float dirLen = dir.length();

        if (dirLen < 1e-6f) return;
        dir.div(dirLen);

        Vector3f axis = up.cross(dir, new Vector3f());
        float axisLen = axis.length();
        float dot = clampF(up.dot(dir), -1f, 1f);
        float angle = (float) Math.acos(dot);

        if (axisLen < 1e-6f) {
            axis.set(1f, 0f, 0f);
        } else {
            axis.div(axisLen);
        }

        AxisAngle4f rot = new AxisAngle4f(angle, axis.x, axis.y, axis.z);

        Transformation transform = new Transformation(
                new Vector3f(0, 0, 0),
                rot,
                new Vector3f(1.0f, 1.0f, 1.0f),
                new AxisAngle4f(0, 0, 0, 1)
        );
        display.setTransformation(transform);
    }

    private Sound parseSoundOrDefault(String name, Sound def) {
        if (name == null) return def;
        try {
            return Sound.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Unknown sound '" + name + "', using default: " + def.name());
            return def;
        }
    }

    private void playRocketSound(Location location, Sound sound, float volume, float pitch, double radius) {
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distance(location) <= radius) {
                player.playSound(location, sound, volume, pitch);
            }
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
    
    private static float clampF(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static Location vectorToLocation(Vector v, World w) {
        return new Location(w, v.getX(), v.getY(), v.getZ());
    }
}