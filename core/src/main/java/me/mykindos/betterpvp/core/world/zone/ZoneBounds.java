package me.mykindos.betterpvp.core.world.zone;

import it.unimi.dsi.fastutil.longs.LongSet;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Defines the spatial extent of a {@link Zone} and how a location is tested against it.
 * <p>
 * A bounds is one of:
 * <ul>
 *     <li>{@link RegionBounds} - free-form, backed by a Mapper region (loaded, never saved).</li>
 *     <li>{@link ChunkBounds} - chunk-aligned, a set of chunk keys (the only saved shape, used for clan territory).</li>
 *     <li>{@link ColumnMaskBounds} - the footprint one terrain type occupies in a scanned mask.</li>
 *     <li>{@link GlobalBounds} - a predicate over a whole world (ambient, e.g. "in water"); not chunk-indexable.</li>
 *     <li>{@link CompositeBounds} - the union of several of the above, for a zone made of disjoint shapes.</li>
 * </ul>
 */
public interface ZoneBounds {

    /**
     * @param location the location to test
     * @return whether this bounds contains the location
     */
    boolean contains(@NotNull Location location);

    /**
     * Whether this bounds covers a whole block column, ignoring how tall it is. Top-down consumers — the minimap above
     * all — see one pixel per column and have no meaningful Y to test against: a dock that starts a block above the
     * pier still <em>occupies</em> the water columns beside it, and drawing it otherwise leaves holes wherever the
     * surface happens to sit outside the bounds' Y range.
     * <p>
     * The default answers with a plain {@link #contains} at {@code y}, which is all a bounds whose extent genuinely
     * depends on height (e.g. a {@link GlobalBounds} predicate) can say. Shapes that know their footprint override it.
     *
     * @param world the world the column is in
     * @param x     block x
     * @param y     a representative height in the column (typically its surface), for bounds that need one
     * @param z     block z
     * @return whether the column is covered
     */
    default boolean containsColumn(@NotNull World world, int x, int y, int z) {
        return contains(new Location(world, x, y, z));
    }

    /**
     * @return the world this bounds lives in, or {@code null} if it is not yet bound to one
     */
    @Nullable World getWorld();

    /**
     * The chunk keys (see {@link org.bukkit.Chunk#getChunkKey(int, int)}) this bounds occupies, used to register the
     * owning zone into the spatial index for near-O(1) lookups. An <b>empty</b> set marks the bounds as ambient: it is
     * not chunk-indexable and the manager evaluates it against every resolution instead. Keep ambient bounds few.
     *
     * @return the covered chunk keys, never {@code null}
     */
    @NotNull LongSet coveredChunks();
}
