# DoomsdayCore Development Guidelines

This document outlines best practices for development in the DoomsdayCore plugin, including nation player tracking, GUI creation, user data management, and other core systems.

# Nation Player Tracking System

## Overview
The nation player tracking system uses a centralized in-memory cache for fast lookups. All nation queries are handled through `NationPlayerManager` which maintains an automatic cache synchronized with persistent storage.

## Usage Guidelines

### ✅ DO - Use These Methods

```java
// Get player's nation (works for online/offline players)
String nationId = nationPlayerManager.getPlayerNation(playerId);

// Check if player has joined a nation
boolean hasNation = nationPlayerManager.hasPlayerJoinedNation(playerId);

// Get online players in nation
Set<UUID> onlinePlayers = nationPlayerManager.getOnlinePlayersInNation(nationId);

// Get all players in nation (online + offline)
Set<UUID> allPlayers = nationPlayerManager.getAllPlayersInNation(nationId);

// Get player counts
int onlineCount = nationPlayerManager.getOnlinePlayerCountInNation(nationId);
int totalCount = nationPlayerManager.getTotalPlayerCountInNation(nationId);

// Get all nations with online counts
Map<String, Integer> counts = nationPlayerManager.getOnlinePlayerCountsByNation();
```

### ❌ DON'T - Avoid These Patterns

```java
// DON'T: Direct YAML access for lookups
playersConfig.getString("players." + playerId + ".nation");

// DON'T: Scanning through all online players
for (Player p : Bukkit.getOnlinePlayers()) {
    if (getPlayerNation(p.getUniqueId()).equals(targetNation)) {
        // Use getOnlinePlayersInNation() instead
    }
}
```

## Development Rules

1. **Always use `nationPlayerManager.getPlayerNation(UUID)` for lookups**
2. **Use bulk methods for operations on multiple players**
3. **Cache is automatically maintained - no manual updates needed**
4. **All operations are thread-safe for async usage**

## Debugging

```java
// Get cache statistics
String stats = nationPlayerManager.getCacheStats();

// Enable debug logging
nationPlayerManager.getNationPlayerCache().setDebug(true);
```

# User Data Management

## Overview
All user-related data storage MUST go through the UserDataService system. This provides a consistent interface for managing user data with support for different storage backends (currently file-based, with future database support planned).

## Core Classes
- `gg.doomsday.core.data.UserDataService` - Interface for user data operations
- `gg.doomsday.core.data.FileUserDataService` - File-based implementation (current)

## Usage Guidelines

### 1. Service Injection
Always inject the UserDataService through your plugin's main class:

```java
private final UserDataService userDataService;

public YourClass(JavaPlugin plugin, UserDataService userDataService) {
    this.userDataService = userDataService;
}
```

### 2. Basic Operations

#### Loading User Data
```java
// Load complete user data
CompletableFuture<YamlConfiguration> userData = userDataService.loadUserData(player);

// Get specific value with default
CompletableFuture<Integer> kills = userDataService.getUserValue(player, "stats.kills", 0);

// Usage example
userDataService.getUserValue(player, "stats.kills", 0)
    .thenAccept(kills -> {
        player.sendMessage("You have " + kills + " kills!");
    });
```

#### Saving User Data
```java
// Set specific value
userDataService.setUserValue(player, "stats.kills", newKillCount)
    .thenAccept(success -> {
        if (success) {
            player.sendMessage("Stats updated!");
        }
    });

// Increment numeric value
userDataService.incrementUserValue(player, "stats.kills", 1)
    .thenAccept(newValue -> {
        player.sendMessage("Kill count: " + newValue);
    });
```

#### Complex Data Operations
```java
// For complex operations, load, modify, then save
userDataService.loadUserData(player)
    .thenCompose(config -> {
        // Modify config
        config.set("nations.currentNation", "america");
        config.set("nations.joinDate", System.currentTimeMillis());
        
        // Save modified data
        return userDataService.saveUserData(player, config);
    })
    .thenAccept(success -> {
        if (success) {
            player.sendMessage("Nation data updated!");
        }
    });
```

