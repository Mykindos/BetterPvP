package me.mykindos.betterpvp.core.recipe.minecraft;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipe;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import me.mykindos.betterpvp.core.recipe.crafting.ShapelessCraftingRecipe;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Adapter for working with Minecraft's built-in recipes.
 * Handles finding, matching, and converting Minecraft recipes to our custom recipe format.
 */
@CustomLog
@Singleton
public class MinecraftRecipeAdapter {

    private final ItemFactory itemFactory;
    private final Set<NamespacedKey> disabledRecipes = new HashSet<>();
    private boolean enabled = true;

    @Inject
    private MinecraftRecipeAdapter(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;
    }

    /**
     * Finds a matching Minecraft recipe for the given items.
     * 
     * @param items The items to match against
     * @return The first matching Minecraft recipe, or empty if none match
     */
    @NotNull
    public Optional<org.bukkit.inventory.Recipe> findMatchingRecipe(@NotNull Map<Integer, ItemStack> items) {
        if (!enabled) {
            return Optional.empty();
        }
        
        // Convert our item map to a 3x3 crafting grid
        ItemStack[] craftingGrid = new ItemStack[9];
        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            int slot = entry.getKey();
            if (slot >= 0 && slot < 9) {
                craftingGrid[slot] = entry.getValue();
            }
        }
        
        // Try to match against Minecraft recipes
        Iterator<org.bukkit.inventory.Recipe> recipeIterator = Bukkit.recipeIterator();
        while (recipeIterator.hasNext()) {
            org.bukkit.inventory.Recipe recipe = recipeIterator.next();
            
            // Skip disabled recipes - check if recipe has a key and if it's disabled
            try {
                if (recipe.getClass().getMethod("getKey").invoke(recipe) instanceof NamespacedKey key 
                        && disabledRecipes.contains(key)) {
                    continue;
                }
            } catch (Exception ignored) {
                // If we can't get the key, just continue with the recipe
            }
            
            if (recipe instanceof org.bukkit.inventory.ShapedRecipe shapedRecipe) {
                if (matchesShapedRecipe(shapedRecipe, craftingGrid)) {
                    return Optional.of(recipe);
                }
            } else if (recipe instanceof org.bukkit.inventory.ShapelessRecipe shapelessRecipe) {
                if (matchesShapelessRecipe(shapelessRecipe, craftingGrid)) {
                    return Optional.of(recipe);
                }
            }
            // Other recipe types not supported yet
        }
        
