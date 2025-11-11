package me.mykindos.betterpvp.core.recipe.smelting;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipe;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.RecipeType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service class for handling smelting operations.
 * Provides validation, recipe matching, and smelting execution.
 */
@Singleton
public class SmeltingService {
    
    private final SmeltingRecipeRegistry smeltingRecipeRegistry;
    private final ItemFactory itemFactory;
    
    @Inject
    private SmeltingService(SmeltingRecipeRegistry smeltingRecipeRegistry,
                            ItemFactory itemFactory) {
        this.smeltingRecipeRegistry = smeltingRecipeRegistry;
        this.itemFactory = itemFactory;
    }
    
    /**
     * Validates that an item can be placed in a smelter.
     * Only items that are ingredients in smelting recipes can be smelted.
     * 
     * @param itemStack The item to validate
     * @return true if the item can be smelted, false otherwise
     */
    public boolean canBeSmelted(@NotNull ItemStack itemStack) {
        if (itemStack.getType().isAir()) {
            return false;
        }
        
        Optional<ItemInstance> instance = itemFactory.fromItemStack(itemStack);
        if (instance.isEmpty()) {
            return false;
        }

        return smeltingRecipeRegistry.isSmeltable(instance.get().getBaseItem());
    }
    
    /**
     * Validates that a list of items can all be smelted.
     * @param items The items to validate
     * @return true if all items can be smelted, false otherwise
     */
    public boolean canAllBeSmelted(@NotNull List<ItemInstance> items) {
        return items.stream()
                .allMatch(item -> smeltingRecipeRegistry.isSmeltable(item.getBaseItem()));
    }
    
    /**
     * Finds a smelting recipe that matches the given items
     * @param items Map of slot positions to item stacks
     * @return The matching smelting recipe, or empty if none found
     */
    public Optional<SmeltingRecipe> findMatchingRecipe(@NotNull Map<Integer, ItemStack> items) {
        return smeltingRecipeRegistry.getRecipes().stream()
                .filter(recipe -> recipe.matches(items))
                .findFirst();
    }
    
    /**
     * Executes a smelting recipe, consuming ingredients and producing liquid alloys.
     * @param recipe The recipe to execute
     * @param matrix The current items in the smelter
     * @return The smelting result containing liquid alloys
     */
    public SmeltingResult executeRecipe(@NotNull SmeltingRecipe recipe, @NotNull Map<Integer, ItemInstance> matrix) {
        // Consume ingredients from the crafting matrix
        recipe.consumeIngredients(matrix, itemFactory);

        // Create the primary result
        return recipe.createPrimaryResult();
    }
    
    /**
     * Gets all recipes that can be made with the given items (ignoring temperature).
     * Useful for showing possible recipes in UI.
     * @param items The available items
     * @return A list of possible smelting recipes
     */
    public List<SmeltingRecipe> getPossibleRecipes(@NotNull Map<Integer, ItemStack> items) {
        return smeltingRecipeRegistry.getRecipes().stream()
                .filter(recipe -> recipe.matches(items))
                .toList();
    }
    
    /**
     * Checks if the smelter has valid items for any smelting recipe.
     * @param items The items to check
     * @return true if there are items that could potentially be smelted
     */
    public boolean hasValidSmeltingItems(@NotNull Map<Integer, ItemStack> items) {
        for (ItemStack stack : items.values()) {
            if (stack != null && !stack.getType().isAir() && canBeSmelted(stack)) {
                return true;
            }
        }
        return false;
    }
} 