### 3. Data Structure Standards

#### Standard Paths
Use these standardized paths for common data:

```yaml
# Player statistics
stats:
  playtime: 0
  joins: 0
  kills: 0
  deaths: 0
  missilesLaunched: 0
  blocksDestroyed: 0

# Player preferences
preferences:
  receiveNotifications: true
  showCoordinates: true

# Nation-related data
nations:
  currentNation: "america"
  joinDate: 1234567890
  switchCooldown: 0

# Achievement data
achievements:
  unlocked: []
  progress: {}

# Custom plugin data
customData:
  yourPluginName:
    someValue: 123
```

### 4. Initialization and Cleanup

#### In Plugin onEnable()
```java
@Override
public void onEnable() {
    // Initialize user data service
    userDataService = new FileUserDataService(this);
    userDataService.initialize().thenAccept(success -> {
        if (success) {
            getLogger().info("User data service initialized successfully");
        } else {
            getLogger().severe("Failed to initialize user data service!");
            setEnabled(false);
        }
    });
}
```

#### In Plugin onDisable()
```java
@Override
public void onDisable() {
    if (userDataService != null) {
        userDataService.shutdown().join(); // Wait for completion
    }
}
```

### 5. Error Handling

Always handle CompletableFuture exceptions:

```java
userDataService.getUserValue(player, "stats.kills", 0)
    .thenAccept(kills -> {
        // Success handling
        player.sendMessage("Kills: " + kills);
    })
    .exceptionally(throwable -> {
        // Error handling
        getLogger().log(Level.WARNING, "Failed to load user data", throwable);
        player.sendMessage("Failed to load your stats!");
        return null;
    });
```

### 6. Performance Best Practices

#### Batch Operations
For multiple users, use bulk operations:

```java
Map<UUID, YamlConfiguration> bulkData = new HashMap<>();
// ... populate bulkData ...

userDataService.bulkSaveUserData(bulkData)
    .thenAccept(success -> {
        getLogger().info("Bulk save completed: " + success);
    });
```

#### Caching
The FileUserDataService includes built-in caching. Don't implement your own caching layer.

### 7. Migration Support

When adding new data fields, always provide defaults:

```java
// Good - provides default value
int level = userDataService.getUserValue(player, "newFeature.level", 1);

// Bad - might cause issues if field doesn't exist
int level = userDataService.getUserValue(player, "newFeature.level", null);
```

## File Structure

User data files are stored in:
```
plugins/DoomsdayCore/users/
├── 12345678-1234-1234-1234-123456789abc.yml
├── 87654321-4321-4321-4321-cba987654321.yml
└── ...
```

Each file follows the standard YAML configuration format and contains all data for that specific user.

## Future Database Support

The interface is designed to support database backends. When migrating to database storage:

1. Implement a new `DatabaseUserDataService` class
2. Update plugin initialization to use the new service
3. Existing code using the interface will work unchanged

---

# GUI Development Guidelines

This section outlines best practices for creating GUIs in the DoomsdayCore plugin using our established foundation classes and patterns.

## Foundation Classes

### Core Classes
- `gg.doomsday.core.gui.framework.GUIBuilder` - Basic inventory creation utilities
- `gg.doomsday.core.gui.framework.builders.LayoutBuilder` - Advanced layout building with fluent API
- `gg.doomsday.core.gui.framework.NavigationGUI` - Base class for navigational GUIs
- `gg.doomsday.core.gui.utils.ItemBuilder` - Item creation with fluent API
- `gg.doomsday.core.gui.utils.GUIColors` - Standardized color schemes

## Basic GUI Structure Pattern

### 1. Creating a Basic GUI with Glass Border

