package gg.doomsday.core.utils;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * Utility class for coordinate-related operations
 */
public class CoordinateUtils {
    
    /**
     * Parse coordinates from a lore line containing "X: 123 Y: 45 Z: 678"
     * 
     * @param coordLine The line containing coordinates
     * @return Location with parsed coordinates (world will be null)
     */
    public static Location parseCoordinatesFromLore(String coordLine) {
        try {
            // Remove color codes
            String cleanLine = coordLine.replaceAll("§[0-9a-fk-or]", "").trim();
            
            double x = 0, y = 0, z = 0;
            String[] parts = cleanLine.split(" ");
            
            for (int i = 0; i < parts.length - 1; i++) {
                if (parts[i].equals("X:")) {
                    x = Double.parseDouble(parts[i + 1]);
                } else if (parts[i].equals("Y:")) {
                    y = Double.parseDouble(parts[i + 1]);
                } else if (parts[i].equals("Z:")) {
                    z = Double.parseDouble(parts[i + 1]);
                }
            }
            
            return new Location(null, x, y, z);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Format coordinates for display
     * 
     * @param location The location to format
     * @return Formatted coordinate string
     */
    public static String formatCoordinates(Location location) {
        return "X: " + (int)location.getX() + " Y: " + (int)location.getY() + " Z: " + (int)location.getZ();
    }
    
    /**
     * Create a safe teleport location (adds 1 to Y coordinate)
     * 
     * @param location The base location
     * @param world The world to teleport to
     * @return Safe teleport location
     */
    public static Location createSafeTeleportLocation(Location location, World world) {
        return new Location(world, location.getX(), location.getY() + 1, location.getZ());
    }
    
    /**
     * Extract rocket key from a command lore line like "Use /r missile_key to launch"
     * 
     * @param loreLine The lore line containing the command
     * @return The rocket key, or null if not found
     */
    public static String extractRocketKeyFromCommandLore(String loreLine) {
        if (!loreLine.contains("Use /r ") || !loreLine.contains(" to launch")) {
            return null;
        }
        
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
     * Check if two locations are approximately equal (within 1 block)
     * 
     * @param loc1 First location
     * @param loc2 Second location
     * @return true if locations are approximately equal
     */
    public static boolean locationsEqual(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null) return false;
        if (!loc1.getWorld().equals(loc2.getWorld())) return false;
        
        return Math.abs(loc1.getX() - loc2.getX()) < 1 &&
               Math.abs(loc1.getY() - loc2.getY()) < 1 &&
               Math.abs(loc1.getZ() - loc2.getZ()) < 1;
    }
}