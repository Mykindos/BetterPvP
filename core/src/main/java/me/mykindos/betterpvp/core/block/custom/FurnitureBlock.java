package me.mykindos.betterpvp.core.block.custom;

import org.jetbrains.annotations.NotNull;

/**
 * A smart block drawn as furniture: an item display with barrier blocks for collision.
 */
public interface FurnitureBlock {

    /**
     * The id the block's item places it by.
     */
    @NotNull String getBlockId();

    @NotNull FurnitureShape getShape();

}