```java
// Create inventory using foundation classes
Inventory gui = gg.doomsday.core.gui.framework.GUIBuilder.createInventory("GUI Title", 27);

// Add glass pane border for 27-slot GUI
ItemStack glassBorder = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
        .setDisplayName(" ")
        .build();

// Border pattern for 27-slot (3x9) GUI
for (int i = 0; i < 9; i++) {
    gui.setItem(i, glassBorder);         // Top row
    gui.setItem(18 + i, glassBorder);    // Bottom row
}
gui.setItem(9, glassBorder);             // Left middle
gui.setItem(17, glassBorder);            // Right middle

// For 54-slot (6x9) GUI:
for (int i = 0; i < 9; i++) {
    gui.setItem(i, glassBorder);         // Top row
    gui.setItem(45 + i, glassBorder);    // Bottom row
}
for (int row = 1; row < 5; row++) {
    gui.setItem(row * 9, glassBorder);       // Left column
    gui.setItem(row * 9 + 8, glassBorder);   // Right column
}
```

### 2. Using LayoutBuilder (Advanced Pattern)

```java
// Use LayoutBuilder for cleaner construction
LayoutBuilder layout = LayoutBuilder.forInventory("GUI Title", 27)
        .withBorder()  // Automatically adds gray glass pane border
        .withTitle(Material.TNT, "§c§lMain Title", "§7Description line");

Inventory gui = layout.build();
```

## Standard GUI Layouts

### 27-Slot GUI (3x9) Layout
```
[G][G][G][G][T][G][G][G][G]  // Row 0: Border with title at slot 4
[G][ ][ ][ ][ ][ ][ ][ ][G]  // Row 1: Border with content area
[G][G][G][G][B][G][G][G][G]  // Row 2: Border with back button at slot 22
```
- **G** = Glass pane border
- **T** = Title item (slot 4)
- **B** = Back button (slot 22)
- **[ ]** = Content area (slots 10-16)

### Common Slot Positions (27-slot GUI)
- **Title**: Slot 4 (center top)
- **Back Button**: Slot 22 (center bottom)
- **Main Actions**: Slots 11, 13, 15 (center row)
- **Content Area**: Slots 10-16 (middle row excluding borders)

## Item Creation Best Practices

### Using ItemBuilder
```java
ItemStack item = new ItemBuilder(Material.DIAMOND)
        .setDisplayName("§b§lAction Button")
        .setLore(
            "§7Description of what this does",
            "",
            "§e> Click to perform action"
        )
        .build();
```

### Color Schemes
Use consistent color coding:
- **§c** - Red for destructive actions, warnings
- **§a** - Green for success, confirmations
- **§6** - Gold for important values, currency
- **§b** - Aqua for informational items
- **§7** - Gray for descriptions
- **§e** - Yellow for instructions, hints
- **§8** - Dark gray for hidden data

## Data Storage in GUIs

### Storing Identifiers
Store important identifiers in item lore using hidden color codes:
```java
.setLore(
    "§7Visible information",
    "",
    "§8" + hiddenIdentifier  // Hidden identifier for extraction
)
```

### Extracting Data
```java
private String extractIdentifier(List<String> lore) {
    for (String line : lore) {
        if (line.startsWith("§8")) {
            return line.replaceAll("§[0-9a-fk-or]", "").trim();
        }
    }
    return null;
}
```

## Click Handling Pattern

### Method Structure
```java
public boolean handleGUIClick(Player player, String displayName, Inventory inventory) {
    // Extract data from GUI items
    ItemStack dataItem = inventory.getItem(SLOT_WITH_DATA);
    String identifier = extractIdentifier(dataItem.getItemMeta().getLore());
    
    if (displayName.contains("Action Button")) {
        return handleSpecificAction(player, identifier);
    } else if (displayName.contains("Back")) {
        return handleBack(player);
    }
    
    return false;
}
```

