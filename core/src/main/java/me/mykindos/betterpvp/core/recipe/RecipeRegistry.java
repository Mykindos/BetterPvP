package me.mykindos.betterpvp.core.recipe;

import me.mykindos.betterpvp.core.recipe.resolver.RecipeResolver;
import org.bukkit.NamespacedKey;

import java.util.Collection;
import java.util.Optional;

public interface RecipeRegistry<T extends Recipe<?, ?>> {

    void registerRecipe(NamespacedKey key, T recipe);

    Optional<T> getRecipe(NamespacedKey key);

    default void clearRecipe(NamespacedKey key) {
        throw new UnsupportedOperationException("Cannot clear recipes from this registry");
    }

    Collection<T> getRecipes();

    RecipeResolver<T> getResolver();

}
