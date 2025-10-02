package me.mykindos.betterpvp.core.recipe.resolver;

import lombok.AllArgsConstructor;
import me.mykindos.betterpvp.core.anvil.AnvilRecipe;
import me.mykindos.betterpvp.core.imbuement.ImbuementRecipe;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.metal.casting.CastingMoldRecipe;
import me.mykindos.betterpvp.core.recipe.Recipe;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipe;
import me.mykindos.betterpvp.core.recipe.smelting.SmeltingRecipe;

import java.util.Objects;

/**
 * Matches a {@link Recipe} if the primary result of the recipe matches the specified item.
 */
@AllArgsConstructor
public class ExactResultParameter implements LookupParameter {

    private final BaseItem exactResult;

    @Override
    public boolean test(Recipe<?, ?> recipe) {
        if (recipe instanceof CastingMoldRecipe castingMoldRecipe) {
            return Objects.equals(castingMoldRecipe.getResult(), this.exactResult);
        } else if (recipe instanceof CraftingRecipe craftingRecipe) {
            return Objects.equals(craftingRecipe.getPrimaryResult().getBaseItem(), this.exactResult);
        } else if (recipe instanceof AnvilRecipe anvilRecipe) {
            return Objects.equals(anvilRecipe.getPrimaryResult().getPrimaryResult(), this.exactResult);
        } else if (recipe instanceof ImbuementRecipe imbuementRecipe) {
            return Objects.equals(imbuementRecipe.getPrimaryResult().getPrimaryResult(), this.exactResult);
        } else if (recipe instanceof SmeltingRecipe smeltingRecipe) {
            return false; // Smelting recipes yield ALLOYS
        } else {
            throw new IllegalArgumentException("Unsupported recipe type: " + recipe.getClass().getName());
        }
    }

}
