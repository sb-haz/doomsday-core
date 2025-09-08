package gg.doomsday.core.seasons;

import gg.doomsday.core.nations.NationRoleManager;
import gg.doomsday.core.nations.RoleAssignmentScheduler;
import org.bukkit.plugin.java.JavaPlugin;

public class SeasonEventListener {
    
    private final JavaPlugin plugin;
    private final NationRoleManager roleManager;
    private final RoleAssignmentScheduler roleScheduler;
    private final SeasonManager seasonManager;
    
    public SeasonEventListener(JavaPlugin plugin, NationRoleManager roleManager, 
                              RoleAssignmentScheduler roleScheduler, SeasonManager seasonManager) {
        this.plugin = plugin;
        this.roleManager = roleManager;
        this.roleScheduler = roleScheduler;
        this.seasonManager = seasonManager;
    }
    
    public void onSeasonActivated(Season season) {
        plugin.getLogger().info("Season activated - triggering role system events");
        
        // Reset all roles for the new season
        roleManager.onSeasonStart();
        
        // Start the role assignment scheduler
        roleScheduler.onSeasonStart();
        
        plugin.getLogger().info("Role system prepared for season " + season.getId());
    }
    
    public void onSeasonArchived(Season season) {
        plugin.getLogger().info("Season archived - cleaning up role system");
        
        // Stop the role assignment scheduler
        roleScheduler.onSeasonEnd();
        
        // Clear all role assignments
        roleManager.onSeasonEnd();
        
        plugin.getLogger().info("Role system cleaned up for ended season " + season.getId());
    }
}