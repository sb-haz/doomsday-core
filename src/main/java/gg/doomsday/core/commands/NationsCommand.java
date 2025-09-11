package gg.doomsday.core.commands;

import gg.doomsday.core.gui.NationGUI;
import gg.doomsday.core.nations.Nation;
import gg.doomsday.core.nations.NationManager;
import gg.doomsday.core.nations.NationPlayerManager;
import gg.doomsday.core.gui.utils.ItemBuilder;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

public class NationsCommand implements CommandExecutor, TabCompleter, Listener {
    
    private final JavaPlugin plugin;
    private final NationManager nationManager;
    private final NationPlayerManager playerManager;
    private final NationGUI nationGUI;
    
    public NationsCommand(JavaPlugin plugin, NationManager nationManager, NationPlayerManager playerManager, NationGUI nationGUI) {
        this.plugin = plugin;
        this.nationManager = nationManager;
        this.playerManager = playerManager;
        this.nationGUI = nationGUI;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("nation")) {
            return false;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Players only!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            return handleMainCommand(player);
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "leave":
                return handleLeave(player);
            case "info":
                return handleInfo(player, args);
            case "teleport":
            case "tp":
                return handleTeleport(player, args);
            case "missiles":
                return handleMissiles(player, args);
            case "antiair":
                return handleAntiair(player, args);
            case "disasters":
                return handleDisasters(player, args);
            case "items":
                return handleItems(player);
            default:
                return handleMainCommand(player);
        }
    }
    
    private boolean handleMainCommand(Player player) {
        UUID playerId = player.getUniqueId();
        String currentNation = playerManager.getPlayerNation(playerId);
        
        if (currentNation == null) {
            // Player not in nation - show nation selection GUI
            nationGUI.openNationSelectionGUI(player);
        } else {
            // Player in nation - show nation-specific menu
            openNationMainGUI(player, currentNation);
        }
        
        return true;
    }
    
    private void openMainMenuGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "Doomsday Nations - Main Menu");
        
        // Add glass pane border
        addGlassPaneBorder(gui);
        
        // Info item
        gui.setItem(4, new ItemBuilder(Material.COMPASS)
                .setDisplayName("&6&lDoomsday Nations")
                .setLore(
                    "&fWelcome to the Nations system!",
                    "",
                    "&fYou are not currently in any nation.",
                    "&fExplore the options below to learn more",
                    "&fand choose a nation to join.",
                    "",
                    "&eClick on the options below to explore!"
                )
                .build());
        
        // Join Nations
        gui.setItem(20, new ItemBuilder(Material.EMERALD)
                .setDisplayName("&a&lJoin a Nation")
                .setLore(
                    "&fChoose your nation and join the fight!",
                    "&fEach nation has unique advantages:",
                    "&f• Different missile types",
                    "&f• Unique natural disasters",
                    "&f• Geographic locations",
                    "",
                    "&eClick to view nation selection!"
                )
                .build());
        
        // View All Nations
        gui.setItem(21, new ItemBuilder(Material.MAP)
                .setDisplayName("&6&lExplore Nations")
                .setLore(
                    "&fLearn about all available nations:",
                    "&f• Population and statistics",
                    "&f• Available weapons and defenses",
                    "&f• Natural disaster types",
                    "&f• Geographic information",
                    "",
                    "&eClick to explore nations!"
                )
                .build());
        
        // Missile Types
        gui.setItem(22, new ItemBuilder(Material.TNT)
                .setDisplayName("&c&lMissile Arsenal")
                .setLore(
                    "&fView all available missile types:",
                    "&f• R1 - Standard explosive missile",
                    "&f• R2 - Bunker buster with drilling",
                    "&f• R3 - Cluster bomb with spread",
                    "&f• R4 - Thermobaric with magma",
                    "&f• R5 - Nuclear with mushroom cloud",
                    "",
                    "&eClick to view missile details!"
                )
                .build());
        
        // Anti-Air Systems
        gui.setItem(23, new ItemBuilder(Material.CROSSBOW)
                .setDisplayName("&9&lAnti-Air Defenses")
                .setLore(
                    "&fLearn about defensive systems:",
                    "&f• Automated interceptor systems",
                    "&f• Manual defense operations",
                    "&f• Range and accuracy stats",
                    "&f• Nation-specific deployments",
                    "",
                    "&eClick to view defense systems!"
                )
                .build());
        
        // Natural Disasters
        gui.setItem(24, new ItemBuilder(Material.BLAZE_POWDER)
                .setDisplayName("&4&lNatural Disasters")
                .setLore(
                    "&fDiscover nation-specific disasters:",
                    "&f• America: Meteors, Wildfires, Tornadoes",
                    "&f• Europe: Floods, Plagues, Storms",
                    "&f• Africa: Droughts, Sandstorms",
                    "&f• Antarctica: Blizzards, Ice Storms",
                    "",
                    "&eClick to learn about disasters!"
                )
                .build());
        
        // Custom Items
        gui.setItem(32, new ItemBuilder(Material.DIAMOND_PICKAXE)
                .setDisplayName("&b&lCustom Items & Reinforcement")
                .setLore(
                    "&fLearn about special items:",
                    "&f• Reinforcement Powder",
                    "&f• Detection Helmets",
                    "&f• Block reinforcement system",
                    "&f• Crafting recipes",
                    "",
                    "&eClick to view item information!"
                )
                .build());
        
        player.openInventory(gui);
    }
    
    private void openNationMainGUI(Player player, String nationId) {
        Nation nation = nationManager.getAllNations().get(nationId);
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Your nation could not be found!");
            return;
        }
        
        Inventory gui = Bukkit.createInventory(null, 54, nation.getDisplayName() + " - Main Menu");
        
        // Add glass pane border
        addGlassPaneBorder(gui);
        
        // Player info with player head (on glass pane border - top center)
        gui.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                .setDisplayName("&6&l" + player.getName())
                .setLore(
                    "&fYour Nation: &a" + nation.getDisplayName(),
                    "&fPopulation: &f" + nation.getTotalPlayers(),
                    "&fRegion: &f" + getLocationDescription(nationId),
                    "",
                    "&fWelcome back, citizen!"
                )
                .setSkullOwner(player.getName())
                .build());
        
        // Your Nation Details (shifted right)
        gui.setItem(20, new ItemBuilder(Material.GREEN_BANNER)
                .setDisplayName("&a&lYour Nation Info")
                .setLore(
                    "&fView detailed nation information:",
                    "&f• Population statistics",
                    "&f• Territory boundaries",
                    "&f• Military capabilities",
                    "",
                    "&eClick to view details!"
                )
                .build());
        
        // Available Missiles (shifted right)
        gui.setItem(21, new ItemBuilder(Material.TNT)
                .setDisplayName("&c&lYour Arsenal")
                .setLore(
                    "&fView your nation's missiles:",
                    "&f• " + (nation.getMissileTypes() != null ? nation.getMissileTypes().size() : 0) + " missile types available",
                    "&f• Detailed specifications",
                    "&f• Usage commands",
                    "",
                    "&eClick to view your missiles!"
                )
                .build());
        
        // Nation Disasters (shifted right)
        gui.setItem(22, new ItemBuilder(Material.BLAZE_POWDER)
                .setDisplayName("&4&lNatural Disasters")
                .setLore(
                    "&fView disasters in your region:",
                    "&f• " + nation.getDisasters().size() + " disaster types",
                    "&f• Current active disasters",
                    "&f• Disaster descriptions and effects",
                    "",
                    "&eClick to view disasters!"
                )
                .build());
        
        // Custom Items (shifted right)
        gui.setItem(23, new ItemBuilder(Material.DIAMOND_PICKAXE)
                .setDisplayName("&b&lCustom Items & Reinforcement")
                .setLore(
                    "&fLearn about special items:",
                    "&f• Reinforcement system",
                    "&f• Detection tools",
                    "&f• Crafting information",
                    "",
                    "&eClick to view item info!"
                )
                .build());
        
        // Other Nations (moved next to others, shifted right)
        gui.setItem(24, new ItemBuilder(Material.MAP)
                .setDisplayName("&6&lExplore Other Nations")
                .setLore(
                    "&fLearn about foreign nations:",
                    "&f• Compare capabilities",
                    "&f• View their arsenals",
                    "&f• Understand their disasters",
                    "&f• View all missile types",
                    "&f• View all anti-air systems", 
                    "&f• View all disaster types",
                    "",
                    "&eClick to explore!"
                )
                .build());
        
        // Leave Nation (if allowed)
        UUID playerId = player.getUniqueId();
        if (playerManager.canPlayerSwitch(playerId)) {
            gui.setItem(49, new ItemBuilder(Material.BARRIER)
                    .setDisplayName("&c&lLeave Nation")
                    .setLore(
                        "&fLeave " + nation.getDisplayName(),
                        "&cWarning: You may not be able to rejoin immediately!",
                        "",
                        "&eClick to leave your nation"
                    )
                    .build());
        } else {
            gui.setItem(49, new ItemBuilder(Material.GRAY_DYE)
                    .setDisplayName("&7&lLeave Nation")
                    .setLore(
                        "&fYou cannot leave your nation",
                        "&fdue to cooldown restrictions.",
                        "",
                        "&cLeaving is currently disabled"
                    )
                    .build());
        }
        
        player.openInventory(gui);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        // Handle main menu GUI clicks
        if (title.equals("Doomsday Nations - Main Menu")) {
            handleMainMenuClick(event, player);
            return;
        }
        
        // Handle nation main menu clicks
        if (title.endsWith(" - Main Menu")) {
            handleNationMainMenuClick(event, player);
            return;
        }
        
        // Handle comprehensive GUIs
        if (title.equals("All Missile Types")) {
            handleAllMissilesClick(event, player);
            return;
        }
        
        if (title.equals("All Anti-Air Systems")) {
            handleAllAntiAirClick(event, player);
            return;
        }
        
        if (title.equals("All Natural Disasters")) {
            handleAllDisastersClick(event, player);
            return;
        }
        
        if (title.equals("Custom Items Information")) {
            handleCustomItemsClick(event, player);
            return;
        }
    }
    
    private void handleMainMenuClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        switch (event.getSlot()) {
            case 20: // Join a Nation
                nationGUI.openNationSelectionGUI(player);
                break;
            case 21: // Explore Nations
                nationGUI.openNationsListGUI(player);
                break;
            case 22: // Missile Arsenal
                openAllMissilesGUI(player);
                break;
            case 23: // Anti-Air Defenses
                openAllAntiAirGUI(player);
                break;
            case 24: // Natural Disasters
                openAllDisastersGUI(player);
                break;
            case 32: // Custom Items
                openCustomItemsGUI(player);
                break;
        }
    }
    
    private void handleNationMainMenuClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        String title = event.getView().getTitle();
        String nationName = title.replace(" - Main Menu", "");
        String nationId = getNationIdFromDisplayName(nationName);
        
        switch (event.getSlot()) {
            case 20: // Your Nation Info
                if (nationId != null) {
                    nationGUI.pushToNavigationStackFromExternal(player, title);
                    nationGUI.openNationDetailsGUI(player, nationId);
                }
                break;
            case 21: // Your Arsenal
                if (nationId != null) {
                    nationGUI.pushToNavigationStackFromExternal(player, title);
                    nationGUI.openMissilesGUI(player, nationId);
                }
                break;
            case 22: // Nation Disasters
                if (nationId != null) {
                    nationGUI.pushToNavigationStackFromExternal(player, title);
                    nationGUI.openDisastersGUI(player, nationId);
                }
                break;
            case 23: // Custom Items
                openCustomItemsGUI(player);
                break;
            case 24: // Other Nations
                nationGUI.openNationsListGUI(player, title);
                break;
            case 49: // Leave Nation
                UUID playerId = player.getUniqueId();
                if (playerManager.canPlayerSwitch(playerId)) {
                    handleLeaveNationFromGUI(player, nationId);
                }
                break;
        }
    }
    
    private void openAllMissilesGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "All Missile Types");
        
        // Add glass pane border
        addGlassPaneBorder(gui);
        
        // Back button (on top of glass pane)
        gui.setItem(45, new ItemBuilder(Material.ARROW)
                .setDisplayName("&f« Back")
                .setLore("&fReturn to previous menu")
                .build());
        
        // Title
        gui.setItem(4, new ItemBuilder(Material.TNT)
                .setDisplayName("&c&lComplete Missile Arsenal")
                .setLore(
                    "&fAll missile types in the system",
                    "&fLearn about damage, effects, and usage"
                )
                .build());
        
        // Display missiles from config
        if (plugin.getConfig().contains("rockets")) {
            int slot = 19;
            for (String missileKey : plugin.getConfig().getConfigurationSection("rockets").getKeys(false)) {
                if (slot > 25) break;
                
                String displayName = plugin.getConfig().getString("rockets." + missileKey + ".displayName", missileKey.toUpperCase());
                String explosionType = plugin.getConfig().getString("rockets." + missileKey + ".explosionType", "DEFAULT");
                double speed = plugin.getConfig().getDouble("rockets." + missileKey + ".speed", 1.0);
                
                // Get additional config details
                double arcScale = plugin.getConfig().getDouble("rockets." + missileKey + ".arcScale", 1.0);
                
                List<String> lore = new ArrayList<>();
                lore.add("&fType: &f" + explosionType);
                lore.add("&fSpeed: &f" + speed);
                lore.add("&fArc Scale: &f" + arcScale);
                lore.add("");
                lore.add("&fNations with this missile:");
                
                // Check which nations have this missile
                for (Nation nation : nationManager.getAllNations().values()) {
                    if (nation.getMissileTypes() != null && nation.getMissileTypes().contains(missileKey)) {
                        lore.add("&f• &f" + nation.getDisplayName());
                    }
                }
                
                lore.add("");
                lore.add(getMissileDescription(missileKey));
                
                gui.setItem(slot, new ItemBuilder(getMissileMaterial(missileKey))
                        .setDisplayName("&6&l" + displayName)
                        .setLore(lore)
                        .build());
                
                slot++;
            }
        }
        
        player.openInventory(gui);
    }
    
    private void openAllAntiAirGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "All Anti-Air Systems");
        
        // Add glass pane border
        addGlassPaneBorder(gui);
        
        // Back button (on top of glass pane)
        gui.setItem(45, new ItemBuilder(Material.ARROW)
                .setDisplayName("&f« Back")
                .setLore("&fReturn to previous menu")
                .build());
        
        // Title
        gui.setItem(4, new ItemBuilder(Material.CROSSBOW)
                .setDisplayName("&9&lAnti-Aircraft Defense Systems")
                .setLore(
                    "&fOverview of all defensive systems",
                    "&fLearn about interception capabilities"
                )
                .build());
        
        // Info about anti-air systems
        gui.setItem(20, new ItemBuilder(Material.TARGET)
                .setDisplayName("&a&lAutomated Systems")
                .setLore(
                    "&fAI-controlled interceptor systems",
                    "&f• Automatic threat detection",
                    "&f• High response time",
                    "&f• Configurable accuracy",
                    "&f• 24/7 operational capability"
                )
                .build());
        
        gui.setItem(22, new ItemBuilder(Material.LEVER)
                .setDisplayName("&e&lManual Systems")
                .setLore(
                    "&fPlayer-operated defense systems",
                    "&f• Requires nearby operators",
                    "&f• Higher accuracy potential",
                    "&f• Strategic positioning",
                    "&f• Human decision making"
                )
                .build());
        
        gui.setItem(24, new ItemBuilder(Material.REDSTONE)
                .setDisplayName("&c&lSystem Specifications")
                .setLore(
                    "&fTechnical information:",
                    "&f• Range: 50-200 blocks",
                    "&f• Accuracy: 65-95%",
                    "&f• Reload time: 3-8 seconds",
                    "&f• Interceptor speed: 2-4x",
                    "&f• Multiple targeting modes"
                )
                .build());
        
        player.openInventory(gui);
    }
    
    private void openAllDisastersGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "All Natural Disasters");
        
        // Add glass pane border
        addGlassPaneBorder(gui);
        
        // Back button (on top of glass pane)
        gui.setItem(45, new ItemBuilder(Material.ARROW)
                .setDisplayName("&f« Back")
                .setLore("&fReturn to previous menu")
                .build());
        
        // Title
        gui.setItem(4, new ItemBuilder(Material.NETHER_STAR)
                .setDisplayName("&4&lNatural Disasters Encyclopedia")
                .setLore(
                    "&fComplete guide to all disaster types",
                    "&fLearn about effects and nation impacts"
                )
                .build());
        
        // America disasters
        gui.setItem(11, new ItemBuilder(Material.FIRE_CHARGE)
                .setDisplayName("&c&lAmerica Disasters")
                .setLore(
                    "&6Meteor Showers:",
                    "&f• TNT meteors with flame trails",
                    "&f• Rare ore drops on impact",
                    "",
                    "&6Wildfires:",
                    "&f• Spreads fire through forests",
                    "&f• Burns grasslands during dry periods",
                    "",
                    "&6Tornadoes:",
                    "&f• Moving vortex pulls players",
                    "&f• Creates widespread chaos"
                )
                .build());
        
        // Europe disasters
        gui.setItem(13, new ItemBuilder(Material.WATER_BUCKET)
                .setDisplayName("&a&lEurope Disasters")
                .setLore(
                    "&9Flooding:",
                    "&f• Rivers overflow extensively",
                    "&f• Farmland gets completely wiped",
                    "",
                    "&5Plagues:",
                    "&f• Sickness spreads slowly",
                    "&f• Causes hunger and weakness",
                    "",
                    "&8Storms:",
                    "&f• Lightning strikes with fire chance",
                    "&f• Environmental damage"
                )
                .build());
        
        // Africa disasters
        gui.setItem(15, new ItemBuilder(Material.DEAD_BUSH)
                .setDisplayName("&6&lAfrica Disasters")
                .setLore(
                    "&6Droughts:",
                    "&f• Water sources completely dry up",
                    "&f• Crops fail, severe hunger effects",
                    "",
                    "&e Sandstorms:",
                    "&f• Massive visibility reduction",
                    "&f• Blindness and slow movement"
                )
                .build());
        
        // Antarctica disasters
        gui.setItem(31, new ItemBuilder(Material.SNOWBALL)
                .setDisplayName("&f&lAntarctica Disasters")
                .setLore(
                    "&fBlizzards:",
                    "&f• Heavy snow reduces visibility",
                    "&f• Near-zero visibility conditions",
                    "&f• Severe movement penalties",
                    "",
                    "&bIce Storms:",
                    "&f• Freezing conditions everywhere",
                    "&f• Water turns to ice blocks",
                    "&f• Cold damage to players"
                )
                .build());
        
        player.openInventory(gui);
    }
    
    private void openCustomItemsGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "Custom Items Information");
        
        // Add glass pane border
        addGlassPaneBorder(gui);
        
        // Back button (on top of glass pane)
        gui.setItem(45, new ItemBuilder(Material.ARROW)
                .setDisplayName("&f« Back")
                .setLore("&fReturn to previous menu")
                .build());
        
        // Title
        gui.setItem(4, new ItemBuilder(Material.DIAMOND_PICKAXE)
                .setDisplayName("&b&lCustom Items & Systems")
                .setLore(
                    "&fSpecial items and reinforcement system",
                    "&fLearn about crafting and usage"
                )
                .build());
        
        // Reinforcement Powder
        gui.setItem(20, new ItemBuilder(Material.GUNPOWDER)
                .setDisplayName("&e&lReinforcement Powder")
                .setLore(
                    "&fStrengthen blocks against explosions",
                    "",
                    "&fRecipe (Shapeless):",
                    "&f• 1x Iron Ingot",
                    "&f• 1x Stone",
                    "&f= 1x Reinforcement Powder",
                    "",
                    "&fUsage:",
                    "&fRight-click on blocks to reinforce them",
                    "&fReinforced blocks resist explosions",
                    "",
                    "&fResistance varies by block type:",
                    "&f• Stone: 50%",
                    "&f• Obsidian: 95%",
                    "&f• Concrete: 60%"
                )
                .build());
        
        // Detection Helmet
        gui.setItem(22, new ItemBuilder(Material.DIAMOND_HELMET)
                .setDisplayName("&b&lDetection Helmet")
                .setLore(
                    "&fVisualize reinforced blocks",
                    "",
                    "&fUsage:",
                    "&fWear as helmet to activate",
                    "&fShows particles on reinforced blocks",
                    "&fWorks within 16 block radius",
                    "",
                    "&fFeatures:",
                    "&f• Real-time scanning",
                    "&f• Particle indicators",
                    "&f• Range-based detection",
                    "&f• Continuous updates while worn"
                )
                .build());
        
        // Block System
        gui.setItem(24, new ItemBuilder(Material.OBSIDIAN)
                .setDisplayName("&7&lReinforcement System")
                .setLore(
                    "&fAdvanced block protection system",
                    "",
                    "&fHow it works:",
                    "&f• Apply powder to valid blocks",
                    "&f• Blocks gain explosion resistance",
                    "&f• Data saved across server restarts",
                    "&f• Automatic cleanup of invalid blocks",
                    "",
                    "&fValid blocks:",
                    "&fStone, Cobblestone, Bricks, Concrete,",
                    "&fand many more construction materials"
                )
                .build());
        
        player.openInventory(gui);
    }
    
    // Handle click events for comprehensive GUIs
    private void handleAllMissilesClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        if (event.getSlot() == 45) { // Back button
            UUID playerId = player.getUniqueId();
            String currentNation = playerManager.getPlayerNation(playerId);
            
            if (currentNation == null) {
                openMainMenuGUI(player);
            } else {
                openNationMainGUI(player, currentNation);
            }
        }
    }
    
    private void handleAllAntiAirClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        if (event.getSlot() == 45) { // Back button
            UUID playerId = player.getUniqueId();
            String currentNation = playerManager.getPlayerNation(playerId);
            
            if (currentNation == null) {
                openMainMenuGUI(player);
            } else {
                openNationMainGUI(player, currentNation);
            }
        }
    }
    
    private void handleAllDisastersClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        if (event.getSlot() == 45) { // Back button
            UUID playerId = player.getUniqueId();
            String currentNation = playerManager.getPlayerNation(playerId);
            
            if (currentNation == null) {
                openMainMenuGUI(player);
            } else {
                openNationMainGUI(player, currentNation);
            }
        }
    }
    
    private void handleCustomItemsClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        if (event.getSlot() == 45) { // Back button
            UUID playerId = player.getUniqueId();
            String currentNation = playerManager.getPlayerNation(playerId);
            
            if (currentNation == null) {
                openMainMenuGUI(player);
            } else {
                openNationMainGUI(player, currentNation);
            }
        }
    }
    
    private boolean handleLeave(Player player) {
        UUID playerId = player.getUniqueId();
        String currentNation = playerManager.getPlayerNation(playerId);
        
        if (currentNation == null) {
            player.sendMessage(ChatColor.RED + "You are not in any nation!");
            return true;
        }
        
        return handleLeaveNationFromGUI(player, currentNation);
    }
    
    private boolean handleLeaveNationFromGUI(Player player, String currentNation) {
        UUID playerId = player.getUniqueId();
        
        if (!playerManager.canPlayerLeave(player)) {
            player.sendMessage(ChatColor.RED + "Player leaving is currently disabled!");
            return true;
        }
        
        if (!playerManager.canPlayerSwitch(playerId)) {
            player.sendMessage(ChatColor.RED + "You cannot leave your current nation!");
            long cooldown = playerManager.getSwitchCooldown();
            if (cooldown > 0) {
                long timeLeft = (playerManager.getPlayerJoinDate(playerId) + cooldown) - System.currentTimeMillis();
                if (timeLeft > 0) {
                    long hours = timeLeft / (1000 * 60 * 60);
                    long minutes = (timeLeft % (1000 * 60 * 60)) / (1000 * 60);
                    player.sendMessage(ChatColor.GRAY + "Cooldown remaining: " + ChatColor.WHITE + hours + "h " + minutes + "m");
                }
            }
            return true;
        }
        
        if (playerManager.leaveNation(player, true)) {
            Nation nation = nationManager.getAllNations().get(currentNation);
            String nationName = nation != null ? nation.getDisplayName() : currentNation;
            
            player.sendMessage(ChatColor.YELLOW + "You have left " + ChatColor.WHITE + nationName + ChatColor.YELLOW + ".");
            player.sendMessage(ChatColor.GRAY + "Use /nations to join a new nation.");
            player.closeInventory();
        } else {
            player.sendMessage(ChatColor.RED + "Failed to leave your nation!");
        }
        return true;
    }
    
    private boolean handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            // Show own nation info
            UUID playerId = player.getUniqueId();
            String currentNation = playerManager.getPlayerNation(playerId);
            
            if (currentNation == null) {
                player.sendMessage(ChatColor.RED + "You are not in any nation! Use /nations to join one.");
                return true;
            }
            
            nationGUI.openNationDetailsGUI(player, currentNation);
        } else {
            // Show specific nation info
            String nationName = args[1].toLowerCase();
            Nation nation = nationManager.getAllNations().get(nationName);
            
            if (nation == null) {
                player.sendMessage(ChatColor.RED + "Nation not found: " + args[1]);
                return true;
            }
            
            nationGUI.openNationDetailsGUI(player, nation.getId());
        }
        return true;
    }
    
    private boolean handleTeleport(Player player, String[] args) {
        String targetNationId;
        
        if (args.length < 2) {
            // Teleport to own nation
            UUID playerId = player.getUniqueId();
            String currentNation = playerManager.getPlayerNation(playerId);
            
            if (currentNation == null) {
                player.sendMessage(ChatColor.RED + "You are not in any nation! Use /nations to join one.");
                return true;
            }
            
            targetNationId = currentNation;
        } else {
            // Teleport to specific nation
            targetNationId = args[1].toLowerCase();
        }
        
        Nation nation = nationManager.getAllNations().get(targetNationId);
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "Nation not found!");
            return true;
        }
        
        // Teleport to nation center
        int centerX = nation.getBorders().getCenterX();
        int centerZ = nation.getBorders().getCenterZ();
        
        Location teleportLoc = new Location(
            player.getWorld(),
            centerX,
            player.getWorld().getHighestBlockYAt(centerX, centerZ) + 1,
            centerZ
        );
        
        player.teleport(teleportLoc);
        player.sendMessage(ChatColor.GREEN + "Teleported to " + nation.getDisplayName() + " center!");
        
        return true;
    }
    
    private boolean handleMissiles(Player player, String[] args) {
        if (args.length < 2) {
            openAllMissilesGUI(player);
        } else {
            String nationName = args[1].toLowerCase();
            Nation nation = nationManager.getAllNations().get(nationName);
            
            if (nation == null) {
                player.sendMessage(ChatColor.RED + "Nation not found: " + args[1]);
                return true;
            }
            
            nationGUI.openMissilesGUI(player, nation.getId());
        }
        return true;
    }
    
    private boolean handleAntiair(Player player, String[] args) {
        openAllAntiAirGUI(player);
        return true;
    }
    
    private boolean handleDisasters(Player player, String[] args) {
        if (args.length < 2) {
            openAllDisastersGUI(player);
        } else {
            String nationName = args[1].toLowerCase();
            Nation nation = nationManager.getAllNations().get(nationName);
            
            if (nation == null) {
                player.sendMessage(ChatColor.RED + "Nation not found: " + args[1]);
                return true;
            }
            
            nationGUI.openDisastersGUI(player, nation.getId());
        }
        return true;
    }
    
    private boolean handleItems(Player player) {
        openCustomItemsGUI(player);
        return true;
    }
    
    // Helper methods
    private Material getNationMaterial(String nationId) {
        switch (nationId.toLowerCase()) {
            case "america": return Material.BLUE_WOOL;
            case "europe": return Material.GREEN_WOOL;
            case "africa": return Material.ORANGE_WOOL;
            case "asia": return Material.RED_WOOL;
            case "antarctica": return Material.WHITE_WOOL;
            default: return Material.GRAY_WOOL;
        }
    }
    
    private String getLocationDescription(String nationId) {
        switch (nationId.toLowerCase()) {
            case "america": return "West";
            case "europe": return "North";
            case "africa": return "Central";
            case "asia": return "East";
            case "antarctica": return "South";
            default: return "Unknown";
        }
    }
    
    private String getNationIdFromDisplayName(String displayName) {
        for (Nation nation : nationManager.getAllNations().values()) {
            if (nation.getDisplayName().equals(displayName)) {
                return nation.getId();
            }
        }
        return null;
    }
    
    private Material getMissileMaterial(String missileKey) {
        if (missileKey.contains("r5") || missileKey.toLowerCase().contains("nuclear")) return Material.TNT;
        if (missileKey.contains("r4")) return Material.FIRE_CHARGE;
        if (missileKey.contains("r3")) return Material.FIREWORK_ROCKET;
        if (missileKey.contains("r2")) return Material.CROSSBOW;
        return Material.BOW;
    }
    
    private String getMissileDescription(String missileKey) {
        switch (missileKey.toLowerCase()) {
            case "r1": return "&fStandard explosive with reliable damage";
            case "r2": return "&fBunker buster that drills deep underground";
            case "r3": return "&fCluster bomb with horizontal spread explosions";
            case "r4": return "&fThermobaric with magma block placement";
            case "r5": return "&fNuclear with massive mushroom cloud";
            default: return "&fAdvanced military-grade missile system";
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("leave", "info", "teleport", "tp", "missiles", "antiair", "disasters", "items")
                .stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("info") || subCommand.equals("teleport") || subCommand.equals("tp") || 
                subCommand.equals("missiles") || subCommand.equals("disasters")) {
                return nationManager.getAllNations().keySet()
                    .stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }
        
        return new ArrayList<>();
    }
    
    // Helper method to add glass pane borders to GUIs
    private void addGlassPaneBorder(Inventory gui) {
        ItemStack glassPaneItem = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .setDisplayName("&f")
                .build();
        
        // Top row (0-8)
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, glassPaneItem);
        }
        
        // Bottom row (45-53)
        for (int i = 45; i < 54; i++) {
            gui.setItem(i, glassPaneItem);
        }
        
        // Left and right columns
        for (int row = 1; row < 5; row++) { // rows 1-4
            gui.setItem(row * 9, glassPaneItem); // left column
            gui.setItem(row * 9 + 8, glassPaneItem); // right column
        }
    }
}