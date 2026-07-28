package me.mykindos.betterpvp.core.scene.behavior;

import lombok.Value;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * One stop on a {@link WaypointPatrolBehavior} route: where to walk to, and how long to stand there.
 * <p>
 * The pause is what separates a patrol from a treadmill - an NPC that stops to look at a stall reads
 * as a person, one that walks a loop without breaking stride reads as a machine. The waypoint's own
 * yaw and pitch are adopted while paused, so a stop can also face something.
 */
@Value
public class Waypoint {

    Location location;

    /** How long to stand here on arrival, in milliseconds. Zero walks straight through. */
    long dwellMillis;

    public Waypoint(Location location, long dwellMillis) {
        this.location = location.clone();
        this.dwellMillis = dwellMillis;
    }

    /** A waypoint the NPC passes through without stopping. */
    public Waypoint(Location location) {
        this(location, 0L);
    }

    /**
     * Converts plain locations into pass-through waypoints.
     *
     * @param locations the ordered route positions
     * @return one waypoint per location, none of which pause
     */
    public static List<Waypoint> passingThrough(List<Location> locations) {
        final List<Waypoint> waypoints = new ArrayList<>(locations.size());
        for (Location location : locations) {
            waypoints.add(new Waypoint(location));
        }
        return waypoints;
    }
}
