package me.mykindos.betterpvp.core.block.data;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.data.storage.SmartBlockDataStorage;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Manages SmartBlock data instances, including caching and persistence to storage.
 */
@Singleton
@CustomLog
public class SmartBlockDataManager {

    private final Cache<String, SmartBlockData<?>> dataCache;
    private final SmartBlockDataStorage dataStorage;
    
    @Inject
    private SmartBlockDataManager(SmartBlockDataStorage dataStorage) {
        this.dataStorage = dataStorage;
        this.dataCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .removalListener(this::onCacheRemoval)
            .build();
    }
    
    /**
     * Gets or creates block data for the given instance.
     * The data type is inferred from the SmartBlock's defined data type.
     * 
     * @param instance The block instance
     * @return Optional containing the block data if the block supports data
     */
    @SuppressWarnings("unchecked")
    public <T> SmartBlockData<T> getOrCreateData(@NotNull SmartBlockInstance instance) {
        if (!instance.supportsData()) {
            return null; // Block does not support data
        }

        String cacheKey = getCacheKey(instance);
        SmartBlockData<?> cached = dataCache.getIfPresent(cacheKey);
        
        if (cached != null) {
            return (SmartBlockData<T>) cached;
        }
        
        // Load from storage or create default
        return loadOrCreateData(instance);
    }

    /**
     * Internal method to load or create data with a specific type.
     */
    @SuppressWarnings("unchecked")
    private <T> SmartBlockData<T> loadOrCreateData(@NotNull SmartBlockInstance instance) {
        final SmartBlockData<T> computed = dataStorage.<T>load(instance).orElseGet(() -> {
            final T defaultData = ((DataHolder<T>) instance.getType()).createDefaultData();
            SmartBlockData<T> data = new SmartBlockData<>(instance, (Class<T>) defaultData.getClass(), defaultData, this);
            save(data);
            return data;
        });
        dataCache.put(getCacheKey(instance), computed);
        return computed;
    }
    
        /**
     * Saves block data to the storage.
     */
    public <T> void save(@NotNull SmartBlockData<T> blockData) {
        dataStorage.save(blockData.getBlockInstance(), blockData);
    }
    
    /**
     * Removes data from cache and storage for a block instance.
     */
    public void removeData(@NotNull SmartBlockInstance instance) {
        dataStorage.remove(instance);
        String cacheKey = getCacheKey(instance);
        dataCache.invalidate(cacheKey);
    }
    
    /**
     * Generates a cache key for a block instance.
     */
    private String getCacheKey(@NotNull SmartBlockInstance instance) {
        final Block handle = instance.getHandle();
        return String.format("%s_%d_%d_%d",
                handle.getWorld().getName(),
                handle.getX(),
                handle.getY(),
                handle.getZ());
    }
    
    /**
     * Handles cache removal events.
     */
    private void onCacheRemoval(String key, SmartBlockData<?> data, RemovalCause cause) {
        if (cause.wasEvicted()) {
            // Save to storage before eviction
            log.info("Cache evicted data for {}, saving to storage", key).submit();
            save(data);
        }
    }
} 