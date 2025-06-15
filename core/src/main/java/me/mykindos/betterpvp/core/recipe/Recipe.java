package me.mykindos.betterpvp.core.recipe;

import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Base interface for all recipe types in the system.
 * Recipes define how items can be combined to create new items.
 */
public interface Recipe {
    
    /**
     * Gets the primary result of this recipe.
     *
     * @return The ItemInstance that will be created when this recipe is crafted
     */
    @NotNull BaseItem getPrimaryResult();
    
    /**
     * Gets all results of this recipe.
     *
     * @return A list of ItemInstances that will be created when this recipe is crafted
     */
    @NotNull List<BaseItem> getResults();

    /**
     * Creates the primary result of this recipe.
     * @return An ItemInstance representing the primary result of this recipe
     */
    @NotNull ItemInstance createPrimaryResult();

    /**
     * Creates all results of this recipe.
     * @return A list of ItemInstances representing all results of this recipe
     */
    @NotNull List<ItemInstance> createResults();
    
    /**
     * Checks if the provided items match this recipe.
     * @param items A map of slot indices to ItemStacks
     * @return true if the items match this recipe, false otherwise
     */
    boolean matches(@NotNull Map<Integer, ItemStack> items);
    
    /**
     * Gets the ingredients required for this recipe.
     * @return A map of slot indices to ingredient requirements
     */
    @NotNull Map<Integer, RecipeIngredient> getIngredients();
    
    /**
     * Gets the type of this recipe.
     * @return The recipe type
     */
    @NotNull RecipeType getType();
    
    /**
     * Consumes the ingredients required for this recipe from the crafting matrix.
     * 
     * @param craftingMatrix The items in the crafting matrix
     * @param itemFactory The ItemFactory to use for item creation
     * @return A list of slots that were consumed from
     */
    @NotNull List<Integer> consumeIngredients(@NotNull Map<Integer, ItemInstance> craftingMatrix, @NotNull ItemFactory itemFactory);
    
    /**
     * Checks if this recipe can be crafted by the player in the current context.
     * This can be used to restrict recipes to specific conditions, such as requiring 
     * a blueprint to be unlocked.
     * 
     * @param player The player attempting to craft the recipe
     * @return true if the recipe can be crafted, false otherwise
     */
    default boolean canCraft(@Nullable Player player) {
        return true;
    }

    /**
     * Checks if this recipe requires a blueprint to be crafted.
     * @return true if a blueprint is needed, false otherwise
     */
    default boolean needsBlueprint() {
        return false;
    }
} 