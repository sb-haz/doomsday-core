# Enhanced GUI Framework Guide

This guide explains how to use the enhanced GUI framework for creating professional, maintainable Minecraft plugin GUIs.

## Overview

Your GUI framework has been enhanced with several powerful patterns:

- **Decorator Pattern**: Add common behaviors like confirmations and pagination
- **Builder Pattern**: Fluent API for creating complex layouts
- **Factory Pattern**: Extensible GUI registration system
- **Navigation System**: Automatic back button and history management

## Core Components

### 1. Base Classes

- `GUI`: Abstract base class for all GUIs
- `NavigationGUI`: Enhanced base with automatic navigation
- `GUIManager`: Central controller for all GUI operations
- `GUIFactory`: Interface for creating GUI instances

### 2. Decorators (`gg.doomsday.core.gui.framework.decorators`)

#### ConfirmationDialog
```java
// Simple confirmation
ConfirmationDialog confirmation = new ConfirmationDialog(
    "Delete Item?",
    "This action cannot be undone",
    player -> deleteItem(player),  // onConfirm
    null,                          // onCancel (defaults to back/close)
    guiManager
);

// Factory methods for common scenarios
ConfirmationDialog.createJoinConfirmation(nationName, onConfirm, guiManager);
ConfirmationDialog.createLeaveConfirmation(nationName, onConfirm, guiManager);
ConfirmationDialog.createDeleteConfirmation(itemName, onConfirm, guiManager);
```

#### PaginatedGUI
```java
List<Nation> nations = getAllNations();

PaginatedGUI<Nation> gui = new PaginatedGUI<>(
    "All Nations",
    nations,
    (nation, index) -> createNationItem(nation),  // Item renderer
    player -> handleNationClick(player),          // Click handler
    guiManager
);
```

### 3. Builders (`gg.doomsday.core.gui.framework.builders`)

#### LayoutBuilder
```java
Inventory gui = LayoutBuilder.forInventory("My GUI", 54)
    .withBorder()                                    // Glass pane border
    .withTitle(Material.DIAMOND, "GUI Title",        // Title item
               "Subtitle", "Description")
    .withCrossPattern(                               // + shaped layout
        northItem, southItem, eastItem, 
        westItem, centerItem,
        item -> createItemStack(item)
    )
    .withControlButtons(                             // Bottom controls
        backButton, infoButton, closeButton
    )
    .withBackButton()                                // Standard back button
    .build();
```

## Best Practices

### 1. Use Existing Patterns

Instead of hardcoding navigation logic:

```java
// ❌ Bad - Hardcoded navigation
private void handleBackNavigation(Player player) {
    String previousGUITitle = popFromNavigationStack(player);
    if (previousGUITitle.equals("Select Your Nation")) {
        openNationSelectionGUIWithoutPush(player);
    } else if (previousGUITitle.equals("Nations Overview")) {
        openNationsListGUIWithoutPush(player);
    }
    // ... 50 more lines of if/else
}

// ✅ Good - Use framework navigation
public class MyGUI extends NavigationGUI {
    @Override
    protected boolean handleSpecificClick(Player player, int slot, ItemStack item) {
        // Framework handles back button automatically
        // Just handle your specific slots
        return false;
    }
}
```

### 2. Use Decorators for Common Patterns

```java
// ❌ Bad - Inline confirmation logic
private void handleDeleteClick(Player player) {
    Inventory confirmGUI = Bukkit.createInventory(null, 27, "Confirm Delete?");
    // ... 30 lines of confirmation GUI setup
    player.openInventory(confirmGUI);
}

// ✅ Good - Use decorator
private void handleDeleteClick(Player player) {
    ConfirmationDialog confirmation = ConfirmationDialog.createDeleteConfirmation(
        "this item",
        p -> performDelete(p),
        guiManager
    );
    guiManager.openGUI(player, confirmation);
}
```

### 3. Use Builders for Complex Layouts

```java
// ❌ Bad - Manual inventory setup
Inventory gui = Bukkit.createInventory(null, 54, "My GUI");
ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
for (int i = 0; i < 9; i++) gui.setItem(i, border);
// ... 20 more lines of border setup

// ✅ Good - Use builder
Inventory gui = LayoutBuilder.forInventory("My GUI", 54)
    .withBorder()
    .withTitle(Material.DIAMOND, "Title")
    .build();
```

