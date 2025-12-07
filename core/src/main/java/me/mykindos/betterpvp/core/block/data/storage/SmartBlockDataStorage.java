package me.mykindos.betterpvp.core.block.data.storage;

import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.data.SmartBlockData;
import org.bukkit.Chunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Storage abstraction for SmartBlockData that allows swapping between
 * PDC and database storage implementations.
 * All operations are now consistently asynchronous for better performance and consistency.
 */
public interface SmartBlockDataStorage {
    
    /**
     * Saves data for a specific block instance.
     * 
     * @param instance the block instance
     * @param data the data to save
     * @return a CompletableFuture that completes when the save operation is finished
     */
    <T> CompletableFuture<Void> save(@NotNull SmartBlockInstance instance, @NotNull SmartBlockData<T> data);
    
    /**
     * Loads data for a specific block instance.
     *
     * @param instance the block instance
     * @return a CompletableFuture that completes with the loaded data, or null if not found
     */
    @Nullable
    <T> CompletableFuture<SmartBlockData<T>> load(@NotNull SmartBlockInstance instance);
    
    /**
     * Removes data for a specific block instance.
     * 
     * @param instance the block instance
     * @return a CompletableFuture that completes when the removal is finished
     */
    CompletableFuture<Void> remove(@NotNull SmartBlockInstance instance);
    
    /**
     * Loads all data for a chunk at once.
     * Used for chunk-based loading and caching.
     *
     * @param chunk the chunk to load data for
     * @return map of block keys to their data (empty map if no data found)
     */
    @NotNull CompletableFuture<Map<Long, SmartBlockData<?>>> loadChunk(@NotNull Chunk chunk);
    
    /**
     * Removes all data for a chunk.
     * Used when chunks are unloaded.
     * 
     * @param chunk the chunk to remove data for
     * @return a CompletableFuture that completes when the removal is finished
     */
    CompletableFuture<Void> removeChunk(@NotNull Chunk chunk);

    /**
     * Checks if this storage implementation allows asynchronous loading.
     * @return true if asynchronous loading is allowed, false otherwise
     */
    default boolean allowsAsynchronousLoading() {
        return false; // Default implementation does not allow async loading
    }
    
    /**
     * Saves data synchronously. Use sparingly and avoid on main thread.
     * @param instance the block instance
     * @param data the data to save
     */
    default <T> void saveSync(@NotNull SmartBlockInstance instance, @NotNull SmartBlockData<T> data) {
        save(instance, data).join();
    }
    
    /**
     * Removes data synchronously. Use sparingly and avoid on main thread.
     * @param instance the block instance
     */
    default void removeSync(@NotNull SmartBlockInstance instance) {
        remove(instance).join();
    }
    
    /**
     * Removes chunk data synchronously. Use sparingly and avoid on main thread.
     * @param chunk the chunk to remove data for
     */
    default void removeChunkSync(@NotNull Chunk chunk) {
        removeChunk(chunk).join();
    }
} 