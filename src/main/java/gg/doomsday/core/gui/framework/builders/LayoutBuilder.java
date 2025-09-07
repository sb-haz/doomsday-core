package gg.doomsday.core.gui.framework.builders;

import gg.doomsday.core.gui.framework.GUIBuilder;
import gg.doomsday.core.gui.utils.ItemBuilder;
import gg.doomsday.core.gui.utils.GUIColors;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builder for common GUI layouts and patterns
 * Provides fluent API for creating standardized GUI layouts
 */
public class LayoutBuilder {
    
    private final Inventory inventory;
    private final int size;
    
    public LayoutBuilder(Inventory inventory) {
        this.inventory = inventory;
        this.size = inventory.getSize();
    }
    
    /**
     * Create a new layout builder for an inventory
     */
    public static LayoutBuilder forInventory(String title, int size) {
        Inventory inv = GUIBuilder.createInventory(title, size);
        return new LayoutBuilder(inv);
    }
    
    /**
     * Add a glass pane border around the inventory
     */
    public LayoutBuilder withBorder() {
        return withBorder(Material.GRAY_STAINED_GLASS_PANE, "");
    }
    
    /**
     * Add a custom border with specified material and name
     */
    public LayoutBuilder withBorder(Material material, String name) {
        ItemStack borderItem = new ItemBuilder(material).setDisplayName(name).build();
        
        if (size == 54) {
            // 6-row inventory border
            for (int i = 0; i < 9; i++) {
                inventory.setItem(i, borderItem); // Top row
                inventory.setItem(45 + i, borderItem); // Bottom row
            }
            for (int row = 1; row < 5; row++) {
                inventory.setItem(row * 9, borderItem); // Left column
                inventory.setItem(row * 9 + 8, borderItem); // Right column
            }
        } else if (size == 27) {
            // 3-row inventory border
            for (int i = 0; i < 9; i++) {
                inventory.setItem(i, borderItem); // Top row
                inventory.setItem(18 + i, borderItem); // Bottom row
            }
            inventory.setItem(9, borderItem); // Left middle
            inventory.setItem(17, borderItem); // Right middle
        }
        
        return this;
    }
    
    /**
     * Add a title item in the center top of the GUI
     */
    public LayoutBuilder withTitle(Material material, String title, String... lore) {
        int titleSlot = size == 54 ? 4 : size == 27 ? 4 : 0;
        
        ItemBuilder builder = new ItemBuilder(material).setDisplayName(title);
        if (lore.length > 0) {
            builder.setLore(lore);
        }
        
        inventory.setItem(titleSlot, builder.build());
        return this;
    }
    
    /**
     * Fill a row with items from a list
     */
    public LayoutBuilder withRowItems(int startRow, List<ItemStack> items) {
        int startSlot = startRow * 9;
        int maxItems = Math.min(items.size(), 9);
        
        for (int i = 0; i < maxItems; i++) {
            int slot = startSlot + i;
            if (slot < size) {
                inventory.setItem(slot, items.get(i));
            }
        }
        
        return this;
    }
    
    /**
     * Create a grid layout in the center content area
     */
    public LayoutBuilder withGridItems(List<ItemStack> items) {
        int[] slots = size == 54 ? GUIBuilder.getContentSlots54() : GUIBuilder.getContentSlots27();
        List<Integer> contentSlots = Arrays.stream(slots).boxed().collect(Collectors.toList());
        
        int maxItems = Math.min(items.size(), contentSlots.size());
        for (int i = 0; i < maxItems; i++) {
            inventory.setItem(contentSlots.get(i), items.get(i));
        }
        
        return this;
    }
    
    /**
     * Add a cross/plus pattern in the center (like your nation layout)
     */
    public <T> LayoutBuilder withCrossPattern(T north, T south, T east, T west, T center,
                                            Function<T, ItemStack> itemCreator) {
        if (size != 54) {
            throw new IllegalStateException("Cross pattern only works with 54-slot GUIs");
        }
        
        // Cross pattern slots for 54-slot GUI
        if (north != null) inventory.setItem(13, itemCreator.apply(north));   // North
        if (west != null) inventory.setItem(21, itemCreator.apply(west));     // West
        if (center != null) inventory.setItem(22, itemCreator.apply(center)); // Center
        if (east != null) inventory.setItem(23, itemCreator.apply(east));     // East
        if (south != null) inventory.setItem(31, itemCreator.apply(south));   // South
        
        return this;
    }
    
