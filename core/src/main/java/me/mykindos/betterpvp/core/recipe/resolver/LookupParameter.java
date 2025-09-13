package me.mykindos.betterpvp.core.recipe.resolver;

import me.mykindos.betterpvp.core.recipe.Recipe;

import java.util.function.Predicate;

/**
 * Marker class for lookup parameters used in {@link RecipeResolver}
 */
public interface LookupParameter extends Predicate<Recipe<?, ?>> {

    static LookupParameter of(Predicate<Recipe<?, ?>> predicate) {
        return predicate::test;
    }

    /**
     * Checks if the given recipe matches the lookup parameter.
     *
     * @param recipe the recipe to check
     * @return true if the recipe matches, false otherwise
     */
    @Override
    boolean test(Recipe<?, ?> recipe);

}
