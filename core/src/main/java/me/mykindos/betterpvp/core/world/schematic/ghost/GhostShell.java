package me.mykindos.betterpvp.core.world.schematic.ghost;

import me.mykindos.betterpvp.core.world.schematic.Schematic;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Picks the blocks of a structure a ghost has to draw. Every block is drawn as itself, one display each with its own
 * block state, except a block with every face against a full block, which can never be seen and is left out.
 */
public final class GhostShell {

    private static final int[][] NEIGHBOURS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    private final Predicate<BlockData> fullBlock;

    /**
     * @param fullBlock whether a block is a full opaque cube, which hides its neighbours' faces
     */
    public GhostShell(@NotNull Predicate<BlockData> fullBlock) {
        this.fullBlock = fullBlock;
    }

    /** Uses the block's own material to decide what is a full opaque cube. */
    public static @NotNull GhostShell standard() {
        return new GhostShell(data -> data.getMaterial().isOccluding());
    }

    /** @return the blocks that can be seen, in the order given */
    public @NotNull List<Schematic.PlacedBlock> visible(@NotNull Collection<Schematic.PlacedBlock> blocks) {
        final Map<Long, Schematic.PlacedBlock> byPosition = new HashMap<>(blocks.size());
        blocks.forEach(block -> byPosition.put(pack(block.getX(), block.getY(), block.getZ()), block));

        final List<Schematic.PlacedBlock> visible = new ArrayList<>();
        for (Schematic.PlacedBlock block : blocks) {
            if (isExposed(block, byPosition)) {
                visible.add(block);
            }
        }
        return visible;
    }

    private boolean isExposed(@NotNull Schematic.PlacedBlock block, @NotNull Map<Long, Schematic.PlacedBlock> byPosition) {
        for (int[] offset : NEIGHBOURS) {
            final Schematic.PlacedBlock neighbour = byPosition.get(
                    pack(block.getX() + offset[0], block.getY() + offset[1], block.getZ() + offset[2]));
            if (neighbour == null || !fullBlock.test(neighbour.getData())) {
                return true;
            }
        }
        return false;
    }

    private static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }
}
