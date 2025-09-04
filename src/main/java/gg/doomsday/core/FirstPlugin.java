package gg.doomsday.core;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import gg.doomsday.core.services.MissileService;
import gg.doomsday.core.managers.MessageManager;
import gg.doomsday.core.managers.BlockManager;
import gg.doomsday.core.defense.AntiAirDefenseManager;
import gg.doomsday.core.defense.ReinforcedBlockManager;
import gg.doomsday.core.explosions.ExplosionHandler;
import gg.doomsday.core.explosions.RocketLauncher;
import gg.doomsday.core.items.ReinforcementHandler;
import gg.doomsday.core.items.ReinforcementDetectorManager;
import gg.doomsday.core.nations.NationManager;
import gg.doomsday.core.nations.NationPlayerManager;
import gg.doomsday.core.gui.NationGUI;
import gg.doomsday.core.seasons.SeasonManager;
import gg.doomsday.core.commands.SeasonCommand;
import gg.doomsday.core.listeners.PlayerJoinListener;
import gg.doomsday.core.scoreboard.GameScoreboard;
import gg.doomsday.core.messaging.MessagingManager;
import gg.doomsday.core.gui.utils.ItemBuilder;

import java.util.UUID;

public final class DoomsdayCore extends JavaPlugin {

    private ExplosionHandler explosionHandler;
    private RocketLauncher rocketLauncher;
    private ReinforcedBlockManager reinforcedBlockManager;
    private ReinforcementHandler reinforcementHandler;
    private AntiAirDefenseManager antiAirManager;
    private ReinforcementDetectorManager detectorManager;
    private gg.doomsday.core.utils.ColorChatHandler colorChatHandler;
    private gg.doomsday.core.utils.ReloadCommandHandler reloadCommandHandler;
    private MessageManager messageManager;
    private MissileService missileService;
    private GUIManager guiManager;
    private BlockManager blockManager;
    private NationManager nationManager;
    private NationPlayerManager nationPlayerManager;
    private NationGUI nationGUI;
    private SeasonManager seasonManager;
    private GameScoreboard gameScoreboard;
    private MessagingManager messagingManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        // Initialize message manager
        messageManager = new MessageManager(this);
        
        // Initialize reinforced blocks system
        reinforcedBlockManager = new ReinforcedBlockManager(this);
        reinforcementHandler = new ReinforcementHandler(this, reinforcedBlockManager);
        
        // Initialize detector manager
        detectorManager = new ReinforcementDetectorManager(this, reinforcedBlockManager, reinforcementHandler.getCustomItemManager());
        
        // Initialize anti-air defense system
        antiAirManager = new AntiAirDefenseManager(this);
        
        // Initialize explosion handler with reinforced blocks support
        explosionHandler = new ExplosionHandler(this, reinforcedBlockManager);
        rocketLauncher = new RocketLauncher(this, explosionHandler, reinforcedBlockManager, antiAirManager);
        
        // Initialize utility handlers
        colorChatHandler = new gg.doomsday.core.utils.ColorChatHandler(this);
        reloadCommandHandler = new gg.doomsday.core.utils.ReloadCommandHandler(this);
        
        // Initialize missile service (centralized missile operations)
        missileService = new MissileService(this, rocketLauncher, messageManager);
        
        // Initialize nation manager with disaster system
        nationManager = new NationManager(this);
        
        // Initialize nation player manager
        nationPlayerManager = new NationPlayerManager(this, nationManager);
        
        // Connect nation player manager to nation manager for disaster effects
        nationManager.setNationPlayerManager(nationPlayerManager);
        
        // Initialize messaging manager
        messagingManager = new MessagingManager(this, nationPlayerManager);
        
        // Initialize nation GUI
        nationGUI = new NationGUI(this, nationManager, nationPlayerManager);
        
        // Initialize season system
        seasonManager = new SeasonManager(this);
        
