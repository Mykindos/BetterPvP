package me.mykindos.betterpvp.core.block.data;

/**
 * Represents the cause of block removal.
 * Used to determine how block data should handle removal.
 */
public enum BlockRemovalCause {
    
    /**
     * Block was broken naturally by a player or environmental cause.
     * Data should drop items and clean up naturally.
     */
    NATURAL,
    
    /**
     * Block was removed by force (commands, admin actions, etc.).
     * Data should be cleaned up but may not drop items.
     */
    FORCED,
} 