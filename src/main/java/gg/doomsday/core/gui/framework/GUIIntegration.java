package gg.doomsday.core.gui.framework;

import gg.doomsday.core.gui.nations.NationsGUIFactory;
import gg.doomsday.core.gui.nations.NationsOverviewGUI;
import gg.doomsday.core.nations.NationManager;
import gg.doomsday.core.nations.NationPlayerManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Integration helper for setting up the GUI framework
 */
public class GUIIntegration {
    
    private final GUIManager guiManager;
    private final NationManager nationManager;
    private final NationPlayerManager playerManager;
    
    public GUIIntegration(JavaPlugin plugin, NationManager nationManager, NationPlayerManager playerManager) {
        this.guiManager = new GUIManager(plugin);
        this.nationManager = nationManager;
        this.playerManager = playerManager;
        
        registerGUIFactories();
    }
    
    private void registerGUIFactories() {
        // Register nations GUI factory
        guiManager.registerGUI("nations", new NationsGUIFactory(guiManager, nationManager, playerManager));
    }
    
    public GUIManager getGUIManager() {
        return guiManager;
    }
    
    /**
     * Open the nations overview GUI
     * @param player The player
     */
    public void openNationsOverview(Player player) {
        guiManager.openGUI(player, new NationsOverviewGUI(guiManager, nationManager, playerManager));
    }
    
    /**
     * Open the nations overview GUI from another GUI
     * @param player The player
     */
    public void openNationsOverviewFromGUI(Player player) {
        guiManager.openGUI(player, new NationsOverviewGUI(guiManager, nationManager, playerManager), true);
    }
    
    /**
     * Open nations overview and clear navigation history (fresh start)
     * @param player The player
     */
    public void openNationsOverviewFresh(Player player) {
        guiManager.clearNavigationHistory(player);
        openNationsOverview(player);
    }
}

/*
INTEGRATION EXAMPLE FOR DoomsdayCore.java:

public class DoomsdayCore extends JavaPlugin {
    
    private GUIIntegration guiIntegration;
    
    @Override
    public void onEnable() {
        // ... existing initialization ...
        
        // Initialize GUI framework after nation managers
        this.guiIntegration = new GUIIntegration(this, nationManager, playerManager);
        
        // Update commands to use new GUI system
        NationsCommand nationsCommand = new NationsCommand(this, nationManager, playerManager, guiIntegration);
        getCommand("nations").setExecutor(nationsCommand);
    }
    
    public GUIIntegration getGUIIntegration() {
        return guiIntegration;
    }
}

UPDATED NationsCommand EXAMPLE:

public class NationsCommand implements CommandExecutor {
    
    private final GUIIntegration guiIntegration;
    
    public NationsCommand(..., GUIIntegration guiIntegration) {
        this.guiIntegration = guiIntegration;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        
        if (args.length == 0) {
            String nationId = playerManager.getPlayerNation(player.getUniqueId());
            if (nationId != null) {
                // Show nation main menu (existing logic)
                showNationMainMenu(player, nationId);
            } else {
                // Open nation selection using new framework
                guiIntegration.openNationSelectionGUI(player);
            }
        } else if (args[0].equalsIgnoreCase("overview")) {
            // Open nations overview
            guiIntegration.openNationsOverviewFresh(player);
        }
        
        return true;
    }
    
    // In your existing GUI click handler for "Other Nations":
    case 24: // Other Nations
        guiIntegration.openNationsOverviewFromGUI(player);
        break;
}
*/