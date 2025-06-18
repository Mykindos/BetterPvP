package me.mykindos.betterpvp.core.block.nexo;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an item translated from Nexo to BetterPVP.
 */
@FunctionalInterface
public interface NexoItem {

    /**
     * Returns the unique identifier of this item.
     * This must be the same as the Nexo ID.
     *
     * @return the unique identifier
     */
    @NotNull String getId();

    /**
     * Returns the display name of this item.
     *
     * @return the display name
     */
    default boolean isFurniture() {
        return false;
    }

}
