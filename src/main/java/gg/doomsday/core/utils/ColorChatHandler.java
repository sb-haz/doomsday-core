package gg.doomsday.core.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import gg.doomsday.core.utils.GradientUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ColorChatHandler implements CommandExecutor, TabCompleter {
    
    private final JavaPlugin plugin;

    public ColorChatHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("cc")) {
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only!");
            return true;
        }

        if (args.length == 0) {
            // Send empty line to chat
            for (Player worldPlayer : ((Player) sender).getWorld().getPlayers()) {
                worldPlayer.sendMessage("");
            }
            return true;
        }

        Player player = (Player) sender;
        
        // Join all arguments into one message
        String message = String.join(" ", args);
        
        // First parse gradients, then apply legacy color codes
        String gradientMessage = GradientUtils.parseGradients(message);
        String coloredMessage = ChatColor.translateAlternateColorCodes('&', gradientMessage);
        
        // Send to all players in the world (just the colored message)
        for (Player worldPlayer : player.getWorld().getPlayers()) {
            worldPlayer.sendMessage(coloredMessage);
        }
        
        // Log to console
        plugin.getLogger().info("[ColorChat] " + player.getName() + ": " + ChatColor.stripColor(coloredMessage));
        
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            
            // Add legacy color codes
            completions.addAll(Arrays.asList(
                "&4Red", "&cLight_Red", "&6Gold", "&eYellow", "&2Dark_Green", "&aGreen", 
                "&bAqua", "&3Dark_Aqua", "&1Dark_Blue", "&9Blue", "&dLight_Purple", "&5Dark_Purple", 
                "&fWhite", "&7Gray", "&8Dark_Gray", "&0Black", 
                "&lBold", "&oItalic", "&nUnderline", "&mStrikethrough", "&kMagic", "&rReset"
            ));
            
            // Add gradient examples
            completions.addAll(Arrays.asList(
                "<gradient:#ff0000:#0000ff>Text</gradient>",
                "<gradient:#ff0000:#ffffff:#0000ff>America</gradient>",
                "<gradient:#00ff00:#ffff00>Spring</gradient>",
                "<gradient:#ff6600:#ffff00:#ff6600>Fire</gradient>",
                "<gradient:#000000:#ffffff>Fade</gradient>"
            ));
            
            return completions;
        }
        return new ArrayList<>();
    }
}