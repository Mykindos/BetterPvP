package me.mykindos.betterpvp.core.block.data.manager;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.block.SmartBlockFactory;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.data.LoadHandler;
import me.mykindos.betterpvp.core.block.data.SmartBlockData;
import me.mykindos.betterpvp.core.block.data.storage.SmartBlockDataStorage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Chunk;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages chunk-based operations for SmartBlock data.
 * Handles chunk loading, unloading, and related lifecycle events.
 */
@CustomLog
public class SmartBlockDataChunkManager {

    private final SmartBlockDataCache cache;
    private final SmartBlockDataStorage storage;
    private final SmartBlockFactory blockFactory;
    private final Core plugin;
    
    // Track chunks currently being loaded to prevent duplicate loading operations
    private final Set<Long> loadingChunks = ConcurrentHashMap.newKeySet();

    public SmartBlockDataChunkManager(SmartBlockDataCache cache, 
                                    SmartBlockDataStorage storage, 
                                    SmartBlockFactory blockFactory, 
                                    Core plugin) {
        this.cache = cache;
        this.storage = storage;
        this.blockFactory = blockFactory;
        this.plugin = plugin;
    }

    /**
     * Loads data for a chunk.
     * @param chunk the chunk to load
     */
    public void loadChunk(@NotNull Chunk chunk) {
        long chunkKey = chunk.getChunkKey();
        
        // Check if this chunk is already being loaded
        if (!loadingChunks.add(chunkKey)) {
            log.debug("Chunk {},{} is already being loaded, skipping duplicate request", 
                chunk.getX(), chunk.getZ()).submit();
            return;
        }
        
        if (storage.allowsAsynchronousLoading()) {
            loadChunkAsync(chunk);
        } else {
            loadChunkSync(chunk);
        }
    }

    /**
     * Unloads data for a chunk.
     * @param chunk the chunk to unload
     */
    public void unloadChunk(@NotNull Chunk chunk) {
        if (storage.allowsAsynchronousLoading()) {
            unloadChunkAsync(chunk);
        } else {
            unloadChunkSync(chunk);
        }
    }

    /**
     * Loads chunk data asynchronously.
     * @param chunk the chunk to load
     */
    private void loadChunkAsync(Chunk chunk) {
        long chunkKey = chunk.getChunkKey();
        
        storage.loadChunk(chunk).thenAcceptAsync(chunkData -> {
            try {
                processLoadedChunkData(chunkData);
            } finally {
                // Always remove from loading set when done
                loadingChunks.remove(chunkKey);
            }
        }).exceptionally(ex -> {
            try {
                log.error("Failed to load chunk data for chunk {},{}", chunk.getX(), chunk.getZ(), ex).submit();
                return null;
            } finally {
                // Always remove from loading set when done
                loadingChunks.remove(chunkKey);
            }
        });
    }

    /**
     * Loads chunk data synchronously.
     * @param chunk the chunk to load
     */
    private void loadChunkSync(Chunk chunk) {
        long chunkKey = chunk.getChunkKey();
        
        try {
            Map<Long, SmartBlockData<?>> chunkData = storage.loadChunk(chunk).join();
            processLoadedChunkData(chunkData);
        } catch (Exception e) {
            log.error("Failed to load chunk data for chunk {},{}", chunk.getX(), chunk.getZ(), e).submit();
        } finally {
            // Always remove from loading set when done
            loadingChunks.remove(chunkKey);
        }
    }

    /**
     * Unloads chunk data asynchronously.
     * @param chunk the chunk to unload
     */
    private void unloadChunkAsync(Chunk chunk) {
        try {
            unloadChunkInternal(chunk, true);
        } catch (Exception e) {
            log.error("Failed to unload chunk {},{}", chunk.getX(), chunk.getZ(), e).submit();
        }
    }

    /**
     * Unloads chunk data synchronously.
     * @param chunk the chunk to unload
     */
    private void unloadChunkSync(Chunk chunk) {
        try {
            unloadChunkInternal(chunk, false);
        } catch (Exception e) {
            log.error("Failed to unload chunk {},{}", chunk.getX(), chunk.getZ(), e).submit();
        }
    }

    /**
     * Internal chunk unloading logic.
     * @param chunk the chunk to unload
     * @param async whether to perform operations asynchronously
     */
    private void unloadChunkInternal(Chunk chunk, boolean async) {
        List<SmartBlockData<?>> chunkData = cache.findChunkData(chunk);
        for (SmartBlockData<?> data : chunkData) {
            try {
                handleUnload(data, async);
                cache.invalidate(data.getBlockInstance());
            } catch (Exception e) {
                log.error("Failed to unload smart block at {}", data.getBlockInstance().getHandle().getLocation(), e).submit();
            }
        }
    }

    /**
     * Processes loaded chunk data.
     * @param chunkData the chunk data to process
     */
    private void processLoadedChunkData(Map<Long, SmartBlockData<?>> chunkData) {
        cache.processLoadedChunkData(chunkData, this::verifySmartBlock);
        
        // Trigger load handlers for valid blocks
        for (SmartBlockData<?> data : chunkData.values()) {
            SmartBlockInstance instance = data.getBlockInstance();
            if (verifySmartBlock(instance) && data.get() instanceof LoadHandler loadHandler) {
                UtilServer.runTask(plugin, () -> loadHandler.onLoad(instance));
            }
        }
    }

    /**
     * Verifies that a smart block instance is still valid.
     * @param instance the instance to verify
     * @return true if valid, false otherwise
     */
    private boolean verifySmartBlock(SmartBlockInstance instance) {
        var block = instance.getHandle();
        final CompletableFuture<Optional<SmartBlockInstance>> future = new CompletableFuture<>();
        UtilServer.runTask(plugin, () -> future.complete(blockFactory.load(block)));
        Optional<SmartBlockInstance> currentInstance = future.join();
        return currentInstance.isPresent() && currentInstance.get().getType().equals(instance.getType());
    }

    /**
     * Handles unloading of a single block data.
     * @param data the data to unload
     * @param async whether to perform operations asynchronously
     */
    private void handleUnload(SmartBlockData<?> data, boolean async) {
        if (data.get() instanceof LoadHandler handler) {
            UtilServer.runTask(plugin, () -> handler.onUnload(data.getBlockInstance()));
        }
        UtilServer.runTask(plugin, async, () -> storage.save(data.getBlockInstance(), data));
    }
}