### 4. Leverage Color Consistency

```java
// ❌ Bad - Hardcoded colors
.setDisplayName("&c&lError Message")
.setLore("&7This is secondary text", "&aThis is success")

// ✅ Good - Use color constants
.setDisplayName(GUIColors.ERROR + GUIColors.BOLD + "Error Message")
.setLore(GUIColors.secondary("This is secondary text"), 
         GUIColors.SUCCESS + "This is success")
```

## Migration Guide

### From Old NationGUI to New Framework

1. **Replace hardcoded navigation**:
   - Extend `NavigationGUI` instead of implementing `Listener`
   - Remove manual navigation stack management
   - Use `handleSpecificClick()` instead of `onInventoryClick()`

2. **Use decorators for confirmations**:
   - Replace confirmation GUIs with `ConfirmationDialog`
   - Remove manual confirmation handling code

3. **Use builders for layout**:
   - Replace manual inventory setup with `LayoutBuilder`
   - Use predefined patterns like `withCrossPattern()`

4. **Centralize through GUIManager**:
   - Register all GUIs through `GUIIntegration`
   - Use `guiManager.openGUI()` instead of `player.openInventory()`

### Example Migration

**Before:**
```java
public class OldGUI implements Listener {
    private Map<UUID, Stack<String>> navigationStack = new ConcurrentHashMap<>();
    
    public void openGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "My GUI");
        // 100 lines of manual setup
        player.openInventory(gui);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // 200 lines of click handling
    }
}
```

**After:**
```java
public class NewGUI extends NavigationGUI {
    public NewGUI(GUIManager guiManager) {
        super("My GUI", 54, guiManager);
    }
    
    @Override
    public Inventory build(Player player) {
        return LayoutBuilder.forInventory(title, size)
            .withBorder()
            .withTitle(Material.DIAMOND, "My GUI")
            .build();
    }
    
    @Override
    protected boolean handleSpecificClick(Player player, int slot, ItemStack item) {
        // Only handle specific clicks - framework handles navigation
        return false;
    }
}
```

## Advanced Features

### Custom Decorators

Create your own decorators for domain-specific patterns:

```java
public class NationInfoDialog extends GUI {
    private final Nation nation;
    private final GUIManager guiManager;
    
    public NationInfoDialog(Nation nation, GUIManager guiManager) {
        super(nation.getDisplayName() + " Info", 27);
        this.nation = nation;
        this.guiManager = guiManager;
    }
    
    // Implementation details...
}
```

### Custom Layout Patterns

Extend LayoutBuilder with your own patterns:

```java
public class CustomLayoutBuilder extends LayoutBuilder {
    public CustomLayoutBuilder withNationLayout(List<Nation> nations) {
        // Custom layout logic
        return this;
    }
}
```

### Integration with Commands

```java
public class NationsCommand implements CommandExecutor {
    private final GUIIntegration guiIntegration;
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;
        
        if (args.length == 0) {
            guiIntegration.openNationsOverviewFresh(player);
        }
        return true;
    }
}
```

## Performance Considerations

1. **Reuse GUI instances** where possible
2. **Clean up player data** in `onClose()` methods
3. **Use lazy loading** for expensive data in `build()` method
4. **Cache ItemStacks** for repeated items

## Troubleshooting

### Common Issues

1. **Navigation not working**: Ensure GUI extends `NavigationGUI` and is registered with `GUIManager`
2. **Colors not showing**: Use `GUIColors` constants and ensure color codes are translated
3. **Click events not firing**: Check that `handleSpecificClick()` returns `true` for handled clicks
4. **Memory leaks**: Ensure `onClose()` cleans up any player-specific data

### Debug Tips

1. Enable debug logging in `GUIManager`
2. Use `player.sendMessage()` in click handlers to debug slot numbers
3. Check console for any exceptions during GUI operations
4. Verify GUI registration in `GUIIntegration.registerGUIFactories()`

This enhanced framework maintains all your existing functionality while providing much cleaner, more maintainable code patterns. The old hardcoded navigation logic can be completely eliminated while preserving the exact same user experience.