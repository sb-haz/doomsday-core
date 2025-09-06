package gg.doomsday.core.utils;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class ReloadCommandHandler implements CommandExecutor, TabCompleter {
    
    private final JavaPlugin plugin;

    public ReloadCommandHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("rr")) {
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only!");
            return true;
        }

        Player player = (Player) sender;
        
        if (!player.hasPermission("rocket.reload")) {
            player.sendMessage("You don't have permission to reload!");
            return true;
        }

        // Execute /reload confirm as the player
        Bukkit.dispatchCommand(player, "plugman reload Doomsday");
        
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}