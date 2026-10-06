package me.mykindos.betterpvp.core.block.custom;

import org.jetbrains.annotations.NotNull;

/**
 * An item that places a furniture or note block smart block when used on a block, and that the block drops.
 */
public interface PlacesBlock {

    /**
     * The block id of the {@link FurnitureBlock} or {@link CustomNoteBlock} this item places.
     */
    @NotNull String getBlockId();

}