    /**
     * Add a diamond pattern layout (5 items in diamond shape)
     */
    public <T> LayoutBuilder withDiamondPattern(T top, T left, T center, T right, T bottom,
                                              Function<T, ItemStack> itemCreator) {
        if (size != 54) {
            throw new IllegalStateException("Diamond pattern only works with 54-slot GUIs");
        }
        
        // Diamond pattern slots
        if (top != null) inventory.setItem(4, itemCreator.apply(top));       // Top
        if (left != null) inventory.setItem(20, itemCreator.apply(left));    // Left
        if (center != null) inventory.setItem(22, itemCreator.apply(center)); // Center
        if (right != null) inventory.setItem(24, itemCreator.apply(right));   // Right
        if (bottom != null) inventory.setItem(40, itemCreator.apply(bottom)); // Bottom
        
        return this;
    }
    
    /**
     * Add control buttons at the bottom of the GUI
     */
    public LayoutBuilder withControlButtons(ItemStack... buttons) {
        if (size == 54) {
            // Bottom row control positions: 46-52 (avoiding corners)
            int[] controlSlots = {46, 47, 48, 49, 50, 51, 52};
            for (int i = 0; i < Math.min(buttons.length, controlSlots.length); i++) {
                inventory.setItem(controlSlots[i], buttons[i]);
            }
        } else if (size == 27) {
            // Bottom row control positions: 19-25 (avoiding corners)
            int[] controlSlots = {19, 20, 21, 22, 23, 24, 25};
            for (int i = 0; i < Math.min(buttons.length, controlSlots.length); i++) {
                inventory.setItem(controlSlots[i], buttons[i]);
            }
        }
        
        return this;
    }
    
    /**
     * Add a back button at the standard position
     */
    public LayoutBuilder withBackButton() {
        return withBackButton("&7« Back", "&7Return to previous menu");
    }
    
    /**
     * Add a custom back button
     */
    public LayoutBuilder withBackButton(String name, String lore) {
        int backSlot = size == 54 ? 45 : 18; // Bottom-left corner
        
        inventory.setItem(backSlot, new ItemBuilder(Material.ARROW)
                .setDisplayName(name)
                .setLore(lore)
                .build());
        
        return this;
    }
    
    /**
     * Add a close button at the standard position
     */
    public LayoutBuilder withCloseButton() {
        return withCloseButton("&c&lClose", "&cClose this menu");
    }
    
    /**
     * Add a custom close button
     */
    public LayoutBuilder withCloseButton(String name, String lore) {
        int closeSlot = size == 54 ? 53 : 26; // Bottom-right corner
        
        inventory.setItem(closeSlot, new ItemBuilder(Material.BARRIER)
                .setDisplayName(name)
                .setLore(lore)
                .build());
        
        return this;
    }
    
    /**
     * Add a separator line at the specified row
     */
    public LayoutBuilder withSeparator(int row, Material material) {
        int startSlot = row * 9;
        ItemStack separator = new ItemBuilder(material).setDisplayName("").build();
        
        for (int i = 0; i < 9 && startSlot + i < size; i++) {
            inventory.setItem(startSlot + i, separator);
        }
        
        return this;
    }
    
    /**
     * Add custom item at specific slot
     */
    public LayoutBuilder withItem(int slot, ItemStack item) {
        if (slot >= 0 && slot < size) {
            inventory.setItem(slot, item);
        }
        return this;
    }
    
    /**
     * Add custom item at specific slot using builder pattern
     */
    public LayoutBuilder withItem(int slot, Material material, String name, String... lore) {
        ItemBuilder builder = new ItemBuilder(material).setDisplayName(name);
        if (lore.length > 0) {
            builder.setLore(lore);
        }
        return withItem(slot, builder.build());
    }
    
    /**
     * Get the built inventory
     */
    public Inventory build() {
        return inventory;
    }
}