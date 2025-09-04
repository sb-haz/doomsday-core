package gg.doomsday.core.items;

import gg.doomsday.core.defense.ReinforcedBlockManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class ReinforcementHandler implements Listener {
    
    private final JavaPlugin plugin;
    private final ReinforcedBlockManager reinforcedBlockManager;
    private final CustomItemManager customItemManager;
    
    public ReinforcementHandler(JavaPlugin plugin, ReinforcedBlockManager reinforcedBlockManager) {
        this.plugin = plugin;
        this.reinforcedBlockManager = reinforcedBlockManager;
        this.customItemManager = new CustomItemManager(plugin);
        registerRecipe();
    }
    
    
    private void registerRecipe() {
        try {
            // Remove existing recipe if it exists (for reloads)
            NamespacedKey key = new NamespacedKey(plugin, "reinforcement_powder");
            Bukkit.removeRecipe(key);
            
            // Create dummy recipe result (will be replaced by PrepareItemCraftEvent)
            ItemStack dummyResult = new ItemStack(Material.PAPER);
            ItemMeta dummyMeta = dummyResult.getItemMeta();
            dummyMeta.setDisplayName("§6Reinforcement Powder");
            dummyMeta.setCustomModelData(9999); // Special marker for our recipe
            dummyResult.setItemMeta(dummyMeta);
            
            // Create recipe based on configuration
            ShapelessRecipe recipe = new ShapelessRecipe(key, dummyResult);
            
            // Add ingredients from configuration
            List<String> ingredients = customItemManager.getRecipeIngredients();
            for (String ingredientName : ingredients) {
                try {
                    Material ingredient = Material.valueOf(ingredientName.toUpperCase());
                    recipe.addIngredient(ingredient);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid ingredient '" + ingredientName + "' in custom_items.yml, skipping");
                }
            }
            
            // Register the recipe
            boolean registered = Bukkit.addRecipe(recipe);
            if (registered) {
                plugin.getLogger().info("Successfully registered reinforcement powder recipe (dummy)");
                plugin.getLogger().info("Recipe configured from custom_items.yml");
            } else {
                plugin.getLogger().warning("Failed to register reinforcement powder recipe - recipe might already exist");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error registering reinforcement powder recipe: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null) return;
        
        ItemStack result = event.getRecipe().getResult();
        // Check if this is our dummy recipe (Paper with custom model data 9999)
        if (result.getType() != Material.PAPER || 
            !result.hasItemMeta() || 
            !result.getItemMeta().hasCustomModelData() ||
            result.getItemMeta().getCustomModelData() != 9999) {
            return;
        }
        
        // Check if the crafting matrix contains exactly 1 iron ingot and 1 stone
        boolean hasIron = false;
        boolean hasStone = false;
        int totalItems = 0;
        
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (item == null || item.getType() == Material.AIR) continue;
            totalItems++;
            
            if (item.getType() == Material.IRON_INGOT) {
                hasIron = true;
            } else if (item.getType() == Material.STONE) {
                hasStone = true;
            }
        }
        
        // Only proceed if we have exactly iron + stone (2 items total)
        if (hasIron && hasStone && totalItems == 2) {
            // Replace the result with our custom reinforcement powder
            event.getInventory().setResult(customItemManager.createReinforcementPowder());
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        // Check if player is holding reinforcement powder
        if (!customItemManager.isReinforcementPowder(item)) {
            return;
        }
        
        // Check if player right-clicked a block
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }
        
        event.setCancelled(true); // Prevent other interactions
        
        // Check if block is already reinforced
        if (reinforcedBlockManager.isReinforced(clickedBlock.getLocation())) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        
        // Check if block can be reinforced
        if (!reinforcedBlockManager.isValidBlock(clickedBlock.getType())) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        
        // Reinforce the block
        if (reinforcedBlockManager.reinforceBlock(clickedBlock.getLocation())) {
            // Success - remove one powder from inventory
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
            
            // Visual and audio feedback
            clickedBlock.getWorld().spawnParticle(
                Particle.CRIT_MAGIC, 
                clickedBlock.getLocation().add(0.5, 0.5, 0.5), 
                20, 0.3, 0.3, 0.3, 0.1
            );
            
            clickedBlock.getWorld().spawnParticle(
                Particle.VILLAGER_HAPPY,
                clickedBlock.getLocation().add(0.5, 1.0, 0.5),
                10, 0.2, 0.2, 0.2, 0.0
            );
            
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.5f);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
            
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
    

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        
        // If this block was reinforced, remove it from the reinforcement list
        if (reinforcedBlockManager.isReinforced(block.getLocation())) {
            reinforcedBlockManager.removeReinforcement(block.getLocation());
            
            // Visual feedback only
            block.getWorld().spawnParticle(
                Particle.BLOCK_CRACK,
                block.getLocation().add(0.5, 0.5, 0.5),
                15, 0.3, 0.3, 0.3, 0.1,
                block.getBlockData()
            );
        }
    }
    
    public ItemStack getReinforcementPowder() {
        return customItemManager.createReinforcementPowder();
    }
    
    public ItemStack getReinforcementPowder(int amount) {
        ItemStack powder = customItemManager.createReinforcementPowder();
        powder.setAmount(amount);
        return powder;
    }
    
    public void reloadCustomItems() {
        customItemManager.reloadCustomItemsConfig();
    }
    
    public void unregisterRecipe() {
        try {
            NamespacedKey key = new NamespacedKey(plugin, "reinforcement_powder");
            boolean removed = Bukkit.removeRecipe(key);
            if (removed) {
                plugin.getLogger().info("Successfully unregistered reinforcement powder recipe");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to unregister reinforcement powder recipe: " + e.getMessage());
        }
    }
    
    public CustomItemManager getCustomItemManager() {
        return customItemManager;
    }
}