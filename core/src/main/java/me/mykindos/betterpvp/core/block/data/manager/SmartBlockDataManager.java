package me.mykindos.betterpvp.core.block.data.manager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.block.SmartBlockFactory;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.data.*;
import me.mykindos.betterpvp.core.block.data.storage.SmartBlockDataStorage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Main manager for SmartBlock data operations.
 * Coordinates between cache, storage, and chunk management components.
 */
@Singleton
@CustomLog
public class SmartBlockDataManager {

    private final SmartBlockDataCache cache;
    private final SmartBlockDataStorage dataStorage;
    private final SmartBlockDataChunkManager chunkManager;
    private final Core plugin;
    @Getter
    private final SmartBlockDataProvider provider;

    @Inject
    private SmartBlockDataManager(SmartBlockDataStorage dataStorage, SmartBlockFactory blockFactory, Core plugin) {
        this.dataStorage = dataStorage;
        this.plugin = plugin;
        this.cache = new SmartBlockDataCache(dataStorage);
        this.chunkManager = new SmartBlockDataChunkManager(cache, dataStorage, blockFactory, plugin);
        this.provider = new SmartBlockDataProvider(cache, this);
    }

    /**
     * Gets the cache instance for internal use.
     * @return the cache
     */
    public SmartBlockDataCache getCache() {
        return cache;
    }

    /**
     * Gets the storage instance for internal use.
     * @return the storage
     */
    public SmartBlockDataStorage getStorage() {
        return dataStorage;
    }

    /**
     * Saves a specific block data.
     * @param blockData the data to save
     * @param <T> the data type
     */
    public <T> CompletableFuture<Void> saveWorld(@NotNull SmartBlockData<T> blockData) {
        return dataStorage.save(blockData.getBlockInstance(), blockData);
    }

    public CompletableFuture<Void> saveWorlds() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            futures.add(saveWorld(world).exceptionally(ex -> {
                log.error("Failed to save smart block data for world {}", world.getName(), ex).submit();
                return null;
            }));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * Saves all cached data for a specific world.
     */
    public CompletableFuture<Void> saveWorld(World world) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (var data : cache.getCache().asMap().values()) {
            if (!data.getBlockInstance().getLocation().getWorld().equals(world)) {
                continue; // Skip data for other worlds
            }
            try {
                futures.add(saveWorld(data).exceptionally(ex -> {
                    log.error("Failed to save smart block data for {}", data.getBlockInstance().getHandle().getLocation(), ex).submit();
                    return null;
                }));
            } catch (Exception e) {
                log.error("Failed to save smart block data for {}", data.getBlockInstance().getHandle().getLocation(), e).submit();
            }
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * Removes data for a block instance.
     * @param instance the block instance
     * @param cause the removal cause
     */
    public void removeData(@NotNull SmartBlockInstance instance, @NotNull BlockRemovalCause cause) {
        String cacheKey = cache.getCacheKey(instance);
        var data = cache.getCache().getIfPresent(cacheKey);

        if (data != null && data.get() instanceof RemovalHandler handler) {
            UtilServer.runTask(plugin, () -> handler.onRemoval(instance, cause));
        }

        dataStorage.removeSync(instance);
        cache.invalidate(instance);
    }

    /**
     * Loads data for a chunk.
     * @param chunk the chunk to load
     */
    public void loadChunk(@NotNull Chunk chunk) {
        chunkManager.loadChunk(chunk);
    }

    /**
     * Unloads data for a chunk.
     * @param chunk the chunk to unload
     */
    public void unloadChunk(@NotNull Chunk chunk) {
        chunkManager.unloadChunk(chunk);
    }

}
