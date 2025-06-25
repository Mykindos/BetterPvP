package me.mykindos.betterpvp.core.block.data;

import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for block data that can handle its own removal.
 * Implementers can define custom behavior for different removal causes.
 */
public interface RemovalHandler {
    
    /**
     * Handles the removal of this block data.
     * Called when the block containing this data is being removed.
     *
     * @param instance The location where the block was removed
     * @param cause    The cause of the removal
     */
    void onRemoval(@NotNull SmartBlockInstance instance, @NotNull BlockRemovalCause cause);
} 