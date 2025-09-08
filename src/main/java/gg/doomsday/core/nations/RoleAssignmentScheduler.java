package gg.doomsday.core.nations;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class RoleAssignmentScheduler {
    
    private final JavaPlugin plugin;
    private final NationRoleManager roleManager;
    private BukkitTask claimWindowTask;
    private BukkitTask periodicCheckTask;
    
    public RoleAssignmentScheduler(JavaPlugin plugin, NationRoleManager roleManager) {
        this.plugin = plugin;
        this.roleManager = roleManager;
    }
    
    public void startPeriodicCheck() {
        // Cancel existing task if running
        if (periodicCheckTask != null && !periodicCheckTask.isCancelled()) {
            periodicCheckTask.cancel();
        }
        
        // Check every minute (1200 ticks) for claim window status
        periodicCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkClaimWindowStatus();
            }
        }.runTaskTimer(plugin, 1200L, 1200L); // Start after 1 minute, repeat every minute
        
        plugin.getLogger().info("Started role assignment periodic check task");
    }
    
    public void stopPeriodicCheck() {
        if (periodicCheckTask != null && !periodicCheckTask.isCancelled()) {
            periodicCheckTask.cancel();
            periodicCheckTask = null;
        }
        
        if (claimWindowTask != null && !claimWindowTask.isCancelled()) {
            claimWindowTask.cancel();
            claimWindowTask = null;
        }
        
        plugin.getLogger().info("Stopped role assignment scheduler tasks");
    }
    
    private void checkClaimWindowStatus() {
        if (!roleManager.isClaimWindowActive()) {
            // Claim window has ended, perform random assignment
            performDelayedRandomAssignment();
        }
    }
    
    public void scheduleClaimWindowEnd(long delayMinutes) {
        // Cancel existing claim window task
        if (claimWindowTask != null && !claimWindowTask.isCancelled()) {
            claimWindowTask.cancel();
        }
        
        long delayTicks = delayMinutes * 60 * 20; // Convert minutes to ticks
        
        claimWindowTask = new BukkitRunnable() {
            @Override
            public void run() {
                onClaimWindowEnd();
            }
        }.runTaskLater(plugin, delayTicks);
        
        plugin.getLogger().info("Scheduled claim window end in " + delayMinutes + " minutes");
    }
    
    private void onClaimWindowEnd() {
        plugin.getLogger().info("Claim window has ended - triggering role assignment");
        
        // Broadcast to all online players
        Bukkit.broadcastMessage("§6§l[DOOMSDAY] §eRole claim window has ended!");
        Bukkit.broadcastMessage("§7Assigning remaining roles randomly...");
        
        // Perform random assignment with a small delay
        performDelayedRandomAssignment();
    }
    
    private void performDelayedRandomAssignment() {
        // Small delay to ensure all systems are ready
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    roleManager.performRandomAssignment();
                    
                    // Broadcast completion
                    Bukkit.broadcastMessage("§a§l[DOOMSDAY] §aRole assignment completed!");
                    Bukkit.broadcastMessage("§7Check §e/nations gui §7to see role assignments.");
                    
                } catch (Exception e) {
                    plugin.getLogger().severe("Error during random role assignment: " + e.getMessage());
                    e.printStackTrace();
                    
                    Bukkit.broadcastMessage("§c§l[DOOMSDAY] §cError during role assignment!");
                    Bukkit.broadcastMessage("§7Please contact an administrator.");
                }
            }
        }.runTaskLater(plugin, 100L); // 5 second delay
    }
    
    public void onSeasonStart() {
        plugin.getLogger().info("Season started - starting role assignment scheduler");
        
        // Calculate claim window duration and schedule the end
        long claimWindowMinutes = 60; // This should come from config, but hardcoded for now
        
        // Start the periodic check
        startPeriodicCheck();
        
        // Schedule claim window end
        scheduleClaimWindowEnd(claimWindowMinutes);
        
        // Broadcast to players
        Bukkit.broadcastMessage("§6§l[DOOMSDAY] §eNew season started!");
        Bukkit.broadcastMessage("§7Role claim window is now active for §e" + claimWindowMinutes + " minutes§7!");
        Bukkit.broadcastMessage("§7Use role claim items to secure your position!");
    }
    
    public void onSeasonEnd() {
        plugin.getLogger().info("Season ended - stopping role assignment scheduler");
        
        // Stop all scheduled tasks
        stopPeriodicCheck();
        
        // Broadcast to players
        Bukkit.broadcastMessage("§c§l[DOOMSDAY] §cSeason has ended!");
        Bukkit.broadcastMessage("§7All role assignments have been cleared.");
    }
    
    public boolean isClaimWindowTaskActive() {
        return claimWindowTask != null && !claimWindowTask.isCancelled();
    }
    
    public boolean isPeriodicCheckActive() {
        return periodicCheckTask != null && !periodicCheckTask.isCancelled();
    }
}