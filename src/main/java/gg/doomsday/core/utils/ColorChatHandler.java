package gg.doomsday.core.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ColorChatHandler implements CommandExecutor {
    
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
        
        // Replace & with § for color codes
        String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);
        
        // Send to all players in the world (just the colored message)
        for (Player worldPlayer : player.getWorld().getPlayers()) {
            worldPlayer.sendMessage(coloredMessage);
        }
        
        // Log to console
        plugin.getLogger().info("[ColorChat] " + player.getName() + ": " + ChatColor.stripColor(coloredMessage));
        
        return true;
    }
}