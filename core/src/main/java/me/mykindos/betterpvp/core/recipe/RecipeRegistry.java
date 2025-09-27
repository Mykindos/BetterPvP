package me.mykindos.betterpvp.core.recipe;

import me.mykindos.betterpvp.core.recipe.resolver.RecipeResolver;
import org.bukkit.NamespacedKey;

import java.util.Collection;

public interface RecipeRegistry<T extends Recipe<?, ?>> {

    void registerRecipe(NamespacedKey key, T recipe);

    default void clearRecipe(NamespacedKey key) {
        throw new UnsupportedOperationException("Cannot clear recipes from this registry");
    }

    Collection<T> getRecipes();

    RecipeResolver<T> getResolver();

}
