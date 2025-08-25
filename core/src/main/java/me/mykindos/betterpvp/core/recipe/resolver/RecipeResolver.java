package me.mykindos.betterpvp.core.recipe.resolver;

import com.google.common.base.Preconditions;
import me.mykindos.betterpvp.core.recipe.Recipe;
import org.jetbrains.annotations.Contract;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Looks up recipes based on a given set of parameters.
 *
 * Used in quick crafting
 */
public class RecipeResolver<T extends Recipe<?, ?>> {

    private final Collection<T> registry;

    /**
     * Creates a new recipe resolver.
     * @param registry the backing collection of recipes. Must not be a copy.
     */
    public RecipeResolver(final Collection<T> registry) {
        this.registry = registry;
    }

    @SafeVarargs
    @Contract("null -> fail")
    public final LinkedList<T> lookup(LookupParameter<T>... parameters) {
        Preconditions.checkNotNull(parameters, "Parameters must not be null");
        Preconditions.checkArgument(parameters.length > 0, "At least one parameter must be provided");
        return registry.stream()
                .filter(recipe -> {
                    for (LookupParameter<T> parameter : parameters) {
                        if (!parameter.test(recipe)) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(LinkedList::new, LinkedList::add, LinkedList::addAll);
    }
}
