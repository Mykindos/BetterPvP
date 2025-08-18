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
import me.mykindos.betterpvp.core.block.data.storage.SmartBlockDataStorage;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

    private final Cache<@NotNull String, SmartBlockData<?>> dataCache;
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
            .build();
    }

    public Collection<SmartBlockData<?>> collectAll() {
        return Collections.unmodifiableCollection(dataCache.asMap().values());
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
     * Saves all cached data to storage.
     */
    public void save() {
        dataCache.asMap().values().forEach(data -> {
            try {
                save((SmartBlockData<?>) data);
            } catch (Exception e) {
                log.error("Failed to save smart block data for {}", data.getBlockInstance().getHandle().getLocation(), e).submit();
            }
        });
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
            new BukkitRunnable() {
                @Override
                public void run() {
                    // Call the removal handler on the main thread
                    handler.onRemoval(instance, cause);
                }
            }.runTask(plugin);
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
        dataStorage.loadChunk(chunk).thenAccept(chunkData -> {
            // Switch back to main thread for cache operations and block validation
            processLoadedChunkData(chunkData);
        }).exceptionally(throwable -> {
            log.error("Failed to load chunk data for chunk {},{}", 
                     chunk.getX(), chunk.getZ(), throwable).submit();
            return null;
        });
    }
    
    private void loadChunkSync(Chunk chunk) {
        try {
            Map<Integer, SmartBlockData<?>> chunkData = dataStorage.loadChunk(chunk).join();
            processLoadedChunkData(chunkData);
        } catch (Exception e) {
            log.error("Failed to load chunk data for chunk {},{}", 
                     chunk.getX(), chunk.getZ(), e).submit();
        }
    }
    
    private void unloadChunkAsync(Chunk chunk) {
        try {
            unloadChunkInternal(chunk, true);
        } catch (Exception e) {
            log.error("Failed to unload chunk {},{}",
                    chunk.getX(), chunk.getZ(), e).submit();
        }
    }

    private void unloadChunkSync(Chunk chunk) {
        try {
            unloadChunkInternal(chunk, false);
        } catch (Exception e) {
            log.error("Failed to unload chunk {},{}",
                     chunk.getX(), chunk.getZ(), e).submit();
        }
    }
    
    private void unloadChunkInternal(Chunk chunk, boolean async) {
        List<SmartBlockData<?>> chunkData = findChunkData(chunk);

        for (SmartBlockData<?> data : chunkData) {
            try {
                // Call unload handler if implemented
                if (data.get() instanceof LoadHandler handler) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            handler.onUnload(data.getBlockInstance());
                        }
                    }.runTask(plugin);
                }
                
                // Save the data
                final BukkitRunnable saveFuture = new BukkitRunnable() {
                    @Override
                    public void run() {
                        save(data);
                    }
                };

                if (async) saveFuture.runTaskAsynchronously(plugin);
                else saveFuture.runTask(plugin);

                // Remove from cache
                String cacheKey = getCacheKey(data.getBlockInstance());
                dataCache.invalidate(cacheKey);
            } catch (Exception e) {
                log.error("Failed to unload smart block at {}", 
                         data.getBlockInstance().getHandle().getLocation(), e).submit();
            }
        }
    }
    
    private void processLoadedChunkData(Map<Integer, SmartBlockData<?>> chunkData) {
        for (Map.Entry<Integer, SmartBlockData<?>> entry : chunkData.entrySet()) {
            SmartBlockData<?> data = entry.getValue();
            SmartBlockInstance instance = data.getBlockInstance();
            
            // Verify the block still exists and is the correct type
            if (verifySmartBlock(instance)) {
                String cacheKey = getCacheKey(instance);
                dataCache.put(cacheKey, data);

                if (instance.getData() instanceof LoadHandler loadHandler) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            // Call load handler if implemented, already on main thread
                            loadHandler.onLoad(instance);
                        }
                    }.runTask(plugin);
                }
            } else {
                // Delete invalid smart block data
                removeData(instance, BlockRemovalCause.FORCED);
                log.info("Deleted invalid smart block data at {}",
                        instance.getHandle().getLocation()).submit();
            }
        }
    }
    
    private boolean verifySmartBlock(SmartBlockInstance instance) {
        // Check if the block at this location is still a smart block of the expected type
        Block block = instance.getHandle();
        Optional<SmartBlockInstance> currentInstance = blockFactory.load(block);

        // Not a smart block anymore
        if (currentInstance.isEmpty()) {
            return false;
        }

        return currentInstance.get().getType().equals(instance.getType());
    }
    
    private List<SmartBlockData<?>> findChunkData(Chunk chunk) {
        final World world = chunk.getWorld();
        final long chunkKey = chunk.getChunkKey();
        List<SmartBlockData<?>> dataList = new ArrayList<>();
        for (SmartBlockData<?> data : dataCache.asMap().values()) {
            final SmartBlockInstance instance = data.getBlockInstance();
            final Location location = instance.getLocation();
            if (world.equals(location.getWorld()) && Chunk.getChunkKey(location) == chunkKey) {
                dataList.add(data);
            }
        }
        return dataList;
    }
    
    /**
     * Handles cache removal events.
     */
    private void onCacheRemoval(String key, SmartBlockData<?> data, RemovalCause cause) {
        if (cause.wasEvicted()) {
            // Save to storage before eviction
            save(data);
        }
    }
}