package me.mykindos.betterpvp.core.world.zone;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * Contributes the zones present at a location. The {@link ZoneManager} owns a built-in {@link IndexedZoneProvider} for
 * registered {@link RegionBounds}/{@link ChunkBounds} zones; other modules can plug in their own source via
 * {@link ZoneManager#addProvider(ZoneProvider)} without the framework knowing how they store ownership.
 * <p>
 * For example, clans will register a provider that reads chunk ownership from each chunk's
 * {@link org.bukkit.persistence.PersistentDataContainer} and maps it to that clan's territory zone, keeping the
 * PDC as the source of truth while staying O(1).
 */
@FunctionalInterface
public interface ZoneProvider {

    /**
     * @param location the location to resolve
     * @return a stream of zones that contain the location (already filtered by containment)
     */
    @NotNull Stream<Zone> zonesAt(@NotNull Location location);

    /**
     * Best-effort enumeration of every zone this provider could resolve, used for admin/listing tooling (e.g.
     * {@code /zone list}). Resolution still happens via {@link #zonesAt(Location)}; providers that cannot cheaply
     * enumerate their zones (e.g. purely predicate-based sources) may leave this returning an empty collection.
     *
     * @return all zones this provider knows about, or an empty collection if it cannot enumerate them
     */
    default @NotNull Collection<Zone> zones() {
        return List.of();
    }
}
