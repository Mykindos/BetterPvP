package me.mykindos.betterpvp.core.world.zone;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

/**
 * Chunk-aligned {@link ZoneBounds} defined by a set of chunk keys in a single world. This is the only saved bounds
 * shape: it backs clan territory, where ownership is keyed per chunk and looked up in O(1). Containment is a single
 * chunk-key set check; the covered chunks <i>are</i> the set, so spatial indexing is exact.
 */
public final class ChunkBounds implements ZoneBounds {

    private final World world;
    private final LongSet chunks;

    public ChunkBounds(@NotNull World world) {
        this(world, new LongOpenHashSet());
    }

    public ChunkBounds(@NotNull World world, @NotNull LongSet chunks) {
        this.world = world;
        this.chunks = chunks;
    }

    @Override
    public boolean contains(@NotNull Location location) {
        return location.getWorld() == world && chunks.contains(Chunk.getChunkKey(location));
    }

    @Override
    public @NotNull World getWorld() {
        return world;
    }

    @Override
    public @NotNull LongSet coveredChunks() {
        return chunks;
    }

    /**
     * Adds a chunk to this bounds. Callers mutating a live zone's bounds must re-register it with the
     * {@link ZoneManager} so the spatial index stays in sync.
     *
     * @param chunkKey the chunk key (see {@link Chunk#getChunkKey(int, int)})
     */
    public void addChunk(long chunkKey) {
        chunks.add(chunkKey);
    }

    /**
     * Removes a chunk from this bounds.
     *
     * @param chunkKey the chunk key (see {@link Chunk#getChunkKey(int, int)})
     */
    public void removeChunk(long chunkKey) {
        chunks.remove(chunkKey);
    }
}
