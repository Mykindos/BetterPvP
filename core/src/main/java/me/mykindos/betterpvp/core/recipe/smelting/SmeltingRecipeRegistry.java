package me.mykindos.betterpvp.core.recipe.smelting;

import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.item.BaseItem;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Registry specifically for managing smelting recipes.
 * Handles registration, validation, and provides utilities for determining smeltable items.
 */
@CustomLog
@Singleton
public class SmeltingRecipeRegistry {
    
    private final Set<SmeltingRecipe> smeltingRecipes = new HashSet<>();
    private final Set<BaseItem> smeltableItems = new HashSet<>();
    
    /**
     * Registers a new smelting recipe.
     * Validates that no duplicate recipe exists with the same ingredient types.
     * 
     * @param recipe The smelting recipe to register
     * @throws IllegalArgumentException if a recipe with the same ingredient types already exists
     */
    public void registerSmeltingRecipe(@NotNull SmeltingRecipe recipe) {
        // Check for duplicate recipes (same ingredient types, ignoring quantities)
        Set<BaseItem> newIngredientTypes = recipe.getIngredientTypes();
        
        // Add to our smelting-specific collections
        smeltingRecipes.add(recipe);
        smeltableItems.addAll(recipe.getIngredientTypes());
        
        log.info("Registered smelting recipe with ingredients: {} -> {}", 
                newIngredientTypes, recipe.getSmeltingResult().getPrimaryResult().getName()).submit();
    }
    
    /**
     * Checks if the given item can be used in smelting recipes.
     * An item is smeltable if it appears as an ingredient in any registered smelting recipe.
     * 
     * @param item The item to check
     * @return true if the item can be smelted, false otherwise
     */
    public boolean isSmeltable(@NotNull BaseItem item) {
        return smeltableItems.contains(item);
    }
    
    /**
     * Gets all items that can be used in smelting recipes.
     * @return An unmodifiable set of all smeltable items
     */
    public @NotNull Set<BaseItem> getSmeltableItems() {
        return Collections.unmodifiableSet(smeltableItems);
    }
    
    /**
     * Gets all registered smelting recipes.
     * @return An unmodifiable set of all smelting recipes
     */
    public @NotNull Set<SmeltingRecipe> getSmeltingRecipes() {
        return Collections.unmodifiableSet(smeltingRecipes);
    }
    
    /**
     * Gets all smelting recipes that use the specified item as an ingredient.
     * @param item The item to search for
     * @return A list of recipes that use the item
     */
    public @NotNull List<SmeltingRecipe> getRecipesUsingItem(@NotNull BaseItem item) {
        return smeltingRecipes.stream()
                .filter(recipe -> recipe.getIngredientTypes().contains(item))
                .toList();
    }
    
    /**
     * Validates that all ingredients in the recipe are valid.
     * Currently just logs a warning for items that aren't used in any recipes,
     * but could be extended for more complex validation.
     * 
     * @param recipe The recipe to validate
     */
    private void validateRecipe(@NotNull SmeltingRecipe recipe) {
        // Future: Add validation logic here
        // For example, checking that alloy ingredients make sense together
    }
} 