        // Validate active season - disable plugin if invalid
        try {
            seasonManager.validateActiveSeason();
        } catch (IllegalStateException e) {
            getLogger().severe("SEASON VALIDATION FAILED: " + e.getMessage());
            getLogger().severe("Plugin will be disabled. Fix the season configuration and restart.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize GUI manager
        guiManager = new GUIManager(this, reinforcementHandler, detectorManager, antiAirManager, missileService, nationManager);
        
        // Initialize block manager
        blockManager = new BlockManager(this, antiAirManager, guiManager);
        
        // Place missile and anti-air blocks on startup
        Bukkit.getScheduler().runTaskLater(this, () -> {
            // Get default world or first available world
            org.bukkit.World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
            if (world != null) {
                blockManager.placeAllBlocks(world);
                getLogger().info("Placed missile and anti-air blocks on startup");
            } else {
                getLogger().warning("No world available for placing blocks on startup");
            }
        }, 20L); // Delay 1 second to ensure world is loaded
        
        // Initialize scoreboard system
        gameScoreboard = new GameScoreboard(this, seasonManager, nationManager, nationPlayerManager);
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(explosionHandler, this);
        getServer().getPluginManager().registerEvents(reinforcementHandler, this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(seasonManager, gameScoreboard), this);
        
        // Register command executors
        getCommand("cc").setExecutor(colorChatHandler);
        getCommand("rr").setExecutor(reloadCommandHandler);
        getCommand("season").setExecutor(new SeasonCommand(seasonManager));
        
        // Start periodic cleanup task for reinforced blocks (every 5 minutes)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (reinforcedBlockManager != null) {
                    reinforcedBlockManager.cleanupInvalidReinforcements();
                }
            }
        }.runTaskTimer(this, 20L * 60 * 5, 20L * 60 * 5); // 5 minutes = 6000 ticks
        
        getLogger().info("Rocket plugin enabled!");
        getLogger().info("Reinforced blocks system initialized with " + reinforcedBlockManager.getReinforcedBlockCount() + " blocks");
        getLogger().info("Anti-air defense system initialized with " + antiAirManager.getDefenses().size() + " defense installations");
        getLogger().info("Nation system initialized with " + nationManager.getAllNations().size() + " nations and natural disasters");
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }
    
    public RocketLauncher getRocketLauncher() {
        return rocketLauncher;
    }
    
    public MissileService getMissileService() {
        return missileService;
    }
    
    public NationManager getNationManager() {
        return nationManager;
    }
    
    public MessagingManager getMessagingManager() {
        return messagingManager;
    }
    
    public NationPlayerManager getNationPlayerManager() {
        return nationPlayerManager;
    }
    
    @Override
    public void onDisable() {
        // Save reinforced blocks data
        if (reinforcedBlockManager != null) {
            reinforcedBlockManager.saveReinforcedBlocks();
        }
        
        // Shutdown detector manager
        if (detectorManager != null) {
            detectorManager.shutdown();
        }
        
        // Shutdown nation player manager
        if (nationPlayerManager != null) {
            nationPlayerManager.saveConfiguration();
        }
        
        // Shutdown nation manager
        if (nationManager != null) {
            nationManager.shutdown();
        }
        
        // Shutdown scoreboard system
        if (gameScoreboard != null) {
            gameScoreboard.shutdown();
        }
        
        // Unregister custom recipe
        if (reinforcementHandler != null) {
            reinforcementHandler.unregisterRecipe();
        }
        
        getLogger().info("Rocket plugin disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Handle nation commands
        if (command.getName().equalsIgnoreCase("nation") || command.getName().equalsIgnoreCase("nations") || command.getName().equalsIgnoreCase("n")) {
            return handleNationCommand(sender, args);
        }
        
        if (!command.getName().equalsIgnoreCase("rocket") && !command.getName().equalsIgnoreCase("r")) return false;

        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only!");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("Usage: /r <rocketName> | /r r | /r c | /r o | /r show | /r powder [amount] | /r helmet [amount] | /r aa [cmd] | /r nation");
            sender.sendMessage("Available missiles: Use /r show to see all configured missiles");
            sender.sendMessage("Commands: /r r (reload all configs), /r c (reset environment), /r o (view config), /r show (open GUI)");
            sender.sendMessage("Reinforcement: /r powder [amount] (get reinforcement powder), /r helmet [amount] (get detection helmet)");
            sender.sendMessage("Config: /r items (reload custom items only)");
            sender.sendMessage("Anti-Air: /r aa <status|reload|online|offline> [name]");
            sender.sendMessage("Nations: /r nation (check current location and nation info)");
            sender.sendMessage("GUI: /r show (opens interactive control panel)");
            return true;
        }

        // /rocket reload or /rocket r
        if (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("r")) {
            if (!sender.hasPermission("rocket.reload")) {
                sender.sendMessage("You don't have permission to reload the config!");
                return true;
            }
            
            // Reload configurations without calling reloadConfig() 
            // This uses in-memory objects instead of reloading from disk
            
            // Reload custom items config
            reinforcementHandler.reloadCustomItems();
            // Reload anti-air defenses
            antiAirManager.reloadDefenses();
            // Reload messages
            messageManager.reloadMessages();
            // Reload nation system
            nationManager.reload();
            // Reload nation player manager
            nationPlayerManager.reload();
            // Reload messaging manager
            messagingManager.reload();
            // Place physical blocks for missiles and anti-air
            if (sender instanceof Player) {
                blockManager.placeAllBlocks(((Player) sender).getWorld());
                sender.sendMessage("§a§lPlaced physical blocks for missiles and anti-air defenses!");
            }
            
            sender.sendMessage(messageManager.getMessage("commands.reload.success"));
            for (String line : messageManager.getMessageList("commands.reload.config_list")) {
                sender.sendMessage(line);
            }
            return true;
        }

        // /rocket options or /rocket o
        if (args[0].equalsIgnoreCase("options") || args[0].equalsIgnoreCase("o")) {
            if (!sender.hasPermission("rocket.use")) {
                sender.sendMessage("You don't have permission to view config!");
                return true;
            }
            displayConfigOptions(sender);
            return true;
        }

        // /rocket items - reload custom items config
        if (args[0].equalsIgnoreCase("items") || args[0].equalsIgnoreCase("i")) {
            if (!sender.hasPermission("rocket.reload")) {
                sender.sendMessage("You don't have permission to reload configs!");
                return true;
            }
            
            reinforcementHandler.reloadCustomItems();
            sender.sendMessage("§a🔄 Custom items configuration reloaded!");
            return true;
        }

        // /rocket aa - anti-air defense commands
        if (args[0].equalsIgnoreCase("aa") || args[0].equalsIgnoreCase("antiair")) {
            if (!sender.hasPermission("rocket.antiair")) {
                sender.sendMessage("You don't have permission to manage anti-air defenses!");
                return true;
            }
            
            if (args.length < 2) {
                sender.sendMessage("Usage: /r aa <status|reload|online|offline> [name]");
                return true;
            }
            
            String subCmd = args[1].toLowerCase();
            
            if (subCmd.equals("status")) {
                for (String line : antiAirManager.getDefenseStatus()) {
                    sender.sendMessage(line);
                }
                return true;
            }
            
            if (subCmd.equals("reload")) {
                antiAirManager.reloadAllDefenses();
                sender.sendMessage("§a✅ Anti-air defenses have unlimited ammo!");
                return true;
            }
            
            if (args.length < 3) {
                sender.sendMessage("Usage: /r aa " + subCmd + " <defense_name>");
                return true;
            }
            
            String defenseName = args[2];
            
            if (subCmd.equals("online")) {
                antiAirManager.setDefenseOperational(defenseName, true);
                sender.sendMessage("§a✅ Defense '" + defenseName + "' brought online!");
                return true;
            }
            
            if (subCmd.equals("offline")) {
                antiAirManager.setDefenseOperational(defenseName, false);
                sender.sendMessage("§c❌ Defense '" + defenseName + "' taken offline!");
                return true;
            }
            
            sender.sendMessage("Unknown anti-air command: " + subCmd);
            return true;
        }

        Player player = (Player) sender;
        World world = player.getWorld();

        // /rocket powder [amount] - give reinforcement powder
        if (args[0].equalsIgnoreCase("powder") || args[0].equalsIgnoreCase("p")) {
            if (!sender.hasPermission("rocket.powder")) {
                sender.sendMessage("You don't have permission to get reinforcement powder!");
                return true;
            }
            
            int amount = 1;
            if (args.length > 1) {
                try {
                    amount = Integer.parseInt(args[1]);
                    if (amount < 1 || amount > 64) {
                        sender.sendMessage("Amount must be between 1 and 64!");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage("Invalid amount! Use a number between 1 and 64.");
                    return true;
                }
            }
            
            player.getInventory().addItem(reinforcementHandler.getReinforcementPowder(amount));
            sender.sendMessage("§a§lGiven " + amount + "x Reinforcement Powder!");
            sender.sendMessage("§7Right-click on blocks to reinforce them against explosions.");
            return true;
        }

        // /rocket helmet [amount] - give reinforcement detector helmet
        if (args[0].equalsIgnoreCase("helmet") || args[0].equalsIgnoreCase("h")) {
            if (!sender.hasPermission("rocket.helmet")) {
                sender.sendMessage("You don't have permission to get detector helmets!");
                return true;
            }
            
            int amount = 1;
            if (args.length > 1) {
                try {
                    amount = Integer.parseInt(args[1]);
                    if (amount < 1 || amount > 64) {
                        sender.sendMessage("Amount must be between 1 and 64!");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage("Invalid amount! Use a number between 1 and 64.");
                    return true;
                }
            }
            
            player.getInventory().addItem(detectorManager.createDetectorHelmet(amount));
            sender.sendMessage("§b§lGiven " + amount + "x Reinforcement Detection Helmet!");
            sender.sendMessage("§7Wear this helmet to see reinforced blocks with particles.");
            return true;
        }

        // /rocket c - create testing environment and give GUI items
        if (args[0].equalsIgnoreCase("c")) {
            getLogger().info("Handling 'c' command");
            if (!sender.hasPermission("rocket.use")) {
                sender.sendMessage("You don't have permission to use this command!");
                return true;
            }
            setupTestingEnvironment(player.getWorld());
            clearInventoryAndGiveGUIItems(player);
            sender.sendMessage("§a§l[DD] Environment reset complete!");
            return true;
        }

        // /rocket show - open GUI
        if (args[0].equalsIgnoreCase("show") || args[0].equalsIgnoreCase("gui")) {
            if (!sender.hasPermission("rocket.use")) {
                sender.sendMessage("You don't have permission to use this command!");
                return true;
            }
            guiManager.openMainGUI(player);
            return true;
        }

        // /rocket nation - show nation info
        if (args[0].equalsIgnoreCase("nation") || args[0].equalsIgnoreCase("n")) {
            if (!sender.hasPermission("rocket.use")) {
                sender.sendMessage("You don't have permission to use this command!");
                return true;
            }
            
            Location loc = player.getLocation();
            gg.doomsday.core.nations.Nation nation = nationManager.getNationAt(loc);
            
            sender.sendMessage("§6§l=== NATION INFORMATION ===");
            sender.sendMessage("§7Your location: §f" + String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ()));
            
            if (nation != null) {
                sender.sendMessage("§7Current nation: §a" + nation.getDisplayName());
                sender.sendMessage("§7Nation borders: §f" + nation.getBorders());
                
                sender.sendMessage("§7Active disasters:");
                boolean hasActiveDisasters = false;
                for (gg.doomsday.core.nations.Disaster disaster : nation.getDisasters().values()) {
                    if (disaster.isActive()) {
                        sender.sendMessage("§7  §c" + disaster.getId().replace("_", " ").toUpperCase() + 
                            " §7(ends in " + ((disaster.getEndTime() - System.currentTimeMillis()) / 1000) + "s)");
                        hasActiveDisasters = true;
                    }
                }
                if (!hasActiveDisasters) {
                    sender.sendMessage("§7  §aNo active disasters");
                }
                
                sender.sendMessage("§7Possible disasters: §f" + nation.getDisasters().keySet().toString());
            } else {
                sender.sendMessage("§7Current nation: §cNone (Outside all nation borders)");
                sender.sendMessage("§7Available nations:");
                for (gg.doomsday.core.nations.Nation n : nationManager.getAllNations().values()) {
                    sender.sendMessage("§7  §6" + n.getDisplayName() + " §7- " + n.getBorders());
                }
            }
            
            return true;
        }

        // /rocket <rocketName> - Use centralized missile service
        String rocketName = args[0];
        missileService.launchMissileViaCommand(player, rocketName);

        return true;
    }

    private void displayConfigOptions(CommandSender sender) {
        sender.sendMessage("§6§l=== ROCKET PLUGIN CONFIG ===");
        sender.sendMessage("");
        
        sender.sendMessage("§e🔊 Sound Settings:");
        sender.sendMessage("§7  Sound Radius: §f" + getConfig().getDouble("soundRadius", 100.0) + " blocks");
        sender.sendMessage("");
        
        sender.sendMessage("§e🚀 Available Rockets:");
        ConfigurationSection rocketsSection = getConfig().getConfigurationSection("rockets");
        if (rocketsSection != null) {
            for (String rocketKey : rocketsSection.getKeys(false)) {
                ConfigurationSection rocket = rocketsSection.getConfigurationSection(rocketKey);
                if (rocket != null) {
                    String displayName = rocket.getString("displayName", rocketKey.toUpperCase());
                    String explosionType = rocket.getString("explosionType", "DEFAULT");
                    double speed = rocket.getDouble("speed", 1.0);
                    
                    sender.sendMessage("§7  §a" + rocketKey + "§7: §f" + displayName);
                    sender.sendMessage("§7    Type: §6" + explosionType + " §7| Speed: §6" + speed);
                }
            }
        }
        sender.sendMessage("");
        
        sender.sendMessage("§e🛡️ Reinforced Block System:");
        sender.sendMessage("§7  Active reinforced blocks: §a" + reinforcedBlockManager.getReinforcedBlockCount());
        sender.sendMessage("§7  Recipe: §f1 Iron Ingot + 1 Stone → 1 Reinforcement Powder (shapeless)");
        sender.sendMessage("");
        
        sender.sendMessage("§e⚒️ Reinforcement Resistance (when reinforced):");
        ConfigurationSection reinforcementSection = getConfig().getConfigurationSection("reinforcement.resistance");
        if (reinforcementSection != null) {
            for (String material : reinforcementSection.getKeys(false)) {
                double resistance = reinforcementSection.getDouble(material, 0.0);
                int percentage = (int)(resistance * 100);
                
                String resistanceColor = "§c";
                if (percentage >= 40) resistanceColor = "§a";
                else if (percentage >= 25) resistanceColor = "§e";
                else if (percentage >= 15) resistanceColor = "§6";
                
                sender.sendMessage("§7  " + material.replace("_", " ") + ": " + resistanceColor + percentage + "%");
            }
        } else {
            sender.sendMessage("§7  No reinforcement resistance configured");
        }
        
        sender.sendMessage("");
        sender.sendMessage("§8§l================================");
    }

    private void setupTestingEnvironment(World world) {
        getLogger().info("Setting up testing environment...");
        
        // Set world conditions for testing
        world.setTime(1000);
        world.setStorm(false);
        world.setThundering(false);
        
        getLogger().info("Testing environment setup complete!");
    }

    private void cleanupDisplayBlocks(World world) {
        int removedCount = 0;
        
        // Remove all BlockDisplay entities (mushroom clouds, rocket displays, etc.)
        for (Entity entity : world.getEntities()) {
            if (entity instanceof BlockDisplay) {
                entity.remove();
                removedCount++;
            }
        }
        
        getLogger().info("Removed " + removedCount + " display blocks from world: " + world.getName());
    }
    
    private void clearInventoryAndGiveGUIItems(Player player) {
        // Clear inventory
        player.getInventory().clear();
        
        // Create Nation GUI item
        ItemStack nationItem = new ItemBuilder(Material.COMPASS)
            .setDisplayName("§6Nation Information")
            .setLore(
                "§7Click to open nation information",
                "§8Command: /nation"
            )
            .build();
        
        // Create Rocket Show GUI item
        ItemStack rocketShowItem = new ItemBuilder(Material.FIREWORK_ROCKET)
            .setDisplayName("§cRocket Control Panel")
            .setLore(
                "§7Click to open rocket GUI",
                "§8Command: /rocket show"
            )
            .build();
        
        // Add items to inventory
        player.getInventory().addItem(nationItem);
        player.getInventory().addItem(rocketShowItem);
    }
    
    private boolean handleNationCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cPlayers only!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("rocket.use")) {
            player.sendMessage("§cYou don't have permission to use nation commands!");
            return true;
        }
        
        // /nation with no args - smart behavior based on player's nation status
        if (args.length == 0) {
            UUID playerId = player.getUniqueId();
            String currentNation = nationPlayerManager.getPlayerNation(playerId);
            
            if (currentNation == null) {
                // Player not in nation - show nation selection
                nationGUI.openNationSelectionGUI(player);
            } else {
                // Player in nation - show nations list for exploration
                nationGUI.openNationsListGUI(player);
            }
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "leave":
                handleLeaveNationCommand(player);
                break;
                
            default:
                player.sendMessage("§6§lNation Commands:");
                player.sendMessage("§7/nation §f- Smart nation GUI (selection or list)");
                player.sendMessage("§7/nation leave §f- Leave your current nation");
                break;
        }
        
        return true;
    }
    
    private void showPlayerNationInfo(Player player) {
        java.util.UUID playerId = player.getUniqueId();
        String currentNationId = nationPlayerManager.getPlayerNation(playerId);
        Location loc = player.getLocation();
        gg.doomsday.core.nations.Nation locationNation = nationManager.getNationAt(loc);
        
        player.sendMessage("§6§l=== NATION INFORMATION ===");
        player.sendMessage("§7Your location: §f" + String.format("%.1f, %.1f, %.1f", loc.getX(), loc.getY(), loc.getZ()));
        
        if (currentNationId != null) {
            gg.doomsday.core.nations.Nation nation = nationManager.getAllNations().get(currentNationId);
            if (nation != null) {
                player.sendMessage("§7Your nation: §a" + nation.getDisplayName());
                player.sendMessage("§7Member since: §f" + new java.util.Date(nationPlayerManager.getPlayerJoinDate(playerId)));
                player.sendMessage("§7Nation citizens: §f" + nation.getTotalPlayers());
                player.sendMessage("§7Can switch nations: " + (nationPlayerManager.canPlayerSwitch(playerId) ? "§aYes" : "§cNo"));
                
                player.sendMessage("§7Available missiles:");
                for (String missile : nation.getMissileTypes()) {
                    player.sendMessage("§7  §6• §f" + missile);
                }
            }
        } else {
            player.sendMessage("§7Your nation: §cNone");
            player.sendMessage("§eUse §f/nation gui §eto join a nation!");
        }
        
        if (locationNation != null) {
            if (currentNationId == null || !locationNation.getId().equals(currentNationId)) {
                player.sendMessage("§7Currently in: §e" + locationNation.getDisplayName() + " territory");
            }
        } else {
            player.sendMessage("§7Currently in: §7Neutral territory");
        }
        
        player.sendMessage("§7Use §f/nation gui §7to manage your nation membership.");
    }
    
    private void handleLeaveNationCommand(Player player) {
        java.util.UUID playerId = player.getUniqueId();
        String currentNation = nationPlayerManager.getPlayerNation(playerId);
        
        if (currentNation == null) {
            player.sendMessage("§cYou are not in any nation!");
            return;
        }
        
        if (!nationPlayerManager.canPlayerSwitch(playerId)) {
            player.sendMessage("§cYou cannot leave your current nation!");
            long cooldown = nationPlayerManager.getSwitchCooldown();
            if (cooldown > 0) {
                long timeLeft = (nationPlayerManager.getPlayerJoinDate(playerId) + cooldown) - System.currentTimeMillis();
                if (timeLeft > 0) {
                    long hours = timeLeft / (1000 * 60 * 60);
                    long minutes = (timeLeft % (1000 * 60 * 60)) / (1000 * 60);
                    player.sendMessage("§7Cooldown remaining: §f" + hours + "h " + minutes + "m");
                }
            }
            return;
        }
        
        if (nationPlayerManager.leaveNation(player, true)) {
            gg.doomsday.core.nations.Nation nation = nationManager.getAllNations().get(currentNation);
            String nationName = nation != null ? nation.getDisplayName() : currentNation;
            
            player.sendMessage("§eYou have left §f" + nationName + "§e.");
            player.sendMessage("§7Use §f/nation gui §7to join a new nation.");
        } else {
            player.sendMessage("§cFailed to leave your nation!");
        }
    }

}