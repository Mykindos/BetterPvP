package me.mykindos.betterpvp.core.block.data;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.block.SmartBlockFactory;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.data.UnloadHandler;
import me.mykindos.betterpvp.core.block.data.storage.SmartBlockDataStorage;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Manages SmartBlock data instances, including caching and persistence to storage.
 */
@Singleton
@CustomLog
public class SmartBlockDataManager {

    private final Cache<String, SmartBlockData<?>> dataCache;
    private final SmartBlockDataStorage dataStorage;
    private final SmartBlockFactory blockFactory;
    private final Core plugin;
    
    @Inject
    private SmartBlockDataManager(SmartBlockDataStorage dataStorage, SmartBlockFactory blockFactory, Core plugin) {
        this.dataStorage = dataStorage;
        this.blockFactory = blockFactory;
        this.plugin = plugin;
        this.dataCache = Caffeine.newBuilder()
            .maximumSize(10000) // Increased for chunk-based loading
            .removalListener(this::onCacheRemoval)
            // Remove expireAfterAccess - let chunks control lifecycle
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
     * Removes data from cache and storage for a block instance with a specific cause.
     * If the data implements RemovalHandler, its onRemoval method will be called first.
     * 
     * @param instance The block instance
     * @param cause The cause of the removal
     */
    public void removeData(@NotNull SmartBlockInstance instance, @NotNull BlockRemovalCause cause) {
        String cacheKey = getCacheKey(instance);
        SmartBlockData<?> data = dataCache.getIfPresent(cacheKey);
        
        // If we have cached data, call the removal handler if it implements the interface
        if (data != null && data.get() instanceof RemovalHandler handler) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Call the removal handler on the main thread
                handler.onRemoval(instance, cause);
            });
        }
        
        // Remove from storage and cache
        dataStorage.remove(instance);
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
     * Loads all smart block data for a chunk and caches it.
     * Handles async/sync operations based on storage type.
     */
    public void loadChunk(@NotNull Chunk chunk) {
        if (dataStorage.allowsAsynchronousLoading()) {
            loadChunkAsync(chunk);
        } else {
            loadChunkSync(chunk);
        }
    }
    
    /**
     * Unloads all smart block data for a chunk, calling unload handlers and saving.
     * Handles async/sync operations based on storage type.
     */
    public void unloadChunk(@NotNull Chunk chunk) {
        if (dataStorage.allowsAsynchronousLoading()) {
            unloadChunkAsync(chunk);
        } else {
            unloadChunkSync(chunk);
        }
    }
    
    private void loadChunkAsync(Chunk chunk) {
        CompletableFuture.supplyAsync(() -> {
            return dataStorage.loadChunk(chunk);
        }).thenAcceptAsync(chunkData -> {
            // Switch back to main thread for cache operations and block validation
            Bukkit.getScheduler().runTask(plugin, () -> {
                processLoadedChunkData(chunk, chunkData);
            });
        }).exceptionally(throwable -> {
            log.error("Failed to load chunk data for chunk {},{}", 
                     chunk.getX(), chunk.getZ(), throwable).submit();
            return null;
        });
    }
    
    private void loadChunkSync(Chunk chunk) {
        try {
            Map<Integer, SmartBlockData<?>> chunkData = dataStorage.loadChunk(chunk);
            processLoadedChunkData(chunk, chunkData);
        } catch (Exception e) {
            log.error("Failed to load chunk data for chunk {},{}", 
                     chunk.getX(), chunk.getZ(), e).submit();
        }
    }
    
    private void unloadChunkAsync(Chunk chunk) {
        CompletableFuture.runAsync(() -> {
            unloadChunkInternal(chunk);
        }).exceptionally(throwable -> {
            log.error("Failed to unload chunk {},{}",
                     chunk.getX(), chunk.getZ(), throwable).submit();
            return null;
        });
    }

    private void unloadChunkSync(Chunk chunk) {
        try {
            unloadChunkInternal(chunk);
        } catch (Exception e) {
            log.error("Failed to unload chunk {},{}",
                     chunk.getX(), chunk.getZ(), e).submit();
        }
    }
    
    private void unloadChunkInternal(Chunk chunk) {
        List<SmartBlockData<?>> chunkData = findChunkData(chunk);
        
        int unloadedCount = 0;
        for (SmartBlockData<?> data : chunkData) {
            try {
                // Call unload handler if implemented
                if (data.get() instanceof UnloadHandler handler) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        handler.onUnload(data.getBlockInstance());
                    });
                }
                
                // Save the data
                save(data);
                
                // Remove from cache
                String cacheKey = getCacheKey(data.getBlockInstance());
                dataCache.invalidate(cacheKey);
                unloadedCount++;
                
            } catch (Exception e) {
                log.error("Failed to unload smart block at {}", 
                         data.getBlockInstance().getHandle().getLocation(), e).submit();
            }
        }
        
        log.info("Unloaded chunk {},{}: {} blocks saved and removed from cache",
                chunk.getX(), chunk.getZ(), unloadedCount).submit();
    }
    
    private void processLoadedChunkData(Chunk chunk, Map<Integer, SmartBlockData<?>> chunkData) {
        int loadedCount = 0;
        int deletedCount = 0;
        
        for (Map.Entry<Integer, SmartBlockData<?>> entry : chunkData.entrySet()) {
            SmartBlockData<?> data = entry.getValue();
            SmartBlockInstance instance = data.getBlockInstance();
            
            // Verify the block still exists and is the correct type
            if (verifySmartBlock(instance)) {
                String cacheKey = getCacheKey(instance);
                dataCache.put(cacheKey, data);
                loadedCount++;
            } else {
                // Delete invalid smart block data
                removeData(instance, BlockRemovalCause.FORCED);
                log.info("Deleted invalid smart block data at {}",
                        instance.getHandle().getLocation()).submit();
                deletedCount++;
            }
        }
        
        log.info("Loaded chunk {},{}: {} blocks cached, {} invalid blocks deleted",
                chunk.getX(), chunk.getZ(), loadedCount, deletedCount).submit();
    }
    
    private boolean verifySmartBlock(SmartBlockInstance instance) {
        // Check if the block at this location is still a smart block of the expected type
        Block block = instance.getHandle();
        Optional<SmartBlockInstance> currentInstance = blockFactory.from(block);

        // Not a smart block anymore
        return currentInstance.map(blockInstance -> blockInstance.getType().equals(instance.getType())).orElse(false);
    }
    
    private List<SmartBlockData<?>> findChunkData(Chunk chunk) {
        return dataCache.asMap().values().stream()
            .filter(data -> {
                Block block = data.getBlockInstance().getHandle();
                return block.getWorld().equals(chunk.getWorld()) && 
                       block.getChunk().getChunkKey() == chunk.getChunkKey();
            })
            .toList();
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