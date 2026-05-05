package me.mykindos.betterpvp.core.recipe.resolver;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.anvil.AnvilRecipe;
import me.mykindos.betterpvp.core.imbuement.ImbuementRecipe;
import me.mykindos.betterpvp.core.imbuement.StandardImbuementRecipe;
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
@EqualsAndHashCode
public class ExactResultParameter implements LookupParameter {

    private final BaseItem exactResult;

    @Override
    public boolean test(Recipe<?> recipe) {
        if (recipe instanceof CastingMoldRecipe castingMoldRecipe) {
            return Objects.equals(castingMoldRecipe.getResult(), this.exactResult);
        } else if (recipe instanceof CraftingRecipe craftingRecipe) {
            // Crafting recipes expose result base item directly via the recipe; previewResult would
            // materialize an ItemInstance, which is wasteful for a pure equality check. Use the
            // recipe's stored result field through the shape-specific accessors.
            if (craftingRecipe instanceof me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe shaped) {
                return Objects.equals(shaped.getResult(), this.exactResult);
            } else if (craftingRecipe instanceof me.mykindos.betterpvp.core.recipe.crafting.ShapelessCraftingRecipe shapeless) {
                return Objects.equals(shapeless.getResult(), this.exactResult);
            }
            return Objects.equals(craftingRecipe.previewResult().getBaseItem(), this.exactResult);
        } else if (recipe instanceof AnvilRecipe anvilRecipe) {
            return Objects.equals(anvilRecipe.getPrimaryBaseItem(), this.exactResult);
        } else if (recipe instanceof StandardImbuementRecipe standardImbuement) {
            return Objects.equals(standardImbuement.getPrimaryBaseItem(), this.exactResult);
        } else if (recipe instanceof ImbuementRecipe imbuementRecipe) {
            return Objects.equals(imbuementRecipe.previewResult().getPrimaryBaseItem(), this.exactResult);
        } else if (recipe instanceof SmeltingRecipe smeltingRecipe) {
            return false; // Smelting recipes yield ALLOYS
        } else {
            throw new IllegalArgumentException("Unsupported recipe type: " + recipe.getClass().getName());
        }
    }

}
