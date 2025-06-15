package me.mykindos.betterpvp.core.recipe.crafting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.recipe.Recipe;
import me.mykindos.betterpvp.core.recipe.RecipeRegistry;
import me.mykindos.betterpvp.core.recipe.RecipeType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Manager for handling crafting operations.
 * Provides methods for checking recipes, preparing crafting, and crafting items.
 */
@CustomLog
@Singleton
public class CraftingManager {

    private final RecipeRegistry recipeRegistry;
    private final ItemFactory itemFactory;
    
    @Inject
    public CraftingManager(RecipeRegistry recipeRegistry, ItemFactory itemFactory) {
        this.recipeRegistry = recipeRegistry;
        this.itemFactory = itemFactory;
    }
    
    /**
     * Converts a map of ItemInstances to a map of ItemStacks.
     *
     * @param itemInstances The map of ItemInstances to convert
     * @return A map of ItemStacks
     */
    private Map<Integer, ItemStack> convertToItemStacks(Map<Integer, ItemInstance> itemInstances) {
        final Map<Integer, ItemStack> itemStackMap = new HashMap<>();
        for (Map.Entry<Integer, ItemInstance> entry : itemInstances.entrySet()) {
            int slot = entry.getKey();
            ItemInstance instance = entry.getValue();
            if (instance != null) {
                ItemStack stack = instance.createItemStack();
                itemStackMap.put(slot, stack);
            }
        }
        return itemStackMap;
    }
    
    /**
     * Updates the result slot based on the items in the crafting matrix.
     * 
     * @param player The player viewing the crafting interface, or null if not applicable
     * @param craftingMatrix The items in the crafting matrix
     * @return The result item, or null if no recipe matches
     */
    @Nullable
    public Recipe updateCraftingResult(@Nullable Player player, @NotNull Map<Integer, ItemInstance> craftingMatrix) {
        // Convert to ItemStacks for recipe matching
        Map<Integer, ItemStack> itemStackMatrix = convertToItemStacks(craftingMatrix);
        
        // Find a matching recipe
        Optional<Recipe> recipeOpt = recipeRegistry.findMatchingRecipe(itemStackMatrix, RecipeType.SHAPED_CRAFTING)
                .or(() -> recipeRegistry.findMatchingRecipe(itemStackMatrix, RecipeType.SHAPELESS_CRAFTING));
        
        // Call the prepare event
        if (player != null) {
            PrepareCraftingRecipeEvent event = new PrepareCraftingRecipeEvent(player, craftingMatrix, recipeOpt.orElse(null));
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                return null;
            }

            return event.getRecipe();
        }

        // If no player, just return the result
        return recipeOpt.orElse(null);
    }
    
    /**
     * Crafts an item using the items in the crafting matrix.
     * 
     * @param player The player crafting the item, or null if not applicable
     * @param craftingMatrix The items in the crafting matrix
     * @return A CraftingResult containing the results and updated matrix, or null if crafting failed
     */
    @Nullable
    public CraftingResult craftItem(@Nullable Player player, @NotNull Map<Integer, ItemInstance> craftingMatrix) {
        // Convert to ItemStacks for recipe matching
        Map<Integer, ItemStack> itemStackMatrix = convertToItemStacks(craftingMatrix);
        
        // Find a matching recipe
        Optional<Recipe> recipeOpt = recipeRegistry.findMatchingRecipe(itemStackMatrix, RecipeType.SHAPED_CRAFTING)
                .or(() -> recipeRegistry.findMatchingRecipe(itemStackMatrix, RecipeType.SHAPELESS_CRAFTING));

        if (recipeOpt.isEmpty()) {
            return null;
        }
        
        Recipe recipe = recipeOpt.get();
        ItemInstance result = recipe.createPrimaryResult();
        
        // Call the crafting event
        if (player != null) {
            CraftingRecipeEvent event = new CraftingRecipeEvent(player, craftingMatrix, recipe, result);
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                return null;
            }

            result = event.getResult();
        }
        
        // Update the crafting matrix by consuming ingredients
        Map<Integer, ItemInstance> updatedMatrix = new HashMap<>(craftingMatrix);
        consumeIngredients(recipe, updatedMatrix);
        
        // Get all results
        List<ItemInstance> results = new ArrayList<>();
        results.add(result); // Primary result (possibly modified by the event)
        
        // Add additional results
        List<ItemInstance> additionalResults = recipe.createResults();
        if (additionalResults.size() > 1) {
            for (int i = 1; i < additionalResults.size(); i++) {
                results.add(additionalResults.get(i));
            }
        }

        return new CraftingResult(result,
                results.size() > 1 ? results.subList(1, results.size()) : Collections.emptyList(),
                updatedMatrix);
    }
    
    /**
     * Consumes the ingredients required for a recipe from the crafting matrix.
     * 
     * @param recipe The recipe being crafted
     * @param craftingMatrix The items in the crafting matrix, which will be modified
     */
    private void consumeIngredients(@NotNull Recipe recipe, @NotNull Map<Integer, ItemInstance> craftingMatrix) {
        // Delegate to the recipe implementation
        recipe.consumeIngredients(craftingMatrix, itemFactory);
    }
} 