package me.mykindos.betterpvp.core.block.oraxen;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a block translated from Oraxen to BetterPVP.
 */
@FunctionalInterface
public interface OraxenBlock {

    /**
     * Returns the unique identifier of this block.
     * This must be the same as the Oraxen ID.
     *
     * @return the unique identifier
     */
    @NotNull String getId();

}
