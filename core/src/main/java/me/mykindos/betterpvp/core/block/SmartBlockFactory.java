package me.mykindos.betterpvp.core.block;

import org.bukkit.block.Block;

import java.util.Optional;

/**
 * Factory class for creating {@link SmartBlockInstance}s
 */
public interface SmartBlockFactory {

    /**
     * Creates a new {@link SmartBlockInstance} from the given block.
     * @param block the block to create the instance from
     * @return a new {@link SmartBlockInstance} if the block is a smart block
     */
    Optional<SmartBlockInstance> from(Block block);

    /**
     * Loads a {@link SmartBlockInstance} from the given block for the first time.
     * This is only called when a chunk loads.
     * @param block the block to load the instance for
     * @return a new {@link SmartBlockInstance} if the block is a smart block, empty otherwise
     */
    Optional<SmartBlockInstance> load(Block block);

    /**
     * Checks if the given block is a smart block.
     * @param block  the block to check
     * @return true if the block is a smart block, false otherwise
     */
    boolean isSmartBlock(Block block);

}
