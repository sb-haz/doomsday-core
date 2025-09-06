package gg.doomsday.core.commands;

import gg.doomsday.core.config.ConfigManager;
import gg.doomsday.core.services.MissileService;
import gg.doomsday.core.managers.MessageManager;
import gg.doomsday.core.defense.ReinforcedBlockManager;
import gg.doomsday.core.items.ReinforcementHandler;
import gg.doomsday.core.items.ReinforcementDetectorManager;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RocketCommand implements CommandExecutor, TabCompleter {
    
    private final JavaPlugin plugin;
    private final MissileService missileService;
    private final MessageManager messageManager;
    private final ReinforcedBlockManager reinforcedBlockManager;
    private final ReinforcementHandler reinforcementHandler;
    private final ReinforcementDetectorManager detectorManager;
    
    public RocketCommand(JavaPlugin plugin, MissileService missileService, MessageManager messageManager,
                        ReinforcedBlockManager reinforcedBlockManager, ReinforcementHandler reinforcementHandler,
                        ReinforcementDetectorManager detectorManager) {
        this.plugin = plugin;
        this.missileService = missileService;
        this.messageManager = messageManager;
        this.reinforcedBlockManager = reinforcedBlockManager;
        this.reinforcementHandler = reinforcementHandler;
        this.detectorManager = detectorManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("rocket")) {
            return false;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Players only!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("rocket.use")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use rocket commands!");
            return true;
        }
        
        if (args.length == 0) {
            showUsage(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                return handleReload(player);
            case "options":
            case "config":
                return handleOptions(player);
            case "powder":
                return handlePowder(player, args);
            case "helmet":
                return handleHelmet(player, args);
            case "items":
                return handleItems(player);
            default:
                // Try to launch missile
                return handleMissileLaunch(player, subCommand);
        }
    }
    
    private boolean handleReload(Player player) {
        if (!player.hasPermission("rocket.reload")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to reload configurations!");
            return true;
        }
        
        // Reload all configurations using ConfigManager
        gg.doomsday.core.DoomsdayCore doomsdayPlugin = (gg.doomsday.core.DoomsdayCore) plugin;
        ConfigManager configManager = doomsdayPlugin.getConfigManager();
        
        // Reload all config files
        configManager.reloadAllConfigs();
        
        // Reload other systems that depend on configs
        reinforcementHandler.reloadCustomItems();
        messageManager.reloadMessages();
        reinforcedBlockManager.reloadConfiguration();
        
        // Reload messaging configuration
        doomsdayPlugin.getMessagingManager().loadConfiguration();
        
        player.sendMessage(messageManager.getMessage("commands.reload.success"));
        for (String line : messageManager.getMessageList("commands.reload.config_list")) {
            player.sendMessage(line);
        }
        return true;
    }
    
    private boolean handleOptions(Player player) {
        player.sendMessage(messageManager.getMessage("commands.rocket.options_header"));
        player.sendMessage("");
        
        player.sendMessage(ChatColor.YELLOW + "🔊 Sound Settings:");
        player.sendMessage(ChatColor.GRAY + "  Sound Radius: " + ChatColor.WHITE + plugin.getConfig().getDouble("soundRadius", 100.0) + " blocks");
        player.sendMessage("");
        
        player.sendMessage(ChatColor.YELLOW + "🚀 Available Missiles:");
        ConfigurationSection rocketsSection = plugin.getConfig().getConfigurationSection("rockets");
        if (rocketsSection != null) {
            for (String rocketKey : rocketsSection.getKeys(false)) {
                ConfigurationSection rocket = rocketsSection.getConfigurationSection(rocketKey);
                if (rocket != null) {
                    String displayName = rocket.getString("displayName", rocketKey.toUpperCase());
                    String explosionType = rocket.getString("explosionType", "DEFAULT");
                    double speed = rocket.getDouble("speed", 1.0);
                    
                    player.sendMessage(ChatColor.GRAY + "  " + ChatColor.GREEN + rocketKey + ChatColor.GRAY + ": " + ChatColor.WHITE + displayName);
                    player.sendMessage(ChatColor.GRAY + "    Type: " + ChatColor.GOLD + explosionType + ChatColor.GRAY + " | Speed: " + ChatColor.GOLD + speed);
                }
            }
        }
        player.sendMessage("");
        
        player.sendMessage(ChatColor.YELLOW + "🛡️ Reinforcement System:");
        player.sendMessage(ChatColor.GRAY + "  Active reinforced blocks: " + ChatColor.GREEN + reinforcedBlockManager.getReinforcedBlockCount());
        player.sendMessage(ChatColor.GRAY + "  Recipe: " + ChatColor.WHITE + "1 Iron Ingot + 1 Stone → 1 Reinforcement Powder (shapeless)");
        
        return true;
    }
    
    private boolean handlePowder(Player player, String[] args) {
        if (!player.hasPermission("rocket.powder")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to get reinforcement powder!");
            return true;
        }
        
        int amount = 1;
        if (args.length > 1) {
            try {
                amount = Integer.parseInt(args[1]);
                if (amount < 1 || amount > 64) {
                    player.sendMessage(ChatColor.RED + "Amount must be between 1 and 64!");
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid amount! Use a number between 1 and 64.");
                return true;
            }
        }
        
        player.getInventory().addItem(reinforcementHandler.getReinforcementPowder(amount));
        player.sendMessage(messageManager.getMessage("commands.rocket.powder_given").replace("{amount}", String.valueOf(amount)));
        player.sendMessage(messageManager.getMessage("commands.rocket.powder_usage"));
        return true;
    }
    
    private boolean handleHelmet(Player player, String[] args) {
        if (!player.hasPermission("rocket.helmet")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to get detector helmets!");
            return true;
        }
        
        int amount = 1;
        if (args.length > 1) {
            try {
                amount = Integer.parseInt(args[1]);
                if (amount < 1 || amount > 64) {
                    player.sendMessage(ChatColor.RED + "Amount must be between 1 and 64!");
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid amount! Use a number between 1 and 64.");
                return true;
            }
        }
        
        player.getInventory().addItem(detectorManager.createDetectorHelmet(amount));
        player.sendMessage(messageManager.getMessage("commands.rocket.helmet_given").replace("{amount}", String.valueOf(amount)));
        player.sendMessage(messageManager.getMessage("commands.rocket.helmet_usage"));
        return true;
    }
    
    private boolean handleItems(Player player) {
        if (!player.hasPermission("rocket.reload")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to reload configs!");
            return true;
        }
        
        reinforcementHandler.reloadCustomItems();
        player.sendMessage(ChatColor.GREEN + "🔄 Custom items configuration reloaded!");
        return true;
    }
    
    private boolean handleMissileLaunch(Player player, String missileType) {
        ConfigurationSection rocketsSection = plugin.getConfig().getConfigurationSection("rockets");
        if (rocketsSection == null || !rocketsSection.contains(missileType)) {
            player.sendMessage(ChatColor.RED + "Unknown missile type: " + missileType);
            player.sendMessage(ChatColor.GRAY + "Use /rocket options to see available missiles");
            return true;
        }
        
        missileService.launchMissileViaCommand(player, missileType);
        return true;
    }
    
    private void showUsage(Player player) {
        player.sendMessage(messageManager.getMessage("commands.rocket.usage_header"));
        player.sendMessage(ChatColor.YELLOW + "/rocket <missile_type>" + ChatColor.GRAY + " - Launch a specific missile");
        player.sendMessage(ChatColor.YELLOW + "/rocket options" + ChatColor.GRAY + " - View missile configurations");
        
        if (player.hasPermission("rocket.reload")) {
            player.sendMessage(ChatColor.YELLOW + "/rocket reload" + ChatColor.GRAY + " - Reload all configurations");
            player.sendMessage(ChatColor.YELLOW + "/rocket items" + ChatColor.GRAY + " - Reload custom items only");
        }
        
        if (player.hasPermission("rocket.powder")) {
            player.sendMessage(ChatColor.YELLOW + "/rocket powder [amount]" + ChatColor.GRAY + " - Get reinforcement powder");
        }
        
        if (player.hasPermission("rocket.helmet")) {
            player.sendMessage(ChatColor.YELLOW + "/rocket helmet [amount]" + ChatColor.GRAY + " - Get detection helmets");
        }
        
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "Available missiles: Use /rocket options to view all configured missiles");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            
            // Add subcommands
            completions.addAll(Arrays.asList("reload", "options", "config", "powder", "helmet", "items"));
            
            // Add configured missile names
            ConfigurationSection rocketsSection = plugin.getConfig().getConfigurationSection("rockets");
            if (rocketsSection != null) {
                completions.addAll(rocketsSection.getKeys(false));
            }
            
            return completions;
        }
        
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("powder") || subCommand.equals("helmet")) {
                return Arrays.asList("1", "2", "4", "8", "16", "32", "64");
            }
        }
        
        return new ArrayList<>();
    }
}