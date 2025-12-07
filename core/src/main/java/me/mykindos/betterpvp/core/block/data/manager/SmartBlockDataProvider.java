package me.mykindos.betterpvp.core.block.data.manager;

import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.data.DataHolder;
import me.mykindos.betterpvp.core.block.data.SmartBlockData;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * Provides access to SmartBlock data with caching and lazy loading capabilities.
 * Handles data creation, retrieval, and caching operations.
 */
public class SmartBlockDataProvider {

    private final SmartBlockDataCache cache;
    private final SmartBlockDataManager dataManager;

    public SmartBlockDataProvider(SmartBlockDataCache cache, SmartBlockDataManager dataManager) {
        this.cache = cache;
        this.dataManager = dataManager;
    }

    /**
     * Collects all cached SmartBlock data.
     * @return unmodifiable collection of all cached data
     */
    public Collection<SmartBlockData<?>> collectAll() {
        return Collections.unmodifiableCollection(cache.getCache().asMap().values());
    }

    /**
     * Gets or creates data for a SmartBlock instance.
     * @param instance the block instance
     * @return CompletableFuture containing the data, or null if the block doesn't support data
     * @param <T> the data type
     */
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<SmartBlockData<T>> getOrCreateData(@NotNull SmartBlockInstance instance) {
        if (!instance.supportsData()) return CompletableFuture.completedFuture(null);

        String cacheKey = cache.getCacheKey(instance);
        SmartBlockData<?> cached = cache.getCache().getIfPresent(cacheKey);
        if (cached != null) return CompletableFuture.completedFuture((SmartBlockData<T>) cached);

        return loadOrCreateData(instance);
    }

    /**
     * Loads or creates data for a SmartBlock instance.
     * @param instance the block instance
     * @return the loaded or created data
     * @param <T> the data type
     */
    @SuppressWarnings("unchecked")
    private <T> CompletableFuture<SmartBlockData<T>> loadOrCreateData(@NotNull SmartBlockInstance instance) {
        CompletableFuture<SmartBlockData<T>> future = dataManager.getStorage().load(instance);
        return future.thenApply(found -> {
            SmartBlockData<T> computed = found;
            if (computed == null) {
                T defaultData = ((DataHolder<T>) instance.getType()).createDefaultData();
                computed = new SmartBlockData<>(instance, (Class<T>) defaultData.getClass(), defaultData, dataManager);
                cache.save(computed);
            }

            cache.getCache().put(cache.getCacheKey(instance), computed);
            return computed;
        });
    }
}

