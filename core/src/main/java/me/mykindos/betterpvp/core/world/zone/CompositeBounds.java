package me.mykindos.betterpvp.core.world.zone;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A {@link ZoneBounds} that is the union of several others, so one zone can be several disjoint shapes.
 * <p>
 * This is what a mapper region name matching more than one region should become. Building one {@link Zone} per region
 * instead would give them all the same {@link Zone#getKey() key}, and since identity is the key, every consumer that
 * indexes zones by it — the manager's registry, discovery, listings, the map's style snapshot — would keep only one of
 * them and silently drop the rest.
 * <p>
 * A composite is chunk-indexable when all its parts are; if any part is ambient the union is treated as ambient, since
 * the index would otherwise miss the part it cannot bucket.
 */
public final class CompositeBounds implements ZoneBounds {

    private final List<ZoneBounds> parts;
    private final LongSet chunks;

    private CompositeBounds(@NotNull List<ZoneBounds> parts) {
        this.parts = List.copyOf(parts);
        this.chunks = union(this.parts);
    }

    /**
     * @param parts the bounds to union; a single part is returned unwrapped
     * @return a bounds covering all of them
     */
    public static ZoneBounds of(@NotNull List<ZoneBounds> parts) {
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("A composite bounds needs at least one part");
        }
        return parts.size() == 1 ? parts.getFirst() : new CompositeBounds(parts);
    }

    @Override
    public boolean contains(@NotNull Location location) {
        for (ZoneBounds part : parts) {
            if (part.contains(location)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsColumn(@NotNull World world, int x, int y, int z) {
        for (ZoneBounds part : parts) {
            if (part.containsColumn(world, x, y, z)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @Nullable World getWorld() {
        for (ZoneBounds part : parts) {
            final World world = part.getWorld();
            if (world != null) {
                return world;
            }
        }
        return null;
    }

    @Override
    public @NotNull LongSet coveredChunks() {
        return chunks;
    }

    private static LongSet union(List<ZoneBounds> parts) {
        final LongSet set = new LongOpenHashSet();
        for (ZoneBounds part : parts) {
            final LongSet covered = part.coveredChunks();
            if (covered.isEmpty()) {
                return LongSet.of();
            }
            set.addAll(covered);
        }
        return set;
    }
}
