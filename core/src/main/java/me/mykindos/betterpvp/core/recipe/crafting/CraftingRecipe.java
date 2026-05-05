package me.mykindos.betterpvp.core.recipe.crafting;

import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.recipe.Recipe;

/**
 * Base interface for all recipe types in the system.
 * Recipes define how items can be combined to create new items.
 */
public interface CraftingRecipe extends Recipe<ItemInstance> {

    /**
     * Checks if this recipe requires a blueprint to be crafted.
     * @return true if a blueprint is needed, false otherwise
     */
    default boolean needsBlueprint() {
        return false;
    }
}
