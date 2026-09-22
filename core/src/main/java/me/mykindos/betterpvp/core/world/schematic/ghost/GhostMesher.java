package me.mykindos.betterpvp.core.world.schematic.ghost;

import me.mykindos.betterpvp.core.world.schematic.Schematic;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Turns a structure's blocks into as few displays as it can, so a ghost of a whole building stays a bounded number of
 * entities.
 * <p>
 * Two passes. First the shell: a block with every face against a full block is never seen, so nothing is drawn for it
 * on its own. Then the merge: runs of identical full blocks become one box, grown along x, then z, then y, starting only
 * from blocks that can be seen but free to swallow hidden ones, which are inside the box anyway. Anything that is not a
 * full block (stairs, fences, slabs) stays a display of its own, because stretching one would distort its shape.
 */
public final class GhostMesher {

    private static final int[][] NEIGHBOURS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    private final Predicate<BlockData> fullBlock;

    /**
     * @param fullBlock whether a block is a full opaque cube, which both hides its neighbours' faces and can be merged
     */
    public GhostMesher(@NotNull Predicate<BlockData> fullBlock) {
        this.fullBlock = fullBlock;
    }

    /** Uses the block's own material to decide what is a full opaque cube. */
    public static @NotNull GhostMesher standard() {
        return new GhostMesher(data -> data.getMaterial().isOccluding());
    }

    /** @param blocks solid blocks, in blocks from the anchor */
    public @NotNull List<GhostPiece> mesh(@NotNull Collection<Schematic.PlacedBlock> blocks) {
        final Map<Long, Schematic.PlacedBlock> byPosition = new HashMap<>(blocks.size());
        blocks.forEach(block -> byPosition.put(pack(block.getX(), block.getY(), block.getZ()), block));

        final List<Schematic.PlacedBlock> visible = new ArrayList<>();
        for (Schematic.PlacedBlock block : blocks) {
            if (isExposed(block, byPosition)) {
                visible.add(block);
            }
        }
        visible.sort(Comparator.comparingInt(Schematic.PlacedBlock::getY)
                .thenComparingInt(Schematic.PlacedBlock::getZ)
                .thenComparingInt(Schematic.PlacedBlock::getX));

        final Map<Long, Schematic.PlacedBlock> mergeable = new HashMap<>();
        for (Schematic.PlacedBlock block : blocks) {
            if (fullBlock.test(block.getData())) {
                mergeable.put(pack(block.getX(), block.getY(), block.getZ()), block);
            }
        }

        final List<GhostPiece> pieces = new ArrayList<>();
        final Set<Long> used = new HashSet<>();
        for (Schematic.PlacedBlock block : visible) {
            final long key = pack(block.getX(), block.getY(), block.getZ());
            if (used.contains(key)) {
                continue;
            }
            if (!mergeable.containsKey(key)) {
                pieces.add(new GhostPiece(block.getData(), block.getX(), block.getY(), block.getZ(), 1, 1, 1));
                continue;
            }
            pieces.add(grow(block, mergeable, used));
        }
        return pieces;
    }

    /** Grows the largest box of blocks identical to {@code start} it can, along x, then z, then y. */
    private @NotNull GhostPiece grow(@NotNull Schematic.PlacedBlock start, @NotNull Map<Long, Schematic.PlacedBlock> mergeable,
                                     @NotNull Set<Long> used) {
        final BlockData data = start.getData();
        final int x = start.getX();
        final int y = start.getY();
        final int z = start.getZ();

        int width = 1;
        while (matches(mergeable, used, data, x + width, y, z, 1, 1)) {
            width++;
        }
        int depth = 1;
        while (matches(mergeable, used, data, x, y, z + depth, width, 1)) {
            depth++;
        }
        int height = 1;
        while (matches(mergeable, used, data, x, y + height, z, width, depth)) {
            height++;
        }

        for (int dy = 0; dy < height; dy++) {
            for (int dz = 0; dz < depth; dz++) {
                for (int dx = 0; dx < width; dx++) {
                    used.add(pack(x + dx, y + dy, z + dz));
                }
            }
        }
        return new GhostPiece(data, x, y, z, width, height, depth);
    }

    /** Whether a {@code width} × {@code depth} area starting at the given block is all unused copies of {@code data}. */
    private static boolean matches(@NotNull Map<Long, Schematic.PlacedBlock> mergeable, @NotNull Set<Long> used,
                                   @NotNull BlockData data, int x, int y, int z, int width, int depth) {
        for (int dz = 0; dz < depth; dz++) {
            for (int dx = 0; dx < width; dx++) {
                final long key = pack(x + dx, y, z + dz);
                final Schematic.PlacedBlock block = mergeable.get(key);
                if (block == null || used.contains(key) || !block.getData().equals(data)) {
                    return false;
                }
            }
        }
        return true;
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
