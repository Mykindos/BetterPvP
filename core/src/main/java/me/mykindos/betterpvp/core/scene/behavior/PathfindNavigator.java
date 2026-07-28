package me.mykindos.betterpvp.core.scene.behavior;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Mob;

import java.util.List;

/**
 * Travels each leg using the server's own pathfinder, so the NPC routes itself around geometry.
 * <p>
 * The route's waypoints are treated as anchors to reach, not as a line to walk: the path between two
 * of them is whatever the server's search happens to find. Choose {@link DirectNavigator} instead
 * when the authored line is the point.
 *
 * <h3>Why the follow range is widened</h3>
 * A mob's follow range caps both the box a path search may look inside and how many nodes it may
 * visit, and the search returns a <i>partial</i> path rather than failing once it runs out. A
 * default-range mob therefore walks a fraction of a long leg, stops short, and reads as wandering off
 * the route entirely. Sizing the range to the route's longest leg makes a single search enough.
 */
public class PathfindNavigator implements PatrolNavigator {

    /** Slack added to the longest leg, so a detour around geometry still fits inside the search. */
    private static final double PATH_RANGE_MARGIN = 16.0;

    /** Ceiling on the follow range - the search's node budget scales with it, so it cannot be unbounded. */
    private static final double MAX_PATH_RANGE = 96.0;

    /** Minimum ticks between two searches on one leg, bounding the cost when a path keeps failing. */
    private static final int REPATH_COOLDOWN_TICKS = 10;

    /** Searches allowed per leg before the waypoint is written off as unreachable. */
    private static final int MAX_PATHS_PER_LEG = 20;

    private int pathsThisLeg = 0;
    private int repathCooldown = 0;

    @Override
    public void prepare(Mob mob, List<Waypoint> route) {
        // Not restored afterwards: the backing entity belongs to this NPC alone and dies with it.
        final AttributeInstance followRange = mob.getAttribute(Attribute.FOLLOW_RANGE);
        final double required = requiredRange(route);
        if (followRange != null && followRange.getBaseValue() < required) {
            followRange.setBaseValue(required);
        }
    }

    @Override
    public void begin(Mob mob, Location target, double speed) {
        pathsThisLeg = 0;
        repathCooldown = 0;
        path(mob, target, speed);
    }

    @Override
    public boolean advance(Mob mob, Location target, double speed) {
        if (repathCooldown > 0) {
            repathCooldown--;
            return true;
        }
        // A path running out short of the waypoint is the ordinary case on a long leg, not a fault.
        // Extending it immediately is what keeps the NPC on the route.
        if (mob.getPathfinder().getCurrentPath() == null) {
            return path(mob, target, speed);
        }
        return true;
    }

    @Override
    public boolean recover(Mob mob, Location target, double speed) {
        return path(mob, target, speed);
    }

    @Override
    public void stop(Mob mob) {
        mob.getPathfinder().stopPathfinding();
    }

    /** @return {@code false} once the leg has burned its allowance, so a walled-off waypoint is dropped */
    private boolean path(Mob mob, Location target, double speed) {
        if (pathsThisLeg++ >= MAX_PATHS_PER_LEG) {
            return false;
        }
        repathCooldown = REPATH_COOLDOWN_TICKS;
        mob.getPathfinder().moveTo(target, speed);
        return true;
    }

    /** @return the follow range needed to search a whole leg at once, capped for cost */
    private static double requiredRange(List<Waypoint> route) {
        double longestLeg = 0;
        for (int i = 0; i < route.size(); i++) {
            final Location from = route.get(i).getLocation();
            final Location to = route.get((i + 1) % route.size()).getLocation();
            final double dx = to.getX() - from.getX();
            final double dy = to.getY() - from.getY();
            final double dz = to.getZ() - from.getZ();
            longestLeg = Math.max(longestLeg, Math.sqrt(dx * dx + dy * dy + dz * dz));
        }
        return Math.min(MAX_PATH_RANGE, longestLeg + PATH_RANGE_MARGIN);
    }

}
