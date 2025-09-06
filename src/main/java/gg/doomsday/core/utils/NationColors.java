package gg.doomsday.core.utils;

/**
 * Utility class for consistent nation color theming throughout the plugin
 */
public class NationColors {
    
    /**
     * Get the colored and formatted nation name
     * @param nationId The nation ID (e.g., "america", "europe")
     * @return Colored and bold nation name
     */
    public static String getColoredNationName(String nationId) {
        if (nationId == null) return "&7&lUnknown";
        
        switch (nationId.toLowerCase()) {
            case "america":
                return "&b&lAmerica";
            case "africa":
                return "&6&lAfrica";
            case "antarctica":
                return "&f&lAntarctica";
            case "europe":
                return "&a&lEurope";
            case "asia":
                return "&e&lAsia";
            default:
                return "&7&l" + nationId;
        }
    }
    
    /**
     * Get just the color code for a nation (without formatting)
     * @param nationId The nation ID
     * @return Color code (e.g., "&b")
     */
    public static String getNationColor(String nationId) {
        if (nationId == null) return "&7";
        
        switch (nationId.toLowerCase()) {
            case "america":
                return "&b";
            case "africa":
                return "&6";
            case "antarctica":
                return "&f";
            case "europe":
                return "&a";
            case "asia":
                return "&e";
            default:
                return "&7";
        }
    }
    
    /**
     * Get the nation name with just color (no bold)
     * @param nationId The nation ID
     * @return Colored nation name without bold
     */
    public static String getColoredNationNameNoBold(String nationId) {
        if (nationId == null) return "&7Unknown";
        
        switch (nationId.toLowerCase()) {
            case "america":
                return "&bAmerica";
            case "africa":
                return "&6Africa";
            case "antarctica":
                return "&fAntarctica";
            case "europe":
                return "&aEurope";
            case "asia":
                return "&eAsia";
            default:
                return "&7" + nationId;
        }
    }
}