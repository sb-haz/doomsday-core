package gg.doomsday.core.listeners;

import gg.doomsday.core.nations.NationPlayerManager;
import gg.doomsday.core.nations.NationRole;
import gg.doomsday.core.nations.NationRoleManager;
import gg.doomsday.core.nations.RoleClaimItemManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class RoleClaimListener implements Listener {
    
    private final JavaPlugin plugin;
    private final NationRoleManager roleManager;
    private final NationPlayerManager nationPlayerManager;
    private final RoleClaimItemManager itemManager;
    
    public RoleClaimListener(JavaPlugin plugin, NationRoleManager roleManager, 
                           NationPlayerManager nationPlayerManager, RoleClaimItemManager itemManager) {
        this.plugin = plugin;
        this.roleManager = roleManager;
        this.nationPlayerManager = nationPlayerManager;
        this.itemManager = itemManager;
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only handle right-click actions
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        // Check if the item is a role claim item
        if (!itemManager.isAnyRoleClaimItem(item)) {
            return;
        }
        
        // Cancel the event to prevent other interactions
        event.setCancelled(true);
        
        // Get the role from the item
        NationRole role = itemManager.getRoleFromItem(item);
        if (role == null) {
            player.sendMessage("§cError: Invalid role claim item");
            return;
        }
        
        // Check if player is in a nation
        String playerNation = nationPlayerManager.getPlayerNation(player.getUniqueId());
        if (playerNation == null) {
            player.sendMessage("§cYou must be in a nation to claim a role!");
            player.sendMessage("§7Use §e/nations gui §7to join a nation first.");
            return;
        }
        
        // Check if claim window is active
        if (!roleManager.isClaimWindowActive()) {
            long remaining = roleManager.getClaimWindowRemainingMinutes();
            if (remaining <= 0) {
                player.sendMessage("§cThe role claim window has ended!");
                player.sendMessage("§7Roles are now assigned randomly by the system.");
            } else {
                player.sendMessage("§cThe role claim window is not active!");
            }
            return;
        }
        
        // Check if player already has a role
        if (roleManager.hasRole(player.getUniqueId())) {
            NationRole currentRole = roleManager.getPlayerRole(player.getUniqueId());
            player.sendMessage("§cYou already have the role: " + currentRole.getColoredName());
            player.sendMessage("§7You can only have one role per season!");
            return;
        }
        
        // Check if role slots are available
        int availableSlots = roleManager.getAvailableSlots(playerNation, role);
        if (availableSlots <= 0) {
            player.sendMessage("§cNo " + role.getColoredName() + " §cslots available in " + playerNation.toUpperCase() + "!");
            player.sendMessage("§7All " + role.getDisplayName() + " positions have been claimed.");
            return;
        }
        
        // Attempt to claim the role
        boolean success = roleManager.claimRole(player, role);
        
        if (success) {
            // Remove the item from player's inventory
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
            
            // Send success messages
            player.sendMessage("§a§l✓ Role Claimed Successfully!");
            player.sendMessage("§7You are now the " + role.getColoredName() + " §7of " + playerNation.toUpperCase() + "!");
            
            // Calculate remaining time
            long remainingMinutes = roleManager.getClaimWindowRemainingMinutes();
            if (remainingMinutes > 0) {
                player.sendMessage("§8Claim window ends in " + remainingMinutes + " minutes.");
            }
            
            plugin.getLogger().info("Player " + player.getName() + " claimed role " + role.getDisplayName() + " in " + playerNation);
            
        } else {
            player.sendMessage("§cFailed to claim role!");
            player.sendMessage("§7Please try again or contact an administrator.");
        }
    }
}