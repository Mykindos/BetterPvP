package me.mykindos.betterpvp.core.world.terrain;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * A per-column {@link TerrainType} classification of one world, the output of a terrain scan and the source of truth for
 * the capability zones built on top of it.
 * <p>
 * Storage is a chunk-tiled grid: each 16×16 chunk that holds at least one non-{@link TerrainType#INTERIOR INTERIOR}
 * column keeps a {@code byte[256]} of ordinals, and interior columns cost nothing (an absent chunk or a zero byte).
 * Lookups are O(1) — pack the chunk key, index the byte — which is what lets a {@code ColumnMaskBounds} answer
 * containment on the movement hot path. The chunk keying matches {@link Chunk#getChunkKey(int, int)} so the covered
 * chunks feed straight into the zone spatial index.
 */
public final class TerrainMask {

    private static final int MAGIC = 0x544D_5348; // "TMSH"
    private static final int VERSION = 1;
    private static final int CHUNK_AREA = 256; // 16 * 16 columns

    private final World world;
    private final Long2ObjectMap<byte[]> chunks;

    public TerrainMask(@NotNull World world) {
        this(world, new Long2ObjectOpenHashMap<>());
    }

    private TerrainMask(@NotNull World world, @NotNull Long2ObjectMap<byte[]> chunks) {
        this.world = world;
        this.chunks = chunks;
    }

    public @NotNull World getWorld() {
        return world;
    }

    /**
     * @param x block x
     * @param z block z
     * @return the classification of that column, {@link TerrainType#INTERIOR} if none was recorded
     */
    public @NotNull TerrainType get(int x, int z) {
        final byte[] cell = chunks.get(Chunk.getChunkKey(x >> 4, z >> 4));
        return cell == null ? TerrainType.INTERIOR : TerrainType.byId(cell[index(x, z)]);
    }

    /**
     * Records a column's classification. Writing {@link TerrainType#INTERIOR} clears it, keeping the mask sparse.
     *
     * @param x    block x
     * @param z    block z
     * @param type the classification
     */
    public void set(int x, int z, @NotNull TerrainType type) {
        final long key = Chunk.getChunkKey(x >> 4, z >> 4);
        if (type == TerrainType.INTERIOR) {
            final byte[] cell = chunks.get(key);
            if (cell != null) {
                cell[index(x, z)] = 0;
            }
            return;
        }
        chunks.computeIfAbsent(key, ignored -> new byte[CHUNK_AREA])[index(x, z)] = (byte) type.ordinal();
    }

    /**
     * @param type the classification to look for
     * @return the keys of every chunk holding at least one column of that type, ready for the zone spatial index
     */
    public @NotNull LongSet coveredChunks(@NotNull TerrainType type) {
        final LongSet set = new LongOpenHashSet();
        final byte id = (byte) type.ordinal();
        for (Long2ObjectMap.Entry<byte[]> entry : chunks.long2ObjectEntrySet()) {
            final byte[] cell = entry.getValue();
            for (int i = 0; i < CHUNK_AREA; i++) {
                if (cell[i] == id) {
                    set.add(entry.getLongKey());
                    break;
                }
            }
        }
        return set;
    }

    public boolean isEmpty() {
        return chunks.isEmpty();
    }

    private static int index(int x, int z) {
        return (z & 15) * 16 + (x & 15);
    }

    /**
     * Writes this mask to disk in the compact chunk-tiled format {@link #load(File, World)} reads back.
     *
     * @param file the destination file
     * @throws IOException if writing fails
     */
    public void save(@NotNull File file) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            out.writeInt(chunks.size());
            for (Long2ObjectMap.Entry<byte[]> entry : chunks.long2ObjectEntrySet()) {
                out.writeLong(entry.getLongKey());
                out.write(entry.getValue());
            }
        }
    }

    /**
     * @param file  a file previously written by {@link #save(File)}
     * @param world the world the mask belongs to
     * @return the loaded mask
     * @throws IOException if the file is missing, truncated, or not a terrain mask of a supported version
     */
    public static @NotNull TerrainMask load(@NotNull File file, @NotNull World world) throws IOException {
        final Long2ObjectMap<byte[]> chunks = new Long2ObjectOpenHashMap<>();
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            if (in.readInt() != MAGIC) {
                throw new IOException("Not a terrain mask file: " + file);
            }
            final int version = in.readInt();
            if (version != VERSION) {
                throw new IOException("Unsupported terrain mask version " + version + " in " + file);
            }
            final int count = in.readInt();
            for (int i = 0; i < count; i++) {
                final long key = in.readLong();
                final byte[] cell = new byte[CHUNK_AREA];
                in.readFully(cell);
                chunks.put(key, cell);
            }
        }
        return new TerrainMask(world, chunks);
    }
}
