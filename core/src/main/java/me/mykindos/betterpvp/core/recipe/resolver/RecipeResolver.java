package me.mykindos.betterpvp.core.recipe.resolver;

import com.google.common.base.Preconditions;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.recipe.Recipe;
import me.mykindos.betterpvp.core.recipe.RecipeRegistry;
import org.jetbrains.annotations.Contract;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;

/**
 * Looks up recipes based on a given set of parameters.
 */
public class RecipeResolver<T extends Recipe<?, ?>> {

    private final RecipeRegistry<T> registry;

    /**
     * Creates a new recipe resolver.
     * @param registry the backing collection of recipes. Must not be a copy.
     */
    public RecipeResolver(final RecipeRegistry<T> registry) {
        this.registry = registry;
    }

    // Async because we're looping through a lot of items and this becomes
    // n*m
    // n = size of recipes
    // m = size of parameters
    @Contract("null -> fail")
    public final CompletableFuture<LinkedList<T>> lookup(LookupParameter... parameters) {
        Preconditions.checkNotNull(parameters, "Parameters must not be null");
        Preconditions.checkArgument(parameters.length > 0, "At least one parameter must be provided");
        return CompletableFuture.supplyAsync(() -> registry.getRecipes().stream()
                .filter(recipe -> {
                    for (LookupParameter parameter : parameters) {
                        if (!parameter.test(recipe)) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(LinkedList::new, LinkedList::add, LinkedList::addAll));
    }
}
