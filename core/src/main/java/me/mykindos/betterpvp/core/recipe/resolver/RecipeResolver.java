package me.mykindos.betterpvp.core.recipe.resolver;

import com.google.common.base.Preconditions;
import me.mykindos.betterpvp.core.recipe.Recipe;
import me.mykindos.betterpvp.core.recipe.RecipeRegistry;
import org.jetbrains.annotations.Contract;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Looks up recipes based on a given set of parameters.
 * <p>
 * Two entry points are exposed:
 * <ul>
 *   <li>{@link #lookup(LookupParameter...)} — memoized. Identical queries (by {@code equals}/
 *       {@code hashCode} of their {@link LookupParameter}s) reuse the same in-flight or completed
 *       future, so spamming the same lookup does not re-scan the registry. The cache must be
 *       dropped via {@link #invalidate()} whenever the backing registry mutates.</li>
 *   <li>{@link #coldLookup(LookupParameter...)} — uncached. Always scans the registry on a fresh
 *       async task. Use this for one-shot or non-equatable parameters (e.g. inline lambdas) where
 *       caching would either leak or never hit.</li>
 * </ul>
 */
public class RecipeResolver<T extends Recipe<?>> {

    private final RecipeRegistry<T> registry;
    private final ConcurrentHashMap<List<LookupParameter>, CompletableFuture<List<T>>> cache = new ConcurrentHashMap<>();
    private static final ExecutorService executor = Executors.newFixedThreadPool(2);
    /**
     * Creates a new recipe resolver.
     * @param registry the backing collection of recipes. Must not be a copy.
     */
    public RecipeResolver(final RecipeRegistry<T> registry) {
        this.registry = registry;
    }

    /**
     * Memoized lookup. Returns a cached future for repeated identical parameter combinations.
     * <p>
     * Async because we're looping through a lot of items and this becomes n*m
     * (n = size of recipes, m = size of parameters). After the first call for a given key, the
     * result is reused on the calling thread with no executor handoff.
     */
    @Contract("null -> fail")
    public final CompletableFuture<LinkedList<T>> lookup(LookupParameter... parameters) {
        Preconditions.checkNotNull(parameters, "Parameters must not be null");
        Preconditions.checkArgument(parameters.length > 0, "At least one parameter must be provided");

        final List<LookupParameter> key = List.of(parameters);
        final CompletableFuture<List<T>> shared = cache.computeIfAbsent(key, k ->
                CompletableFuture.supplyAsync(() -> filter(k), executor));

        // Defensive copy: callers receive a mutable LinkedList they may drain or modify.
        return shared.thenApply(LinkedList::new);
    }

    /**
     * Uncached lookup. Always schedules a fresh registry scan. Prefer this when parameters cannot
     * be meaningfully cached (e.g. inline lambdas with reference-only equality) or when staleness
     * tolerance is lower than the resolver's invalidation hooks can guarantee.
     */
    @Contract("null -> fail")
    public final CompletableFuture<LinkedList<T>> coldLookup(LookupParameter... parameters) {
        Preconditions.checkNotNull(parameters, "Parameters must not be null");
        Preconditions.checkArgument(parameters.length > 0, "At least one parameter must be provided");

        final List<LookupParameter> key = List.of(parameters);
        return CompletableFuture.supplyAsync(() -> filter(key), executor)
                .thenApply(LinkedList::new);
    }

    private List<T> filter(List<LookupParameter> parameters) {
        return registry.getRecipes().stream()
                .filter(recipe -> {
                    for (LookupParameter parameter : parameters) {
                        if (!parameter.test(recipe)) {
                            return false;
                        }
                    }
                    return true;
                })
                .toList();
    }

    /**
     * Drops all memoized {@link #lookup} results. Must be called whenever the backing registry's
     * recipe set changes (recipe registered, cleared, or replaced).
     */
    public void invalidate() {
        cache.clear();
    }
}
