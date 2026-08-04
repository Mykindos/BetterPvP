package me.mykindos.betterpvp.clans.world.discovery.sighting;

import me.mykindos.betterpvp.clans.world.discovery.OceanOffset;
import me.mykindos.betterpvp.clans.world.discovery.OceanPoint;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * What one expedition can currently see, and the two ways off the horizon: falling astern, and being reached.
 * <p>
 * The projection is passed in rather than looked up, so what is culled, what is drawn and what has been reached are all
 * decided from the offsets alone.
 */
public class SightingField {

    private final List<Sighting> sightings = new ArrayList<>();

    /** When the distance texts were last written, so they are not rewritten twenty times a second. */
    private long labelledAt;

    public void add(@NotNull Sighting sighting) {
        sightings.add(sighting);
    }

    /** A snapshot of what is out there. */
    public @NotNull List<Sighting> live() {
        return List.copyOf(sightings);
    }

    public int size() {
        return sightings.size();
    }

    /** Whether the distance texts are due a rewrite, starting the next interval when they are. */
    public boolean dueForLabels(long now, long intervalMillis) {
        if (now - labelledAt < intervalMillis) {
            return false;
        }
        labelledAt = now;
        return true;
    }

    /**
     * Culls whatever has fallen astern, draws the rest on their current bearings, and reports the island the crew has
     * arrived at.
     * <p>
     * A reached sighting leaves the field before it is handed back, so landfall cannot fire twice while the ship is
     * still sitting on top of it.
     *
     * @param project where a fixed point on the plane lies relative to the ship right now
     * @param render  where a ship-local offset falls in the world
     */
    public @NotNull Optional<Sighting> sweep(@NotNull Function<OceanPoint, OceanOffset> project,
                                             @NotNull Function<OceanOffset, Location> render,
                                             @NotNull SightingConfig config, boolean refreshLabels) {
        Sighting reached = null;

        final Iterator<Sighting> iterator = sightings.iterator();
        while (iterator.hasNext()) {
            final Sighting sighting = iterator.next();
            final OceanOffset offset = project.apply(sighting.getPoint());
            final double distance = offset.distance();

            if (distance > config.getDespawnDistance()) {
                iterator.remove();
                sighting.remove();
                continue;
            }

            // Only the first is landed at; a crew can only go ashore in one place, and the rest go with the ocean.
            if (reached == null && distance <= config.getArrivalDistance()) {
                iterator.remove();
                sighting.remove();
                reached = sighting;
                continue;
            }

            sighting.render(render.apply(SightingGeometry.pinned(offset, config.getPinDistance())), distance,
                    config.getArrivalDistance(), config.getRampStartDistance(), refreshLabels);
        }
        return Optional.ofNullable(reached);
    }

    /** Clears the horizon, taking every marker with it. */
    public void clear() {
        sightings.forEach(Sighting::remove);
        sightings.clear();
    }
}
