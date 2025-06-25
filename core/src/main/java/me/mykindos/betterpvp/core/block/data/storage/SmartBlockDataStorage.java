package me.mykindos.betterpvp.core.block.data.storage;

import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.data.SmartBlockData;
import org.bukkit.Chunk;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

/**
 * Storage abstraction for SmartBlockData that allows swapping between
 * PDC and database storage implementations.
 * 
 * @param <T> the type of data being stored
 */
public interface SmartBlockDataStorage {
    
    /**
     * Saves data for a specific block instance.
     * 
     * @param instance the block instance
     * @param data the data to save
     */
    <T> void save(@NotNull SmartBlockInstance instance, @NotNull SmartBlockData<T> data);
    
    /**
     * Loads data for a specific block instance.
     *
     * @param instance the block instance
     * @return the loaded data, or empty if not found
     */
    <T> Optional<SmartBlockData<T>> load(@NotNull SmartBlockInstance instance);
    
    /**
     * Removes data for a specific block instance.
     * 
     * @param instance the block instance
     */
    void remove(@NotNull SmartBlockInstance instance);
    
    /**
     * Loads all data for a chunk at once.
     * Used for chunk-based loading and caching.
     * 
     * @param chunk the chunk to load data for
     * @return map of block keys to their data
     */
    @NotNull Map<Integer, SmartBlockData<?>> loadChunk(@NotNull Chunk chunk);
    
    /**
     * Removes all data for a chunk.
     * Used when chunks are unloaded.
     * 
     * @param chunk the chunk to remove data for
     */
    void removeChunk(@NotNull Chunk chunk);
} 