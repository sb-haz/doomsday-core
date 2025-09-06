package gg.doomsday.core.gui.framework;

import org.bukkit.entity.Player;

/**
 * Interface for creating GUI instances
 */
public interface GUIFactory {
    /**
     * Create a GUI instance if the title matches
     * @param title The GUI title
     * @param player The player (for context)
     * @return GUI instance or null if title doesn't match
     */
    GUI create(String title, Player player);
}