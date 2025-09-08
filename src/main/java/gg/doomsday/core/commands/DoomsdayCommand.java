package gg.doomsday.core.commands;

import gg.doomsday.core.DoomsdayCore;
import gg.doomsday.core.config.ConfigManager;
import gg.doomsday.core.managers.MessageManager;
import gg.doomsday.core.managers.BlockManager;
import gg.doomsday.core.defense.ReinforcedBlockManager;
import gg.doomsday.core.items.ReinforcementHandler;
import gg.doomsday.core.defense.AntiAirDefenseManager;
import gg.doomsday.core.nations.NationManager;
import gg.doomsday.core.nations.NationPlayerManager;
import gg.doomsday.core.nations.Nation;
import gg.doomsday.core.nations.NationRoleManager;
import gg.doomsday.core.nations.NationRole;
import gg.doomsday.core.nations.NationRoleAssignment;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DoomsdayCommand implements CommandExecutor, TabCompleter {
    
    private final DoomsdayCore plugin;
    private final MessageManager messageManager;
    private final BlockManager blockManager;
    private final ConfigManager configManager;
    private final ReinforcedBlockManager reinforcedBlockManager;
    private final ReinforcementHandler reinforcementHandler;
    private final AntiAirDefenseManager antiAirManager;
    private final NationManager nationManager;
    private final NationPlayerManager nationPlayerManager;
    private NationRoleManager roleManager; // Nullable for backwards compatibility
    
    public DoomsdayCommand(DoomsdayCore plugin, MessageManager messageManager, BlockManager blockManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
        this.blockManager = blockManager;
        this.configManager = plugin.getConfigManager();
        this.reinforcedBlockManager = plugin.getReinforcedBlockManager();
        this.reinforcementHandler = plugin.getReinforcementHandler();
        this.antiAirManager = plugin.getAntiAirManager();
        this.nationManager = plugin.getNationManager();
        this.nationPlayerManager = plugin.getNationPlayerManager();
        // Role manager is optional - may be null if not initialized
        this.roleManager = null; // Will be set via setter method
    }
    
    public void setRoleManager(NationRoleManager roleManager) {
        this.roleManager = roleManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("dd")) {
            return false;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Players only!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            showUsage(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "help":
                return handleHelp(player);
            case "reload":
                return handleReload(player, args);
            case "blocks":
                return handleBlocks(player, args);
            case "kill-entities":
                return handleKillEntities(player);
            case "nation":
            case "nations":
                return handleNationAdmin(player, args);
            case "toggle":
                return handleToggle(player, args);
            case "role":
            case "roles":
                return handleRoleAdmin(player, args);
            default:
                showUsage(player);
                return true;
        }
    }
    
    private boolean handleReload(Player player, String[] args) {
        if (!player.hasPermission("rocket.reload")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to reload configurations!");
            return true;
        }
        
        if (args.length == 1) {
            // Reload everything
            return reloadAll(player);
        }
        
        String reloadType = args[1].toLowerCase();
        switch (reloadType) {
            case "config":
            case "configs":
                return reloadConfigs(player);
            case "blocks":
            case "markers":
                return reloadBlocks(player);
            case "items":
                return reloadItems(player);
            case "messages":
                return reloadMessages(player);
            case "antiair":
                return reloadAntiAir(player);
            case "disasters":
                return reloadDisasters(player);
            case "chat":
                return reloadChat(player);
            default:
                player.sendMessage(ChatColor.RED + "Unknown reload type: " + reloadType);
                player.sendMessage(ChatColor.GRAY + "Available types: config, blocks, items, messages, antiair, disasters, chat");
                return true;
        }
    }
    
    private boolean handleBlocks(Player player, String[] args) {
        if (!player.hasPermission("rocket.reload")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to manage world blocks!");
            return true;
        }
        
        if (args.length == 1) {
            player.sendMessage(ChatColor.YELLOW + "Block Management Commands:");
            player.sendMessage(ChatColor.GRAY + "/dd blocks place - Place missile and anti-air markers in world");
            player.sendMessage(ChatColor.GRAY + "/dd blocks remove - Remove all markers from world");
            player.sendMessage(ChatColor.GRAY + "/dd blocks reload - Remove and replace all markers");
            return true;
        }
        
        String action = args[1].toLowerCase();
        World world = player.getWorld();
        
        switch (action) {
            case "place":
                blockManager.placeAllBlocks(world);
                player.sendMessage(ChatColor.GREEN + "✅ Placed all missile and anti-air markers in the world!");
                break;
            case "remove":
                blockManager.removeAllBlocks(world);
                player.sendMessage(ChatColor.GREEN + "✅ Removed all markers from the world!");
                break;
            case "reload":
                blockManager.removeAllBlocks(world);
                blockManager.placeAllBlocks(world);
                player.sendMessage(ChatColor.GREEN + "🔄 Reloaded all world markers!");
                break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown action: " + action);
                player.sendMessage(ChatColor.GRAY + "Available actions: place, remove, reload");
        }
        return true;
    }
    
    private boolean handleKillEntities(Player player) {
        if (!player.hasPermission("rocket.reload")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to kill entities!");
            return true;
        }
        
        player.sendMessage(ChatColor.YELLOW + "🔪 Killing all non-player entities...");
        
        // Execute the kill command as the server console to avoid permission issues
        try {
            org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), "kill @e[type=!player]");
            player.sendMessage(ChatColor.GREEN + "✅ Successfully killed all non-player entities!");
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "❌ Failed to execute kill command: " + e.getMessage());
            plugin.getLogger().warning("Failed to execute kill command: " + e.getMessage());
        }
        
        return true;
    }
    
    private boolean handleNationAdmin(Player player, String[] args) {
        if (!player.hasPermission("rocket.reload")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to manage nations!");
            return true;
        }
        
        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "Nation Admin Commands:");
            player.sendMessage(ChatColor.GRAY + "/dd nation set <player> <nation|none> - Set player's nation");
            player.sendMessage(ChatColor.GRAY + "/dd nation remove <player> - Remove player from nation");
            player.sendMessage(ChatColor.GRAY + "/dd nation info <player> - View player's nation info");
            return true;
        }
        
        String action = args[1].toLowerCase();
        
        switch (action) {
            case "set":
                return handleNationSet(player, args);
            case "remove":
                return handleNationRemove(player, args);
            case "info":
                return handleNationInfo(player, args);
            default:
                player.sendMessage(ChatColor.RED + "Unknown action: " + action);
                player.sendMessage(ChatColor.GRAY + "Available actions: set, remove, info");
                return true;
        }
    }
    
    private boolean handleNationSet(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Usage: /dd nation set <player> <nation|none>");
            return true;
        }
        
        String targetPlayerName = args[2];
        String nationId = args[3].toLowerCase();
        
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            player.sendMessage(ChatColor.RED + "Player '" + targetPlayerName + "' not found!");
            return true;
        }
        
        if (nationId.equals("none")) {
            if (nationPlayerManager.adminRemovePlayerFromNation(targetPlayer)) {
                player.sendMessage(ChatColor.GREEN + "✓ Removed " + targetPlayer.getName() + " from their nation");
                targetPlayer.sendMessage(ChatColor.YELLOW + "An admin has removed you from your nation.");
            } else {
                player.sendMessage(ChatColor.RED + "Failed to remove " + targetPlayer.getName() + " from their nation");
            }
        } else {
            Nation nation = nationManager.getAllNations().get(nationId);
            if (nation == null) {
                player.sendMessage(ChatColor.RED + "Nation '" + nationId + "' not found!");
                player.sendMessage(ChatColor.GRAY + "Available nations: " + String.join(", ", nationManager.getAllNations().keySet()));
                return true;
            }
            
            if (nationPlayerManager.adminSetPlayerNation(targetPlayer, nationId)) {
                player.sendMessage(ChatColor.GREEN + "✓ Set " + targetPlayer.getName() + "'s nation to " + nation.getDisplayName());
                targetPlayer.sendMessage(ChatColor.YELLOW + "An admin has set your nation to " + nation.getDisplayName());
            } else {
                player.sendMessage(ChatColor.RED + "Failed to set " + targetPlayer.getName() + "'s nation");
            }
        }
        
        return true;
    }
    
    private boolean handleNationRemove(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /dd nation remove <player>");
            return true;
        }
        
        String targetPlayerName = args[2];
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            player.sendMessage(ChatColor.RED + "Player '" + targetPlayerName + "' not found!");
            return true;
        }
        
        String currentNation = nationPlayerManager.getPlayerNation(targetPlayer.getUniqueId());
        if (currentNation == null) {
            player.sendMessage(ChatColor.YELLOW + targetPlayer.getName() + " is not in any nation.");
            return true;
        }
        
        if (nationPlayerManager.adminRemovePlayerFromNation(targetPlayer)) {
            Nation nation = nationManager.getAllNations().get(currentNation);
            String nationName = nation != null ? nation.getDisplayName() : currentNation;
            player.sendMessage(ChatColor.GREEN + "✓ Removed " + targetPlayer.getName() + " from " + nationName);
            targetPlayer.sendMessage(ChatColor.YELLOW + "An admin has removed you from " + nationName);
        } else {
            player.sendMessage(ChatColor.RED + "Failed to remove " + targetPlayer.getName() + " from their nation");
        }
        
        return true;
    }
    
    private boolean handleNationInfo(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /dd nation info <player>");
            return true;
        }
        
        String targetPlayerName = args[2];
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            player.sendMessage(ChatColor.RED + "Player '" + targetPlayerName + "' not found!");
            return true;
        }
        
        String currentNation = nationPlayerManager.getPlayerNation(targetPlayer.getUniqueId());
        if (currentNation == null) {
            player.sendMessage(ChatColor.YELLOW + targetPlayer.getName() + " is not in any nation.");
        } else {
            Nation nation = nationManager.getAllNations().get(currentNation);
            String nationName = nation != null ? nation.getDisplayName() : currentNation;
            long joinDate = nationPlayerManager.getPlayerJoinDate(targetPlayer.getUniqueId());
            boolean canSwitch = nationPlayerManager.canPlayerSwitch(targetPlayer.getUniqueId());
            
            player.sendMessage(ChatColor.GOLD + "=== " + targetPlayer.getName() + "'s Nation Info ===");
            player.sendMessage(ChatColor.YELLOW + "Nation: " + ChatColor.WHITE + nationName);
            player.sendMessage(ChatColor.YELLOW + "Joined: " + ChatColor.WHITE + new java.util.Date(joinDate));
            player.sendMessage(ChatColor.YELLOW + "Can Switch: " + (canSwitch ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
        }
        
        return true;
    }
    
    private boolean handleToggle(Player player, String[] args) {
        if (!player.hasPermission("rocket.reload")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to toggle settings!");
            return true;
        }
        
        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "Toggle Commands:");
            player.sendMessage(ChatColor.GRAY + "/dd toggle join - Toggle player joining");
            player.sendMessage(ChatColor.GRAY + "/dd toggle leave - Toggle player leaving");
            player.sendMessage(ChatColor.GRAY + "/dd toggle status - View current settings");
            return true;
        }
        
        String setting = args[1].toLowerCase();
        
        switch (setting) {
            case "join":
            case "joining":
                boolean currentJoinSetting = nationPlayerManager.getAllowPlayerJoining();
                nationPlayerManager.setAllowPlayerJoining(!currentJoinSetting);
                String joinStatus = !currentJoinSetting ? "enabled" : "disabled";
                player.sendMessage(ChatColor.GREEN + "✓ Player joining " + joinStatus);
                break;
                
            case "leave":
            case "leaving":
                boolean currentLeaveSetting = nationPlayerManager.getAllowPlayerLeaving();
                nationPlayerManager.setAllowPlayerLeaving(!currentLeaveSetting);
                String leaveStatus = !currentLeaveSetting ? "enabled" : "disabled";
                player.sendMessage(ChatColor.GREEN + "✓ Player leaving " + leaveStatus);
                break;
                
            case "status":
                boolean joinEnabled = nationPlayerManager.getAllowPlayerJoining();
                boolean leaveEnabled = nationPlayerManager.getAllowPlayerLeaving();
                
                player.sendMessage(ChatColor.GOLD + "=== Nation Settings ===");
                player.sendMessage(ChatColor.YELLOW + "Player Joining: " + (joinEnabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));
                player.sendMessage(ChatColor.YELLOW + "Player Leaving: " + (leaveEnabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));
                break;
                
            default:
                player.sendMessage(ChatColor.RED + "Unknown setting: " + setting);
                player.sendMessage(ChatColor.GRAY + "Available settings: join, leave, status");
                return true;
        }
        
        return true;
    }
    
    private boolean handleRoleAdmin(Player player, String[] args) {
        if (!player.hasPermission("rocket.reload")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to manage roles!");
            return true;
        }
        
        if (roleManager == null) {
            player.sendMessage(ChatColor.RED + "Role management system is not available!");
            return true;
        }
        
        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "Role Admin Commands:");
            player.sendMessage(ChatColor.GRAY + "/dd role add <player> <role> - Assign role to player");
            player.sendMessage(ChatColor.GRAY + "/dd role remove <player> - Remove player's role");
            player.sendMessage(ChatColor.GRAY + "/dd role list <nation> - List all role assignments");
            player.sendMessage(ChatColor.GRAY + "/dd role window - Check claim window status");
            player.sendMessage(ChatColor.GRAY + "/dd role assign - Force random assignment");
            player.sendMessage(ChatColor.GRAY + "/dd role reset - Reset all roles (new season)");
            return true;
        }
        
        String action = args[1].toLowerCase();
        
        switch (action) {
            case "add":
            case "assign":
                if (action.equals("assign") && args.length == 2) {
                    return handleRoleForceAssign(player);
                }
                return handleRoleAdd(player, args);
            case "remove":
                return handleRoleRemove(player, args);
            case "list":
                return handleRoleList(player, args);
            case "window":
                return handleRoleWindow(player);
            case "reset":
                return handleRoleReset(player);
            default:
                player.sendMessage(ChatColor.RED + "Unknown action: " + action);
                player.sendMessage(ChatColor.GRAY + "Available actions: add, remove, list, window, assign, reset");
                return true;
        }
    }
    
    private boolean handleRoleAdd(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Usage: /dd role add <player> <role>");
            player.sendMessage(ChatColor.GRAY + "Available roles: President, ArmyChief, Soldier, Builder, Healer, Trader");
            return true;
        }
        
        String targetPlayerName = args[2];
        String roleName = args[3];
        
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            player.sendMessage(ChatColor.RED + "Player '" + targetPlayerName + "' not found!");
            return true;
        }
        
        NationRole role = NationRole.fromString(roleName);
        if (role == NationRole.CITIZEN) {
            player.sendMessage(ChatColor.RED + "Invalid role: " + roleName);
            player.sendMessage(ChatColor.GRAY + "Available roles: President, ArmyChief, Soldier, Builder, Healer, Trader");
            return true;
        }
        
        String playerNation = nationPlayerManager.getPlayerNation(targetPlayer.getUniqueId());
        if (playerNation == null) {
            player.sendMessage(ChatColor.RED + targetPlayer.getName() + " is not in any nation!");
            return true;
        }
        
        if (roleManager.adminAssignRole(targetPlayer.getUniqueId(), targetPlayer.getName(), role)) {
            Nation nation = nationManager.getAllNations().get(playerNation);
            String nationName = nation != null ? nation.getDisplayName() : playerNation;
            
            player.sendMessage(ChatColor.GREEN + "✓ Assigned " + role.getColoredName() + " §ato " + targetPlayer.getName() + " in " + nationName);
            targetPlayer.sendMessage(ChatColor.YELLOW + "An admin has assigned you the role: " + role.getColoredName());
        } else {
            player.sendMessage(ChatColor.RED + "Failed to assign role to " + targetPlayer.getName());
            player.sendMessage(ChatColor.GRAY + "Reason: No available slots or player already has a role");
        }
        
        return true;
    }
    
    private boolean handleRoleRemove(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /dd role remove <player>");
            return true;
        }
        
        String targetPlayerName = args[2];
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            player.sendMessage(ChatColor.RED + "Player '" + targetPlayerName + "' not found!");
            return true;
        }
        
        if (!roleManager.hasRole(targetPlayer.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + targetPlayer.getName() + " doesn't have any role assigned.");
            return true;
        }
        
        NationRole currentRole = roleManager.getPlayerRole(targetPlayer.getUniqueId());
        
        if (roleManager.adminRemoveRole(targetPlayer.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN + "✓ Removed " + currentRole.getColoredName() + " §afrom " + targetPlayer.getName());
            targetPlayer.sendMessage(ChatColor.YELLOW + "An admin has removed your role: " + currentRole.getColoredName());
        } else {
            player.sendMessage(ChatColor.RED + "Failed to remove role from " + targetPlayer.getName());
        }
        
        return true;
    }
    
    private boolean handleRoleList(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /dd role list <nation>");
            player.sendMessage(ChatColor.GRAY + "Available nations: " + String.join(", ", nationManager.getAllNations().keySet()));
            return true;
        }
        
        String nationId = args[2].toLowerCase();
        Nation nation = nationManager.getAllNations().get(nationId);
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Nation '" + nationId + "' not found!");
            player.sendMessage(ChatColor.GRAY + "Available nations: " + String.join(", ", nationManager.getAllNations().keySet()));
            return true;
        }
        
        player.sendMessage(ChatColor.GOLD + "=== " + nation.getDisplayName() + " Role Assignments ===");
        
        Map<NationRole, List<NationRoleAssignment>> roles = roleManager.getAllRoleAssignments(nationId);
        
        for (NationRole role : NationRole.getClaimableRoles()) {
            List<NationRoleAssignment> assignments = roles.get(role);
            int maxSlots = roleManager.getAvailableSlots(nationId, role) + (assignments != null ? assignments.size() : 0);
            
            if (maxSlots == 0) {
                continue; // Skip roles not available in this nation
            }
            
            player.sendMessage(ChatColor.YELLOW + role.getDisplayName() + " (" + 
                (assignments != null ? assignments.size() : 0) + "/" + maxSlots + "):");
            
            if (assignments != null && !assignments.isEmpty()) {
                for (NationRoleAssignment assignment : assignments) {
                    String method = assignment.wasClaimed() ? "§2[CLAIMED]" : "§6[ASSIGNED]";
                    player.sendMessage(ChatColor.GRAY + "  • §f" + assignment.getPlayerName() + " " + method);
                }
            } else {
                player.sendMessage(ChatColor.GRAY + "  • §8No assignments");
            }
        }
        
        return true;
    }
    
    private boolean handleRoleWindow(Player player) {
        boolean isActive = roleManager.isClaimWindowActive();
        long remaining = roleManager.getClaimWindowRemainingMinutes();
        
        player.sendMessage(ChatColor.GOLD + "=== Role Claim Window Status ===");
        
        if (isActive) {
            player.sendMessage(ChatColor.GREEN + "Status: §aACTIVE");
            player.sendMessage(ChatColor.YELLOW + "Time Remaining: §f" + remaining + " minutes");
            player.sendMessage(ChatColor.GRAY + "Players can use role claim items to secure positions");
        } else {
            player.sendMessage(ChatColor.RED + "Status: §cINACTIVE");
            if (remaining <= 0) {
                player.sendMessage(ChatColor.GRAY + "Claim window has ended - roles are auto-assigned");
            } else {
                player.sendMessage(ChatColor.GRAY + "No active season or claim window not yet started");
            }
        }
        
        return true;
    }
    
    private boolean handleRoleForceAssign(Player player) {
        player.sendMessage(ChatColor.YELLOW + "🎯 Forcing random role assignment...");
        
        try {
            roleManager.performRandomAssignment();
            player.sendMessage(ChatColor.GREEN + "✅ Random role assignment completed!");
            player.sendMessage(ChatColor.GRAY + "All unfilled roles have been assigned to available players");
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "❌ Failed to perform role assignment: " + e.getMessage());
            plugin.getLogger().warning("Failed to perform role assignment: " + e.getMessage());
        }
        
        return true;
    }
    
    private boolean handleRoleReset(Player player) {
        player.sendMessage(ChatColor.YELLOW + "⚠️ Resetting all nation roles...");
        
        roleManager.resetAllRoles();
        
        player.sendMessage(ChatColor.GREEN + "✅ All nation roles have been reset!");
        player.sendMessage(ChatColor.GRAY + "Ready for new season role assignments");
        
        return true;
    }
    
    private boolean handleHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== 🎮 Doomsday Plugin Commands ===");
        player.sendMessage("");
        
        player.sendMessage(ChatColor.YELLOW + "🚀 Rocket System:");
        player.sendMessage(ChatColor.GRAY + "/rocket <missile_name>" + ChatColor.WHITE + " - Launch missiles (r1, r2, r3, r4, r5)");
        player.sendMessage(ChatColor.GRAY + "/rocket options" + ChatColor.WHITE + " - View missile configurations");
        if (player.hasPermission("rocket.reload")) {
            player.sendMessage(ChatColor.GRAY + "/rocket reload" + ChatColor.WHITE + " - Reload rocket configs");
            player.sendMessage(ChatColor.GRAY + "/rocket powder [amount]" + ChatColor.WHITE + " - Get reinforcement powder");
            player.sendMessage(ChatColor.GRAY + "/rocket helmet [amount]" + ChatColor.WHITE + " - Get detection helmets");
        }
        player.sendMessage("");
        
        player.sendMessage(ChatColor.YELLOW + "🛡️ Anti-Air Defense:");
        player.sendMessage(ChatColor.GRAY + "/antiair status" + ChatColor.WHITE + " - View defense systems status");
        if (player.hasPermission("rocket.antiair")) {
            player.sendMessage(ChatColor.GRAY + "/antiair online <name>" + ChatColor.WHITE + " - Bring defense online");
            player.sendMessage(ChatColor.GRAY + "/antiair offline <name>" + ChatColor.WHITE + " - Take defense offline");
            player.sendMessage(ChatColor.GRAY + "/antiair reload" + ChatColor.WHITE + " - Reload anti-air configs");
        }
        player.sendMessage("");
        
        player.sendMessage(ChatColor.YELLOW + "🗺️ Nations System:");
        player.sendMessage(ChatColor.GRAY + "/nations" + ChatColor.WHITE + " - Check your current nation");
        player.sendMessage(ChatColor.GRAY + "/nations gui" + ChatColor.WHITE + " - Open nations overview GUI");
        player.sendMessage(ChatColor.GRAY + "/nations teleport [nation]" + ChatColor.WHITE + " - Teleport to nation center");
        player.sendMessage("");
        
        player.sendMessage(ChatColor.YELLOW + "🏆 Seasons System:");
        if (player.hasPermission("season.admin")) {
            player.sendMessage(ChatColor.GRAY + "/season current" + ChatColor.WHITE + " - View current season");
            player.sendMessage(ChatColor.GRAY + "/season create <id>" + ChatColor.WHITE + " - Create new season");
            player.sendMessage(ChatColor.GRAY + "/season activate" + ChatColor.WHITE + " - Activate season");
        } else {
            player.sendMessage(ChatColor.GRAY + "/season current" + ChatColor.WHITE + " - View current season info");
        }
        player.sendMessage("");
        
        if (player.hasPermission("rocket.disaster")) {
            player.sendMessage(ChatColor.YELLOW + "🌪️ Natural Disasters:");
            player.sendMessage(ChatColor.GRAY + "/disaster trigger <nation> <type>" + ChatColor.WHITE + " - Trigger disaster");
            player.sendMessage(ChatColor.GRAY + "/disaster stop <nation> <type>" + ChatColor.WHITE + " - Stop disaster");
            player.sendMessage(ChatColor.GRAY + "/disaster status" + ChatColor.WHITE + " - View disaster status");
            player.sendMessage("");
        }
        
        if (player.hasPermission("rocket.reload")) {
            player.sendMessage(ChatColor.YELLOW + "⚙️ System Management:");
            player.sendMessage(ChatColor.GRAY + "/dd reload" + ChatColor.WHITE + " - Reload everything + world markers");
            player.sendMessage(ChatColor.GRAY + "/dd blocks place" + ChatColor.WHITE + " - Place world markers");
            player.sendMessage(ChatColor.GRAY + "/dd blocks remove" + ChatColor.WHITE + " - Remove world markers");
            player.sendMessage(ChatColor.GRAY + "/dd kill-entities" + ChatColor.WHITE + " - Kill all non-player entities");
            player.sendMessage("");
            
            player.sendMessage(ChatColor.YELLOW + "👑 Nation Administration:");
            player.sendMessage(ChatColor.GRAY + "/dd nation set <player> <nation|none>" + ChatColor.WHITE + " - Set player's nation");
            player.sendMessage(ChatColor.GRAY + "/dd nation remove <player>" + ChatColor.WHITE + " - Remove player from nation");
            player.sendMessage(ChatColor.GRAY + "/dd nation info <player>" + ChatColor.WHITE + " - View player's nation info");
            player.sendMessage("");
            
            player.sendMessage(ChatColor.YELLOW + "🎭 Role Management:");
            player.sendMessage(ChatColor.GRAY + "/dd role add <player> <role>" + ChatColor.WHITE + " - Assign role to player");
            player.sendMessage(ChatColor.GRAY + "/dd role remove <player>" + ChatColor.WHITE + " - Remove player's role");
            player.sendMessage(ChatColor.GRAY + "/dd role list <nation>" + ChatColor.WHITE + " - List nation role assignments");
            player.sendMessage(ChatColor.GRAY + "/dd role window" + ChatColor.WHITE + " - Check claim window status");
            player.sendMessage(ChatColor.GRAY + "/dd role assign" + ChatColor.WHITE + " - Force random assignment");
            player.sendMessage("");
            
            player.sendMessage(ChatColor.YELLOW + "🔧 Nation Settings:");
            player.sendMessage(ChatColor.GRAY + "/dd toggle join" + ChatColor.WHITE + " - Enable/disable player joining");
            player.sendMessage(ChatColor.GRAY + "/dd toggle leave" + ChatColor.WHITE + " - Enable/disable player leaving");
            player.sendMessage(ChatColor.GRAY + "/dd toggle status" + ChatColor.WHITE + " - View current settings");
            player.sendMessage("");
        }
        
        player.sendMessage(ChatColor.YELLOW + "💬 Utility:");
        player.sendMessage(ChatColor.GRAY + "/cc <message>" + ChatColor.WHITE + " - Send colored chat (use & codes)");
        if (player.hasPermission("rocket.reload")) {
            player.sendMessage(ChatColor.GRAY + "/rr" + ChatColor.WHITE + " - Quick plugin reload");
        }
        player.sendMessage("");
        
        player.sendMessage(ChatColor.GRAY + "💡 " + ChatColor.ITALIC + "Use " + ChatColor.YELLOW + "/dd help" + ChatColor.GRAY + ChatColor.ITALIC + " to see this help again!");
        return true;
    }
    
    private boolean reloadAll(Player player) {
        player.sendMessage(ChatColor.YELLOW + "🔄 Reloading all Doomsday configurations...");
        
        // Reload all configurations
        configManager.reloadAllConfigs();
        reinforcementHandler.reloadCustomItems();
        messageManager.reloadMessages();
        reinforcedBlockManager.reloadConfiguration();
        plugin.getMessagingManager().loadConfiguration();
        antiAirManager.reloadDefenses();
        nationManager.reload();
        
        // Reload world markers
        World world = player.getWorld();
        blockManager.removeAllBlocks(world);
        blockManager.placeAllBlocks(world);
        
        player.sendMessage(ChatColor.GREEN + "✅ Complete reload finished!");
        player.sendMessage(ChatColor.GRAY + "Reloaded: configs, items, messages, anti-air, disasters, world markers");
        
        return true;
    }
    
    private boolean reloadConfigs(Player player) {
        configManager.reloadAllConfigs();
        player.sendMessage(ChatColor.GREEN + "✅ Reloaded all configuration files!");
        return true;
    }
    
    private boolean reloadBlocks(Player player) {
        World world = player.getWorld();
        blockManager.removeAllBlocks(world);
        blockManager.placeAllBlocks(world);
        player.sendMessage(ChatColor.GREEN + "🔄 Reloaded world markers (missiles & anti-air)!");
        return true;
    }
    
    private boolean reloadItems(Player player) {
        reinforcementHandler.reloadCustomItems();
        player.sendMessage(ChatColor.GREEN + "✅ Reloaded custom items configuration!");
        return true;
    }
    
    private boolean reloadMessages(Player player) {
        messageManager.reloadMessages();
        plugin.getMessagingManager().loadConfiguration();
        player.sendMessage(ChatColor.GREEN + "✅ Reloaded message configurations!");
        return true;
    }
    
    private boolean reloadAntiAir(Player player) {
        antiAirManager.reloadDefenses();
        player.sendMessage(ChatColor.GREEN + "✅ Reloaded anti-air defense systems!");
        return true;
    }
    
    private boolean reloadDisasters(Player player) {
        nationManager.reload();
        player.sendMessage(ChatColor.GREEN + "✅ Reloaded disaster configurations!");
        return true;
    }
    
    private boolean reloadChat(Player player) {
        plugin.getCustomChatListener().reloadChatConfig();
        player.sendMessage(ChatColor.GREEN + "✅ Reloaded chat configuration!");
        return true;
    }
    
    private void showUsage(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Doomsday Master Control ===");
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "/dd help" + ChatColor.GRAY + " - Show all available commands");
        player.sendMessage(ChatColor.YELLOW + "/dd reload" + ChatColor.GRAY + " - Reload everything (configs + world markers)");
        
        if (player.hasPermission("rocket.reload")) {
            player.sendMessage("");
            player.sendMessage(ChatColor.YELLOW + "Specific Reload Commands:");
            player.sendMessage(ChatColor.GRAY + "/dd reload config" + ChatColor.WHITE + " - Reload configuration files only");
            player.sendMessage(ChatColor.GRAY + "/dd reload blocks" + ChatColor.WHITE + " - Reload world markers only");
            player.sendMessage(ChatColor.GRAY + "/dd reload items" + ChatColor.WHITE + " - Reload custom items only");
            player.sendMessage(ChatColor.GRAY + "/dd reload messages" + ChatColor.WHITE + " - Reload message files only");
            player.sendMessage(ChatColor.GRAY + "/dd reload antiair" + ChatColor.WHITE + " - Reload anti-air systems only");
            player.sendMessage(ChatColor.GRAY + "/dd reload disasters" + ChatColor.WHITE + " - Reload disaster configs only");
            player.sendMessage("");
            player.sendMessage(ChatColor.YELLOW + "Block & Entity Management:");
            player.sendMessage(ChatColor.GRAY + "/dd blocks place" + ChatColor.WHITE + " - Place world markers");
            player.sendMessage(ChatColor.GRAY + "/dd blocks remove" + ChatColor.WHITE + " - Remove world markers");
            player.sendMessage(ChatColor.GRAY + "/dd blocks reload" + ChatColor.WHITE + " - Remove and replace markers");
            player.sendMessage(ChatColor.GRAY + "/dd kill-entities" + ChatColor.WHITE + " - Kill all non-player entities");
            player.sendMessage("");
            player.sendMessage(ChatColor.YELLOW + "Nation Administration:");
            player.sendMessage(ChatColor.GRAY + "/dd nation set <player> <nation|none>" + ChatColor.WHITE + " - Set player's nation");
            player.sendMessage(ChatColor.GRAY + "/dd nation remove <player>" + ChatColor.WHITE + " - Remove player from nation");
            player.sendMessage(ChatColor.GRAY + "/dd role add <player> <role>" + ChatColor.WHITE + " - Assign player roles");
            player.sendMessage(ChatColor.GRAY + "/dd toggle join/leave" + ChatColor.WHITE + " - Toggle player permissions");
        }
        
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "💡 Use " + ChatColor.YELLOW + "/dd help" + ChatColor.GRAY + " for a complete command overview!");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(Arrays.asList("help", "reload", "blocks", "kill-entities"));
            if (sender.hasPermission("rocket.reload")) {
                completions.addAll(Arrays.asList("nation", "nations", "toggle", "role", "roles"));
            }
            return completions;
        }
        
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("reload")) {
                return Arrays.asList("config", "blocks", "items", "messages", "antiair", "disasters");
            } else if (subCommand.equals("blocks")) {
                return Arrays.asList("place", "remove", "reload");
            } else if ((subCommand.equals("nation") || subCommand.equals("nations")) && sender.hasPermission("rocket.reload")) {
                return Arrays.asList("set", "remove", "info");
            } else if (subCommand.equals("toggle") && sender.hasPermission("rocket.reload")) {
                return Arrays.asList("join", "leave", "status");
            } else if ((subCommand.equals("role") || subCommand.equals("roles")) && sender.hasPermission("rocket.reload")) {
                return Arrays.asList("add", "remove", "list", "window", "assign", "reset");
            }
        }
        
        if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            if ((subCommand.equals("nation") || subCommand.equals("nations")) && sender.hasPermission("rocket.reload")) {
                // Return online player names
                List<String> playerNames = new ArrayList<>();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    playerNames.add(player.getName());
                }
                return playerNames;
            } else if ((subCommand.equals("role") || subCommand.equals("roles")) && sender.hasPermission("rocket.reload")) {
                String action = args[1].toLowerCase();
                if (action.equals("add") || action.equals("remove")) {
                    // Return online player names
                    List<String> playerNames = new ArrayList<>();
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        playerNames.add(player.getName());
                    }
                    return playerNames;
                } else if (action.equals("list")) {
                    // Return nation names
                    return new ArrayList<>(nationManager.getAllNations().keySet());
                }
            }
        }
        
        if (args.length == 4) {
            String subCommand = args[0].toLowerCase();
            String action = args[1].toLowerCase();
            if ((subCommand.equals("nation") || subCommand.equals("nations")) && action.equals("set") && sender.hasPermission("rocket.reload")) {
                List<String> nations = new ArrayList<>(nationManager.getAllNations().keySet());
                nations.add("none");
                return nations;
            } else if ((subCommand.equals("role") || subCommand.equals("roles")) && action.equals("add") && sender.hasPermission("rocket.reload")) {
                // Return role names
                return Arrays.asList("President", "ArmyChief", "Soldier", "Builder", "Healer", "Trader");
            }
        }
        
        return new ArrayList<>();
    }
}