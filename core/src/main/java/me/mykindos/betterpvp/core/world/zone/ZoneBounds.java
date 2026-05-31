package me.mykindos.betterpvp.core.world.zone;

import it.unimi.dsi.fastutil.longs.LongSet;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Defines the spatial extent of a {@link Zone} and how a location is tested against it.
 * <p>
 * A bounds is one of three shapes:
 * <ul>
 *     <li>{@link RegionBounds} - free-form, backed by a Mapper region (loaded, never saved).</li>
 *     <li>{@link ChunkBounds} - chunk-aligned, a set of chunk keys (the only saved shape, used for clan territory).</li>
 *     <li>{@link GlobalBounds} - a predicate over a whole world (ambient, e.g. "in water"); not chunk-indexable.</li>
 * </ul>
 */
public interface ZoneBounds {

    /**
     * @param location the location to test
     * @return whether this bounds contains the location
     */
    boolean contains(@NotNull Location location);

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
