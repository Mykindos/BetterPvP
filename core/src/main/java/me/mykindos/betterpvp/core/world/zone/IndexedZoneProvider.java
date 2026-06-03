package me.mykindos.betterpvp.core.world.zone;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * The built-in {@link ZoneProvider}: a per-world chunk-bucket spatial hash. Each zone is filed under every chunk key
 * its bounds overlap, so resolving a location is pack-the-chunk-key (O(1)) then a containment check against the handful
 * of zones in that bucket (usually 0-2). This is what keeps per-move zone resolution cheap regardless of how many
 * zones exist.
 */
final class IndexedZoneProvider implements ZoneProvider {

    private final Map<UUID, Long2ObjectMap<List<Zone>>> worlds = new HashMap<>();

    void add(@NotNull Zone zone) {
        final World world = zone.getBounds().getWorld();
        if (world == null) {
            return;
        }
        final Long2ObjectMap<List<Zone>> buckets = worlds.computeIfAbsent(world.getUID(), ignored -> new Long2ObjectOpenHashMap<>());
        final LongIterator iterator = zone.getBounds().coveredChunks().iterator();
        while (iterator.hasNext()) {
            buckets.computeIfAbsent(iterator.nextLong(), ignored -> new ArrayList<>(2)).add(zone);
        }
    }

    void remove(@NotNull Zone zone) {
        final World world = zone.getBounds().getWorld();
        if (world == null) {
            return;
        }
        final Long2ObjectMap<List<Zone>> buckets = worlds.get(world.getUID());
        if (buckets == null) {
            return;
        }
        final LongIterator iterator = zone.getBounds().coveredChunks().iterator();
        while (iterator.hasNext()) {
            final long key = iterator.nextLong();
            final List<Zone> bucket = buckets.get(key);
            if (bucket != null && bucket.remove(zone) && bucket.isEmpty()) {
                buckets.remove(key);
            }
        }
    }

    boolean isEmpty() {
        return worlds.isEmpty();
    }

    @Override
    public @NotNull Stream<Zone> zonesAt(@NotNull Location location) {
        final World world = location.getWorld();
        if (world == null) {
            return Stream.empty();
        }
        final Long2ObjectMap<List<Zone>> buckets = worlds.get(world.getUID());
        if (buckets == null) {
            return Stream.empty();
        }
        final List<Zone> bucket = buckets.get(Chunk.getChunkKey(location));
        if (bucket == null || bucket.isEmpty()) {
            return Stream.empty();
        }
        return bucket.stream().filter(zone -> zone.contains(location));
    }

    @Override
    public @NotNull Collection<Zone> zones() {
        // A zone is filed under every chunk it overlaps, so dedupe across buckets.
        final Set<Zone> all = new LinkedHashSet<>();
        for (Long2ObjectMap<List<Zone>> buckets : worlds.values()) {
            for (List<Zone> bucket : buckets.values()) {
                all.addAll(bucket);
            }
        }
        return all;
    }
}