        return Optional.empty();
    }

    /**
     * Converts a Minecraft recipe to our recipe format.
     * 
     * @param minecraftRecipe The Minecraft recipe to convert
     * @return Our recipe format
     */
    public CraftingRecipe convertToCustomRecipe(org.bukkit.inventory.Recipe minecraftRecipe) {
        ItemStack resultStack = minecraftRecipe.getResult();
        BaseItem result = itemFactory.fromItemStack(resultStack).orElseThrow().getBaseItem();
        
        if (minecraftRecipe instanceof org.bukkit.inventory.ShapedRecipe shapedRecipe) {
            return convertShapedRecipe(shapedRecipe, result);
        } else if (minecraftRecipe instanceof org.bukkit.inventory.ShapelessRecipe shapelessRecipe) {
            return convertShapelessRecipe(shapelessRecipe, result);
        } else {
            // Default to a simple recipe
            return new SimpleMinecraftCraftingRecipe(result, minecraftRecipe, this, itemFactory);
        }
    }
    
    /**
     * Converts a Minecraft shaped recipe to our recipe format.
     * 
     * @param shapedRecipe The Minecraft shaped recipe to convert
     * @param result Our result ItemInstance
     * @return Our shaped recipe format
     */
    private ShapedCraftingRecipe convertShapedRecipe(org.bukkit.inventory.ShapedRecipe shapedRecipe, BaseItem result) {
        // Get the recipe shape and choice map
        String[] shape = shapedRecipe.getShape();
        Map<Character, RecipeChoice> choiceMap = shapedRecipe.getChoiceMap();
        
        // Create a builder for our shaped recipe
        ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(result, shape, itemFactory);
        
        // Add the ingredients
        for (Map.Entry<Character, RecipeChoice> entry : choiceMap.entrySet()) {
            char key = entry.getKey();
            RecipeChoice choice = entry.getValue();
            
            if (choice instanceof RecipeChoice.MaterialChoice materialChoice) {
                // Use the first material as the ingredient
                Material material = materialChoice.getChoices().getFirst();
                ItemStack itemStack = new ItemStack(material);
                ItemInstance instance = itemFactory.fromItemStack(itemStack).orElseThrow();
                builder.setIngredient(key, new RecipeIngredient(instance.getBaseItem(), 1));
            } else if (choice instanceof RecipeChoice.ExactChoice exactChoice) {
                // Use the first item as the ingredient
                ItemStack item = exactChoice.getChoices().getFirst();
                ItemInstance instance = itemFactory.fromItemStack(item).orElseThrow();
                builder.setIngredient(key, new RecipeIngredient(instance.getBaseItem(), 1));
            }
        }
        
        return builder.build();
    }
    
    /**
     * Converts a Minecraft shapeless recipe to our recipe format.
     * 
     * @param shapelessRecipe The Minecraft shapeless recipe to convert
     * @param result Our result ItemInstance
     * @return Our shapeless recipe format
     */
    private ShapelessCraftingRecipe convertShapelessRecipe(org.bukkit.inventory.ShapelessRecipe shapelessRecipe, BaseItem result) {
        List<RecipeIngredient> ingredients = new ArrayList<>();
        
        // Convert each choice to an ingredient
        for (RecipeChoice choice : shapelessRecipe.getChoiceList()) {
            if (choice instanceof RecipeChoice.MaterialChoice materialChoice) {
                // Use the first material as the ingredient
                Material material = materialChoice.getChoices().getFirst();
                ItemStack itemStack = new ItemStack(material);
                ItemInstance instance = itemFactory.fromItemStack(itemStack).orElseThrow();
                ingredients.add(new RecipeIngredient(instance.getBaseItem(), 1));
            } else if (choice instanceof RecipeChoice.ExactChoice exactChoice) {
                // Use the first item as the ingredient
                ItemStack item = exactChoice.getChoices().getFirst();
                ItemInstance instance = itemFactory.fromItemStack(item).orElseThrow();
                ingredients.add(new RecipeIngredient(instance.getBaseItem(), 1));
            }
        }
        
        Map<Integer, RecipeIngredient> ingredientMap = new HashMap<>();
        for (int i = 0; i < ingredients.size(); i++) {
            ingredientMap.put(i, ingredients.get(i));
        }
        
        return new ShapelessCraftingRecipe(result, ingredientMap, itemFactory, false);
    }
    
    /**
     * Checks if the given items match a shaped Minecraft recipe.
     * 
     * @param recipe The shaped recipe to check
     * @param craftingGrid The items in the crafting grid
     * @return True if the items match the recipe, false otherwise
     */
    private boolean matchesShapedRecipe(org.bukkit.inventory.ShapedRecipe recipe, ItemStack[] craftingGrid) {
        // Get the recipe shape
        String[] shape = recipe.getShape();
        Map<Character, RecipeChoice> choiceMap = recipe.getChoiceMap();
        
        // Try all possible positions in the crafting grid
        for (int startRow = 0; startRow <= 3 - shape.length; startRow++) {
            for (int startCol = 0; startCol <= 3 - shape[0].length(); startCol++) {
                if (matchesShapeAtPosition(recipe, craftingGrid, startRow, startCol)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Checks if the given items match a shaped Minecraft recipe at a specific position.
     * 
     * @param recipe The shaped recipe to check
     * @param craftingGrid The items in the crafting grid
     * @param startRow The starting row in the grid
     * @param startCol The starting column in the grid
     * @return True if the items match the recipe at the given position, false otherwise
     */
    private boolean matchesShapeAtPosition(org.bukkit.inventory.ShapedRecipe recipe, ItemStack[] craftingGrid, int startRow, int startCol) {
        String[] shape = recipe.getShape();
        Map<Character, RecipeChoice> choiceMap = recipe.getChoiceMap();
        
        // Check if items outside the recipe shape are empty
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int gridIndex = row * 3 + col;
                
                if (row < startRow || row >= startRow + shape.length || 
                    col < startCol || col >= startCol + shape[0].length()) {
                    // This position is outside the recipe shape, should be empty
                    if (craftingGrid[gridIndex] != null && !craftingGrid[gridIndex].getType().isAir()) {
                        return false;
                    }
                    continue;
                }
                
                // This position is inside the recipe shape
                int recipeRow = row - startRow;
                int recipeCol = col - startCol;
                char ingredientChar = shape[recipeRow].charAt(recipeCol);
                
                if (ingredientChar == ' ') {
                    // Space means no ingredient, slot should be empty
                    if (craftingGrid[gridIndex] != null && !craftingGrid[gridIndex].getType().isAir()) {
                        return false;
                    }
                } else {
                    // Check if the item matches the recipe choice
                    RecipeChoice choice = choiceMap.get(ingredientChar);
                    if (choice == null) {
                        // No choice for this character, slot should be empty
                        if (craftingGrid[gridIndex] != null && !craftingGrid[gridIndex].getType().isAir()) {
                            return false;
                        }
                    } else {
                        // Check if the item matches the choice
                        if (craftingGrid[gridIndex] == null || !choice.test(craftingGrid[gridIndex])) {
                            return false;
                        }
                    }
                }
            }
        }
        
        return true;
    }
    
    /**
     * Checks if the given items match a shapeless Minecraft recipe.
     * 
     * @param recipe The shapeless recipe to check
     * @param craftingGrid The items in the crafting grid
     * @return True if the items match the recipe, false otherwise
     */
    private boolean matchesShapelessRecipe(org.bukkit.inventory.ShapelessRecipe recipe, ItemStack[] craftingGrid) {
        List<RecipeChoice> remainingChoices = new ArrayList<>(recipe.getChoiceList());
        List<ItemStack> remainingItems = new ArrayList<>();
        
        // Collect non-null items from the crafting grid
        for (ItemStack item : craftingGrid) {
            if (item != null && !item.getType().isAir()) {
                remainingItems.add(item);
            }
        }
        
        // Check if the number of items matches
        if (remainingItems.size() != remainingChoices.size()) {
            return false;
        }
        
        // Try to match each item with a choice
        for (ItemStack item : new ArrayList<>(remainingItems)) {
            boolean matched = false;
            
            for (RecipeChoice choice : new ArrayList<>(remainingChoices)) {
                if (choice.test(item)) {
                    remainingChoices.remove(choice);
                    remainingItems.remove(item);
                    matched = true;
                    break;
                }
            }
            
            if (!matched) {
                return false;
            }
        }
        
        return remainingItems.isEmpty() && remainingChoices.isEmpty();
    }
    
    /**
     * Enables or disables the use of Minecraft's default recipes.
     * 
     * @param enabled True to enable, false to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        log.info("Minecraft default recipes are now {}", enabled ? "enabled" : "disabled").submit();
    }
    
    /**
     * Checks if Minecraft's default recipes are enabled.
     * 
     * @return True if enabled, false if disabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Disables a specific Minecraft recipe by its key.
     * 
     * @param key The key of the recipe to disable
     * @return True if the recipe was disabled, false if it was already disabled
     */
    public boolean disableRecipe(@NotNull NamespacedKey key) {
        boolean added = disabledRecipes.add(key);
        if (added) {
            log.info("Disabled Minecraft recipe: {}", key).submit();
        }
        return added;
    }
    
    /**
     * Enables a previously disabled Minecraft recipe by its key.
     * 
     * @param key The key of the recipe to enable
     * @return True if the recipe was enabled, false if it was already enabled
     */
    public boolean enableRecipe(@NotNull NamespacedKey key) {
        boolean removed = disabledRecipes.remove(key);
        if (removed) {
            log.info("Enabled Minecraft recipe: {}", key).submit();
        }
        return removed;
    }
    
    /**
     * Gets all disabled Minecraft recipes.
     * 
     * @return An unmodifiable set of disabled recipe keys
     */
    @NotNull
    public Set<NamespacedKey> getDisabledRecipes() {
        return Collections.unmodifiableSet(disabledRecipes);
    }
} 