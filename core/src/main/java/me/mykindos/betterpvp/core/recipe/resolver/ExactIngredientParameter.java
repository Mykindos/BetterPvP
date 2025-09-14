package me.mykindos.betterpvp.core.recipe.resolver;

import lombok.AllArgsConstructor;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.recipe.Recipe;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipe;

import java.util.Objects;

/**
 * Matches a {@link Recipe} if one of its ingredients matches the specified item.
 */
@AllArgsConstructor
public class ExactIngredientParameter implements LookupParameter {

    private final BaseItem exactIngredient;

    @Override
    public boolean test(Recipe<?, ?> recipe) {
        for (RecipeIngredient value : recipe.getIngredients().values()) {
            if (Objects.equals(value.getBaseItem(), this.exactIngredient)) {
                return true;
            }
        }
        return false;
    }

}