### Action Processing
```java
private boolean handleSpecificAction(Player player, String identifier) {
    // 1. Validate input
    if (identifier == null) {
        player.sendMessage("§cError: Invalid action");
        return false;
    }
    
    // 2. Perform action
    boolean success = performAction(player, identifier);
    
    // 3. Provide feedback
    if (success) {
        player.sendMessage("§a§lSuccess!");
        // 4. Refresh GUI if needed
        refreshGUI(player, identifier);
    } else {
        player.sendMessage("§cAction failed!");
    }
    
    return success;
}
```

## Inventory Management

### Consuming Items from Player Inventory
```java
private boolean removeItemFromInventory(Player player, ItemStack targetItem, int amount) {
    int remaining = amount;
    
    for (ItemStack item : player.getInventory().getContents()) {
        if (remaining <= 0) break;
        
        if (item != null && isSameItem(item, targetItem)) {
            int itemAmount = item.getAmount();
            if (itemAmount <= remaining) {
                remaining -= itemAmount;
                item.setAmount(0);  // Remove item
            } else {
                item.setAmount(itemAmount - remaining);
                remaining = 0;
            }
        }
    }
    
    return remaining == 0;  // True if all items were removed
}
```

### Item Validation
```java
private boolean isSameItem(ItemStack item1, ItemStack item2) {
    if (item1.getType() != item2.getType()) return false;
    
    ItemMeta meta1 = item1.getItemMeta();
    ItemMeta meta2 = item2.getItemMeta();
    
    if (meta1 == null || meta2 == null) return meta1 == meta2;
    
    return Objects.equals(meta1.getDisplayName(), meta2.getDisplayName());
}
```

## GUI Refresh Pattern

### Refreshing After Actions
```java
private void refreshGUI(Player player, String identifier) {
    // Create new GUI with updated data
    Inventory newGUI = createGUI(identifier);
    if (newGUI != null) {
        player.openInventory(newGUI);
    }
}
```

## Navigation Between GUIs

### Back Button Implementation
```java
private boolean handleBack(Player player) {
    Inventory parentGUI = createParentGUI();
    if (parentGUI != null) {
        player.openInventory(parentGUI);
        return true;
    }
    
    player.closeInventory();
    return false;
}
```

## Error Handling

### Standard Error Patterns
```java
// Validation errors
if (data == null) {
    player.sendMessage("§cError: Could not identify selection");
    player.closeInventory();
    return false;
}

// Action failures
if (!actionSuccessful) {
    player.sendMessage("§cAction failed! Please try again.");
    return false;
}

// Recovery from failures
if (!primaryAction() && !fallbackAction()) {
    player.sendMessage("§cCritical error occurred!");
    player.closeInventory();
    return false;
}
```

## Example Implementation

See `MissileGUI.createFuelDepotGUI()` for a complete example that follows all these patterns:

1. ✅ Uses foundation classes (`GUIBuilder.createInventory`)
2. ✅ Implements glass pane borders correctly
3. ✅ Uses standard slot positions (4, 11, 13, 15, 22)
4. ✅ Stores data in hidden lore (`§8` + identifier)
5. ✅ Handles inventory consumption properly
6. ✅ Refreshes GUI after actions
7. ✅ Provides proper error handling and feedback

## Common Mistakes to Avoid

1. **❌ Don't create inventories without borders** - Always add glass pane borders for visual consistency
2. **❌ Don't hardcode strings** - Use consistent color schemes and formatting
3. **❌ Don't forget to validate data** - Always check for null values and invalid states
4. **❌ Don't modify GUI items directly** - Items in GUIs should not be draggable/modifiable
5. **❌ Don't forget to refresh GUIs** - Always update GUIs after state changes
6. **❌ Don't use wrong slot positions** - Follow the standard layout patterns
7. **❌ Don't forget error handling** - Always provide meaningful error messages

Following these patterns ensures consistency across all GUIs in the plugin and makes maintenance easier.