package me.mykindos.betterpvp.clans.world.resource;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Value;
import me.mykindos.betterpvp.clans.Clans;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Crash-surviving record of individual <em>blocks</em> that are mid-respawn — the small per-block delta a world-derived
 * archetype (currently {@link me.mykindos.betterpvp.clans.world.resource.archetype.OreArchetype OreArchetype}) needs to
 * reconcile its region scan against, since a degraded block's pristine state can't be re-derived from config after a
 * restart. Each in-flight block is one tiny file under {@code scenes/cache/block-single/}, written when the block is
 * degraded and removed when it respawns; {@link #entriesWithin} answers a node's reconcile from the in-memory mirror.
 * <p>
 * A thin block-shaped facade over the generic {@link ResourceCacheStore} — it owns the {@link Entry} shape and the
 * spatial query, the store owns the I/O.
 */
@Singleton
public class BlockReplacementStore {

    private final ResourceCacheStore<Entry> store;

    @Inject
    public BlockReplacementStore(@NotNull Clans clans) {
        this.store = new ResourceCacheStore<>(new File(clans.getDataFolder(), "scenes/cache/block-single"),
                new EntryCodec());
    }

    /** Records (or overwrites) a block as mid-respawn. {@code blockData} is its pristine {@code BlockData.getAsString()}. */
    public void markDirty(@NotNull String world, int x, int y, int z, @NotNull String blockData, long minedAtMs) {
        store.put(new Entry(world, x, y, z, blockData, minedAtMs));
    }

    /** Drops a block's record once it has respawned. */
    public void clear(@NotNull String world, int x, int y, int z) {
        store.remove(key(world, x, y, z));
    }

    /** Every in-flight block whose position lies inside the given cuboid (inclusive), used to reconcile a node at load. */
    public @NotNull List<Entry> entriesWithin(@NotNull World world, @NotNull Location min, @NotNull Location max) {
        final String name = world.getName();
        final int minX = min.getBlockX();
        final int minY = min.getBlockY();
        final int minZ = min.getBlockZ();
        final int maxX = max.getBlockX();
        final int maxY = max.getBlockY();
        final int maxZ = max.getBlockZ();
        final List<Entry> within = new ArrayList<>();
        for (Entry entry : store.values()) {
            if (entry.world.equals(name)
                    && entry.x >= minX && entry.x <= maxX
                    && entry.y >= minY && entry.y <= maxY
                    && entry.z >= minZ && entry.z <= maxZ) {
                within.add(entry);
            }
        }
        return within;
    }

    private static @NotNull String key(@NotNull String world, int x, int y, int z) {
        return world + ";" + x + ";" + y + ";" + z;
    }

    /** A single mid-respawn block: its world, position, pristine block data, and the time it was mined. */
    @Value
    public static class Entry {
        String world;
        int x;
        int y;
        int z;
        String blockData;
        long minedAtMs;
    }

    /** Serialises an {@link Entry}; the body carries the full identity so the key rebuilds on load. */
    private static final class EntryCodec implements RecordCodec<Entry> {
        @Override
        public @NotNull String key(@NotNull Entry entry) {
            return BlockReplacementStore.key(entry.world, entry.x, entry.y, entry.z);
        }

        @Override
        public void write(@NotNull DataOutputStream out, @NotNull Entry entry) throws IOException {
            out.writeUTF(entry.world);
            out.writeInt(entry.x);
            out.writeInt(entry.y);
            out.writeInt(entry.z);
            out.writeUTF(entry.blockData);
            out.writeLong(entry.minedAtMs);
        }

        @Override
        public @NotNull Entry read(@NotNull DataInputStream in) throws IOException {
            final String world = in.readUTF();
            final int x = in.readInt();
            final int y = in.readInt();
            final int z = in.readInt();
            final String blockData = in.readUTF();
            final long minedAt = in.readLong();
            return new Entry(world, x, y, z, blockData, minedAt);
        }
    }
}
