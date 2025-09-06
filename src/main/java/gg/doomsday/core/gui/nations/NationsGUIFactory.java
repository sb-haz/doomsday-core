package gg.doomsday.core.gui.nations;

import gg.doomsday.core.gui.framework.GUI;
import gg.doomsday.core.gui.framework.GUIFactory;
import gg.doomsday.core.gui.framework.GUIManager;
import gg.doomsday.core.nations.NationManager;
import gg.doomsday.core.nations.NationPlayerManager;
import org.bukkit.entity.Player;

/**
 * Factory for creating nation-related GUIs
 */
public class NationsGUIFactory implements GUIFactory {
    
    private final GUIManager guiManager;
    private final NationManager nationManager;
    private final NationPlayerManager playerManager;
    
    public NationsGUIFactory(GUIManager guiManager, NationManager nationManager, NationPlayerManager playerManager) {
        this.guiManager = guiManager;
        this.nationManager = nationManager;
        this.playerManager = playerManager;
    }
    
    @Override
    public GUI create(String title, Player player) {
        // Nations Overview
        if (title.equals("Nations Overview")) {
            return new NationsOverviewGUI(guiManager, nationManager, playerManager);
        }
        
        // Nation Details
        if (title.endsWith(" Details")) {
            String nationName = title.replace(" Details", "");
            String nationId = getNationIdFromDisplayName(nationName);
            if (nationId != null) {
                return new NationDetailsGUI(guiManager, nationManager, playerManager, nationId);
            }
        }
        
        // Nation Missiles
        if (title.endsWith(" Missiles")) {
            String nationName = title.replace(" Missiles", "");
            String nationId = getNationIdFromDisplayName(nationName);
            if (nationId != null) {
                return new NationMissilesGUI(guiManager, nationManager, playerManager, nationId);
            }
        }
        
        // Nation Defenses
        if (title.endsWith(" Defenses")) {
            String nationName = title.replace(" Defenses", "");
            String nationId = getNationIdFromDisplayName(nationName);
            if (nationId != null) {
                return new NationDefenseGUI(guiManager, nationManager, playerManager, nationId);
            }
        }
        
        // Nation Disasters
        if (title.endsWith(" Disasters")) {
            String nationName = title.replace(" Disasters", "");
            String nationId = getNationIdFromDisplayName(nationName);
            if (nationId != null) {
                return new NationDisastersGUI(guiManager, nationManager, playerManager, nationId);
            }
        }
        
        // All Missile Types
        if (title.equals("All Missile Types")) {
            return new AllMissilesGUI(guiManager, nationManager);
        }
        
        return null;
    }
    
    private String getNationIdFromDisplayName(String displayName) {
        for (var entry : nationManager.getAllNations().entrySet()) {
            if (entry.getValue().getDisplayName().equals(displayName)) {
                return entry.getKey();
            }
        }
        return null;
    }
}