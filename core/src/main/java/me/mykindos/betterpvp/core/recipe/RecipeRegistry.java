package me.mykindos.betterpvp.core.recipe;

import me.mykindos.betterpvp.core.recipe.resolver.RecipeResolver;

import java.util.Collection;

public interface RecipeRegistry<T extends Recipe<?, ?>> {

    void registerRecipe(T recipe);

    Collection<T> getRecipes();

    RecipeResolver<T> getResolver();

}
