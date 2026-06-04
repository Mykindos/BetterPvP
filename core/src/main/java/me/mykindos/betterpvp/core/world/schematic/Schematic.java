package me.mykindos.betterpvp.core.world.schematic;

import lombok.Getter;
import lombok.Value;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * An immutable, format-agnostic snapshot of a block volume in origin-relative coordinates.
 * <p>
 * A {@link Schematic} carries no Bukkit world or FAWE types beyond {@link BlockData}; it is produced by a
 * {@link SchematicFormat} (one per file type) and pasted by {@link SchematicAnimator}. Coordinates are relative to the
 * volume's minimum corner — pasting at a {@code Location} places that corner at the location.
 */
@Getter
public final class Schematic {

    private final int width;
    private final int height;
    private final int length;
    /** The author-chosen anchor (the FAWE {@code //copy} origin), in min-relative coordinates. */
    private final int anchorX;
    private final int anchorY;
    private final int anchorZ;
    private final List<PlacedBlock> blocks;

    public Schematic(int width, int height, int length, @NotNull List<PlacedBlock> blocks) {
        this(width, height, length, 0, 0, 0, blocks);
    }

    public Schematic(int width, int height, int length, int anchorX, int anchorY, int anchorZ,
                     @NotNull List<PlacedBlock> blocks) {
        this.width = width;
        this.height = height;
        this.length = length;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorZ = anchorZ;
        this.blocks = List.copyOf(blocks);
    }

    /**
     * @return the number of blocks captured (air included, so animation frames can clear cells)
     */
    public int blockCount() {
        return blocks.size();
    }

    /**
     * A single block at an origin-relative position.
     */
    @Value
    public static class PlacedBlock {
        int x;
        int y;
        int z;
        @NotNull BlockData data;
    }
}
