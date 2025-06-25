package me.mykindos.betterpvp.core.block.data;

import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for block data that needs to handle chunk unloading.
 * Implementers can define custom behavior for when their chunk is unloaded.
 */
public interface UnloadHandler {
    
    /**
     * Called when the chunk containing this block data is being unloaded.
     * This allows custom cleanup, final calculations, or special saving logic
     * before the data is removed from cache.
     * 
     * Note: The data will be automatically saved after this method returns.
     * This is primarily for custom logic that should happen before unloading.
     * 
     * @param instance The block instance being unloaded
     */
    void onUnload(@NotNull SmartBlockInstance instance);
} 