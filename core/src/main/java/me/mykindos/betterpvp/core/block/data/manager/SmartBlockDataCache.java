package me.mykindos.betterpvp.core.block.data.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.data.SmartBlockData;
import me.mykindos.betterpvp.core.block.data.storage.SmartBlockDataStorage;
import org.bukkit.Chunk;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Manages the caching layer for SmartBlock data.
 * Handles cache operations, eviction policies, and cache-related utilities.
 */
@CustomLog
public class SmartBlockDataCache {

    private final Cache<@NotNull String, SmartBlockData<?>> dataCache;
    private final SmartBlockDataStorage dataStorage;

    public SmartBlockDataCache(SmartBlockDataStorage dataStorage) {
        this.dataStorage = dataStorage;
        this.dataCache = Caffeine.newBuilder()
                .maximumSize(10000)
                .removalListener((String key, SmartBlockData<?> data, RemovalCause cause) -> {
                    if (cause.wasEvicted()) {
                        save(data);
                    }
                })
                .build();
    }

    /**
     * Gets the underlying cache instance.
     * @return the cache
     */
    public Cache<@NotNull String, SmartBlockData<?>> getCache() {
        return dataCache;
    }

    /**
     * Saves data to storage when evicted from cache.
     * @param data the data to save
     */
    void save(@NotNull SmartBlockData<?> data) {
        try {
            dataStorage.save(data.getBlockInstance(), data);
        } catch (Exception e) {
            log.error("Failed to save smart block data for {}", data.getBlockInstance().getHandle().getLocation(), e).submit();
        }
    }

    /**
     * Invalidates cache entry for a specific block instance.
     * @param instance the block instance
     */
    public void invalidate(@NotNull SmartBlockInstance instance) {
        String cacheKey = getCacheKey(instance);
        dataCache.invalidate(cacheKey);
    }

    /**
     * Gets cache key for a block instance.
     * @param instance the block instance
     * @return the cache key
     */
    protected @NotNull String getCacheKey(@NotNull SmartBlockInstance instance) {
        var handle = instance.getHandle();
        return handle.getWorld().getName() + "_" + handle.getX() + "_" + handle.getY() + "_" + handle.getZ();
    }

    /**
     * Finds all cached data for a specific chunk.
     * @param chunk the chunk
     * @return list of data in the chunk
     */
    public List<SmartBlockData<?>> findChunkData(@NotNull Chunk chunk) {
        var world = chunk.getWorld();
        long chunkKey = chunk.getChunkKey();
        return dataCache.asMap().values().stream()
                .filter(data -> {
                    var loc = data.getBlockInstance().getLocation();
                    return world.equals(loc.getWorld()) && loc.getChunk().getChunkKey() == chunkKey;
                })
                .toList();
    }

    /**
     * Processes loaded chunk data and adds it to the cache.
     * @param chunkData the chunk data to process
     * @param validator function to validate block instances
     */
    public void processLoadedChunkData(Map<Integer, SmartBlockData<?>> chunkData, 
                                     java.util.function.Function<SmartBlockInstance, Boolean> validator) {
        for (Map.Entry<Integer, SmartBlockData<?>> entry : chunkData.entrySet()) {
            SmartBlockData<?> data = entry.getValue();
            SmartBlockInstance instance = data.getBlockInstance();
            if (validator.apply(instance)) {
                String cacheKey = getCacheKey(instance);
                dataCache.put(cacheKey, data);
            } else {
                // Invalid data - remove from storage
                dataStorage.remove(instance);
                log.info("Deleted invalid smart block data at {}", instance.getHandle().getLocation()).submit();
            }
        }
    }
}

