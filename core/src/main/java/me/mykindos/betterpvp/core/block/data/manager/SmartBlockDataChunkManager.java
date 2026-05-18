package me.mykindos.betterpvp.core.block.data.manager;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.data.LoadHandler;
import me.mykindos.betterpvp.core.block.data.SmartBlockData;
import me.mykindos.betterpvp.core.block.data.storage.SmartBlockDataStorage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Chunk;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages chunk-based operations for SmartBlock data.
 * Handles chunk loading, unloading, and related lifecycle events.
 */
@CustomLog
public class SmartBlockDataChunkManager {

    private final SmartBlockDataCache cache;
    private final SmartBlockDataStorage storage;
    private final Core plugin;

    // Track chunks currently being loaded to prevent duplicate loading operations
    private final Set<Long> loadingChunks = ConcurrentHashMap.newKeySet();

    public SmartBlockDataChunkManager(SmartBlockDataCache cache,
                                    SmartBlockDataStorage storage,
                                    Core plugin) {
        this.cache = cache;
        this.storage = storage;
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
        final long chunkKey = chunk.getChunkKey();

        // Process on the main thread once the data future settles. Post-load processing needs
        // the main thread anyway (block/entity access, display spawning), and doing it here
        // deterministically — rather than on a pooled thread that then blocks on the main
        // thread — both removes the runTask+join anti-pattern and guarantees onLoad runs.
        storage.loadChunk(chunk).whenComplete((chunkData, throwable) ->
                UtilServer.runTask(plugin, () -> {
                    try {
                        if (throwable != null) {
                            log.error("Failed to load chunk data for chunk {},{}",
                                    chunk.getX(), chunk.getZ(), throwable).submit();
                        } else {
                            processLoadedChunkData(chunkData);
                        }
                    } finally {
                        loadingChunks.remove(chunkKey);
                    }
                }));
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
        // Runs on the main thread (see loadChunkAsync). Every instance here was already
        // resolved against the live world while loading (furniture entity present + stored
        // type matched + DataHolder) — see DatabaseSmartBlockDataStorage#resolveRow. A second
        // resolution here would only reintroduce the entity-load timing / resolver-identity
        // race, so we trust the resolved instance: cache it and fire its load handler.
        for (SmartBlockData<?> data : chunkData.values()) {
            final SmartBlockInstance instance = data.getBlockInstance();
            cache.getCache().put(cache.getCacheKey(instance), data);
            if (data.get() instanceof LoadHandler loadHandler) {
                loadHandler.onLoad(instance);
            }
        }
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

