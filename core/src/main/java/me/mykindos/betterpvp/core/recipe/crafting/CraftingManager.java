package me.mykindos.betterpvp.core.recipe.crafting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
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

    private final CraftingRecipeRegistry craftingRecipeRegistry;
    private final ItemFactory itemFactory;
    
    @Inject
    public CraftingManager(CraftingRecipeRegistry craftingRecipeRegistry, ItemFactory itemFactory) {
        this.craftingRecipeRegistry = craftingRecipeRegistry;
        this.itemFactory = itemFactory;
    }

    public CraftingRecipeRegistry getRegistry() {
        return craftingRecipeRegistry;
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
    public CraftingRecipe updateCraftingResult(@Nullable Player player, @NotNull Map<Integer, ItemInstance> craftingMatrix) {
        // Convert to ItemStacks for recipe matching
        Map<Integer, ItemStack> itemStackMatrix = convertToItemStacks(craftingMatrix);
        
        // Find a matching recipe
        Optional<CraftingRecipe> recipeOpt = craftingRecipeRegistry.findMatchingRecipe(itemStackMatrix, RecipeType.SHAPED_CRAFTING)
                .or(() -> craftingRecipeRegistry.findMatchingRecipe(itemStackMatrix, RecipeType.SHAPELESS_CRAFTING));
        
        // Call the prepare event
        if (player != null) {
            PrepareCraftingRecipeEvent event = new PrepareCraftingRecipeEvent(player, craftingMatrix, recipeOpt.orElse(null));
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                return null;
            }

            return event.getCraftingRecipe();
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
        Optional<CraftingRecipe> recipeOpt = craftingRecipeRegistry.findMatchingRecipe(itemStackMatrix, RecipeType.SHAPED_CRAFTING)
                .or(() -> craftingRecipeRegistry.findMatchingRecipe(itemStackMatrix, RecipeType.SHAPELESS_CRAFTING));

        if (recipeOpt.isEmpty()) {
            return null;
        }
        
        CraftingRecipe craftingRecipe = recipeOpt.get();
        ItemInstance result = craftingRecipe.createPrimaryResult();
        
        // Call the crafting event
        if (player != null) {
            CraftingRecipeEvent event = new CraftingRecipeEvent(player, craftingMatrix, craftingRecipe, result);
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                return null;
            }

            result = event.getResult();
        }
        
        // Update the crafting matrix by consuming ingredients
        Map<Integer, ItemInstance> updatedMatrix = new HashMap<>(craftingMatrix);
        consumeIngredients(craftingRecipe, updatedMatrix);
        
        // Get all results
        List<ItemInstance> results = new ArrayList<>();
        results.add(result); // Primary result (possibly modified by the event)
        
        // Add additional results
        List<ItemInstance> additionalResults = craftingRecipe.createResults();
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
     * @param craftingRecipe The recipe being crafted
     * @param craftingMatrix The items in the crafting matrix, which will be modified
     */
    private void consumeIngredients(@NotNull CraftingRecipe craftingRecipe, @NotNull Map<Integer, ItemInstance> craftingMatrix) {
        // Delegate to the recipe implementation
        craftingRecipe.consumeIngredients(craftingMatrix, itemFactory);
    }
} 