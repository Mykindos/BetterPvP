package me.mykindos.betterpvp.core.recipe;

import me.mykindos.betterpvp.core.recipe.resolver.RecipeResolver;
import org.bukkit.NamespacedKey;

import java.util.Collection;

public interface RecipeRegistry<T extends Recipe<?, ?>> {

    void registerRecipe(NamespacedKey key, T recipe);

    Collection<T> getRecipes();

    RecipeResolver<T> getResolver();

}
