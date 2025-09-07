package gg.doomsday.core.gui.utils;

/**
 * Centralized color constants for GUI elements.
 * This allows easy modification of all GUI colors from one place.
 */
public final class GUIColors {
    
    // Base GUI colors
    public static final String TEXT_PRIMARY = "&f";        // White - Primary text
    public static final String TEXT_SECONDARY = "&7";      // Gray - Secondary text (bullet points, descriptions)
    public static final String TEXT_ACCENT = "&e";         // Yellow - Accent text (highlights, values)
    public static final String TEXT_TITLE = "&6&l";        // Gold Bold - GUI titles
    
    // Status colors
    public static final String SUCCESS = "&a";             // Green - Success messages, positive states
    public static final String WARNING = "&6";             // Gold - Warning messages
    public static final String ERROR = "&c";               // Red - Error messages, negative states
    public static final String INFO = "&b";                // Aqua - Information, neutral states
    
    // Nation-specific colors
    public static final String NATION_AMERICA = "&b";      // Aqua
    public static final String NATION_EUROPE = "&a";       // Green
    public static final String NATION_ASIA = "&e";         // Yellow
    public static final String NATION_AFRICA = "&6";       // Gold
    public static final String NATION_ANTARCTICA = "&f";   // White
    
    // System colors
    public static final String MISSILE = "&c";             // Red - Missile related
    public static final String DEFENSE = "&3";             // Dark Aqua - Defense related
    public static final String DISASTER = "&6";            // Gold - Disaster related
    
    // UI element colors
    public static final String BUTTON_POSITIVE = "&a&l";   // Green Bold - Positive action buttons
    public static final String BUTTON_NEGATIVE = "&c&l";   // Red Bold - Negative action buttons
    public static final String BUTTON_NEUTRAL = "&7";      // Gray - Neutral action buttons
    public static final String SEPARATOR = "&8";           // Dark Gray - Separators, dividers
    
    // Formatting helpers
    public static final String BOLD = "&l";
    public static final String UNDERLINE = "&n";
    public static final String RESET = "&r";
    
    // Common combinations
    public static final String BULLET_POINT = TEXT_SECONDARY + "• ";
    public static final String ARROW_RIGHT = TEXT_SECONDARY + "→ ";
    public static final String CHECKMARK = SUCCESS + "✓ ";
    public static final String CROSS = ERROR + "✗ ";
    public static final String WARNING_SYMBOL = WARNING + "⚠ ";
    public static final String INFO_SYMBOL = INFO + "ℹ ";
    
    // Private constructor to prevent instantiation
    private GUIColors() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    // Utility methods
    
    /**
     * Format text with primary color
     */
    public static String primary(String text) {
        return TEXT_PRIMARY + text;
    }
    
    /**
     * Format text with secondary color
     */
    public static String secondary(String text) {
        return TEXT_SECONDARY + text;
    }
    
    /**
     * Format text with accent color
     */
    public static String accent(String text) {
        return TEXT_ACCENT + text;
    }
    
    /**
     * Format text as a bullet point
     */
    public static String bullet(String text) {
        return BULLET_POINT + TEXT_PRIMARY + text;
    }
    
    /**
     * Format key-value pair with proper coloring
     */
    public static String keyValue(String key, String value) {
        return TEXT_SECONDARY + key + ": " + TEXT_PRIMARY + value;
    }
    
    /**
     * Get nation color by nation ID
     */
    public static String getNationColor(String nationId) {
        if (nationId == null) return TEXT_PRIMARY;
        
        switch (nationId.toLowerCase()) {
            case "america": return NATION_AMERICA;
            case "europe": return NATION_EUROPE;
            case "asia": return NATION_ASIA;
            case "africa": return NATION_AFRICA;
            case "antarctica": return NATION_ANTARCTICA;
            default: return TEXT_PRIMARY;
        }
    }
}