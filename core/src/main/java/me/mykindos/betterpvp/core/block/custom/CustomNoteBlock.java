package me.mykindos.betterpvp.core.block.custom;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.bukkit.inventory.ItemStack;

/**
 * A smart block drawn as a note block state the resource pack retextures. The release lists which state draws each
 * block id.
 */
public interface CustomNoteBlock {

    /**
     * The block's id in the pack's registry/blocks.yml, which is also the id its item places it by.
     */
    @NotNull String getBlockId();

    /**
     * Break hardness, on the vanilla scale (stone is 1.5).
     */
    double getHardness();

    /**
     * The block whose place, step and break sounds this block makes.
     */
    @NotNull Material getSounds();

    /**
     * Whether breaking the block with this item drops it.
     */
    default boolean canHarvest(@NotNull ItemStack held) {
        return true;
    }

}
