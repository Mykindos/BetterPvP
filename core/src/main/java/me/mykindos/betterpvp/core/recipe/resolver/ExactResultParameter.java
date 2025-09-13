package me.mykindos.betterpvp.core.recipe.resolver;

import lombok.AllArgsConstructor;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.recipe.Recipe;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipe;

import java.util.Objects;

/**
 * Matches a {@link CraftingRecipe} if the primary result of the recipe matches the specified item.
 */
@AllArgsConstructor
public class ExactResultParameter implements LookupParameter {

    private final BaseItem exactResult;

    @Override
    public boolean test(Recipe<?, ?> recipe) {
        return recipe instanceof CraftingRecipe && Objects.equals(recipe.getPrimaryResult(), this.exactResult);
    }

}
