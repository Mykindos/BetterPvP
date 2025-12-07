package me.mykindos.betterpvp.core.block.nexo;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a block translated from Nexo to BetterPVP.
 */
@FunctionalInterface
public interface NexoBlock {

    /**
     * Returns the unique identifier of this block.
     * This must be the same as the Nexo ID.
     *
     * @return the unique identifier
     */
    @NotNull String getId();

}
