package gg.doomsday.core;

import gg.doomsday.core.defense.AntiAirDefense;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import gg.doomsday.core.config.ConfigManager;
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
import gg.doomsday.core.nations.NationRoleManager;
import gg.doomsday.core.nations.RoleAssignmentScheduler;
import gg.doomsday.core.nations.RoleClaimItemManager;
import gg.doomsday.core.gui.NationGUI;
import gg.doomsday.core.listeners.RoleClaimListener;
import gg.doomsday.core.seasons.SeasonManager;
import gg.doomsday.core.seasons.SeasonEventListener;
import gg.doomsday.core.commands.SeasonCommand;
import gg.doomsday.core.commands.DisasterCommand;
import gg.doomsday.core.commands.RocketCommand;
import gg.doomsday.core.commands.AntiairCommand;
import gg.doomsday.core.commands.NationsCommand;
import gg.doomsday.core.commands.DoomsdayCommand;
import gg.doomsday.core.commands.AICommand;
import gg.doomsday.core.commands.NationChatCommand;
import gg.doomsday.core.listeners.PlayerJoinListener;
import gg.doomsday.core.listeners.AIPlayerListener;
import gg.doomsday.core.listeners.AIChatListener;
import gg.doomsday.core.listeners.CustomChatListener;
import gg.doomsday.core.ai.AIService;
import gg.doomsday.core.data.PlayerDataManager;
import gg.doomsday.core.scoreboard.GameScoreboard;
import gg.doomsday.core.messaging.MessagingManager;
import gg.doomsday.core.gui.utils.ItemBuilder;
import gg.doomsday.core.fuel.MissileFuelManager;
import gg.doomsday.core.fuel.AntiAirFuelManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class DoomsdayCore extends JavaPlugin implements TabCompleter {

    private ConfigManager configManager;
    private ExplosionHandler explosionHandler;
    private RocketLauncher rocketLauncher;
    private ReinforcedBlockManager reinforcedBlockManager;
    private ReinforcementHandler reinforcementHandler;
    private AntiAirDefenseManager antiAirManager;
    private ReinforcementDetectorManager detectorManager;
    private gg.doomsday.core.utils.ColorChatHandler colorChatHandler;
    private MessageManager messageManager;
    private MissileService missileService;
    private GUIManager guiManager;
    private BlockManager blockManager;
    private NationManager nationManager;
    private NationPlayerManager nationPlayerManager;
    private NationRoleManager roleManager;
    private RoleAssignmentScheduler roleScheduler;
    private RoleClaimItemManager roleItemManager;
    private SeasonEventListener seasonEventListener;
    private NationGUI nationGUI;
    private SeasonManager seasonManager;
    private GameScoreboard gameScoreboard;
    private MessagingManager messagingManager;
    private MissileFuelManager fuelManager;
    private AntiAirFuelManager antiAirFuelManager;
    private AIService aiService;
    private PlayerDataManager playerDataManager;
    private CustomChatListener customChatListener;

    @Override
    public void onEnable() {
        try {
            getLogger().info("Starting DoomsdayCore plugin initialization...");
            
            // Initialize configuration manager first
            getLogger().info("Loading configuration manager...");
            configManager = new ConfigManager(this);
            
            // Initialize message manager
            getLogger().info("Loading message manager...");
            messageManager = new MessageManager(this);
        
            // Initialize reinforced blocks system
            getLogger().info("Loading reinforced blocks system...");
            reinforcedBlockManager = new ReinforcedBlockManager(this);
            reinforcementHandler = new ReinforcementHandler(this, reinforcedBlockManager);
            
            // Initialize detector manager
            getLogger().info("Loading detector manager...");
            detectorManager = new ReinforcementDetectorManager(this, reinforcedBlockManager, reinforcementHandler.getCustomItemManager());
            
            // Initialize anti-air defense system
            getLogger().info("Loading anti-air defense system...");
            antiAirManager = new AntiAirDefenseManager(this);
        
            // Initialize explosion handler with reinforced blocks support
            getLogger().info("Loading explosion handler...");
            explosionHandler = new ExplosionHandler(this, reinforcedBlockManager);
            rocketLauncher = new RocketLauncher(this, explosionHandler, reinforcedBlockManager, antiAirManager);
            
            // Initialize utility handlers
            getLogger().info("Loading utility handlers...");
            colorChatHandler = new gg.doomsday.core.utils.ColorChatHandler(this);
            
            // Initialize missile service (centralized missile operations)
            getLogger().info("Loading missile service...");
            missileService = new MissileService(this, rocketLauncher, messageManager);
            
            // Initialize nation manager with disaster system
            getLogger().info("Loading nation manager...");
            nationManager = new NationManager(this);
            
            // Initialize nation player manager
            getLogger().info("Loading nation player manager...");
            nationPlayerManager = new NationPlayerManager(this, nationManager, playerDataManager);
            
            // Connect nation player manager to nation manager for disaster effects
            nationManager.setNationPlayerManager(nationPlayerManager);
            
            // Initialize messaging manager
            getLogger().info("Loading messaging manager...");
            messagingManager = new MessagingManager(this, nationPlayerManager);
            
            // Initialize fuel managers
            getLogger().info("Loading missile fuel manager...");
            fuelManager = new MissileFuelManager(this);
            
            getLogger().info("Loading anti-air fuel manager...");
            antiAirFuelManager = new AntiAirFuelManager(this);
            
            // Initialize player data manager and AI service
            getLogger().info("Loading player data manager...");
            playerDataManager = new PlayerDataManager(this);
            
            getLogger().info("Loading AI service...");
            File aiConfigFile = new File(getDataFolder(), "ai_config.yml");
            if (!aiConfigFile.exists()) {
                saveResource("ai_config.yml", false);
            }
            YamlConfiguration aiConfig = YamlConfiguration.loadConfiguration(aiConfigFile);
            aiService = new AIService(this, aiConfig, playerDataManager);
        
        // Initialize season system first (needed for role management)
        getLogger().info("Loading season system...");
        seasonManager = new SeasonManager(this);
        
        // Initialize role management system
        getLogger().info("Loading role management system...");
        roleManager = new NationRoleManager(this, nationPlayerManager, seasonManager, playerDataManager);
        roleScheduler = new RoleAssignmentScheduler(this, roleManager);
        roleItemManager = new RoleClaimItemManager(this);
        
        // Initialize nation GUI with role manager
        nationGUI = new NationGUI(this, nationManager, nationPlayerManager, roleManager);
        
        // Initialize season event listener for role integration
        seasonEventListener = new SeasonEventListener(this, roleManager, roleScheduler, seasonManager);
        seasonManager.setEventListener(seasonEventListener);
        
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
        blockManager = new BlockManager(this, antiAirManager, guiManager, configManager);
        
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
        getServer().getPluginManager().registerEvents(new AIPlayerListener(this, playerDataManager, aiService), this);
        getServer().getPluginManager().registerEvents(new AIChatListener(this, aiService), this);
        customChatListener = new CustomChatListener(this, nationPlayerManager, nationManager, playerDataManager);
        customChatListener.setRoleManager(roleManager);
        getServer().getPluginManager().registerEvents(customChatListener, this);
        getServer().getPluginManager().registerEvents(new RoleClaimListener(this, roleManager, nationPlayerManager, roleItemManager), this);
        
        // Register new command structure
        RocketCommand rocketCommand = new RocketCommand(this, missileService, messageManager, reinforcedBlockManager, reinforcementHandler, detectorManager);
        getCommand("rocket").setExecutor(rocketCommand);
        getCommand("rocket").setTabCompleter(rocketCommand);
        
        AntiairCommand antiairCommand = new AntiairCommand(this, antiAirManager, messageManager);
        getCommand("antiair").setExecutor(antiairCommand);
        getCommand("antiair").setTabCompleter(antiairCommand);
        
        NationsCommand nationsCommand = new NationsCommand(this, nationManager, nationPlayerManager, nationGUI);
        getCommand("nation").setExecutor(nationsCommand);
        getCommand("nation").setTabCompleter(nationsCommand);
        
        SeasonCommand seasonCommand = new SeasonCommand(seasonManager);
        getCommand("seasons").setExecutor(seasonCommand);
        getCommand("seasons").setTabCompleter(seasonCommand);
        
        DisasterCommand disasterCommand = new DisasterCommand(nationManager, this);
        getCommand("disaster").setExecutor(disasterCommand);
        getCommand("disaster").setTabCompleter(disasterCommand);
        
        // Master doomsday command with role manager
        DoomsdayCommand doomsdayCommand = new DoomsdayCommand(this, messageManager, blockManager);
        doomsdayCommand.setRoleManager(roleManager);
        getCommand("dd").setExecutor(doomsdayCommand);
        getCommand("dd").setTabCompleter(doomsdayCommand);
        
        // AI command
        AICommand aiCommand = new AICommand(this, aiService);
        getCommand("ai").setExecutor(aiCommand);
        getCommand("ai").setTabCompleter(aiCommand);
        
        // Nation chat command
        NationChatCommand nationChatCommand = new NationChatCommand(this, nationPlayerManager, nationManager);
        nationChatCommand.setRoleManager(roleManager);
        getCommand("n").setExecutor(nationChatCommand);
        getCommand("n").setTabCompleter(nationChatCommand);
        
        // Utility commands
        getCommand("cc").setExecutor(colorChatHandler);
        getCommand("cc").setTabCompleter(colorChatHandler);
        
        // Start periodic cleanup task for reinforced blocks (every 5 minutes)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (reinforcedBlockManager != null) {
                    reinforcedBlockManager.cleanupInvalidReinforcements();
                }
            }
        }.runTaskTimer(this, 20L * 60 * 5, 20L * 60 * 5); // 5 minutes = 6000 ticks
        
            getLogger().info("DoomsdayCore plugin enabled successfully!");
            getLogger().info("Reinforced blocks system initialized with " + reinforcedBlockManager.getReinforcedBlockCount() + " blocks");
            getLogger().info("Anti-air defense system initialized with " + antiAirManager.getDefenses().size() + " defense installations");
            getLogger().info("Nation system initialized with " + nationManager.getAllNations().size() + " nations and natural disasters");
            getLogger().info("Role management system initialized with season integration");
            
        } catch (Exception e) {
            getLogger().severe("Failed to initialize DoomsdayCore plugin: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
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
    
    public ReinforcedBlockManager getReinforcedBlockManager() {
        return reinforcedBlockManager;
    }
    
    public ReinforcementHandler getReinforcementHandler() {
        return reinforcementHandler;
    }
    
    public AntiAirDefenseManager getAntiAirManager() {
        return antiAirManager;
    }
    
    public MissileFuelManager getFuelManager() {
        return fuelManager;
    }
    
    public AntiAirFuelManager getAntiAirFuelManager() {
        return antiAirFuelManager;
    }
    
    public CustomChatListener getCustomChatListener() {
        return customChatListener;
    }
    
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
    
    @Override
    public void onDisable() {
        // Save reinforced blocks data
        if (reinforcedBlockManager != null) {
            reinforcedBlockManager.saveReinforcedBlocks();
        }
        
        // Save missile fuel data
        if (fuelManager != null) {
            fuelManager.saveFuelData();
        }
        
        // Shutdown AI service
        if (aiService != null) {
            aiService.shutdown();
        }
        
        // Shutdown detector manager
        if (detectorManager != null) {
            detectorManager.shutdown();
        }
        
        // Shutdown nation player manager
        if (nationPlayerManager != null) {
            nationPlayerManager.saveConfiguration();
        }
        
        // Shutdown role management system
        if (roleScheduler != null) {
            roleScheduler.stopPeriodicCheck();
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


}