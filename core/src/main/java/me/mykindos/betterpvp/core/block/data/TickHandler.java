package me.mykindos.betterpvp.core.block.data;

import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for block data that can handle its own tick updates.
 * Implementers can define custom behavior for each tick.
 */
public interface TickHandler {

    /**
     * Handles the tick update for this block data.
     * Called every tick for the block containing this data.
     *
     * @param instance The instance of the block being ticked
     */
    void onTick(@NotNull SmartBlockInstance instance);
} 